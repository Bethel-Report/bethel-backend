package com.bethelreport.backend.storage;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StorageCleanup {
  private static final Logger LOGGER = LoggerFactory.getLogger(StorageCleanup.class);
  private final JdbcTemplate jdbc;
  private final MinioStorage storage;

  public StorageCleanup(JdbcTemplate jdbc, MinioStorage storage) {
    this.jdbc = jdbc;
    this.storage = storage;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registerUpload(String key) {
    jdbc.update("INSERT INTO storage_deletions (object_key, available_at) VALUES (?, ?)", key,
        LocalDateTime.now().plusHours(1));
  }

  public void cancelUploadCleanup(String key) {
    jdbc.update("DELETE FROM storage_deletions WHERE object_key = ?", key);
  }

  public void enqueueDeletion(String key) {
    jdbc.update("INSERT INTO storage_deletions (object_key, available_at) VALUES (?, ?) "
        + "ON CONFLICT (object_key) DO NOTHING", key, LocalDateTime.now());
  }

  @Scheduled(fixedDelayString = "${storage.cleanup-delay-ms:60000}")
  public void cleanup() {
    List<String> keys = jdbc
        .queryForList("SELECT object_key FROM storage_deletions WHERE available_at <= ? "
            + "ORDER BY available_at LIMIT 50", String.class, LocalDateTime.now());
    for (String key : keys) {
      try {
        storage.delete(key);
        jdbc.update("DELETE FROM storage_deletions WHERE object_key = ?", key);
      } catch (ResponseStatusException exception) {
        LOGGER.warn("Limpeza de arquivo adiada: armazenamento indisponível.", exception);
      }
    }
  }
}
