package com.huyuhui.quiz.common;

/**
 * 统一响应结构
 *
 * <pre>
 * { "code": 0, "message": "ok", "data": {...} }
 * </pre>
 * code = 0 表示成功，其余为业务错误码。
 */
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;

    public ApiResult() {}

    public ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(0, "ok", null);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}