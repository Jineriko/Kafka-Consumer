package com.example.dataflow.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.openapitools.jackson.nullable.JsonNullableModule;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Поддержка LocalDate, LocalDateTime
        mapper.registerModule(new JavaTimeModule());

        // Поддержка JsonNullable
        mapper.registerModule(new JsonNullableModule());

        // Не сериализовать даты как timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}