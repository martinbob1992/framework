package com.marsh.framework.log.context.strategy;

import cn.hutool.core.util.StrUtil;
import com.marsh.framework.log.context.LogContext;
import org.slf4j.MDC;
import org.springframework.util.Assert;

/**
 * @author marsh
 */
public class MDCLogContextHolderStrategy implements ILogContextHolderStrategy {

    public static final String TRACE_KEY = "traceId";

    @Override
    public void clearContext() {
        MDC.clear();
    }

    @Override
    public LogContext getContext() {
        String traceId = MDC.get(TRACE_KEY);
        if (StrUtil.isNotBlank(traceId)){
            return new LogContext(traceId);
        } else{
            return null;
        }
    }

    @Override
    public LogContext getContextIfAbsentCreate(){
        LogContext context = getContext();
        if (context != null){
            return context;
        }
        LogContext newContext = LogContext.createContext();
        setLogContext(newContext);
        return newContext;
    }

    @Override
    public boolean hasContext() {
        return StrUtil.isNotBlank(MDC.get(TRACE_KEY));
    }


    @Override
    public LogContext putIfAbsent(LogContext context){
        Assert.notNull(context, "日志上下文LogContext不能为空!");
        LogContext oldContext = getContext();
        if (oldContext != null){
            return oldContext;
        }
        setLogContext(context);
        return null;
    }

    private void setLogContext(LogContext context){
        MDC.put(TRACE_KEY,context.getTraceId());
    }
}
