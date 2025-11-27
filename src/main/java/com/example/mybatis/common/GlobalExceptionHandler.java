package com.example.mybatis.common;
import io.jsonwebtoken.ExpiredJwtException;
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
 * 统一处理所有异常并返回标准的 HttpResult 响应
 *
 * @author yihui
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // ==================== 业务异常 ====================
    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(SpringException.class)
    public HttpResult<Void> handleSpringException(SpringException e) {
        log.error("业务异常: {}", e.getMessage());
        return HttpResult.error(e.getCode(), e.getMessage());
    }
    // ==================== 参数校验异常 ====================
    /**
     * 处理 @Valid 参数校验异常
     * 优化：返回所有字段的错误信息（用分号分隔）
     */
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
    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public HttpResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return HttpResult.error(
                HttpStatus.BAD_REQUEST.value(),
                "缺少必要的请求参数: " + e.getParameterName()
        );
    }
    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public HttpResult<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {} -> {}", e.getName(), e.getValue());

        String requiredType = e.getRequiredType() != null
                ? e.getRequiredType().getSimpleName()
                : "未知类型";

        return HttpResult.error(
                HttpStatus.BAD_REQUEST.value(),
                String.format("参数 %s 类型不匹配，期望类型: %s", e.getName(), requiredType)
        );
    }
    // ==================== 认证授权异常 ====================
    /**
     * 处理 JWT 过期异常
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public HttpResult<Void> handleExpiredJwt(ExpiredJwtException e) {
        log.warn("JWT令牌已过期");
        return HttpResult.error(HttpStatus.UNAUTHORIZED.value(), "令牌已过期，请重新登录");
    }
    // ==================== 数据库异常 ====================
    /**
     * 处理唯一约束冲突异常
     * 优化：支持更多表的智能识别
     */
    @ExceptionHandler({DataIntegrityViolationException.class, DuplicateKeyException.class})
    public HttpResult<Void> handleDuplicateKey(Exception e) {
        log.error("数据库唯一约束冲突: {}", e.getMessage());

        String message = parseConstraintMessage(e.getMessage());
        return HttpResult.error(HttpStatus.CONFLICT.value(), message);
    }
    /**
     * 处理 SQL 异常
     */
    @ExceptionHandler(SQLException.class)
    public HttpResult<Void> handleSQLException(SQLException e) {
        log.error("数据库异常: SQLState={}, ErrorCode={}", e.getSQLState(), e.getErrorCode(), e);
        return HttpResult.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "数据库操作失败，请联系管理员"
        );
    }
    // ==================== 其他异常 ====================
    /**
     * 处理 404 异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public HttpResult<Void> handleNotFound(NoHandlerFoundException e) {
        log.warn("资源未找到: {} {}", e.getHttpMethod(), e.getRequestURL());
        return HttpResult.error(
                HttpStatus.NOT_FOUND.value(),
                String.format("请求的资源不存在: %s %s", e.getHttpMethod(), e.getRequestURL())
        );
    }
    /**
     * 处理 IO 异常
     */
    @ExceptionHandler(IOException.class)
    public HttpResult<Void> handleIOException(IOException e) {
        log.error("IO异常: {}", e.getMessage(), e);
        return HttpResult.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "文件操作失败，请检查文件是否存在或有权限"
        );
    }
    /**
     * 处理所有未捕获的异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public HttpResult<Void> handleException(Exception e) {
        log.error("未知异常: ", e);
        return HttpResult.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "系统发生了未知错误，请联系管理员"
        );
    }
    // ==================== 私有辅助方法 ====================
    /**
     * 解析数据库唯一约束冲突的错误信息
     * 支持更灵活的表名识别
     */
    private String parseConstraintMessage(String errorMessage) {
        if (errorMessage == null) {
            return "操作失败：数据重复";
        }
        String lowerMsg = errorMessage.toLowerCase();
        // ✅ 只处理真正有唯一约束的字段
        if (lowerMsg.contains("user")) {
            if (lowerMsg.contains("name") || lowerMsg.contains("uk_name") || lowerMsg.contains("ux_user_name")) {
                return "用户名已存在，请更换其他用户名";
            }
        } else if (lowerMsg.contains("role")) {
            if (lowerMsg.contains("code")) {
                return "角色代码已存在，请修改后重试";
            }
        } else if (lowerMsg.contains("permission")) {
            if (lowerMsg.contains("code")) {
                return "权限代码已存在，请修改后重试";
            }
        } else if (lowerMsg.contains("highlight")) {
            if (lowerMsg.contains("name")) {
                return "亮点名称重复，请修改后重试";
            }
        } else if (lowerMsg.contains("duplicate entry")) {
            // 通用兜底：提取重复值
            String duplicateValue = extractDuplicateValue(errorMessage);
            return duplicateValue != null
                    ? String.format("数据重复：%s 已存在", duplicateValue)
                    : "操作失败：数据重复";
        }
        return "操作失败：数据重复";
    }
    /**
     * 从错误消息中提取重复的值
     * 例如："Duplicate entry 'admin' for key 'username'" -> "admin"
     */
    private String extractDuplicateValue(String errorMessage) {
        try {
            int start = errorMessage.indexOf("'") + 1;
            int end = errorMessage.indexOf("'", start);
            if (start > 0 && end > start) {
                return errorMessage.substring(start, end);
            }
        } catch (Exception ignored) {
            // 忽略解析异常
        }
        return null;
    }
}