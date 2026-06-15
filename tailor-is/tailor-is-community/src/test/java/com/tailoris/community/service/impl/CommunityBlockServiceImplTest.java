package com.tailoris.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.community.entity.CommunityBlock;
import com.tailoris.community.mapper.CommunityBlockMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CommunityBlockServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityBlockServiceImplTest {

    @Mock
    private CommunityBlockMapper blockMapper;

    @InjectMocks
    private CommunityBlockServiceImpl blockService;

    @Test
    @DisplayName("屏蔽用户 - 成功")
    void testBlockUser_Success() {
        when(blockMapper.existsBlock(1L, 2L)).thenReturn(0L);
        doReturn(1).when(blockMapper).insert(any(CommunityBlock.class));

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            blockService.blockUser(1L, 2L, "骚扰");

            verify(blockMapper).insert(any(CommunityBlock.class));
        }
    }

    @Test
    @DisplayName("屏蔽用户 - 已屏蔽则跳过")
    void testBlockUser_AlreadyBlocked() {
        when(blockMapper.existsBlock(1L, 2L)).thenReturn(1L);

        blockService.blockUser(1L, 2L, "骚扰");

        verify(blockMapper, never()).insert(any(CommunityBlock.class));
    }

    @Test
    @DisplayName("屏蔽自己抛异常")
    void testBlockUser_Self() {
        assertThrows(BusinessException.class, () -> blockService.blockUser(1L, 1L, "test"));
    }

    @Test
    @DisplayName("屏蔽参数为空抛异常")
    void testBlockUser_NullParams() {
        assertThrows(BusinessException.class, () -> blockService.blockUser(null, 2L, "test"));
        assertThrows(BusinessException.class, () -> blockService.blockUser(1L, null, "test"));
    }

    @Test
    @DisplayName("取消屏蔽用户")
    void testUnblockUser() {
        when(blockMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        blockService.unblockUser(1L, 2L);

        verify(blockMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("屏蔽列表")
    void testListBlocked() {
        CommunityBlock block = new CommunityBlock();
        block.setBlockedUserId(2L);
        when(blockMapper.selectBlockListByUser(1L)).thenReturn(Arrays.asList(block));

        List<CommunityBlock> result = blockService.listBlocked(1L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("是否屏蔽 - 已屏蔽")
    void testIsBlocked_True() {
        when(blockMapper.existsBlock(1L, 2L)).thenReturn(1L);
        assertTrue(blockService.isBlocked(1L, 2L));
    }

    @Test
    @DisplayName("是否屏蔽 - 未屏蔽")
    void testIsBlocked_False() {
        when(blockMapper.existsBlock(1L, 2L)).thenReturn(0L);
        assertFalse(blockService.isBlocked(1L, 2L));
    }

    @Test
    @DisplayName("是否屏蔽 - 参数为空返回false")
    void testIsBlocked_NullParams() {
        assertFalse(blockService.isBlocked(null, 2L));
        assertFalse(blockService.isBlocked(1L, null));
    }
}
