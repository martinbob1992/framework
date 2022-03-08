package com.marsh.auth.authentication.passwd.filter;

import com.marsh.auth.constant.AuthenticationEndpointEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Marsh
 * @date 2022-02-23日 17:41
 */
@Slf4j
public class UsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String SPRING_SECURITY_FORM_USERNAME_KEY = "username";
    public static final String SPRING_SECURITY_FORM_PASSWORD_KEY = "password";

    private AuthenticationEntryPoint authenticationEntryPoint;

    public UsernamePasswordAuthenticationFilter() {
        super(new AntPathRequestMatcher(AuthenticationEndpointEnum.USER_PASSWD_TOKEN_URL.getEndpointUrl(), "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        String username = obtainUsername(request);
        String password = obtainPassword(request);

        if (username == null) {
            username = "";
        }

        if (password == null) {
            password = "";
        }

        username = username.trim();
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username,password);

        // Allow subclasses to set the "details" property
        setDetails(request, token);
        try {
            return this.getAuthenticationManager().authenticate(token);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            if (log.isDebugEnabled()){
                log.debug("商户端账号密码流程认证失败!",e);
            }
            try {
                authenticationEntryPoint.commence(request, response,
                        new BadCredentialsException(e.getMessage(), e));
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        }
        return null;
    }

    protected String obtainUsername(HttpServletRequest request) {
        return request.getParameter(SPRING_SECURITY_FORM_USERNAME_KEY);
    }

    protected String obtainPassword(HttpServletRequest request) {
        return request.getParameter(SPRING_SECURITY_FORM_PASSWORD_KEY);
    }

    protected void setDetails(HttpServletRequest request,
                              UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }

    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

    public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }
}
