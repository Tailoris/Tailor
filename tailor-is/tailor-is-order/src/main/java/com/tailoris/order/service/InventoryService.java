package com.tailoris.order.service;

import com.tailoris.order.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 库存服务 - 修复 B-C07
 *
 * <p>订单创建时执行库存预扣减，防止超卖。
 * 采用乐观锁 + 条件UPDATE实现：UPDATE WHERE stock >= quantity。</p>
 *
 * <h3>关键实现</h3>
 * <ol>
 *   <li>SQL: UPDATE inventory SET stock = stock - #{quantity}, locked = locked + #{quantity} WHERE sku_id = ? AND stock >= ?</li>
 *   <li>影响行数为0表示库存不足，事务回滚</li>
 *   <li>已扣减的库存通过 order_item.reserved_stock 跟踪</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 预扣减库存 (修复 B-C07)
     *
     * <p>在订单创建事务中调用，确保库存足够才能创建订单。
     * 使用条件UPDATE保证原子性，防止超卖。</p>
     *
     * @param orderItems 订单明细
     * @return 是否全部扣减成功
     */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public boolean deductStock(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            if (item.getSkuId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                log.warn("跳过无效订单明细: skuId={}, quantity={}", item.getSkuId(), item.getQuantity());
                continue;
            }
            int affected = jdbcTemplate.update(
                    "UPDATE product_sku SET stock = stock - ?, locked_stock = locked_stock + ? "
                            + "WHERE id = ? AND stock >= ? AND deleted = 0",
                    item.getQuantity(),
                    item.getQuantity(),
                    item.getSkuId(),
                    item.getQuantity()
            );
            if (affected == 0) {
                log.warn("库存预扣减失败: skuId={}, 需求={}, 库存可能不足", item.getSkuId(), item.getQuantity());
                return false;
            }
        }
        log.debug("库存预扣减成功: {}条", orderItems.size());
        return true;
    }

    /**
     * 释放库存（订单取消/超时）
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void releaseStock(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            if (item.getSkuId() == null || item.getQuantity() == null) {
                continue;
            }
            int affected = jdbcTemplate.update(
                    "UPDATE product_sku SET stock = stock + ?, locked_stock = locked_stock - ? "
                            + "WHERE id = ? AND locked_stock >= ?",
                    item.getQuantity(),
                    item.getQuantity(),
                    item.getSkuId(),
                    item.getQuantity()
            );
            if (affected == 0) {
                log.warn("库存释放失败: skuId={}, 需人工核查", item.getSkuId());
            }
        }
    }

    /**
     * 扣减已锁定库存（支付成功后）
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void confirmDeduct(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            if (item.getSkuId() == null || item.getQuantity() == null) {
                continue;
            }
            int affected = jdbcTemplate.update(
                    "UPDATE product_sku SET locked_stock = locked_stock - ? WHERE id = ? AND locked_stock >= ?",
                    item.getQuantity(),
                    item.getSkuId(),
                    item.getQuantity()
            );
            if (affected == 0) {
                log.error("已锁定库存扣减失败: skuId={}", item.getSkuId());
            }
        }
    }
}
