package com.tailoris.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.api.admin.dto.ShopCategoryDTO;
import com.tailoris.api.admin.dto.ShopCategoryQueryRequest;
import com.tailoris.admin.service.AdminShopCategoryService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.merchant.entity.MerchantModifyLog;
import com.tailoris.api.merchant.entity.ShopCategory;
import com.tailoris.api.merchant.mapper.MerchantModifyLogMapper;
import com.tailoris.api.merchant.mapper.ShopCategoryMapper;
import com.tailoris.common.util.AdminSecurityUtils;
import com.tailoris.common.util.HttpRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminShopCategoryServiceImpl implements AdminShopCategoryService {

    private final ShopCategoryMapper shopCategoryMapper;
    private final MerchantModifyLogMapper merchantModifyLogMapper;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Override
    public PageResponse<ShopCategory> page(ShopCategoryQueryRequest req) {
        LambdaQueryWrapper<ShopCategory> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w.like(ShopCategory::getCategoryName, req.getKeyword())
                    .or().like(ShopCategory::getCategoryCode, req.getKeyword()));
        }
        if (req.getParentId() != null) {
            wrapper.eq(ShopCategory::getParentId, req.getParentId());
        }
        if (req.getStatus() != null) {
            wrapper.eq(ShopCategory::getStatus, req.getStatus());
        }
        wrapper.orderByAsc(ShopCategory::getSortOrder).orderByDesc(ShopCategory::getCreateTime);

        Page<ShopCategory> page = new Page<>(req.getPageNum(), req.getPageSize());
        shopCategoryMapper.selectPage(page, wrapper);
        return PageResponse.of(page);
    }

    @Override
    public List<ShopCategory> tree() {
        LambdaQueryWrapper<ShopCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopCategory::getStatus, 1).orderByAsc(ShopCategory::getSortOrder);
        List<ShopCategory> all = shopCategoryMapper.selectList(wrapper);

        Map<Long, List<ShopCategory>> childrenMap = new HashMap<>();
        List<ShopCategory> roots = new ArrayList<>();

        for (ShopCategory category : all) {
            if (category.getParentId() == null || category.getParentId() == 0) {
                roots.add(category);
            } else {
                childrenMap.computeIfAbsent(category.getParentId(), k -> new ArrayList<>()).add(category);
            }
        }

        for (ShopCategory root : roots) {
            attachChildren(root, childrenMap);
        }

        return roots;
    }

    @SuppressWarnings("unchecked")
    private void attachChildren(ShopCategory parent, Map<Long, List<ShopCategory>> childrenMap) {
        List<ShopCategory> children = childrenMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            Map<String, Object> data = objectMapper.convertValue(parent, Map.class);
            data.put("children", children);
            for (ShopCategory child : children) {
                attachChildren(child, childrenMap);
            }
        }
    }

    @Override
    public ShopCategory getById(Long id) {
        ShopCategory category = shopCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("店铺分类不存在");
        }
        return category;
    }

    @Override
    public void create(ShopCategoryDTO dto, Long adminId) {
        ShopCategory category = objectMapper.convertValue(dto, ShopCategory.class);
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        String operatorName = adminId == null || adminId < 0 ? "system" : String.valueOf(adminId);
        category.setCreateBy(operatorName);
        category.setUpdateBy(operatorName);
        shopCategoryMapper.insert(category);

        writeModifyLog(adminId, null, category.getId(), 1, "shop_category",
                null, objectToJson(dto), "新增店铺分类: " + category.getCategoryName());
        log.info("新增店铺分类成功, id: {}, name: {}, adminId: {}", category.getId(), category.getCategoryName(), adminId);
    }

    @Override
    public void update(Long id, ShopCategoryDTO dto, Long adminId) {
        ShopCategory old = getById(id);
        String before = objectToJson(old);

        ShopCategory category = objectMapper.convertValue(dto, ShopCategory.class);
        category.setId(id);
        String operatorName = adminId == null || adminId < 0 ? "system" : String.valueOf(adminId);
        category.setUpdateBy(operatorName);
        shopCategoryMapper.updateById(category);

        writeModifyLog(adminId, null, id, 2, "shop_category",
                before, objectToJson(dto), "修改店铺分类: " + old.getCategoryName());
        log.info("修改店铺分类成功, id: {}, adminId: {}", id, adminId);
    }

    @Override
    public void remove(Long id, Long adminId) {
        ShopCategory old = getById(id);

        LambdaQueryWrapper<ShopCategory> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(ShopCategory::getParentId, id);
        Long childCount = shopCategoryMapper.selectCount(childWrapper);
        if (childCount != null && childCount > 0) {
            throw new BusinessException("该分类下存在子分类，无法删除");
        }

        shopCategoryMapper.deleteById(id);

        writeModifyLog(adminId, null, id, 3, "shop_category",
                objectToJson(old), null, "删除店铺分类: " + old.getCategoryName());
        log.info("删除店铺分类成功, id: {}, adminId: {}", id, adminId);
    }

    private String objectToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("对象转JSON失败", e);
            return null;
        }
    }

    private void writeModifyLog(Long operatorId, Long merchantId, Long shopId, Integer operateType,
                                String operateModule, String valueBefore, String valueAfter, String remark) {
        try {
            MerchantModifyLog logEntity = new MerchantModifyLog();
            logEntity.setMerchantId(merchantId);
            logEntity.setShopId(shopId);
            logEntity.setOperatorId(operatorId != null && operatorId > 0 ? operatorId : AdminSecurityUtils.UNKNOWN_OPERATOR);
            logEntity.setOperatorType(AdminSecurityUtils.OPERATOR_TYPE_ADMIN); // 1=平台管理员
            logEntity.setOperateType(operateType);
            logEntity.setOperateModule(operateModule);
            logEntity.setValueBefore(AdminSecurityUtils.safeTruncate(valueBefore, 4000));
            logEntity.setValueAfter(AdminSecurityUtils.safeTruncate(valueAfter, 4000));
            logEntity.setRemark(AdminSecurityUtils.safeTruncate(remark, 512));
            String ip = request != null ? HttpRequestUtils.getClientIp(request) : AdminSecurityUtils.getCurrentIp();
            logEntity.setIp(AdminSecurityUtils.safeTruncate(ip, 64));
            String ua = request != null ? request.getHeader("User-Agent") : AdminSecurityUtils.getCurrentUserAgent();
            logEntity.setUserAgent(AdminSecurityUtils.safeTruncate(ua, 512));
            merchantModifyLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("写入修改日志失败 module={}", operateModule, e);
        }
    }
}
