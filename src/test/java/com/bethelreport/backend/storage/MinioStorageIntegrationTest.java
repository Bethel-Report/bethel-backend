package com.bethelreport.backend.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MinioStorageIntegrationTest {
  @Container
  private static final GenericContainer<?> MINIO = new GenericContainer<>(image())
      .withEnv("MINIO_ROOT_USER", "integration-test")
      .withEnv("MINIO_ROOT_PASSWORD", "integration-test-secret").withCommand("server", "/data")
      .withExposedPorts(9000);

  private static Future<String> image() {
    String existingImage = System.getProperty("minio.test.image");
    if (existingImage != null && !existingImage.isBlank()) {
      return CompletableFuture.completedFuture(existingImage);
    }
    return new ImageFromDockerfile("bethel-minio:RELEASE.2025-10-15T17-29-55Z", false)
        .withFileFromPath("Dockerfile", Path.of("docker/minio/Dockerfile"));
  }

  @Test
  void privateObjectCanBeUploadedDownloadedAndDeleted() throws Exception {
    MinioStorage storage = new MinioStorage(
        "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000), "integration-test",
        "integration-test-secret", "publications-test");
    byte[] bytes = "%PDF-1.7 integration".getBytes(StandardCharsets.US_ASCII);
    String key = "documentos/publicacoes/1/test";
    storage.upload(key, new ByteArrayInputStream(bytes), bytes.length, "application/pdf");
    assertArrayEquals(bytes, storage.download(key));
    try (HttpClient anonymous = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .build()) {
      HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + MINIO.getHost() + ":"
          + MINIO.getMappedPort(9000) + "/publications-test/" + key))
          .timeout(Duration.ofSeconds(10)).GET().build();
      assertEquals(403,
          anonymous.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
    }
    storage.delete(key);
    storage.delete(key);
    assertThrows(ResponseStatusException.class, () -> storage.download(key));
  }
}
