package com.marsh.framework.redisson.queue;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import org.redisson.*;
import org.redisson.api.*;
import org.redisson.api.listener.BaseStatusListener;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.IntegerCodec;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.client.protocol.ScoredEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Marsh
 * @date 2022-06-01日 11:29
 */
@Slf4j
public class RAckQueue<T extends QueueData> extends RedissonObject implements RDestroyable {

    private static final String CMD_TIMEOUT = "timeout";
    private static final String CMD_MESSAGE = "message";
    private static final String CMD_ERROR = "error";

    private final RTopic topic;
    private final AtomicReference<QueueTransferTask.TimeoutTask> lastTimeout = new AtomicReference<QueueTransferTask.TimeoutTask>();

    private final Redisson redisson;
    private final String queueName;
    private final String dataHashName;
    private final String timeoutQueueName;
    private final String retryName;
    private final String errorName;
    private final String channelName;
    private final BatchOptions defaultBatchOptions = BatchOptions.defaults().executionMode(BatchOptions.ExecutionMode.IN_MEMORY_ATOMIC);


    public RAckQueue(Redisson redisson, String name, Codec codec){
        super(codec, redisson.getCommandExecutor(), name);
        this.redisson = redisson;
        this.queueName = prefixName("redisson_security_queue", getName());
        this.dataHashName = suffixName(this.queueName, "data");
        this.timeoutQueueName = suffixName(this.queueName, "timeout");
        this.retryName = suffixName(this.queueName, "retry");
        this.errorName = suffixName(this.queueName, "error");
        this.channelName = suffixName(this.queueName, "channel");

        topic = new RedissonTopic(StringCodec.INSTANCE, commandExecutor, channelName);
        topic.addListener(new BaseStatusListener() {
            @Override
            public void onSubscribe(String channel) {
                cleanTimeout();
            }
        });

        topic.addListener(String.class, new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence channel, String cmd) {
                if (StrUtil.isBlank(cmd)){
                    return;
                }
                String[] split = cmd.split(":");
                if (CMD_TIMEOUT.equals(split[0])){
                    scheduleTimeoutTask(Long.parseLong(split[1]));
                } else if (CMD_MESSAGE.equals(split[0])){
                    RAckQueue.this.onMessage(split[1]);
                } else if (CMD_ERROR.equals(split[0])){
                    onError(split[1]);
                }
            }
        });

    }

    /**
     * 获取执行超时的毫秒数
     * @author Marsh
     * @date 2022-06-01
     * @return java.lang.Long 默认5分钟
     */
    public Long getExecutionTimeoutMs(){
        return 300000L;
    }

    /**
     * 失败重试次数，超过重试次数的会将其写到错误队列中，后续人工检查进行处理
     * 默认5次，如果该值为-1则无限重试
     * @return
     */
    public int getRetryNumber(){
        return 5;
    }

    public void onMessage(String id){
        System.out.println(getName()+"\t"+"onMessage('"+id+"')");
    }

    public void onError(String id){
        System.out.println(getName()+"\t"+"onError('"+id+"')");
    }

    private void scheduleTimeoutTask(final Long startTime) {
        if (startTime == null) {
            return;
        }
        QueueTransferTask.TimeoutTask oldTimeout = lastTimeout.get();
        if (oldTimeout != null) {
            oldTimeout.getTask().cancel();
        }

        long delay = startTime - System.currentTimeMillis();
        if (delay > 10) {
            Timeout timeout = commandExecutor.getConnectionManager().newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    cleanTimeout();

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
            cleanTimeout();
        }
    }

    private void cleanTimeout() {
        RFuture<Long> startTimeFuture = cleanTimeoutAsync();
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

    /**
     * 用来进行清理超时的lua脚本
     */
    private static final String CLEAN_TIMEOUT_LUA =
            "local expiredValues = redis.call('zrangebyscore', KEYS[1], 0, ARGV[1], 'limit', 0, ARGV[2]); " +
            "if #expiredValues > 0 then " +
                "local maxRetry = tonumber(ARGV[3]); " +
                "for i, v in ipairs(expiredValues) do " +
                    "if maxRetry < 0 then " +
                        "redis.call('LPUSH',KEYS[2],v); " +
                        "redis.call('publish', KEYS[5], '"+CMD_MESSAGE+"'..':'..v); " +
                        "redis.call('HINCRBY', KEYS[3], v, 1);" +
                    "elseif maxRetry == 0 then " +
                        "redis.call('ZADD',KEYS[4],ARGV[1],v);" +
                        "redis.call('publish', KEYS[5], '"+CMD_ERROR+"'..':'..v); " +
                    "else " +
                        "local retry = redis.call('HGET', KEYS[3], v);" +
                        "if retry ~= false and tonumber(retry) >= maxRetry then " +
                            "redis.call('ZADD',KEYS[4],ARGV[1],v); " +
                            "redis.call('publish', KEYS[5], '"+CMD_ERROR+"'..':'..v); " +
                        "else " +
                            "redis.call('LPUSH',KEYS[2],v); " +
                            "redis.call('publish', KEYS[5], '"+CMD_MESSAGE+"'..':'..v); " +
                            "redis.call('HINCRBY', KEYS[3], v, 1);" +
                        "end " +
                    "end; " +
                "end; " +
                "redis.call('zrem', KEYS[1], unpack(expiredValues));" +
                // 查询一个最新的过期任务,主要用来更新超时任务时间
                "local v = redis.call('zrange', KEYS[1], 0, 0, 'WITHSCORES'); " +
                "if v[1] ~= nil then " +
                    "return v[2]; " +
                "end " +
            "end " +
            "return nil;";
    private RFuture<Long> cleanTimeoutAsync() {
        return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_LONG,
                CLEAN_TIMEOUT_LUA,
                Arrays.<Object>asList(timeoutQueueName,queueName,retryName,errorName,channelName),
                System.currentTimeMillis(),100,getRetryNumber());
    }


    public boolean push(List<T> datas) {
        if (datas == null || datas.size() == 0){
            return false;
        }
        RBatch batch = redisson.createBatch(defaultBatchOptions);
        RMapAsync<String, T> map = batch.getMap(dataHashName, codec);
        RDequeAsync<String> deque = batch.getDeque(queueName, StringCodec.INSTANCE);
        for (T data : datas){
            map.putAsync(data.getId(),data);
            deque.addFirstAsync(data.getId());
        }
        batch.execute();
        return true;
    }

    public boolean push(T data) {
        if (data == null){
            return false;
        }
        return push(ListUtil.toList(data));
    }

    public T pop(){
        List<T> results = pop(1);
        return results != null && results.size() > 0 ? results.get(0) : null;
    }

    private static final String POP_LUA =
            "local i = 1; " +
            "local res = {}; " +
            "local size = tonumber(ARGV[2]); " +
            "while i <= size do " +
                "local v = redis.call('RPOP',KEYS[1]); " +
                "if v == false then " +
                    "if #res > 0 then " +
                        "local timeout = redis.call('zrange', KEYS[2], 0, 0, 'WITHSCORES'); " +
                        "if timeout[1] ~= nil and tonumber(ARGV[1]) <= tonumber(timeout[2]) then " +
                            "redis.call('publish', KEYS[4], '"+CMD_TIMEOUT+"'..':'..timeout[2]); " +
                        "end; " +
                    "end; " +
                    "return res; " +
                "end; " +
                "redis.call('ZADD',KEYS[2],ARGV[1],v); " +
                "local data = redis.call('HGET',KEYS[3],v); " +
                "table.insert(res, data);" +
                "i = i + 1; " +
            "end; " +
            "local timeout = redis.call('zrange', KEYS[2], 0, 0, 'WITHSCORES'); " +
            "if timeout[1] ~= nil and tonumber(ARGV[1]) <= tonumber(timeout[2]) then " +
                "redis.call('publish', KEYS[4], '"+CMD_TIMEOUT+"'..':'..timeout[2]); " +
            "end; " +
            "return res; ";

    public List<T> pop(int size){
        RScript script = redisson.getScript(codec);
        return script.eval(RScript.Mode.READ_WRITE, POP_LUA, RScript.ReturnType.MULTI,
                Arrays.asList(queueName, timeoutQueueName, dataHashName,channelName),
                System.currentTimeMillis()+getExecutionTimeoutMs(),size);
    }

    public void ack(String id){
        List<String> ids = new ArrayList<>();
        ids.add(id);
        ack(ids);
    }

    public void ack(List<String> ids){
        if (ids == null || ids.size() == 0){
            return;
        }
        RBatch batch = redisson.createBatch(defaultBatchOptions);
        RMapAsync<String, T> map = batch.getMap(dataHashName, codec);
        RScoredSortedSetAsync<String> timeoutSet = batch.getScoredSortedSet(timeoutQueueName,StringCodec.INSTANCE);
        RMapAsync<String, Integer> retryMap = batch.getMap(retryName, IntegerCodec.INSTANCE);
        ids.stream().forEach(id -> {
            map.removeAsync(id);
            timeoutSet.removeAsync(id);
            retryMap.removeAsync(id);
        });
        RFuture<Collection<ScoredEntry<String>>> collectionRFuture = timeoutSet.entryRangeAsync(0, 0);
        collectionRFuture.onComplete((v,e)->{
            if (e != null){
                ScoredEntry<String> entry = v.stream().findFirst().get();
                scheduleTimeoutTask(entry.getScore().longValue());
            }
        });
        batch.execute();
    }

    public void unack(String id){
        List<String> ids = new ArrayList<>();
        ids.add(id);
        unack(ids);
    }

    public void unack(List<String> ids){
        if (ids == null || ids.size() == 0){
            return;
        }
        RBatch batch = redisson.createBatch(defaultBatchOptions);
        RTopicAsync topic = batch.getTopic(channelName,StringCodec.INSTANCE);
        RDequeAsync<String> queueBatch = batch.getDeque(queueName, StringCodec.INSTANCE);
        RScoredSortedSetAsync<String> timeoutSet = batch.getScoredSortedSet(timeoutQueueName, StringCodec.INSTANCE);
        for (String key : ids){
            // 将这个数据重新添加到队列尾部,让这条消息被尽快消费
            queueBatch.addLastAsync(key);
            // 删除这个key的过期时间
            timeoutSet.removeAsync(key);
            topic.publishAsync(CMD_MESSAGE+":"+key);
        }
        batch.execute();
    }

    @Override
    public void destroy(){
        topic.removeAllListeners();
    }
}
