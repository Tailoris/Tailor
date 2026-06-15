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
@TableName("cr_blockchain_event")
public class CrBlockchainEvent extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("record_id")
    private Long recordId;

    @TableField("event_type")
    private String eventType;

    @TableField("tx_hash")
    private String txHash;

    @TableField("block_height")
    private Long blockHeight;

    @TableField("block_time")
    private java.time.LocalDateTime blockTime;

    @TableField("platform")
    private String platform;

    @TableField("node")
    private String node;

    @TableField("event_data")
    private String eventData;

    @TableField("processed")
    private Integer processed;

    @TableField("process_time")
    private java.time.LocalDateTime processTime;

    @TableField("process_result")
    private String processResult;

    @TableField("retry_count")
    private Integer retryCount;
}
