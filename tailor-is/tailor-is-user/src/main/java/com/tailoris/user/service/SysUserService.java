package com.tailoris.user.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.user.dto.LoginRequest;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.RealNameAuthRequest;
import com.tailoris.user.dto.RegisterRequest;
import com.tailoris.user.dto.UserUpdateRequest;
import com.tailoris.user.entity.SysUser;

/**
 * 系统用户服务接口.
 *
 * <p>定义用户管理、登录认证、注册等核心业务接口。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface SysUserService {

    /**
     * 用户登录.
     *
     * <p>支持用户名或手机号+密码登录，包含账号锁定检测和缓存预热。</p>
     *
     * @param request 登录请求
     * @return 登录响应，包含Token和用户信息
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册.
     *
     * <p>手机号+密码+短信验证码注册，验证码校验通过后方可注册。</p>
     *
     * @param request 注册请求
     */
    void register(RegisterRequest request);

    /**
     * 获取用户信息.
     *
     * @param userId 用户ID
     * @return 用户信息（包含角色和权限列表）
     */
    LoginResponse.UserInfo getUserInfo(Long userId);

    /**
     * 更新用户基本信息.
     *
     * @param userId 用户ID
     * @param request 更新请求
     */
    void updateUser(Long userId, UserUpdateRequest request);

    /**
     * 失效用户缓存 - USR-005.
     *
     * @param userId 用户ID
     */
    void invalidateUserCache(Long userId);

    /**
     * 实名认证.
     *
     * <p>提交身份证信息进行实名认证，身份证信息使用AES-GCM加密存储。</p>
     *
     * @param userId 用户ID
     * @param request 实名认证请求
     */
    void realNameAuth(Long userId, RealNameAuthRequest request);

    /**
     * 分页查询用户列表.
     *
     * @param pageRequest 分页参数
     * @return 用户分页列表
     */
    PageResponse<SysUser> listUsers(PageRequest pageRequest);

    /**
     * 根据ID查询用户.
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    SysUser getUserById(Long userId);

    /**
     * 根据用户名查询用户.
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回null
     */
    SysUser getUserByUsername(String username);

    /**
     * 根据手机号查询用户.
     *
     * @param phone 手机号
     * @return 用户实体，不存在时返回null
     */
    SysUser getUserByPhone(String phone);

    /**
     * 更新用户登录信息（最后登录时间/IP）.
     *
     * @param userId 用户ID
     * @param ip 登录IP
     */
    void updateLoginInfo(Long userId, String ip);

    /**
     * 用户登出.
     *
     * @param userId 用户ID
     */
    void logout(Long userId);

    /**
     * 刷新登录Token.
     *
     * <p>在Token过期前主动刷新，获取新Token，避免频繁重新登录。</p>
     *
     * @param userId 用户ID
     * @return 新的登录响应，包含新Token
     */
    LoginResponse refresh(Long userId);

    /**
     * 发送短信验证码.
     *
     * @param phone 手机号
     */
    void sendSmsCode(String phone);
}
