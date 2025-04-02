package com.VMTecnologia.userService.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests().anyRequest().permitAll();
        return http.build();
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return mock(PlatformTransactionManager.class);
    }

    @Bean
    public TransactionTemplate transactionTemplate() {
        return mock(TransactionTemplate.class);
    }

}