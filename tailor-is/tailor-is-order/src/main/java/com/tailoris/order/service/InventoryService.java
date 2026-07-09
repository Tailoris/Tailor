package com.tailoris.order.service;

import com.tailoris.order.entity.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
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
 *   <li>🔒 M-09修复: 使用 JdbcTemplate.batchUpdate() 替代逐条 UPDATE，减少数据库往返</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Service
public class InventoryService {

    private final JdbcTemplate jdbcTemplate;

    public InventoryService(@Qualifier("productJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 预扣减库存 (修复 B-C07)
     *
     * <p>在订单创建事务中调用，确保库存足够才能创建订单。
     * 使用条件UPDATE保证原子性，防止超卖。</p>
     *
     * <p>🔒 M-09修复: 使用批量 UPDATE 替代逐条 UPDATE，显著减少数据库往返次数。
     * 事务保证原子性：如果任何一条扣减失败（库存不足），整个事务回滚。</p>
     *
     * @param orderItems 订单明细
     * @return 是否全部扣减成功
     */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public boolean deductStock(List<OrderItem> orderItems) {
        // 过滤有效明细
        List<OrderItem> validItems = new ArrayList<>();
        for (OrderItem item : orderItems) {
            if (item.getSkuId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                log.warn("跳过无效订单明细: skuId={}, quantity={}", item.getSkuId(), item.getQuantity());
                continue;
            }
            validItems.add(item);
        }
        if (validItems.isEmpty()) {
            return true;
        }

        // 🔒 M-09修复: 批量 UPDATE
        int[] results = jdbcTemplate.batchUpdate(
                "UPDATE product_sku SET stock = stock - ?, locked_stock = locked_stock + ? "
                        + "WHERE id = ? AND stock >= ? AND deleted = 0",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        OrderItem item = validItems.get(i);
                        ps.setLong(1, item.getQuantity());
                        ps.setLong(2, item.getQuantity());
                        ps.setLong(3, item.getSkuId());
                        ps.setLong(4, item.getQuantity());
                    }

                    @Override
                    public int getBatchSize() {
                        return validItems.size();
                    }
                }
        );

        // 检查批量结果
        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0) {
                log.warn("库存预扣减失败: skuId={}, 需求={}, 库存可能不足",
                        validItems.get(i).getSkuId(), validItems.get(i).getQuantity());
                return false; // 事务将由调用方回滚
            }
        }
        log.debug("库存预扣减成功: {}条", validItems.size());
        return true;
    }

    /**
     * 释放库存（订单取消/超时）
     *
     * <p>🔒 M-09修复: 使用批量 UPDATE 替代逐条 UPDATE</p>
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void releaseStock(List<OrderItem> orderItems) {
        List<OrderItem> validItems = new ArrayList<>();
        for (OrderItem item : orderItems) {
            if (item.getSkuId() != null && item.getQuantity() != null) {
                validItems.add(item);
            }
        }
        if (validItems.isEmpty()) {
            return;
        }

        int[] results = jdbcTemplate.batchUpdate(
                "UPDATE product_sku SET stock = stock + ?, locked_stock = locked_stock - ? "
                        + "WHERE id = ? AND locked_stock >= ?",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        OrderItem item = validItems.get(i);
                        ps.setLong(1, item.getQuantity());
                        ps.setLong(2, item.getQuantity());
                        ps.setLong(3, item.getSkuId());
                        ps.setLong(4, item.getQuantity());
                    }

                    @Override
                    public int getBatchSize() {
                        return validItems.size();
                    }
                }
        );

        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0) {
                log.warn("库存释放失败: skuId={}, 需人工核查", validItems.get(i).getSkuId());
            }
        }
    }

    /**
     * 扣减已锁定库存（支付成功后）
     *
     * <p>🔒 M-09修复: 使用批量 UPDATE 替代逐条 UPDATE</p>
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void confirmDeduct(List<OrderItem> orderItems) {
        List<OrderItem> validItems = new ArrayList<>();
        for (OrderItem item : orderItems) {
            if (item.getSkuId() != null && item.getQuantity() != null) {
                validItems.add(item);
            }
        }
        if (validItems.isEmpty()) {
            return;
        }

        int[] results = jdbcTemplate.batchUpdate(
                "UPDATE product_sku SET locked_stock = locked_stock - ? WHERE id = ? AND locked_stock >= ?",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        OrderItem item = validItems.get(i);
                        ps.setLong(1, item.getQuantity());
                        ps.setLong(2, item.getSkuId());
                        ps.setLong(3, item.getQuantity());
                    }

                    @Override
                    public int getBatchSize() {
                        return validItems.size();
                    }
                }
        );

        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0) {
                log.error("已锁定库存扣减失败: skuId={}", validItems.get(i).getSkuId());
            }
        }
    }
}
