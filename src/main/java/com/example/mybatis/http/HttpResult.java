package com.example.mybatis.http;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class HttpResult {
    private int code;
    private String msg;
    private Object data;

    // 成功响应
    public static HttpResult ok() {
        return ok(null);
    }

    public static HttpResult ok(Object data) {
        HttpResult result = new HttpResult();
        result.setCode(HttpStatus.OK.value());
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 错误响应
    public static HttpResult error(int code, String msg) {
        return error(code, msg, null);
    }

    public static HttpResult error(int code, String msg, Object data) {
        HttpResult result = new HttpResult();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }
}