package com.tailoris.academy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_chapter")
public class CourseChapter extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属课程ID */
    @TableField("course_id")
    private Long courseId;

    /** 章节标题 */
    @TableField("title")
    private String title;

    /** 章节视频地址 */
    @TableField("video_url")
    private String videoUrl;

    /** 章节时长（分钟） */
    @TableField("duration")
    private Integer duration;

    /** 排序序号 */
    @TableField("sort_order")
    private Integer sortOrder;
}