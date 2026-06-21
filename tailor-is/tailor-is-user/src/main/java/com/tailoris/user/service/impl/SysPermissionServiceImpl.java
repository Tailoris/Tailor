package com.tailoris.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.user.entity.SysPermission;
import com.tailoris.user.entity.SysRolePermission;
import com.tailoris.user.entity.SysUserRole;
import com.tailoris.user.mapper.SysPermissionMapper;
import com.tailoris.user.mapper.SysRolePermissionMapper;
import com.tailoris.user.mapper.SysUserRoleMapper;
import com.tailoris.user.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements SysPermissionService {

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<SysPermission> getPermissionsByUserId(Long userId) {
        return sysPermissionMapper.selectByUserId(userId);
    }

    @Override
    public Map<Long, List<SysPermission>> getPermissionsByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 批量查询用户-角色关联（1 次 SQL）
        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.in(SysUserRole::getUserId, userIds);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(urWrapper);

        if (userRoles.isEmpty()) {
            Map<Long, List<SysPermission>> empty = new HashMap<>();
            userIds.forEach(id -> empty.put(id, List.of()));
            return empty;
        }

        // roleId -> userIds（反向映射）
        Map<Long, List<Long>> roleToUserIds = userRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getRoleId,
                        Collectors.mapping(SysUserRole::getUserId, Collectors.toList())));

        // 批量查询角色-权限关联（1 次 SQL）
        List<Long> allRoleIds = new ArrayList<>(roleToUserIds.keySet());
        LambdaQueryWrapper<SysRolePermission> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.in(SysRolePermission::getRoleId, allRoleIds);
        List<SysRolePermission> rolePermissions = sysRolePermissionMapper.selectList(rpWrapper);

        if (rolePermissions.isEmpty()) {
            Map<Long, List<SysPermission>> empty = new HashMap<>();
            userIds.forEach(id -> empty.put(id, List.of()));
            return empty;
        }

        // roleId -> permissionIds
        Map<Long, List<Long>> rolePermissionIdsMap = rolePermissions.stream()
                .collect(Collectors.groupingBy(SysRolePermission::getRoleId,
                        Collectors.mapping(SysRolePermission::getPermissionId, Collectors.toList())));

        // 批量查询所有权限（1 次 SQL）
        List<Long> allPermissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .distinct()
                .collect(Collectors.toList());
        LambdaQueryWrapper<SysPermission> pWrapper = new LambdaQueryWrapper<>();
        pWrapper.in(SysPermission::getId, allPermissionIds)
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getSort);
        List<SysPermission> permissions = sysPermissionMapper.selectList(pWrapper);
        Map<Long, SysPermission> permissionMap = permissions.stream()
                .collect(Collectors.toMap(SysPermission::getId, permission -> permission));

        // 组装 userId -> 权限列表（按用户去重）
        Map<Long, List<SysPermission>> result = new HashMap<>();
        for (Long userId : userIds) {
            Set<Long> seenPermissionIds = new LinkedHashSet<>();
            for (Map.Entry<Long, List<Long>> entry : roleToUserIds.entrySet()) {
                if (!entry.getValue().contains(userId)) {
                    continue;
                }
                List<Long> permissionIds = rolePermissionIdsMap.getOrDefault(entry.getKey(), Collections.emptyList());
                seenPermissionIds.addAll(permissionIds);
            }
            List<SysPermission> userPermissions = seenPermissionIds.stream()
                    .map(permissionMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            result.put(userId, userPermissions);
        }
        return result;
    }

    @Override
    public List<SysPermission> getPermissionsByRoleId(Long roleId) {
        List<SysRolePermission> rolePermissions = getRolePermissionsByRoleId(roleId);
        if (rolePermissions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysPermission::getId, permissionIds)
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getSort);
        return sysPermissionMapper.selectList(wrapper);
    }

    @Override
    public List<SysPermission> listAllPermissions() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getSort);
        return sysPermissionMapper.selectList(wrapper);
    }

    @Override
    public SysPermission getPermissionById(Long permissionId) {
        SysPermission permission = sysPermissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }
        return permission;
    }

    @Override
    @Transactional
    public SysPermission createPermission(SysPermission permission) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getPermissionCode, permission.getPermissionCode());
        if (sysPermissionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("权限编码已存在");
        }
        permission.setStatus(1);
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        if (permission.getVisible() == null) {
            permission.setVisible(1);
        }
        sysPermissionMapper.insert(permission);
        return permission;
    }

    @Override
    @Transactional
    public void updatePermission(SysPermission permission) {
        SysPermission existing = sysPermissionMapper.selectById(permission.getId());
        if (existing == null) {
            throw new BusinessException("权限不存在");
        }
        sysPermissionMapper.updateById(permission);
    }

    @Override
    @Transactional
    public void deletePermission(Long permissionId) {
        SysPermission existing = sysPermissionMapper.selectById(permissionId);
        if (existing == null) {
            throw new BusinessException("权限不存在");
        }
        sysPermissionMapper.deleteById(permissionId);

        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getPermissionId, permissionId);
        sysRolePermissionMapper.delete(wrapper);
    }

    @Override
    @Transactional
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, roleId)
                .eq(SysRolePermission::getPermissionId, permissionId);
        if (sysRolePermissionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("角色已拥有该权限");
        }
        SysRolePermission rolePermission = new SysRolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        rolePermission.setCreatedAt(LocalDateTime.now());
        sysRolePermissionMapper.insert(rolePermission);
    }

    @Override
    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, roleId)
                .eq(SysRolePermission::getPermissionId, permissionId);
        sysRolePermissionMapper.delete(wrapper);
    }

    private List<SysRolePermission> getRolePermissionsByRoleId(Long roleId) {
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, roleId);
        return sysRolePermissionMapper.selectList(wrapper);
    }
}
