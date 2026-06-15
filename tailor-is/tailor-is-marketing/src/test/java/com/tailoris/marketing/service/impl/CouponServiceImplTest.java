package com.tailoris.marketing.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.marketing.entity.CouponTemplate;
import com.tailoris.marketing.entity.UserCoupon;
import com.tailoris.marketing.mapper.CouponTemplateMapper;
import com.tailoris.marketing.mapper.UserCouponMapper;
import com.tailoris.marketing.service.MktStatisticsService;
import com.tailoris.common.lock.DistributedLock;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CouponServiceImpl 单元测试 - 分布式锁与领券")
@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponTemplateMapper couponTemplateMapper;
    @Mock
    private UserCouponMapper userCouponMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private DistributedLock distributedLock;
    @Mock
    private MktStatisticsService statisticsService;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private CouponServiceImpl couponService;

    private CouponTemplate template;

    @BeforeEach
    void setUp() {
        template = new CouponTemplate();
        template.setId(1L);
        template.setName("测试券");
        template.setType(1);
        template.setDiscountType(1);
        template.setDiscountValue(new BigDecimal("10"));
        template.setMinAmount(new BigDecimal("50"));
        template.setTotalCount(100);
        template.setReceivedCount(50);
        template.setPerLimit(0); // disable perLimit Lua check by default
        template.setStatus(1);
        template.setStartTime(LocalDateTime.now().minusDays(1));
        template.setEndTime(LocalDateTime.now().plusDays(7));
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("领取优惠券：库存不足应抛异常并释放锁")
    void testReceiveCoupon_OutOfStock() {
        when(valueOps.setIfAbsent(any(), any(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(couponTemplateMapper.selectById(1L)).thenReturn(template);
        when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class))).thenReturn("token");
        template.setReceivedCount(100);
        template.setTotalCount(100);
        // 重新mock返回已领完的库存
        when(couponTemplateMapper.selectById(1L)).thenReturn(template);

        assertThrows(BusinessException.class,
                () -> couponService.receiveCoupon(1L, 1L));
        verify(distributedLock).unlock(anyString(), eq("token"));
    }

    @Test
    @DisplayName("领取优惠券：获取锁失败应抛异常")
    void testReceiveCoupon_LockFailed() {
        when(valueOps.setIfAbsent(any(), any(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(couponTemplateMapper.selectById(1L)).thenReturn(template);
        lenient().when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> couponService.receiveCoupon(1L, 1L));
    }

    @Test
    @DisplayName("领取优惠券：幂等性保护-重复请求")
    void testReceiveCoupon_Idempotent() {
        when(valueOps.setIfAbsent(any(), any(), anyLong(), any(TimeUnit.class))).thenReturn(false);
        assertThrows(BusinessException.class,
                () -> couponService.receiveCoupon(1L, 1L));
    }

    @Test
    @DisplayName("领取优惠券：已过领取时间")
    void testReceiveCoupon_BeforeReceiveTime() {
        when(valueOps.setIfAbsent(any(), any(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        template.setReceiveStartTime(LocalDateTime.now().plusHours(1));
        when(couponTemplateMapper.selectById(1L)).thenReturn(template);
        assertThrows(BusinessException.class,
                () -> couponService.receiveCoupon(1L, 1L));
    }

    @Test
    @DisplayName("使用优惠券：成功标记已使用")
    void testUseCoupon_Success() {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(1L);
        userCoupon.setUserId(1L);
        userCoupon.setCouponId(1L);
        userCoupon.setStatus(0);
        userCoupon.setValidEndTime(LocalDateTime.now().plusDays(1));
        when(userCouponMapper.selectById(1L)).thenReturn(userCoupon);
        when(couponTemplateMapper.selectById(1L)).thenReturn(template);
        couponService.useCoupon(1L, 1L, 100L);
        assertEquals(3, userCoupon.getStatus());
        verify(userCouponMapper).updateById(userCoupon);
    }

    @Test
    @DisplayName("使用优惠券：已过期")
    void testUseCoupon_Expired() {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(1L);
        userCoupon.setUserId(1L);
        userCoupon.setCouponId(1L);
        userCoupon.setStatus(0);
        userCoupon.setValidEndTime(LocalDateTime.now().minusDays(1));
        when(userCouponMapper.selectById(1L)).thenReturn(userCoupon);
        assertThrows(BusinessException.class,
                () -> couponService.useCoupon(1L, 1L, 100L));
    }
}
