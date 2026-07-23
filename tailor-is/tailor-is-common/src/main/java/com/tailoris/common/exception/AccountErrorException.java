package com.tailoris.common.exception;

import lombok.Getter;

/**
 * 账号错误异常 - 账号密码错误、账号不存在、账号被禁用等认证相关异常.
 *
 * <p>用于区分账号认证层面的错误（账号不存在、密码错误、账号被禁用等），
 * 与业务异常（BusinessException）和数据源连接异常（DataSourceConnectException）分离，
 * 返回 HTTP 400 BAD_REQUEST，前端根据错误码提示「账号或密码不正确」。</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>登录时账号不存在</li>
 *   <li>登录时密码错误</li>
 *   <li>账号已被禁用</li>
 *   <li>账号未通过审核</li>
 *   <li>权限校验失败（非 RBAC 层面的权限不足）</li>
 * </ul>
 *
 * <h3>与 NotLoginException 的区别</h3>
 * <ul>
 *   <li>NotLoginException: 未登录/Token 无效 → HTTP 401</li>
 *   <li>AccountErrorException: 登录时账号/密码错误 → HTTP 400</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.2.0
 */
@Getter
public class AccountErrorException extends RuntimeException {

    /**
     * 错误码（与 ResultCode 对齐）
     */
    private final int code;

    /**
     * 错误类型标识（如 ACCOUNT_NOT_FOUND, PASSWORD_ERROR, ACCOUNT_DISABLED 等）
     */
    private final String errorType;

    public AccountErrorException(String message) {
        super(message);
        this.code = 400;
        this.errorType = "ACCOUNT_ERROR";
    }

    public AccountErrorException(int code, String message) {
        super(message);
        this.code = code;
        this.errorType = "ACCOUNT_ERROR";
    }

    public AccountErrorException(String errorType, String message) {
        super(message);
        this.code = 400;
        this.errorType = errorType;
    }

    public AccountErrorException(int code, String errorType, String message) {
        super(message);
        this.code = code;
        this.errorType = errorType;
    }

    public AccountErrorException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
        this.errorType = "ACCOUNT_ERROR";
    }
}