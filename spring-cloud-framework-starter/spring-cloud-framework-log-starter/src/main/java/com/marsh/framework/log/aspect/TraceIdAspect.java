package com.marsh.framework.log.aspect;

import com.marsh.framework.log.annotation.ThreadTraceId;
import com.marsh.framework.log.context.LogContextHolder;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 *
 * @author Marsh
 * @date 2022-06-17
 */
@Aspect
@Component
public class TraceIdAspect implements Ordered {

    @Pointcut("@annotation(trace)")
    public void traceAop(ThreadTraceId trace) {
    }

    @Before("traceAop(trace)")
    public void addTraceId(ThreadTraceId trace){
        LogContextHolder.getContextIfAbsentCreate();
    }

    @After("traceAop(trace)")
    public void removeTraceId(ThreadTraceId trace){
        LogContextHolder.clearContext();
    }

    @Override
    public int getOrder() {
        return -20;
    }
}
