package com.marsh.framework.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsh.framework.security.handler.ThrowAccessDeniedHandler;
import com.marsh.framework.security.handler.ThrowEntryPointHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;

@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Slf4j
public class ResourceServiceStarterConfig extends ResourceServerConfigurerAdapter {

	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private TokenStore tokenStore;
	@Autowired
	private ThrowEntryPointHandler throwEntryPointHandler;
	@Autowired
	private ThrowAccessDeniedHandler throwAccessDeniedHandler;


	@Override
	public void configure(ResourceServerSecurityConfigurer resource) throws Exception {
		log.error("xxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		//这里把自定义异常加进去
		resource.tokenStore(tokenStore).authenticationEntryPoint(throwEntryPointHandler)
				.accessDeniedHandler(throwAccessDeniedHandler);
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();
        http.httpBasic().disable();
        http.headers().frameOptions().disable();
        http.exceptionHandling()
                .authenticationEntryPoint(throwEntryPointHandler)
                .accessDeniedHandler(throwAccessDeniedHandler);
        //配置哪些路径是允许不登录访问的
		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry expressionInterceptUrlRegistry = http
				.authorizeRequests()
				.antMatchers("/**").permitAll();
		expressionInterceptUrlRegistry.anyRequest().authenticated();

	}


}