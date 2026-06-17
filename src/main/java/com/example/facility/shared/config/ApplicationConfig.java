package com.example.facility.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ApplicationConfig {
    
    @Bean
    ObjectMapper mapper() {
        return new ObjectMapper();
    }
}
