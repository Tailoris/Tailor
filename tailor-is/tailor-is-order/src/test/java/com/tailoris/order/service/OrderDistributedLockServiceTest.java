package com.tailoris.order.service;

import com.tailoris.common.lock.DistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("OrderDistributedLockService 测试")
@ExtendWith(MockitoExtension.class)
class OrderDistributedLockServiceTest {

    @Mock
    private DistributedLock distributedLock;

    @InjectMocks
    private OrderDistributedLockService orderDistributedLockService;

    @Test
    @DisplayName("执行订单创建-成功")
    void testExecuteOrderCreation_Success() {
        String requestId = "req-001";
        String expected = "order-001";

        when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn("token");
        when(distributedLock.unlock(anyString(), anyString())).thenReturn(true);

        Supplier<String> supplier = () -> expected;
        String result = orderDistributedLockService.executeOrderCreation(requestId, supplier);

        assertEquals(expected, result);
        verify(distributedLock).tryLock(anyString(), anyLong(), any(TimeUnit.class));
        verify(distributedLock).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("执行订单创建-获取锁失败")
    void testExecuteOrderCreation_LockFailed() {
        String requestId = "req-001";

        when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);

        Supplier<String> supplier = () -> "order-001";

        assertThrows(DistributedLock.LockAcquisitionException.class,
                () -> orderDistributedLockService.executeOrderCreation(requestId, supplier));

        verify(distributedLock).tryLock(anyString(), anyLong(), any(TimeUnit.class));
        verify(distributedLock, never()).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("执行库存预扣减-成功")
    void testExecuteStockDeduction_Success() {
        Long productId = 100L;
        Boolean expected = true;

        when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn("token");
        when(distributedLock.unlock(anyString(), anyString())).thenReturn(true);

        Supplier<Boolean> supplier = () -> expected;
        Boolean result = orderDistributedLockService.executeStockDeduction(productId, supplier);

        assertEquals(expected, result);
        verify(distributedLock).tryLock(anyString(), anyLong(), any(TimeUnit.class));
        verify(distributedLock).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("执行库存预扣减-业务异常")
    void testExecuteStockDeduction_BusinessException() {
        Long productId = 100L;

        when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn("token");
        when(distributedLock.unlock(anyString(), anyString())).thenReturn(true);

        Supplier<Boolean> supplier = () -> {
            throw new RuntimeException("业务异常");
        };

        assertThrows(RuntimeException.class,
                () -> orderDistributedLockService.executeStockDeduction(productId, supplier));

        verify(distributedLock).unlock(anyString(), anyString());
    }
}
