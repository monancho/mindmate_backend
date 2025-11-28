package com.mind_mate.home.config;


import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {

    
    public ObjectMapper objectMapper(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}