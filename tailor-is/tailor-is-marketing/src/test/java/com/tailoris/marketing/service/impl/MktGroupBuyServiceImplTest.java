package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
import com.tailoris.marketing.entity.MktGroupBuy;
import com.tailoris.marketing.entity.MktGroupBuyInstance;
import com.tailoris.marketing.entity.MktGroupBuyMember;
import com.tailoris.marketing.mapper.MktGroupBuyInstanceMapper;
import com.tailoris.marketing.mapper.MktGroupBuyMapper;
import com.tailoris.marketing.mapper.MktGroupBuyMemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("MktGroupBuyServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class MktGroupBuyServiceImplTest {

    @Mock
    private MktGroupBuyMapper groupBuyMapper;

    @Mock
    private MktGroupBuyInstanceMapper instanceMapper;

    @Mock
    private MktGroupBuyMemberMapper memberMapper;

    @Mock
    private SpringSnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private MktGroupBuyServiceImpl groupBuyService;

    private MktGroupBuy activity;

    @BeforeEach
    void setUp() {
        lenient().when(snowflakeIdGenerator.nextId()).thenReturn(1L);
        activity = new MktGroupBuy();
        activity.setId(1L);
        activity.setActivityName("测试拼团");
        activity.setProductId(1L);
        activity.setShopId(1L);
        activity.setGroupSize(3);
        activity.setGroupPrice(new BigDecimal("99.00"));
        activity.setOriginalPrice(new BigDecimal("199.00"));
        activity.setTotalStock(100);
        activity.setSoldCount(0);
        activity.setStatus(1);
        activity.setStartTime(LocalDateTime.now().minusHours(1));
        activity.setEndTime(LocalDateTime.now().plusHours(24));
        activity.setValidHours(24);
    }

    @Test
    @DisplayName("成功创建拼团活动")
    void testCreateActivity_Success() {
        when(groupBuyMapper.insert(any(MktGroupBuy.class))).thenReturn(1);
        MktGroupBuy result = groupBuyService.createActivity(activity);
        assertNotNull(result);
        assertEquals(0, result.getStatus());
        assertEquals(0, result.getSoldCount());
        assertEquals(0, result.getGroupCount());
        verify(groupBuyMapper).insert(any(MktGroupBuy.class));
    }

    @Test
    @DisplayName("创建拼团：结束时间早于开始时间应抛异常")
    void testCreateActivity_EndTimeInvalid() {
        activity.setStartTime(LocalDateTime.now().plusDays(2));
        activity.setEndTime(LocalDateTime.now().plusDays(1));
        assertThrows(BusinessException.class, () -> groupBuyService.createActivity(activity));
    }

    @Test
    @DisplayName("创建拼团：拼团价必须小于原价")
    void testCreateActivity_PriceInvalid() {
        activity.setGroupPrice(new BigDecimal("299.00"));
        assertThrows(BusinessException.class, () -> groupBuyService.createActivity(activity));
    }

    @Test
    @DisplayName("创建拼团：成团人数至少2人")
    void testCreateActivity_GroupSizeInvalid() {
        activity.setGroupSize(1);
        assertThrows(BusinessException.class, () -> groupBuyService.createActivity(activity));
    }

    @Test
    @DisplayName("开团：活动已结束应抛异常")
    void testOpenGroup_ActivityExpired() {
        activity.setStatus(2); // 已结束
        when(groupBuyMapper.selectById(activity.getId())).thenReturn(activity);
        assertThrows(BusinessException.class,
                () -> groupBuyService.openGroup(1L, activity.getId()));
    }

    @Test
    @DisplayName("开团：活动未开始应抛异常")
    void testOpenGroup_ActivityNotStarted() {
        activity.setStartTime(LocalDateTime.now().plusHours(1));
        when(groupBuyMapper.selectById(activity.getId())).thenReturn(activity);
        assertThrows(BusinessException.class,
                () -> groupBuyService.openGroup(1L, activity.getId()));
    }

    @Test
    @DisplayName("开团：成功创建拼团实例")
    void testOpenGroup_Success() {
        when(groupBuyMapper.selectById(activity.getId())).thenReturn(activity);
        when(instanceMapper.insert(any(MktGroupBuyInstance.class))).thenReturn(1);
        when(memberMapper.insert(any(MktGroupBuyMember.class))).thenReturn(1);
        MktGroupBuyInstance result = groupBuyService.openGroup(1L, activity.getId());
        assertNotNull(result);
        assertEquals(0, result.getStatus());
        assertEquals(1, result.getCurrentSize());
    }

    @Test
    @DisplayName("加入团：团已满应抛异常")
    void testJoinGroup_Full() {
        MktGroupBuyInstance instance = new MktGroupBuyInstance();
        instance.setId(1L);
        instance.setCurrentSize(3);
        instance.setGroupSize(3);
        instance.setStatus(0);
        instance.setExpireTime(LocalDateTime.now().plusHours(1));
        when(instanceMapper.selectById(1L)).thenReturn(instance);
        assertThrows(BusinessException.class, () -> groupBuyService.joinGroup(1L, 1L));
    }

    @Test
    @DisplayName("加入团：团已过期应抛异常")
    void testJoinGroup_Expired() {
        MktGroupBuyInstance instance = new MktGroupBuyInstance();
        instance.setId(1L);
        instance.setCurrentSize(1);
        instance.setGroupSize(3);
        instance.setStatus(0);
        instance.setExpireTime(LocalDateTime.now().minusHours(1));
        when(instanceMapper.selectById(1L)).thenReturn(instance);
        assertThrows(BusinessException.class, () -> groupBuyService.joinGroup(1L, 1L));
    }

    @Test
    @DisplayName("拼团成功：更新状态与活动统计")
    void testOnGroupSuccess() {
        MktGroupBuyInstance instance = new MktGroupBuyInstance();
        instance.setId(1L);
        instance.setActivityId(activity.getId());
        instance.setStatus(0);
        instance.setCurrentSize(3);
        when(instanceMapper.selectById(1L)).thenReturn(instance);
        when(groupBuyMapper.selectById(activity.getId())).thenReturn(activity);
        groupBuyService.onGroupSuccess(1L);
        assertEquals(1, instance.getStatus());
        verify(groupBuyMapper).updateById(any(MktGroupBuy.class));
    }

    @Test
    @DisplayName("扫描过期团：返回处理数量")
    void testScanExpiredGroups() {
        MktGroupBuyInstance expired = new MktGroupBuyInstance();
        expired.setId(1L);
        expired.setActivityId(activity.getId());
        expired.setStatus(0);
        when(instanceMapper.selectExpiredInstances()).thenReturn(Collections.singletonList(expired));
        when(instanceMapper.selectById(1L)).thenReturn(expired);
        when(memberMapper.selectList(any())).thenReturn(Collections.emptyList());
        int count = groupBuyService.scanExpiredGroups();
        assertEquals(1, count);
    }
}
