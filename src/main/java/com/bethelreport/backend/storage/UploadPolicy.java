package com.bethelreport.backend.storage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UploadPolicy {
  private static final Map<String, String> MIME_TYPES = Map.ofEntries(
      Map.entry("pdf", "application/pdf"), Map.entry("jpg", "image/jpeg"),
      Map.entry("jpeg", "image/jpeg"), Map.entry("png", "image/png"),
      Map.entry("webp", "image/webp"), Map.entry("mp4", "video/mp4"),
      Map.entry("webm", "video/webm"),
      Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
      Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
      Map.entry("pptx",
          "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
  private static final Map<String, int[]> SIGNATURES = Map.of("pdf",
      new int[]{0x25, 0x50, 0x44, 0x46, 0x2d}, "jpg", new int[]{0xff, 0xd8, 0xff}, "jpeg",
      new int[]{0xff, 0xd8, 0xff}, "png", new int[]{0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a},
      "webm", new int[]{0x1a, 0x45, 0xdf, 0xa3}, "docx", new int[]{0x50, 0x4b, 0x03, 0x04}, "xlsx",
      new int[]{0x50, 0x4b, 0x03, 0x04}, "pptx", new int[]{0x50, 0x4b, 0x03, 0x04});

  private final Set<String> allowedExtensions;
  private final long maxBytes;

  public UploadPolicy(@Value("${storage.upload.allowed-extensions:}") String extensions,
      @Value("${storage.upload.max-size:0B}") String maxSize) {
    this.allowedExtensions = Arrays.stream(extensions.split(","))
        .map(value -> value.strip().toLowerCase(Locale.ROOT)).filter(value -> !value.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
    this.maxBytes = DataSize.parse(maxSize).toBytes();
    if (!MIME_TYPES.keySet().containsAll(allowedExtensions)) {
      throw new IllegalArgumentException("Extensão de upload sem suporte.");
    }
  }

  public ValidatedUpload validate(MultipartFile file) {
    requireConfigured();
    if (file.isEmpty()) {
      throw new IllegalArgumentException("Arquivo vazio.");
    }
    validateSize(file.getSize());
    String filename = safeFilename(file.getOriginalFilename());
    int dot = filename.lastIndexOf('.');
    String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    validateExtension(filename, extension);
    byte[] bytes = read(file);
    validateSize(bytes.length);
    if (!matchesSignature(extension, bytes)) {
      throw new IllegalArgumentException("Conteúdo incompatível com a extensão do arquivo.");
    }
    return new ValidatedUpload(filename, MIME_TYPES.get(extension), bytes);
  }

  private void requireConfigured() {
    if (allowedExtensions.isEmpty() || maxBytes <= 0) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Configure os formatos e o limite de upload.");
    }
  }

  private void validateSize(long size) {
    if (size > maxBytes) {
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
          "Arquivo excede o limite permitido.");
    }
  }

  private String safeFilename(String original) {
    String filename = original == null ? "" : original.replace('\\', '/');
    return filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
  }

  private void validateExtension(String filename, String extension) {
    if (filename.length() > 255 || !allowedExtensions.contains(extension)) {
      throw new IllegalArgumentException("Formato de arquivo não permitido.");
    }
  }

  private byte[] read(MultipartFile file) {
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException exception) {
      throw new IllegalArgumentException("Não foi possível ler o arquivo.", exception);
    }
    return bytes;
  }

  private boolean matchesSignature(String extension, byte[] bytes) {
    return switch (extension) {
      case "webp" -> isWebp(bytes);
      case "mp4" -> isMp4(bytes);
      default -> matchesPrefix(extension, bytes);
    };
  }

  private boolean matchesPrefix(String extension, byte[] bytes) {
    int[] prefix = SIGNATURES.get(extension);
    return prefix != null && startsWith(bytes, prefix);
  }

  private boolean isWebp(byte[] bytes) {
    return bytes.length >= 12 && startsWith(bytes, 0x52, 0x49, 0x46, 0x46) && bytes[8] == 'W'
        && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
  }

  private boolean isMp4(byte[] bytes) {
    return bytes.length >= 12 && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y'
        && bytes[7] == 'p';
  }

  private boolean startsWith(byte[] bytes, int... prefix) {
    if (bytes.length < prefix.length) {
      return false;
    }
    for (int index = 0; index < prefix.length; index++) {
      if ((bytes[index] & 0xff) != prefix[index]) {
        return false;
      }
    }
    return true;
  }

  public record ValidatedUpload(String filename, String contentType, byte[] bytes) {
  }
}
