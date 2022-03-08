package com.marsh.auth.config;

import com.marsh.framework.security.handler.ThrowAccessDeniedHandler;
import com.marsh.framework.security.handler.ThrowEntryPointHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * 主要用来配置哪些路径被拦截，哪些路径放过，并且配置拦截成功和失败处理
 */
@Configuration
public class AuthorizationSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private ThrowEntryPointHandler throwEntryPointHandler;
	@Autowired
	private ThrowAccessDeniedHandler throwAccessDeniedHandler;

	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.exceptionHandling()
				.authenticationEntryPoint(throwEntryPointHandler)
				.accessDeniedHandler(throwAccessDeniedHandler);
		http
				.authorizeRequests()
				.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.anyRequest().authenticated()
				.and().formLogin()
				.and().csrf()
				.disable().httpBasic();
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/favor.ioc");
	}
	
}
