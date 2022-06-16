package com.marsh.framework.redisson.queue;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.IdUtil;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 带有ack消息确认的队列
 * 使用方式：子类继承这个类，并交由spring管理即可
 * @Component
 * public class MyAckQueue<V> extends RAckQueue<V> {
 *
 *     public MyAckQueue(RedissonClient redisson) {
 *         super((Redisson) redisson, "test", new GsonCodec(Object.class));
 *     }
 *
 *     // 子类可以重写已下方法来完成自己的特殊业务
 *     // getExecutionTimeoutMs()  方法设置超时时间，当规定时间内没处理完任务会将任务重新投递
 *     // getRetryNumber()  任务重试次数
 *     // onMessage(消息id) 当队列中放入了一个新的数据时
 *     // onError(消息id) 当消息重试次数操作设置的最大重试次数时
 * }
 * @author Marsh
 * @date 2022-06-01日 11:29
 */
@Slf4j
public class RAckQueue<V> extends RedissonObject implements RDestroyable {

    private static final String CMD_TIMEOUT = "timeout";
    private static final String CMD_MESSAGE = "message";
    private static final String CMD_ERROR = "error";

    private static final ConcurrentMap<String, RTopic> topicCache = new ConcurrentHashMap<>();

    private final AtomicReference<QueueTransferTask.TimeoutTask> lastTimeout = new AtomicReference<QueueTransferTask.TimeoutTask>();

    private final Redisson redisson;
    /**
     * 数据队列key的名称
     * redis数据类型：List<消息id>
     */
    private final String queueName;
    /**
     * 消息id和数据对应关系表
     * redis数据类型：Hash<消息id,用户保存到队列中的数据>
     */
    private final String dataHashName;
    /**
     * 超时队列，当数据从队列中取出一条数据后，会将这个数据放到超时队列中一份，如果在指定的超时时间内未调用ack(消息id)方法，则这个数据会重新
     * 保存到队列中。
     * PS:这样做主要是为了防止在程序中获取了一个队列数据，但是还未处理的情况下系统宕机重启等一系列故障导致数据丢失问题
     * redis数据类型：Zset<超时时间时间戳,消息id>
     */
    private final String timeoutQueueName;
    /**
     * 记录重试次数的（不论是超时还是主动调用unack都是使用这个来记录次数的）
     * redis数据类型：Hash<消息id,重试次数>
     */
    private final String retryName;
    /**
     * 错误队列 （超过最大允许的重试次数时会放到这个队列中)
     * redis数据类型：Zset<错误发生的时间戳,消息id>
     */
    private final String errorName;
    /**
     * 进行消息传输的topic名称，当队列放入数据通知，错误队列放入数据通知和数据超时清理均会通过消息进行
     * redis数据类型: Pub/Sub 订阅发布
     */
    private final String channelName;
    private final BatchOptions defaultBatchOptions = BatchOptions.defaults().executionMode(BatchOptions.ExecutionMode.IN_MEMORY_ATOMIC);


    public RAckQueue(Redisson redisson, String name, Codec codec){
        super(codec, redisson.getCommandExecutor(), name);
        this.redisson = redisson;
        this.queueName = prefixName("redisson_ack_queue", getName());
        this.dataHashName = suffixName(this.queueName, "data");
        this.timeoutQueueName = suffixName(this.queueName, "timeout");
        this.retryName = suffixName(this.queueName, "retry");
        this.errorName = suffixName(this.queueName, "error");
        this.channelName = suffixName(this.queueName, "channel");

        RTopic topic = new RedissonTopic(StringCodec.INSTANCE, commandExecutor, channelName);
        RTopic oldTopic = topicCache.putIfAbsent(getName(), topic);
        if (oldTopic == null){
            // 这里主要是为了防止name相同的子类创建多个监听导致数据混乱问题
            topic.addListener(new BaseStatusListener() {
                @Override
                public void onSubscribe(String channel) {
                    cleanTimeout();
                }
            });
            /**
             * 添加一个消息处理器监听，主要用来监听
             * 1.超时事件（会将超时的队列任务重新投递到队列中，如果超过最大重试次数则放到失败队列中）
             * 2.队列中新增数据事件（用户主动向队列中放置数据，由于超时重新投递的数据，主动调用unack()方法重新投放到队列中的数据
             * 3.当数据被放到失败队列中
             */
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
    }

    /**
     * 获取执行超时的毫秒数（默认5分钟），子类可以覆盖这个方法设置自己的任务执行超时时间
     * @author Marsh
     * @date 2022-06-01
     * @return java.lang.Long
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

    /**
     * 当队列中投递了消息时，触发这个方法，子类可以通过重写这个方法来实现对消息的处理
     * @author Marsh
     * @date 2022-06-14
     * @param id
     */
    public void onMessage(String id){
        log.info(getName()+"\t"+"onMessage('"+id+"')");
    }

    /**
     * 当消息重试次数达到最大被放入到失败队列时，这个方法会被触发
     * @author Marsh
     * @date 2022-06-14
     * @param id
     */
    public void onError(String id){
        log.info(getName()+"\t"+"onError('"+id+"')");
    }

    /**
     * 定时处理超时的任务
     * @author Marsh
     * @date 2022-06-14
     * @param startTime
     */
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

    /**
     * 清理所有已经超时的任务
     * @author Marsh
     * @date 2022-06-14
     */
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

    /**
     * 清理超时任务异步方法
     * @author Marsh
     * @date 2022-06-14
     * @return org.redisson.api.RFuture<java.lang.Long>
     */
    private RFuture<Long> cleanTimeoutAsync() {
        return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_LONG,
                CLEAN_TIMEOUT_LUA,
                Arrays.<Object>asList(timeoutQueueName,queueName,retryName,errorName,channelName),
                System.currentTimeMillis(),100,getRetryNumber());
    }


    /**
     * 批量给队列放置数据 （数据放置到队列头）
     * @author Marsh
     * @date 2022-06-14
     * @param datas
     * @return List<String> 返回保存到队列中的消息id
     */
    public List<String> push(List<V> datas) {
        List<String> result = new ArrayList<>();
        if (datas == null || datas.size() == 0){
            return result;
        }
        RBatch batch = redisson.createBatch(defaultBatchOptions);
        RTopicAsync topic = batch.getTopic(channelName,StringCodec.INSTANCE);
        RMapAsync<String, V> map = batch.getMap(dataHashName, codec);
        RDequeAsync<String> deque = batch.getDeque(queueName, StringCodec.INSTANCE);

        for (V data : datas){
            String msgId = IdUtil.fastUUID();
            result.add(msgId);
            map.putAsync(msgId,data);
            deque.addFirstAsync(msgId);
            topic.publishAsync(CMD_MESSAGE+":"+msgId);
        }
        batch.execute();
        return result;
    }

    /**
     * 给队列中放置一个数据（数据放置到队列头）
     * @author Marsh
     * @date 2022-06-14
     * @param data
     * @return boolean
     */
    public String push(V data) {
        if (data == null){
            return "";
        }
        List<String> result = push(ListUtil.toList(data));
        return result != null && result.size() > 0 ? result.get(0):"";
    }

    /**
     * 通过消息id弹出数据的lua脚本
     */
    private static final String POP_ID_LUA =
            "local flag = redis.call('LREM',KEYS[2],-1,ARGV[2]);" +
            "if flag > 0 then " +
                "redis.call('ZADD',KEYS[1],ARGV[1],ARGV[2]);" +
                "local timeout = redis.call('zrange', KEYS[1], 0, 0, 'WITHSCORES'); " +
                "if timeout[1] ~= nil then " +
                    "redis.call('publish', KEYS[4], '"+CMD_TIMEOUT+"'..':'..timeout[2]); " +
                "end; " +
                "return redis.call('HGET',KEYS[3],ARGV[2]);" +
            "end;" +
            "return nil;";
    /**
     * 直接通过消息id弹出数据
     * @author Marsh
     * @date 2022-06-15
     * @param id
     * @return com.marsh.framework.redisson.queue.RAckQueueData<V>
     */
    public RAckQueueData<V> pop(String id){
        if (StrUtil.isBlank(id)){
            return null;
        }
        RScript script = redisson.getScript(getCodec());
        V result = script.eval(RScript.Mode.READ_WRITE, POP_ID_LUA, RScript.ReturnType.VALUE,
                Arrays.asList(timeoutQueueName,queueName,dataHashName,channelName),
                System.currentTimeMillis()+getExecutionTimeoutMs(),id);
        if (result == null){
            return null;
        }
        RAckQueueData<V> data = new RAckQueueData<>();
        data.setData(result);
        data.setId(id);
        return data;
    }

    /**
     * 从队列末尾弹出一个数据 （数据从队列末尾取出数据）
     * @author Marsh
     * @date 2022-06-14
     * @return V
     */
    public RAckQueueData<V> pop(){
        List<RAckQueueData<V>> results = pop(1);
        return results != null && results.size() > 0 ? results.get(0) : null;
    }

    /**
     * 批量取出数据的lua脚本
     */
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
                            "redis.call('publish', KEYS[3], '"+CMD_TIMEOUT+"'..':'..timeout[2]); " +
                        "end; " +
                    "end; " +
                    "return res; " +
                "end; " +
                "redis.call('ZADD',KEYS[2],ARGV[1],v); " +
                "table.insert(res, v);" +
                "i = i + 1; " +
            "end; " +
            "local timeout = redis.call('zrange', KEYS[2], 0, 0, 'WITHSCORES'); " +
            "if timeout[1] ~= nil and tonumber(ARGV[1]) <= tonumber(timeout[2]) then " +
                "redis.call('publish', KEYS[3], '"+CMD_TIMEOUT+"'..':'..timeout[2]); " +
            "end; " +
            "return res; ";
    /**
     * 批量取出数据
     * @author Marsh
     * @date 2022-06-14
     * @param size
     * @return java.util.List<V>
     */
    public List<RAckQueueData<V>> pop(int size){
        RScript script = redisson.getScript(StringCodec.INSTANCE);
        List<String> ids = script.eval(RScript.Mode.READ_WRITE, POP_LUA, RScript.ReturnType.MULTI,
                Arrays.asList(queueName, timeoutQueueName,channelName),
                System.currentTimeMillis()+getExecutionTimeoutMs(),size);
        List<RAckQueueData<V>> results = new ArrayList<>();
        if (ids != null && ids.size() > 0){
            RMap<String, V> map1 = redisson.getMap(dataHashName, codec);
            Set<String> query = new HashSet();
            query.addAll(ids);
            Map<String, V> datas = map1.getAll(query);
            if (datas != null && datas.size() > 0){
                datas.forEach((id,v) -> {
                    RAckQueueData<V> data = new RAckQueueData();
                    data.setId(id);
                    data.setData(v);
                    results.add(data);
                });
            }
        }
        return results;
    }

    /**
     * 确认消息已成功处理
     * @author Marsh
     * @date 2022-06-15
     * @param id
     */
    public boolean ack(String id){
        List<String> ids = new ArrayList<>();
        ids.add(id);
        List<String> results = ack(ids);
        return results != null && results.size() > 0;
    }

    /**
     * 回滚数据的lua脚本
     */
    private static final String ACK_LUA =
            "local results = {};" +
            "for i = 1, #ARGV, 1 do " +
                "local flag = redis.call('ZREM',KEYS[1],ARGV[i]);" +
                "if flag > 0 then " +
                    "redis.call('HDEL',KEYS[2],ARGV[i]);" +
                    "redis.call('HDEL',KEYS[3],ARGV[i]);" +
                    "table.insert(results, ARGV[i]);" +
                "end;" +
            "end;" +
            "local timeout = redis.call('zrange', KEYS[1], 0, 0, 'WITHSCORES'); " +
            "if timeout[1] ~= nil then " +
                "redis.call('publish', KEYS[4], '"+CMD_TIMEOUT+"'..':'..timeout[2]); " +
            "end; " +
            "return results;";

    /**
     * 批量确认消息已处理
     * @author Marsh
     * @date 2022-06-15
     * @param ids
     */
    public List<String> ack(List<String> ids){
        List<String> results = new ArrayList<>();
        if (ids == null || ids.size() == 0){
            return results;
        }

        RScript script = redisson.getScript(StringCodec.INSTANCE);
        results = script.eval(RScript.Mode.READ_WRITE, ACK_LUA, RScript.ReturnType.MULTI,
                Arrays.asList(timeoutQueueName,dataHashName,retryName,channelName),
                ids.toArray());
        return results;
    }

    /**
     * 通过消息id将消息回滚,回滚次数共享getRetryNumber()
     * @author Marsh
     * @date 2022-06-15
     * @param id
     */
    public void unack(String id){
        List<String> ids = new ArrayList<>();
        ids.add(id);
        unack(ids);
    }

    /**
     * 回滚数据的lua脚本
     */
    private static final String UNACK_LUA =
            "local maxRetry = tonumber(ARGV[2]);"+
            "for i = 3, #ARGV, 1 do " +
                    "local v = ARGV[i];" +
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
                    "redis.call('ZREM',KEYS[1],v);" +
            "end;";
    /**
     * 通过消息id将消息回滚,回滚次数共享getRetryNumber()
     * @author Marsh
     * @date 2022-06-15
     * @param ids
     */
    public void unack(List<String> ids){
        if (ids == null || ids.size() == 0){
            return;
        }
        List<String> params = new ArrayList<>();
        params.add(System.currentTimeMillis()+"");
        params.add(getRetryNumber()+"");
        params.addAll(ids);
        commandExecutor.evalWriteAsync(getName(), StringCodec.INSTANCE, RedisCommands.EVAL_VOID,
                UNACK_LUA,
                Arrays.asList(timeoutQueueName,queueName,retryName,errorName,channelName),
                params.toArray());
    }

    @Override
    public void destroy(){
        RTopic topic = topicCache.get(getName());
        if (topic != null){
            topic.removeAllListeners();
        }
    }
}
