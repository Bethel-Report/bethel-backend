# Mensageria — RabbitMQ

Documentação da infraestrutura de comunicação assíncrona entre o back-end Spring e o Worker via RabbitMQ.

## Visão geral

O back-end publica mensagens em filas do RabbitMQ. O Worker consome essas mensagens e executa as ações correspondentes (envio de e-mails, geração de relatórios, etc.).

```
Back-end  ──publish──▶  RabbitMQ  ──consume──▶  Worker
```

## Topologia (exchanges, filas e rotas)

| Recurso | Nome | Tipo | Descrição |
|---------|------|------|-----------|
| Exchange | `email.exchange` | Direct | Recebe mensagens de e-mail |
| Fila | `email.queue` | Durable | Fila principal de e-mails |
| Routing key | `email.send` | — | Liga a exchange à fila |
| DLX | `dlx.exchange` | Direct | Exchange para mensagens rejeitadas |
| DLQ | `email.dlq` | Durable | Fila de mensagens não processadas |

### Fluxo de mensagens

1. O back-end publica na exchange `email.exchange` com routing key `email.send`.
2. O RabbitMQ roteia a mensagem para `email.queue`.
3. O Worker consome de `email.queue` e processa.
4. Se o Worker rejeitar a mensagem (nack sem requeue), ela é encaminhada para `email.dlq` via `dlx.exchange`.

## Contrato de mensagem

### EmailMessage

Mensagem publicada ao criar convites e outras ações que demandam envio de e-mail.

```json
{
  "recipientEmail": "usuario@example.com",
  "subject": "Assunto do e-mail",
  "template": "invitation",
  "variables": {
    "invitationLink": "https://app.bethelreport.com/invite/abc123"
  }
}
```

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `recipientEmail` | `String` | Endereço de e-mail do destinatário |
| `subject` | `String` | Assunto do e-mail |
| `template` | `String` | Nome do template a ser utilizado pelo Worker |
| `variables` | `Map<String, Object>` | Variáveis para interpolação no template |

As mensagens são serializadas em JSON via `JacksonJsonMessageConverter`.

## Variáveis de ambiente

| Variável | Descrição | Valor padrão |
|----------|-----------|--------------|
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |
| `RABBITMQ_PORT` | Porta AMQP do RabbitMQ | `5672` |
| `RABBITMQ_USERNAME` | Usuário do RabbitMQ | `guest` |
| `RABBITMQ_PASSWORD` | Senha do RabbitMQ | `guest` |

Adicione essas variáveis no arquivo `.env` (use `.env-example` como referência).

## Configuração no Docker

O RabbitMQ está definido no `docker-compose.yaml`:

```bash
docker compose up -d rabbitmq
```

Acesse o painel de gerenciamento em `http://localhost:15672` com as credenciais definidas no `.env`.

## Execução local

1. Copie `.env-example` para `.env` e preencha as variáveis do RabbitMQ.
2. Suba o RabbitMQ:
   ```bash
   docker compose up -d rabbitmq
   ```
3. Inicie o back-end:
   ```bash
   ./mvnw spring-boot:run
   ```
4. As filas, exchanges e bindings são criados automaticamente pelo Spring AMQP na inicialização.

## Confiabilidade

### Publisher confirms

O back-end utiliza `publisher-confirm-type=correlated` para receber confirmação do broker ao publicar mensagens. Caso o broker não consiga persistir a mensagem, o Spring AMQP registra o erro.

### Retry na publicação

Configurado via `spring.rabbitmq.template.retry.*`:

| Propriedade | Valor |
|-------------|-------|
| `max-attempts` | 3 |
| `initial-interval` | 1000ms |
| `multiplier` | 2.0 |
| `max-interval` | 10000ms |

Se a publicação falhar (conexão indisponível, por exemplo), o Spring tentará novamente até 3 vezes com backoff exponencial.

### Dead Letter Queue (DLQ)

Mensagens rejeitadas pelo Worker (nack sem requeue) são automaticamente encaminhadas para `email.dlq`. Isso permite:

- Inspeção manual de mensagens com falha.
- Reprocessamento posterior movendo mensagens de volta para `email.queue`.

### Reprocessamento

Para reprocessar mensagens da DLQ, utilize o painel de gerenciamento do RabbitMQ (`http://localhost:15672`):

1. Acesse a fila `email.dlq`.
2. Utilize a funcionalidade "Move messages" para mover as mensagens de volta para `email.queue`.

Alternativamente, via CLI:

```bash
docker exec backend-rabbitmq rabbitmq-plugins enable rabbitmq_shovel rabbitmq_shovel_management
```

E configure um shovel temporário via API para mover mensagens da DLQ para a fila principal.

## Comunicação com o Worker

O Worker deve:

1. Conectar ao mesmo broker RabbitMQ (mesmas variáveis de ambiente).
2. Declarar um listener para a fila `email.queue`.
3. Deserializar as mensagens com `JacksonJsonMessageConverter`.
4. Confirmar o processamento com acknowledge (ack) ou rejeitar com nack.

Exemplo de listener no Worker (Spring AMQP):

```java
@RabbitListener(queues = "email.queue")
public void handleEmail(EmailMessage message) {
    // Processar e enviar o e-mail
}
```

O contrato `EmailMessage` deve ser replicado no Worker com os mesmos campos.

## Testes

Os testes de integração utilizam Testcontainers com uma instância RabbitMQ efêmera:

```bash
./mvnw test -Dtest=RabbitMqIntegrationTest
```

Os testes validam:
- Declaração correta da topologia (filas e DLQ).
- Publicação e recebimento de mensagens em formato JSON.
