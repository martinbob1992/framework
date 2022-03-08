package com.marsh.framework.redisson.task;

/**
 * 延时任务执行器
 * @author marsh
 * @date 2022年02月11日 17:07
 */
@FunctionalInterface
public interface RDelayedTaskHandler<V> {

    /**
     * 当延时任务触发执行时运行
     * @param taskId 任务id
     * @param msg 发布任务是当时传入的参数
     * @throws Exception
     */
    void execute(String taskId,V msg) throws Exception;
}
