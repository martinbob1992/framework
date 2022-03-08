package com.marsh.framework.redisson.task;

import org.redisson.QueueTransferTask;
import org.redisson.RedissonObject;
import org.redisson.RedissonTopic;
import org.redisson.api.RFuture;
import org.redisson.api.RTopic;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.command.CommandAsyncExecutor;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 基于redisson的延时任务,具体使用可参考 com.yihai.cloud.redis.task.spring.RedissonDelayedTaskProxy
 * 1.支持分布式提交和消费任务
 * 2.支持任务取消，修改功能
 * 3.采用异步编程模型
 * 4.任务具有自动的ack机制,如果代码执行成功则自动ack，如果执行消费程序报错则unack将任务重新投递到定时列表中并记录失败次数
 * 5.所有发布的任务均保存到redis中，即使程序关闭，下次运行时还是会将过期的任务继续执行
 * 6.一个延时任务仅被消费一次(除非之前执行报错)
 *
 * 不足：
 * 1.任务执行超时固定是3分钟，超过3分钟的任务会认为是超时了，重新投递到任务列表中5分钟后重试执行
 * @author Marsh
 * @date 2022-02-11日 8:59
 */
public class RRedissonDelayedTask<V> extends RedissonObject implements RDelayedTask<V> {

    private final DelayedTaskTransferService delayedTaskTransferService;

    /**
     * 任务名称，所有redis数据key都是已任务名称作为前缀来使用的
     */
    private final String taskName;
    /**
     * 任务计划表
     * redis数据类型:SortedSet
     * score => 任务开始的时间戳
     * member => V
     */
    private final String taskScheduleName;

    /**
     * 任务数据表(这里存放任务id和数据的hash对应表)
     * redis数据类型:Hash
     * key => 任务id
     * value => V
     */
    private final String taskDataName;

    /**
     * 任务队列(这里存放已经准备好的任务)
     * redis数据类型:List
     * value => 任务id
     */
    private final String taskQueueName;
    /**
     * 任务队列超时表
     * 任务在执行时会从{taskQueueName}队列将数据放到超时表中，如果任务执行超时会认为任务执行失败，将任务重新投递到任务队列中
     * redis数据类型:SortedSet
     * score => 任务超时的时间戳
     * member => 任务id
     */
    private final String taskTimeoutQueueName;
    /**
     * 任务重试表（当任务出现失败时，重试表重试次数+1，直到任务取消或成功才删除这条数据）
     * redis数据类型:Hash
     * key => 任务id
     * value => 重试次数
     */
    private final String taskRetryName;

    /**
     * 任务计划表通道名称
     */
    private final String scheduleChannelName;
    /**
     * 任务已准备就绪通道名称
     */
    private final String taskChannelName;

    public RRedissonDelayedTask(String taskType,
                                Codec codec,
                                DelayedTaskTransferService delayedTaskTransferService,
                                CommandAsyncExecutor commandExecutor,
                                RDelayedTaskHandler delayedTaskHandler) {
        super(codec, commandExecutor, taskType);
        this.taskName = prefixName("redisson_delay_task", getName());
        this.taskQueueName = suffixName(this.taskName, "task_queue");
        this.taskDataName = suffixName(this.taskName, "data");
        this.taskRetryName = suffixName(this.taskName, "retry");
        this.taskTimeoutQueueName = suffixName(this.taskName, "task_timeout_queue");
        this.taskScheduleName = suffixName(this.taskName, "schedule");
        this.scheduleChannelName = suffixName(this.taskName, "schedule_channel");
        this.taskChannelName = suffixName(this.taskName, "task_channel");

        QueueTransferTask task = new QueueTransferTask(commandExecutor.getConnectionManager()) {
            @Override
            protected RFuture<Long> pushTaskAsync() {
                return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_LONG,
                        "local expiredValues = redis.call('zrangebyscore', KEYS[1], 0, ARGV[1], 'limit', 0, ARGV[2]); "
                                + "if #expiredValues > 0 then "
                                + "for i, v in ipairs(expiredValues) do "
                                + "redis.call('rpush', KEYS[2], v);"
                                + "end; "
                                + "redis.call('zrem', KEYS[1], unpack(expiredValues));"
                                // 通知系统进行任务消费
                                + "redis.call('publish', KEYS[3], #expiredValues);"
                                + "end; "
                                // 查询一个最新的过期任务,主要用来更新超时任务时间  QueueTransferTask#scheduleTask
                                + "local v = redis.call('zrange', KEYS[1], 0, 0, 'WITHSCORES'); "
                                + "if v[1] ~= nil then "
                                + "return v[2]; "
                                + "end "
                                + "return nil;",
                        Arrays.<Object>asList(taskScheduleName, taskQueueName, taskChannelName),
                        System.currentTimeMillis(), 100);
            }


            @Override
            protected RTopic getTopic() {
                return new RedissonTopic(LongCodec.INSTANCE, commandExecutor, scheduleChannelName);
            }
        };

        DelayedConsumerTask<V> delayedConsumerTask = new DelayedConsumerTask<V>(commandExecutor.getConnectionManager(),delayedTaskHandler) {

            @Override
            protected RTopic getTopic() {
                return new RedissonTopic(LongCodec.INSTANCE, commandExecutor, taskChannelName);
            }

            @Override
            protected RFuture<String> pushTaskAsync() {
                String lua = "local v = redis.call('LPOP',KEYS[1]); " +
                        "if v ~= false then " +
                        "redis.call('ZADD',KEYS[2],ARGV[1],v); " +
                        "return v; " +
                        "end; " +
                        "return nil; ";
                return commandExecutor.evalWriteAsync(getName(), StringCodec.INSTANCE,RedisCommands.EVAL_OBJECT,
                        lua,Arrays.<Object>asList(taskQueueName,taskTimeoutQueueName),
                        // 目前写死3分钟内如果这个任务执行失败则重新投递到任务队列中
                        getMillisecondsByAfterMinutes(3));
            }

            @Override
            protected RFuture<Long> pushTimeoutTaskAsync() {
                String lua = "local expiredValues = redis.call('zrangebyscore', KEYS[1], 0, ARGV[1], 'limit', 0, ARGV[2]); "
                        + "if #expiredValues > 0 then "
                        + "for i, v in ipairs(expiredValues) do "
                        + "redis.call('ZADD',KEYS[2],ARGV[3],v); "
                        + "redis.call('HINCRBY', KEYS[3], v, 1);"
                        + "end; "
                        + "redis.call('zrem', KEYS[1], unpack(expiredValues));"
                        + "end; "
                        // 查询一个最新的过期任务,主要用来更新超时任务时间  QueueTransferTask#scheduleTask
                        + "local v = redis.call('zrange', KEYS[1], 0, 0, 'WITHSCORES'); "
                        + "if v[1] ~= nil then "
                        + "return v[2]; "
                        + "end "
                        + "return nil;";
                return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE,RedisCommands.EVAL_LONG,
                        lua,
                        Arrays.<Object>asList(taskTimeoutQueueName,taskScheduleName,taskRetryName),
                        System.currentTimeMillis(),100,getMillisecondsByAfterMinutes(5)// 把所有超时的任务推后到5分钟后重新执行
                );
            }

            private long getMillisecondsByAfterMinutes(int minutes){
                long timeout = minutes*60*1000;
                return System.currentTimeMillis() + timeout;
            }

            @Override
            protected RFuture<V> getData(String taskId) {
                return commandExecutor.evalReadAsync(getName(),codec,RedisCommands.EVAL_OBJECT,
                        "return redis.call('hget', KEYS[1],KEYS[2]);",
                        Arrays.<Object>asList(taskDataName,taskId)
                        );
            }

            @Override
            protected RFuture<Void> ack(String taskId) {
                String lua = "redis.call('ZREM', KEYS[2], KEYS[1]);"
                        + "redis.call('HDEL',KEYS[3],KEYS[1]);"
                        + "redis.call('HDEL',KEYS[4],KEYS[1]);";
                return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE,RedisCommands.EVAL_VOID,
                        lua,
                        Arrays.<Object>asList(taskId,taskTimeoutQueueName,taskDataName,taskRetryName)
                );
            }

            @Override
            protected RFuture<Void> unack(String taskId) {
                String lua = "local result = redis.call('ZREM', KEYS[2], KEYS[1]); "
                        + "if result > 0 then "
                        + "redis.call('ZADD',KEYS[3],ARGV[1],KEYS[1]);"
                        + "redis.call('HINCRBY', KEYS[4],KEYS[1], 1);"
                        //检查这个定时任务是否是队列中最先过期的
                        + "local v = redis.call('zrange', KEYS[3], 0, 0,'WITHSCORES'); "
                        + "if v[2] == ARGV[1] then "
                        + "redis.call('publish', KEYS[5], ARGV[1]); "
                        + "end;"
                        + "return 1;"
                        + "end; "
                        + "return 0;";
                return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE,RedisCommands.EVAL_BOOLEAN,
                        lua,
                        Arrays.<Object>asList(taskId,taskTimeoutQueueName,taskScheduleName,taskRetryName,scheduleChannelName),
                        getMillisecondsByAfterMinutes(5)// 5分钟后重新执行这个任务
                );
            }
        };

        delayedTaskTransferService.schedule(taskName, task, delayedConsumerTask);
        this.delayedTaskTransferService = delayedTaskTransferService;
    }

    @Override
    public boolean tryCancel(String taskId) {
        String lua = "local result = redis.call('ZREM', KEYS[2], KEYS[1]); "
                + "if result > 0 then "
                + "redis.call('HDEL',KEYS[4],KEYS[1]);"
                + "redis.call('HDEL',KEYS[5],KEYS[1]);"
                + "return 1;"
                + "end; "
                + "local result1 = redis.call('LREM', KEYS[3],0,KEYS[1]);"
                + "if result1 > 0 then "
                + "redis.call('HDEL',KEYS[4],KEYS[1]);"
                + "redis.call('HDEL',KEYS[5],KEYS[1]);"
                + "return 1;"
                + "end; "
                + "return 0;";
        return get(commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE,RedisCommands.EVAL_BOOLEAN,
                lua,
                Arrays.<Object>asList(taskId,taskScheduleName,taskQueueName,taskDataName,taskRetryName)
        ));
    }

    @Override
    public void offer(String taskId, V data, Date schedule) {
        get(offerAsync(taskId, data, schedule.getTime()));
    }

    @Override
    public void offer(String taskId, V data, long schedule) {
        commandExecutor.get(offerAsync(taskId, data, schedule));
    }

    @Override
    public void offer(String taskId, V data, long time, TimeUnit timeUnit) {
        long delayInMs = timeUnit.toMillis(time);
        long timeout = System.currentTimeMillis() + delayInMs;
        offer(taskId, data, timeout);
    }

    @Override
    public RFuture<Void> offerAsync(String taskId, V data, long time, TimeUnit timeUnit) {
        long delayInMs = timeUnit.toMillis(time);
        long timeout = System.currentTimeMillis() + delayInMs;
        return offerAsync(taskId, data, timeout);
    }

    @Override
    public RFuture<Void> offerAsync(String taskId, V data, long schedule) {
        if (schedule < 0) {
            throw new IllegalArgumentException("定时任务时间戳设置异常");
        }
        return commandExecutor.evalWriteAsync(getName(), codec, RedisCommands.EVAL_VOID,
                "redis.call('zadd', KEYS[1], ARGV[1], KEYS[3]);"
                        + "redis.call('hset', KEYS[2], KEYS[3], ARGV[2]);"
                        // if new object added to queue head when publish its startTime
                        // to all scheduler workers
                        + "local v = redis.call('zrange', KEYS[1], 0, 0); "
                        + "if v[1] == KEYS[3] then "
                        + "redis.call('publish', KEYS[4], ARGV[1]); "
                        + "end;",
                Arrays.<Object>asList(taskScheduleName, taskDataName,taskId, scheduleChannelName),
                schedule, encode(data));
    }

    @Override
    public RFuture<Void> offerAsync(String taskId, V data, Date schedule) {
        return offerAsync(taskId, data, schedule.getTime());
    }

    @Override
    public void destroy() {
        this.delayedTaskTransferService.remove(this.taskName);
    }
}
