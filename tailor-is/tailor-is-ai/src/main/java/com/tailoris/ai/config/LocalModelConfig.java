package com.tailoris.ai.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地轻量模型配置。
 *
 * <p>用于常规体型的快速纸样生成，部署在应用服务器本地或边缘节点。
 * 当本地模型不可用时，自动降级到云端模型。</p>
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>model-path: 本地模型文件路径（ONNX/TensorRT格式）</li>
 *   <li>gpu-enabled: 是否启用GPU加速</li>
 *   <li>gpu-device-id: GPU设备ID（多卡环境）</li>
 *   <li>batch-size: 本地推理批量大小</li>
 *   <li>max-threads: 最大推理线程数</li>
 *   <li>timeout-ms: 本地推理超时时间（毫秒）</li>
 *   <li>enabled: 是否启用本地模型</li>
 *   <li>fallback-to-cloud: 本地模型不可用时是否回退到云端</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "tailoris.ai.local-model")
@ConditionalOnProperty(prefix = "tailoris.ai.local-model", name = "enabled", havingValue = "true", matchIfMissing = false)
public class LocalModelConfig {

    /** 本地模型文件路径 */
    private String modelPath = "/opt/tailoris/models/pattern-lightweight.onnx";

    /** 是否启用GPU加速 */
    private boolean gpuEnabled = true;

    /** GPU设备ID */
    private int gpuDeviceId = 0;

    /** 本地推理批量大小 */
    private int batchSize = 8;

    /** 最大推理线程数 */
    private int maxThreads = 4;

    /** 本地推理超时时间（毫秒） */
    private long timeoutMs = 3000;

    /** 模型内存上限（MB） */
    private int maxMemoryMb = 2048;

    /** 本地模型不可用时是否回退到云端 */
    private boolean fallbackToCloud = true;

    /** 健康检查间隔（秒） */
    private int healthCheckIntervalSeconds = 60;

    /**
     * 判断本地模型是否可用（由健康检查器维护的状态）。
     */
    private volatile boolean available = true;

    /**
     * 标记本地模型不可用。
     */
    public void markUnavailable() {
        this.available = false;
    }

    /**
     * 标记本地模型恢复。
     */
    public void markAvailable() {
        this.available = true;
    }
}
