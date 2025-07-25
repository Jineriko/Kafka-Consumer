package com.example.dataflow.repositories;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonDto {
    private UUID userId;
    private String name;
    private String lastname;
    private UUID operatorId;
}