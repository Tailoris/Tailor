package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.ApplyTypeDTO;
import com.tailoris.api.admin.dto.ApplyTypeQueryRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.api.merchant.entity.ApplyType;

import java.util.List;

public interface AdminApplyTypeService {

    PageResponse<ApplyType> page(ApplyTypeQueryRequest req);

    List<ApplyType> listEnabled();

    ApplyType getById(Long id);

    /** @deprecated 请使用 create(ApplyTypeDTO, Long) 传递管理员ID */
    @Deprecated(forRemoval = true)
    default void create(ApplyTypeDTO dto, String adminName) {
        create(dto, (Long) null);
    }

    void create(ApplyTypeDTO dto, Long adminId);

    /** @deprecated 请使用 update(Long, ApplyTypeDTO, Long) */
    @Deprecated(forRemoval = true)
    default void update(Long id, ApplyTypeDTO dto, String adminName) {
        update(id, dto, (Long) null);
    }

    void update(Long id, ApplyTypeDTO dto, Long adminId);

    /** @deprecated 请使用 remove(Long, Long) */
    @Deprecated(forRemoval = true)
    default void remove(Long id, String adminName) {
        remove(id, (Long) null);
    }

    void remove(Long id, Long adminId);
}
