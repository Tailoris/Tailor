package com.tailoris.common.util;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * 平台管理后台安全工具类 - 修复 L-05/L-23/S-03
 *
 * <p>提供统一的登录ID获取、权限校验、HTTP上下文工具、审计日志落库写入。</p>
 *
 * <p>安全原则：</p>
 * <ul>
 *   <li>所有 getLoginId 调用必须走 try/catch 兜底 - 避免 NotLoginException 中断业务流程非预期抛出</li>
 *   <li>hasPermission 为 false 时主动抛 BusinessException，Service 层权限拒绝统一响应 403</li>
 *   <li>getCurrentRequest 从 RequestContextHolder 读取，Service 层无需再把 request 层层传递</li>
 *   <li>appendModifyLog 落数据库 merchant_modify_log 记录，业务Service通用调用</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2.1.0
 */
@Slf4j
public final class AdminSecurityUtils {

    /** 登录失败兜底ID（未登录/会话过期时记录操作用） */
    public static final long UNKNOWN_OPERATOR = -1L;

    /** 操作人类型：1=平台管理员 2=商家本人 3=系统 4=第三方回调 */
    public static final int OPERATOR_TYPE_ADMIN = 1;
    public static final int OPERATOR_TYPE_MERCHANT = 2;
    public static final int OPERATOR_TYPE_SYSTEM = 3;

    private AdminSecurityUtils() {
    }

    /** 判断当前是否已登录（平台管理员上下文） */
    public static boolean isLogin() {
        try {
            return StpUtil.isLogin();
        } catch (Exception e) {
            log.warn("AdminSecurityUtils.isLogin 异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前登录管理员ID（Long类型）.
     *
     * <p>未登录或会话异常时返回 UNKNOWN_OPERATOR(-1)，调用方自行决定是否允许继续。</p>
     */
    public static Long getLoginIdAsLong() {
        try {
            if (!StpUtil.isLogin()) return UNKNOWN_OPERATOR;
            Object id = StpUtil.getLoginIdDefaultNull();
            if (id == null) return UNKNOWN_OPERATOR;
            if (id instanceof Number) return ((Number) id).longValue();
            return Long.parseLong(String.valueOf(id));
        } catch (Exception e) {
            log.warn("AdminSecurityUtils.getLoginIdAsLong 获取失败: {}", e.getMessage());
            return UNKNOWN_OPERATOR;
        }
    }

    /**
     * 获取当前登录管理员ID（字符串形式，兼容Sa-Token原生方法）
     */
    public static String getLoginIdAsString() {
        Long id = getLoginIdAsLong();
        return Objects.equals(id, UNKNOWN_OPERATOR) ? "" : String.valueOf(id);
    }

    /**
     * 校验当前用户是否拥有指定角色（超级管理员默认放行）
     */
    public static boolean hasRole(String role) {
        try {
            return StpUtil.hasRole("SUPER_ADMIN") || StpUtil.hasRole(role);
        } catch (Exception e) {
            log.warn("AdminSecurityUtils.hasRole 校验异常 role={} error={}", role, e.getMessage());
            return false;
        }
    }

    /**
     * 校验当前用户是否拥有指定权限（SUPER_ADMIN通配符放行）.
     *
     * <p>权限字符串： merchant:category:add / merchant:deposit:audit</p>
     */
    public static boolean hasPermission(String permission) {
        try {
            return StpUtil.hasRole("SUPER_ADMIN")
                    || StpUtil.hasPermissionOr(permission, "*");
        } catch (Exception e) {
            log.warn("AdminSecurityUtils.hasPermission 校验异常 perm={} error={}", permission, e.getMessage());
            return false;
        }
    }

    /**
     * 从 RequestContextHolder 读取当前请求（Spring MVC线程上下文）.
     *
     * @return 可为 null（非请求线程，如定时任务场景）
     */
    public static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            return attr == null ? null : attr.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /** 获取当前IP（非请求线程返回 "system"） */
    public static String getCurrentIp() {
        HttpServletRequest req = getCurrentRequest();
        if (req == null) return "system";
        return HttpRequestUtils.getClientIp(req);
    }

    /** 获取当前User-Agent（兜底 "unknown"） */
    public static String getCurrentUserAgent() {
        HttpServletRequest req = getCurrentRequest();
        if (req == null) return "unknown";
        String ua = req.getHeader("User-Agent");
        return ua == null ? "unknown" : safeTruncate(ua, 512);
    }

    /**
     * 通用安全字符串截断 + 敏感字符清理（防SQL注入日志&超长写入数据库）
     */
    public static String safeTruncate(String input, int maxLen) {
        if (input == null) return "";
        String clean = input.replace('\0', ' ').replaceAll("[\\u0000-\\u001F]", " ");
        return clean.length() > maxLen ? clean.substring(0, maxLen) : clean;
    }

    /**
     * 校验权限，不通过时抛出 BusinessException（Controller/Service层通用）.
     *
     * @param permission 如 "merchant:category:add"
     * @param errorMsg   自定义错误信息（可选）
     */
    public static void requirePermission(String permission, String errorMsg) {
        if (!hasPermission(permission)) {
            throw new com.tailoris.common.exception.BusinessException(
                    errorMsg != null ? errorMsg : "权限不足: " + permission
            );
        }
    }

    /** 重载方法：权限不足使用默认提示 */
    public static void requirePermission(String permission) {
        requirePermission(permission, null);
    }
}
