package com.tailoris.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.tailoris.ai.config.AiModelConfig;
import com.tailoris.ai.dto.PatternGenerateResponse;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.mapper.PatternRecordMapper;
import com.tailoris.ai.model.PatternRequest;
import com.tailoris.ai.service.AiModelService;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private final AiModelConfig aiModelConfig;
    private final PatternRecordMapper patternRecordMapper;
    private final SpringSnowflakeIdGenerator snowflakeIdGenerator;

    private static final String PATTERN_ID_PREFIX = "AI-";

    @Override
    public PatternGenerateResponse generatePattern(PatternRequest request) {
        log.info("AiModelService.generatePattern: garmentType={}, style={}, provider={}",
                request.getGarmentType(), request.getStylePreference(), aiModelConfig.getProvider());

        String patternId = generatePatternId();
        String svgContent;

        if ("local".equalsIgnoreCase(aiModelConfig.getProvider())) {
            svgContent = callLocalModel(request, patternId);
        } else {
            svgContent = callCloudModel(request, patternId);
        }

        Map<String, Object> dimensions = extractDimensions(request);

        return PatternGenerateResponse.builder()
                .patternId(patternId)
                .name(request.getGarmentType() + "-" + patternId)
                .garmentType(request.getGarmentType())
                .svgContent(svgContent)
                .previewUrl("/api/v1/ai/pattern/preview/" + patternId)
                .dimensions(dimensions)
                .version(1)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public String checkStructure(byte[] pattern) {
        log.info("AiModelService.checkStructure: patternSize={} bytes", pattern != null ? pattern.length : 0);
        try {
            if ("local".equalsIgnoreCase(aiModelConfig.getProvider())) {
                return callLocalModelForCheck(pattern);
            }
            return callCloudModelForCheck(pattern);
        } catch (Exception e) {
            log.error("Structure check failed", e);
            return "{\"structure\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    @Override
    public PatternGenerateResponse iteratePattern(Long patternId, String feedback) {
        log.info("AiModelService.iteratePattern: patternId={}, feedback={}", patternId, feedback);
        PatternRecord record = patternRecordMapper.selectById(patternId);
        if (record == null) {
            throw new RuntimeException("Pattern record not found: " + patternId);
        }

        PatternRequest request = PatternRequest.builder()
                .garmentType(record.getPatternType() != null ? String.valueOf(record.getPatternType()) : "SHIRT")
                .stylePreference(feedback)
                .build();

        PatternGenerateResponse response = generatePattern(request);
        return PatternGenerateResponse.builder()
                .patternId(response.getPatternId())
                .name("迭代-" + record.getPatternName())
                .garmentType(response.getGarmentType())
                .svgContent(response.getSvgContent())
                .previewUrl(response.getPreviewUrl())
                .dimensions(response.getDimensions())
                .version(record.getVersion() != null ? record.getVersion() + 1 : 2)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public String exportPattern(Long patternId, String format) {
        log.info("AiModelService.exportPattern: patternId={}, format={}", patternId, format);
        PatternRecord record = patternRecordMapper.selectById(patternId);
        if (record == null) {
            throw new RuntimeException("Pattern record not found: " + patternId);
        }
        return "https://pattern-export.tailoris.com/" + patternId + "." + (format != null ? format.toLowerCase() : "svg");
    }

    private String callLocalModel(PatternRequest request, String patternId) {
        try {
            if (!aiModelConfig.isModelAvailable()) {
                log.warn("Local model unavailable, fallback to cloud");
                if (aiModelConfig.isFallbackToCloud()) {
                    return callCloudModel(request, patternId);
                }
                throw new RuntimeException("Local model is not available");
            }

            String requestBody = buildLocalModelRequest(request, patternId);

            String response = sendHttpRequest(aiModelConfig.getLocalModelUrl(), requestBody);

            return parseLocalModelResponse(response, request);

        } catch (Exception e) {
            log.error("Local model call failed, fallback to cloud", e);
            aiModelConfig.markUnavailable();
            if (aiModelConfig.isFallbackToCloud()) {
                return callCloudModel(request, patternId);
            }
            return generateFallbackSvg(request);
        }
    }

    private String callCloudModel(PatternRequest request, String patternId) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < aiModelConfig.getMaxRetries()) {
            attempts++;
            try {
                String requestBody = buildCloudModelRequest(request, patternId);

                String response = sendHttpRequest(aiModelConfig.getEndpointUrl() + "/chat/completions", requestBody);

                return parseCloudModelResponse(response, request);

            } catch (Exception e) {
                lastException = e;
                log.warn("Cloud model call attempt {}/{} failed", attempts, aiModelConfig.getMaxRetries(), e);
                if (attempts < aiModelConfig.getMaxRetries()) {
                    try {
                        long delay = (long) (aiModelConfig.getRetryDelayMs() *
                                Math.pow(aiModelConfig.getRetryBackoffMultiplier(), attempts - 1));
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        log.error("All cloud model retry attempts failed", lastException);
        return generateFallbackSvg(request);
    }

    private String buildLocalModelRequest(PatternRequest request, String patternId) {
        JSONObject body = new JSONObject();
        body.put("pattern_id", patternId);
        body.put("garment_type", request.getGarmentType());
        body.put("measurements", request.getMeasurements());
        body.put("style_preference", request.getStylePreference());
        body.put("constraints", request.getConstraints());
        return body.toJSONString();
    }

    private String buildCloudModelRequest(PatternRequest request, String patternId) {
        JSONObject body = new JSONObject();
        body.put("model", aiModelConfig.getModelName());
        body.put("max_tokens", 4096);
        body.put("temperature", 0.7);

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", buildSystemPrompt());

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", buildUserPrompt(request, patternId));

        body.put("messages", new Object[]{systemMessage, userMessage});

        JSONObject responseFormat = new JSONObject();
        responseFormat.put("type", "json_object");
        body.put("response_format", responseFormat);

        return body.toJSONString();
    }

    private String buildSystemPrompt() {
        return "You are a professional fashion pattern designer AI. " +
               "You specialize in generating accurate clothing pattern designs based on body measurements. " +
               "You must output valid SVG pattern data with precise measurements. " +
               "The SVG must include: outer boundary, midline, shoulder lines, chest lines, waist lines, hip lines, " +
               "and measurement annotations. " +
               "All measurements must be in centimeters. " +
               "Respond only with a JSON object containing: {\"svg_content\": \"<svg>...</svg>\", " +
               "\"measurements\": {\"width\": number, \"height\": number, ...}, \"description\": \"...\"}";
    }

    private String buildUserPrompt(PatternRequest request, String patternId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a clothing pattern for a ").append(request.getGarmentType()).append(".\n\n");

        prompt.append("Body Measurements (cm):\n");
        if (request.getMeasurements() != null && !request.getMeasurements().isEmpty()) {
            for (Map.Entry<String, Double> entry : request.getMeasurements().entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("cm\n");
            }
        }

        if (request.getStylePreference() != null && !request.getStylePreference().isEmpty()) {
            prompt.append("\nStyle Preference: ").append(request.getStylePreference()).append("\n");
        }

        if (request.getConstraints() != null && !request.getConstraints().isEmpty()) {
            prompt.append("\nConstraints: ").append(request.getConstraints()).append("\n");
        }

        prompt.append("\nPattern ID: ").append(patternId).append("\n");
        prompt.append("Please generate the SVG pattern now.");

        return prompt.toString();
    }

    private String parseLocalModelResponse(String response, PatternRequest request) {
        try {
            JSONObject json = JSON.parseObject(response);
            if (json.containsKey("svg_content")) {
                return json.getString("svg_content");
            }
        } catch (Exception e) {
            log.warn("Failed to parse local model response as JSON, using raw response", e);
        }
        if (response.contains("<svg")) {
            return response;
        }
        return generateFallbackSvg(request);
    }

    private String parseCloudModelResponse(String response, PatternRequest request) {
        try {
            JSONObject json = JSON.parseObject(response);
            JSONObject choice = json.getJSONArray("choices").getJSONObject(0);
            String content = choice.getJSONObject("message").getString("content");

            JSONObject contentJson = JSON.parseObject(content);
            if (contentJson.containsKey("svg_content")) {
                return contentJson.getString("svg_content");
            }
            return content;
        } catch (Exception e) {
            log.warn("Failed to parse cloud model response as JSON", e);
            if (response.contains("<svg")) {
                return response;
            }
            return generateFallbackSvg(request);
        }
    }

    private String callLocalModelForCheck(byte[] pattern) {
        try {
            String base64Pattern = java.util.Base64.getEncoder().encodeToString(pattern);
            JSONObject body = new JSONObject();
            body.put("action", "check_structure");
            body.put("pattern_data", base64Pattern);

            return sendHttpRequest(aiModelConfig.getLocalModelUrl(), body.toJSONString());
        } catch (Exception e) {
            log.error("Local model structure check failed", e);
            return "{\"structure\":\"unknown\",\"message\":\"check failed: " + e.getMessage() + "\"}";
        }
    }

    private String callCloudModelForCheck(byte[] pattern) {
        try {
            String base64Pattern = java.util.Base64.getEncoder().encodeToString(pattern);
            JSONObject body = new JSONObject();
            body.put("model", aiModelConfig.getModelName());
            body.put("max_tokens", 2048);
            body.put("temperature", 0.3);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", "Analyze the following clothing pattern structure for validity. " +
                    "Check for: closed boundaries, proportional measurements, symmetry, structural integrity. " +
                    "Respond with JSON: {\"structure\":\"valid|warning|invalid\",\"issues\":[],\"confidence\":0.0}. " +
                    "Pattern data (base64): " + base64Pattern);
            body.put("messages", new Object[]{userMessage});

            String response = sendHttpRequest(aiModelConfig.getEndpointUrl() + "/chat/completions", body.toJSONString());

            JSONObject json = JSON.parseObject(response);
            JSONObject choice = json.getJSONArray("choices").getJSONObject(0);
            return choice.getJSONObject("message").getString("content");
        } catch (Exception e) {
            log.error("Cloud model structure check failed", e);
            return "{\"structure\":\"unknown\",\"message\":\"check failed: " + e.getMessage() + "\"}";
        }
    }

    private String sendHttpRequest(String urlStr, String requestBody) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + aiModelConfig.getApiKey());
        conn.setDoOutput(true);
        conn.setConnectTimeout((int) aiModelConfig.getConnectTimeoutMs());
        conn.setReadTimeout((int) aiModelConfig.getTimeoutMs());

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP request failed with status: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        conn.disconnect();
        return response.toString();
    }

    private String generateFallbackSvg(PatternRequest request) {
        String garmentType = request.getGarmentType() != null ? request.getGarmentType() : "SHIRT";
        Map<String, Double> measurements = request.getMeasurements();
        double width = measurements != null && measurements.containsKey("width") ? measurements.get("width") : 200;
        double height = measurements != null && measurements.containsKey("height") ? measurements.get("height") : 300;
        double midX = width / 2;
        double midY = height / 2;

        StringBuilder svg = new StringBuilder(512);
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")
           .append("viewBox=\"0 0 ").append((int) width).append(' ').append((int) height)
           .append("\" width=\"").append((int) width).append("\" height=\"").append((int) height).append("\">\n");
        svg.append("  <rect x=\"0\" y=\"0\" width=\"").append((int) width)
           .append("\" height=\"").append((int) height)
           .append("\" fill=\"#fff\" stroke=\"#333\" stroke-width=\"2\"/>\n");
        svg.append("  <line x1=\"0\" y1=\"").append((int) midY)
           .append("\" x2=\"").append((int) width).append("\" y2=\"").append((int) midY)
           .append("\" stroke=\"#999\" stroke-width=\"1\" stroke-dasharray=\"5,5\"/>\n");
        svg.append("  <line x1=\"").append((int) midX).append("\" y1=\"0\" x2=\"").append((int) midX)
           .append("\" y2=\"").append((int) height)
           .append("\" stroke=\"#999\" stroke-width=\"1\" stroke-dasharray=\"5,5\"/>\n");
        svg.append("  <text x=\"").append((int) midX).append("\" y=\"20\" text-anchor=\"middle\" font-size=\"14\" fill=\"#333\">")
           .append(garmentType).append("</text>\n");
        if (measurements != null) {
            int yPos = 40;
            for (Map.Entry<String, Double> entry : measurements.entrySet()) {
                svg.append("  <text x=\"10\" y=\"").append(yPos).append("\" font-size=\"10\" fill=\"#666\">")
                   .append(entry.getKey()).append(": ").append(entry.getValue()).append("cm</text>\n");
                yPos += 14;
            }
        }
        svg.append("  <text x=\"").append((int) midX).append("\" y=\"").append((int) height - 10)
           .append("\" text-anchor=\"middle\" font-size=\"10\" fill=\"#999\">")
           .append("Tailor IS AI Generated</text>\n");
        svg.append("</svg>");
        return svg.toString();
    }

    private Map<String, Object> extractDimensions(PatternRequest request) {
        Map<String, Object> dimensions = new LinkedHashMap<>();
        if (request.getMeasurements() != null) {
            request.getMeasurements().forEach((k, v) -> dimensions.put(k + "_cm", v));
        }
        return dimensions;
    }

    private String generatePatternId() {
        return PATTERN_ID_PREFIX + snowflakeIdGenerator.nextId();
    }
}