package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.marketing.dto.MemberLevelRequest;
import com.tailoris.marketing.entity.MemberLevel;
import com.tailoris.marketing.entity.ShopMember;
import com.tailoris.marketing.mapper.MemberLevelMapper;
import com.tailoris.marketing.mapper.ShopMemberMapper;
import com.tailoris.marketing.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberLevelMapper memberLevelMapper;
    private final ShopMemberMapper shopMemberMapper;

    @Override
    @Transactional
    public void upgradeMember(Long userId) {
        throw new BusinessException("会员升级需要依赖用户累计积分，请通过积分增长自动升级");
    }

    @Override
    public List<MemberLevel> getMemberBenefits() {
        LambdaQueryWrapper<MemberLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberLevel::getStatus, 1);
        wrapper.orderByAsc(MemberLevel::getLevelValue);
        return memberLevelMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void setShopMember(MemberLevelRequest request) {
        LambdaQueryWrapper<ShopMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopMember::getUserId, request.getUserId());
        wrapper.eq(ShopMember::getShopId, request.getShopId());
        ShopMember shopMember = shopMemberMapper.selectOne(wrapper);

        if (shopMember == null) {
            shopMember = new ShopMember();
            shopMember.setId(SnowflakeIdGenerator.getInstance().nextId());
            shopMember.setUserId(request.getUserId());
            shopMember.setShopId(request.getShopId());
            shopMember.setLevel(request.getLevel() != null ? request.getLevel() : 1);
            shopMember.setMemberPriceEnabled(1);
            shopMember.setTotalConsume(BigDecimal.ZERO);
            shopMember.setOrderCount(0);
            shopMember.setPoints(0);
            shopMember.setJoinTime(LocalDateTime.now());
            shopMember.setStatus(1);
            shopMemberMapper.insert(shopMember);
        } else {
            if (request.getLevel() != null) {
                shopMember.setLevel(request.getLevel());
            }
            shopMemberMapper.updateById(shopMember);
        }
    }

    @Override
    public ShopMember getShopMember(Long userId, Long shopId) {
        LambdaQueryWrapper<ShopMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopMember::getUserId, userId);
        wrapper.eq(ShopMember::getShopId, shopId);
        return shopMemberMapper.selectOne(wrapper);
    }

    @Override
    public MemberLevel getMemberLevelByPoints(Integer points) {
        LambdaQueryWrapper<MemberLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(MemberLevel::getMinPoints, points);
        wrapper.eq(MemberLevel::getStatus, 1);
        wrapper.orderByDesc(MemberLevel::getLevelValue);
        wrapper.last("LIMIT 1");
        return memberLevelMapper.selectOne(wrapper);
    }
}
