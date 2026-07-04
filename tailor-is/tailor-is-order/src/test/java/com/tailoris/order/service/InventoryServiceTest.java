package com.tailoris.order.service;

import com.tailoris.order.entity.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InventoryService 单元测试
 * <p>🔒 H-04修复: 补充单元测试，验证批量 UPDATE 库存操作的正确性</p>
 */
@DisplayName("InventoryService 测试")
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private InventoryService inventoryService;

    private OrderItem buildOrderItem(Long skuId, Integer quantity) {
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    @DisplayName("预扣减库存成功 - 批量UPDATE")
    void testDeductStock_Success() {
        OrderItem item1 = buildOrderItem(100L, 2);
        OrderItem item2 = buildOrderItem(101L, 1);

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 1});

        boolean result = inventoryService.deductStock(Arrays.asList(item1, item2));

        assertTrue(result);
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("预扣减库存失败 - 库存不足")
    void testDeductStock_InsufficientStock() {
        OrderItem item = buildOrderItem(100L, 5);

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{0});

        boolean result = inventoryService.deductStock(Collections.singletonList(item));

        assertFalse(result);
    }

    @Test
    @DisplayName("预扣减库存 - 部分成功部分失败")
    void testDeductStock_PartialFailure() {
        OrderItem item1 = buildOrderItem(100L, 2);
        OrderItem item2 = buildOrderItem(101L, 10);

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 0});

        boolean result = inventoryService.deductStock(Arrays.asList(item1, item2));

        assertFalse(result);
    }

    @Test
    @DisplayName("预扣减库存 - 跳过无效明细")
    void testDeductStock_SkipInvalidItem() {
        OrderItem item1 = buildOrderItem(null, 2);
        OrderItem item2 = buildOrderItem(100L, null);
        OrderItem item3 = buildOrderItem(101L, 0);
        OrderItem item4 = buildOrderItem(102L, 1);

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1});

        boolean result = inventoryService.deductStock(Arrays.asList(item1, item2, item3, item4));

        assertTrue(result);
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("预扣减库存 - 空列表")
    void testDeductStock_EmptyList() {
        boolean result = inventoryService.deductStock(Collections.emptyList());
        assertTrue(result);
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("释放库存成功")
    void testReleaseStock_Success() {
        OrderItem item1 = buildOrderItem(100L, 2);
        OrderItem item2 = buildOrderItem(101L, 1);

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 1});

        assertDoesNotThrow(() -> inventoryService.releaseStock(Arrays.asList(item1, item2)));
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("释放库存 - 跳过无效明细")
    void testReleaseStock_SkipInvalidItem() {
        OrderItem item1 = buildOrderItem(null, 2);
        OrderItem item2 = buildOrderItem(100L, null);

        assertDoesNotThrow(() -> inventoryService.releaseStock(Arrays.asList(item1, item2)));
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("释放库存 - 部分失败不抛异常")
    void testReleaseStock_PartialFailure() {
        OrderItem item1 = buildOrderItem(100L, 2);
        OrderItem item2 = buildOrderItem(101L, 1);

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 0});

        assertDoesNotThrow(() -> inventoryService.releaseStock(Arrays.asList(item1, item2)));
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("扣减已锁定库存成功")
    void testConfirmDeduct_Success() {
        OrderItem item1 = buildOrderItem(100L, 2);
        OrderItem item2 = buildOrderItem(101L, 1);

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 1});

        assertDoesNotThrow(() -> inventoryService.confirmDeduct(Arrays.asList(item1, item2)));
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("扣减已锁定库存 - 跳过无效明细")
    void testConfirmDeduct_SkipInvalidItem() {
        OrderItem item1 = buildOrderItem(null, 2);
        OrderItem item2 = buildOrderItem(100L, null);

        assertDoesNotThrow(() -> inventoryService.confirmDeduct(Arrays.asList(item1, item2)));
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("扣减已锁定库存 - 部分失败不抛异常")
    void testConfirmDeduct_PartialFailure() {
        OrderItem item1 = buildOrderItem(100L, 2);
        OrderItem item2 = buildOrderItem(101L, 1);

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 0});

        assertDoesNotThrow(() -> inventoryService.confirmDeduct(Arrays.asList(item1, item2)));
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }
}
