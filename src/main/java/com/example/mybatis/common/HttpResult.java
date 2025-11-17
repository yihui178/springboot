package com.example.mybatis.common;

import lombok.Data;

//@Data
//public class HttpResult {
//    private int code;
//    private String msg;
//    private Object data;
//
//    // 成功响应
//    public static HttpResult ok() {
//        return ok(null);
//    }
//
//    public static HttpResult ok(Object data) {
//        HttpResult result = new HttpResult();
//        result.setCode(0);
//        result.setMsg("操作成功");
//        result.setData(data);
//        return result;
//    }
//
//    // 错误响应
//    public static HttpResult error(int code, String msg) {
//
//        return error(code == 0 ? -1 : code, msg, null);
//    }

//    public static HttpResult error(int code, String msg, Object data) {
//        HttpResult result = new HttpResult();
//        result.setCode(code);
//        result.setMsg(msg);
//        result.setData(data);
//        return result;
//    }
//}


/**
 * 统一接口响应格式（适配 Vben Admin 前端要求）
 */
@Data
public class HttpResult<T> {
    /** 状态码：0=成功，非0=失败 */
    private int code;
    /** 业务数据 */
    private T data;
    /** 提示信息 */
    private String message;

    // 成功响应（无数据）
    public static <T> HttpResult<T> ok() {
        HttpResult<T> result = new HttpResult<>();
        result.setCode(0);
        result.setMessage("操作成功");
        return result;
    }

    // 成功响应（带数据）
    public static <T> HttpResult<T> ok(T data) {
        HttpResult<T> result = new HttpResult<>();
        result.setCode(0);
        result.setData(data);
        result.setMessage("操作成功");
        return result;
    }
    public static <T> HttpResult<T> ok(T data, String message) {
        HttpResult<T> result = new HttpResult<>();
        result.setCode(0);
        result.setData(data);
        result.setMessage(message); // 自定义成功消息
        return result;
    }


    // 失败响应
    public static <T> HttpResult<T> error(int code, String message) {
        HttpResult<T> result = new HttpResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}