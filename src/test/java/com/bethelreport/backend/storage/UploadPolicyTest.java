package com.bethelreport.backend.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class UploadPolicyTest {
  @Test
  void policyStaysDisabledUntilBusinessFormatsAndSizeAreConfigured() {
    UploadPolicy policy = new UploadPolicy("", "0B");
    assertThrows(ResponseStatusException.class,
        () -> policy.validate(file("file.pdf", "%PDF-1.7")));
  }

  @Test
  void normalizesFilenameAndAssignsMimeWithoutTrustingClient() {
    var file = new MockMultipartFile("file", "../../manual.pdf", "text/html",
        "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));
    var validated = new UploadPolicy("pdf", "1MB").validate(file);
    assertEquals("manual.pdf", validated.filename());
    assertEquals("application/pdf", validated.contentType());
  }

  @Test
  void supportsPhotoAndVideoSignaturesWithSharedPolicy() {
    UploadPolicy policy = new UploadPolicy("png,mp4", "1MB");
    var photo = new MockMultipartFile("file", "photo.png", "image/png",
        new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
    var video = new MockMultipartFile("file", "video.mp4", "video/mp4",
        new byte[]{0, 0, 0, 12, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'});
    assertEquals("image/png", policy.validate(photo).contentType());
    assertEquals("video/mp4", policy.validate(video).contentType());
  }

  @Test
  void rejectsUnsupportedConfigurationAndFileLargerThanPolicy() {
    assertThrows(IllegalArgumentException.class, () -> new UploadPolicy("exe", "1MB"));
    UploadPolicy policy = new UploadPolicy("pdf", "4B");
    assertThrows(ResponseStatusException.class,
        () -> policy.validate(file("file.pdf", "%PDF-1.7")));
  }

  private MockMultipartFile file(String filename, String content) {
    return new MockMultipartFile("file", filename, "application/pdf",
        content.getBytes(StandardCharsets.US_ASCII));
  }
}
