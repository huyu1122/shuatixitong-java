package com.huyuhui.quiz.common;

/**
 * 业务异常：由全局异常处理器统一转为 JSON 响应
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(4000, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}