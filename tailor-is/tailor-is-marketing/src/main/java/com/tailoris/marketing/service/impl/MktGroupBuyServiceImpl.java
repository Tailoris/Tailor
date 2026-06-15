package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
import com.tailoris.marketing.entity.MktGroupBuy;
import com.tailoris.marketing.entity.MktGroupBuyInstance;
import com.tailoris.marketing.entity.MktGroupBuyMember;
import com.tailoris.marketing.mapper.MktGroupBuyInstanceMapper;
import com.tailoris.marketing.mapper.MktGroupBuyMapper;
import com.tailoris.marketing.mapper.MktGroupBuyMemberMapper;
import com.tailoris.marketing.service.MktGroupBuyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 拼团活动 Service 实现
 * 任务编号: MKT-002
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MktGroupBuyServiceImpl implements MktGroupBuyService {

    private final MktGroupBuyMapper groupBuyMapper;
    private final MktGroupBuyInstanceMapper instanceMapper;
    private final MktGroupBuyMemberMapper memberMapper;
    private final SpringSnowflakeIdGenerator snowflakeIdGenerator;

    private static final String STOCK_KEY_PREFIX = "mkt:group:stock:";

    @Override
    @Transactional
    public MktGroupBuy createActivity(MktGroupBuy activity) {
        if (activity.getStartTime() == null || activity.getEndTime() == null) {
            throw new BusinessException("活动时间不能为空");
        }
        if (activity.getEndTime().isBefore(activity.getStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        if (activity.getGroupSize() == null || activity.getGroupSize() < 2) {
            throw new BusinessException("成团人数至少2人");
        }
        if (activity.getGroupPrice() == null
                || activity.getOriginalPrice() == null
                || activity.getGroupPrice().compareTo(activity.getOriginalPrice()) > 0) {
            throw new BusinessException("拼团价必须小于原价");
        }
        activity.setId(snowflakeIdGenerator.nextId());
        activity.setStatus(0);
        activity.setSoldCount(0);
        activity.setGroupCount(0);
        activity.setSuccessCount(0);
        groupBuyMapper.insert(activity);
        log.info("创建拼团活动成功: id={}, name={}", activity.getId(), activity.getActivityName());
        return activity;
    }

    @Override
    @Transactional
    public MktGroupBuy updateActivity(MktGroupBuy activity) {
        MktGroupBuy existing = groupBuyMapper.selectById(activity.getId());
        if (existing == null) {
            throw new BusinessException("活动不存在");
        }
        if (existing.getStatus() != null && existing.getStatus() == 1) {
            throw new BusinessException("进行中的活动不能修改");
        }
        activity.setSoldCount(existing.getSoldCount());
        activity.setGroupCount(existing.getGroupCount());
        activity.setSuccessCount(existing.getSuccessCount());
        groupBuyMapper.updateById(activity);
        return activity;
    }

    @Override
    @Transactional
    public void cancelActivity(Long activityId) {
        MktGroupBuy activity = groupBuyMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        activity.setStatus(3);
        groupBuyMapper.updateById(activity);
        log.info("取消拼团活动: id={}", activityId);
    }

    @Override
    public MktGroupBuy getActivityDetail(Long activityId) {
        return groupBuyMapper.selectById(activityId);
    }

    @Override
    public PageResponse<MktGroupBuy> listActivities(PageRequest pageRequest, Long shopId, Integer status) {
        Page<MktGroupBuy> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<MktGroupBuy> wrapper = new LambdaQueryWrapper<>();
        if (shopId != null) {
            wrapper.eq(MktGroupBuy::getShopId, shopId);
        }
        if (status != null) {
            wrapper.eq(MktGroupBuy::getStatus, status);
        }
        wrapper.orderByDesc(MktGroupBuy::getCreateTime);
        Page<MktGroupBuy> result = groupBuyMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    @Transactional
    public MktGroupBuyInstance openGroup(Long userId, Long activityId) {
        MktGroupBuy activity = groupBuyMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (activity.getStatus() != 1) {
            throw new BusinessException("活动未在进行中");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException("活动不在有效期内");
        }
        if (activity.getTotalStock() != null && activity.getTotalStock() > 0
                && activity.getSoldCount() != null
                && activity.getSoldCount() >= activity.getTotalStock()) {
            throw new BusinessException("活动库存不足");
        }

        // 限购校验
        if (activity.getLimitPerUser() != null && activity.getLimitPerUser() > 0) {
            // M-001 修复: 使用单次 JOIN 查询替代循环中逐条 selectCount (N+1)
            Long count = instanceMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MktGroupBuyInstance>()
                            .eq(MktGroupBuyInstance::getActivityId, activityId)
                            .inSql(MktGroupBuyInstance::getId,
                                    "SELECT instance_id FROM mkt_group_buy_member " +
                                    "WHERE user_id = " + userId + " AND status = 1"));
            if (count != null && count >= activity.getLimitPerUser()) {
                throw new BusinessException("已到达活动限购数");
            }
        }

        MktGroupBuyInstance instance = new MktGroupBuyInstance();
        instance.setId(snowflakeIdGenerator.nextId());
        instance.setActivityId(activityId);
        instance.setGroupNo("GB" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        instance.setLeaderUserId(userId);
        instance.setCurrentSize(1);
        instance.setGroupSize(activity.getGroupSize());
        instance.setStatus(0);
        instance.setExpireTime(now.plusHours(activity.getValidHours() != null ? activity.getValidHours() : 24));
        instanceMapper.insert(instance);

        MktGroupBuyMember member = new MktGroupBuyMember();
        member.setId(snowflakeIdGenerator.nextId());
        member.setInstanceId(instance.getId());
        member.setUserId(userId);
        member.setIsLeader(1);
        member.setJoinTime(now);
        member.setStatus(0);
        memberMapper.insert(member);

        // 更新活动开团数
        activity.setGroupCount(activity.getGroupCount() == null ? 1 : activity.getGroupCount() + 1);
        groupBuyMapper.updateById(activity);

        log.info("开团成功: user={}, activity={}, instance={}", userId, activityId, instance.getId());
        return instance;
    }

    @Override
    @Transactional
    public MktGroupBuyInstance joinGroup(Long userId, Long instanceId) {
        MktGroupBuyInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("团不存在");
        }
        if (instance.getStatus() != 0) {
            throw new BusinessException("团不在进行中");
        }
        if (instance.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("团已过期");
        }
        if (instance.getCurrentSize() >= instance.getGroupSize()) {
            throw new BusinessException("团已满");
        }

        // 检查是否已加入
        Long exists = memberMapper.selectCount(new LambdaQueryWrapper<MktGroupBuyMember>()
                .eq(MktGroupBuyMember::getInstanceId, instanceId)
                .eq(MktGroupBuyMember::getUserId, userId));
        if (exists != null && exists > 0) {
            throw new BusinessException("已加入该团");
        }

        MktGroupBuyMember member = new MktGroupBuyMember();
        member.setId(snowflakeIdGenerator.nextId());
        member.setInstanceId(instanceId);
        member.setUserId(userId);
        member.setIsLeader(0);
        member.setJoinTime(LocalDateTime.now());
        member.setStatus(0);
        memberMapper.insert(member);

        instance.setCurrentSize(instance.getCurrentSize() + 1);
        if (instance.getCurrentSize() >= instance.getGroupSize()) {
            instance.setStatus(1);
            instance.setCompleteTime(LocalDateTime.now());
        }
        instanceMapper.updateById(instance);
        return instance;
    }

    @Override
    public MktGroupBuyInstance getInstanceDetail(Long instanceId) {
        return instanceMapper.selectById(instanceId);
    }

    @Override
    public PageResponse<MktGroupBuyInstance> listUserInstances(Long userId, PageRequest pageRequest, Integer status) {
        Page<MktGroupBuyInstance> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        // 查询用户参与的所有团
        List<Long> instanceIds = memberMapper.selectList(
                new LambdaQueryWrapper<MktGroupBuyMember>().eq(MktGroupBuyMember::getUserId, userId))
                .stream().map(MktGroupBuyMember::getInstanceId).toList();
        if (instanceIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0L, pageRequest.getPageNum(), pageRequest.getPageSize());
        }
        LambdaQueryWrapper<MktGroupBuyInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MktGroupBuyInstance::getId, instanceIds);
        if (status != null) {
            wrapper.eq(MktGroupBuyInstance::getStatus, status);
        }
        wrapper.orderByDesc(MktGroupBuyInstance::getCreateTime);
        Page<MktGroupBuyInstance> result = instanceMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public List<MktGroupBuyInstance> listJoinableGroups(Long activityId) {
        LambdaQueryWrapper<MktGroupBuyInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MktGroupBuyInstance::getActivityId, activityId)
                .eq(MktGroupBuyInstance::getStatus, 0)
                .apply("current_size < group_size")
                .gt(MktGroupBuyInstance::getExpireTime, LocalDateTime.now())
                .orderByDesc(MktGroupBuyInstance::getCreateTime)
                .last("LIMIT 20");
        return instanceMapper.selectList(wrapper);
    }

    @Override
    public List<MktGroupBuyMember> listGroupMembers(Long instanceId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<MktGroupBuyMember>()
                        .eq(MktGroupBuyMember::getInstanceId, instanceId)
                        .orderByAsc(MktGroupBuyMember::getJoinTime));
    }

    @Override
    @Transactional
    public void onGroupSuccess(Long instanceId) {
        MktGroupBuyInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            return;
        }
        if (instance.getStatus() != 0) {
            return;
        }
        instance.setStatus(1);
        instance.setCompleteTime(LocalDateTime.now());
        instanceMapper.updateById(instance);

        MktGroupBuy activity = groupBuyMapper.selectById(instance.getActivityId());
        if (activity != null) {
            activity.setSuccessCount(activity.getSuccessCount() == null ? 1 : activity.getSuccessCount() + 1);
            int size = instance.getCurrentSize() == null ? 0 : instance.getCurrentSize();
            activity.setSoldCount(activity.getSoldCount() == null ? size : activity.getSoldCount() + size);
            groupBuyMapper.updateById(activity);
        }
        log.info("拼团成功: instanceId={}, size={}", instanceId, instance.getCurrentSize());
    }

    @Override
    @Transactional
    public void onGroupFailed(Long instanceId) {
        MktGroupBuyInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            return;
        }
        if (instance.getStatus() != 0) {
            return;
        }
        instance.setStatus(2);
        instanceMapper.updateById(instance);

        // 标记成员失败
        List<MktGroupBuyMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<MktGroupBuyMember>()
                        .eq(MktGroupBuyMember::getInstanceId, instanceId));
        // M-001 修复: 使用批量更新代替逐条 updateById
        for (MktGroupBuyMember m : members) {
            m.setStatus(3);
        }
        memberMapper.updateBatch(members);
        log.info("拼团失败: instanceId={}, 成员数={}", instanceId, members.size());
    }

    @Override
    public int scanExpiredGroups() {
        List<MktGroupBuyInstance> expired = instanceMapper.selectExpiredInstances();
        int count = 0;
        for (MktGroupBuyInstance inst : expired) {
            try {
                onGroupFailed(inst.getId());
                count++;
            } catch (Exception e) {
                log.error("处理过期团失败: instanceId={}", inst.getId(), e);
            }
        }
        if (count > 0) {
            log.info("扫描过期团完成，处理{}个", count);
        }
        return count;
    }
}
