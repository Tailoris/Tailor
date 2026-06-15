package com.tailoris.copyright.entity;

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
@TableName("infringement_record")
public class InfringementRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("report_no")
    private String reportNo;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("copyright_id")
    private Long copyrightId;

    @TableField("reported_product_id")
    private Long reportedProductId;

    @TableField("reported_user_id")
    private Long reportedUserId;

    @TableField("reported_shop_id")
    private Long reportedShopId;

    @TableField("reported_merchant_id")
    private Long reportedMerchantId;

    @TableField("infringement_type")
    private Integer infringementType;

    @TableField("reason")
    private String reason;

    @TableField("description")
    private String description;

    @TableField("evidence_images")
    private String evidenceImages;

    @TableField("evidence_files")
    private String evidenceFiles;

    @TableField("evidence_urls")
    private String evidenceUrls;

    @TableField("comparison_description")
    private String comparisonDescription;

    @TableField("status")
    private Integer status;

    @TableField("urgency")
    private Integer urgency;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handler_remark")
    private String handlerRemark;

    @TableField("handled_at")
    private LocalDateTime handledAt;

    @TableField("punishment_type")
    private Integer punishmentType;

    @TableField("punishment_detail")
    private String punishmentDetail;
}
