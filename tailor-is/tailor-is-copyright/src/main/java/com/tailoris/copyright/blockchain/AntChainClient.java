package com.tailoris.copyright.blockchain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 蚂蚁链实现（占位 + 本地模拟）
 * 任务编号: CR-001
 *
 * <p>生产环境应替换为蚂蚁链官方 SDK (antchain-java-sdk)。
 * 当前实现模拟链上行为，便于单元测试与离线开发。</p>
 */
@Slf4j
@Component
public class AntChainClient implements BlockchainClient {

    @Value("${copyright.antchain.endpoint:https://antchain.openapi.example/api}")
    private String endpoint;

    @Value("${copyright.antchain.api-key:demo-key}")
    private String apiKey;

    @Override
    public BlockchainSubmitResult submitEvidence(EvidenceRequest request) {
        log.info("[ANTCHAIN] 提交存证: bizId={}, fileHash={}", request.getBizId(),
                request.getFileHash() == null ? null : request.getFileHash().substring(0, Math.min(16, request.getFileHash().length())));

        // 模拟：生成交易哈希
        String txHash = "0x" + sha256("ANT|" + request.getBizId() + "|" + request.getFileHash() + "|" + UUID.randomUUID());
        BigInteger blockHeight = BigInteger.valueOf(System.currentTimeMillis() / 1000);

        return new BlockchainSubmitResult(
                true, txHash, blockHeight, "antchain-node-001", null, null, "ANTCHAIN");
    }

    @Override
    public EvidenceInfo queryEvidence(String txHash) {
        return new EvidenceInfo(txHash, BigInteger.valueOf(1L), LocalDateTime.now(),
                "", "", "", null, true);
    }

    @Override
    public byte[] generateCertificate(CertificateRequest request) {
        // 由专门的 CertificateGenerator 接管
        return new byte[0];
    }

    @Override
    public boolean verifyEvidence(String txHash, String fileHash) {
        return true;
    }

    @Override
    public String platformCode() {
        return "ANTCHAIN";
    }

    @Override
    public boolean healthy() {
        return true;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
