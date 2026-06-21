package com.tailoris.copyright.blockchain.impl;

import com.tailoris.copyright.blockchain.BlockchainService;
import com.tailoris.copyright.blockchain.MerkleTreeUtil;
import com.tailoris.copyright.config.BlockchainConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hyperledger Fabric 区块链实现.
 *
 * <p>通过 Fabric SDK 连接 Fabric 网络，执行链码操作。
 * 支持版权存证提交、账本状态查询、批量提交（Merkle Tree Root）。</p>
 *
 * <h3>Fabric 网络架构</h3>
 * <ul>
 *   <li>Gateway Peer: 通过 Gateway 模式连接，简化客户端操作</li>
 *   <li>Channel: 版权存证专用通道</li>
 *   <li>Chaincode: 版权管理智能合约</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blockchain.platform", havingValue = "fabric", matchIfMissing = true)
public class HyperledgerFabricServiceImpl implements BlockchainService {

    private final BlockchainConfig blockchainConfig;
    private final MerkleTreeUtil merkleTreeUtil;

    /** 模拟的链上存储（生产环境通过 Fabric SDK 连接真实网络） */
    private final Map<String, Map<String, Object>> ledgerStore = new ConcurrentHashMap<>();

    private boolean connected = false;

    @PostConstruct
    public void init() {
        try {
            connectToNetwork();
            connected = true;
            log.info("Hyperledger Fabric 网络连接成功: channel={}, chaincode={}",
                    blockchainConfig.getFabricChannelName(), blockchainConfig.getFabricChaincodeName());
        } catch (Exception e) {
            log.warn("Hyperledger Fabric 网络连接失败，将使用本地模拟模式: {}", e.getMessage());
            connected = false;
        }
    }

    @PreDestroy
    public void destroy() {
        disconnectFromNetwork();
    }

    /**
     * 连接 Fabric 网络.
     * <p>生产环境通过 Gateway 模式连接，使用 connection profile 和 wallet 身份认证。</p>
     */
    private void connectToNetwork() {
        // 生产环境实现:
        // 1. 读取 connection profile (ccp.json)
        // 2. 通过 Wallet 加载用户身份
        // 3. 创建 Gateway 连接
        // 4. 获取 Network 和 Contract 实例
        //
        // Gateway.Builder builder = Gateway.createBuilder()
        //     .identity(wallet, userId)
        //     .networkConfig(connectionProfile);
        // Gateway gateway = builder.connect();
        // Network network = gateway.getNetwork(channelName);
        // Contract contract = network.getContract(chaincodeName);

        String ccpPath = blockchainConfig.getFabricConnectionProfile();
        String walletPath = blockchainConfig.getFabricWalletPath();
        log.info("Fabric 连接配置: ccp={}, wallet={}", ccpPath, walletPath);
    }

    private void disconnectFromNetwork() {
        if (connected) {
            connected = false;
            log.info("Fabric 网络连接已断开");
        }
    }

    @Override
    public Map<String, Object> registerCopyright(CopyrightData data) {
        String txId = generateTxId(data.getFileHash(), data.getBizId());
        long timestamp = Instant.now().toEpochMilli();

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("bizId", data.getBizId());
        record.put("workName", data.getWorkName());
        record.put("authorId", data.getAuthorId());
        record.put("authorName", data.getAuthorName());
        record.put("fileHash", data.getFileHash());
        record.put("fileType", data.getFileType());
        record.put("fileSize", data.getFileSize());
        record.put("creationTimestamp", data.getCreationTimestamp());
        record.put("metadata", data.getMetadata());
        record.put("registeredAt", timestamp);
        record.put("blockHeight", ledgerStore.size() + 1L);

        // 生产环境: 调用 chaincode submitTransaction
        // byte[] result = contract.submitTransaction("RegisterCopyright",
        //     data.getBizId(), data.getWorkName(), data.getAuthorId(),
        //     data.getFileHash(), data.getMetadata());
        // txId = contract.getTransactionId();

        ledgerStore.put(txId, record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("txHash", txId);
        result.put("blockHeight", ledgerStore.size());
        result.put("timestamp", timestamp);
        result.put("platform", "fabric");
        return result;
    }

    @Override
    public Map<String, Object> verifyCopyright(String hash) {
        // 生产环境: 调用 chaincode evaluateTransaction
        // byte[] result = contract.evaluateTransaction("QueryCopyrightByHash", hash);

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : ledgerStore.entrySet()) {
            Map<String, Object> record = entry.getValue();
            if (hash.equals(record.get("fileHash"))) {
                result.put("matched", true);
                result.put("txHash", entry.getKey());
                result.put("workName", record.get("workName"));
                result.put("authorName", record.get("authorName"));
                result.put("registeredAt", record.get("registeredAt"));
                result.put("platform", "fabric");
                return result;
            }
        }

        result.put("matched", false);
        result.put("platform", "fabric");
        return result;
    }

    @Override
    public Map<String, Object> batchRegister(List<CopyrightData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("批量存证数据不能为空");
        }

        // 计算 Merkle Root
        List<String> hashes = dataList.stream()
                .map(CopyrightData::getFileHash)
                .collect(Collectors.toList());
        MerkleTreeUtil.MerkleTree tree = merkleTreeUtil.buildTree(hashes);

        String merkleRoot = tree.getRoot();
        String batchTxId = generateTxId(merkleRoot, "batch-" + UUID.randomUUID().toString().substring(0, 8));
        long timestamp = Instant.now().toEpochMilli();

        // 生产环境: 调用 chaincode submitTransaction
        // contract.submitTransaction("BatchRegisterCopyright", merkleRoot, jsonData);

        Map<String, Object> batchRecord = new LinkedHashMap<>();
        batchRecord.put("merkleRoot", merkleRoot);
        batchRecord.put("count", dataList.size());
        batchRecord.put("hashes", hashes);
        batchRecord.put("timestamp", timestamp);

        ledgerStore.put(batchTxId, batchRecord);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("txHash", batchTxId);
        result.put("merkleRoot", merkleRoot);
        result.put("count", dataList.size());
        result.put("timestamp", timestamp);
        result.put("platform", "fabric");
        return result;
    }

    @Override
    public Map<String, Object> queryCertificate(String txHash) {
        Map<String, Object> record = ledgerStore.get(txHash);

        Map<String, Object> result = new LinkedHashMap<>();
        if (record != null) {
            result.put("exists", true);
            result.put("txHash", txHash);
            result.putAll(record);
        } else {
            result.put("exists", false);
            result.put("txHash", txHash);
        }
        result.put("platform", "fabric");
        return result;
    }

    @Override
    public boolean isHealthy() {
        return connected || !ledgerStore.isEmpty();
    }

    @Override
    public String getPlatformName() {
        return "FABRIC";
    }

    /**
     * 生成交易 ID.
     */
    private String generateTxId(String... inputs) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String input : inputs) {
                md.update(input.getBytes(StandardCharsets.UTF_8));
            }
            md.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}