package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.merchant.entity.MerchantRoleTemplate;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantRoleTemplateMapper;
import com.tailoris.merchant.service.IMerchantRoleTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 商家角色权限模板服务实现 - MER-002.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantRoleTemplateServiceImpl
        extends ServiceImpl<MerchantRoleTemplateMapper, MerchantRoleTemplate>
        implements IMerchantRoleTemplateService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public List<MerchantRoleTemplate> listSystemRoles() {
        return baseMapper.selectSystemRoles();
    }

    @Override
    public List<MerchantRoleTemplate> listMerchantRoles(Long merchantId) {
        return baseMapper.selectByMerchantId(merchantId);
    }

    @Override
    public MerchantRoleTemplate getRoleWithPermissions(Long id) {
        MerchantRoleTemplate role = getById(id);
        if (role == null) {
            return null;
        }
        return parsePermissions(role);
    }

    @Override
    public MerchantRoleTemplate getByRoleCode(String roleCode) {
        return parsePermissions(baseMapper.selectByRoleCode(roleCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantRoleTemplate createMerchantRole(Long merchantId, String roleName,
                                                   List<String> permissions, String description) {
        // 校验角色名唯一
        LambdaQueryWrapper<MerchantRoleTemplate> check = new LambdaQueryWrapper<>();
        check.eq(MerchantRoleTemplate::getMerchantId, merchantId)
             .eq(MerchantRoleTemplate::getRoleName, roleName)
             .last("LIMIT 1");
        if (count(check) > 0) {
            throw new MerchantBusinessException("角色名已存在: " + roleName);
        }

        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setMerchantId(merchantId);
        role.setRoleCode("custom_" + merchantId + "_" + System.currentTimeMillis());
        role.setRoleName(roleName);
        role.setRoleType(2);
        role.setDescription(description);
        role.setIsEnabled(1);
        role.setPermissions(toJson(permissions));
        save(role);
        return parsePermissions(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePermissions(Long roleId, List<String> permissions) {
        MerchantRoleTemplate role = getById(roleId);
        if (role == null) {
            throw new MerchantBusinessException("角色不存在");
        }
        if (role.getRoleType() != null && role.getRoleType() == 1) {
            throw new MerchantBusinessException("系统预设角色不可修改");
        }
        role.setPermissions(toJson(permissions));
        return updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long roleId, Long merchantId) {
        MerchantRoleTemplate role = getById(roleId);
        if (role == null) {
            throw new MerchantBusinessException("角色不存在");
        }
        if (role.getRoleType() != null && role.getRoleType() == 1) {
            throw new MerchantBusinessException("系统预设角色不可删除");
        }
        if (!merchantId.equals(role.getMerchantId())) {
            throw new MerchantBusinessException("无权删除该角色");
        }
        return removeById(roleId);
    }

    // ============================================================
    // 私有方法
    // ============================================================

    private String toJson(List<String> list) {
        if (list == null) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("权限列表转JSON失败: {}", e.getMessage());
            return "[]";
        }
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("权限JSON解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private MerchantRoleTemplate parsePermissions(MerchantRoleTemplate role) {
        if (role != null) {
            role.setPermissionList(parseJson(role.getPermissions()));
        }
        return role;
    }
}
