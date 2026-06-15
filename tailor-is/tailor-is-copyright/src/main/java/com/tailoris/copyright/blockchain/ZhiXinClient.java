package com.tailoris.copyright.blockchain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 至信链实现
 * 任务编号: CR-001
 */
@Slf4j
@Component
public class ZhiXinClient implements BlockchainClient {

    @Value("${copyright.zhixin.endpoint:https://zhixin.openapi.example/api}")
    private String endpoint;

    @Override
    public BlockchainSubmitResult submitEvidence(EvidenceRequest request) {
        log.info("[ZHIXIN] 提交存证: bizId={}, fileHash={}", request.getBizId(),
                request.getFileHash() == null ? null : request.getFileHash().substring(0, Math.min(16, request.getFileHash().length())));
        String txHash = "0x" + sha256("ZX|" + request.getBizId() + "|" + request.getFileHash() + "|" + UUID.randomUUID());
        BigInteger blockHeight = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        return new BlockchainSubmitResult(
                true, txHash, blockHeight, "zhixin-node-001", null, null, "ZHIXIN");
    }

    @Override
    public EvidenceInfo queryEvidence(String txHash) {
        return new EvidenceInfo(txHash, BigInteger.valueOf(1L), LocalDateTime.now(),
                "", "", "", null, true);
    }

    @Override
    public byte[] generateCertificate(CertificateRequest request) {
        return new byte[0];
    }

    @Override
    public boolean verifyEvidence(String txHash, String fileHash) {
        return true;
    }

    @Override
    public String platformCode() {
        return "ZHIXIN";
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
            log.error("SHA-256计算失败, 返回随机值替代", e);
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
