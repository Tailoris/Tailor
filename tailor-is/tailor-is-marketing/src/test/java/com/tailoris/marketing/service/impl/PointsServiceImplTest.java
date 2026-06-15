package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.marketing.dto.PointsExchangeRequest;
import com.tailoris.marketing.entity.PointsMallProduct;
import com.tailoris.marketing.entity.PointsRecord;
import com.tailoris.marketing.mapper.PointsMallProductMapper;
import com.tailoris.marketing.mapper.PointsRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("积分服务单元测试")
@ExtendWith(MockitoExtension.class)
class PointsServiceImplTest {

    @Mock
    private PointsRecordMapper pointsRecordMapper;

    @Mock
    private PointsMallProductMapper pointsMallProductMapper;

    @InjectMocks
    private PointsServiceImpl pointsService;

    private PointsMallProduct product;
    private PointsRecord record;

    @BeforeEach
    void setUp() {
        product = new PointsMallProduct();
        product.setId(1L);
        product.setName("测试商品");
        product.setPointsRequired(100);
        product.setStock(50);
        product.setExchangeCount(10);
        product.setStatus(1);
        product.setSort(1);

        record = new PointsRecord();
        record.setId(1L);
        record.setUserId(100L);
        record.setPointsChange(50);
        record.setChangeType(1);
        record.setPointsBefore(100);
        record.setPointsAfter(150);
    }

    @Test
    @DisplayName("兑换积分商品：商品不存在应抛异常")
    void testExchangePoints_ProductNotFound() {
        PointsExchangeRequest request = new PointsExchangeRequest();
        request.setProductId(999L);
        request.setQuantity(1);

        when(pointsMallProductMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> pointsService.exchangePoints(100L, request));
    }

    @Test
    @DisplayName("兑换积分商品：库存不足应抛异常")
    void testExchangePoints_InsufficientStock() {
        PointsExchangeRequest request = new PointsExchangeRequest();
        request.setProductId(1L);
        request.setQuantity(100);

        product.setStock(10);
        when(pointsMallProductMapper.selectById(1L)).thenReturn(product);

        assertThrows(BusinessException.class, () -> pointsService.exchangePoints(100L, request));
    }

    @Test
    @DisplayName("兑换积分商品：积分不足应抛异常")
    void testExchangePoints_InsufficientPoints() {
        PointsExchangeRequest request = new PointsExchangeRequest();
        request.setProductId(1L);
        request.setQuantity(1);

        record.setPointsAfter(50);
        when(pointsMallProductMapper.selectById(1L)).thenReturn(product);
        when(pointsRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        assertThrows(BusinessException.class, () -> pointsService.exchangePoints(100L, request));
    }

    @Test
    @DisplayName("兑换积分商品：成功兑换")
    void testExchangePoints_Success() {
        PointsExchangeRequest request = new PointsExchangeRequest();
        request.setProductId(1L);
        request.setQuantity(1);

        record.setPointsAfter(200);
        when(pointsMallProductMapper.selectById(1L)).thenReturn(product);
        when(pointsRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);
        when(pointsRecordMapper.insert(any(PointsRecord.class))).thenReturn(1);
        when(pointsMallProductMapper.updateById(any(PointsMallProduct.class))).thenReturn(1);

        assertDoesNotThrow(() -> pointsService.exchangePoints(100L, request));
        assertEquals(49, product.getStock());
        assertEquals(11, product.getExchangeCount());
    }

    @Test
    @DisplayName("记录积分：积分不足应抛异常")
    void testRecordPoints_InsufficientPoints() {
        record.setPointsAfter(50);
        when(pointsRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        assertThrows(BusinessException.class, () ->
                pointsService.recordPoints(100L, -100, 1, "order", 1L, "测试"));
    }

    @Test
    @DisplayName("记录积分：成功记录")
    void testRecordPoints_Success() {
        record.setPointsAfter(100);
        when(pointsRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);
        when(pointsRecordMapper.insert(any(PointsRecord.class))).thenReturn(1);

        assertDoesNotThrow(() -> pointsService.recordPoints(100L, 50, 1, "order", 1L, "测试"));
        verify(pointsRecordMapper).insert(any(PointsRecord.class));
    }

    @Test
    @DisplayName("获取积分余额：无记录返回0")
    void testGetPointsBalance_NoRecord() {
        when(pointsRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Integer result = pointsService.getPointsBalance(100L);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("获取积分余额：成功返回")
    void testGetPointsBalance_Success() {
        when(pointsRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        Integer result = pointsService.getPointsBalance(100L);

        assertEquals(150, result);
    }

    @Test
    @DisplayName("列出积分商城商品：成功返回")
    void testListPointsMallProducts_Success() {
        when(pointsMallProductMapper.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        assertDoesNotThrow(() -> pointsService.listPointsMallProducts(
                new com.tailoris.common.dto.PageRequest()));
    }
}
