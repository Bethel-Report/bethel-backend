package com.bethelreport.backend.message.dtos;

import java.util.Map;

public record EmailMessage(String recipientEmail, String subject, String template,
    Map<String, Object> variables) {
}
