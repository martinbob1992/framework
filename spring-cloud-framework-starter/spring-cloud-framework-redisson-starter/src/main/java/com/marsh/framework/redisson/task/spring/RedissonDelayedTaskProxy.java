package com.marsh.framework.redisson.task.spring;

import com.marsh.framework.redisson.task.DelayedTaskTransferService;
import com.marsh.framework.redisson.task.RDelayedTask;
import com.marsh.framework.redisson.task.RDelayedTaskHandler;
import com.marsh.framework.redisson.task.RRedissonDelayedTask;
import lombok.Getter;
import org.redisson.Redisson;
import org.redisson.api.RFuture;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 延时任务代理类用于快速生产延时任务
 *
 * 使用如下方式快速创建一个延时任务实例类
 * @Component
 * public class TestDelayedTaskService extends RedissonDelayedTaskProxy<String> {
 *
 *     public TestDelayedTaskService(Redisson redisson) {
 *         super(redisson,"test",((taskId, msg) -> {
 *             System.out.println("任务id:"+taskId+"\t执行了任务:"+msg);
 *         }));
 *     }
 * }
 *
 * 在需要使用的地方
 * @Autowired
 * private TestDelayedTaskService testDelayedTaskService;
 * //发布一个1秒后执行的任务
 * testDelayedTaskService.offerAsync(IdUtil.randomUUID(),"任务内容:"+IdUtil.randomUUID(),System.currentTimeMillis()+1000);
 * //发布一个1分钟后执行的任务
 * testDelayedTaskService.offerAsync(IdUtil.randomUUID(),"任务内容:"+IdUtil.randomUUID(),1,TimeUnit.MINUTES);
 * //发布一个2022-02-14 17:00:00执行的任务
 * testDelayedTaskService.offerAsync(IdUtil.randomUUID(),"任务内容:"+IdUtil.randomUUID()
 *      ,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2022-02-14 17:00:00"));
 *
 * @author Marsh
 * @date 2022-02-14日 15:02
 */
public class RedissonDelayedTaskProxy<V> implements RDelayedTask<V> {

    private static final DelayedTaskTransferService DELAYED_TASK_TRANSFER_SERVICE = new DelayedTaskTransferService();
    private static final Codec DEFAULT_CODEC = StringCodec.INSTANCE;

    @Getter
    public final RRedissonDelayedTask<V> proxy;

    public RedissonDelayedTaskProxy(RRedissonDelayedTask<V> proxy){
        this.proxy = proxy;
    }

    public RedissonDelayedTaskProxy(Redisson redisson, String taskType, RDelayedTaskHandler<V> delayedTaskHandler){
        this(redisson,taskType,DEFAULT_CODEC,delayedTaskHandler);
    }

    public RedissonDelayedTaskProxy(Redisson redisson, String taskType, Codec codec, RDelayedTaskHandler<V> delayedTaskHandler){
        this(new RRedissonDelayedTask(taskType, codec,
                DELAYED_TASK_TRANSFER_SERVICE, redisson.getCommandExecutor(),delayedTaskHandler));
    }

    @Override
    public boolean tryCancel(String taskId) {
        return proxy.tryCancel(taskId);
    }

    @Override
    public void offer(String taskId, V data, Date schedule) {
        proxy.offer(taskId,data,schedule);
    }

    @Override
    public void offer(String taskId, V data, long schedule) {
        proxy.offer(taskId,data,schedule);
    }

    @Override
    public void offer(String taskId, V data, long time, TimeUnit timeUnit) {
        proxy.offer(taskId,data,time,timeUnit);
    }

    @Override
    public RFuture<Void> offerAsync(String taskId, V data, long time, TimeUnit timeUnit) {
        return proxy.offerAsync(taskId,data,time,timeUnit);
    }

    @Override
    public RFuture<Void> offerAsync(String taskId, V data, long schedule) {
        return proxy.offerAsync(taskId,data,schedule);
    }

    @Override
    public RFuture<Void> offerAsync(String taskId, V data, Date schedule) {
        return proxy.offerAsync(taskId,data,schedule);
    }

    @Override
    public void destroy() {
        proxy.destroy();
    }
}
