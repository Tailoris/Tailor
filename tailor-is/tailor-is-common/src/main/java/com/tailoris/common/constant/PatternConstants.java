package com.tailoris.common.constant;

/**
 * 纸样设计模块业务常量.
 *
 * <p>封装纸样定制流程状态、纸样评分维度、纸样尺寸检测等业务常量，
 * 禁止在页面代码中硬编码这些值。</p>
 *
 * <h3>纸样定制流程</h3>
 * <ol>
 *   <li>需求审核（REQUIREMENT_REVIEW）</li>
 *   <li>初版制作（FIRST_DRAFT）</li>
 *   <li>修改优化（REVISION）</li>
 *   <li>定稿交付（DELIVERY）</li>
 *   <li>售后服务（AFTER_SALE）</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 1.2.0
 */
public final class PatternConstants {

    private PatternConstants() {
    }

    // ===== 纸样定制流程状态 =====
    /** 需求审核中 */
    public static final int PATTERN_STATUS_REQUIREMENT_REVIEW = 0;
    /** 初版制作中 */
    public static final int PATTERN_STATUS_FIRST_DRAFT = 1;
    /** 修改优化中 */
    public static final int PATTERN_STATUS_REVISION = 2;
    /** 定稿交付 */
    public static final int PATTERN_STATUS_DELIVERY = 3;
    /** 售后中 */
    public static final int PATTERN_STATUS_AFTER_SALE = 4;
    /** 已完成 */
    public static final int PATTERN_STATUS_COMPLETED = 5;
    /** 已取消 */
    public static final int PATTERN_STATUS_CANCELLED = 6;

    // ===== 纸样评分维度（固定三大标签） =====
    /** 版型精准度（评分权重 40%） */
    public static final String RATING_DIMENSION_ACCURACY = "pattern_accuracy";
    /** 工艺清晰度（评分权重 35%） */
    public static final String RATING_DIMENSION_CLARITY = "craft_clarity";
    /** 适配性（评分权重 25%） */
    public static final String RATING_DIMENSION_ADAPTABILITY = "adaptability";

    /** 版型精准度权重 */
    public static final double RATING_WEIGHT_ACCURACY = 0.40;
    /** 工艺清晰度权重 */
    public static final double RATING_WEIGHT_CLARITY = 0.35;
    /** 适配性权重 */
    public static final double RATING_WEIGHT_ADAPTABILITY = 0.25;

    /** 评分范围：最低分 */
    public static final int RATING_MIN = 1;
    /** 评分范围：最高分 */
    public static final int RATING_MAX = 5;

    // ===== 纸样尺寸检测 =====
    /** 标准 A3 尺寸（宽×高 mm） */
    public static final int PAPER_SIZE_A3_WIDTH = 297;
    public static final int PAPER_SIZE_A3_HEIGHT = 420;
    /** 标准 A4 尺寸（宽×高 mm） */
    public static final int PAPER_SIZE_A4_WIDTH = 210;
    public static final int PAPER_SIZE_A4_HEIGHT = 297;
    /** 自定义尺寸最大限制（mm） */
    public static final int PAPER_SIZE_CUSTOM_MAX = 1200;
    /** 自定义尺寸最小限制（mm） */
    public static final int PAPER_SIZE_CUSTOM_MIN = 50;

    /** 尺寸检测精度容差（mm） */
    public static final double PAPER_SIZE_TOLERANCE_MM = 0.5;

    // ===== 纸样文件相关 =====
    /** 支持的文件格式 */
    public static final String[] SUPPORTED_FILE_FORMATS = {"PDF", "DXF", "AI", "PSD"};
    /** 文件上传最大大小（MB） */
    public static final int FILE_UPLOAD_MAX_SIZE_MB = 100;
    /** 纸样文件加密算法 */
    public static final String FILE_ENCRYPTION_ALGORITHM = "AES-256-GCM";

    // ===== 人体尺寸库相关 =====
    /** 体型分类：标准 */
    public static final String BODY_TYPE_STANDARD = "standard";
    /** 体型分类：大码 */
    public static final String BODY_TYPE_PLUS_SIZE = "plus_size";
    /** 体型分类：娇小 */
    public static final String BODY_TYPE_PETITE = "petite";

    // ===== 版师相关 =====
    /** 版师审核状态：待审核 */
    public static final int PATTERN_MAKER_STATUS_PENDING = 0;
    /** 版师审核状态：已通过 */
    public static final int PATTERN_MAKER_STATUS_APPROVED = 1;
    /** 版师审核状态：已拒绝 */
    public static final int PATTERN_MAKER_STATUS_REJECTED = 2;
}