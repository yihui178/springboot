package com.example.mybatis.common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;
/**
 * 全局异常处理器
 * @author yihui
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // ==================== 业务异常 ====================
    @ExceptionHandler(SpringException.class)
    public HttpResult<Void> handleSpringException(SpringException e) {
        log.error("业务异常: {}", e.getMessage());
        return HttpResult.error(e.getCode(), e.getMessage());
    }
    // ==================== 参数校验异常 ====================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public HttpResult<Void> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = bindingResult.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return HttpResult.error(HttpStatus.BAD_REQUEST.value(), errorMessage);
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public HttpResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return HttpResult.error(HttpStatus.BAD_REQUEST.value(), "缺少必要的请求参数: " + e.getParameterName());
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public HttpResult<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {} -> {}", e.getName(), e.getValue());
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知类型";
        return HttpResult.error(HttpStatus.BAD_REQUEST.value(),
                String.format("参数 %s 类型不匹配，期望类型: %s", e.getName(), requiredType));
    }
    // ==================== 数据库异常 ====================
    @ExceptionHandler({DataIntegrityViolationException.class, DuplicateKeyException.class})
    public HttpResult<Void> handleDuplicateKey(Exception e) {
        log.error("数据库唯一约束冲突: {}", e.getMessage());
        String message = parseConstraintMessage(e.getMessage());
        return HttpResult.error(HttpStatus.CONFLICT.value(), message);
    }
    @ExceptionHandler(SQLException.class)
    public HttpResult<Void> handleSqlException(SQLException e) {
        log.error("数据库异常: SQLState={}, ErrorCode={}", e.getSQLState(), e.getErrorCode(), e);
        return HttpResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "数据库操作失败，请联系管理员");
    }
    // ==================== 其他异常 ====================
    @ExceptionHandler(NoHandlerFoundException.class)
    public HttpResult<Void> handleNotFound(NoHandlerFoundException e) {
        log.warn("资源未找到: {} {}", e.getHttpMethod(), e.getRequestURL());
        return HttpResult.error(HttpStatus.NOT_FOUND.value(),
                String.format("请求的资源不存在: %s %s", e.getHttpMethod(), e.getRequestURL()));
    }
    @ExceptionHandler(IOException.class)
    public HttpResult<Void> handleIoException(IOException e) {
        log.error("IO异常: {}", e.getMessage(), e);
        return HttpResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件操作失败，请检查文件是否存在或有权限");
    }
    @ExceptionHandler(Exception.class)
    public HttpResult<Void> handleException(Exception e) {
        log.error("未知异常: ", e);
        return HttpResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统发生了未知错误，请联系管理员");
    }
    // ==================== 私有辅助方法 ====================
    private String parseConstraintMessage(String errorMessage) {
        if (errorMessage == null) {
            return "操作失败：数据重复";
        }
        String lowerMsg = errorMessage.toLowerCase();
        if (lowerMsg.contains("user") && lowerMsg.contains("name")) {
            return "用户名已存在，请更换其他用户名";
        }
        if (lowerMsg.contains("role") && lowerMsg.contains("code")) {
            return "角色代码已存在，请修改后重试";
        }
        if (lowerMsg.contains("permission") && lowerMsg.contains("code")) {
            return "权限代码已存在，请修改后重试";
        }
        if (lowerMsg.contains("highlight") && lowerMsg.contains("name")) {
            return "亮点名称重复，请修改后重试";
        }
        return "操作失败：数据重复";
    }
}