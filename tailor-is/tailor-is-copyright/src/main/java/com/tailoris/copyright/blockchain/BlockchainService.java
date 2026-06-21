package com.tailoris.copyright.blockchain;

import java.util.List;
import java.util.Map;

/**
 * 区块链操作服务接口.
 *
 * <p>提供版权登记、验证、批量存证、证书查询等区块链核心操作。
 * 屏蔽底层区块链平台差异（Hyperledger Fabric / 以太坊），提供统一接口。</p>
 */
public interface BlockchainService {

    /**
     * 登记版权上链.
     *
     * @param data 版权数据
     * @return 交易哈希和区块信息
     */
    Map<String, Object> registerCopyright(CopyrightData data);

    /**
     * 验证版权存证.
     *
     * @param hash 文件哈希值
     * @return 验证结果，包含是否匹配、存证信息等
     */
    Map<String, Object> verifyCopyright(String hash);

    /**
     * 批量注册版权.
     *
     * @param dataList 版权数据列表
     * @return 批量存证结果，包含 Merkle Root 和交易哈希
     */
    Map<String, Object> batchRegister(List<CopyrightData> dataList);

    /**
     * 查询证书.
     *
     * @param txHash 交易哈希
     * @return 证书信息
     */
    Map<String, Object> queryCertificate(String txHash);

    /**
     * 健康检查.
     *
     * @return true 连接到区块链网络
     */
    boolean isHealthy();

    /**
     * 获取平台名称.
     */
    String getPlatformName();

    /**
     * 版权上链数据模型.
     */
    class CopyrightData {
        /** 业务 ID（本地数据库记录 ID） */
        private String bizId;
        /** 作品名称 */
        private String workName;
        /** 作者 ID */
        private String authorId;
        /** 作者姓名 */
        private String authorName;
        /** 文件 SHA-256 哈希 */
        private String fileHash;
        /** 文件类型 */
        private String fileType;
        /** 文件大小（字节） */
        private Long fileSize;
        /** 创作时间戳 */
        private Long creationTimestamp;
        /** 元数据（JSON） */
        private String metadata;
        /** 扩展字段 */
        private Map<String, String> extra;

        public CopyrightData() {}

        public void setBizId(String bizId) { this.bizId = bizId; }
        public String getBizId() { return bizId; }

        public void setWorkName(String workName) { this.workName = workName; }
        public String getWorkName() { return workName; }

        public void setAuthorId(String authorId) { this.authorId = authorId; }
        public String getAuthorId() { return authorId; }

        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getAuthorName() { return authorName; }

        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        public String getFileHash() { return fileHash; }

        public void setFileType(String fileType) { this.fileType = fileType; }
        public String getFileType() { return fileType; }

        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public Long getFileSize() { return fileSize; }

        public void setCreationTimestamp(Long creationTimestamp) { this.creationTimestamp = creationTimestamp; }
        public Long getCreationTimestamp() { return creationTimestamp; }

        public void setMetadata(String metadata) { this.metadata = metadata; }
        public String getMetadata() { return metadata; }

        public void setExtra(Map<String, String> extra) { this.extra = extra; }
        public Map<String, String> getExtra() { return extra; }
    }
}