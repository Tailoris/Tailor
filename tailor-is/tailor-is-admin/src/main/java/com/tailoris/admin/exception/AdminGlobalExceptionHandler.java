package com.tailoris.admin.exception;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin 模块全局异常处理器.
 *
 * <p>仅处理 Admin 模块特有的业务异常，通用异常交由全局 GlobalExceptionHandler 处理。</p>
 * <p>重命名以避免与 com.tailoris.common.exception.GlobalExceptionHandler 的 bean 名称冲突.</p>
 *
 * <h3>关键约束</h3>
 * <ul>
 *   <li>使用 basePackages 限制作用范围，仅拦截 admin 包下的异常</li>
 *   <li>优先级低于全局异常处理器（LOWEST_PRECEDENCE - 100）</li>
 *   <li>不包含 Exception.class 兜底处理，避免覆盖全局精准异常处理</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.tailoris.admin")
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class AdminGlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("Admin业务异常: path={}, code={}, message={}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Admin参数校验失败: path={}, errors={}", request.getRequestURI(), errors);
        Result<Map<String, String>> result = new Result<>();
        result.setCode(400);
        result.setMessage("参数校验失败");
        result.setData(errors);
        return ResponseEntity.badRequest().body(result);
    }
}
