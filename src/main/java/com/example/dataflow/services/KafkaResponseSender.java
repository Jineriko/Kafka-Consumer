package com.example.dataflow.services;

import com.example.dataflow.repositories.CommonDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaResponseSender {

    private final KafkaTemplate<String, CommonDto> kafkaTemplate;
    private final OperationLogService logService;

    public void sendResponse(String topic, CommonDto dto, UUID operationUuid) {
        kafkaTemplate.send(topic, dto)
                .addCallback(
                        result -> {
                            log.info("Данные отправлены в топик {}: {} {}", topic, dto.getName(), dto.getLastname());
                            logService.log(operationUuid, "SEND_SUCCESS", "Отправка прошла успешно");
                        },
                        ex -> {
                            log.error("Ошибка при отправке в Kafka", ex);
                            logService.log(operationUuid, "SEND_FAILURE", ex.getMessage());
                        }
                );
    }
}