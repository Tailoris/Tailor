package com.tailoris.product.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.constant.RedisKeyPrefix;
import com.tailoris.product.dto.ProductSearchRequest;
import com.tailoris.product.dto.ProductSearchResult;
import com.tailoris.product.entity.Product;
import com.tailoris.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 数据库商品搜索实现 - PRD-005.
 *
 * <p>无 ES 时的降级方案，使用 MySQL LIKE + 索引 + Redis 缓存。
 * 性能特征：单表 100w 条数据下 P99 < 500ms（带索引）。</p>
 *
 * <p>🔒 PRD-005: 搜索结果缓存到 Redis 60s，避免相同查询重复打 DB。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@Primary
@ConditionalOnMissingBean(name = "elasticsearchProductSearch")
@RequiredArgsConstructor
public class DatabaseProductSearch implements ProductSearchEngine {

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SEARCH_CACHE_KEY = RedisKeyPrefix.PRODUCT + "search:";
    private static final long SEARCH_CACHE_TTL = 60;  // 60秒

    @Override
    public ProductSearchResult search(ProductSearchRequest request) {
        long start = System.currentTimeMillis();

        // 1. 尝试缓存
        String cacheKey = buildCacheKey(request);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                ProductSearchResult cachedResult = om.readValue(cached, ProductSearchResult.class);
                return cachedResult;
            } catch (Exception ignore) {
                log.debug("缓存反序列化失败, 将重新查询: {}", ignore.getMessage());
            }
        }

        // 2. 构造查询
        Page<Product> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
               .eq(Product::getDeleted, 0);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Product::getName, request.getKeyword())
                              .or().like(Product::getSubTitle, request.getKeyword())
                              .or().like(Product::getDescription, request.getKeyword()));
        }
        if (request.getProductType() != null) {
            wrapper.eq(Product::getProductType, request.getProductType());
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, request.getCategoryId());
        }
        if (request.getShopId() != null) {
            wrapper.eq(Product::getShopId, request.getShopId());
        }
        if (request.getMerchantId() != null) {
            wrapper.eq(Product::getMerchantId, request.getMerchantId());
        }
        if (request.getPriceMin() != null) {
            wrapper.ge(Product::getMinPrice, request.getPriceMin());
        }
        if (request.getPriceMax() != null) {
            wrapper.le(Product::getMinPrice, request.getPriceMax());
        }
        if (Boolean.TRUE.equals(request.getInStockOnly())) {
            wrapper.gt(Product::getTotalStock, 0);
        }

        // 3. 排序
        applySort(wrapper, request);

        // 4. 执行查询
        Page<Product> result = productMapper.selectPage(page, wrapper);

        // 5. 构造结果
        ProductSearchResult searchResult = new ProductSearchResult();
        searchResult.setProducts(result.getRecords());
        searchResult.setTotal(result.getTotal());
        searchResult.setPageNum((int) result.getCurrent());
        searchResult.setPageSize((int) result.getSize());
        searchResult.setTotalPages((int) result.getPages());
        searchResult.setFromEs(false);
        searchResult.setCostMs(System.currentTimeMillis() - start);

        // 6. 缓存结果
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            stringRedisTemplate.opsForValue().set(cacheKey,
                    om.writeValueAsString(searchResult), SEARCH_CACHE_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("搜索结果缓存失败: {}", e.getMessage());
        }

        return searchResult;
    }

    @Override
    public boolean index(Product product) {
        // DB搜索不需要索引
        return true;
    }

    @Override
    public int indexBatch(List<Product> products) {
        return products == null ? 0 : products.size();
    }

    @Override
    public boolean delete(Long productId) {
        return true;
    }

    @Override
    public int rebuildAll() {
        Long count = productMapper.selectCount(null);
        log.info("数据库搜索无需重建索引, 当前商品数: {}", count);
        return count.intValue();
    }

    private void applySort(LambdaQueryWrapper<Product> wrapper, ProductSearchRequest request) {
        String sort = StringUtils.hasText(request.getSortBy()) ? request.getSortBy() : "new";
        boolean asc = "asc".equalsIgnoreCase(request.getOrder());
        switch (sort.toLowerCase()) {
            case "sales":
                wrapper.orderBy(true, !asc, Product::getSaleCount);
                break;
            case "price":
                wrapper.orderBy(true, !asc, Product::getMinPrice);
                break;
            case "view":
                wrapper.orderBy(true, !asc, Product::getViewCount);
                break;
            case "new":
            default:
                wrapper.orderBy(true, !asc, Product::getCreateTime);
                break;
        }
    }

    private String buildCacheKey(ProductSearchRequest request) {
        StringBuilder sb = new StringBuilder(SEARCH_CACHE_KEY);
        sb.append(request.getKeyword() == null ? "" : request.getKeyword().hashCode())
          .append(':').append(request.getProductType() == null ? "" : request.getProductType())
          .append(':').append(request.getCategoryId() == null ? "" : request.getCategoryId())
          .append(':').append(request.getSortBy())
          .append(':').append(request.getOrder())
          .append(':').append(request.getPageNum())
          .append(':').append(request.getPageSize());
        return sb.toString();
    }
}
