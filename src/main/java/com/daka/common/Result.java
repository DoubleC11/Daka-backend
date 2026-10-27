package com.daka.common;

import lombok.Data;

/**
 * 统一响应体：{ code, msg, data }
 * code = 0 表示成功，其余为业务错误码。
 */
@Data
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        r.data = null;
        return r;
    }
}
