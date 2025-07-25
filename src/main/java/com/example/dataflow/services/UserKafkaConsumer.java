package com.example.dataflow.services;

import com.example.dataflow.repositories.CommonDto;
import com.example.dataflow.repositories.User;
import com.example.dataflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserKafkaConsumer {

    private final KafkaTemplate<String, CommonDto> userDtoKafkaTemplate;
    private final OperationLogService logService;
    private final UserService userService;
    private final UserRepository userRepository;

    @KafkaListener(topics = "user.dto.request", groupId = "user-dto-group", containerFactory = "userDtoKafkaListenerFactory")
    public void consume(CommonDto dto) {

        logService.log(dto.getOperatorId(), "RECEIVED", "Получены данные пользователя: " + dto.getName() + " " + dto.getLastname());
        log.info("Получены данные пользователя: " + dto.getName() + " " + dto.getLastname());

        // Сохраняем в БД
        userService.saveUser(dto);

        log.info("Пользователь {} {} ", dto.getName(), dto.getLastname() + " успешно сохранен в базе");

        // Отправляем ответ
        userDtoKafkaTemplate.send("user.dto.response", dto)
                .addCallback(
                        result -> {
                            log.info("Данные пользователя отправлены обратно: {} {}", dto.getName(), dto.getLastname());
                            logService.log(dto.getOperatorId(), "SEND_SUCCESS", "Отправка обратно прошла успешно");
                        },
                        ex -> {
                            log.error("Ошибка при отправке: ", ex);
                            logService.log(dto.getOperatorId(), "SEND_FAILURE", ex.getMessage());
                        }
                );

    }

    @SneakyThrows
    @KafkaListener(topics = "user.dto.update", groupId = "user-dto-group", containerFactory = "userDtoKafkaListenerFactory")
    public void updateUser(CommonDto dto) {

        logService.log(dto.getOperatorId(), "UPDATE_RECEIVED", "Получен запрос на обновление: " + dto);

        Optional<User> userOpt = userRepository.findByOperationUuid(dto.getOperatorId());
        if (userOpt.isEmpty()) {
            logService.log(dto.getOperatorId(), "UPDATE_NOT_FOUND", "Пользователь с UUID не найден");
            return;
        }

        User user = userOpt.get();

        if (new Random().nextBoolean()) {
            user.setLastName(dto.getLastname());

            log.info("Пользователь успешно обновлен, сохраняем пользоватя в базу");
            logService.log(dto.getOperatorId(), "UPDATE_SUCCESS", "Обновление пользователя прошло успешно: " + dto);
            userRepository.save(user);

            userDtoKafkaTemplate.send("user.dto.positiveResponse", dto)
                    .addCallback(
                            result -> {
                                log.info("Данные пользователя отправлены обратно: {} {}", dto.getName(), dto.getLastname());
                                logService.log(dto.getOperatorId(), "SEND_SUCCESS", "Отправка обратно прошла успешно");
                            },
                            ex -> {
                                log.error("Ошибка при отправке: ", ex);
                                logService.log(dto.getOperatorId(), "SEND_FAILURE", ex.getMessage());
                            }
                    );
        } else {
            log.info("Ошибка во время обновления пользователя, пользователь будет удален из базы");
            logService.log(dto.getOperatorId(), "UPDATE_ERROR", "Ошибка во время обновления пользователя");

            log.info("Пользователь " + user.getFirstName() + " " + user.getLastName() + " удален");
            userRepository.delete(user);

            userDtoKafkaTemplate.send("user.dto.negativeResponse", dto)
                    .addCallback(
                            result -> {
                                log.info("Отправлено уведомление об откате операции");
                                logService.log(dto.getOperatorId(), "SEND_SUCCESS", "Отправка обратно прошла успешно");
                            },
                            ex -> {
                                log.error("Ошибка при отправке: ", ex);
                                logService.log(dto.getOperatorId(), "SEND_FAILURE", ex.getMessage());
                            }
                    );
        }
    }
}