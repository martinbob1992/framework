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
public class MyAckQueue1<T extends QueueData> extends RAckQueue<T> {

    public MyAckQueue1(RedissonClient redisson) {
        super((Redisson) redisson, "test1", new GsonCodec(MyQueueData.class));
    }

    public int getRetryNumber(){
        return 0;
    }

    @Override
    public Long getExecutionTimeoutMs(){
        return 2000L;
    }
}
