package com.marsh.framework.security.handler;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsh.common.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

/**
 * 登陆用户访问了不属于自己的资源发生的权限不足处理器
 * 
 * @author marsh
 * @date 2018-10-23
 */
@Slf4j
public class ThrowAccessDeniedHandler implements AccessDeniedHandler {

    private ObjectMapper objectMapper;

    public ThrowAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String uri = request.getRequestURI();
        log.error("Exception: status[{}], code[{}], uri[{}], message[{}], error[{}]", HttpServletResponse.SC_FORBIDDEN, "access_denied", StrUtil.isNotBlank(uri) ? uri : StrUtil.EMPTY,
                "您没有访问该资源的权限!", accessDeniedException);
        if (objectMapper == null) {
            throw accessDeniedException;
        } else {
            R<Serializable> error = R.error(HttpServletResponse.SC_FORBIDDEN, "您没有访问该资源的权限!");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }

}