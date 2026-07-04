package com.tailoris.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.tailoris.common.result.Result;
import com.tailoris.common.result.ResultCode;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 - 修复 B-H14
 *
 * <p>补充全局异常处理的堆栈日志，确保生产环境异常可追溯。</p>
 *
 * <h3>关键改进</h3>
 * <ul>
 *   <li>B-H14: 异常日志输出完整堆栈</li>
 *   <li>区分业务异常和系统异常</li>
 *   <li>支持参数校验异常处理</li>
 *   <li>支持限流异常</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常: path={}, code={}, message={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        // 🔒 L-04修复: 业务异常返回 HTTP 400，而非 200
        return ResponseEntity.badRequest().body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    /**
     * 参数校验异常（@RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleValidationException(MethodArgumentNotValidException ex,
                                                                    HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: path={}, errors={}", request.getRequestURI(), errors);
        // 🔒 L-04修复: 参数校验异常返回 HTTP 400
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.BAD_REQUEST.getCode(), "参数校验失败: " + errors));
    }

    /**
     * 绑定异常（@ModelAttribute）
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Object>> handleBindException(BindException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("绑定异常: path={}, errors={}", request.getRequestURI(), errors);
        // 🔒 L-04修复: 绑定异常返回 HTTP 400
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.BAD_REQUEST.getCode(), "参数绑定失败: " + errors));
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("非法参数: path={}, message={}", request.getRequestURI(), ex.getMessage());
        // 🔒 L-04修复: 非法参数异常返回 HTTP 400
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.BAD_REQUEST.getCode(), ex.getMessage()));
    }

    /**
     * 空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Result<Object>> handleNullPointer(NullPointerException ex, HttpServletRequest request) {
        // 🔒 B-H14: 输出完整堆栈
        log.error("空指针异常: path={}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "系统内部错误"));
    }

    /**
     * 运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Object>> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        // 🔒 B-H14: 输出完整堆栈，便于排查
        log.error("运行时异常: path={}, type={}", request.getRequestURI(), ex.getClass().getName(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后再试"));
    }

    /**
     * Sa-Token 认证异常（未登录）
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Object>> handleNotLogin(NotLoginException ex, HttpServletRequest request) {
        log.warn("未登录访问: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(401, "请先登录"));
    }

    /**
     * Sa-Token 角色异常
     */
    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<Result<Object>> handleNotRole(NotRoleException ex, HttpServletRequest request) {
        log.warn("角色权限不足: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(403, "角色权限不足"));
    }

    /**
     * Sa-Token 权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<Result<Object>> handleNotPermission(NotPermissionException ex, HttpServletRequest request) {
        log.warn("操作权限不足: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(403, "操作权限不足"));
    }

    /**
     * Sa-Token 其他异常
     */
    @ExceptionHandler(SaTokenException.class)
    public ResponseEntity<Result<Object>> handleSaToken(SaTokenException ex, HttpServletRequest request) {
        log.warn("Sa-Token 异常: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(Result.fail(400, ex.getMessage()));
    }

    /**
     * HTTP 方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("HTTP方法不支持: path={}, method={}", request.getRequestURI(), request.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(405, "HTTP方法不支持"));
    }

    /**
     * 媒体类型不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("媒体类型不支持: path={}, contentType={}", request.getRequestURI(), request.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Result.fail(415, "请求格式不支持"));
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("参数类型不匹配: path={}, param={}, type={}",
                request.getRequestURI(), ex.getName(), ex.getRequiredType());
        return ResponseEntity.badRequest().body(Result.fail(400, "参数类型错误"));
    }

    /**
     * 缺少必需参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Object>> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("缺少参数: path={}, param={}", request.getRequestURI(), ex.getParameterName());
        return ResponseEntity.badRequest().body(Result.fail(400, "缺少必需参数: " + ex.getParameterName()));
    }

    /**
     * 404 资源未找到
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Object>> handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("资源未找到: path={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(404, "请求的资源不存在"));
    }

    /**
     * 兜底异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception ex, HttpServletRequest request) {
        log.error("未处理异常: path={}, type={}, message={}",
                request.getRequestURI(), ex.getClass().getName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "系统错误"));
    }
}
