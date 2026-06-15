package com.tailoris.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "product", autoResultMap = true)
public class Product extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("shop_id")
    private Long shopId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("product_type")
    private Integer productType;

    @TableField("name")
    private String name;

    @TableField("sub_title")
    private String subTitle;

    @TableField("main_image")
    private String mainImage;

    @TableField(value = "images", typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    @TableField("video_url")
    private String videoUrl;

    @TableField("description")
    private String description;

    @TableField(value = "specifications", typeHandler = JacksonTypeHandler.class)
    private Object specifications;

    @TableField("status")
    private Integer status;

    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_by")
    private Long auditBy;

    @TableField("copyright_flag")
    private Integer copyrightFlag;

    @TableField("copyright_id")
    private Long copyrightId;

    @TableField("brand_name")
    private String brandName;

    @TableField("weight")
    private BigDecimal weight;

    @TableField("length")
    private BigDecimal length;

    @TableField("width")
    private BigDecimal width;

    @TableField("height")
    private BigDecimal height;

    @TableField("freight_template_id")
    private Long freightTemplateId;

    @TableField("sale_count")
    private Integer saleCount;

    @TableField("total_stock")
    private Long totalStock;

    @TableField("min_price")
    private BigDecimal minPrice;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("favorable_rate")
    private BigDecimal favorableRate;

    @TableField("lower_shelf_reason")
    private String lowerShelfReason;
}
