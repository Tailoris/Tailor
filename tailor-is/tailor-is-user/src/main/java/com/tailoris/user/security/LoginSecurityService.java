package com.tailoris.user.security;

import com.tailoris.common.util.LogMaskUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 登录安全服务 - 修复 Critical B-C05/B-C06
 *
 * <p>提供登录失败计数、账号锁定、验证码原子校验等安全能力。</p>
 *
 * <h3>关键能力</h3>
 * <ul>
 *   <li>登录失败计数（5次失败后锁定30分钟）</li>
 *   <li>账号锁定状态查询</li>
 *   <li>解锁账号</li>
 *   <li>Redis原子操作：验证码防绕过</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 登录失败计数器 Key 前缀 */
    private static final String LOGIN_FAIL_COUNT_KEY = "security:login:fail:";

    /** 账号锁定 Key 前缀 */
    private static final String ACCOUNT_LOCK_KEY = "security:account:lock:";

    /** 验证码 Key 前缀 */
    private static final String SMS_CODE_KEY = "security:sms:code:";

    /** 验证尝试 Key 前缀（用于防绕过） */
    private static final String SMS_VERIFY_ATTEMPT_KEY = "security:sms:attempt:";

    /** 锁定持续时间（30分钟） */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    /** 最大失败次数 */
    private static final int MAX_FAIL_COUNT = 5;

    /** 失败计数窗口（15分钟） */
    private static final Duration FAIL_COUNT_WINDOW = Duration.ofMinutes(15);

    /** 验证码有效期（5分钟） */
    private static final Duration SMS_CODE_TTL = Duration.ofMinutes(5);

    /** 短信验证最大尝试次数（防暴力破解） */
    private static final int MAX_SMS_ATTEMPT = 5;

    /**
     * 🔒 USR-002修复: Lua脚本保证短信验证的原子性.
     * <p>单脚本完成"查码-比对-消费-记录尝试次数"全流程，Redis 单线程执行，
     *    杜绝 GET+DEL 非原子导致的并发绕过问题。</p>
     *
     * <p>KEYS[1] = 验证码key   ARGV[1] = 待校验code
     *    KEYS[2] = 尝试计数key  ARGV[2] = 最大尝试次数</p>
     *
     * <p>返回：</p>
     * <ul>
     *   <li>1: 校验成功（已消费验证码）</li>
     *   <li>0: 验证尝试次数超限</li>
     *   <li>-1: 验证码已过期或不存在</li>
     *   <li>-2: 验证码不匹配（同时会累加尝试次数）</li>
     * </ul>
     */
    private static final RedisScript<Long> SMS_VERIFY_LUA = new DefaultRedisScript<>(
            "local code = redis.call('GET', KEYS[1])\n" +
            "if not code then return -1 end\n" +
            "local attempts = tonumber(redis.call('GET', KEYS[2]) or '0')\n" +
            "if attempts >= tonumber(ARGV[2]) then return 0 end\n" +
            "if code == ARGV[1] then\n" +
            "    redis.call('DEL', KEYS[1])\n" +
            "    redis.call('DEL', KEYS[2])\n" +
            "    return 1\n" +
            "else\n" +
            "    local n = redis.call('INCR', KEYS[2])\n" +
            "    if n == 1 then\n" +
            "        redis.call('EXPIRE', KEYS[2], 300)\n" +
            "    end\n" +
            "    return -2\n" +
            "end",
            Long.class);

    /**
     * 检查账号是否被锁定.
     *
     * <p>🔒 USR-001: 5次/30分钟 登录失败锁定机制的核心入口。
     * 被 SysUserServiceImpl.login() 调用，命中锁定直接抛异常。</p>
     *
     * @param username 用户名/手机号
     * @return true=已锁定，false=未锁定
     */
    public boolean isAccountLocked(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        Boolean exists = stringRedisTemplate.hasKey(ACCOUNT_LOCK_KEY + username);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 获取账号锁定的剩余秒数.
     *
     * <p>🔒 USR-001: 用于业务异常提示"请在X秒后重试"。</p>
     *
     * @param username 用户名/手机号
     * @return 剩余秒数；0 表示未锁定或已过期
     */
    public long getLockRemainSeconds(String username) {
        if (username == null || username.isEmpty()) {
            return 0L;
        }
        Long ttl = stringRedisTemplate.getExpire(ACCOUNT_LOCK_KEY + username, TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? 0L : ttl;
    }

    /**
     * 检查账号是否被锁定（带详细锁定信息）
     *
     * @param username 用户名/手机号
     * @return 锁定信息，null表示未锁定
     */
    public AccountLockInfo checkAccountLock(String username) {
        String lockKey = ACCOUNT_LOCK_KEY + username;
        String lockedAt = stringRedisTemplate.opsForValue().get(lockKey);
        if (lockedAt != null) {
            Long ttl = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            return new AccountLockInfo(true, ttl == null ? 0 : ttl, lockedAt);
        }
        return new AccountLockInfo(false, 0, null);
    }

    /**
     * 记录登录失败
     *
     * @param username 用户名/手机号
     * @return 失败次数
     */
    public long recordLoginFailure(String username) {
        String failKey = LOGIN_FAIL_COUNT_KEY + username;
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, FAIL_COUNT_WINDOW);
        }
        log.warn("登录失败: username={}, failCount={}", username, count);

        // 达到最大失败次数，锁定账号
        if (count != null && count >= MAX_FAIL_COUNT) {
            lockAccount(username);
        }
        return count == null ? 0 : count;
    }

    /**
     * 锁定账号
     */
    public void lockAccount(String username) {
        String lockKey = ACCOUNT_LOCK_KEY + username;
        stringRedisTemplate.opsForValue().set(lockKey, String.valueOf(System.currentTimeMillis()), LOCK_DURATION);
        log.warn("账号已锁定: username={}, duration={}min", username, LOCK_DURATION.toMinutes());
    }

    /**
     * 登录成功后清除失败计数
     */
    public void clearLoginFailures(String username) {
        stringRedisTemplate.delete(LOGIN_FAIL_COUNT_KEY + username);
        stringRedisTemplate.delete(ACCOUNT_LOCK_KEY + username);
    }

    /**
     * 原子校验短信验证码 (USR-002).
     *
     * <p>🔒 USR-002: 使用Redis Lua脚本实现原子操作，修复 GET+DEL 非原子导致的并发绕过：</p>
     * <ol>
     *   <li>单脚本完成"查码-比对-消费-计数"全流程</li>
     *   <li>Redis 单线程执行，杜绝竞态</li>
     *   <li>验证通过立即消费（一次性使用，防重放）</li>
     *   <li>失败自动累加尝试次数并设置5分钟过期</li>
     * </ol>
     *
     * @param phone 手机号
     * @param code  待校验的验证码
     * @return 校验结果
     */
    public SmsVerifyResult verifySmsCode(String phone, String code) {
        if (phone == null || code == null) {
            return SmsVerifyResult.MISMATCH;
        }
        String codeKey = SMS_CODE_KEY + phone;
        String attemptKey = SMS_VERIFY_ATTEMPT_KEY + phone;

        Long result = stringRedisTemplate.execute(
                SMS_VERIFY_LUA,
                Arrays.asList(codeKey, attemptKey),
                code, String.valueOf(MAX_SMS_ATTEMPT));

        if (result == null) {
            log.error("短信验证 Lua 脚本执行失败: phone={}", phone);
            return SmsVerifyResult.MISMATCH;
        }

        long ret = result;
        if (ret == 1L) {
            log.debug("短信验证成功: phone={}", LogMaskUtils.maskPhone(phone));
            return SmsVerifyResult.SUCCESS;
        } else if (ret == 0L) {
            log.warn("短信验证尝试超限: phone={}", LogMaskUtils.maskPhone(phone));
            return SmsVerifyResult.TOO_MANY_ATTEMPTS;
        } else if (ret == -1L) {
            log.warn("短信验证码已过期: phone={}", LogMaskUtils.maskPhone(phone));
            return SmsVerifyResult.EXPIRED;
        } else {
            // -2 = 不匹配
            return SmsVerifyResult.MISMATCH;
        }
    }

    /**
     * 存储短信验证码
     */
    public void storeSmsCode(String phone, String code) {
        String codeKey = SMS_CODE_KEY + phone;
        stringRedisTemplate.opsForValue().set(codeKey, code, SMS_CODE_TTL);
        // 清除旧的尝试计数
        stringRedisTemplate.delete(SMS_VERIFY_ATTEMPT_KEY + phone);
    }

    /** 账号锁定信息 */
    public record AccountLockInfo(boolean locked, long remainingSeconds, String lockedAt) {
    }

    /** 短信验证结果 */
    public enum SmsVerifyResult {
        /** 验证成功 */
        SUCCESS,
        /** 验证码错误 */
        MISMATCH,
        /** 验证码已过期或不存在 */
        EXPIRED,
        /** 已被其他请求使用（防重放） */
        ALREADY_USED,
        /** 尝试次数过多 */
        TOO_MANY_ATTEMPTS
    }
}
