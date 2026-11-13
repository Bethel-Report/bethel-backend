package com.bethelreport.backend.message.dtos;

public record EmailMessage(String recipientEmail, String subject, String textBody,
    String htmlBody) {
}
