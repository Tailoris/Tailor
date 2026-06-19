package com.tailoris.order.state;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.order.constant.OrderConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("订单状态机测试")
class OrderStateMachineTest {

    @Test
    @DisplayName("验证状态转换 - 待支付到已支付")
    void testVerifyTransition_PendingToPaid() {
        assertDoesNotThrow(() ->
            OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_PENDING_PAY,
                OrderConstants.ORDER_STATUS_PAID
            )
        );
    }

    @Test
    @DisplayName("验证状态转换 - 待支付到已取消")
    void testVerifyTransition_PendingToCancelled() {
        assertDoesNotThrow(() ->
            OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_PENDING_PAY,
                OrderConstants.ORDER_STATUS_CANCELLED
            )
        );
    }

    @Test
    @DisplayName("验证状态转换 - 已支付到已发货")
    void testVerifyTransition_PaidToShipped() {
        assertDoesNotThrow(() ->
            OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_PAID,
                OrderConstants.ORDER_STATUS_SHIPPED
            )
        );
    }

    @Test
    @DisplayName("验证状态转换 - 已发货到已完成")
    void testVerifyTransition_ShippedToCompleted() {
        assertDoesNotThrow(() ->
            OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_SHIPPED,
                OrderConstants.ORDER_STATUS_COMPLETED
            )
        );
    }

    @Test
    @DisplayName("验证状态转换 - 非法转换抛出异常")
    void testVerifyTransition_IllegalTransition() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
            OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_COMPLETED,
                OrderConstants.ORDER_STATUS_PAID
            )
        );
        assertTrue(exception.getMessage().contains("不允许"));
    }

    @Test
    @DisplayName("验证状态转换 - 相同状态不抛异常")
    void testVerifyTransition_SameStatus() {
        assertDoesNotThrow(() ->
            OrderStateMachine.verifyTransition(
                OrderConstants.ORDER_STATUS_PENDING_PAY,
                OrderConstants.ORDER_STATUS_PENDING_PAY
            )
        );
    }

    @Test
    @DisplayName("验证状态转换 - 空状态抛出异常")
    void testVerifyTransition_NullStatus() {
        assertThrows(BusinessException.class, () ->
            OrderStateMachine.verifyTransition(null, OrderConstants.ORDER_STATUS_PAID)
        );
        assertThrows(BusinessException.class, () ->
            OrderStateMachine.verifyTransition(OrderConstants.ORDER_STATUS_PENDING_PAY, null)
        );
    }

    @Test
    @DisplayName("验证状态转换 - 未知状态抛出异常")
    void testVerifyTransition_UnknownStatus() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
            OrderStateMachine.verifyTransition(999, OrderConstants.ORDER_STATUS_PAID)
        );
        assertTrue(exception.getMessage().contains("未知订单状态"));
    }

    @Test
    @DisplayName("判断终态 - 已完成是终态")
    void testIsFinalStatus_Completed() {
        assertTrue(OrderStateMachine.isFinalStatus(OrderConstants.ORDER_STATUS_COMPLETED));
    }

    @Test
    @DisplayName("判断终态 - 已取消是终态")
    void testIsFinalStatus_Cancelled() {
        assertTrue(OrderStateMachine.isFinalStatus(OrderConstants.ORDER_STATUS_CANCELLED));
    }

    @Test
    @DisplayName("判断终态 - 已退款是终态")
    void testIsFinalStatus_Refunded() {
        assertTrue(OrderStateMachine.isFinalStatus(OrderConstants.ORDER_STATUS_REFUNDED));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5})
    @DisplayName("判断终态 - 非终态返回false")
    void testIsFinalStatus_NotFinal(int status) {
        assertFalse(OrderStateMachine.isFinalStatus(status));
    }

    @Test
    @DisplayName("判断终态 - 空状态返回false")
    void testIsFinalStatus_Null() {
        assertFalse(OrderStateMachine.isFinalStatus(null));
    }

    @Test
    @DisplayName("判断可取消 - 待支付可取消")
    void testCanCancel_PendingPay() {
        assertTrue(OrderStateMachine.canCancel(OrderConstants.ORDER_STATUS_PENDING_PAY));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("判断可取消 - 其他状态不可取消")
    void testCanCancel_OtherStatus(int status) {
        assertFalse(OrderStateMachine.canCancel(status));
    }

    @Test
    @DisplayName("判断可取消 - 空状态返回false")
    void testCanCancel_Null() {
        assertFalse(OrderStateMachine.canCancel(null));
    }

    @Test
    @DisplayName("判断可支付 - 待支付可支付")
    void testCanPay_PendingPay() {
        assertTrue(OrderStateMachine.canPay(OrderConstants.ORDER_STATUS_PENDING_PAY));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("判断可支付 - 其他状态不可支付")
    void testCanPay_OtherStatus(int status) {
        assertFalse(OrderStateMachine.canPay(status));
    }

    @Test
    @DisplayName("判断可支付 - 空状态返回false")
    void testCanPay_Null() {
        assertFalse(OrderStateMachine.canPay(null));
    }

    @Test
    @DisplayName("判断可发货 - 已支付可发货")
    void testCanShip_Paid() {
        assertTrue(OrderStateMachine.canShip(OrderConstants.ORDER_STATUS_PAID));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 3, 4, 5, 6})
    @DisplayName("判断可发货 - 其他状态不可发货")
    void testCanShip_OtherStatus(int status) {
        assertFalse(OrderStateMachine.canShip(status));
    }

    @Test
    @DisplayName("判断可发货 - 空状态返回false")
    void testCanShip_Null() {
        assertFalse(OrderStateMachine.canShip(null));
    }

    @Test
    @DisplayName("判断可退款 - 待支付可退款")
    void testCanRefund_PendingPay() {
        assertTrue(OrderStateMachine.canRefund(OrderConstants.ORDER_STATUS_PENDING_PAY));
    }

    @Test
    @DisplayName("判断可退款 - 已支付可退款")
    void testCanRefund_Paid() {
        assertTrue(OrderStateMachine.canRefund(OrderConstants.ORDER_STATUS_PAID));
    }

    @Test
    @DisplayName("判断可退款 - 已发货可退款")
    void testCanRefund_Shipped() {
        assertTrue(OrderStateMachine.canRefund(OrderConstants.ORDER_STATUS_SHIPPED));
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 6})
    @DisplayName("判断可退款 - 终态不可退款")
    void testCanRefund_FinalStatus(int status) {
        assertFalse(OrderStateMachine.canRefund(status));
    }

    @Test
    @DisplayName("判断可确认收货 - 已发货可确认")
    void testCanConfirm_Shipped() {
        assertTrue(OrderStateMachine.canConfirm(OrderConstants.ORDER_STATUS_SHIPPED));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 4, 5, 6})
    @DisplayName("判断可确认收货 - 其他状态不可确认")
    void testCanConfirm_OtherStatus(int status) {
        assertFalse(OrderStateMachine.canConfirm(status));
    }

    @Test
    @DisplayName("判断可确认收货 - 空状态返回false")
    void testCanConfirm_Null() {
        assertFalse(OrderStateMachine.canConfirm(null));
    }
}
