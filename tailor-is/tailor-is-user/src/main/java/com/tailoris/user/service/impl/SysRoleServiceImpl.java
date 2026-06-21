package com.tailoris.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.user.entity.SysRole;
import com.tailoris.user.entity.SysUserRole;
import com.tailoris.user.mapper.SysRoleMapper;
import com.tailoris.user.mapper.SysUserRoleMapper;
import com.tailoris.user.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<SysRole> listRolesByUserId(Long userId) {
        List<SysUserRole> userRoles = getUserRolesByUserId(userId);
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort);
        return sysRoleMapper.selectList(wrapper);
    }

    @Override
    public Map<Long, List<SysRole>> listRolesByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 批量查询用户-角色关联（1 次 SQL）
        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.in(SysUserRole::getUserId, userIds);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(urWrapper);

        if (userRoles.isEmpty()) {
            Map<Long, List<SysRole>> empty = new HashMap<>();
            userIds.forEach(id -> empty.put(id, List.of()));
            return empty;
        }

        // userId -> roleIds
        Map<Long, List<Long>> userRoleIdsMap = userRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));

        // 批量查询所有角色（1 次 SQL）
        List<Long> allRoleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysRole::getId, allRoleIds)
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort);
        List<SysRole> roles = sysRoleMapper.selectList(roleWrapper);
        Map<Long, SysRole> roleMap = roles.stream()
                .collect(Collectors.toMap(SysRole::getId, role -> role));

        // 组装 userId -> 角色列表
        Map<Long, List<SysRole>> result = new HashMap<>();
        for (Long userId : userIds) {
            List<Long> roleIds = userRoleIdsMap.getOrDefault(userId, Collections.emptyList());
            List<SysRole> userRoles2 = roleIds.stream()
                    .map(roleMap::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            result.put(userId, userRoles2);
        }
        return result;
    }

    @Override
    public List<SysRole> listAllRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort);
        return sysRoleMapper.selectList(wrapper);
    }

    @Override
    public SysRole getRoleById(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    @Override
    @Transactional
    public SysRole createRole(SysRole role) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode());
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("角色编码已存在");
        }
        role.setStatus(1);
        sysRoleMapper.insert(role);
        return role;
    }

    @Override
    @Transactional
    public void updateRole(SysRole role) {
        SysRole existing = sysRoleMapper.selectById(role.getId());
        if (existing == null) {
            throw new BusinessException("角色不存在");
        }
        sysRoleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        SysRole existing = sysRoleMapper.selectById(roleId);
        if (existing == null) {
            throw new BusinessException("角色不存在");
        }
        sysRoleMapper.deleteById(roleId);

        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getRoleId, roleId);
        sysUserRoleMapper.delete(wrapper);
    }

    @Override
    @Transactional
    public void assignRoleToUser(Long userId, Long roleId) {
        SysRole role = getRoleById(roleId);
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId);
        if (sysUserRoleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户已拥有该角色");
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setCreatedAt(LocalDateTime.now());
        sysUserRoleMapper.insert(userRole);
    }

    @Override
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId);
        sysUserRoleMapper.delete(wrapper);
    }

    private List<SysUserRole> getUserRolesByUserId(Long userId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        return sysUserRoleMapper.selectList(wrapper);
    }
}
