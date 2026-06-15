package com.tailoris.api.order.entity;

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
@TableName("after_sale_ticket")
public class AfterSaleTicket extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("ticket_no")
    private String ticketNo;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("order_item_id")
    private Long orderItemId;

    @TableField("user_id")
    private Long userId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("shop_id")
    private Long shopId;

    @TableField("product_id")
    private Long productId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("ticket_type")
    private Integer ticketType;

    @TableField("status")
    private Integer status;

    @TableField("reason")
    private String reason;

    @TableField("description")
    private String description;

    @TableField("images")
    private String images;

    @TableField("video_url")
    private String videoUrl;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("refund_quantity")
    private Integer refundQuantity;

    @TableField("return_logistics_company")
    private String returnLogisticsCompany;

    @TableField("return_logistics_no")
    private String returnLogisticsNo;

    @TableField("merchant_remark")
    private String merchantRemark;

    @TableField("merchant_handle_time")
    private LocalDateTime merchantHandleTime;

    @TableField("platform_intervene")
    private Integer platformIntervene;

    @TableField("platform_handler")
    private Long platformHandler;

    @TableField("platform_result")
    private String platformResult;

    @TableField("platform_handle_time")
    private LocalDateTime platformHandleTime;

    @TableField("processed_at")
    private LocalDateTime processedAt;
}
