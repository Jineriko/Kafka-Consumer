package com.example.dataflow.services;

import com.example.dataflow.repositories.CommonDto;
import com.example.dataflow.repositories.User;
import com.example.dataflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void saveUser(CommonDto dto) {
        User entity = User.builder()
                .id(dto.getUserId())
                .firstName(dto.getName())
                .lastName(dto.getLastname())
                .createdAt(LocalDateTime.now())
                .operationUuid(dto.getOperatorId())
                .build();

        userRepository.save(entity);
    }
}