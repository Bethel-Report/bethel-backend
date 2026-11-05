package com.bethelreport.backend.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StorageCleanupTest {
  @Mock
  private JdbcTemplate jdbc;
  @Mock
  private MinioStorage storage;
  @InjectMocks
  private StorageCleanup cleanup;

  @Test
  void failedRemovalStaysQueuedWhileOtherObjectsAreProcessed() {
    when(jdbc.queryForList(any(String.class), eq(String.class), any(Object.class)))
        .thenReturn(List.of("unavailable", "removed"));
    doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(storage)
        .delete("unavailable");
    cleanup.cleanup();
    verify(jdbc, never()).update("DELETE FROM storage_deletions WHERE object_key = ?",
        "unavailable");
    verify(storage).delete("removed");
    verify(jdbc).update("DELETE FROM storage_deletions WHERE object_key = ?", "removed");
  }
}
