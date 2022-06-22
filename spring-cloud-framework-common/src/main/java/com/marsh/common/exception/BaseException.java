package com.marsh.common.exception;

import lombok.Data;

/**
 * @author Marsh
 * @date 2022-06-17日 11:27
 */
@Data
public class BaseException extends RuntimeException {

    private String msg;

    public BaseException(String msg){
        super(msg);
    }
}
