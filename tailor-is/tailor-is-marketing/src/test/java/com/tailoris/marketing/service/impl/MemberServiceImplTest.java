package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.marketing.entity.MemberLevel;
import com.tailoris.marketing.entity.ShopMember;
import com.tailoris.marketing.mapper.MemberLevelMapper;
import com.tailoris.marketing.mapper.ShopMemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("会员服务单元测试")
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberLevelMapper memberLevelMapper;

    @Mock
    private ShopMemberMapper shopMemberMapper;

    @InjectMocks
    private MemberServiceImpl memberService;

    private MemberLevel level;
    private ShopMember shopMember;

    @BeforeEach
    void setUp() {
        level = new MemberLevel();
        level.setId(1L);
        level.setLevelName("普通会员");
        level.setLevelValue(1);
        level.setMinPoints(0);
        level.setStatus(1);

        shopMember = new ShopMember();
        shopMember.setId(1L);
        shopMember.setUserId(100L);
        shopMember.setShopId(1L);
        shopMember.setLevel(1);
        shopMember.setMemberPriceEnabled(1);
        shopMember.setTotalConsume(BigDecimal.ZERO);
        shopMember.setOrderCount(0);
        shopMember.setPoints(0);
        shopMember.setStatus(1);
    }

    @Test
    @DisplayName("获取会员权益：成功返回")
    void testGetMemberBenefits_Success() {
        when(memberLevelMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(level));

        List<MemberLevel> result = memberService.getMemberBenefits();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("设置店铺会员：新增会员")
    void testSetShopMember_CreateNew() {
        com.tailoris.marketing.dto.MemberLevelRequest request =
                new com.tailoris.marketing.dto.MemberLevelRequest();
        request.setUserId(100L);
        request.setShopId(1L);
        request.setLevel(1);

        when(shopMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(shopMemberMapper.insert(any(ShopMember.class))).thenReturn(1);

        assertDoesNotThrow(() -> memberService.setShopMember(request));
        verify(shopMemberMapper).insert(any(ShopMember.class));
    }

    @Test
    @DisplayName("设置店铺会员：更新现有会员")
    void testSetShopMember_UpdateExisting() {
        com.tailoris.marketing.dto.MemberLevelRequest request =
                new com.tailoris.marketing.dto.MemberLevelRequest();
        request.setUserId(100L);
        request.setShopId(1L);
        request.setLevel(2);

        when(shopMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(shopMember);
        when(shopMemberMapper.updateById(any(ShopMember.class))).thenReturn(1);

        assertDoesNotThrow(() -> memberService.setShopMember(request));
        assertEquals(2, shopMember.getLevel());
    }

    @Test
    @DisplayName("获取店铺会员：成功返回")
    void testGetShopMember_Success() {
        when(shopMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(shopMember);

        ShopMember result = memberService.getShopMember(100L, 1L);

        assertNotNull(result);
        assertEquals(100L, result.getUserId());
        assertEquals(1L, result.getShopId());
    }

    @Test
    @DisplayName("根据积分获取会员等级：成功返回")
    void testGetMemberLevelByPoints_Success() {
        when(memberLevelMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(level);

        MemberLevel result = memberService.getMemberLevelByPoints(100);

        assertNotNull(result);
        assertEquals("普通会员", result.getLevelName());
    }
}
