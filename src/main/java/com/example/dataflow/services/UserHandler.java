package com.example.dataflow.services;

import com.example.dataflow.repositories.CommonDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserHandler {
    private final UserCreateHandler userCreateHandler;
    private final UserUpdateHandler userUpdateHandler;

    @KafkaListener(topics = "user.dto.request", groupId = "user-dto-group", containerFactory = "userDtoKafkaListenerFactory")
    public void consume(CommonDto dto) {
        userCreateHandler.handle(dto);
    }

    @KafkaListener(topics = "user.dto.update", groupId = "user-dto-group", containerFactory = "userDtoKafkaListenerFactory")
    public void updateUser(CommonDto dto) {
        userUpdateHandler.handle(dto);
    }
}
