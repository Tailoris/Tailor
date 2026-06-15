package com.tailoris.order.state;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.order.constant.OrderConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单状态机测试 - 覆盖 B-M37
 *
 * @author Tailor IS Team
 */
@DisplayName("OrderStateMachine 测试")
class OrderStateMachineTest {

    @Test
    @DisplayName("待支付 -> 已支付 转换合法")
    void shouldAllowPendingPayToPaid() {
        assertDoesNotThrow(() -> OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_PENDING_PAY,
                OrderConstants.ORDER_STATUS_PAID));
    }

    @Test
    @DisplayName("待支付 -> 已取消 转换合法")
    void shouldAllowPendingPayToCancelled() {
        assertDoesNotThrow(() -> OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_PENDING_PAY,
                OrderConstants.ORDER_STATUS_CANCELLED));
    }

    @Test
    @DisplayName("已支付 -> 已发货 转换合法")
    void shouldAllowPaidToShipped() {
        assertDoesNotThrow(() -> OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_PAID,
                OrderConstants.ORDER_STATUS_SHIPPED));
    }

    @Test
    @DisplayName("已发货 -> 已完成 转换合法")
    void shouldAllowShippedToCompleted() {
        assertDoesNotThrow(() -> OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_SHIPPED,
                OrderConstants.ORDER_STATUS_COMPLETED));
    }

    @Test
    @DisplayName("已完成 -> 任何状态 转换非法（终态）")
    void shouldRejectFromFinalState() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OrderStateMachine.verifyTransition(
                        OrderConstants.ORDER_STATUS_COMPLETED,
                        OrderConstants.ORDER_STATUS_CANCELLED));
        assertTrue(ex.getMessage().contains("不允许"));
    }

    @Test
    @DisplayName("已取消 -> 已支付 转换非法（终态）")
    void shouldRejectFromCancelledState() {
        assertThrows(BusinessException.class,
                () -> OrderStateMachine.verifyTransition(
                        OrderConstants.ORDER_STATUS_CANCELLED,
                        OrderConstants.ORDER_STATUS_PAID));
    }

    @Test
    @DisplayName("已支付 -> 已完成 不允许跳级")
    void shouldRejectIllegalSkipTransition() {
        assertThrows(BusinessException.class,
                () -> OrderStateMachine.verifyTransition(
                        OrderConstants.ORDER_STATUS_PAID,
                        OrderConstants.ORDER_STATUS_COMPLETED));
    }

    @Test
    @DisplayName("相同状态无需转换")
    void shouldAllowSameState() {
        assertDoesNotThrow(() -> OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_PENDING_PAY,
                OrderConstants.ORDER_STATUS_PENDING_PAY));
    }

    @Test
    @DisplayName("null状态抛出异常")
    void shouldThrowForNullStatus() {
        assertThrows(BusinessException.class,
                () -> OrderStateMachine.verifyTransition(null, 1));
        assertThrows(BusinessException.class,
                () -> OrderStateMachine.verifyTransition(1, null));
    }

    @Test
    @DisplayName("未知状态抛出异常")
    void shouldThrowForUnknownStatus() {
        assertThrows(BusinessException.class,
                () -> OrderStateMachine.verifyTransition(999, 1));
    }

    @Test
    @DisplayName("已支付 -> 退款中 合法")
    void shouldAllowPaidToRefunding() {
        assertDoesNotThrow(() -> OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_PAID,
                OrderConstants.ORDER_STATUS_REFUNDING));
    }

    @Test
    @DisplayName("isFinalStatus 应正确判断终态")
    void shouldDetectFinalStatus() {
        assertTrue(OrderStateMachine.isFinalStatus(OrderConstants.ORDER_STATUS_COMPLETED));
        assertTrue(OrderStateMachine.isFinalStatus(OrderConstants.ORDER_STATUS_CANCELLED));
        assertTrue(OrderStateMachine.isFinalStatus(OrderConstants.ORDER_STATUS_REFUNDED));
        assertFalse(OrderStateMachine.isFinalStatus(OrderConstants.ORDER_STATUS_PENDING_PAY));
        assertFalse(OrderStateMachine.isFinalStatus(OrderConstants.ORDER_STATUS_PAID));
    }

    @Test
    @DisplayName("canCancel 仅待支付可取消")
    void shouldCheckCanCancel() {
        assertTrue(OrderStateMachine.canCancel(OrderConstants.ORDER_STATUS_PENDING_PAY));
        assertFalse(OrderStateMachine.canCancel(OrderConstants.ORDER_STATUS_PAID));
        assertFalse(OrderStateMachine.canCancel(OrderConstants.ORDER_STATUS_SHIPPED));
        assertFalse(OrderStateMachine.canCancel(null));
    }

    @Test
    @DisplayName("canPay 仅待支付可支付")
    void shouldCheckCanPay() {
        assertTrue(OrderStateMachine.canPay(OrderConstants.ORDER_STATUS_PENDING_PAY));
        assertFalse(OrderStateMachine.canPay(OrderConstants.ORDER_STATUS_PAID));
    }

    @Test
    @DisplayName("canShip 仅已支付可发货")
    void shouldCheckCanShip() {
        assertTrue(OrderStateMachine.canShip(OrderConstants.ORDER_STATUS_PAID));
        assertFalse(OrderStateMachine.canShip(OrderConstants.ORDER_STATUS_PENDING_PAY));
    }
}
