package com.example.mybatis.common;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.stream.Collectors;

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
     * 🔥 处理参数校验异常（@Valid 注解校验失败）
     * 优化版：返回更友好的错误提示
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public HttpResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("参数校验异常: {}", e.getMessage());
        BindingResult bindingResult = e.getBindingResult();

        // 收集所有字段错误信息
        Map<String, String> errorMap = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // 🔥 拼接所有错误信息（适配前端 ElMessage 展示）
        String errorMessage = errorMap.values().stream()
                .collect(Collectors.joining("；"));

        return HttpResult.error(HttpStatus.BAD_REQUEST.value(), errorMessage);
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
                "参数 %s 类型不匹配，期望类型: %s",
                e.getName(),
                e.getRequiredType().getSimpleName()
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
        return HttpResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "数据库操作失败，请联系管理员");
    }

    /**
     * 处理IO异常
     */
    @ExceptionHandler(IOException.class)
    public HttpResult handleIOException(IOException e) {
        log.error("IO异常: {}", e.getMessage(), e);
        return HttpResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件操作失败，请检查文件是否存在或有权限");
    }

    /**
     * 🔥 处理JWT过期异常（统一返回格式）
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public HttpResult handleExpiredJwtException(ExpiredJwtException e) {
        log.error("JWT令牌已过期: {}", e.getMessage());
        return HttpResult.error(HttpStatus.UNAUTHORIZED.value(), "令牌已过期，请重新登录");
    }

    /**
     * 🔥 处理唯一约束冲突（如 name 唯一）
     */
    @ExceptionHandler({DataIntegrityViolationException.class, org.springframework.dao.DuplicateKeyException.class})
    public HttpResult handleDuplicateKeyException(Exception e) {
        log.error("唯一约束冲突: {}", e.getMessage());

        // 🔥 智能识别错误类型
        String message = "操作失败：数据重复";
        if (e.getMessage().contains("highlight")) {
            message = "亮点名称重复，请修改后重试";
        } else if (e.getMessage().contains("course")) {
            message = "课程名称重复，请修改后重试";
        }

        return HttpResult.error(409, message);
    }

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