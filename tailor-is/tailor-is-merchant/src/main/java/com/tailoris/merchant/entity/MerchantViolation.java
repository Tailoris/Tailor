package com.tailoris.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商家违规处罚记录 - MER-007.
 *
 * <h3>违规类型 violation_type</h3>
 * <ul>
 *   <li>1: 商品违规（假货、违禁品、信息不符）</li>
 *   <li>2: 价格违规（虚假折扣、价格欺诈）</li>
 *   <li>3: 虚假宣传（夸大、虚假承诺）</li>
 *   <li>4: 售后违规（拒绝售后、拖延退款）</li>
 *   <li>5: 资质过期（营业执照、许可证过期）</li>
 *   <li>6: 其他</li>
 * </ul>
 *
 * <h3>违规级别 violation_level</h3>
 * <ul>
 *   <li>1: 轻微</li>
 *   <li>2: 一般</li>
 *   <li>3: 严重</li>
 *   <li>4: 特别严重</li>
 * </ul>
 *
 * <h3>处罚类型 punishment_type</h3>
 * <ul>
 *   <li>0: 待定</li>
 *   <li>1: 警告</li>
 *   <li>2: 限流（商品曝光降权）</li>
 *   <li>3: 下架（强制下架违规商品）</li>
 *   <li>4: 封禁（关闭店铺）</li>
 *   <li>5: 清退（永久封禁）</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_violation")
public class MerchantViolation extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家ID */
    @TableField("merchant_id")
    private Long merchantId;

    /** 关联店铺ID */
    @TableField("shop_id")
    private Long shopId;

    /** 违规类型 1=商品 2=价格 3=虚假宣传 4=售后 5=资质 6=其他 */
    @TableField("violation_type")
    private Integer violationType;

    /** 违规级别 1=轻微 2=一般 3=严重 4=特别严重 */
    @TableField("violation_level")
    private Integer violationLevel;

    /** 违规标题 */
    @TableField("title")
    private String title;

    /** 违规描述 */
    @TableField("description")
    private String description;

    /** 违规证据（JSON） */
    @TableField("evidence")
    private String evidence;

    /** 处罚类型 0=待定 1=警告 2=限流 3=下架 4=封禁 5=清退 */
    @TableField("punishment_type")
    private Integer punishmentType;

    /** 处罚天数（0=永久） */
    @TableField("punishment_days")
    private Integer punishmentDays;

    /** 处罚开始时间 */
    @TableField("punishment_start")
    private LocalDateTime punishmentStart;

    /** 处罚结束时间（永久为 null） */
    @TableField("punishment_end")
    private LocalDateTime punishmentEnd;

    /** 状态 0=待处理 1=已处罚 2=已申诉 3=已撤销 4=已解除 */
    @TableField("status")
    private Integer status;

    /** 是否申诉 0否 1是 */
    @TableField("is_appealed")
    private Integer isAppealed;

    /** 申诉内容 */
    @TableField("appeal_content")
    private String appealContent;

    /** 申诉时间 */
    @TableField("appeal_time")
    private LocalDateTime appealTime;

    /** 申诉处理结果 */
    @TableField("appeal_result")
    private String appealResult;

    /** 举报人ID（系统举报为空） */
    @TableField("reporter_id")
    private Long reporterId;

    /** 处理人ID */
    @TableField("handler_id")
    private Long handlerId;

    /** 处理时间 */
    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
