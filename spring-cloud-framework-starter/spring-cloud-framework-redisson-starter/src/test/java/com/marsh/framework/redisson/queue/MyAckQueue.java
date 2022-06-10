package com.marsh.framework.redisson.queue;

import com.marsh.framework.redisson.codec.GsonCodec;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * @author Marsh
 * @date 2022-06-02日 17:05
 */
@Component
public class MyAckQueue<T extends QueueData> extends RAckQueue<T> {

    public MyAckQueue(RedissonClient redisson) {
        super((Redisson) redisson, "test", new GsonCodec(MyQueueData.class));
    }

    @Override
    public Long getExecutionTimeoutMs(){
        return 3000L;
    }

    /**
     * 失败重试次数，超过重试次数的会将其写到错误队列中，后续人工检查进行处理
     * 默认5次，如果该值为-1则无限重试
     * @return
     */
    @Override
    public int getRetryNumber(){
        return 3;
    }
}
