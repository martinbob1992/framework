package com.marsh.framework.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Marsh
 * @date 2022-06-20日 11:40
 */
@Data
@ConfigurationProperties(LogProperties.LOG_PROPERTIES_PREFIX)
public class LogProperties {

    public final static String LOG_PROPERTIES_PREFIX = "framework.log";

    /**
     * 配置Controller Aop拦截规则
     */
    private String rule;
}
