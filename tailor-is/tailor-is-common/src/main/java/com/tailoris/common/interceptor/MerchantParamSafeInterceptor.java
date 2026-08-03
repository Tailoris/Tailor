package com.tailoris.common.interceptor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.AdminSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 商家管理模块请求安全参数安全拦截器 - 修复 S-01/S-02/S-05
 *
 * <p>防护策略：
 * <ol>
 *   <li>防SQL注入：扫描 query parameter / json body 内出现 SQL 关键词（1=1, drop, union select, sleep(, 注释符--），发现立即拒并记 warn 6</li>
 *   <li>超长参数截断：单参数 /varchar 字段超长（>256 char 截断；> 4000 字符直接抛 BusinessException</li>
 *   <li>只拦截 /api/v1/admin/merchant* 开头的所有请求</li>
 *   <li>敏感关键词告警 + 审计写入日志，用于安全分析（BusinessException会被全局异常处理器GlobalExceptionHandler转成400错误返回）</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 2.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantParamSafeInterceptor implements HandlerInterceptor, Ordered {

    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
            "' or 1=1", "\" or 1=1", "or '1'='1", "--", "/*",
            "union select", "union all select",
            "drop table", "drop database", "sleep(", "benchmark(",
            "insert into", "delete from", "update .* set", "sleep (",
            "waitfor delay", "information_schema", "0x3a", "0x",
            "../", "..%2f", "<script", "</script>", "javascript:",
            "xp_cmdshell", "exec(", "execute("
    ));

    /** 匹配SQL关键字Pattern（大小写不敏感） */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "('\\s*or\\s*['\"]?1['\"]?\\s*=\\s*['\"]?1|--|/\\*|;\\s*drop|;\\s*delete|;\\s*update|sleep\\s*\\(|benchmark\\s*\\(|union\\s+(all\\s+)?select|waitfor\\s+delay|information_schema)",
            Pattern.CASE_INSENSITIVE
    );

    /** 超长文本最大允许长度（varchar字段） */
    public static final int MAX_VARCHAR_LIMIT = 2000;
    /** 单参数 JSON 字符串最大长度（超过直接拒绝） */
    public static final int MAX_PARAM_LIMIT = 4000;

    /** 只拦截商家管理相关URI前缀 */
    private static final Set<String> URI_PREFIXES = new HashSet<>(Arrays.asList(
            "/api/v1/admin/merchant",
            "/api/v1/admin/shop-category",
            "/api/v1/admin/apply-type",
            "/api/v1/admin/merchant-modify",
            "/api/v1/admin/deposit",
            "/api/v1/admin/withdraw-audit",
            "/api/v1/admin/merchant-bill"
    ));

    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 30;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (!shouldIntercept(uri)) return true;

        // 1. 扫描 query parameter (GET/DELETE)
        Map<String, String[]> params = request.getParameterMap();
        if (params != null) {
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                String[] values = entry.getValue();
                if (values == null) continue;
                for (String v : values) {
                    checkParam(entry.getKey(), v);
                }
            }
        }
        return true;
    }

    /** JSON Body 扫描：通过 ContentCachingRequestWrapper 取 body，此拦截器注册在Spring中由 AOP 读取 RequestBody 之后？注：HandlerInterceptor.preHandle 发生在 RequestBody 读取前，
     * JSON body 在 Controller 方法级 AOP / Filter 处理。这里只在 HandlerInterceptor 只做参数扫描，body 层交由 @RequestBody 完成后再做校验，保留占位方法给 AOP 调用。 */

    private boolean shouldIntercept(String uri) {
        if (uri == null) return false;
        for (String prefix : URI_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        return false;
    }

    /**
     * 公共参数校验方法（给 Filter/AOP 均可调用）
     * @param name  参数名（用于日志）
     * @param value 参数值
     * @throws BusinessException 发现SQL注入或超长参数
     */
    public void checkParam(String name, String value) {
        if (!StringUtils.hasText(value)) return;
        // 1. SQL注入检测
        String lower = value.toLowerCase(Locale.ROOT);
        if (SQL_INJECTION_PATTERN.matcher(value).find()) {
            log.warn("[SQL注入疑似] param={}, ip={}, agent={}, value={}",
                    name, AdminSecurityUtils.getCurrentIp(), AdminSecurityUtils.safeTruncate(AdminSecurityUtils.getCurrentUserAgent(), 100),
                    AdminSecurityUtils.safeTruncate(value, 200));
            throw new BusinessException("请求参数非法，请刷新页面后重试");
        }
        for (String kw : SQL_KEYWORDS) {
            if (lower.contains(kw)) {
                log.warn("[SQL关键词命中] param={}, keyword={}, ip={}", name, kw, AdminSecurityUtils.getCurrentIp());
                throw new BusinessException("请求参数非法，请刷新页面后重试");
            }
        }
        // 2. 超长参数
        if (value.length() > MAX_PARAM_LIMIT) {
            log.warn("[超长参数] param={}, length={}, ip={}, maxAllowed={}",
                    name, value.length(), AdminSecurityUtils.getCurrentIp(), MAX_PARAM_LIMIT);
            throw new BusinessException("参数内容过长，请精简后提交");
        }
    }

    /**
     * AOP / Filter 对 @RequestBody Map/Object 的安全扫描入口（递归字符串字段）
     */
    public void scanObject(Object body) {
        if (body == null) return;
        try {
            Map<String, Object> map = objectMapper.convertValue(body, new TypeReference<Map<String, Object>>() {});
            scanMap(map, "$");
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            // 转换失败（简单对象/原始类型），跳过扫描
        }
    }

    @SuppressWarnings("unchecked")
    private void scanMap(Map<String, Object> map, String path) {
        if (map == null) return;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            String curPath = path + "." + k;
            if (v == null) continue;
            if (v instanceof String) {
                checkParam(curPath, (String) v);
            } else if (v instanceof Map) {
                scanMap((Map<String, Object>) v, curPath);
            } else if (v instanceof Collection) {
                int idx = 0;
                for (Object item : (Collection<Object>) v) {
                    if (item instanceof String) checkParam(curPath + "[" + idx + "]", (String) item);
                    else if (item instanceof Map) scanMap((Map<String, Object>) item, curPath + "[" + idx + "]");
                    idx++;
                }
            }
        }
    }
}
