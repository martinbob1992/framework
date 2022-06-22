package com.marsh.framework.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 该接口配合切面对标记了该接口的方法设置追踪链路
 * @author Marsh
 * @date 2022-06-17
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ThreadTraceId {
}
