package com.marsh.common.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 接口统一返回对象
 * @author marsh
 * @date 2020/6/4 11:14
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求正常的状态码
     */
    public static final int RESPONSE_SUCCESS = 200;
    /**
     * 用户访问了需要登陆的资源
     */
    public static final int RESPONSE_NEED_LOGIN = 401;
    /**
     * 未知的异常状态码（服务器代码异常）
     */
    public static final int RESPONSE_UNKNOWN_ERROR = 500;
    /**
     * 程序主动抛出异常的状态码
     */
    public static final int RESPONSE_THROW_ERROR = 501;
    /**
     * SQL异常状态码
     */
    public static final Integer RESPONSE_SQL_ERROR = 502;

    /**
     * 接口返回结果
     */
    protected T result;
    /**
     * 接口请求状态  200正常，非200均是错误
     */
    protected int status;
    /**
     * 用于客户端回显的信息，由于该字段是给用户看的，请勿书写代码，英文信息
     */
    protected String message;


    public R(T result){
        this.result = result;
        this.status = RESPONSE_SUCCESS;
    }

    public R(Exception exception){
        this.status = RESPONSE_THROW_ERROR;
    }

    public R(int status, String message){
        this.status = status;
        this.message = message;
    }


    public static <T> R<T> ok(T data){
        return new R(data);
    }

    public static <T> R<T> error(int status, String message){
        return new R(status,message);
    }

    public static <T> R<T> error(Exception exception){
        return new R(exception);
    }

}
