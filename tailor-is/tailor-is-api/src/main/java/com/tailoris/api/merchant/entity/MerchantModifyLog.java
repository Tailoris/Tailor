package com.tailoris.api.merchant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("merchant_modify_log")
public class MerchantModifyLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("shop_id")
    private Long shopId;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_type")
    private Integer operatorType;

    @TableField("operate_type")
    private Integer operateType;

    @TableField("operate_module")
    private String operateModule;

    @TableField("field_name")
    private String fieldName;

    @TableField("value_before")
    private String valueBefore;

    @TableField("value_after")
    private String valueAfter;

    @TableField("remark")
    private String remark;

    @TableField("ip")
    private String ip;

    @TableField("user_agent")
    private String userAgent;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
