package com.bethelreport.backend.reportgeneration.message.consumers;

import com.bethelreport.backend.reportgeneration.entities.GenerationStatus;
import com.bethelreport.backend.reportgeneration.message.dtos.GenerationResultMessage;
import com.bethelreport.backend.reportgeneration.services.GenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GenerationResultConsumer {

  private final GenerationService generationService;

  public GenerationResultConsumer(GenerationService generationService) {
    this.generationService = generationService;
  }

  @RabbitListener(queues = "${app.messaging.report.result-queue}")
  public void consume(GenerationResultMessage message) {
    log.info("Resultado recebido para solicitação {}: {}", message.generationRequestId(),
        message.status());
    GenerationStatus status = GenerationStatus.valueOf(message.status());
    generationService.updateStatus(message.generationRequestId(), status, message.storageKey(),
        message.failureReason());
  }
}
