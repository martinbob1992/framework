package com.marsh.framework.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsh.framework.security.handler.ThrowAccessDeniedHandler;
import com.marsh.framework.security.handler.ThrowEntryPointHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Marsh
 * @date 2022-03-01日 14:46
 */
@Configuration
public class SpringSecurityStarterConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public ThrowAccessDeniedHandler throwAccessDeniedHandler(){
        return new ThrowAccessDeniedHandler(objectMapper);
    }

    @Bean
    public ThrowEntryPointHandler throwEntryPointHandler(){
        return new ThrowEntryPointHandler(objectMapper);
    }
}
