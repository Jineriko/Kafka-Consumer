package com.example.dataflow.services;

import com.example.dataflow.repositories.OperationLog;
import com.example.dataflow.repositories.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository repository;

    public void log(UUID operationUuid, String eventType, String message) {
        OperationLog log = OperationLog.builder()
                .operationUuid(operationUuid)
                .eventType(eventType)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(log);
    }
}