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
@TableName("course")
public class Course extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 课程标题 */
    @TableField("title")
    private String title;

    /** 课程描述 */
    @TableField("description")
    private String description;

    /** 封面图片地址 */
    @TableField("cover_image")
    private String coverImage;

    /** 视频地址 */
    @TableField("video_url")
    private String videoUrl;

    /** 分类ID */
    @TableField("category_id")
    private Long categoryId;

    /** 课程时长（分钟） */
    @TableField("duration")
    private Integer duration;

    /** 浏览量 */
    @TableField("view_count")
    private Long viewCount;

    /** 状态：0下架 1上架 */
    @TableField("status")
    private Integer status;

    /** 作者/讲师用户ID */
    @TableField("author_id")
    private Long authorId;
}