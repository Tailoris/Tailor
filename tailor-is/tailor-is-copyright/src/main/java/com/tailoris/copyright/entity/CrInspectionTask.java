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
@TableName("cr_inspection_task")
public class CrInspectionTask extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("task_type")
    private Integer taskType;

    @TableField("task_name")
    private String taskName;

    @TableField("target_type")
    private Integer targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("check_items")
    private String checkItems;

    @TableField("check_result")
    private Integer checkResult;

    @TableField("result_detail")
    private String resultDetail;

    @TableField("evidence")
    private String evidence;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("executor_id")
    private Long executorId;

    @TableField("executor_type")
    private Integer executorType;

    @TableField("status")
    private Integer status;

    @TableField("scheduled_time")
    private LocalDateTime scheduledTime;

    @TableField("next_run_time")
    private LocalDateTime nextRunTime;

    @TableField("cron_expr")
    private String cronExpr;
}
