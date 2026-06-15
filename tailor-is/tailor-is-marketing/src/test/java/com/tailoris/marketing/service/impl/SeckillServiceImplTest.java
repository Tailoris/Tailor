package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.marketing.dto.SeckillCreateRequest;
import com.tailoris.marketing.entity.SeckillActivity;
import com.tailoris.marketing.entity.SeckillProduct;
import com.tailoris.marketing.mapper.SeckillActivityMapper;
import com.tailoris.marketing.mapper.SeckillProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SeckillServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class SeckillServiceImplTest {

    @Mock
    private SeckillActivityMapper seckillActivityMapper;

    @Mock
    private SeckillProductMapper seckillProductMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SeckillServiceImpl seckillService;

    private SeckillActivity mockActivity;
    private SeckillProduct mockProduct;

    @BeforeEach
    void setUp() {
        mockActivity = new SeckillActivity();
        mockActivity.setId(1L);
        mockActivity.setName("测试秒杀活动");
        mockActivity.setStartTime(LocalDateTime.now().minusHours(1));
        mockActivity.setEndTime(LocalDateTime.now().plusHours(1));
        mockActivity.setStatus(1);

        mockProduct = new SeckillProduct();
        mockProduct.setId(100L);
        mockProduct.setActivityId(1L);
        mockProduct.setProductId(500L);
        mockProduct.setSeckillPrice(new BigDecimal("9.90"));
        mockProduct.setOriginalPrice(new BigDecimal("99.00"));
        mockProduct.setStock(100);
        mockProduct.setAvailableStock(100);
        mockProduct.setLimitCount(2);
        mockProduct.setStatus(1);
        mockProduct.setOrderCount(0);

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("创建秒杀活动成功")
    void testCreateSeckill_Success() {
        SeckillCreateRequest request = new SeckillCreateRequest();
        request.setName("双11秒杀");
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setProductId(500L);
        request.setSkuId(1000L);
        request.setSeckillPrice(new BigDecimal("9.90"));
        request.setOriginalPrice(new BigDecimal("99.00"));
        request.setStock(100);

        when(seckillActivityMapper.insert(any(SeckillActivity.class))).thenReturn(1);
        when(seckillProductMapper.insert(any(SeckillProduct.class))).thenReturn(1);
        when(seckillActivityMapper.updateById(any(SeckillActivity.class))).thenReturn(1);

        SeckillActivity result = seckillService.createActivity(request);

        assertNotNull(result);
        assertEquals("双11秒杀", result.getName());
        assertEquals(0, result.getStatus());

        verify(seckillActivityMapper).insert(any(SeckillActivity.class));
        verify(seckillProductMapper).insert(any(SeckillProduct.class));
    }

    @Test
    @DisplayName("参与秒杀成功")
    void testJoinSeckill_Success() {
        when(seckillProductMapper.selectById(100L)).thenReturn(mockProduct);
        when(seckillActivityMapper.selectById(1L)).thenReturn(mockActivity);
        when(stringRedisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);

        assertDoesNotThrow(() -> seckillService.joinSeckill(1L, 100L));
    }

    @Test
    @DisplayName("秒杀商品已售罄")
    void testPurchase_StockOut() {
        mockProduct.setAvailableStock(0);
        when(seckillProductMapper.selectById(100L)).thenReturn(mockProduct);
        when(seckillActivityMapper.selectById(1L)).thenReturn(mockActivity);
        when(stringRedisTemplate.execute(any(), anyList(), anyString())).thenReturn(0L);

        assertThrows(BusinessException.class, () -> seckillService.joinSeckill(1L, 100L));
    }

    @Test
    @DisplayName("获取秒杀商品详情")
    void testGetSeckillProduct_Success() {
        when(seckillProductMapper.selectById(100L)).thenReturn(mockProduct);

        SeckillProduct result = seckillService.getSeckillProduct(100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(new BigDecimal("9.90"), result.getSeckillPrice());
    }
}