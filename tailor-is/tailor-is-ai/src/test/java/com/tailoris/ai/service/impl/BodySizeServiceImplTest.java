package com.tailoris.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.ai.dto.SizeDataRequest;
import com.tailoris.ai.entity.BodySizeData;
import com.tailoris.ai.mapper.BodySizeDataMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BodySizeServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class BodySizeServiceImplTest {

    @Mock
    private BodySizeDataMapper bodySizeDataMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BodySizeServiceImpl bodySizeService;

    @Test
    @DisplayName("新增体型数据成功")
    void testManageSizeData_Insert() {
        SizeDataRequest request = buildRequest("标准体型", 0);
        when(bodySizeDataMapper.insert(any(BodySizeData.class))).thenReturn(1);

        BodySizeData result = bodySizeService.manageSizeData(100L, request);

        assertNotNull(result);
        assertEquals(100L, result.getUserId());
        assertEquals("标准体型", result.getSizeName());
        assertEquals(0, result.getIsDefault());
        verify(bodySizeDataMapper).insert(any(BodySizeData.class));
    }

    @Test
    @DisplayName("查询用户体型数据列表")
    void testListUserSizeData() {
        BodySizeData data1 = new BodySizeData();
        data1.setSizeName("标准");
        BodySizeData data2 = new BodySizeData();
        data2.setSizeName("宽松");

        when(bodySizeDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(data1, data2));

        List<BodySizeData> result = bodySizeService.listUserSizeData(100L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("获取体型数据详情成功")
    void testGetSizeData_Success() {
        BodySizeData data = new BodySizeData();
        data.setId(1L);
        data.setUserId(100L);
        data.setSizeName("标准体型");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(bodySizeDataMapper.selectById(1L)).thenReturn(data);

        BodySizeData result = bodySizeService.getSizeData(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("标准体型", result.getSizeName());
    }

    @Test
    @DisplayName("获取不存在的体型数据返回null")
    void testGetSizeData_NotFound() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(bodySizeDataMapper.selectById(99L)).thenReturn(null);

        BodySizeData result = bodySizeService.getSizeData(99L);

        assertNull(result);
    }

    @Test
    @DisplayName("删除体型数据成功")
    void testDelete_Success() {
        BodySizeData data = new BodySizeData();
        data.setId(1L);
        data.setUserId(100L);

        when(bodySizeDataMapper.selectById(1L)).thenReturn(data);
        when(bodySizeDataMapper.deleteById(1L)).thenReturn(1);

        bodySizeService.deleteSizeData(100L, 1L);

        verify(bodySizeDataMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除他人数据抛出异常")
    void testDelete_NotOwner() {
        BodySizeData data = new BodySizeData();
        data.setId(1L);
        data.setUserId(999L);

        when(bodySizeDataMapper.selectById(1L)).thenReturn(data);

        assertThrows(BusinessException.class, () ->
                bodySizeService.deleteSizeData(100L, 1L));
        verify(bodySizeDataMapper, never()).deleteById(anyLong());
    }

    private SizeDataRequest buildRequest(String name, int isDefault) {
        SizeDataRequest request = new SizeDataRequest();
        request.setSizeName(name);
        request.setHeight(new BigDecimal("170"));
        request.setWeight(new BigDecimal("65"));
        request.setShoulderWidth(new BigDecimal("44"));
        request.setChestCircumference(new BigDecimal("96"));
        request.setWaistCircumference(new BigDecimal("82"));
        request.setHipCircumference(new BigDecimal("95"));
        request.setGender(1);
        request.setBodyType("normal");
        request.setIsDefault(isDefault);
        return request;
    }
}