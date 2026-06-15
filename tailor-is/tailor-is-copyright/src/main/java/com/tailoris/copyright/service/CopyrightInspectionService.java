package com.tailoris.copyright.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.copyright.entity.CrInspectionTask;
import com.tailoris.copyright.entity.CrViolationHandling;
import com.tailoris.copyright.mapper.CrInspectionTaskMapper;
import com.tailoris.copyright.mapper.CrViolationHandlingMapper;
import com.tailoris.copyright.service.SimilarityCheckService.MatchedItem;
import com.tailoris.copyright.service.SimilarityCheckService.SimilarityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 巡检 Service
 * 任务编号: CR-005 事中风控
 *
 * <p>机器日检 + 人工月检工作流</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopyrightInspectionService {

    private final CrInspectionTaskMapper inspectionTaskMapper;
    private final CrViolationHandlingMapper violationHandlingMapper;
    private final SimilarityCheckService similarityCheckService;
    private final com.tailoris.copyright.mapper.CopyrightRecordMapper copyrightRecordMapper;

    private static final int SAMPLE_PER_DAY = 200; // 每日抽样数

    /**
     * 定时任务：每日凌晨02:00执行机器日检
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyMachineInspection() {
        log.info("[CR-005] 开始执行机器日检...");
        long start = System.currentTimeMillis();
        int count = 0;
        try {
            count = runMachineInspection(1, "每日全量巡检", SAMPLE_PER_DAY);
        } catch (Exception e) {
            log.error("机器日检异常", e);
        }
        log.info("[CR-005] 机器日检完成,处理{}个,耗时{}ms", count, System.currentTimeMillis() - start);
    }

    /**
     * 定时任务：每月1号03:00生成人工月检任务
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void monthlyManualInspection() {
        log.info("[CR-005] 生成人工月检任务...");
        try {
            CrInspectionTask task = new CrInspectionTask();
            task.setId(SnowflakeIdGenerator.getInstance().nextId());
            task.setTaskType(2);
            task.setTaskName("人工月检-" + LocalDate.now());
            task.setExecutorType(2); // 人工
            task.setStatus(0);
            task.setScheduledTime(LocalDateTime.now());
            task.setCronExpr("0 0 3 1 * ?");
            inspectionTaskMapper.insert(task);
            log.info("人工月检任务已创建,id={}", task.getId());
        } catch (Exception e) {
            log.error("生成人工月检任务失败", e);
        }
    }

    /**
     * 立即执行机器巡检
     */
    @Transactional
    public int runMachineInspection(Integer taskType, String taskName, int sampleSize) {
        CrInspectionTask task = new CrInspectionTask();
        task.setId(SnowflakeIdGenerator.getInstance().nextId());
        task.setTaskType(taskType);
        task.setTaskName(taskName);
        task.setExecutorType(1); // 系统
        task.setStatus(1);
        task.setStartTime(LocalDateTime.now());
        task.setScheduledTime(LocalDateTime.now());
        inspectionTaskMapper.insert(task);

        int count = 0;
        try {
            // 抽样最近N个版权记录
            List<com.tailoris.copyright.entity.CopyrightRecord> samples = sampleRecentCopyrights(sampleSize);
            for (com.tailoris.copyright.entity.CopyrightRecord record : samples) {
                try {
                    boolean violate = checkSingleRecord(record, task.getId());
                    if (violate) count++;
                } catch (Exception e) {
                    log.warn("单条巡检失败: recordId={}", record.getId(), e);
                }
            }
            task.setStatus(2);
            task.setEndTime(LocalDateTime.now());
            task.setDurationMs(System.currentTimeMillis() -
                    (task.getStartTime() == null ? System.currentTimeMillis() : toEpoch(task.getStartTime())));
            task.setCheckResult(count > 0 ? 1 : 0);
            task.setResultDetail("巡检样本" + samples.size() + "条,发现违规" + count + "条");
            inspectionTaskMapper.updateById(task);
            return count;
        } catch (Exception e) {
            task.setStatus(3);
            task.setResultDetail("异常: " + e.getMessage());
            inspectionTaskMapper.updateById(task);
            throw e;
        }
    }

    /**
     * 单条记录巡检
     */
    @Transactional
    public boolean checkSingleRecord(com.tailoris.copyright.entity.CopyrightRecord record, Long inspectionId) {
        if (record == null || record.getFileUrl() == null) {
            return false;
        }
        // 1. AI 相似度检测
        SimilarityResult sim = similarityCheckService.preCheck(
                record.getFileUrl(), record.getFileHash(), record.getFileType());
        if (sim.getScore() >= 80) {
            // 记录违规
            CrViolationHandling violation = new CrViolationHandling();
            violation.setId(SnowflakeIdGenerator.getInstance().nextId());
            violation.setRecordId(record.getId());
            violation.setInspectionId(inspectionId);
            violation.setViolationType(sim.getScore() >= 90 ? 1 : 2);
            violation.setViolationLevel(sim.getRiskLevel());
            violation.setDescription("机器巡检发现高度相似作品,疑似侵权,相似度=" + sim.getScore() + "%");
            violation.setEvidenceUrls(sim.getEvidenceImageUrl());
            violation.setStatus(0);
            violationHandlingMapper.insert(violation);
            log.warn("巡检发现疑似侵权: recordId={}, score={}", record.getId(), sim.getScore());
            return true;
        }
        return false;
    }

    /**
     * 处置违规
     */
    @Transactional
    public CrViolationHandling handleViolation(Long violationId, Long handlerId,
                                                Integer handleType, String handleRemark) {
        CrViolationHandling v = violationHandlingMapper.selectById(violationId);
        if (v == null) {
            throw new BusinessException("违规记录不存在");
        }
        v.setHandleType(handleType);
        v.setHandleRemark(handleRemark);
        v.setHandlerId(handlerId);
        v.setHandleTime(LocalDateTime.now());
        v.setStatus(1);
        violationHandlingMapper.updateById(v);
        return v;
    }

    /**
     * 待办违规列表
     */
    public PageResponse<CrViolationHandling> listPending(PageRequest pageRequest) {
        Page<CrViolationHandling> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CrViolationHandling> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CrViolationHandling::getViolationLevel);
        wrapper.orderByAsc(CrViolationHandling::getCreateTime);
        Page<CrViolationHandling> result = violationHandlingMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    /**
     * 巡检任务列表
     */
    public PageResponse<CrInspectionTask> listTasks(PageRequest pageRequest, Integer taskType, Integer status) {
        Page<CrInspectionTask> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CrInspectionTask> wrapper = new LambdaQueryWrapper<>();
        if (taskType != null) {
            wrapper.eq(CrInspectionTask::getTaskType, taskType);
        }
        if (status != null) {
            wrapper.eq(CrInspectionTask::getStatus, status);
        }
        wrapper.orderByDesc(CrInspectionTask::getCreateTime);
        Page<CrInspectionTask> result = inspectionTaskMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    private List<com.tailoris.copyright.entity.CopyrightRecord> sampleRecentCopyrights(int n) {
        // 简化：取最近N条
        Page<com.tailoris.copyright.entity.CopyrightRecord> page = new Page<>(1, n);
        Page<com.tailoris.copyright.entity.CopyrightRecord> result = copyrightRecordMapper.selectPage(page,
                new LambdaQueryWrapper<com.tailoris.copyright.entity.CopyrightRecord>()
                        .orderByDesc(com.tailoris.copyright.entity.CopyrightRecord::getCreateTime));
        return result.getRecords() != null ? result.getRecords() : new ArrayList<>();
    }

    private long toEpoch(LocalDateTime t) {
        return t.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
