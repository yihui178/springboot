package com.example.mybatis.common;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * @author yihui
 */
@Setter
@Getter
public class SpringException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    private String msg;
    private int code = 500;

    public SpringException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public SpringException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public SpringException(String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public SpringException(String msg, int code, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }

}
