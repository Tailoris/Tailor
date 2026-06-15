package com.tailoris.product.service;

import com.tailoris.product.dto.CategoryRequest;
import com.tailoris.product.entity.ProductTag;

import java.util.List;

public interface ProductTagService {

    Long createTag(ProductTag tag);

    void updateTag(Long id, ProductTag tag);

    List<ProductTag> listTags();

    void assignTagToProduct(Long productId, Long tagId);

    void removeTagFromProduct(Long productId, Long tagId);

    List<ProductTag> getTagsByProduct(Long productId);
}
