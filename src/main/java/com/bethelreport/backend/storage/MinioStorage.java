package com.bethelreport.backend.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MinioStorage {
  private final String endpoint;
  private final String accessKey;
  private final String secretKey;
  private final String bucket;

  public MinioStorage(@Value("${storage.minio.endpoint}") String endpoint,
      @Value("${storage.minio.access-key}") String accessKey,
      @Value("${storage.minio.secret-key}") String secretKey,
      @Value("${storage.minio.bucket}") String bucket) {
    this.endpoint = endpoint;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.bucket = bucket;
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void upload(String key, InputStream stream, long size, String contentType) {
    try (MinioClient client = client()) {
      if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
        createBucket(client);
      }
      client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(stream, size, -1L)
          .contentType(contentType).build());
    } catch (Exception exception) {
      throw unavailable(exception);
    }
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public byte[] download(String key) {
    try (MinioClient client = client();
        InputStream stream = client
            .getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
      return stream.readAllBytes();
    } catch (Exception exception) {
      throw unavailable(exception);
    }
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void delete(String key) {
    try (MinioClient client = client()) {
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
    } catch (ErrorResponseException exception) {
      if (!"NoSuchBucket".equals(exception.errorResponse().code())
          && !"NoSuchKey".equals(exception.errorResponse().code())) {
        throw unavailable(exception);
      }
    } catch (Exception exception) {
      throw unavailable(exception);
    }
  }

  private MinioClient client() {
    if (accessKey.isBlank() || secretKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Armazenamento de arquivos não configurado.");
    }
    MinioClient client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey)
        .build();
    client.setTimeout(10_000, 30_000, 30_000);
    return client;
  }

  private void createBucket(MinioClient client) throws MinioException {
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    } catch (ErrorResponseException exception) {
      if (!"BucketAlreadyOwnedByYou".equals(exception.errorResponse().code())) {
        throw exception;
      }
    }
  }

  private ResponseStatusException unavailable(Exception cause) {
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
        "Armazenamento temporariamente indisponível.", cause);
  }
}
