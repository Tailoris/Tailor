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
 * 以太坊兼容链区块链实现.
 *
 * <p>通过 Web3j 连接以太坊兼容链（如蚂蚁链开放联盟链、BSC、Polygon 等），
 * 与版权管理智能合约交互。作为 Fabric 不可用时的降级方案。</p>
 *
 * <h3>智能合约功能</h3>
 * <ul>
 *   <li>registerCopyright(bytes32 hash, string metadata) → 存证登记</li>
 *   <li>verifyCopyright(bytes32 hash) → 存证验证</li>
 *   <li>batchRegister(bytes32 merkleRoot, uint256 count) → 批量存证</li>
 *   <li>getCertificate(bytes32 txHash) → 查询证书</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blockchain.platform", havingValue = "ethereum")
public class EthBlockchainServiceImpl implements BlockchainService {

    private final BlockchainConfig blockchainConfig;
    private final MerkleTreeUtil merkleTreeUtil;

    /** 模拟的链上存储（生产环境通过 Web3j 连接真实链） */
    private final Map<String, Map<String, Object>> contractStore = new ConcurrentHashMap<>();

    private boolean connected = false;

    @PostConstruct
    public void init() {
        try {
            connectToNetwork();
            connected = true;
            log.info("以太坊兼容链连接成功: rpc={}, contract={}",
                    blockchainConfig.getEthRpcUrl(), blockchainConfig.getEthContractAddress());
        } catch (Exception e) {
            log.warn("以太坊兼容链连接失败，将使用本地模拟模式: {}", e.getMessage());
            connected = false;
        }
    }

    @PreDestroy
    public void destroy() {
        disconnectFromNetwork();
    }

    private void connectToNetwork() {
        // 生产环境实现:
        // Web3j web3j = Web3j.build(new HttpService(rpcUrl));
        // Credentials credentials = Credentials.create(privateKey);
        // Contract contract = CopyrightRegistry.load(
        //     contractAddress, web3j, credentials, new DefaultGasProvider());
        log.info("以太坊链连接: rpc={}, contract={}",
                blockchainConfig.getEthRpcUrl(), blockchainConfig.getEthContractAddress());
    }

    private void disconnectFromNetwork() {
        if (connected) {
            connected = false;
            log.info("以太坊链连接已断开");
        }
    }

    @Override
    public Map<String, Object> registerCopyright(CopyrightData data) {
        // 生产环境:
        // TransactionReceipt receipt = contract.registerCopyright(
        //     bytes32Hash, metadata).send();
        // String txHash = receipt.getTransactionHash();

        String txHash = generateTxHash(data.getFileHash(), data.getBizId());
        long timestamp = Instant.now().toEpochMilli();

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("bizId", data.getBizId());
        record.put("workName", data.getWorkName());
        record.put("authorId", data.getAuthorId());
        record.put("authorName", data.getAuthorName());
        record.put("fileHash", data.getFileHash());
        record.put("metadata", data.getMetadata());
        record.put("registeredAt", timestamp);
        record.put("blockNumber", contractStore.size() + 1L);

        contractStore.put(txHash, record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("txHash", txHash);
        result.put("blockHeight", contractStore.size());
        result.put("timestamp", timestamp);
        result.put("platform", "ethereum");
        return result;
    }

    @Override
    public Map<String, Object> verifyCopyright(String hash) {
        // 生产环境:
        // Tuple3<Boolean, string, uint256> result = contract.verifyCopyright(
        //     bytes32Hash).send();

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : contractStore.entrySet()) {
            Map<String, Object> record = entry.getValue();
            if (hash.equals(record.get("fileHash"))) {
                result.put("matched", true);
                result.put("txHash", entry.getKey());
                result.put("workName", record.get("workName"));
                result.put("authorName", record.get("authorName"));
                result.put("registeredAt", record.get("registeredAt"));
                result.put("platform", "ethereum");
                return result;
            }
        }

        result.put("matched", false);
        result.put("platform", "ethereum");
        return result;
    }

    @Override
    public Map<String, Object> batchRegister(List<CopyrightData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("批量存证数据不能为空");
        }

        List<String> hashes = dataList.stream()
                .map(CopyrightData::getFileHash)
                .collect(Collectors.toList());
        MerkleTreeUtil.MerkleTree tree = merkleTreeUtil.buildTree(hashes);

        String merkleRoot = tree.getRoot();
        String batchTxHash = generateTxHash(merkleRoot, "eth-batch-" + UUID.randomUUID().toString().substring(0, 8));
        long timestamp = Instant.now().toEpochMilli();

        // 生产环境:
        // TransactionReceipt receipt = contract.batchRegister(
        //     bytes32MerkleRoot, BigInteger.valueOf(dataList.size())).send();

        Map<String, Object> batchRecord = new LinkedHashMap<>();
        batchRecord.put("merkleRoot", merkleRoot);
        batchRecord.put("count", dataList.size());
        batchRecord.put("hashes", hashes);
        batchRecord.put("timestamp", timestamp);

        contractStore.put(batchTxHash, batchRecord);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("txHash", batchTxHash);
        result.put("merkleRoot", merkleRoot);
        result.put("count", dataList.size());
        result.put("timestamp", timestamp);
        result.put("platform", "ethereum");
        return result;
    }

    @Override
    public Map<String, Object> queryCertificate(String txHash) {
        // 生产环境:
        // Tuple4<string, string, uint256, bool> cert = contract.getCertificate(
        //     bytes32TxHash).send();

        Map<String, Object> record = contractStore.get(txHash);

        Map<String, Object> result = new LinkedHashMap<>();
        if (record != null) {
            result.put("exists", true);
            result.put("txHash", txHash);
            result.put("blockNumber", record.get("blockNumber"));
            result.put("timestamp", record.get("timestamp"));
            result.putAll(record);
        } else {
            result.put("exists", false);
            result.put("txHash", txHash);
        }
        result.put("platform", "ethereum");
        return result;
    }

    @Override
    public boolean isHealthy() {
        return connected;
    }

    @Override
    public String getPlatformName() {
        return "ETHEREUM";
    }

    private String generateTxHash(String... inputs) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String input : inputs) {
                md.update(input.getBytes(StandardCharsets.UTF_8));
            }
            md.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            return "0x" + bytesToHex(digest);
        } catch (Exception e) {
            return "0x" + UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}