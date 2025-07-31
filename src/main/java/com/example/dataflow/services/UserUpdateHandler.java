package com.example.dataflow.services;

import com.example.dataflow.repositories.CommonDto;
import com.example.dataflow.repositories.User;
import com.example.dataflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserUpdateHandler {

    private final UserRepository userRepository;
    private final OperationLogService logService;
    private final KafkaResponseSender kafkaResponseSender;

    public void handle(CommonDto dto) {
        logService.log(dto.getOperatorId(), "UPDATE_RECEIVED", "Получен запрос на обновление: " + dto);

        Optional<User> userOpt = userRepository.findByOperationUuid(dto.getOperatorId());
        if (userOpt.isEmpty()) {
            logService.log(dto.getOperatorId(), "UPDATE_NOT_FOUND", "Пользователь с UUID не найден");
            return;
        }

        User user = userOpt.get();

        if (new Random().nextBoolean()) {
            user.setLastName(dto.getLastname());
            userRepository.save(user);
            logService.log(dto.getOperatorId(), "UPDATE_SUCCESS", "Обновление пользователя прошло успешно: " + dto);

            kafkaResponseSender.sendResponse("user.dto.positiveResponse", dto, dto.getOperatorId());
        } else {
            userRepository.delete(user);
            logService.log(dto.getOperatorId(), "UPDATE_ERROR", "Ошибка во время обновления пользователя. Пользователь удалён.");

            kafkaResponseSender.sendResponse("user.dto.negativeResponse", dto, dto.getOperatorId());
        }
    }
}