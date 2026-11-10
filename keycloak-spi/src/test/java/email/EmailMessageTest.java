package email;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmailMessageTest {

    @Test
    void serializesRenderedBodiesAndNullWithoutLosingContractFields() {
        RabbitEmailSenderProvider.EmailMessage message = new RabbitEmailSenderProvider.EmailMessage(
                "person@example.com", "Welcome", null,
                "<a href=\"https://example.test/action?token=a&b=c\">Continue</a>");

        assertEquals("{\"recipientEmail\":\"person@example.com\",\"subject\":\"Welcome\","
                + "\"textBody\":null,\"htmlBody\":\"<a href=\\\"https://example.test/action?token=a&b=c"
                + "\\\">Continue</a>\"}", message.toJson());
    }

    @Test
    void escapesJsonControlCharacters() {
        RabbitEmailSenderProvider.EmailMessage message = new RabbitEmailSenderProvider.EmailMessage(
                "person@example.com", "Line\nBreak", "Plain text", null);

        assertEquals("{\"recipientEmail\":\"person@example.com\",\"subject\":\"Line\\nBreak\","
                + "\"textBody\":\"Plain text\",\"htmlBody\":null}", message.toJson());
    }
}
