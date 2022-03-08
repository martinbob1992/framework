package com.marsh.auth.config;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsh.auth.authentication.passwd.config.UsernamePasswordSecurityConfigurer;
import com.marsh.auth.constant.AuthenticationEndpointEnum;
import com.marsh.framework.security.handler.ThrowAccessDeniedHandler;
import com.marsh.framework.security.handler.ThrowEntryPointHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;

import java.util.List;

@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Slf4j
public class ResourceServiceConfig extends ResourceServerConfigurerAdapter {

	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private UsernamePasswordSecurityConfigurer usernamePasswordSecurityConfigurer;
	@Autowired
	private TokenStore tokenStore;
	@Autowired
	private ThrowEntryPointHandler throwEntryPointHandler;
	@Autowired
	private ThrowAccessDeniedHandler throwAccessDeniedHandler;

	@Override
	public void configure(ResourceServerSecurityConfigurer resource) throws Exception {
		log.info("ooooooooooooooooooooooooo");
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
				.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.antMatchers("/v2/api-docs").permitAll()
				.antMatchers("/favicon.ico").permitAll()
				.antMatchers("/webjars/**").permitAll()
				.antMatchers("/i18n/**").permitAll()
				.antMatchers("/swagger-resources/**").permitAll()
				.antMatchers("/swagger-ui.html").permitAll()
				.antMatchers("/auth/**").permitAll()
				.antMatchers("/js/**").permitAll()
				.antMatchers("/actuator/**").permitAll()
				.antMatchers("/login").permitAll()
				.antMatchers("/druid/**").permitAll()
				//微信消息通知
				.antMatchers("/v1/notify/receive_ticket/**").permitAll();
		//配置登录点不需要登录即可访问
		List<String> allEndpointUrls = AuthenticationEndpointEnum.getAllEndpointUrls();
		if (CollectionUtil.isNotEmpty(allEndpointUrls)){
			for (String url : allEndpointUrls){
				expressionInterceptUrlRegistry.antMatchers(url).permitAll();
			}
		}
		expressionInterceptUrlRegistry.anyRequest().authenticated();
        http.apply(usernamePasswordSecurityConfigurer);
	}


}