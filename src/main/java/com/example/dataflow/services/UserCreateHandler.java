package com.example.dataflow.services;

import com.example.dataflow.repositories.CommonDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreateHandler {

    private final UserService userService;
    private final OperationLogService logService;
    private final KafkaResponseSender kafkaResponseSender;

    public void handle(CommonDto dto) {
        logService.log(dto.getOperatorId(), "RECEIVED", "Получены данные пользователя: " + dto.getName() + " " + dto.getLastname());
        log.info("Получены данные пользователя: {} {}", dto.getName(), dto.getLastname());

        userService.saveUser(dto);
        log.info("Пользователь {} {} успешно сохранен в базе", dto.getName(), dto.getLastname());

        kafkaResponseSender.sendResponse("user.dto.response", dto, dto.getOperatorId());
    }
}