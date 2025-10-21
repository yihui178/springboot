package com.example.mybatis.controller;

import com.example.mybatis.exception.SpringException;
import com.example.mybatis.http.HttpResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * 全局异常处理
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(SpringException.class)
    public HttpResult handleSpringException(SpringException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return HttpResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid 注解校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public HttpResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("参数校验异常: {}", e.getMessage(), e);
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errorMap = new HashMap<>();
        // 收集所有字段校验错误
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return HttpResult.error(
                HttpStatus.BAD_REQUEST.value(),
                "参数校验失败",
                errorMap
        );
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public HttpResult handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("缺少请求参数: {}", e.getMessage(), e);
        String message = String.format("缺少必要的请求参数: %s", e.getParameterName());
        return HttpResult.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public HttpResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("参数类型不匹配: {}", e.getMessage(), e);
        String message = String.format(
                "参数 %s 类型不匹配，期望类型: %s，实际值: %s",
                e.getName(),
                e.getRequiredType().getSimpleName(),
                e.getValue()
        );
        return HttpResult.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public HttpResult handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.error("资源未找到: {}", e.getMessage(), e);
        String message = String.format("请求的资源不存在: %s %s", e.getHttpMethod(), e.getRequestURL());
        return HttpResult.error(HttpStatus.NOT_FOUND.value(), message);
    }

    /**
     * 处理数据库异常
     */
    @ExceptionHandler(SQLException.class)
    public HttpResult handleSQLException(SQLException e) {
        log.error("数据库异常: {}", e.getMessage(), e);
        // 生产环境可根据实际情况返回更友好的信息，避免暴露数据库细节
        String message = "数据库操作失败，请联系管理员";
        return HttpResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    /**
     * 处理IO异常
     */
    @ExceptionHandler(IOException.class)
    public HttpResult handleIOException(IOException e) {
        log.error("IO异常: {}", e.getMessage(), e);
        String message = "文件操作失败，请检查文件是否存在或有权限";
        return HttpResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

//    /**
//     * 处理参数校验异常
//     */
//    @ExceptionHandler(IllegalArgumentException.class)
//    public HttpResult handleIllegalArgumentException(IllegalArgumentException e) {
//        log.error("参数校验异常: {}", e.getMessage(), e);
////        String message = "文件操作失败，请检查文件是否存在或有权限";
//        return HttpResult.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
//    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public HttpResult handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        return HttpResult.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "系统发生了未知错误，请联系管理员"
        );
    }
}