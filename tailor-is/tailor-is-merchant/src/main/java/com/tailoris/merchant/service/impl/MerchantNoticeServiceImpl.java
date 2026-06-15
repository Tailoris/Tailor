package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.merchant.entity.MerchantNotice;
import com.tailoris.merchant.mapper.MerchantNoticeMapper;
import com.tailoris.merchant.service.IMerchantNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Merchant notice service implementation
 * <p>商家公告服务实现类</p>
 *
 * @author Tailor IS Team
 * @since 2026-05-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantNoticeServiceImpl extends ServiceImpl<MerchantNoticeMapper, MerchantNotice>
        implements IMerchantNoticeService {

    @Override
    public IPage<MerchantNotice> listNotices(Long merchantId, int pageNum, int pageSize, Integer noticeType) {
        LambdaQueryWrapper<MerchantNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantNotice::getStatus, 1)
               .le(MerchantNotice::getPublishTime, LocalDateTime.now())
               .orderByDesc(MerchantNotice::getPriority)
               .orderByDesc(MerchantNotice::getPublishTime);

        if (noticeType != null) {
            wrapper.eq(MerchantNotice::getNoticeType, noticeType);
        }

        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public long countUnread(Long merchantId) {
        LambdaQueryWrapper<MerchantNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantNotice::getStatus, 1)
               .le(MerchantNotice::getPublishTime, LocalDateTime.now())
               .and(w -> w.isNull(MerchantNotice::getReadStatus)
                          .or()
                          .eq(MerchantNotice::getReadStatus, false));
        return count(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Long noticeId) {
        MerchantNotice notice = getById(noticeId);
        if (notice == null) {
            log.warn("Notice not found: {}", noticeId);
            return false;
        }

        notice.setReadStatus(true);
        notice.setReadTime(LocalDateTime.now());
        return updateById(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllAsRead(Long merchantId) {
        LambdaQueryWrapper<MerchantNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantNotice::getStatus, 1)
               .and(w -> w.isNull(MerchantNotice::getReadStatus)
                          .or()
                          .eq(MerchantNotice::getReadStatus, false));

        MerchantNotice update = new MerchantNotice();
        update.setReadStatus(true);
        update.setReadTime(LocalDateTime.now());

        int count = baseMapper.update(update, wrapper);
        log.info("Merchant {} marked {} notices as read", merchantId, count);
        return count;
    }
}
