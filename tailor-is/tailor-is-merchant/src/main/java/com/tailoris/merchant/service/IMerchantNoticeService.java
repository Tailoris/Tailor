package com.tailoris.merchant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tailoris.merchant.entity.MerchantNotice;

/**
 * Merchant notice service interface
 * <p>商家公告服务接口</p>
 *
 * @author Tailor IS Team
 * @since 2026-05-31
 */
public interface IMerchantNoticeService extends IService<MerchantNotice> {

    /**
     * Paginated notice list for current merchant
     *
     * @param pageNum page number
     * @param pageSize page size
     * @param noticeType filter by notice type
     * @return paginated notices
     */
    IPage<MerchantNotice> listNotices(Long merchantId, int pageNum, int pageSize, Integer noticeType);

    /**
     * Get unread notice count
     *
     * @param merchantId merchant ID
     * @return unread count
     */
    long countUnread(Long merchantId);

    /**
     * Mark notice as read
     *
     * @param noticeId notice ID
     * @return success
     */
    boolean markAsRead(Long noticeId);

    /**
     * Mark all notices as read
     *
     * @param merchantId merchant ID
     * @return affected count
     */
    int markAllAsRead(Long merchantId);
}
