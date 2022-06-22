package com.marsh.framework.log.context.strategy;

import com.marsh.framework.log.context.LogContext;
import org.springframework.util.Assert;

/**
 * @author marsh
 */
public class ThreadLocalLogContextHolderStrategy implements ILogContextHolderStrategy {

    private static final ThreadLocal<LogContext> contextHolder = new ThreadLocal<>();

    @Override
    public void clearContext() {
        contextHolder.remove();
    }

    @Override
    public LogContext getContext() {
        return contextHolder.get();
    }

    @Override
    public LogContext getContextIfAbsentCreate() {
        LogContext context = getContext();
        if (context != null){
            return context;
        }
        LogContext newContext = LogContext.createContext();
        setContext(newContext);
        return newContext;
    }

    @Override
    public boolean hasContext() {
        return null != contextHolder.get();
    }

    @Override
    public LogContext putIfAbsent(LogContext context) {
        LogContext odlContext = getContext();
        if (odlContext != null){
            return odlContext;
        }
        setContext(context);
        return null;
    }

    private void setContext(LogContext context) {
        Assert.notNull(context, "日志上下文LogContext不能为空!");
        contextHolder.set(context);
    }

}
