package com.tailoris.copyright.blockchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * 区块链通用接口
 * 任务编号: CR-001
 *
 * <p>通过统一接口屏蔽不同区块链平台差异，支持蚂蚁链/至信链/BSN/以太坊等。</p>
 */
public interface BlockchainClient {

    /**
     * 上链存证
     *
     * @param request 上链请求
     * @return 上链结果
     */
    BlockchainSubmitResult submitEvidence(EvidenceRequest request);

    /**
     * 查询存证
     *
     * @param txHash 交易哈希
     * @return 存证详情
     */
    EvidenceInfo queryEvidence(String txHash);

    /**
     * 生成存证证书
     */
    byte[] generateCertificate(CertificateRequest request);

    /**
     * 验证存证
     */
    boolean verifyEvidence(String txHash, String fileHash);

    /**
     * 获取平台编码
     */
    String platformCode();

    /**
     * 健康检查
     */
    boolean healthy();

    /** 上链请求 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class EvidenceRequest {
        /** 业务记录ID（本地数据库） */
        private String bizId;
        /** 业务类型 */
        private String bizType;
        /** 文件SHA-256 */
        private String fileHash;
        /** 作者ID */
        private String authorId;
        /** 作者姓名 */
        private String authorName;
        /** 创作时间戳 */
        private LocalDateTime creationTime;
        /** 证据元数据（JSON） */
        private String metadata;
        /** 扩展字段 */
        private java.util.Map<String, String> extra;
    }

    /** 上链结果 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class BlockchainSubmitResult {
        /** 是否成功 */
        private boolean success;
        /** 交易哈希 */
        private String txHash;
        /** 区块高度 */
        private BigInteger blockHeight;
        /** 链上节点 */
        private String node;
        /** 错误码 */
        private String errorCode;
        /** 错误消息 */
        private String errorMessage;
        /** 平台 */
        private String platform;
    }

    /** 存证查询结果 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class EvidenceInfo {
        private String txHash;
        private BigInteger blockHeight;
        private LocalDateTime blockTime;
        private String authorId;
        private String authorName;
        private String fileHash;
        private String metadata;
        private boolean exists;
    }

    /** 证书生成请求 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class CertificateRequest {
        private String certNo;
        private String workName;
        private String authorName;
        private String authorId;
        private String fileHash;
        private String txHash;
        private LocalDateTime registeredAt;
        private String qrContent;
        private String platformName;
    }
}
