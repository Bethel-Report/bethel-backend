package com.bethelreport.backend.storage;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StorageDeletionQueue {
  private final JdbcTemplate jdbc;

  public StorageDeletionQueue(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void enqueueDeletion(String key) {
    jdbc.update("INSERT INTO storage_deletions (object_key, available_at) VALUES (?, ?) "
        + "ON CONFLICT (object_key) DO NOTHING", key, LocalDateTime.now());
  }
}
