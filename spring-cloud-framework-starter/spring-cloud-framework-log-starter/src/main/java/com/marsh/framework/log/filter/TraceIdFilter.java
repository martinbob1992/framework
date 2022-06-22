package com.marsh.framework.log.filter;

import cn.hutool.core.util.StrUtil;
import com.marsh.framework.log.context.LogContext;
import com.marsh.framework.log.context.LogContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 * @author Marsh
 * @date 2022-06-17
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Long startTime = System.currentTimeMillis();
        String traceId = request.getHeader("trace-id");
        LogContext context;
        if (StrUtil.isBlank(traceId)){
            context = LogContext.createContext(traceId);
            LogContext oldContext = LogContextHolder.putIfAbsent(context);
            if (oldContext != null){
                context = oldContext;
            }
        } else {
            context = LogContextHolder.getContextIfAbsentCreate();
        }
        response.addHeader("trace-id", context.getTraceId());
        log.debug("添加traceId = {}",context.getTraceId());
        filterChain.doFilter(request,response);
        log.debug("清除traceId = {}",context.getTraceId());
        LogContextHolder.clearContext();
        response.addHeader("time", System.currentTimeMillis() - startTime + "");
    }
}
