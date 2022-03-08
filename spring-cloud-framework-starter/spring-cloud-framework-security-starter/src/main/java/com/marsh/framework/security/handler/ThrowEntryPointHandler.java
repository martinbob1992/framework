package com.marsh.framework.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsh.common.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;


@Slf4j
public class ThrowEntryPointHandler implements AuthenticationEntryPoint {

    private ObjectMapper objectMapper;

    public ThrowEntryPointHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException exception) throws IOException, ServletException {
        if (objectMapper == null) {
            throw exception;
        } else {
            R<Serializable> error;
            if(exception.getCause() instanceof InvalidTokenException) {
                error = R.error(HttpServletResponse.SC_UNAUTHORIZED,"登陆信息已失效");
            } else if (exception.getCause() instanceof InsufficientAuthenticationException) {
                error = R.error(HttpServletResponse.SC_UNAUTHORIZED,"请登录后再访问该资源");
            } else {
                error = R.error(HttpServletResponse.SC_UNAUTHORIZED,exception.getMessage());
            }
        	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }
}
