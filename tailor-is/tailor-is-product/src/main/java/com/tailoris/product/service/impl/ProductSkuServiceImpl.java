package com.tailoris.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.lock.RedisDistributedLock;
import com.tailoris.product.entity.ProductSku;
import com.tailoris.product.mapper.ProductSkuMapper;
import com.tailoris.product.service.ProductSkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * SKU 服务实现 - PRD-002 库存预扣减 + PRD-004 SKU 完整管理.
 *
 * <p>🔒 PRD-002: 库存预扣减使用 Redis 分布式锁 + 乐观锁（UPDATE stock=stock-? WHERE stock>=?）双重保障：</p>
 * <ol>
 *   <li>分布式锁防止同 SKU 多请求并发（防止超卖）</li>
 *   <li>数据库乐观锁（{@code stock >= ?} 条件）作为兜底</li>
 *   <li>锁粒度按 SKU，避免热点商品阻塞其他 SKU</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl implements ProductSkuService {

    /** 库存锁前缀 */
    private static final String STOCK_LOCK_PREFIX = "tailoris:product:stock:lock:";
    /** 库存锁默认 TTL */
    private static final Duration STOCK_LOCK_TTL = Duration.ofSeconds(10);
    /** 库存锁等待时间 */
    private static final Duration STOCK_LOCK_WAIT = Duration.ofSeconds(3);

    private final ProductSkuMapper productSkuMapper;
    private final RedisDistributedLock distributedLock;

    @Value("${tailoris.product.stock-oversell:false}")
    private boolean stockOverSell;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSku(Long productId, ProductSku sku) {
        sku.setProductId(productId);
        if (sku.getStatus() == null) {
            sku.setStatus(1);
        }
        if (sku.getSalesCount() == null) {
            sku.setSalesCount(0);
        }
        if (sku.getStock() == null) {
            sku.setStock(0);
        }
        productSkuMapper.insert(sku);
        return sku.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSku(Long id, ProductSku sku) {
        ProductSku existing = productSkuMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("SKU不存在");
        }
        sku.setId(id);
        sku.setProductId(null);
        productSkuMapper.updateById(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSku(Long id) {
        ProductSku existing = productSkuMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("SKU不存在");
        }
        productSkuMapper.deleteById(id);
    }

    @Override
    public List<ProductSku> listSkusByProduct(Long productId) {
        LambdaQueryWrapper<ProductSku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductSku::getProductId, productId)
               .eq(ProductSku::getStatus, 1)
               .orderByAsc(ProductSku::getId);
        return productSkuMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStock(Long skuId, Integer quantity, boolean increase) {
        // 🔒 PRD-002: 分布式锁 + 乐观扣减双重防护
        String lockKey = STOCK_LOCK_PREFIX + skuId;
        Boolean success = distributedLock.executeWithLock(
                lockKey,
                STOCK_LOCK_TTL,
                STOCK_LOCK_WAIT,
                () -> doUpdateStock(skuId, quantity, increase));
        return Boolean.TRUE.equals(success);
    }

    /**
     * 实际执行库存变更（在分布式锁内）.
     */
    private boolean doUpdateStock(Long skuId, Integer quantity, boolean increase) {
        // 1. 查询当前库存
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException("SKU不存在");
        }

        // 2. 构造乐观锁更新（UPDATE stock=stock-? WHERE id=? AND stock>=?）
        UpdateWrapper<ProductSku> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", skuId);

        if (increase) {
            // 释放库存：直接累加
            updateWrapper.setSql("stock = stock + " + quantity);
        } else {
            // 扣减库存：乐观锁防止超卖
            if (!stockOverSell && sku.getStock() < quantity) {
                throw new BusinessException(
                        String.format("库存不足: SKU[%d] 当前库存=%d, 需求=%d",
                                skuId, sku.getStock(), quantity));
            }
            // WHERE 条件加入 stock >= ?，数据库层兜底
            updateWrapper.ge("stock", quantity);
            updateWrapper.setSql("stock = stock - " + quantity);
            updateWrapper.setSql("sales_count = sales_count + " + quantity);
        }

        int rows = productSkuMapper.update(null, updateWrapper);
        if (rows == 0) {
            log.warn("库存扣减失败（乐观锁冲突）: skuId={}, qty={}, increase={}", skuId, quantity, increase);
            throw new BusinessException("库存不足，操作失败");
        }
        log.debug("库存变更成功: skuId={}, qty={}, increase={}", skuId, quantity, increase);
        return true;
    }
}
