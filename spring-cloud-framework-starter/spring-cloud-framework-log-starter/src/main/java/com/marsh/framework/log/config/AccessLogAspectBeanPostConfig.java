package com.marsh.framework.log.config;

import cn.hutool.core.util.StrUtil;
import com.marsh.framework.log.aspect.AccessLogAspect;
import com.marsh.framework.log.config.LogProperties;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * @author Marsh
 * @date 2022-06-20日 10:21
 */
@ConditionalOnBean(AccessLogAspect.class)
@Configuration
@Slf4j
public class AccessLogAspectBeanPostConfig implements BeanFactoryPostProcessor {

    @Autowired
    private LogProperties logProperties;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        String rule = logProperties.getRule();
        if (StrUtil.isBlank(rule)){
            return;
        }
        AccessLogAspect accessLogAspectBean = configurableListableBeanFactory.getBean(AccessLogAspect.class);
        try {
            Method controllerAopMethod = accessLogAspectBean.getClass().getMethod("controllerAop");
            Pointcut pointcut = controllerAopMethod.getAnnotation(Pointcut.class);
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(pointcut);
            Field memberValues = invocationHandler.getClass().getDeclaredField("memberValues");
            memberValues.setAccessible(true);
            Map memberValueMap = (Map)memberValues.get(invocationHandler);
            memberValueMap.put("value",rule);
            log.debug("切面规则修改为:{}",rule);
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            log.error("动态修改controller切面规则出错!",e);
        }
    }
}
