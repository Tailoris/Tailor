package com.tailoris.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.constant.RedisKeyPrefix;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.lock.DistributedLock;
import com.tailoris.product.dto.ProductCreateRequest;
import com.tailoris.product.dto.ProductQueryRequest;
import com.tailoris.product.dto.ProductUpdateRequest;
import com.tailoris.product.entity.Product;
import com.tailoris.product.entity.ProductAttribute;
import com.tailoris.product.entity.ProductSku;
import com.tailoris.product.entity.ProductTagMapping;
import com.tailoris.product.mapper.ProductFavoriteMapper;
import com.tailoris.product.mapper.ProductReviewMapper;
import com.tailoris.product.enums.AuditStatusEnum;
import com.tailoris.product.enums.ProductStatusEnum;
import com.tailoris.product.mapper.ProductAttributeMapper;
import com.tailoris.product.mapper.ProductMapper;
import com.tailoris.product.mapper.ProductSkuMapper;
import com.tailoris.product.mapper.ProductTagMappingMapper;
import com.tailoris.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品服务实现.
 *
 * <p>提供商品创建、编辑、查询、上下架等核心商品管理功能。
 * 集成Redis缓存减少数据库查询压力，缓存TTL为30分钟。</p>
 *
 * <p>关键修复：</p>
 * <ul>
 *   <li>B-C08: 商品创建幂等控制（分布式锁 + 业务唯一性校验）</li>
 *   <li>B-M17: saveProductBaseInfo使用BeanUtils.copyProperties</li>
 *   <li>B-M18: saveProductSkus使用Stream API</li>
 *   <li>B-M33: viewCount使用数据库原子更新（setSql）</li>
 *   <li>B-M34: 初始好评率0%（不硬编码100%）</li>
 *   <li>B-M38: 商品状态使用ProductStatusEnum</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductAttributeMapper productAttributeMapper;
    private final ProductTagMappingMapper productTagMappingMapper;
    // 🔒 PRD-007: 商品级联删除需要的额外 Mapper
    private final ProductReviewMapper productReviewMapper;
    private final ProductFavoriteMapper productFavoriteMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final DistributedLock distributedLock;

    private static final String PRODUCT_CACHE_KEY = RedisKeyPrefix.PRODUCT + "detail:";
    // 🔒 B-L09修复: 缓存过期时间改用配置化，支持不同环境差异化
    @Value("${tailoris.product.cache.expire-seconds:1800}")
    private long cacheExpireSeconds;

    /** B-C08 商品创建幂等Key前缀（基于业务唯一标识） */
    private static final String PRODUCT_DUPLICATE_KEY = "product:create:dedup:";

    /** B-M34 初始好评率 - 没有评价时为null，新商品无评分 */
    private static final BigDecimal INITIAL_FAVORABLE_RATE = BigDecimal.ZERO;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductCreateRequest request) {
        // 🔒 PRD-001: 防并发重复创建（基于商品唯一性的分布式锁）
        String dedupKey = PRODUCT_DUPLICATE_KEY + request.getMerchantId() + ":"
                + request.getName().hashCode();
        // 修复：TTL由30s改为10s，避免长时间阻塞其他请求
        String token = distributedLock.tryLock(dedupKey, 10, TimeUnit.SECONDS);
        if (token == null) {
            log.warn("商品并发创建被限流: merchantId={}, name={}", request.getMerchantId(), request.getName());
            throw new BusinessException("商品正在创建中，请稍后再试");
        }

        try {
            // 🔒 B-C08: 业务级唯一性校验
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getMerchantId, request.getMerchantId())
                    .eq(Product::getName, request.getName())
                    .eq(Product::getDeleted, 0);
            Long existingCount = productMapper.selectCount(wrapper);
            if (existingCount > 0) {
                throw new BusinessException("该店铺已存在同名商品");
            }

            // 🔒 B-C08: 同一店铺的SKU编码去重
            if (!CollectionUtils.isEmpty(request.getSkus())) {
                List<String> skuCodes = request.getSkus().stream()
                        .map(ProductCreateRequest.SkuCreateRequest::getSkuCode)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList());
                if (skuCodes.size() > 1) {
                    long distinctCount = skuCodes.stream().distinct().count();
                    if (distinctCount != skuCodes.size()) {
                        throw new BusinessException("SKU编码存在重复");
                    }
                }
            }

            // 🔒 PRD-001: 全局唯一性增强（防止跨店铺重复SKU编码）
            if (!CollectionUtils.isEmpty(request.getSkus())) {
                for (ProductCreateRequest.SkuCreateRequest skuReq : request.getSkus()) {
                    if (StringUtils.hasText(skuReq.getSkuCode())) {
                        LambdaQueryWrapper<ProductSku> skuWrapper = new LambdaQueryWrapper<>();
                        skuWrapper.eq(ProductSku::getSkuCode, skuReq.getSkuCode());
                        Long skuExists = productSkuMapper.selectCount(skuWrapper);
                        if (skuExists > 0) {
                            throw new BusinessException("SKU编码已存在: " + skuReq.getSkuCode());
                        }
                    }
                }
            }

            Product product = saveProductBaseInfo(request);
            saveProductSkus(product.getId(), request.getSkus());
            saveProductAttributes(product.getId(), request.getAttributes());
            saveProductTags(product.getId(), request.getTagIds());

            // 🔒 PRD-001: 写入成功后清理可能的缓存残留
            stringRedisTemplate.delete(PRODUCT_CACHE_KEY + product.getId());

            log.info("商品创建成功: id={}, name={}, merchantId={}",
                    product.getId(), product.getName(), request.getMerchantId());
            return product.getId();
        } catch (Exception e) {
            log.error("商品创建失败: merchantId={}, name={}, error={}",
                    request.getMerchantId(), request.getName(), e.getMessage());
            throw e;
        } finally {
            distributedLock.unlock(dedupKey, token);
        }
    }

    /**
     * 保存商品基础信息 - 修复 B-M17/B-M34/B-M38
     *
     * <p>使用BeanUtils.copyProperties简化属性拷贝，初始状态和评分使用枚举/常量。</p>
     */
    private Product saveProductBaseInfo(ProductCreateRequest request) {
        Product product = new Product();
        // B-M17修复: 使用BeanUtils.copyProperties简化字段复制
        BeanUtils.copyProperties(request, product, "skus", "attributes", "tagIds");
        // B-M38修复: 使用枚举替代魔法数字
        product.setStatus(ProductStatusEnum.OFF_SHELF.getCode());
        product.setAuditStatus(AuditStatusEnum.PENDING.getCode());
        // B-M34修复: 初始好评率为0（新商品无评价），不硬编码100%
        product.setFavorableRate(INITIAL_FAVORABLE_RATE);
        // 字段默认值
        product.setSaleCount(0);
        product.setViewCount(0);
        product.setCommentCount(0);
        // 可选字段默认值
        if (request.getCopyrightFlag() == null) {
            product.setCopyrightFlag(0);
        }
        productMapper.insert(product);
        return product;
    }

    /**
     * 保存商品SKU - 修复 B-M18
     *
     * <p>使用Stream API替代for循环，提升代码可读性。</p>
     */
    private void saveProductSkus(Long productId, List<ProductCreateRequest.SkuCreateRequest> skus) {
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        List<ProductSku> skuList = skus.stream()
                .map(skuReq -> convertToSku(productId, skuReq))
                .collect(Collectors.toList());
        productSkuMapper.insertBatchSomeColumn(skuList);
    }

    /**
     * SKU请求转实体
     */
    private ProductSku convertToSku(Long productId, ProductCreateRequest.SkuCreateRequest skuReq) {
        ProductSku sku = new ProductSku();
        BeanUtils.copyProperties(skuReq, sku);
        sku.setProductId(productId);
        // 字段默认值
        if (skuReq.getStock() == null) {
            sku.setStock(0);
        }
        if (skuReq.getWarningStock() == null) {
            sku.setWarningStock(10);
        }
        if (skuReq.getStatus() == null) {
            sku.setStatus(1);
        }
        sku.setSalesCount(0);
        return sku;
    }

    /**
     * 保存商品属性 - 修复 B-M18
     */
    private void saveProductAttributes(Long productId, List<ProductCreateRequest.AttributeCreateRequest> attributes) {
        if (CollectionUtils.isEmpty(attributes)) {
            return;
        }
        List<ProductAttribute> attrList = attributes.stream()
                .map(attrReq -> {
                    ProductAttribute attr = new ProductAttribute();
                    BeanUtils.copyProperties(attrReq, attr);
                    attr.setProductId(productId);
                    if (attrReq.getAttrType() == null) {
                        attr.setAttrType(1);
                    }
                    if (attrReq.getSort() == null) {
                        attr.setSort(0);
                    }
                    return attr;
                })
                .collect(Collectors.toList());
        productAttributeMapper.insertBatchSomeColumn(attrList);
    }

    /**
     * 保存商品标签映射 - 修复 B-M18
     */
    private void saveProductTags(Long productId, List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return;
        }
        List<ProductTagMapping> mappingList = tagIds.stream()
                .map(tagId -> {
                    ProductTagMapping mapping = new ProductTagMapping();
                    mapping.setProductId(productId);
                    mapping.setTagId(tagId);
                    return mapping;
                })
                .collect(Collectors.toList());
        productTagMappingMapper.insertBatchSomeColumn(mappingList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, ProductUpdateRequest request) {
        validateProductEditable(id);
        updateProductEntity(id, request);
        saveOrUpdateSkus(id, request.getSkus());
        saveOrUpdateAttributes(id, request.getAttributes());
        replaceProductTags(id, request.getTagIds());
        stringRedisTemplate.delete(PRODUCT_CACHE_KEY + id);
    }

    private void validateProductEditable(Long id) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        if (existing.getStatus() != null
                && existing.getStatus() == ProductStatusEnum.DRAFT.getCode()) {
            throw new BusinessException("已上架商品不可直接编辑，请先下架");
        }
    }

    private void updateProductEntity(Long id, ProductUpdateRequest request) {
        Product product = new Product();
        product.setId(id);
        product.setName(request.getName());
        product.setSubTitle(request.getSubTitle());
        product.setCategoryId(request.getCategoryId());
        product.setProductType(request.getProductType());
        product.setMainImage(request.getMainImage());
        product.setImages(request.getImages());
        product.setVideoUrl(request.getVideoUrl());
        product.setDescription(request.getDescription());
        product.setSpecifications(request.getSpecifications());
        product.setCopyrightFlag(request.getCopyrightFlag());
        product.setCopyrightId(request.getCopyrightId());
        product.setBrandName(request.getBrandName());
        product.setWeight(request.getWeight());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
        product.setHeight(request.getHeight());
        product.setFreightTemplateId(request.getFreightTemplateId());
        product.setLowerShelfReason(request.getLowerShelfReason());
        productMapper.updateById(product);
    }

    private void saveOrUpdateSkus(Long productId, List<ProductUpdateRequest.SkuUpdateRequest> skus) {
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        List<ProductSku> insertList = new ArrayList<>();
        for (ProductUpdateRequest.SkuUpdateRequest skuReq : skus) {
            ProductSku sku = new ProductSku();
            if (skuReq.getId() != null) {
                sku.setId(skuReq.getId());
            } else {
                sku.setProductId(productId);
            }
            sku.setSkuCode(skuReq.getSkuCode());
            sku.setBarcode(skuReq.getBarcode());
            sku.setAttributes(skuReq.getAttributes());
            sku.setAttributeText(skuReq.getAttributeText());
            sku.setPrice(skuReq.getPrice());
            sku.setOriginalPrice(skuReq.getOriginalPrice());
            sku.setCostPrice(skuReq.getCostPrice());
            sku.setStock(skuReq.getStock());
            sku.setWarningStock(skuReq.getWarningStock());
            sku.setWeight(skuReq.getWeight());
            sku.setImage(skuReq.getImage());
            sku.setStatus(skuReq.getStatus());
            if (skuReq.getId() != null) {
                productSkuMapper.updateById(sku);
            } else {
                insertList.add(sku);
            }
        }
        if (!insertList.isEmpty()) {
            productSkuMapper.insertBatchSomeColumn(insertList);
        }
    }

    private void saveOrUpdateAttributes(Long productId, List<ProductUpdateRequest.AttributeUpdateRequest> attributes) {
        if (CollectionUtils.isEmpty(attributes)) {
            return;
        }
        List<ProductAttribute> insertList = new ArrayList<>();
        for (ProductUpdateRequest.AttributeUpdateRequest attrReq : attributes) {
            ProductAttribute attr = new ProductAttribute();
            if (attrReq.getId() != null) {
                attr.setId(attrReq.getId());
            } else {
                attr.setProductId(productId);
            }
            attr.setAttrName(attrReq.getAttrName());
            attr.setAttrValue(attrReq.getAttrValue());
            attr.setAttrType(attrReq.getAttrType());
            attr.setSort(attrReq.getSort());
            if (attrReq.getId() != null) {
                productAttributeMapper.updateById(attr);
            } else {
                insertList.add(attr);
            }
        }
        if (!insertList.isEmpty()) {
            productAttributeMapper.insertBatchSomeColumn(insertList);
        }
    }

    private void replaceProductTags(Long productId, List<Long> tagIds) {
        if (tagIds == null) {
            return;
        }
        LambdaQueryWrapper<ProductTagMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductTagMapping::getProductId, productId);
        productTagMappingMapper.delete(wrapper);
        if (!tagIds.isEmpty()) {
            List<ProductTagMapping> mappingList = new ArrayList<>();
            for (Long tagId : tagIds) {
                ProductTagMapping mapping = new ProductTagMapping();
                mapping.setProductId(productId);
                mapping.setTagId(tagId);
                mappingList.add(mapping);
            }
            productTagMappingMapper.insertBatchSomeColumn(mappingList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        // 🔒 PRD-007: 商品级联删除事务补偿 + 顺序优化
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            log.warn("删除商品不存在: id={}", id);
            return;
        }

        // 1. 检查是否允许删除
        if (existing.getStatus() != null
                && existing.getStatus() == ProductStatusEnum.ON_SHELF.getCode()) {
            throw new BusinessException("上架商品不能直接删除，请先下架");
        }

        // 2. 按依赖关系从弱到强顺序删除（先删子表，再删主表）
        // 顺序：标签映射 → 属性 → SKU → 商品
        // 任何一步失败，事务整体回滚，避免脏数据
        int tagRows = deleteProductTagMappings(id);
        log.debug("删除商品标签映射: productId={}, rows={}", id, tagRows);

        int attrRows = deleteProductAttributes(id);
        log.debug("删除商品属性: productId={}, rows={}", id, attrRows);

        int skuRows = deleteProductSkus(id);
        log.debug("删除商品SKU: productId={}, rows={}", id, skuRows);

        // 3. 评价/收藏等业务数据软删除（保留审计）
        int reviewRows = productReviewMapper.softDeleteByProductId(id);
        int favoriteRows = productFavoriteMapper.softDeleteByProductId(id);
        log.debug("商品级联删除完成: productId={}, tags={}, attrs={}, skus={}, reviews={}, favorites={}",
                id, tagRows, attrRows, skuRows, reviewRows, favoriteRows);

        // 4. 最后删除商品主表
        int productRows = productMapper.deleteById(id);
        if (productRows == 0) {
            throw new BusinessException("商品删除失败，请重试");
        }

        // 5. 多级缓存清理
        clearProductCaches(id);

        log.info("商品删除完成: productId={}, name={}", id, existing.getName());
    }

    private int deleteProductTagMappings(Long productId) {
        LambdaQueryWrapper<ProductTagMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductTagMapping::getProductId, productId);
        return productTagMappingMapper.delete(wrapper);
    }

    private int deleteProductAttributes(Long productId) {
        LambdaQueryWrapper<ProductAttribute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductAttribute::getProductId, productId);
        return productAttributeMapper.delete(wrapper);
    }

    private int deleteProductSkus(Long productId) {
        LambdaQueryWrapper<ProductSku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductSku::getProductId, productId);
        return productSkuMapper.delete(wrapper);
    }

    private void clearProductCaches(Long productId) {
        // BE-M-41: 仅清理目标商品缓存，避免使用 KEYS 通配符过度清理
        // 删除单个商品时无需清空全部商品详情缓存
        stringRedisTemplate.delete(PRODUCT_CACHE_KEY + productId);
    }

    @Override
    public Product getProductDetail(Long id) {
        // 🔒 PRD-006: 缓存击穿防护（双检查锁 + 分布式锁 + 空值标记）
        String cacheKey = PRODUCT_CACHE_KEY + id;

        // 1. 第一级：查Redis缓存
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (CACHE_NULL_VALUE.equals(cached)) {
            // 命中空值标记（防穿透），直接返回
            log.debug("🔒 [PRD-006] 命中空值缓存: id={}", id);
            throw new BusinessException("商品不存在");
        }
        if (StringUtils.hasText(cached) && !CACHE_NULL_VALUE.equals(cached)) {
            try {
                Product cachedProduct = objectMapper.readValue(cached, Product.class);
                asyncIncrementViewCount(id);
                return cachedProduct;
            } catch (Exception e) {
                log.error("Failed to deserialize product cache, key: {}, error: {}", cacheKey, e.getMessage());
                stringRedisTemplate.delete(cacheKey);
            }
        }

        // 2. 第二级：分布式锁 + 二级缓存双检查（防击穿）
        String lockKey = PRODUCT_CACHE_KEY + "lock:" + id;
        return distributedLock.executeWithLock(lockKey, 5, 10, TimeUnit.SECONDS, () -> {
            // 双重检查：拿锁后再次查缓存（防在等待锁期间别的线程已写缓存）
            String cached2 = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached2) && !CACHE_NULL_VALUE.equals(cached2)) {
                try {
                    return objectMapper.readValue(cached2, Product.class);
                } catch (Exception ignore) {
                    stringRedisTemplate.delete(cacheKey);
                }
            }

            // 3. 第三级：查数据库
            Product product = productMapper.selectById(id);
            if (product == null) {
                // 🔒 PRD-006: 空值标记缓存（防穿透），短TTL
                stringRedisTemplate.opsForValue().set(cacheKey, CACHE_NULL_VALUE, CACHE_NULL_TTL_SECONDS, TimeUnit.SECONDS);
                log.debug("🔒 [PRD-006] 写入空值缓存: id={}", id);
                throw new BusinessException("商品不存在");
            }

            // 4. 写回缓存
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(product), cacheExpireSeconds, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Failed to write product cache, key: {}, error: {}", cacheKey, e.getMessage());
            }

            asyncIncrementViewCount(id);
            return product;
        });
    }

    /**
     * 🔒 PRD-006: 缓存空值标记常量（区分空值 vs 缓存未命中）
     */
    private static final String CACHE_NULL_VALUE = "__NULL__";

    /**
     * 🔒 PRD-006: 空值缓存 TTL（短，防止恶意构造不存在的id长占缓存）
     */
    private static final long CACHE_NULL_TTL_SECONDS = 60;

    /**
     * 异步增加商品浏览量 - 修复 B-M33
     *
     * <p>使用数据库原子更新（setSql），避免先查后改的竞态条件，
     * 与社区模块 CommunityInteractionServiceImpl 的修复模式一致。</p>
     */
    private void asyncIncrementViewCount(Long productId) {
        try {
            // B-M33: 使用 SQL 原子更新浏览量，避免并发计数不准确
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, productId)
                    .setSql("view_count = COALESCE(view_count, 0) + 1"));
        } catch (Exception e) {
            log.warn("商品浏览量计数失败: productId={}", productId, e);
        }
    }

    @Override
    public Page<Product> listProducts(ProductQueryRequest request) {
        Page<Product> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Product::getName, request.getKeyword())
                   .or()
                   .like(Product::getSubTitle, request.getKeyword());
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, request.getCategoryId());
        }
        if (request.getMerchantId() != null) {
            wrapper.eq(Product::getMerchantId, request.getMerchantId());
        }
        if (request.getShopId() != null) {
            wrapper.eq(Product::getShopId, request.getShopId());
        }
        if (request.getProductType() != null) {
            wrapper.eq(Product::getProductType, request.getProductType());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Product::getStatus, request.getStatus());
        }
        if (request.getAuditStatus() != null) {
            wrapper.eq(Product::getAuditStatus, request.getAuditStatus());
        }
        if (request.getCopyrightFlag() != null) {
            wrapper.eq(Product::getCopyrightFlag, request.getCopyrightFlag());
        }

        wrapper.orderByDesc(Product::getCreateTime);

        return productMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<Product> listProductsByShop(Long shopId, ProductQueryRequest request) {
        request.setShopId(shopId);
        return listProducts(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductStatus(Long id, Integer status) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }

        Product product = new Product();
        product.setId(id);
        product.setStatus(status);

        if (status != null && status == ProductStatusEnum.ON_SHELF.getCode()) {
            product.setAuditStatus(AuditStatusEnum.PENDING.getCode());
            product.setAuditTime(null);
            product.setAuditRemark(null);
            product.setAuditBy(null);
        } else if (status != null && status == ProductStatusEnum.VIOLATED_OFF_SHELF.getCode()) {
            product.setLowerShelfReason("商家主动下架");
        }

        productMapper.updateById(product);
        stringRedisTemplate.delete(PRODUCT_CACHE_KEY + id);
    }

    @Override
    public Page<Product> getProductByType(Integer productType, ProductQueryRequest request) {
        request.setProductType(productType);
        return listProducts(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(Long id, Integer auditStatus, String auditRemark, Long auditBy) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }

        Product product = new Product();
        product.setId(id);
        product.setAuditStatus(auditStatus);
        product.setAuditRemark(auditRemark);
        product.setAuditBy(auditBy);
        product.setAuditTime(LocalDateTime.now());

        if (auditStatus != null && auditStatus == AuditStatusEnum.REJECTED.getCode()) {
            product.setStatus(ProductStatusEnum.DRAFT.getCode());
        } else if (auditStatus != null && auditStatus == AuditStatusEnum.OVERRULED.getCode()) {
            product.setStatus(ProductStatusEnum.AUDIT_REJECTED.getCode());
        }

        productMapper.updateById(product);
        stringRedisTemplate.delete(PRODUCT_CACHE_KEY + id);
    }
}
