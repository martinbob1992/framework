package com.marsh.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsh.auth.handler.LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * @author marsh
 * @date 2020/6/9 16:09
 */
@Configuration
@ComponentScan("com.penglai.framework.starter.security.**")
public class SecurityStarterConfig {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthorizationServerTokenServices authorizationServerTokenServices;
    @Autowired
    private ClientDetailsService clientDetailsService;

    /**
     * 密码加密策略
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        LoginSuccessHandler login = new LoginSuccessHandler();
        login.setAuthorizationServerTokenServices(authorizationServerTokenServices);
        login.setObjectMapper(objectMapper);
        login.setClientDetailsService(clientDetailsService);
        return login;
    }
}
