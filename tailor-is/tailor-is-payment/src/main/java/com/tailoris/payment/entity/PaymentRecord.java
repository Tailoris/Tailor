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
@TableName("payment_record")
public class PaymentRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("payment_no")
    private String paymentNo;

    @TableField("user_id")
    private Long userId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("pay_channel")
    private Integer payChannel;

    @TableField("pay_method")
    private String payMethod;

    @TableField("pay_status")
    private Integer payStatus;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("transaction_id")
    private String transactionId;

    @TableField("channel_request")
    private String channelRequest;

    @TableField("channel_response")
    private String channelResponse;

    @TableField("notify_url")
    private String notifyUrl;

    @TableField("notify_status")
    private Integer notifyStatus;

    @TableField("notify_time")
    private LocalDateTime notifyTime;

    @TableField("client_ip")
    private String clientIp;

    @TableField("device_type")
    private Integer deviceType;

    @TableField("remark")
    private String remark;
}
