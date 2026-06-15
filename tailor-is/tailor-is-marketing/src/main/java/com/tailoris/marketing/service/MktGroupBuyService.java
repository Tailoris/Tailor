package com.tailoris.marketing.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.marketing.entity.MktGroupBuy;
import com.tailoris.marketing.entity.MktGroupBuyInstance;
import com.tailoris.marketing.entity.MktGroupBuyMember;

import java.util.List;

/**
 * 拼团活动 Service
 * 任务编号: MKT-002
 */
public interface MktGroupBuyService {

    /**
     * 创建拼团活动
     */
    MktGroupBuy createActivity(MktGroupBuy activity);

    /**
     * 更新活动
     */
    MktGroupBuy updateActivity(MktGroupBuy activity);

    /**
     * 取消活动
     */
    void cancelActivity(Long activityId);

    /**
     * 活动详情
     */
    MktGroupBuy getActivityDetail(Long activityId);

    /**
     * 活动列表（分页）
     */
    PageResponse<MktGroupBuy> listActivities(PageRequest pageRequest, Long shopId, Integer status);

    /**
     * 开团
     */
    MktGroupBuyInstance openGroup(Long userId, Long activityId);

    /**
     * 用户加入团
     */
    MktGroupBuyInstance joinGroup(Long userId, Long instanceId);

    /**
     * 拼团详情
     */
    MktGroupBuyInstance getInstanceDetail(Long instanceId);

    /**
     * 查询用户参与的团
     */
    PageResponse<MktGroupBuyInstance> listUserInstances(Long userId, PageRequest pageRequest, Integer status);

    /**
     * 查询可加入的团
     */
    List<MktGroupBuyInstance> listJoinableGroups(Long activityId);

    /**
     * 查询团成员
     */
    List<MktGroupBuyMember> listGroupMembers(Long instanceId);

    /**
     * 拼团成功回调
     */
    void onGroupSuccess(Long instanceId);

    /**
     * 拼团失败回调（过期）
     */
    void onGroupFailed(Long instanceId);

    /**
     * 扫描并处理过期团
     */
    int scanExpiredGroups();
}
