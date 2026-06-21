package com.tailoris.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tailoris.ai.model")
public class AiModelConfig {

    private String provider = "cloud";

    private String endpointUrl = "https://api.openai.com/v1";

    private String apiKey = "";

    private String modelName = "gpt-4-vision-preview";

    private long timeoutMs = 60000;

    private long connectTimeoutMs = 10000;

    private int maxRetries = 3;

    private long retryDelayMs = 2000;

    private double retryBackoffMultiplier = 2.0;

    private String localModelUrl = "http://localhost:8000/api/v1/inference";

    private String localModelPath = "/opt/tailoris/models/pattern-model.onnx";

    private int localModelPort = 8000;

    private boolean localModelEnabled = true;

    private boolean fallbackToCloud = true;

    private int healthCheckIntervalSeconds = 30;

    private volatile boolean modelAvailable = true;

    private volatile long lastHealthCheckTime = 0;

    public void markUnavailable() {
        this.modelAvailable = false;
    }

    public void markAvailable() {
        this.modelAvailable = true;
    }

    public boolean isModelAvailable() {
        if (!modelAvailable) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime > healthCheckIntervalSeconds * 1000L) {
            lastHealthCheckTime = now;
            return true;
        }
        return modelAvailable;
    }
}