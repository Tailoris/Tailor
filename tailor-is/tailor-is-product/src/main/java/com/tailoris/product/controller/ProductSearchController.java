package com.tailoris.product.controller;

import com.tailoris.common.result.Result;
import com.tailoris.product.dto.ProductSearchRequest;
import com.tailoris.product.dto.ProductSearchResult;
import com.tailoris.product.search.ProductSearchEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品搜索 Controller - PRD-005.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Tag(name = "商品搜索", description = "商品搜索接口（DB/ES双模）")
@RestController
@RequestMapping("/api/product/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchEngine searchEngine;

    @Operation(summary = "商品搜索", description = "支持关键词、类目、价格、店铺、标签等多维筛选")
    @GetMapping
    public Result<ProductSearchResult> search(@ModelAttribute ProductSearchRequest request) {
        if (request.getPageSize() == null || request.getPageSize() > 100) {
            request.setPageSize(20);
        }
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        return Result.success(searchEngine.search(request));
    }
}
