package com.marsh.auth.authentication.passwd.config;

import com.marsh.auth.authentication.passwd.filter.UsernamePasswordAuthenticationFilter;
import com.marsh.auth.authentication.passwd.provider.UsernamePasswordAuthenticationProvider;
import com.marsh.auth.service.MyUserDetailService;
import com.marsh.framework.security.handler.ThrowEntryPointHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.stereotype.Component;

/**
 * @author Marsh
 * @date 2022-02-23日 17:34
 */
@Component
public class UsernamePasswordSecurityConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    @Autowired
    private AuthenticationSuccessHandler authenticationSuccessHandler;
    @Autowired(required = false)
    private MyUserDetailService userDetailService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ThrowEntryPointHandler throwEntryPointHandler;

    @Override
    public void configure(HttpSecurity http) {
        UsernamePasswordAuthenticationFilter filter = new UsernamePasswordAuthenticationFilter();
        filter.setAuthenticationManager(http.getSharedObject(AuthenticationManager.class));
        filter.setAuthenticationEntryPoint(throwEntryPointHandler);
        filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);

        UsernamePasswordAuthenticationProvider provider = new UsernamePasswordAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsService(userDetailService);

        http.authenticationProvider(provider)
                .addFilterAfter(filter, LogoutFilter.class);
    }
}
