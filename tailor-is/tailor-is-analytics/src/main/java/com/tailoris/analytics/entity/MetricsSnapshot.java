package com.tailoris.analytics.entity;

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
@TableName("metrics_snapshot")
public class MetricsSnapshot extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 指标类型：revenue/order/user/view等 */
    @TableField("metric_type")
    private String metricType;

    /** 指标键名 */
    @TableField("metric_key")
    private String metricKey;

    /** 指标数值 */
    @TableField("metric_value")
    private BigDecimal metricValue;

    /** 快照日期 */
    @TableField("snapshot_date")
    private LocalDate snapshotDate;

    /** 维度信息（JSON格式） */
    @TableField("dimension")
    private String dimension;
}