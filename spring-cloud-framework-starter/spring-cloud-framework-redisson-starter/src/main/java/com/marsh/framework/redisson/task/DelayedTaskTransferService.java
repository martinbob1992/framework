package com.marsh.framework.redisson.task;

import org.redisson.QueueTransferTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 延时任务转换service
 * 这个类主要通过synchronized关键字保证同一个类型的延时任务只允许有一个能启动
 * @author Marsh
 * @date 2022-02-11日 17:37
 */
public class DelayedTaskTransferService {

    private final ConcurrentMap<String, QueueTransferTask> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DelayedConsumerTask> delayedConsumerTasks = new ConcurrentHashMap<>();

    public synchronized void schedule(String name, QueueTransferTask task,DelayedConsumerTask delayedConsumerTask) {
        QueueTransferTask oldTask = tasks.putIfAbsent(name, task);
        if (oldTask == null) {
            task.start();
            delayedConsumerTasks.put(name, delayedConsumerTask);
            delayedConsumerTask.start();
        } else {
            oldTask.incUsage();
        }
    }

    public synchronized void remove(String name) {
        QueueTransferTask task = tasks.get(name);
        if (task != null) {
            if (task.decUsage() == 0) {
                tasks.remove(name, task);
                task.stop();
                DelayedConsumerTask taskPushTopic = delayedConsumerTasks.get(name);
                delayedConsumerTasks.remove(name);
                if (taskPushTopic != null){
                    taskPushTopic.stop();
                }
            }
        }
    }
}
