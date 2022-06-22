package com.marsh.framework.log.vo;

import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * 访问日志类
 * @author marsh
 */
@Getter
@Setter
public class AccessLogVO implements Serializable {

    /**
     * 头信息中的用户代理
     */
    private String userAgent;
    /**
     * 请求ip
     */
    private String ip;

    /**
     * 请求地址
     */
    private String url;
    /**
     * 请求方式
     */
    private String method;
    /**
     * 请求参数
     */
    private String params;
    /**
     * 请求用时
     */
    private Long time;
    /**
     * 请求状态
     */
    private Integer statue = 200;
    /**
     * 报错后的参数记录位置
     */
    private String errorArgs;
    /**
     * 异常信息
     */
    private String exception;

    /**
     * Cookie中的批次
     */
    private String lvt;


    private String openId;

    /**
     * 来源
     */
    private String referer;

    public static AccessLogVO instance(HttpServletRequest request){
        AccessLogVO logVO = new AccessLogVO();
        logVO.setMethod(request.getMethod());
        logVO.setParams(HttpUtil.toParams(request.getParameterMap()));
        logVO.setUserAgent(request.getHeader("user-agent"));
        logVO.setUrl(URLUtil.getPath(request.getRequestURI()));

        logVO.setIp(ServletUtil.getClientIP(request));
        logVO.setReferer(request.getHeader("referer"));

        if(!ObjectUtils.isEmpty(request.getCookies())){
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().toLowerCase().startsWith("hm_lvt_")){
                    logVO.setLvt(cookie.getValue());
                    return logVO;
                }
            }
        }
        return logVO;
    }
}
