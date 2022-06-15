package com.marsh.framework.redisson.queue;

import lombok.Data;

/**
 * @author Marsh
 * @date 2022-06-14日 15:28
 */
@Data
public class RAckQueueData<V> {
    /**
     * 消息id,主要用来标识这个消息用来进行ack和unack操作
     */
    private String id;

    /**
     * 存储的消息数据
     */
    private V data;

}
