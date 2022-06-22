package com.marsh.framework.log.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class LogContext implements Serializable {

    /**
     * 日志调用id
     */
    private String traceId;

    public LogContext(String traceId){
        Assert.notNull(traceId,"日志链路追踪id不能为空!");
        this.traceId = traceId;
    }

    public static LogContext createContext() {
        return createContext(UUID.randomUUID().toString().replace("-",""));
    }

    public static LogContext createContext(String traceId){
        return new LogContext(traceId);
    }
}
