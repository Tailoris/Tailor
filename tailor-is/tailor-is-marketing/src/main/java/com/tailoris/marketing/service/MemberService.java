package com.tailoris.marketing.service;

import com.tailoris.marketing.dto.MemberLevelRequest;
import com.tailoris.marketing.entity.MemberLevel;
import com.tailoris.marketing.entity.ShopMember;

import java.util.List;

/**
 * 会员服务接口.
 *
 * <p>提供会员等级查询、升级、店铺会员管理等核心功能。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface MemberService {

    /**
     * 用户升级.
     *
     * <p>根据用户当前积分自动计算并升级会员等级。</p>
     *
     * @param userId 用户ID
     */
    void upgradeMember(Long userId);

    /**
     * 查询会员等级体系.
     *
     * @return 所有会员等级及对应权益列表
     */
    List<MemberLevel> getMemberBenefits();

    /**
     * 设置店铺会员.
     *
     * @param request 店铺会员设置请求
     */
    void setShopMember(MemberLevelRequest request);

    /**
     * 查询店铺会员信息.
     *
     * @param userId 用户ID
     * @param shopId 店铺ID
     * @return 用户在指定店铺的会员信息
     */
    ShopMember getShopMember(Long userId, Long shopId);

    /**
     * 根据积分查询会员等级.
     *
     * @param points 积分值
     * @return 对应的会员等级
     */
    MemberLevel getMemberLevelByPoints(Integer points);
}
