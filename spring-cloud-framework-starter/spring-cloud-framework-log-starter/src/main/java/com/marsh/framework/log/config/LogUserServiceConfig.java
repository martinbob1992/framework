package com.marsh.framework.log.config;

import com.marsh.framework.log.interfaces.AccessLogExtendService;
import com.marsh.framework.log.interfaces.impl.DefaultSimpleAccessLogExtendServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author marsh
 **/
@Configuration
@ComponentScan(basePackages = {"com.marsh.framework.log"})
@EnableConfigurationProperties(LogProperties.class)
public class LogUserServiceConfig {

    @Bean
    @ConditionalOnMissingBean(AccessLogExtendService.class)
    public AccessLogExtendService simpleUserInfoService(){
        return new DefaultSimpleAccessLogExtendServiceImpl();
    }
}
