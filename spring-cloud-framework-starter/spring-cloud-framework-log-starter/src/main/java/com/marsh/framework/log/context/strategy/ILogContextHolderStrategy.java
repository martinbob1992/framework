package com.marsh.framework.log.context.strategy;


import com.marsh.framework.log.context.LogContext;

/**
 *
 * @author Marsh
 * @date 2022-06-17
 */
public interface ILogContextHolderStrategy {

    void clearContext();

    /**
     * 从当前上下文获取context,如果没有设置过则返回null
     * @author Marsh
     * @date 2022-06-17
     * @return com.marsh.framework.log.context.LogContext
     */
    LogContext getContext();

    /**
     * 获取上下文设置的context，如果没有设置则自动生成一个并返回
     * @author Marsh
     * @date 2022-06-17
     * @return com.marsh.framework.log.context.LogContext
     */
    LogContext getContextIfAbsentCreate();

    boolean hasContext();

    /**
     * 如果不存在则进行设置，返回值永远返回上一次设置的值，如果返回null则表示第一次设值
     * @author Marsh
     * @date 2022-06-17
     * @param context
     * @return com.marsh.framework.log.context.LogContext
     */
    LogContext putIfAbsent(LogContext context);


}
