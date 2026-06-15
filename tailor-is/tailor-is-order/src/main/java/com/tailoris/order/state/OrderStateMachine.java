package com.tailoris.order.state;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.order.constant.OrderConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机 - 修复 B-M37
 *
 * <p>使用状态机模式管理订单状态流转，确保状态转换的合法性。</p>
 *
 * <h3>状态流转图</h3>
 * <pre>
 * PENDING_PAY(0:待支付) → PAID(1:已支付) → SHIPPED(2:已发货) → COMPLETED(3:已完成)
 *      ↓
 * CANCELLED(4:已取消)
 *      ↓
 * PENDING_PAY → REFUNDING(5:退款中) → REFUNDED(6:已退款)
 * PAID → REFUNDING → REFUNDED
 * SHIPPED → REFUNDING → REFUNDED
 * </pre>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
public final class OrderStateMachine {

    private static final Map<Integer, Set<Integer>> TRANSITIONS = new HashMap<>();

    static {
        // 待支付 → 已支付、已取消
        TRANSITIONS.put(OrderConstants.ORDER_STATUS_PENDING_PAY, Set.of(
                OrderConstants.ORDER_STATUS_PAID,
                OrderConstants.ORDER_STATUS_CANCELLED,
                OrderConstants.ORDER_STATUS_REFUNDING
        ));
        // 已支付 → 已发货、退款中
        TRANSITIONS.put(OrderConstants.ORDER_STATUS_PAID, Set.of(
                OrderConstants.ORDER_STATUS_SHIPPED,
                OrderConstants.ORDER_STATUS_REFUNDING
        ));
        // 已发货 → 已完成、退款中
        TRANSITIONS.put(OrderConstants.ORDER_STATUS_SHIPPED, Set.of(
                OrderConstants.ORDER_STATUS_COMPLETED,
                OrderConstants.ORDER_STATUS_REFUNDING
        ));
        // 退款中 → 已退款
        TRANSITIONS.put(OrderConstants.ORDER_STATUS_REFUNDING, Set.of(
                OrderConstants.ORDER_STATUS_REFUNDED,
                OrderConstants.ORDER_STATUS_PAID
        ));
        // 已完成（终态）→ 不可转换
        TRANSITIONS.put(OrderConstants.ORDER_STATUS_COMPLETED, Set.of());
        // 已取消（终态）→ 不可转换
        TRANSITIONS.put(OrderConstants.ORDER_STATUS_CANCELLED, Set.of());
        // 已退款（终态）→ 不可转换
        TRANSITIONS.put(OrderConstants.ORDER_STATUS_REFUNDED, Set.of());
    }

    private OrderStateMachine() {
        throw new IllegalStateException("工具类不允许实例化");
    }

    /**
     * 验证状态转换是否合法
     *
     * @param fromStatus 当前状态
     * @param toStatus 目标状态
     * @throws BusinessException 状态转换非法时抛出
     */
    public static void verifyTransition(Integer fromStatus, Integer toStatus) {
        if (fromStatus == null || toStatus == null) {
            throw new BusinessException("订单状态不能为空");
        }
        if (fromStatus.equals(toStatus)) {
            return; // 相同状态无需转换
        }
        Set<Integer> allowedTargets = TRANSITIONS.get(fromStatus);
        if (allowedTargets == null) {
            throw new BusinessException("未知订单状态: " + fromStatus);
        }
        if (!allowedTargets.contains(toStatus)) {
            throw new BusinessException(
                    String.format("订单状态不允许从[%s]转换为[%s]", fromStatus, toStatus));
        }
    }

    /**
     * 是否为终态
     */
    public static boolean isFinalStatus(Integer status) {
        if (status == null) {
            return false;
        }
        Set<Integer> allowedTargets = TRANSITIONS.get(status);
        return allowedTargets != null && allowedTargets.isEmpty();
    }

    /**
     * 是否可取消
     */
    public static boolean canCancel(Integer currentStatus) {
        return currentStatus != null && currentStatus == OrderConstants.ORDER_STATUS_PENDING_PAY;
    }

    /**
     * 是否可支付
     */
    public static boolean canPay(Integer currentStatus) {
        return currentStatus != null && currentStatus == OrderConstants.ORDER_STATUS_PENDING_PAY;
    }

    /**
     * 是否可发货
     */
    public static boolean canShip(Integer currentStatus) {
        return currentStatus != null && currentStatus == OrderConstants.ORDER_STATUS_PAID;
    }

    /**
     * 是否可退款
     */
    public static boolean canRefund(Integer currentStatus) {
        return TRANSITIONS.getOrDefault(currentStatus, Set.of())
                .contains(OrderConstants.ORDER_STATUS_REFUNDING);
    }

    /**
     * 是否可确认收货
     */
    public static boolean canConfirm(Integer currentStatus) {
        return currentStatus != null && currentStatus == OrderConstants.ORDER_STATUS_SHIPPED;
    }
}
