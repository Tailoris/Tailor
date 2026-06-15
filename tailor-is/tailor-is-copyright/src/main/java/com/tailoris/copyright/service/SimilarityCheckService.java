package com.tailoris.copyright.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.copyright.entity.CrSimilarityCheck;

import java.util.List;

/**
 * 相似度检测 Service
 * 任务编号: CR-003 事前风控
 */
public interface SimilarityCheckService {

    /**
     * 相似度检测结果
     */
    class SimilarityResult {
        private double score; // 0-100
        private boolean isInfringement; // 是否侵权
        private Integer riskLevel; // 1-4
        private String evidenceImageUrl; // 对比图
        private String method; // 检测方法
        private Long checkCostMs; // 耗时
        private List<MatchedItem> matchedItems;

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public boolean isInfringement() { return isInfringement; }
        public void setInfringement(boolean infringement) { isInfringement = infringement; }
        public Integer getRiskLevel() { return riskLevel; }
        public void setRiskLevel(Integer riskLevel) { this.riskLevel = riskLevel; }
        public String getEvidenceImageUrl() { return evidenceImageUrl; }
        public void setEvidenceImageUrl(String evidenceImageUrl) { this.evidenceImageUrl = evidenceImageUrl; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public Long getCheckCostMs() { return checkCostMs; }
        public void setCheckCostMs(Long checkCostMs) { this.checkCostMs = checkCostMs; }
        public List<MatchedItem> getMatchedItems() { return matchedItems; }
        public void setMatchedItems(List<MatchedItem> matchedItems) { this.matchedItems = matchedItems; }
    }

    /** 匹配项 */
    class MatchedItem {
        private Long recordId;
        private String workName;
        private String workUrl;
        private double score;
        private String source; // LIBRARY / WEB
        public Long getRecordId() { return recordId; }
        public void setRecordId(Long recordId) { this.recordId = recordId; }
        public String getWorkName() { return workName; }
        public void setWorkName(String workName) { this.workName = workName; }
        public String getWorkUrl() { return workUrl; }
        public void setWorkUrl(String workUrl) { this.workUrl = workUrl; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }

    /**
     * 上传前相似度检测（库内+库外AI服务）
     */
    SimilarityResult preCheck(String fileUrl, String fileHash, String fileType);

    /**
     * 与库内指定记录比对
     */
    SimilarityResult checkAgainstLibrary(String fileUrl, Long excludeRecordId);

    /**
     * 与库外URL比对（爬虫抓取 + AI对比）
     */
    SimilarityResult checkAgainstWeb(String fileUrl, List<String> targetUrls);

    /**
     * 保存检测记录
     */
    CrSimilarityCheck saveRecord(Long sourceRecordId, SimilarityResult result, String targetUrl, Long targetRecordId);

    /**
     * 查询检测历史
     */
    PageResponse<CrSimilarityCheck> listChecks(Long sourceRecordId, PageRequest pageRequest);
}
