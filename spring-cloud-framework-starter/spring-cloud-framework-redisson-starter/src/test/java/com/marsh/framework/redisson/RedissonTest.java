package com.marsh.framework.redisson;

import cn.hutool.core.lang.generator.UUIDGenerator;
import com.marsh.framework.redisson.queue.MyAckQueue;
import com.marsh.framework.redisson.queue.MyAckQueue1;
import com.marsh.framework.redisson.queue.MyQueueData;
import com.marsh.framework.redisson.queue.RAckQueueData;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Marsh
 * @date 2022-06-02日 16:27
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedissonTest.class)
@SpringBootApplication
public class RedissonTest {

    @Autowired
    private MyAckQueue<MyQueueData> myAckQueue;
    @Autowired
    private MyAckQueue1<MyQueueData> myAckQueue1;
    @Test
    public void a(){
        MyQueueData d1 = new MyQueueData();
        d1.setId(new UUIDGenerator().next());
        d1.setName("d1");
        myAckQueue.push(d1);

        MyQueueData d2 = new MyQueueData();
        d2.setId(new UUIDGenerator().next());
        d2.setName("d2");
        myAckQueue1.push(d2);
    }

    @Test
    public void b(){

        System.out.println(myAckQueue.pop(10));
        System.out.println(myAckQueue1.pop());

    }

    @Test
    public void c(){

        a();
        a();
        a();


    }

    @SneakyThrows
    @Test
    public void d(){
        System.out.println(myAckQueue.pop());
        System.out.println(myAckQueue1.pop());
        Thread.sleep(1000L);
        System.out.println(myAckQueue.pop());
        System.out.println(myAckQueue1.pop());
        Thread.sleep(1000L);
        System.out.println(myAckQueue.pop());
        System.out.println(myAckQueue1.pop());
        Thread.sleep(1000L);
        System.out.println(myAckQueue.pop());
        System.out.println(myAckQueue1.pop());
        Thread.sleep(1000L);
        System.out.println(myAckQueue.pop());
        System.out.println(myAckQueue1.pop());

        Thread.sleep(30000L);
    }

    @Test
    public void e(){
        /*RAckQueueData<MyQueueData> pop = myAckQueue.pop();
        if (pop != null){
            System.out.println(pop);
            myAckQueue.ack(pop.getId());
        }*/
        for (int i = 0;i < 4;i++){
            RAckQueueData<MyQueueData> pop1 = myAckQueue.pop();
            if (pop1 != null){
                System.out.println(pop1);
                myAckQueue.unack(pop1.getId());
            }
        }

    }

}
