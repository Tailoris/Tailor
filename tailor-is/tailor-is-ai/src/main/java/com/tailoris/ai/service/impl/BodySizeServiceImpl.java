package com.tailoris.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.ai.dto.SizeDataRequest;
import com.tailoris.ai.entity.BodySizeData;
import com.tailoris.ai.mapper.BodySizeDataMapper;
import com.tailoris.ai.service.BodySizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BodySizeServiceImpl implements BodySizeService {

    private final BodySizeDataMapper bodySizeDataMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SIZE_CACHE_KEY = "body:size:";

    @Override
    @Transactional
    public BodySizeData manageSizeData(Long userId, SizeDataRequest request) {
        BodySizeData data = new BodySizeData();
        data.setId(SnowflakeIdGenerator.getInstance().nextId());
        data.setUserId(userId);
        data.setSizeName(request.getSizeName());
        data.setHeight(request.getHeight());
        data.setWeight(request.getWeight());
        data.setShoulderWidth(request.getShoulderWidth());
        data.setChestCircumference(request.getChestCircumference());
        data.setWaistCircumference(request.getWaistCircumference());
        data.setHipCircumference(request.getHipCircumference());
        data.setNeckCircumference(request.getNeckCircumference());
        data.setArmLength(request.getArmLength());
        data.setSleeveLength(request.getSleeveLength());
        data.setWaistLength(request.getWaistLength());
        data.setInseamLength(request.getInseamLength());
        data.setBodyType(request.getBodyType());
        data.setGender(request.getGender());
        data.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);

        if (data.getIsDefault() == 1) {
            LambdaUpdateWrapper<BodySizeData> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(BodySizeData::getUserId, userId)
                         .eq(BodySizeData::getIsDefault, 1)
                         .set(BodySizeData::getIsDefault, 0);
            bodySizeDataMapper.update(null, updateWrapper);
        }

        bodySizeDataMapper.insert(data);
        stringRedisTemplate.delete(SIZE_CACHE_KEY + userId);
        return data;
    }

    @Override
    public BodySizeData getSizeData(Long sizeId) {
        String cacheKey = SIZE_CACHE_KEY + sizeId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return bodySizeDataMapper.selectById(sizeId);
        }
        BodySizeData data = bodySizeDataMapper.selectById(sizeId);
        if (data != null) {
            stringRedisTemplate.opsForValue().set(cacheKey, data.getId().toString(), 30, TimeUnit.MINUTES);
        }
        return data;
    }

    @Override
    public List<BodySizeData> listUserSizeData(Long userId) {
        LambdaQueryWrapper<BodySizeData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BodySizeData::getUserId, userId);
        wrapper.orderByDesc(BodySizeData::getIsDefault);
        wrapper.orderByDesc(BodySizeData::getCreateTime);
        return bodySizeDataMapper.selectList(wrapper);
    }

    @Override
    public List<BodySizeData> searchByBodyType(String bodyType, Integer gender) {
        LambdaQueryWrapper<BodySizeData> wrapper = new LambdaQueryWrapper<>();
        if (bodyType != null) {
            wrapper.eq(BodySizeData::getBodyType, bodyType);
        }
        if (gender != null) {
            wrapper.eq(BodySizeData::getGender, gender);
        }
        wrapper.last("LIMIT 50");
        return bodySizeDataMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void setDefaultSize(Long userId, Long sizeId) {
        LambdaUpdateWrapper<BodySizeData> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BodySizeData::getUserId, userId)
                     .eq(BodySizeData::getIsDefault, 1)
                     .set(BodySizeData::getIsDefault, 0);
        bodySizeDataMapper.update(null, updateWrapper);

        BodySizeData data = bodySizeDataMapper.selectById(sizeId);
        if (data == null || !data.getUserId().equals(userId)) {
            throw new BusinessException("体型数据不存在");
        }
        data.setIsDefault(1);
        bodySizeDataMapper.updateById(data);
    }

    @Override
    @Transactional
    public void deleteSizeData(Long userId, Long sizeId) {
        BodySizeData data = bodySizeDataMapper.selectById(sizeId);
        if (data == null || !data.getUserId().equals(userId)) {
            throw new BusinessException("体型数据不存在");
        }
        bodySizeDataMapper.deleteById(sizeId);
        stringRedisTemplate.delete(SIZE_CACHE_KEY + sizeId);
    }
}
