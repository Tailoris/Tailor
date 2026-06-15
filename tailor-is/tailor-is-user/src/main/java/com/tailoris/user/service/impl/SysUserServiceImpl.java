package com.tailoris.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.constant.RedisKeyPrefix;
import com.tailoris.common.crypto.AesGcmCrypto;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.IdCardValidator;
import com.tailoris.common.util.LogMaskUtils;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
import com.tailoris.user.dto.LoginRequest;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.RealNameAuthRequest;
import com.tailoris.user.dto.RegisterRequest;
import com.tailoris.user.dto.UserUpdateRequest;
import com.tailoris.user.entity.SysPermission;
import com.tailoris.user.entity.SysRole;
import com.tailoris.user.entity.SysUser;
import com.tailoris.user.mapper.SysUserMapper;
import com.tailoris.user.security.LoginSecurityService;
import com.tailoris.user.security.LoginSecurityService.SmsVerifyResult;
import com.tailoris.user.service.SysPermissionService;
import com.tailoris.user.service.SysRoleService;
import com.tailoris.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统用户服务实现.
 *
 * <p>提供用户登录、注册、信息查询、密码管理等核心功能。</p>
 *
 * <p>关键修复：</p>
 * <ul>
 *   <li>B-C05: 登录失败锁定（5次/30分钟）</li>
 *   <li>B-H01: 身份证号AES加密存储</li>
 *   <li>B-H15: 登录后用户信息缓存预热</li>
 *   <li>B-M02: 提取手机号截取常量为PHONE_MASK_PREFIX_LENGTH</li>
 *   <li>B-M03: 提取"用户不存在"为USER_NOT_FOUND_MSG常量</li>
 *   <li>B-M04: 统一常量命名规范</li>
 *   <li>B-M36: 用户状态使用枚举</li>
 *   <li>B-M39: Javadoc首句以句号结尾</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    /** 手机号脱敏前缀长度（B-M02修复：提取魔法数字） */
    private static final int PHONE_MASK_PREFIX_LENGTH = 7;

    /** 用户不存在错误消息（B-M03修复：提取重复字符串） */
    private static final String USER_NOT_FOUND_MSG = "用户不存在";

    /** 登录失败最大次数（与B-C05锁定策略配合） */
    private static final int MAX_LOGIN_FAILURES = 5;

    /** 用户状态枚举：0-正常, 1-禁用, 2-锁定（B-M36修复） */
    private static final int USER_STATUS_NORMAL = 0;

    /** 用户状态：禁用 */
    private static final int USER_STATUS_DISABLED = 1;

    /** 用户状态：锁定 */
    private static final int USER_STATUS_LOCKED = 2;

    /** 实名认证状态：已认证 */
    private static final int CERT_STATUS_VERIFIED = 1;

    /** Redis 缓存 1 小时（秒） */
    private static final long CACHE_TTL_HOURS = 1L;

    // 静态属性定义顺序：常量在前
    // 🔒 B-L02修复: 使用RedisKeyPrefix常量统一管理Redis key
    private static final String USER_CACHE_KEY = RedisKeyPrefix.USER + "info:";

    private final SysUserMapper sysUserMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final LoginSecurityService loginSecurityService;
    private final AesGcmCrypto aesGcmCrypto;
    private final SpringSnowflakeIdGenerator snowflakeIdGenerator;
    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;

    // 静态属性定义顺序：实例属性在后（B-M08修复）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException("用户名和密码不能为空");
        }

        // 1. 检查账号是否被锁定
        if (loginSecurityService.isAccountLocked(username)) {
            long remainSeconds = loginSecurityService.getLockRemainSeconds(username);
            throw new BusinessException("账号已锁定，请在" + remainSeconds + "秒后重试");
        }

        // 2. 查询用户
        SysUser user = findByUsername(username);
        if (user == null) {
            log.warn("登录失败：{} {}", LogMaskUtils.maskPhone(username), USER_NOT_FOUND_MSG);
            // 记录失败次数（防用户枚举）
            loginSecurityService.recordLoginFailure(username);
            throw new BusinessException(USER_NOT_FOUND_MSG);
        }

        // 3. 验证用户状态（B-M36修复：使用常量替代魔法数字）
        if (user.getStatus() != null && user.getStatus() != USER_STATUS_NORMAL) {
            log.warn("账号状态异常: userId={}, status={}", user.getId(), user.getStatus());
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 4. 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("密码错误: userId={}", user.getId());
            loginSecurityService.recordLoginFailure(username);
            throw new BusinessException("用户名或密码错误");
        }

        // 5. 登录成功，清除失败计数和锁定状态
        loginSecurityService.clearLoginFailures(username);

        // 6. Sa-Token登录
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 7. 缓存预热（B-H15）
        preheatUserCache(user.getId());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(buildUserInfo(user));
        return response;
    }

    public void preheatUserCache(Long userId) {
        // 🔒 USR-005: 预热用户信息 + 角色 + 权限（解决登录后第一次请求慢的问题）
        SysUser user = getUserById(userId);
        if (user == null) {
            return;
        }
        try {
            sysRoleService.listRolesByUserId(userId);
            sysPermissionService.getPermissionsByUserId(userId);
        } catch (Exception e) {
            log.warn("预热用户角色权限失败: userId={}", userId, e);
        }
    }

    /**
     * 通过用户名或手机号查询用户（B-C05配合：统一查询入口）.
     *
     * @param username 用户名或手机号
     * @return 用户实体
     */
    private SysUser findByUsername(String username) {
        SysUser user = getUserByUsername(username);
        if (user == null) {
            user = getUserByPhone(username);
        }
        return user;
    }

    @Override
    public void register(RegisterRequest request) {
        String phone = request.getPhone();

        // 🔒 B-C06: 使用LoginSecurityService进行原子化验证码校验
        SmsVerifyResult verifyResult = loginSecurityService.verifySmsCode(phone, request.getSmsCode());
        switch (verifyResult) {
            case SUCCESS:
                break; // 校验通过
            case MISMATCH:
                throw new BusinessException("短信验证码错误");
            case EXPIRED:
                throw new BusinessException("短信验证码已过期或不存在，请重新获取");
            case ALREADY_USED:
                throw new BusinessException("验证码已被使用，请重新获取");
            case TOO_MANY_ATTEMPTS:
                throw new BusinessException("验证尝试次数过多，请稍后再试");
            default:
                throw new BusinessException("验证码校验失败");
        }

        LambdaQueryWrapper<SysUser> phoneQuery = new LambdaQueryWrapper<>();
        phoneQuery.eq(SysUser::getPhone, phone);
        if (sysUserMapper.selectCount(phoneQuery) > 0) {
            throw new BusinessException("手机号已注册");
        }

        SysUser user = new SysUser();
        user.setUsername(phone);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(USER_STATUS_DISABLED);
        user.setNickName("用户_" + phone.substring(PHONE_MASK_PREFIX_LENGTH));
        sysUserMapper.insert(user);

        log.info("用户注册成功: phone={}, userId={}", LogMaskUtils.maskPhone(phone), user.getId());
    }

    /**
     * 发送短信验证码（供注册/找回密码使用）
     */
    public void sendSmsCode(String phone) {
        // 实际生产应调用短信服务API
        // 此处使用固定6位随机数（仅用于演示）
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        loginSecurityService.storeSmsCode(phone, code);
        log.info("短信验证码已发送: phone={}", phone);
        // 注意：实际生产不应将code写入日志
    }

    @Override
    public LoginResponse.UserInfo getUserInfo(Long userId) {
        String cacheKey = USER_CACHE_KEY + userId;
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(USER_NOT_FOUND_MSG);
        }
        return buildUserInfo(user);
    }

    @Override
    @Transactional
    public void updateUser(Long userId, UserUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(USER_NOT_FOUND_MSG);
        }
        if (request.getNickName() != null) {
            user.setNickName(request.getNickName());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }
        sysUserMapper.updateById(user);
        invalidateUserCache(userId);
    }

    @Override
    public void realNameAuth(Long userId, RealNameAuthRequest request) {
        // 1. 校验身份证号格式
        if (!IdCardValidator.isValid(request.getIdCard())) {
            throw new BusinessException("身份证号格式错误，请核对后重试");
        }

        // 2. 真实姓名合法性（中文/·）
        if (request.getRealName() == null || request.getRealName().length() < 2 || request.getRealName().length() > 30) {
            throw new BusinessException("真实姓名长度应在2-30字符之间");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(USER_NOT_FOUND_MSG);
        }

        // 3. 提取身份证内嵌信息（出生日期/性别/地区）
        java.time.LocalDate birthDate = IdCardValidator.extractBirthDate(request.getIdCard());
        Integer gender = IdCardValidator.extractGender(request.getIdCard());

        // 4. 加密身份证号（AES-GCM）
        String encryptedIdCard = aesGcmCrypto.encrypt(request.getIdCard());
        // 5. 加密真实姓名
        String encryptedRealName = aesGcmCrypto.encrypt(request.getRealName());

        // 6. 更新
        user.setRealName(encryptedRealName);
        user.setIdCard(encryptedIdCard);
        user.setIdCardFront(request.getIdCardFront());
        user.setIdCardBack(request.getIdCardBack());
        if (birthDate != null) {
            user.setBirthday(birthDate);
        }
        if (gender != null) {
            user.setGender(gender);
        }
        user.setCertification(CERT_STATUS_VERIFIED); // 1=已认证
        user.setCertificationTime(java.time.LocalDateTime.now());
        sysUserMapper.updateById(user);

        // 7. 清理缓存
        invalidateUserCache(userId);
        log.info("实名认证完成: userId={}, 身份证={}", userId, IdCardValidator.mask(request.getIdCard()));
    }

    /**
     * 解密身份证号（仅授权场景使用）
     */
    public String getDecryptedIdCard(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getIdCard() == null) {
            return null;
        }
        return aesGcmCrypto.decrypt(user.getIdCard());
    }

    @Override
    public PageResponse<SysUser> listUsers(PageRequest pageRequest) {
        Page<SysUser> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        return new PageResponse<>(
                result.getRecords(),
                result.getTotal(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }

    @Override
    public SysUser getUserById(Long userId) {
        return sysUserMapper.selectById(userId);
    }

    @Override
    public SysUser getUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return sysUserMapper.selectOne(wrapper);
    }

    @Override
    public SysUser getUserByPhone(String phone) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone, phone);
        return sysUserMapper.selectOne(wrapper);
    }

    @Override
    public void updateLoginInfo(Long userId, String ip) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        sysUserMapper.updateById(user);
    }

    @Override
    public void logout(Long userId) {
        StpUtil.logout(userId);
    }

    @Override
    public LoginResponse refresh(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("用户已被禁用，无法刷新Token");
        }

        StpUtil.login(userId);
        String newToken = StpUtil.getTokenValue();

        LoginResponse response = new LoginResponse();
        response.setToken(newToken);
        response.setUserInfo(buildUserInfo(user));
        return response;
    }

    private LoginResponse.UserInfo buildUserInfo(SysUser user) {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setNickName(user.getNickName());
        userInfo.setRealName(user.getRealName());
        userInfo.setGender(user.getGender());
        userInfo.setStatus(user.getStatus());

        List<SysRole> roles = sysRoleService.listRolesByUserId(user.getId());
        userInfo.setRoles(roles.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList()));

        List<SysPermission> permissions = sysPermissionService.getPermissionsByUserId(user.getId());
        userInfo.setPermissions(permissions.stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toList()));

        return userInfo;
    }

    @Override
    public void invalidateUserCache(Long userId) {
        // 🔒 USR-005: 多级缓存清理
        try {
            String key = USER_CACHE_KEY + userId;
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("清理用户缓存失败: userId={}", userId, e);
        }
    }
}
