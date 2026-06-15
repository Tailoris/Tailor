package com.tailoris.supply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.supply.dto.ContactRequest;
import com.tailoris.supply.entity.SupplyContactRecord;
import com.tailoris.supply.entity.SupplyDemandPost;
import com.tailoris.supply.mapper.SupplyContactRecordMapper;
import com.tailoris.supply.mapper.SupplyDemandPostMapper;
import com.tailoris.supply.service.SupplyContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplyContactServiceImpl implements SupplyContactService {

    private final SupplyContactRecordMapper supplyContactRecordMapper;
    private final SupplyDemandPostMapper supplyDemandPostMapper;

    @Override
    @Transactional
    public SupplyContactRecord createContact(Long userId, ContactRequest request) {
        SupplyDemandPost post = supplyDemandPostMapper.selectById(request.getPostId());
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        SupplyContactRecord record = new SupplyContactRecord();
        record.setId(SnowflakeIdGenerator.getInstance().nextId());
        record.setPostId(request.getPostId());
        record.setFromUserId(userId);
        record.setToUserId(post.getUserId());
        record.setMessage(request.getMessage());
        record.setContactMethod(request.getContactMethod());
        record.setStatus(0);
        record.setIsRead(0);
        supplyContactRecordMapper.insert(record);

        post.setContactCount(post.getContactCount() + 1);
        supplyDemandPostMapper.updateById(post);

        return record;
    }

    @Override
    public PageResponse<SupplyContactRecord> listContacts(Long userId, PageRequest pageRequest) {
        Page<SupplyContactRecord> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<SupplyContactRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SupplyContactRecord::getFromUserId, userId).or().eq(SupplyContactRecord::getToUserId, userId));
        wrapper.orderByDesc(SupplyContactRecord::getCreateTime);
        Page<SupplyContactRecord> result = supplyContactRecordMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    @Transactional
    public void respondContact(Long contactId, Long userId, String replyMessage) {
        SupplyContactRecord record = supplyContactRecordMapper.selectById(contactId);
        if (record == null) {
            throw new BusinessException("联系记录不存在");
        }
        if (!record.getToUserId().equals(userId)) {
            throw new BusinessException("无权回复该联系");
        }
        record.setReplyMessage(replyMessage);
        record.setReplyTime(LocalDateTime.now());
        record.setStatus(1);
        supplyContactRecordMapper.updateById(record);
    }
}
