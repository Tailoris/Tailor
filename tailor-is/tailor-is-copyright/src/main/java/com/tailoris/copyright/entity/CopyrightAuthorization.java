package com.tailoris.copyright.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("copyright_license")
public class CopyrightAuthorization extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("license_no")
    private String licenseNo;

    @TableField("copyright_id")
    private Long copyrightId;

    @TableField("licensor_id")
    private Long licensorId;

    @TableField("licensee_id")
    private Long licenseeId;

    @TableField("license_type")
    private Integer licenseType;

    @TableField("scope")
    private Integer scope;

    @TableField("authorized_products")
    private String authorizedProducts;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("license_fee")
    private BigDecimal licenseFee;

    @TableField("royalty_rate")
    private BigDecimal royaltyRate;

    @TableField("agreement_url")
    private String agreementUrl;

    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;
}
