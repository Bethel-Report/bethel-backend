package com.bethelreport.backend.storage;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class MinioTestContainer extends GenericContainer<MinioTestContainer> {
  public MinioTestContainer() {
    super(image());
    withEnv("MINIO_ROOT_USER", "integration-test");
    withEnv("MINIO_ROOT_PASSWORD", "integration-test-secret");
    withCommand("server", "/data");
    withExposedPorts(9000);
    waitingFor(Wait.forHttp("/minio/health/ready").forStatusCode(200));
    withStartupTimeout(Duration.ofMinutes(2));
  }

  public String endpoint() {
    return "http://" + getHost() + ":" + getMappedPort(9000);
  }

  private static Future<String> image() {
    String existingImage = System.getProperty("minio.test.image");
    if (existingImage != null && !existingImage.isBlank()) {
      return CompletableFuture.completedFuture(existingImage);
    }
    return new ImageFromDockerfile("bethel-minio:RELEASE.2025-10-15T17-29-55Z", false)
        .withFileFromPath("Dockerfile", Path.of("docker/minio/Dockerfile"));
  }
}
