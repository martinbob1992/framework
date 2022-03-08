package com.marsh.framework.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

/**
 * @author Marsh
 * @date 2022-03-02日 10:20
 */
@Configuration
@ConditionalOnMissingBean(ResourceServerConfigurerAdapter.class)
@Import(ResourceServiceStarterConfig.class)
public class SpringSecurityResourceConfig {
}
