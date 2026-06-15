package com.tailoris.copyright.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.copyright.entity.CrInfringementCase;
import com.tailoris.copyright.entity.CrInfringementLog;
import com.tailoris.copyright.entity.CrSimilarityCheck;
import com.tailoris.copyright.entity.CopyrightRecord;
import com.tailoris.copyright.mapper.CrInfringementCaseMapper;
import com.tailoris.copyright.mapper.CrInfringementLogMapper;
import com.tailoris.copyright.mapper.CrSimilarityCheckMapper;
import com.tailoris.copyright.mapper.CopyrightRecordMapper;
import com.tailoris.copyright.service.SimilarityCheckService.SimilarityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 侵权维权 Service
 * 任务编号: CR-006
 *
 * <p>72小时仲裁：案件创建后72小时内必须完成仲裁，超时自动升级。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopyrightInfringementService {

    private final CrInfringementCaseMapper caseMapper;
    private final CrInfringementLogMapper logMapper;
    private final CrSimilarityCheckMapper similarityCheckMapper;
    private final CopyrightRecordMapper copyrightRecordMapper;
    private final SimilarityCheckService similarityCheckService;

    private static final int ARBITRATION_HOURS = 72;
    private static final int EVIDENCE_RETENTION_DAYS = 365 * 5; // 5年

    /**
     * 创建侵权案件
     */
    @Transactional
    public CrInfringementCase createCase(Long recordId, String infringementSource,
                                          String infringerName, String infringerContact,
                                          Integer infringementType, SimilarityResult evidence) {
        CopyrightRecord record = copyrightRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("版权记录不存在");
        }

        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(SnowflakeIdGenerator.getInstance().nextId());
        caseEntity.setCaseNo(generateCaseNo());
        caseEntity.setRecordId(recordId);
        caseEntity.setCopyrightUserId(record.getUserId());
        caseEntity.setInfringerName(infringerName);
        caseEntity.setInfringerContact(infringerContact);
        caseEntity.setInfringementSource(infringementSource);
        caseEntity.setDiscoveredAt(LocalDateTime.now());
        caseEntity.setInfringementType(infringementType);
        if (evidence != null) {
            caseEntity.setSimilarityScore(java.math.BigDecimal.valueOf(evidence.getScore()));
            caseEntity.setEvidenceChain(buildEvidenceChain(record, evidence));
        }
        caseEntity.setStatus(0); // 待处理
        // 72小时仲裁截止
        caseEntity.setArbitrationDeadline(LocalDateTime.now().plusHours(ARBITRATION_HOURS));
        caseMapper.insert(caseEntity);

        // 写入流转日志
        logTransition(caseEntity.getId(), null, 0, "创建案件", "用户/系统上报侵权", null, "system", 4);
        log.info("创建侵权案件: caseNo={}, recordId={}, deadline={}",
                caseEntity.getCaseNo(), recordId, caseEntity.getArbitrationDeadline());
        return caseEntity;
    }

    /**
     * 受理案件
     */
    @Transactional
    public CrInfringementCase acceptCase(Long caseId, Long operatorId, String operatorName) {
        CrInfringementCase c = checkAndGet(caseId, 0);
        c.setStatus(1); // 取证中
        caseMapper.updateStatusIfMatch(caseId, 0, 1);
        logTransition(caseId, 0, 1, "受理案件", "运营人员受理", operatorId, operatorName, 2);
        return c;
    }

    /**
     * 提交仲裁（72小时内）
     */
    @Transactional
    public CrInfringementCase submitArbitration(Long caseId, Long operatorId, String operatorName, String remark) {
        CrInfringementCase c = checkAndGet(caseId, 1);
        if (c.getArbitrationDeadline() != null && LocalDateTime.now().isAfter(c.getArbitrationDeadline())) {
            // 超时：升级到立案
            c.setStatus(4);
            c.setArbitrationResult("72小时未完成仲裁,自动升级立案");
            c.setArbitrationAt(LocalDateTime.now());
            caseMapper.updateStatusIfMatch(caseId, 1, 4);
            logTransition(caseId, 1, 4, "自动立案", "超过72小时未完成仲裁,自动升级", null, "system", 4);
            throw new BusinessException("已超过72小时仲裁期,案件已自动升级立案");
        }
        c.setStatus(3); // 仲裁中
        caseMapper.updateStatusIfMatch(caseId, 1, 3);
        logTransition(caseId, 1, 3, "提交仲裁", remark, operatorId, operatorName, 3);
        return c;
    }

    /**
     * 完成仲裁
     */
    @Transactional
    public CrInfringementCase completeArbitration(Long caseId, Long arbitratorId, String arbitratorName,
                                                   String result, Boolean support) {
        CrInfringementCase c = checkAndGet(caseId, 3);
        c.setArbitratorId(arbitratorId);
        c.setArbitrationResult(result);
        c.setArbitrationAt(LocalDateTime.now());
        c.setStatus(support != null && support ? 5 : 6);
        caseMapper.updateStatusIfMatch(caseId, 3, c.getStatus());
        logTransition(caseId, 3, c.getStatus(), "仲裁完成",
                "仲裁员" + arbitratorName + "：" + result, arbitratorId, arbitratorName, 3);
        return c;
    }

    /**
     * 立案诉讼
     */
    @Transactional
    public CrInfringementCase fileLawsuit(Long caseId, String courtName, String courtCaseNo,
                                            String lawyerName, String lawyerContact, Long operatorId) {
        CrInfringementCase c = checkAndGet(caseId, null);
        c.setCourtName(courtName);
        c.setCourtCaseNo(courtCaseNo);
        c.setLawyerName(lawyerName);
        c.setLawyerContact(lawyerContact);
        c.setStatus(4);
        caseMapper.updateById(c);
        logTransition(caseId, c.getStatus(), 4, "立案诉讼",
                "法院:" + courtName + ",案号:" + courtCaseNo, operatorId, "operator", 2);
        return c;
    }

    /**
     * 法院判决
     */
    @Transactional
    public CrInfringementCase courtVerdict(Long caseId, Integer status, java.math.BigDecimal compensation, Long operatorId) {
        CrInfringementCase c = checkAndGet(caseId, 4);
        c.setStatus(status);
        c.setCompensation(compensation);
        c.setClosedAt(LocalDateTime.now());
        caseMapper.updateById(c);
        logTransition(caseId, 4, status, "法院判决",
                "判赔:" + compensation, operatorId, "operator", 2);
        return c;
    }

    /**
     * 主动撤回
     */
    @Transactional
    public CrInfringementCase withdrawCase(Long caseId, Long userId, String reason) {
        CrInfringementCase c = checkAndGet(caseId, null);
        if (!c.getCopyrightUserId().equals(userId)) {
            throw new BusinessException("非本人不能撤回");
        }
        c.setStatus(8);
        c.setClosedAt(LocalDateTime.now());
        caseMapper.updateById(c);
        logTransition(caseId, c.getStatus(), 8, "撤回案件", reason, userId, "用户", 1);
        return c;
    }

    /**
     * 定时任务：每10分钟扫描超时未仲裁案件
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    public void scanOverdueArbitration() {
        List<CrInfringementCase> overdue = caseMapper.selectOverdueArbitration();
        for (CrInfringementCase c : overdue) {
            try {
                c.setStatus(4);
                c.setArbitrationResult("72小时未完成仲裁,自动升级立案");
                c.setArbitrationAt(LocalDateTime.now());
                caseMapper.updateById(c);
                logTransition(c.getId(), c.getStatus(), 4, "自动立案", "超过72小时,自动升级", null, "system", 4);
                log.warn("案件超时自动升级: caseNo={}", c.getCaseNo());
            } catch (Exception e) {
                log.error("处理超时案件失败: caseId={}", c.getId(), e);
            }
        }
        if (!overdue.isEmpty()) {
            log.info("扫描超时案件,处理{}个", overdue.size());
        }
    }

    /**
     * 案件详情
     */
    public CrInfringementCase getCaseDetail(Long caseId) {
        return caseMapper.selectById(caseId);
    }

    /**
     * 案件列表
     */
    public PageResponse<CrInfringementCase> listUserCases(Long userId, PageRequest pageRequest) {
        Page<CrInfringementCase> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CrInfringementCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrInfringementCase::getCopyrightUserId, userId);
        wrapper.orderByDesc(CrInfringementCase::getCreateTime);
        Page<CrInfringementCase> result = caseMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    public List<CrInfringementLog> listCaseLogs(Long caseId) {
        return logMapper.selectByCase(caseId);
    }

    private CrInfringementCase checkAndGet(Long caseId, Integer expectedStatus) {
        CrInfringementCase c = caseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException("案件不存在");
        }
        if (expectedStatus != null && !expectedStatus.equals(c.getStatus())) {
            throw new BusinessException("案件状态不匹配,期望:" + expectedStatus + ",实际:" + c.getStatus());
        }
        return c;
    }

    private void logTransition(Long caseId, Integer fromStatus, Integer toStatus, String action,
                                String remark, Long operatorId, String operatorName, Integer operatorType) {
        CrInfringementLog log = new CrInfringementLog();
        log.setId(SnowflakeIdGenerator.getInstance().nextId());
        log.setCaseId(caseId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setRemark(remark);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperatorType(operatorType);
        logMapper.insert(log);
    }

    private String buildEvidenceChain(CopyrightRecord record, SimilarityResult evidence) {
        // 简单 JSON 序列化（生产可换 Jackson）
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"recordId\":").append(record.getId()).append(",");
        sb.append("\"fileHash\":\"").append(record.getFileHash()).append("\",");
        sb.append("\"registeredAt\":\"").append(record.getRegisteredAt()).append("\",");
        sb.append("\"authorId\":").append(record.getUserId()).append(",");
        sb.append("\"authorName\":\"").append(record.getAuthorRealName()).append("\",");
        sb.append("\"similarity\":").append(evidence.getScore()).append(",");
        sb.append("\"method\":\"").append(evidence.getMethod()).append("\",");
        sb.append("\"evidenceUrl\":\"").append(evidence.getEvidenceImageUrl()).append("\"}");
        return sb.toString();
    }

    private String generateCaseNo() {
        return "IN-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
