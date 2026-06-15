package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.marketing.dto.PointsExchangeRequest;
import com.tailoris.marketing.entity.PointsMallProduct;
import com.tailoris.marketing.entity.PointsRecord;
import com.tailoris.marketing.mapper.PointsMallProductMapper;
import com.tailoris.marketing.mapper.PointsRecordMapper;
import com.tailoris.marketing.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private final PointsRecordMapper pointsRecordMapper;
    private final PointsMallProductMapper pointsMallProductMapper;

    @Override
    @Transactional
    public void exchangePoints(Long userId, PointsExchangeRequest request) {
        PointsMallProduct product = pointsMallProductMapper.selectById(request.getProductId());
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException("积分商品不存在或已下架");
        }
        if (product.getStock() < request.getQuantity()) {
            throw new BusinessException("库存不足");
        }

        int totalPoints = product.getPointsRequired() * request.getQuantity();
        Integer currentBalance = getPointsBalance(userId);
        if (currentBalance < totalPoints) {
            throw new BusinessException("积分不足");
        }

        recordPoints(userId, -totalPoints, 5, "points_mall", request.getProductId(), "兑换积分商品：" + product.getName());

        product.setStock(product.getStock() - request.getQuantity());
        product.setExchangeCount(product.getExchangeCount() + request.getQuantity());
        pointsMallProductMapper.updateById(product);
    }

    @Override
    @Transactional
    public void recordPoints(Long userId, Integer pointsChange, Integer changeType, String relatedType, Long relatedId, String description) {
        Integer currentBalance = getPointsBalance(userId);
        int pointsAfter = currentBalance + pointsChange;
        if (pointsAfter < 0) {
            throw new BusinessException("积分不足");
        }

        PointsRecord record = new PointsRecord();
        record.setId(SnowflakeIdGenerator.getInstance().nextId());
        record.setUserId(userId);
        record.setPointsChange(pointsChange);
        record.setChangeType(changeType);
        record.setRelatedType(relatedType);
        record.setRelatedId(relatedId);
        record.setDescription(description);
        record.setPointsBefore(currentBalance);
        record.setPointsAfter(pointsAfter);
        pointsRecordMapper.insert(record);
    }

    @Override
    public Integer getPointsBalance(Long userId) {
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId);
        wrapper.orderByDesc(PointsRecord::getCreateTime);
        wrapper.last("LIMIT 1");
        PointsRecord lastRecord = pointsRecordMapper.selectOne(wrapper);
        return lastRecord != null ? lastRecord.getPointsAfter() : 0;
    }

    @Override
    public PageResponse<PointsRecord> getPointsHistory(Long userId, PageRequest pageRequest) {
        Page<PointsRecord> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId);
        wrapper.orderByDesc(PointsRecord::getCreateTime);
        Page<PointsRecord> result = pointsRecordMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public PageResponse<PointsMallProduct> listPointsMallProducts(PageRequest pageRequest) {
        Page<PointsMallProduct> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<PointsMallProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsMallProduct::getStatus, 1);
        wrapper.orderByAsc(PointsMallProduct::getSort);
        Page<PointsMallProduct> result = pointsMallProductMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }
}
