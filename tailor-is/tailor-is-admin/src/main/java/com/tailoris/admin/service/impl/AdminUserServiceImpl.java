package com.tailoris.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.UserQueryRequest;
import com.tailoris.admin.service.AdminUserService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.user.entity.SysUser;
import com.tailoris.api.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper sysUserMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResponse<SysUser> listUsers(UserQueryRequest request) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        if (request.getStatus() != null) {
            queryWrapper.eq(SysUser::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysUser::getUsername, request.getKeyword())
                    .or()
                    .like(SysUser::getPhone, request.getKeyword())
                    .or()
                    .like(SysUser::getNickName, request.getKeyword())
            );
        }

        queryWrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<SysUser> result = sysUserMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus().equals(AdminConstants.USER_STATUS_FROZEN)) {
            throw new BusinessException("用户已处于冻结状态");
        }
        user.setStatus(AdminConstants.USER_STATUS_FROZEN);
        sysUserMapper.updateById(user);
        stringRedisTemplate.delete("tailoris:token:" + userId);
        log.info("用户已冻结, userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!user.getStatus().equals(AdminConstants.USER_STATUS_FROZEN)) {
            throw new BusinessException("用户未处于冻结状态");
        }
        user.setStatus(AdminConstants.USER_STATUS_NORMAL);
        sysUserMapper.updateById(user);
        log.info("用户已解冻, userId: {}", userId);
    }

    @Override
    public SysUser getUserDetail(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }
}
