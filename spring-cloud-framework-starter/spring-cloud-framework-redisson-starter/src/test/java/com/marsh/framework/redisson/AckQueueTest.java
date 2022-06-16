package com.marsh.framework.redisson;

import cn.hutool.core.util.IdUtil;
import com.marsh.framework.redisson.codec.GsonCodec;
import com.marsh.framework.redisson.queue.RAckQueue;
import com.marsh.framework.redisson.queue.RAckQueueData;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Marsh
 * @date 2022-06-16日
 */
//如果需要测试开启这2个注解即可
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@EnableAutoConfiguration
public class AckQueueTest {

    /**
     * 使用的时候自行修改下这2个配置
     */
    private static String redisAddress = "redis://127.0.0.1:6379";
    private static String redisPassword = "123456";

    @Configuration
    @ConditionalOnMissingBean(Redisson.class)
    public static class RedissonConfig {
        @Bean
        public Redisson redisson(){
            Config config = new Config();
            config.useSingleServer().setAddress(redisAddress).setPassword(redisPassword);
            return (Redisson) Redisson.create(config);
        }
    }

    /**
     * 自定义队列中的消息实体
     */
    @Data
    public class MyQueueData {
        private String id;
        private String name;
    }

    /**
     * 创建一个ack队列
     * @param <V>
     */
    @Component
    public static class MyAckQueue<V> extends RAckQueue<V> {

        public MyAckQueue(RedissonClient redisson) {
            // 这里指定了队列名称demo,实体类的编码解码器
            super((Redisson) redisson, "demo", new GsonCodec(MyQueueData.class));
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

        /**
         * 当队列中投递了消息时，触发这个方法，子类可以通过重写这个方法来实现对消息的处理
         * @author Marsh
         * @date 2022-06-14
         * @param id
         */
        public void onMessage(String id){
            System.out.println(getName()+"\t"+"onMessage('"+id+"')");
        }

        /**
         * 当消息重试次数达到最大被放入到失败队列时，这个方法会被触发
         * @author Marsh
         * @date 2022-06-14
         * @param id
         */
        public void onError(String id){
            System.out.println(getName()+"\t"+"onError('"+id+"')");
        }
    }

    @Autowired
    private MyAckQueue<MyQueueData> myAckQueue;

    @Test
    public void add(){
        MyQueueData data = new MyQueueData();
        data.setId(IdUtil.fastUUID());
        data.setName("name");
        String msgId = myAckQueue.push(data);
        System.out.println("投递生成的消息id:"+msgId);
    }


    @Test
    public void pop(){
        // 这里是从队列中按照插入顺序进行消费,除了这个方法外还支持批量消费和消费指定消息id的数据
        RAckQueueData<MyQueueData> data = myAckQueue.pop();
        if (data == null){
            System.out.println("队列中已经没有消息了!");
        } else {
            System.out.println(data);
            try {
                System.out.println("模拟进行数据处理业务!");
                boolean flag = myAckQueue.ack(data.getId());
                System.out.println("数据ack状态:"+(flag?"成功":"失败"));
            } catch (Exception e){
                myAckQueue.unack(data.getId());
            }
        }
    }

    @SneakyThrows
    @Test
    public void timeout(){
        RAckQueueData<MyQueueData> data = myAckQueue.pop();
        if (data == null){
            System.out.println("队列中已经没有消息了,请至少添加一条消息!");
        } else {
            System.out.println(data);
            try {
                System.out.println("模拟进行数据处理业务!");
                // 这里让业务的处理时间比超时时间多1s，那么这个数据会重新投放到队列中
                Thread.sleep(myAckQueue.getExecutionTimeoutMs()+1000L);
                boolean flag = myAckQueue.ack(data.getId());
                System.out.println("数据ack状态:"+(flag?"成功":"失败"));
            } catch (Exception e){
                myAckQueue.unack(data.getId());
            }
        }
    }

    @Test
    public void error(){
        MyQueueData data = new MyQueueData();
        data.setId(IdUtil.fastUUID());
        data.setName("name");
        String msgId = myAckQueue.push(data);
        System.out.println("投递生成的消息id:"+msgId);
        System.out.println("---------------------------------------------");
        // 这里模拟让消息不停失败到最大重试次数
        // 关于 myAckQueue.getRetryNumber()+2 的解释
        // myAckQueue.getRetryNumber() + 2 等价于 1 + myAckQueue.getRetryNumber() + 1
        // 第一次是正常访问数据,然后myAckQueue.getRetryNumber()次由于超时将任务重新投递,
        // 最后又多访问一次,是为了展示这个消息已经被放到错误队列中,无法在通过pop进行消费了
        // ps：如果一个消息消费了N多次一直都是失败的（这里的失败一种是超时，一种是由于异常导致调用了unack方法）,
        // 那么就算在重试多少次也还是失败的,这种情况需要将这个数据从主队列中排除出去,进行人工干预
        for (int i = 0;i< myAckQueue.getRetryNumber()+2;i++){
            RAckQueueData<MyQueueData> d = myAckQueue.pop(msgId);
            if (d != null){
                try {
                    System.out.println("模拟进行数据处理业务!");
                    // 这里让业务的处理时间比超时时间多3s，那么这个数据会重新投放到队列中
                    Thread.sleep(myAckQueue.getExecutionTimeoutMs()+1000L);
                    boolean flag = myAckQueue.ack(d.getId());
                    System.out.println("数据ack状态:"+(flag?"成功":"失败"));
                } catch (Exception e){
                    myAckQueue.unack(d.getId());
                }
            } else {
                System.out.println("已经找不到这个消息了!");
            }
            System.out.println("---------------------------------------------");
        }
    }

}
