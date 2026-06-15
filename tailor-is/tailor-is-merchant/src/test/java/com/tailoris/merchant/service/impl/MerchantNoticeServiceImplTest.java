package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.merchant.entity.MerchantNotice;
import com.tailoris.merchant.mapper.MerchantNoticeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("商家公告服务单元测试")
@ExtendWith(MockitoExtension.class)
class MerchantNoticeServiceImplTest {

    @Mock
    private MerchantNoticeMapper merchantNoticeMapper;

    @InjectMocks
    private MerchantNoticeServiceImpl merchantNoticeService;

    private MerchantNotice notice;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(merchantNoticeService, "baseMapper", merchantNoticeMapper);
        notice = new MerchantNotice();
        notice.setId(1L);
        notice.setTitle("系统公告");
        notice.setContent("系统将于今晚维护");
        notice.setNoticeType(1);
        notice.setPriority(1);
        notice.setStatus(1);
        notice.setPublishTime(LocalDateTime.now().minusHours(1));
        notice.setReadStatus(false);
    }

    @Test
    @DisplayName("列出公告：成功返回分页结果")
    void testListNotices_Success() {
        Page<MerchantNotice> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(notice));
        page.setTotal(1);

        when(merchantNoticeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        IPage<MerchantNotice> result = merchantNoticeService.listNotices(100L, 1, 10, null);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("列出公告：按类型筛选")
    void testListNotices_FilterByType() {
        Page<MerchantNotice> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(notice));
        page.setTotal(1);

        when(merchantNoticeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        IPage<MerchantNotice> result = merchantNoticeService.listNotices(100L, 1, 10, 1);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("统计未读数量：成功返回")
    void testCountUnread_Success() {
        when(merchantNoticeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

        long result = merchantNoticeService.countUnread(100L);

        assertEquals(5L, result);
    }

    @Test
    @DisplayName("标记已读：公告不存在返回false")
    void testMarkAsRead_NotFound() {
        when(merchantNoticeMapper.selectById(999L)).thenReturn(null);

        boolean result = merchantNoticeService.markAsRead(999L);

        assertFalse(result);
    }

    @Test
    @DisplayName("标记已读：成功标记")
    void testMarkAsRead_Success() {
        when(merchantNoticeMapper.selectById(1L)).thenReturn(notice);
        when(merchantNoticeMapper.updateById(any(MerchantNotice.class))).thenReturn(1);

        boolean result = merchantNoticeService.markAsRead(1L);

        assertTrue(result);
        assertTrue(notice.getReadStatus());
        assertNotNull(notice.getReadTime());
    }

    @Test
    @DisplayName("全部标记已读：成功返回影响数量")
    void testMarkAllAsRead_Success() {
        when(merchantNoticeMapper.update(any(MerchantNotice.class), any(LambdaQueryWrapper.class)))
                .thenReturn(10);

        int result = merchantNoticeService.markAllAsRead(100L);

        assertEquals(10, result);
    }
}
