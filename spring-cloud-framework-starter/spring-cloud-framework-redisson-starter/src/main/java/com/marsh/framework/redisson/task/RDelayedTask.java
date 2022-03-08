package com.marsh.framework.redisson.task;

import org.redisson.api.RDestroyable;
import org.redisson.api.RFuture;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 延时任务接口，提供了延时任务的取消及发布功能
 * @author marsh
 * @date 2022年02月11日 8:37
 */
public interface RDelayedTask<V> extends RDestroyable {

    /**
     * 尝试取消这个任务,
     * @param taskId
     * @return 找不到这个任务id或者任务已经运行中则无法取消返回false
     */
    boolean tryCancel(String taskId);

    /**
     * 提供一个任务
     * 相同任务id重复提交会修改这个任务信息（仅在任务还未执行时，如果已经执行，则会插入一个新的任务）
     * @author Marsh
     * @date 2022-02-11
     * @param taskId 任务id
     * @param data 任务数据
     * @param schedule 计划任务执行的时间
     * @return
     */
    void offer(String taskId, V data, Date schedule);

    /**
     * 提供一个任务
     * 相同任务id重复提交会修改这个任务信息（仅在任务还未执行时，如果已经执行，则会插入一个新的任务）
     * @author Marsh
     * @date 2022-02-11
     * @param taskId 任务id
     * @param data 任务数据
     * @param schedule 计划任务执行的时间戳
     * @return
     */
    void offer(String taskId, V data, long schedule);

    /**
     * 提供一个任务
     * 相同任务id重复提交会修改这个任务信息（仅在任务还未执行时，如果已经执行，则会插入一个新的任务）
     * @author Marsh
     * @date 2022-02-11
     * @param taskId 任务id
     * @param data 任务数据
     * @param time 任务在多久后运行，配合timeUnit使用
     * @param timeUnit 时间单位
     * @return
     */
    void offer(String taskId,V data, long time, TimeUnit timeUnit);

    /**
     * 提供一个任务(异步)
     * 相同任务id重复提交会修改这个任务信息（仅在任务还未执行时，如果已经执行，则会插入一个新的任务）
     * @author Marsh
     * @date 2022-02-11
     * @param taskId 任务id
     * @param data 任务数据
     * @param time 任务在多久后运行，配合timeUnit使用
     * @param timeUnit 时间单位
     * @return org.redisson.api.RFuture<java.lang.Void>
     */
    RFuture<Void> offerAsync(String taskId,V data, long time, TimeUnit timeUnit);

    /**
     * 提供一个任务(异步)
     * 相同任务id重复提交会修改这个任务信息（仅在任务还未执行时，如果已经执行，则会插入一个新的任务）
     * @author Marsh
     * @date 2022-02-11
     * @param taskId 任务id
     * @param data 任务数据
     * @param schedule 计划任务执行的时间戳
     * @return org.redisson.api.RFuture<java.lang.Void>
     */
    RFuture<Void> offerAsync(String taskId,V data, long schedule);

    /**
     * 提供一个任务(异步)
     * 相同任务id重复提交会修改这个任务信息（仅在任务还未执行时，如果已经执行，则会插入一个新的任务）
     * @author Marsh
     * @date 2022-02-11
     * @param taskId 任务id
     * @param data 任务数据
     * @param schedule 计划任务执行的时间戳
     * @return org.redisson.api.RFuture<java.lang.Void>
     */
    RFuture<Void> offerAsync(String taskId,V data, Date schedule);

}
