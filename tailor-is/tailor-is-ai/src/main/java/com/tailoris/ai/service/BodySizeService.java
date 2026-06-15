package com.tailoris.ai.service;

import com.tailoris.ai.dto.SizeDataRequest;
import com.tailoris.ai.entity.BodySizeData;

import java.util.List;

public interface BodySizeService {

    BodySizeData manageSizeData(Long userId, SizeDataRequest request);

    BodySizeData getSizeData(Long sizeId);

    List<BodySizeData> listUserSizeData(Long userId);

    List<BodySizeData> searchByBodyType(String bodyType, Integer gender);

    void setDefaultSize(Long userId, Long sizeId);

    void deleteSizeData(Long userId, Long sizeId);
}
