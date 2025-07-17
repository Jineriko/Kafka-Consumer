package com.example.dataflow.services;

import com.example.dataflow.repositories.SimpleUserDto;
import com.example.dataflow.repositories.User;
import com.example.dataflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserKafkaConsumer {

    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final OperationLogService logService;
    private final UserService userService;
    private final UserRepository userRepository;

    @KafkaListener(topics = "user.dto.request", groupId = "user-dto-group", containerFactory = "userDtoKafkaListenerFactory")
    public void consume(SimpleUserDto dto, @Header(name = "operationUuid", required = false) byte[] operationUuidRaw) {

        UUID operationUuid = operationUuidRaw != null
                ? UUID.fromString(new String(operationUuidRaw, StandardCharsets.UTF_8))
                : UUID.randomUUID();

        logService.log(operationUuid, "RECEIVED", "Получены данные пользователя: " + dto.getFirstName() + " " + dto.getLastName());

        try {
            log.info("Сохраняем пользователя в базу: {} {}", dto.getFirstName(), dto.getLastName());

            // Сохраняем в БД
            userService.saveUser(dto, operationUuid);

            // Отправляем ответ
            String response = "Пользователь %s %s сохранён в базе".formatted(dto.getFirstName(), dto.getLastName());
            stringKafkaTemplate.send("user.dto.response", response);

            logService.log(UUID.randomUUID(), "RESPONSE_SENT", "Ответ отправлен: " + response);

        } catch (Exception e) {
            log.error("Ошибка при обработке", e);
            logService.log(UUID.randomUUID(), "HANDLE_FAILED", e.getMessage());
        }
    }

    @KafkaListener(topics = "user.dto.update", groupId = "user-dto-group", containerFactory = "userDtoKafkaListenerFactory")
    public void updateUser(SimpleUserDto dto,
                           @Header(name = "operationUuid", required = false) byte[] operationUuidRaw) {

        UUID operationUuid = operationUuidRaw != null
                ? UUID.fromString(new String(operationUuidRaw, StandardCharsets.UTF_8))
                : UUID.randomUUID();

        logService.log(operationUuid, "UPDATE_RECEIVED", "Получен запрос на обновление: " + dto);

        try {
            Optional<User> userOpt = userRepository.findByOperationUuid(operationUuid);
            if (userOpt.isEmpty()) {
                logService.log(operationUuid, "UPDATE_NOT_FOUND", "Пользователь с UUID не найден");
                return;
            }

            User user = userOpt.get();

            // Частичное обновление
            if (dto.getFirstName() != null) {
                user.setFirstName(dto.getFirstName());
            }
            if (dto.getLastName() != null) {
                user.setLastName(dto.getLastName());
            }

            log.info("Сохраняем пользователя в базу: {} {}", user.getFirstName(), user.getLastName());
            userRepository.save(user);

            String response = "Пользователь %s %s обновлен в базе".formatted(user.getFirstName(), user.getLastName());
            stringKafkaTemplate.send("user.dto.response", response);

            logService.log(operationUuid, "UPDATED", "Пользователь обновлён: " + response);

        } catch (Exception e) {
            logService.log(operationUuid, "UPDATE_FAILED", "Ошибка: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "user.saga.request", groupId = "user-saga-group", containerFactory = "userDtoKafkaListenerFactory")
    public void handleSaga(SimpleUserDto dto, @Header(name = "operationUuid", required = false) byte[] operationUuidRaw) {
        UUID operationUuid = operationUuidRaw != null
                ? UUID.fromString(new String(operationUuidRaw, StandardCharsets.UTF_8))
                : UUID.randomUUID();

        logService.log(operationUuid, "SAGA_STARTED", "Получены данные пользователя: " + dto.getFirstName() + " " + dto.getLastName());

        try {
            // 1. Сохраняем пользователя в БД
            log.info("Сохраняем пользователя в базу: {} {}", dto.getFirstName(), dto.getLastName());
            userService.saveUser(dto, operationUuid);

            logService.log(operationUuid, "USER_CREATED", "Пользователь создан: " + dto.getFirstName() + " " + dto.getLastName());

            Optional<User> userOpt = userRepository.findByOperationUuid(operationUuid);
            User user = userOpt.orElse(null);

            // 2. Решается судьба пользователя (1 --> обновление проходит, 0 --> откат всей транзакции)
            int decision = new Random().nextInt(2);
            Thread.sleep(5000);
            if (decision == 1) {

                // Успешный случай — обновляем пользователя
                user.setLastName("Успех");
                userRepository.save(user);

                log.info("Обновление прошло успешно, пользователь сохранен в базу с новыми данными: " + user.getFirstName() + " " + user.getLastName());
                logService.log(operationUuid, "USER_UPDATED", "Фамилия обновлена");
                stringKafkaTemplate.send("user.saga.response", "Пользователь успешно создан и обновлён");
                logService.log(operationUuid, "SAGA_COMPLETED", "Saga завершена успешно");
            } else {

                // Ошибочный случай — компенсация
                log.info("Ошибка во время обновления пользователя, отмена транзакции. Пользователь " + user.getFirstName() + " " + user.getLastName() + " удален из базы");
                userRepository.deleteById(user.getId());

                logService.log(operationUuid, "COMPENSATION_DONE", "Пользователь удалён");
                stringKafkaTemplate.send("user.saga.response", "Saga откатилась. Пользователь не создан.");
                logService.log(operationUuid, "SAGA_ROLLED_BACK", "Saga завершена с откатом");
            }

        } catch (Exception e) {
            log.error("Ошибка во время выполнения Saga", e);
            logService.log(operationUuid, "SAGA_FAILED", e.getMessage());
            stringKafkaTemplate.send("user.saga.response", "Ошибка выполнения операции: " + e.getMessage());
        }
    }
}