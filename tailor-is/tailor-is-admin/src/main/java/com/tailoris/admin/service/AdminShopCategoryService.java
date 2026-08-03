package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.ShopCategoryDTO;
import com.tailoris.api.admin.dto.ShopCategoryQueryRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.api.merchant.entity.ShopCategory;

import java.util.List;

public interface AdminShopCategoryService {

    PageResponse<ShopCategory> page(ShopCategoryQueryRequest req);

    List<ShopCategory> tree();

    ShopCategory getById(Long id);

    /** @deprecated 请使用 create(ShopCategoryDTO, Long) 传递管理员ID */
    @Deprecated(forRemoval = true)
    default void create(ShopCategoryDTO dto, String adminName) {
        create(dto, (Long) null);
    }

    void create(ShopCategoryDTO dto, Long adminId);

    /** @deprecated 请使用 update(Long, ShopCategoryDTO, Long) */
    @Deprecated(forRemoval = true)
    default void update(Long id, ShopCategoryDTO dto, String adminName) {
        update(id, dto, (Long) null);
    }

    void update(Long id, ShopCategoryDTO dto, Long adminId);

    /** @deprecated 请使用 remove(Long, Long) */
    @Deprecated(forRemoval = true)
    default void remove(Long id, String adminName) {
        remove(id, (Long) null);
    }

    void remove(Long id, Long adminId);
}
