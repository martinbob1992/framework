package com.marsh.framework.redisson.task;

import cn.hutool.core.util.StrUtil;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import org.redisson.QueueTransferTask;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RFuture;
import org.redisson.api.RTopic;
import org.redisson.api.listener.BaseStatusListener;
import org.redisson.api.listener.MessageListener;
import org.redisson.connection.ConnectionManager;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 延时消费任务
 * 这个类的主要职责如下
 * 1.在任务被初次加载时，检查redis任务队列中是否堆积了过期任务，如果有则立即执行掉
 * 具体看start()方法BaseStatusListener监听调用了2个方法
 * 2.将执行过期的任务重新投递到定时计划表中
 * pushTimeoutTask()
 * 3.当监听到任务需要被消费时，调用pushTaskAsync()方法异步执行消费任务，任务如果在执行过程中报异常则会将任务重新投递到任务计划表中
 * 具体看start()方法第二个监听器MessageListener调用了consumerTask()方法
 * @author Marsh
 * @date 2022-02-12日 9:31
 */
@Slf4j
public abstract class DelayedConsumerTask<V> {

    private int messageListenerId;
    private int statusListenerId;
    private final AtomicReference<QueueTransferTask.TimeoutTask> lastTimeout = new AtomicReference<QueueTransferTask.TimeoutTask>();
    private final ConnectionManager connectionManager;
    private final RDelayedTaskHandler<V> delayedTaskHandler;

    public DelayedConsumerTask(ConnectionManager connectionManager, RDelayedTaskHandler<V> delayedTaskHandler){
        this.connectionManager = connectionManager;
        this.delayedTaskHandler = delayedTaskHandler;
    }


    public void start() {
        RTopic topic = getTopic();
        statusListenerId = topic.addListener(new BaseStatusListener() {
            @Override
            public void onSubscribe(String channel) {
                consumerTask();
                pushTimeoutTask();
            }
        });

        messageListenerId = topic.addListener(Long.class, new MessageListener<Long>() {
            @Override
            public void onMessage(CharSequence channel, Long number) {
                consumerTask();
            }
        });
    }

    public void stop() {
        RTopic topic = getTopic();
        topic.removeListener(messageListenerId);
        topic.removeListener(statusListenerId);
    }


    private void scheduleTimeoutTask(final Long startTime) {
        QueueTransferTask.TimeoutTask oldTimeout = lastTimeout.get();
        if (startTime == null) {
            return;
        }

        if (oldTimeout != null) {
            oldTimeout.getTask().cancel();
        }

        long delay = startTime - System.currentTimeMillis();
        if (delay > 10) {
            Timeout timeout = connectionManager.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    pushTimeoutTask();

                    QueueTransferTask.TimeoutTask currentTimeout = lastTimeout.get();
                    if (currentTimeout.getTask() == timeout) {
                        lastTimeout.compareAndSet(currentTimeout, null);
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
            if (!lastTimeout.compareAndSet(oldTimeout, new QueueTransferTask.TimeoutTask(startTime, timeout))) {
                timeout.cancel();
            }
        } else {
            pushTimeoutTask();
        }
    }

    protected void pushTimeoutTask() {
        RFuture<Long> startTimeFuture = pushTimeoutTaskAsync();
        startTimeFuture.onComplete((res, e) -> {
            if (e != null) {
                if (e instanceof RedissonShutdownException) {
                    return;
                }
                log.error(e.getMessage(), e);
                scheduleTimeoutTask(System.currentTimeMillis() + 5 * 1000L);
                return;
            }

            if (res != null) {
                scheduleTimeoutTask(res);
            }
        });
    }

    protected abstract RTopic getTopic();

    protected abstract RFuture<String> pushTaskAsync();


    protected abstract RFuture<Long> pushTimeoutTaskAsync();
    protected abstract RFuture<V> getData(String taskId);

    protected abstract RFuture<Void> ack(String taskId);

    protected abstract RFuture<Void> unack(String taskId);

    private void consumerTask() {
        RFuture<String> dataFuture = pushTaskAsync();
        dataFuture.onComplete((taskId, e) -> {
            if (e != null) {
                if (e instanceof RedissonShutdownException) {
                    return;
                }
                log.error(e.getMessage(), e);
                return;
            }

            if (StrUtil.isNotBlank(taskId)) {
                RFuture<V> data = getData(taskId);
                data.onComplete((v,err) ->{
                    if (e != null) {
                        if (e instanceof RedissonShutdownException) {
                            return;
                        }
                        log.error(e.getMessage(), e);
                        return;
                    }
                    if (v != null){
                        try {
                            delayedTaskHandler.execute(taskId,v);
                            ack(taskId);
                        } catch (Exception exception){
                            log.error(exception.getMessage(), exception);
                            unack(taskId);
                            log.error("{}任务执行失败已回滚任务!",taskId);
                        }
                        //递归查看是否还有没处理完的任务,直到任务队列清空
                        consumerTask();
                    }
                });
            }
        });
    }

}
