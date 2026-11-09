# Messaging

The `message-broker` project owns RabbitMQ exchanges, queues, bindings, dead-letter routing, and the email message contract. Backend declares no production queue topology. It publishes the fully rendered `EmailMessage` using the exchange and routing key configured by `RABBITMQ_EMAIL_EXCHANGE` and `RABBITMQ_EMAIL_ROUTING_KEY`.

The shared contract is documented in [`message-broker/contracts/email-message.schema.json`](../../message-broker/contracts/email-message.schema.json). Email payloads contain `recipientEmail`, `subject`, and nullable `textBody` / `htmlBody`. Keycloak-provided content must be forwarded as rendered; consumers must not rebuild links or render Keycloak templates.

For the integrated local setup, start RabbitMQ from `message-broker` first, then start Backend and Worker on the shared `net1` Docker network. The same RabbitMQ username, password, and virtual host must be configured in the broker, backend, and worker environment files. See the broker [run instructions](../../message-broker/README.md).

Publisher confirms and mandatory routing are enabled. The backend waits for the confirm and fails publication on a negative confirm, timeout, or unroutable return. Never log message bodies, tokens, or action links.
