package com.tailoris.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_logistics")
public class OrderLogistics extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("logistics_company")
    private String logisticsCompany;

    @TableField("logistics_company_name")
    private String logisticsCompanyName;

    @TableField("logistics_no")
    private String logisticsNo;

    @TableField("status")
    private Integer status;

    @TableField("shipped_at")
    private LocalDateTime shippedAt;

    @TableField("delivered_at")
    private LocalDateTime deliveredAt;

    @TableField("logistics_info")
    private String logisticsInfo;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("receiver_address")
    private String receiverAddress;

    @TableField("remark")
    private String remark;
}
