package email;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.UserModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeoutException;

public class RabbitEmailSenderProvider implements EmailSenderProvider {
    private static final String HOST = "RABBITMQ_HOST";
    private static final String PORT = "RABBITMQ_PORT";
    private static final String USER = "RABBITMQ_USERNAME";
    private static final String PASSWORD = "RABBITMQ_PASSWORD";
    private static final String VHOST = "RABBITMQ_VHOST";
    private static final String EXCHANGE = "RABBITMQ_EMAIL_EXCHANGE";
    private static final String ROUTING_KEY = "RABBITMQ_EMAIL_ROUTING_KEY";
    private static final int CONFIRM_TIMEOUT_MILLIS = 5_000;

    @Override
    public void send(Map<String, String> config, UserModel user, String subject,
                     String textBody, String htmlBody) throws EmailException {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            throw new EmailException("Cannot publish email without a recipient address");
        }
        send(config, user.getEmail(), subject, textBody, htmlBody);
    }

    @Override
    public void send(Map<String, String> config, String address, String subject,
                     String textBody, String htmlBody) throws EmailException {
        if (address == null || address.isBlank()) {
            throw new EmailException("Cannot publish email without a recipient address");
        }

        RabbitSettings settings;
        try {
            settings = RabbitSettings.fromEnvironment();
        } catch (IllegalArgumentException e) {
            throw new EmailException("RabbitMQ email publisher is not configured", e);
        }

        byte[] body = new EmailMessage(address, subject, textBody, htmlBody)
                .toJson().getBytes(StandardCharsets.UTF_8);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(settings.host());
        factory.setPort(settings.port());
        factory.setUsername(settings.username());
        factory.setPassword(settings.password());
        factory.setVirtualHost(settings.virtualHost());
        factory.setConnectionTimeout(CONFIRM_TIMEOUT_MILLIS);

        try (Connection connection = factory.newConnection("keycloak-email-sender");
             Channel channel = connection.createChannel()) {
            channel.confirmSelect();
            AtomicBoolean returned = new AtomicBoolean(false);
            channel.addReturnListener((replyCode, replyText, exchange, routingKey,
                                       properties, returnedBody) -> returned.set(true));
            channel.basicPublish(settings.exchange(), settings.routingKey(), true,
                    MessageProperties.PERSISTENT_BASIC.builder()
                            .contentType("application/json")
                            .contentEncoding(StandardCharsets.UTF_8.name())
                            .build(), body);
            if (!channel.waitForConfirms(CONFIRM_TIMEOUT_MILLIS)) {
                throw new EmailException("RabbitMQ did not confirm email publication");
            }
            if (returned.get()) {
                throw new EmailException("RabbitMQ could not route email to a queue");
            }
        } catch (IOException | TimeoutException e) {
            throw new EmailException("Failed to publish email to RabbitMQ", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmailException("Interrupted while waiting for RabbitMQ publisher confirm", e);
        }
    }

    @Override
    public void validate(Map<String, String> config) throws EmailException {
        try {
            RabbitSettings.fromEnvironment();
        } catch (IllegalArgumentException e) {
            throw new EmailException("RabbitMQ email publisher is not configured", e);
        }
    }

    @Override
    public void close() {
        // Connections and channels are scoped to each publication.
    }

    private record RabbitSettings(String host, int port, String username, String password,
                                  String virtualHost, String exchange, String routingKey) {
        private static RabbitSettings fromEnvironment() {
            String host = required(HOST);
            String username = required(USER);
            String password = required(PASSWORD);
            String virtualHost = required(VHOST);
            String exchange = required(EXCHANGE);
            String routingKey = required(ROUTING_KEY);
            int port;
            try {
                port = Integer.parseInt(System.getenv().getOrDefault(PORT, "5672"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(PORT + " must be a valid port number", e);
            }
            if (port < 1 || port > 65_535) {
                throw new IllegalArgumentException(PORT + " must be between 1 and 65535");
            }
            return new RabbitSettings(host, port, username, password,
                    virtualHost, exchange, routingKey);
        }

        private static String required(String name) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing required environment variable " + name);
            }
            return value;
        }
    }

    /** Contract owned by message-broker: recipientEmail, subject, textBody, htmlBody. */
    static record EmailMessage(String recipientEmail, String subject, String textBody, String htmlBody) {
        String toJson() {
            return "{\"recipientEmail\":" + jsonString(recipientEmail)
                    + ",\"subject\":" + jsonString(subject)
                    + ",\"textBody\":" + jsonString(textBody)
                    + ",\"htmlBody\":" + jsonString(htmlBody) + "}";
        }

        private static String jsonString(String value) {
            if (value == null) {
                return "null";
            }
            StringBuilder json = new StringBuilder(value.length() + 2).append('\"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '\"' -> json.append("\\\"");
                    case '\\' -> json.append("\\\\");
                    case '\b' -> json.append("\\b");
                    case '\f' -> json.append("\\f");
                    case '\n' -> json.append("\\n");
                    case '\r' -> json.append("\\r");
                    case '\t' -> json.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            json.append(String.format("\\u%04x", (int) c));
                        } else {
                            json.append(c);
                        }
                    }
                }
            }
            return json.append('\"').toString();
        }
    }
}
