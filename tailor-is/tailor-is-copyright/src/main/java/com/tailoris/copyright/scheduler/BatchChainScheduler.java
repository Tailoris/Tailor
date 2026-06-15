package com.tailoris.copyright.scheduler;

import com.tailoris.copyright.blockchain.BlockchainClient;
import com.tailoris.copyright.blockchain.BlockchainClientRouter;
import com.tailoris.copyright.entity.CopyrightRecord;
import com.tailoris.copyright.mapper.CopyrightRecordMapper;
import com.tailoris.copyright.service.HashGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 批量上链调度器
 *
 * <p>监控 Redis 中待上链队列的记录数，当达到阈值时触发批量上链操作。
 * 同时作为定时任务每 30 分钟执行一次作为兜底保障。</p>
 *
 * <p>上链流程：
 * <ol>
 *   <li>从 Redis pending 队列中批量弹出 hash</li>
 *   <li>根据 hash 从数据库查询对应的版权记录</li>
 *   <li>将多条记录的 hash 聚合为 Merkle Root 上链</li>
 *   <li>上链成功后更新各记录的状态和交易哈希</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchChainScheduler {

    private final HashGenerationService hashGenerationService;
    private final CopyrightRecordMapper copyrightRecordMapper;
    private final BlockchainClientRouter blockchainRouter;

    @Value("${copyright.batch-chain.threshold:100}")
    private int batchThreshold;

    @Value("${copyright.batch-chain.max-batch-size:200}")
    private int maxBatchSize;

    @Value("${copyright.batch-chain.enabled:true}")
    private boolean batchChainEnabled;

    /**
     * 定时批量上链任务
     * 每 30 分钟执行一次（兜底机制）
     */
    @Scheduled(fixedDelayString = "${copyright.batch-chain.schedule-interval:1800000}", initialDelayString = "${copyright.batch-chain.schedule-interval:1800000}")
    public void scheduledBatchChain() {
        if (!batchChainEnabled) {
            log.debug("批量上链功能未启用");
            return;
        }
        long pendingCount = hashGenerationService.getPendingCount();
        if (pendingCount <= 0) {
            log.debug("待上链队列为空，跳过定时批量上链");
            return;
        }
        log.info("定时批量上链触发: pendingCount={}", pendingCount);
        executeBatchChain();
    }

    /**
     * 手动触发批量上链（当 pending 数量达到阈值时调用）
     */
    public void triggerBatchChain() {
        if (!batchChainEnabled) {
            log.debug("批量上链功能未启用");
            return;
        }
        long pendingCount = hashGenerationService.getPendingCount();
        if (pendingCount < batchThreshold) {
            log.debug("待上链数量未达到阈值: {}/{}", pendingCount, batchThreshold);
            return;
        }
        log.info("阈值触发批量上链: pendingCount={}, threshold={}", pendingCount, batchThreshold);
        executeBatchChain();
    }

    /**
     * 执行批量上链核心逻辑
     */
    private void executeBatchChain() {
        long totalPending = hashGenerationService.getPendingCount();
        int batchCount = 0;

        while (hashGenerationService.getPendingCount() > 0 && batchCount < 10) {
            // 1. 弹出一批待上链的 hash
            int batchSize = (int) Math.min(hashGenerationService.getPendingCount(), maxBatchSize);
            Set<String> hashes = hashGenerationService.popPendingHashes(batchSize);
            if (hashes.isEmpty()) {
                break;
            }

            log.info("开始批量上链: batchSize={}", hashes.size());

            // 2. 查询对应的版权记录
            List<CopyrightRecord> records = queryRecordsByHashes(new ArrayList<>(hashes));
            if (records.isEmpty()) {
                log.warn("批量上链：未找到对应的版权记录, hashes={}", hashes);
                // 清理无效 hash
                for (String hash : hashes) {
                    hashGenerationService.removeFromPendingQueue(hash);
                }
                continue;
            }

            // 3. 构建批量证据并上链
            try {
                batchUploadToBlockchain(records, hashes);
                batchCount++;
            } catch (Exception e) {
                log.error("批量上链失败，将记录回退到队列", e);
                // 回退 hash 到队列
                for (String hash : hashes) {
                    Long copyrightId = hashGenerationService.getCopyrightIdByHash(hash);
                    if (copyrightId != null) {
                        hashGenerationService.addToPendingQueue(hash, copyrightId);
                    }
                }
                break;
            }
        }

        log.info("批量上链完成: 本轮处理{}批, 剩余pending={}", batchCount, hashGenerationService.getPendingCount());
    }

    /**
     * 根据 hash 列表查询版权记录
     */
    private List<CopyrightRecord> queryRecordsByHashes(List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return new ArrayList<>();
        }
        List<CopyrightRecord> records = new ArrayList<>();
        for (String hash : hashes) {
            Long copyrightId = hashGenerationService.getCopyrightIdByHash(hash);
            if (copyrightId != null) {
                CopyrightRecord record = copyrightRecordMapper.selectById(copyrightId);
                if (record != null) {
                    records.add(record);
                } else {
                    // 记录不存在，清理无效 hash
                    hashGenerationService.removeFromPendingQueue(hash);
                }
            }
        }
        return records;
    }

    /**
     * 批量上链到区块链
     *
     * @param records 版权记录列表
     * @param hashes  对应的文件哈希集合
     */
    private void batchUploadToBlockchain(List<CopyrightRecord> records, Set<String> hashes) {
        BlockchainClient client = blockchainRouter.defaultClient();

        // 构建批量上链的元数据：包含所有 hash 的 Merkle Root 或逗号分隔列表
        String batchMetadata = buildBatchMetadata(records, hashes);

        // 使用第一条记录作为主记录提交批量证据
        CopyrightRecord primaryRecord = records.get(0);
        BlockchainClient.EvidenceRequest er = new BlockchainClient.EvidenceRequest();
        er.setBizId("BATCH-" + System.currentTimeMillis());
        er.setBizType("COPYRIGHT_BATCH");
        er.setFileHash(computeBatchHash(hashes));
        er.setAuthorId(String.valueOf(primaryRecord.getUserId()));
        er.setAuthorName(primaryRecord.getAuthorRealName() != null ? primaryRecord.getAuthorRealName() : "");
        er.setCreationTime(LocalDateTime.now());
        er.setMetadata(batchMetadata);

        BlockchainClient.BlockchainSubmitResult result = client.submitEvidence(er);

        if (result.isSuccess()) {
            // 更新所有记录的状态
            LocalDateTime txTime = LocalDateTime.now();
            String txHash = result.getTxHash();
            String platform = client.platformCode();
            Long blockHeight = result.getBlockHeight() != null ? result.getBlockHeight().longValue() : null;
            String node = result.getNode();

            for (CopyrightRecord record : records) {
                record.setBlockchainPlatform(platform);
                record.setBlockchainTxHash(txHash);
                record.setBlockchainTxTime(txTime);
                record.setBlockchainBlockHeight(blockHeight);
                record.setBlockchainNode(node);
                record.setStatus(1); // 存证完成
                record.setRegisteredAt(txTime);
                copyrightRecordMapper.updateById(record);

                // 从 pending 队列中移除
                hashGenerationService.removeFromPendingQueue(record.getFileHash());

                log.info("批量上链记录成功: id={}, txHash={}", record.getId(), txHash);
            }
        } else {
            log.error("批量上链失败: {}", result.getErrorMessage());
            // 将失败的记录回退到 pending 队列
            for (CopyrightRecord record : records) {
                record.setStatus(3);
                record.setFailReason("批量上链失败: " + result.getErrorMessage());
                copyrightRecordMapper.updateById(record);
            }
        }
    }

    /**
     * 构建批量上链的元数据 JSON
     */
    private String buildBatchMetadata(List<CopyrightRecord> records, Set<String> hashes) {
        StringBuilder sb = new StringBuilder("{\"batchSize\":").append(records.size())
                .append(",\"hashes\":[");
        boolean first = true;
        for (CopyrightRecord record : records) {
            if (!first) sb.append(",");
            sb.append("{\"id\":\"").append(record.getId())
                    .append("\",\"hash\":\"").append(record.getFileHash())
                    .append("\",\"userId\":\"").append(record.getUserId()).append("\"}");
            first = false;
        }
        sb.append("],\"timestamp\":\"").append(LocalDateTime.now()).append("\"}");
        return sb.toString();
    }

    /**
     * 计算批量哈希（所有 hash 的 SHA-256）
     */
    private String computeBatchHash(Set<String> hashes) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            // 排序后拼接确保确定性
            List<String> sortedHashes = new ArrayList<>(hashes);
            java.util.Collections.sort(sortedHashes);
            for (String hash : sortedHashes) {
                digest.update(hash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算批量哈希失败", e);
            return "batch-" + System.currentTimeMillis();
        }
    }
}
