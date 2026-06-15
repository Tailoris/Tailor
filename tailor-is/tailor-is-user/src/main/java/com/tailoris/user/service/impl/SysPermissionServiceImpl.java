package com.tailoris.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.user.entity.SysPermission;
import com.tailoris.user.entity.SysRolePermission;
import com.tailoris.user.mapper.SysPermissionMapper;
import com.tailoris.user.mapper.SysRolePermissionMapper;
import com.tailoris.user.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements SysPermissionService {

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public List<SysPermission> getPermissionsByUserId(Long userId) {
        return sysPermissionMapper.selectByUserId(userId);
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
