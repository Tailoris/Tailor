package com.tailoris.design.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tailoris.design.dto.DesignQueryRequest;
import com.tailoris.design.entity.Design;

import java.util.List;

public interface DesignService {

    Design createDesign(Design design);

    Design updateDesign(Long id, Design design);

    void deleteDesign(Long id);

    Design getDesignById(Long id);

    IPage<Design> getDesignPage(DesignQueryRequest queryRequest);

    List<Design> getHotDesigns(Integer limit);

    List<Design> getNewDesigns(Integer limit);

    List<Design> getDesignsByUserId(Long userId);

    void incrementViewCount(Long id);

    boolean toggleLike(Long designId, Long userId);

    boolean toggleCollection(Long designId, Long userId);

    boolean checkIsLiked(Long designId, Long userId);

    boolean checkIsCollected(Long designId, Long userId);
}