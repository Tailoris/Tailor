package com.tailoris.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_info")
public class OrderInfo extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("parent_order_no")
    private String parentOrderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("shop_id")
    private Long shopId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("product_type")
    private Integer productType;

    @TableField("status")
    private Integer status;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("coupon_amount")
    private BigDecimal couponAmount;

    @TableField("points_amount")
    private BigDecimal pointsAmount;

    @TableField("freight_amount")
    private BigDecimal freightAmount;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("pay_type")
    private Integer payType;

    @TableField("pay_status")
    private Integer payStatus;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("address_snapshot")
    private String addressSnapshot;

    @TableField("remark")
    private String remark;

    @TableField("seller_remark")
    private String sellerRemark;

    @TableField("invoice_type")
    private Integer invoiceType;

    @TableField("invoice_content")
    private String invoiceContent;

    @TableField("coupon_id")
    private Long couponId;

    @TableField("points_used")
    private Integer pointsUsed;

    @TableField("logistics_no")
    private String logisticsNo;

    @TableField("ship_time")
    private LocalDateTime shipTime;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @TableField("confirm_receive_time")
    private LocalDateTime confirmReceiveTime;

    @TableField(exist = false)
    private java.util.List<OrderItem> orderItems;

    @TableField(exist = false)
    private OrderLogistics logistics;
}
