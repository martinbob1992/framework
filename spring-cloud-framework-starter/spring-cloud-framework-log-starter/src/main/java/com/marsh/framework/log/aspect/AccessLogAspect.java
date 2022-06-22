package com.marsh.framework.log.aspect;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.marsh.common.exception.BaseException;
import com.marsh.framework.log.interfaces.AccessLogExtendService;
import com.marsh.framework.log.vo.AccessLogVO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 *
 * @author marsh
 * @date 2019/6/28 15:20
 **/
@Aspect
@Configuration
@ConditionalOnWebApplication
public class AccessLogAspect {

    private static Logger log = LoggerFactory.getLogger(AccessLogAspect.class);

    @Autowired
    private AccessLogExtendService accessLogExtendService;

    @Pointcut(" execution(public com.marsh.common.response.R com.marsh..controller..*(..)) ")
    public void controllerAop() {
    }

    @Around("controllerAop()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Long startTime = System.currentTimeMillis();
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        Object obj = null;
        try {
            obj = point.proceed();
        } catch (Exception e){
            AccessLogVO accessLog = createAccessLog(point);
            accessLog.setErrorArgs(getMethodArgsString(point.getArgs()));
            if (e instanceof BaseException) {
                accessLog.setStatue(400);
                accessLog.setException(e.getMessage());
            } else {
                accessLog.setStatue(500);
                accessLog.setException("服务器内部异常!");
            }
            accessLog.setTime(System.currentTimeMillis() - startTime);
            response.addHeader("ctime", accessLog.getTime() + " ms");
            printLog(accessLog);
            throw e;
        }
        AccessLogVO logVO = createAccessLog(point);
        logVO.setTime(System.currentTimeMillis() - startTime);
        response.addHeader("ctime", logVO.getTime() + " ms");
        printLog(logVO);
        return obj;
    }

    private String getMethodArgsString(Object[] args) {
        StringBuffer sb = new StringBuffer("[");
        for (int i = 0;i< args.length;i++){
            if (i != 0){
                sb.append(",");
            }
            Object arg = args[i];
            if (arg == null){
                sb.append("null");
                continue;
            }
            if (arg instanceof String || arg instanceof Integer || arg instanceof Long ||
                    arg instanceof Float || arg instanceof Double){
                sb.append(arg.toString());
                continue;
            }
            if (arg instanceof HttpServletRequest){
                sb.append("javax.servlet.http.HttpServletRequest");
                continue;
            }
            if (arg instanceof HttpServletResponse){
                sb.append("javax.servlet.http.HttpServletResponse");
                continue;
            }
            try {
                sb.append(JSONUtil.parse(arg));
            } catch (Exception e){
                if (log.isDebugEnabled()){
                    log.debug(ExceptionUtil.stacktraceToString(e));
                }
                sb.append(arg.getClass()).append("|").append(arg.toString());
            }
        }
        return sb.append("]").toString();
    }

    private AccessLogVO createAccessLog(ProceedingJoinPoint point){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        AccessLogVO logVO = AccessLogVO.instance(request);
        return logVO;
    }

    /**
     * 打印日志
     * @author Marsh
     * @date 2021/8/27 0027
     * @param logVO
     */
    private void printLog(AccessLogVO logVO){
        JSON result = JSONUtil.parse(logVO);
        Map<String, Object> extendParams = accessLogExtendService.getExtendParams();
        if (extendParams != null && extendParams.size() > 0){
            extendParams.forEach((k,v) -> {
                result.putByPath(k,v);
            });
        }
        log.info("[access] : {}", result);
    }

}
