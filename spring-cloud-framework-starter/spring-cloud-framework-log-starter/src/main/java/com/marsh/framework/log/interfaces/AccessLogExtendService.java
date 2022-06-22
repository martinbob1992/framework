package com.marsh.framework.log.interfaces;


import com.marsh.framework.log.aspect.AccessLogAspect;

import java.util.Map;

/**
 * 访问日志的扩展接口
 * @author marsh
 */
public interface AccessLogExtendService {

    /**
     * 获取扩展日志属性，null则表示日志不进行扩展
     * {@link AccessLogAspect}
     * @author Marsh
     * @date 2022-06-17
     * @return java.util.Map<java.lang.String,java.lang.Object>
     */
    Map<String,Object> getExtendParams();

}
