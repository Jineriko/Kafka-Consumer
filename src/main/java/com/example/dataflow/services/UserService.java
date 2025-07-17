package com.example.dataflow.services;

import com.example.dataflow.repositories.SimpleUserDto;
import com.example.dataflow.repositories.User;
import com.example.dataflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void saveUser(SimpleUserDto dto, UUID operationUuid) {
        User entity = User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .createdAt(LocalDateTime.now())
                .operationUuid(operationUuid)
                .build();

        userRepository.save(entity);
    }
}