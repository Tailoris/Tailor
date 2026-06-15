package com.tailoris.payment.entity;

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
@TableName("refund_record")
public class RefundRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("ticket_no")
    private String ticketNo;

    @TableField("refund_no")
    private String refundNo;

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

    @TableField("amount")
    private BigDecimal amount;

    @TableField("refund_channel")
    private Integer refundChannel;

    @TableField("refund_status")
    private Integer refundStatus;

    @TableField("refund_time")
    private LocalDateTime refundTime;

    @TableField("channel_refund_no")
    private String channelRefundNo;

    @TableField("channel_request")
    private String channelRequest;

    @TableField("channel_response")
    private String channelResponse;

    @TableField("fail_reason")
    private String failReason;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("remark")
    private String remark;
}
