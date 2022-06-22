package com.marsh.framework.log.context;

import com.marsh.framework.log.context.strategy.ILogContextHolderStrategy;
import com.marsh.framework.log.context.strategy.MDCLogContextHolderStrategy;
import com.marsh.framework.log.context.strategy.ThreadLocalLogContextHolderStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;

/**
 * @author marsh
 */
@Slf4j
public class LogContextHolder {

    private static ILogContextHolderStrategy strategy;
    public static final String MODE_THREADLOCAL = "MODE_THREADLOCAL";
    public static final String MODE_MDC = "MODE_MDC";
    private static String strategyName = MODE_MDC;

    static {
        initialize();
    }

    private static void initialize() {
        if (null == strategyName || "".equals(strategyName)){
            strategyName = MODE_THREADLOCAL;
        }
        if (MODE_MDC.equals(strategyName)){
            strategy = new MDCLogContextHolderStrategy();
        } else if (MODE_THREADLOCAL.equals(strategyName)){
            strategy = new ThreadLocalLogContextHolderStrategy();
        } else {
            try {
                Class<?> clazz = Class.forName(strategyName);
                Constructor<?> customStrategy = clazz.getConstructor();
                strategy = (ILogContextHolderStrategy) customStrategy.newInstance();
            }
            catch (Exception ex) {
                ReflectionUtils.handleReflectionException(ex);
            }
        }
    }

    /**
     * 获取当前线程的logContext，如果没有则生成一个
     * @return
     */
    public static LogContext getContextIfAbsentCreate(){
        return strategy.getContextIfAbsentCreate();
    }

    public static void clearContext(){
        strategy.clearContext();
    }

    /**
     * 如果不存在则进行设置，返回值永远返回上一次设置的值，如果返回null则表示第一次设值
     * @author Marsh
     * @date 2022-06-17
     * @param logContext
     * @return com.marsh.framework.log.context.LogContext
     */
    public static LogContext putIfAbsent(LogContext logContext){
        return strategy.putIfAbsent(logContext);
    }

    public static LogContext getContext(){
        return strategy.getContext();
    }

    public static boolean hasContext(){
        return strategy.hasContext();
    }

    public static void setStrategyName(String strategyName) {
        LogContextHolder.strategyName = strategyName;
        initialize();
    }


}
