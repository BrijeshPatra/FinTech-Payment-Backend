package com.paypal.transaction_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
    /*
    This app config class gives a Bean which helps to call rest microservices internally
     */
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
