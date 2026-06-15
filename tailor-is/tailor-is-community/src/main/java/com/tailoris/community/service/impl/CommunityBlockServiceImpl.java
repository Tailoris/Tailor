package com.tailoris.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.community.entity.CommunityBlock;
import com.tailoris.community.mapper.CommunityBlockMapper;
import com.tailoris.community.service.CommunityBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 社区屏蔽 Service 实现
 * 任务编号: COM-004
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityBlockServiceImpl implements CommunityBlockService {

    private final CommunityBlockMapper blockMapper;

    @Override
    @Transactional
    public void blockUser(Long userId, Long blockedUserId, String reason) {
        if (userId == null || blockedUserId == null) {
            throw new BusinessException("参数错误");
        }
        if (userId.equals(blockedUserId)) {
            throw new BusinessException("不能屏蔽自己");
        }
        if (blockMapper.existsBlock(userId, blockedUserId) > 0) {
            return;
        }
        CommunityBlock block = new CommunityBlock();
        block.setId(SnowflakeIdGenerator.getInstance().nextId());
        block.setUserId(userId);
        block.setBlockedUserId(blockedUserId);
        block.setReason(reason);
        blockMapper.insert(block);
        log.info("用户屏蔽: from={}, to={}", userId, blockedUserId);
    }

    @Override
    @Transactional
    public void unblockUser(Long userId, Long blockedUserId) {
        LambdaQueryWrapper<CommunityBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityBlock::getUserId, userId)
                .eq(CommunityBlock::getBlockedUserId, blockedUserId);
        blockMapper.delete(wrapper);
    }

    @Override
    public List<CommunityBlock> listBlocked(Long userId) {
        return blockMapper.selectBlockListByUser(userId);
    }

    @Override
    public boolean isBlocked(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) return false;
        return blockMapper.existsBlock(userId, targetUserId) > 0;
    }
}
