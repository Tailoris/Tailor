package com.tailoris.copyright.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 区块链连接配置.
 *
 * <p>集中管理 Hyperledger Fabric 和以太坊兼容链的连接参数。
 * 支持从配置中心（Nacos）动态获取，支持多链切换。</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "blockchain")
public class BlockchainConfig {

    /** 区块链平台类型: fabric | ethereum */
    private String platform = "fabric";

    /** 是否启用区块链存证 */
    private boolean enabled = true;

    // ---- Hyperledger Fabric 配置 ----

    /** Fabric 连接配置文件路径 */
    private String fabricConnectionProfile = "classpath:fabric/connection-profile.json";

    /** Fabric 钱包路径 */
    private String fabricWalletPath = "classpath:fabric/wallet";

    /** Fabric 通道名称 */
    private String fabricChannelName = "copyrightchannel";

    /** Fabric 链码名称 */
    private String fabricChaincodeName = "copyrightcc";

    /** Fabric 组织 MSP ID */
    private String fabricOrgMspId = "TailorMSP";

    /** Fabric 用户 ID */
    private String fabricUserId = "admin";

    // ---- 以太坊兼容链配置 ----

    /** 以太坊 RPC URL */
    private String ethRpcUrl = "https://rpc.example-chain.com";

    /** 以太坊智能合约地址 */
    private String ethContractAddress = "0x0000000000000000000000000000000000000000";

    /** 以太坊账户私钥（Hex格式，不含0x前缀） */
    private String ethPrivateKey = "";

    /** 以太坊 Chain ID */
    private long ethChainId = 1;

    /** Gas Limit */
    private long ethGasLimit = 300000;

    /** Gas Price (Gwei) */
    private long ethGasPrice = 20;

    // ---- 存证策略 ----

    /** 是否启用批量存证 */
    private boolean batchEnabled = true;

    /** 批量存证最大条目数 */
    private int batchMaxSize = 100;

    /** 批量存证间隔（秒） */
    private int batchIntervalSeconds = 60;

    /** 存证重试次数 */
    private int retryMaxAttempts = 3;

    /** 存证重试间隔（毫秒） */
    private long retryBackoffMs = 1000;
}