package com.tailoris.ai.service;

import com.tailoris.ai.config.CloudModelConfig;
import com.tailoris.ai.config.LocalModelConfig;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.dto.PatternGenerateResponse;
import com.tailoris.ai.entity.BodySizeData;
import com.tailoris.ai.enums.ModelRoute;
import com.tailoris.ai.mapper.BodySizeDataMapper;
import com.tailoris.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分层模型调用策略服务。
 *
 * <p>核心策略：根据体型复杂度分数，智能路由到本地轻量模型或云端分布式模型。</p>
 *
 * <h3>路由规则：</h3>
 * <ul>
 *   <li><b>常规体型</b>（复杂度分数 &lt; 阈值）→ 本地轻量模型（低延迟，~50ms）</li>
 *   <li><b>特殊体型</b>（复杂度分数 ≥ 阈值）→ 云端分布式计算（高精度，~500ms）</li>
 *   <li><b>热门款式</b>（在热门款式列表中）→ 云端分布式计算（利用分布式算力）</li>
 *   <li><b>本地降级</b>：本地模型不可用时，自动回退到云端</li>
 * </ul>
 *
 * <h3>复杂度评分维度：</h3>
 * <ul>
 *   <li>胸围/腰围/臀围的差值比例</li>
 *   <li>肩宽与身高的比例</li>
 *   <li>体型标记（body_type）</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatternGenerationStrategy {

    private final LocalModelConfig localModelConfig;
    private final CloudModelConfig cloudModelConfig;
    private final BodySizeDataMapper bodySizeDataMapper;

    /** 复杂度阈值：超过此值路由到云端 */
    private static final double COMPLEXITY_THRESHOLD = 0.35;

    /** 热门款式列表（使用云端分布式计算） */
    private static final String[] POPULAR_GARMENT_TYPES = {"DRESS", "JACKET", "SUIT", "GOWN"};

    /** 特殊体型标记 */
    private static final String[] SPECIAL_BODY_TYPES = {"plus_size", "petite", "tall", "athletic", "irregular"};

    /** 请求统计：本地调用次数 */
    private final ConcurrentHashMap<String, Long> localCallCount = new ConcurrentHashMap<>();

    /** 请求统计：云端调用次数 */
    private final ConcurrentHashMap<String, Long> cloudCallCount = new ConcurrentHashMap<>();

    /**
     * 生成纸样 - 根据体型复杂度自动选择模型。
     *
     * @param request 纸样生成请求
     * @return 纸样生成结果
     */
    public PatternGenerateResponse generatePattern(PatternGenerateRequest request) {
        BodySizeData bodySize = bodySizeDataMapper.selectById(request.getBodySizeId());
        if (bodySize == null) {
            throw new BusinessException("体型数据不存在, ID: " + request.getBodySizeId());
        }

        ModelRoute route = determineRoute(bodySize, request);
        String garmentType = request.getGarmentType() != null ? request.getGarmentType() : "UNKNOWN";

        log.info("[PatternGenerationStrategy] 路由决策: route={}, bodyType={}, garmentType={}, complexity={}",
                route, bodySize.getBodyType(), garmentType,
                calculateComplexityScore(bodySize));

        PatternGenerateResponse response;
        long startTime = System.currentTimeMillis();

        try {
            switch (route) {
                case LOCAL:
                    response = callLocalModel(bodySize, request);
                    localCallCount.merge(garmentType, 1L, Long::sum);
                    break;
                case CLOUD:
                    response = callCloudModel(bodySize, request);
                    cloudCallCount.merge(garmentType, 1L, Long::sum);
                    break;
                default:
                    throw new IllegalStateException("未知的模型路由: " + route);
            }
        } catch (Exception e) {
            log.error("[PatternGenerationStrategy] {} 模型调用失败: {}", route, e.getMessage(), e);

            // 本地模型失败时，尝试降级到云端
            if (route == ModelRoute.LOCAL && localModelConfig.isFallbackToCloud()) {
                log.warn("[PatternGenerationStrategy] 本地模型降级到云端");
                response = callCloudModel(bodySize, request);
            } else {
                throw new BusinessException("纸样生成失败: " + e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[PatternGenerationStrategy] 生成完成: patternId={}, route={}, duration={}ms",
                response.getPatternId(), route, duration);

        return response;
    }

    /**
     * 异步批量生成纸样。
     *
     * @param requests 批量请求
     * @return 异步结果
     */
    public CompletableFuture<PatternGenerateResponse[]> generateBatchAsync(PatternGenerateRequest[] requests) {
        return CompletableFuture.supplyAsync(() -> {
            PatternGenerateResponse[] responses = new PatternGenerateResponse[requests.length];
            for (int i = 0; i < requests.length; i++) {
                responses[i] = generatePattern(requests[i]);
            }
            return responses;
        });
    }

    /**
     * 确定模型路由。
     *
     * @param bodySize 体型数据
     * @param request  生成请求
     * @return 模型路由
     */
    public ModelRoute determineRoute(BodySizeData bodySize, PatternGenerateRequest request) {
        // 优先检查：热门款式 → 云端
        if (isPopularGarmentType(request.getGarmentType())) {
            return ModelRoute.CLOUD;
        }

        // 计算复杂度分数
        double complexity = calculateComplexityScore(bodySize);

        // 特殊体型 → 云端
        if (isSpecialBodyType(bodySize.getBodyType()) || complexity >= COMPLEXITY_THRESHOLD) {
            return ModelRoute.CLOUD;
        }

        // 本地模型不可用 → 云端
        if (!localModelConfig.isAvailable() && localModelConfig.isFallbackToCloud()) {
            return ModelRoute.CLOUD;
        }

        // 默认 → 本地
        return ModelRoute.LOCAL;
    }

    /**
     * 计算体型复杂度分数 [0.0, 1.0]。
     *
     * <p>评分维度：</p>
     * <ul>
     *   <li>胸围-腰围差值比例（反映体型曲线度）</li>
     *   <li>腰围-臀围差值比例</li>
     *   <li>肩宽/身高比例（反映体型匀称度）</li>
     * </ul>
     *
     * @param bodySize 体型数据
     * @return 复杂度分数
     */
    public double calculateComplexityScore(BodySizeData bodySize) {
        double score = 0.0;
        int dimensions = 0;

        // 维度1: 胸围-腰围差值比例
        if (bodySize.getChestCircumference() != null && bodySize.getWaistCircumference() != null) {
            double chest = bodySize.getChestCircumference().doubleValue();
            double waist = bodySize.getWaistCircumference().doubleValue();
            if (chest > 0 && waist > 0) {
                double ratio = Math.abs(chest - waist) / chest;
                score += ratio;
                dimensions++;
            }
        }

        // 维度2: 腰围-臀围差值比例
        if (bodySize.getWaistCircumference() != null && bodySize.getHipCircumference() != null) {
            double waist = bodySize.getWaistCircumference().doubleValue();
            double hip = bodySize.getHipCircumference().doubleValue();
            if (waist > 0 && hip > 0) {
                double ratio = Math.abs(hip - waist) / hip;
                score += ratio;
                dimensions++;
            }
        }

        // 维度3: 肩宽/身高比例
        if (bodySize.getShoulderWidth() != null && bodySize.getHeight() != null) {
            double shoulder = bodySize.getShoulderWidth().doubleValue();
            double height = bodySize.getHeight().doubleValue();
            if (shoulder > 0 && height > 0) {
                double ratio = shoulder / height;
                // 正常比例约 0.20-0.25，偏离越远复杂度越高
                double deviation = Math.abs(ratio - 0.225) / 0.225;
                score += deviation;
                dimensions++;
            }
        }

        return dimensions > 0 ? Math.min(score / dimensions, 1.0) : 0.0;
    }

    /**
     * 调用本地轻量模型。
     *
     * @param bodySize 体型数据
     * @param request  生成请求
     * @return 生成结果
     */
    private PatternGenerateResponse callLocalModel(BodySizeData bodySize, PatternGenerateRequest request) {
        log.debug("[PatternGenerationStrategy] 调用本地模型: bodyType={}", bodySize.getBodyType());

        // TODO: 接入真实本地 ONNX/TensorRT 推理
        // 当前使用规则引擎模拟本地模型推理
        String patternId = "LOC-" + System.currentTimeMillis() + "-" + bodySize.getId();

        return PatternGenerateResponse.builder()
                .patternId(patternId)
                .name(request.getPatternName())
                .garmentType(request.getGarmentType())
                .svgContent(generateLocalSvgPlaceholder(bodySize, request))
                .previewUrl("/api/v1/ai/patterns/" + patternId + "/preview")
                .version(1)
                .build();
    }

    /**
     * 调用云端分布式模型。
     *
     * @param bodySize 体型数据
     * @param request  生成请求
     * @return 生成结果
     */
    private PatternGenerateResponse callCloudModel(BodySizeData bodySize, PatternGenerateRequest request) {
        log.debug("[PatternGenerationStrategy] 调用云端模型: bodyType={}, garmentType={}",
                bodySize.getBodyType(), request.getGarmentType());

        // 检查熔断器状态
        if (!cloudModelConfig.isCircuitAvailable()) {
            throw new BusinessException("云端模型服务熔断中，请稍后重试");
        }

        // TODO: 接入真实云端 API（HTTP/RPC）
        // 当前使用规则引擎模拟云端模型推理
        String patternId = "CLD-" + System.currentTimeMillis() + "-" + bodySize.getId();

        return PatternGenerateResponse.builder()
                .patternId(patternId)
                .name(request.getPatternName())
                .garmentType(request.getGarmentType())
                .svgContent(generateCloudSvgPlaceholder(bodySize, request))
                .previewUrl("/api/v1/ai/patterns/" + patternId + "/preview")
                .version(1)
                .build();
    }

    /**
     * 判断是否为热门款式。
     */
    private boolean isPopularGarmentType(String garmentType) {
        if (garmentType == null) {
            return false;
        }
        for (String popular : POPULAR_GARMENT_TYPES) {
            if (popular.equalsIgnoreCase(garmentType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为特殊体型。
     */
    private boolean isSpecialBodyType(String bodyType) {
        if (bodyType == null) {
            return false;
        }
        for (String special : SPECIAL_BODY_TYPES) {
            if (special.equalsIgnoreCase(bodyType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成本地模型 SVG 占位符（模拟推理结果）。
     */
    private String generateLocalSvgPlaceholder(BodySizeData bodySize, PatternGenerateRequest request) {
        StringBuilder svg = new StringBuilder(512);
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 300\">\n");
        svg.append("  <text x=\"100\" y=\"20\" text-anchor=\"middle\" font-size=\"12\" fill=\"#333\">");
        svg.append("[Local] ").append(request.getGarmentType()).append("</text>\n");
        svg.append("  <rect x=\"10\" y=\"30\" width=\"180\" height=\"260\" fill=\"none\" stroke=\"#4CAF50\" stroke-width=\"2\"/>\n");
        svg.append("  <text x=\"100\" y=\"290\" text-anchor=\"middle\" font-size=\"8\" fill=\"#888\">");
        svg.append("Local Model | Complexity: ").append(String.format("%.2f", calculateComplexityScore(bodySize)));
        svg.append("</text>\n");
        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * 生成云端模型 SVG 占位符（模拟推理结果）。
     */
    private String generateCloudSvgPlaceholder(BodySizeData bodySize, PatternGenerateRequest request) {
        StringBuilder svg = new StringBuilder(512);
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 300\">\n");
        svg.append("  <text x=\"100\" y=\"20\" text-anchor=\"middle\" font-size=\"12\" fill=\"#333\">");
        svg.append("[Cloud] ").append(request.getGarmentType()).append("</text>\n");
        svg.append("  <rect x=\"10\" y=\"30\" width=\"180\" height=\"260\" fill=\"none\" stroke=\"#2196F3\" stroke-width=\"2\"/>\n");
        svg.append("  <text x=\"100\" y=\"290\" text-anchor=\"middle\" font-size=\"8\" fill=\"#888\">");
        svg.append("Cloud Model | Complexity: ").append(String.format("%.2f", calculateComplexityScore(bodySize)));
        svg.append("</text>\n");
        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * 获取本地调用统计。
     */
    public ConcurrentHashMap<String, Long> getLocalCallCount() {
        return localCallCount;
    }

    /**
     * 获取云端调用统计。
     */
    public ConcurrentHashMap<String, Long> getCloudCallCount() {
        return cloudCallCount;
    }
}
