package com.tailoris.product.service;

import com.tailoris.product.dto.CategoryRequest;
import com.tailoris.product.entity.ProductCategory;

import java.util.List;

public interface ProductCategoryService {

    Long createCategory(CategoryRequest request);

    void updateCategory(Long id, CategoryRequest request);

    List<ProductCategory> getCategoryTree();

    List<ProductCategory> listCategories();

    void updateCategoryStatus(Long id, Integer status);
}
