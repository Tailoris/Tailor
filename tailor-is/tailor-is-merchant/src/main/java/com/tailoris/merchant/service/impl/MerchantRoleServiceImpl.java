package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.merchant.entity.MerchantRole;
import com.tailoris.merchant.mapper.MerchantRoleMapper;
import com.tailoris.merchant.service.IMerchantRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Merchant role service implementation
 * <p>商家角色服务实现类</p>
 *
 * @author Tailor IS Team
 * @since 2026-05-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantRoleServiceImpl extends ServiceImpl<MerchantRoleMapper, MerchantRole>
        implements IMerchantRoleService {

    private static final int MAX_ROLES_PER_MERCHANT = 20;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantRole createRole(MerchantRole role) {
        if (role.getMerchantId() == null) {
            throw new IllegalArgumentException("merchantId is required");
        }

        long count = count(new LambdaQueryWrapper<MerchantRole>()
                .eq(MerchantRole::getMerchantId, role.getMerchantId())
                .eq(MerchantRole::getDeleted, 0));
        if (count >= MAX_ROLES_PER_MERCHANT) {
            throw new IllegalStateException("Maximum " + MAX_ROLES_PER_MERCHANT + " roles per merchant");
        }

        if (role.getRoleCode() != null) {
            long codeCount = count(new LambdaQueryWrapper<MerchantRole>()
                    .eq(MerchantRole::getMerchantId, role.getMerchantId())
                    .eq(MerchantRole::getRoleCode, role.getRoleCode())
                    .eq(MerchantRole::getDeleted, 0));
            if (codeCount > 0) {
                throw new IllegalArgumentException("Role code already exists: " + role.getRoleCode());
            }
        }

        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (role.getSortOrder() == null) {
            role.setSortOrder(0);
        }
        if (role.getSystemRole() == null) {
            role.setSystemRole(false);
        }

        save(role);
        log.info("Created role {} for merchant {}", role.getRoleName(), role.getMerchantId());
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(MerchantRole role) {
        if (role.getId() == null) {
            throw new IllegalArgumentException("roleId is required");
        }

        MerchantRole existing = getById(role.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Role not found: " + role.getId());
        }

        if (Boolean.TRUE.equals(existing.getSystemRole()) && role.getPermissions() != null) {
            throw new IllegalStateException("Cannot modify system role permissions");
        }

        return updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long roleId, Long merchantId) {
        MerchantRole role = getById(roleId);
        if (role == null) {
            log.warn("Role not found: {}", roleId);
            return false;
        }

        if (!role.getMerchantId().equals(merchantId)) {
            throw new SecurityException("Unauthorized to delete this role");
        }

        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new IllegalStateException("Cannot delete system role");
        }

        boolean result = removeById(roleId);
        if (result) {
            log.info("Deleted role {} for merchant {}", roleId, merchantId);
        }
        return result;
    }

    @Override
    public List<MerchantRole> listByMerchant(Long merchantId) {
        return list(new LambdaQueryWrapper<MerchantRole>()
                .eq(MerchantRole::getMerchantId, merchantId)
                .eq(MerchantRole::getStatus, 1)
                .eq(MerchantRole::getDeleted, 0)
                .orderByAsc(MerchantRole::getSortOrder)
                .orderByAsc(MerchantRole::getId));
    }

    @Override
    public MerchantRole getRoleWithPermissions(Long roleId) {
        return getById(roleId);
    }
}
