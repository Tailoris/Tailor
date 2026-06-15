package com.tailoris.ai.service.impl;

import com.tailoris.ai.dto.BodySizeAnalysisRequest;
import com.tailoris.ai.dto.BodySizeAnalysisResponse;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.dto.PatternGenerateResponse;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI纸样生成服务实现 - 修复 B-M21/B-M22
 *
 * <p>提供AI纸样生成、身材尺寸分析等AI能力。
 * 使用规则引擎生成SVG纸样，实际生产应接入深度学习模型。</p>
 *
 * <p>关键修复：</p>
 * <ul>
 *   <li>B-M21: SVG生成使用StringBuilder替代String.format，提升可读性</li>
 *   <li>B-M22: 补充类级别Javadoc</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatternGenerateServiceImpl {

    /** 默认纸样宽度 (cm) */
    private static final double DEFAULT_WIDTH_CM = 200.0;

    /** 默认纸样高度 (cm) */
    private static final double DEFAULT_HEIGHT_CM = 300.0;

    /** 图案标题Y坐标 */
    private static final double TITLE_Y_POSITION = 20.0;

    /** 中线比例因子 */
    private static final double MIDLINE_RATIO = 0.5;

    /** 纸样ID前缀 */
    private static final String PATTERN_ID_PREFIX = "PAT-";

    // 🔒 B-L05修复: 使用SnowflakeIdGenerator替代UUID
    private final SpringSnowflakeIdGenerator snowflakeIdGenerator;

    public PatternGenerateResponse generatePattern(PatternGenerateRequest request) {
        String patternId = generatePatternId();
        String svgContent = generateSvgPattern(request);
        String previewUrl = "/api/v1/ai/patterns/" + patternId + "/preview";

        return PatternGenerateResponse.builder()
                .patternId(patternId)
                .name(request.getGarmentType() + "-" + patternId)
                .garmentType(request.getGarmentType())
                .svgContent(svgContent)
                .previewUrl(previewUrl)
                .dimensions(extractDimensions(request))
                .version(1)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public BodySizeAnalysisResponse analyzeBodySize(BodySizeAnalysisRequest request) {
        Map<String, String> recommendedSizes = calculateRecommendedSizes(request);

        return BodySizeAnalysisResponse.builder()
                .height(request.getHeight())
                .weight(request.getWeight())
                .chestCircumference(request.getChestCircumference())
                .waistCircumference(request.getWaistCircumference())
                .hipCircumference(request.getHipCircumference())
                .recommendedSizes(recommendedSizes)
                .confidence(new BigDecimal("0.85"))
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 生成纸样ID - 修复 B-L05.
     *
     * <p>使用SnowflakeIdGenerator生成全局唯一ID，性能优于UUID。</p>
     */
    private String generatePatternId() {
        return PATTERN_ID_PREFIX + snowflakeIdGenerator.nextId();
    }

    /**
     * 生成SVG纸样 - 修复 B-M21.
     *
     * <p>使用StringBuilder替代复杂的String.format，代码更清晰易维护。</p>
     */
    private String generateSvgPattern(PatternGenerateRequest request) {
        double width = request.getWidth() != null ? request.getWidth().doubleValue() : DEFAULT_WIDTH_CM;
        double height = request.getHeight() != null ? request.getHeight().doubleValue() : DEFAULT_HEIGHT_CM;
        String garmentType = request.getGarmentType();
        double midX = width * MIDLINE_RATIO;
        double midY = height * MIDLINE_RATIO;
        double footerY = height - 10;

        // B-M21修复: 使用StringBuilder替代String.format
        StringBuilder svg = new StringBuilder(512);
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")
           .append("viewBox=\"0 0 ").append((int) width).append(' ').append((int) height)
           .append("\" width=\"").append((int) width).append("\" height=\"").append((int) height).append("\">\n");
        svg.append("  <rect x=\"0\" y=\"0\" width=\"").append((int) width)
           .append("\" height=\"").append((int) height)
           .append("\" fill=\"#fff\" stroke=\"#333\" stroke-width=\"2\"/>\n");
        svg.append("  <!-- ").append(garmentType).append(" Pattern -->\n");
        svg.append("  <line x1=\"0\" y1=\"").append((int) midY)
           .append("\" x2=\"").append((int) width).append("\" y2=\"").append((int) midY)
           .append("\" stroke=\"#666\" stroke-width=\"1\" stroke-dasharray=\"5,5\"/>\n");
        svg.append("  <text x=\"").append((int) midX).append("\" y=\"").append((int) TITLE_Y_POSITION)
           .append("\" text-anchor=\"middle\" font-size=\"14\" fill=\"#333\">")
           .append(garmentType).append("</text>\n");
        svg.append("  <text x=\"").append((int) midX).append("\" y=\"").append((int) footerY)
           .append("\" text-anchor=\"middle\" font-size=\"10\" fill=\"#999\">")
           .append("Tailor IS AI Generated</text>\n");
        svg.append("</svg>");
        return svg.toString();
    }

    private Map<String, Object> extractDimensions(PatternGenerateRequest request) {
        Map<String, Object> dimensions = new LinkedHashMap<>();
        if (request.getWidth() != null) dimensions.put("width_cm", request.getWidth());
        if (request.getHeight() != null) dimensions.put("height_cm", request.getHeight());
        if (request.getShoulderWidth() != null) dimensions.put("shoulder_width_cm", request.getShoulderWidth());
        if (request.getSleeveLength() != null) dimensions.put("sleeve_length_cm", request.getSleeveLength());
        return dimensions;
    }

    private Map<String, String> calculateRecommendedSizes(BodySizeAnalysisRequest request) {
        Map<String, String> sizes = new LinkedHashMap<>();

        if (request.getChestCircumference() != null) {
            double chest = request.getChestCircumference().doubleValue();
            if (chest < 88) sizes.put("top", "S");
            else if (chest < 96) sizes.put("top", "M");
            else if (chest < 104) sizes.put("top", "L");
            else sizes.put("top", "XL");
        }

        if (request.getWaistCircumference() != null) {
            double waist = request.getWaistCircumference().doubleValue();
            if (waist < 74) sizes.put("bottom", "S");
            else if (waist < 82) sizes.put("bottom", "M");
            else if (waist < 90) sizes.put("bottom", "L");
            else sizes.put("bottom", "XL");
        }

        return sizes;
    }
}