package com.tailoris.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        return ResponseEntity.ok(Result.fail(ex.getCode(), ex.getMessage()));
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
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST.getCode(), "参数校验失败: " + errors));
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
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST.getCode(), "参数绑定失败: " + errors));
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("非法参数: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST.getCode(), ex.getMessage()));
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
     * 兜底异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception ex, HttpServletRequest request) {
        // 🔒 B-H14: 输出完整堆栈，包含异常类型、消息、堆栈、根本原因
        log.error("未处理异常: path={}, type={}, message={}",
                request.getRequestURI(), ex.getClass().getName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "系统错误"));
    }
}
