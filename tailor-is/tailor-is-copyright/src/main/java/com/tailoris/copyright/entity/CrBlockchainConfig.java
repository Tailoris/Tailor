package com.tailoris.copyright.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cr_blockchain_config")
public class CrBlockchainConfig extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("platform_code")
    private String platformCode;

    @TableField("platform_name")
    private String platformName;

    @TableField("endpoint")
    private String endpoint;

    @TableField("api_key")
    private String apiKey;

    @TableField("api_secret")
    private String apiSecret;

    @TableField("contract_name")
    private String contractName;

    @TableField("chain_id")
    private String chainId;

    @TableField("is_default")
    private Integer isDefault;

    @TableField("is_active")
    private Integer isActive;

    @TableField("priority")
    private Integer priority;

    @TableField("qps_limit")
    private Integer qpsLimit;

    @TableField("daily_limit")
    private Long dailyLimit;

    @TableField("daily_used")
    private Long dailyUsed;

    @TableField("last_reset_date")
    private java.time.LocalDate lastResetDate;

    @TableField("remark")
    private String remark;
}
