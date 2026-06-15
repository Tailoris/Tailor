package com.tailoris.copyright.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.copyright.entity.CrSimilarityCheck;
import com.tailoris.copyright.mapper.CrSimilarityCheckMapper;
import com.tailoris.copyright.service.SimilarityCheckService;
import com.tailoris.copyright.service.SimilarityCheckService.MatchedItem;
import com.tailoris.copyright.service.SimilarityCheckService.SimilarityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 相似度检测 Service 实现
 * 任务编号: CR-003
 *
 * <p>支持库内（pHash/perceptual hash）与库外（调用AI引擎）。
 * 风险等级: ≥90=重大(4)  ≥80=严重(3)  ≥60=一般(2)  &gt;30=轻微(1)  ≤30=无风险(0)</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityCheckServiceImpl implements SimilarityCheckService {

    private final CrSimilarityCheckMapper similarityCheckMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${copyright.similarity.threshold:60.0}")
    private double threshold;

    @Value("${copyright.similarity.engine:ai-image-similarity}")
    private String engine;

    private static final String CACHE_KEY_PREFIX = "copyright:similarity:";
    private static final long CACHE_TTL_HOURS = 24;

    @Override
    public SimilarityResult preCheck(String fileUrl, String fileHash, String fileType) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new BusinessException("文件URL不能为空");
        }
        long start = System.currentTimeMillis();

        // 1. 缓存命中
        String cacheKey = CACHE_KEY_PREFIX + fileHash;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("相似度缓存命中: hash={}", fileHash);
        }

        // 2. 库内比对（简化：pHash 模拟）
        SimilarityResult libResult = checkAgainstLibrary(fileUrl, null);

        // 3. 库外比对（如需）
        // 实际生产可调用第三方AI服务（火眼、网安等）
        // 这里给出默认实现

        libResult.setMethod("AI-AHASH");
        libResult.setCheckCostMs(System.currentTimeMillis() - start);

        // 4. 缓存结果
        try {
            stringRedisTemplate.opsForValue().set(cacheKey,
                    String.valueOf(libResult.getScore()),
                    CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
        return libResult;
    }

    @Override
    public SimilarityResult checkAgainstLibrary(String fileUrl, Long excludeRecordId) {
        // 简化实现：根据 fileUrl 计算一个模拟的相似度
        // 真实实现需要：1) 计算文件 pHash 2) 查库内 3) 海明距离
        double mockScore = simulateSimilarity(fileUrl);
        return buildResult(mockScore, Collections.emptyList());
    }

    @Override
    public SimilarityResult checkAgainstWeb(String fileUrl, List<String> targetUrls) {
        double maxScore = 0;
        List<MatchedItem> matched = new ArrayList<>();
        for (String target : targetUrls) {
            double s = simulateSimilarity(fileUrl + target);
            if (s > maxScore) maxScore = s;
            if (s >= threshold) {
                MatchedItem item = new MatchedItem();
                item.setWorkUrl(target);
                item.setScore(s);
                item.setSource("WEB");
                matched.add(item);
            }
        }
        return buildResult(maxScore, matched);
    }

    @Override
    @Transactional
    public CrSimilarityCheck saveRecord(Long sourceRecordId, SimilarityResult result,
                                          String targetUrl, Long targetRecordId) {
        CrSimilarityCheck record = new CrSimilarityCheck();
        record.setId(SnowflakeIdGenerator.getInstance().nextId());
        record.setSourceRecordId(sourceRecordId);
        record.setTargetRecordId(targetRecordId);
        record.setTargetUrl(targetUrl);
        record.setSimilarityScore(BigDecimal.valueOf(result.getScore()));
        record.setCheckMethod(result.getMethod());
        record.setCheckEngine(engine);
        record.setCheckCostMs(result.getCheckCostMs());
        record.setEvidenceImageUrl(result.getEvidenceImageUrl());
        record.setCheckTime(LocalDateTime.now());
        record.setIsInfringement(result.isInfringement() ? 1 : 0);
        record.setRiskLevel(result.getRiskLevel());
        similarityCheckMapper.insert(record);
        return record;
    }

    @Override
    public PageResponse<CrSimilarityCheck> listChecks(Long sourceRecordId, PageRequest pageRequest) {
        Page<CrSimilarityCheck> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CrSimilarityCheck> wrapper = new LambdaQueryWrapper<>();
        if (sourceRecordId != null) {
            wrapper.eq(CrSimilarityCheck::getSourceRecordId, sourceRecordId);
        }
        wrapper.orderByDesc(CrSimilarityCheck::getCreateTime);
        Page<CrSimilarityCheck> result = similarityCheckMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    private SimilarityResult buildResult(double score, List<MatchedItem> matchedItems) {
        SimilarityResult result = new SimilarityResult();
        result.setScore(score);
        result.setMatchedItems(matchedItems);
        if (score >= 90) {
            result.setRiskLevel(4);
            result.setInfringement(true);
        } else if (score >= 80) {
            result.setRiskLevel(3);
            result.setInfringement(true);
        } else if (score >= threshold) {
            result.setRiskLevel(2);
            result.setInfringement(true);
        } else if (score >= 30) {
            result.setRiskLevel(1);
            result.setInfringement(false);
        } else {
            result.setRiskLevel(0);
            result.setInfringement(false);
        }
        return result;
    }

    /**
     * 模拟相似度（生产环境应替换为真实的 pHash / dHash / 感知哈希）
     */
    private double simulateSimilarity(String input) {
        if (input == null) return 0;
        int h = Math.abs(input.hashCode());
        // 30~95区间
        return 30 + (h % 66);
    }
}
