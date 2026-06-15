package com.tailoris.copyright.scheduler;

import com.tailoris.copyright.blockchain.BlockchainClient;
import com.tailoris.copyright.blockchain.BlockchainClientRouter;
import com.tailoris.copyright.entity.CopyrightRecord;
import com.tailoris.copyright.mapper.CopyrightRecordMapper;
import com.tailoris.copyright.service.HashGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BatchChainScheduler 单元测试")
@ExtendWith(MockitoExtension.class)
class BatchChainSchedulerTest {

    @Mock
    private HashGenerationService hashGenerationService;

    @Mock
    private CopyrightRecordMapper copyrightRecordMapper;

    @Mock
    private BlockchainClientRouter blockchainRouter;

    @Mock
    private BlockchainClient blockchainClient;

    @InjectMocks
    private BatchChainScheduler batchChainScheduler;

    @BeforeEach
    void setUp() throws Exception {
        setField(batchChainScheduler, "batchThreshold", 100);
        setField(batchChainScheduler, "maxBatchSize", 200);
        setField(batchChainScheduler, "batchChainEnabled", true);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("定时批量上链 - 功能未启用")
    void testScheduledBatchChain_Disabled() throws Exception {
        setField(batchChainScheduler, "batchChainEnabled", false);
        
        batchChainScheduler.scheduledBatchChain();
        
        verify(hashGenerationService, never()).getPendingCount();
    }

    @Test
    @DisplayName("定时批量上链 - 队列为空")
    void testScheduledBatchChain_EmptyQueue() {
        when(hashGenerationService.getPendingCount()).thenReturn(0L);
        
        batchChainScheduler.scheduledBatchChain();
        
        verify(hashGenerationService, times(1)).getPendingCount();
    }

    @Test
    @DisplayName("定时批量上链 - 有待处理记录但无对应版权记录")
    void testScheduledBatchChain_WithPending() {
        Set<String> hashes = new HashSet<>();
        hashes.add("hash1");
        
        // getPendingCount 在 executeBatchChain 中被多次调用
        when(hashGenerationService.getPendingCount())
                .thenReturn(5L)   // scheduledBatchChain line 63
                .thenReturn(5L)   // executeBatchChain line 93 (totalPending)
                .thenReturn(5L)   // while condition line 96
                .thenReturn(5L)   // Math.min line 98
                .thenReturn(0L);  // log line 134
        when(hashGenerationService.popPendingHashes(anyInt())).thenReturn(hashes);
        when(hashGenerationService.getCopyrightIdByHash("hash1")).thenReturn(null);
        
        batchChainScheduler.scheduledBatchChain();
        
        verify(hashGenerationService, atLeastOnce()).getPendingCount();
        // queryRecordsByHashes 中清理一次, executeBatchChain 中再清理一次
        verify(hashGenerationService, atLeast(1)).removeFromPendingQueue("hash1");
    }

    @Test
    @DisplayName("手动触发批量上链 - 功能未启用")
    void testTriggerBatchChain_Disabled() throws Exception {
        setField(batchChainScheduler, "batchChainEnabled", false);
        
        batchChainScheduler.triggerBatchChain();
        
        verify(hashGenerationService, never()).getPendingCount();
    }

    @Test
    @DisplayName("手动触发批量上链 - 未达到阈值")
    void testTriggerBatchChain_BelowThreshold() {
        when(hashGenerationService.getPendingCount()).thenReturn(50L);
        
        batchChainScheduler.triggerBatchChain();
        
        verify(hashGenerationService, times(1)).getPendingCount();
    }

    @Test
    @DisplayName("手动触发批量上链 - 达到阈值")
    void testTriggerBatchChain_AboveThreshold() {
        Set<String> hashes = new HashSet<>();
        hashes.add("hash1");
        
        when(hashGenerationService.getPendingCount())
                .thenReturn(150L) // triggerBatchChain line 80
                .thenReturn(150L) // executeBatchChain line 93
                .thenReturn(150L) // while condition line 96
                .thenReturn(150L) // Math.min line 98
                .thenReturn(0L);  // log line 134
        when(hashGenerationService.popPendingHashes(anyInt())).thenReturn(hashes);
        when(hashGenerationService.getCopyrightIdByHash("hash1")).thenReturn(null);
        
        batchChainScheduler.triggerBatchChain();
        
        verify(hashGenerationService, atLeastOnce()).getPendingCount();
        verify(hashGenerationService, atLeast(1)).removeFromPendingQueue("hash1");
    }

    @Test
    @DisplayName("执行批量上链 - 成功")
    void testExecuteBatchChain_Success() {
        Set<String> hashes = new HashSet<>();
        hashes.add("hash1");
        hashes.add("hash2");
        
        CopyrightRecord record1 = new CopyrightRecord();
        record1.setId(1L);
        record1.setUserId(100L);
        record1.setFileHash("hash1");
        record1.setAuthorRealName("作者1");
        
        CopyrightRecord record2 = new CopyrightRecord();
        record2.setId(2L);
        record2.setUserId(101L);
        record2.setFileHash("hash2");
        record2.setAuthorRealName("作者2");
        
        BlockchainClient.BlockchainSubmitResult result = new BlockchainClient.BlockchainSubmitResult();
        result.setSuccess(true);
        result.setTxHash("tx123");
        result.setBlockHeight(BigInteger.valueOf(1000L));
        result.setNode("node1");
        
        // getPendingCount 在 executeBatchChain 中被多次调用，需持续返回值
        when(hashGenerationService.getPendingCount())
                .thenReturn(2L)   // scheduledBatchChain line 63
                .thenReturn(2L)   // executeBatchChain line 93
                .thenReturn(2L)   // while condition line 96
                .thenReturn(2L)   // Math.min line 98
                .thenReturn(0L);  // log line 134
        when(hashGenerationService.popPendingHashes(anyInt())).thenReturn(hashes);
        // getCopyrightIdByHash 在 queryRecordsByHashes 和 catch 块中都可能被调用
        when(hashGenerationService.getCopyrightIdByHash("hash1")).thenReturn(1L);
        when(hashGenerationService.getCopyrightIdByHash("hash2")).thenReturn(2L);
        when(copyrightRecordMapper.selectById(1L)).thenReturn(record1);
        when(copyrightRecordMapper.selectById(2L)).thenReturn(record2);
        when(blockchainRouter.defaultClient()).thenReturn(blockchainClient);
        when(blockchainClient.submitEvidence(any())).thenReturn(result);
        when(blockchainClient.platformCode()).thenReturn("ANTCHAIN");
        
        batchChainScheduler.scheduledBatchChain();
        
        verify(copyrightRecordMapper, times(2)).updateById(any(CopyrightRecord.class));
        verify(hashGenerationService, atLeastOnce()).removeFromPendingQueue(any());
    }

    @Test
    @DisplayName("执行批量上链 - 未找到版权记录")
    void testExecuteBatchChain_NoRecordsFound() {
        Set<String> hashes = new HashSet<>();
        hashes.add("hash1");
        
        when(hashGenerationService.getPendingCount())
                .thenReturn(1L)   // scheduledBatchChain
                .thenReturn(1L)   // executeBatchChain totalPending
                .thenReturn(1L)   // while condition
                .thenReturn(1L)   // Math.min
                .thenReturn(0L);  // log
        when(hashGenerationService.popPendingHashes(anyInt())).thenReturn(hashes);
        when(hashGenerationService.getCopyrightIdByHash("hash1")).thenReturn(null);
        
        batchChainScheduler.scheduledBatchChain();
        
        // queryRecordsByHashes 中清理一次, executeBatchChain 空记录列表再清理一次
        verify(hashGenerationService, atLeast(1)).removeFromPendingQueue("hash1");
    }

    @Test
    @DisplayName("执行批量上链 - 上链失败")
    void testExecuteBatchChain_SubmitFailed() {
        Set<String> hashes = new HashSet<>();
        hashes.add("hash1");
        
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setUserId(100L);
        record.setFileHash("hash1");
        
        BlockchainClient.BlockchainSubmitResult result = new BlockchainClient.BlockchainSubmitResult();
        result.setSuccess(false);
        result.setErrorMessage("上链失败");
        
        when(hashGenerationService.getPendingCount())
                .thenReturn(1L)
                .thenReturn(1L)
                .thenReturn(1L)
                .thenReturn(1L)
                .thenReturn(0L);
        when(hashGenerationService.popPendingHashes(anyInt())).thenReturn(hashes);
        // getCopyrightIdByHash 在 queryRecordsByHashes 和 batchUploadToBlockchain 失败回退中都可能被调用
        when(hashGenerationService.getCopyrightIdByHash("hash1")).thenReturn(1L);
        when(copyrightRecordMapper.selectById(1L)).thenReturn(record);
        when(blockchainRouter.defaultClient()).thenReturn(blockchainClient);
        when(blockchainClient.submitEvidence(any())).thenReturn(result);
        
        batchChainScheduler.scheduledBatchChain();
        
        verify(copyrightRecordMapper, atLeast(1)).updateById(argThat((CopyrightRecord r) -> r.getStatus() == 3));
    }

    @Test
    @DisplayName("执行批量上链 - 上链异常")
    void testExecuteBatchChain_Exception() {
        Set<String> hashes = new HashSet<>();
        hashes.add("hash1");
        
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setUserId(100L);
        record.setFileHash("hash1");
        
        when(hashGenerationService.getPendingCount())
                .thenReturn(1L)
                .thenReturn(1L)
                .thenReturn(1L)
                .thenReturn(1L)
                .thenReturn(0L);
        when(hashGenerationService.popPendingHashes(anyInt())).thenReturn(hashes);
        // getCopyrightIdByHash 在 queryRecordsByHashes 和 catch 回退中都会被调用
        when(hashGenerationService.getCopyrightIdByHash("hash1")).thenReturn(1L);
        when(copyrightRecordMapper.selectById(1L)).thenReturn(record);
        when(blockchainRouter.defaultClient()).thenReturn(blockchainClient);
        when(blockchainClient.submitEvidence(any())).thenThrow(new RuntimeException("网络异常"));
        
        batchChainScheduler.scheduledBatchChain();
        
        // catch 块中回退到队列
        verify(hashGenerationService, atLeast(1)).addToPendingQueue(eq("hash1"), eq(1L));
    }

    @Test
    @DisplayName("查询版权记录 - 记录不存在")
    void testQueryRecordsByHashes_RecordNotFound() {
        Set<String> hashes = new HashSet<>();
        hashes.add("hash1");
        
        when(hashGenerationService.getCopyrightIdByHash("hash1")).thenReturn(1L);
        when(copyrightRecordMapper.selectById(1L)).thenReturn(null);
        
        when(hashGenerationService.getPendingCount())
                .thenReturn(1L)
                .thenReturn(1L)
                .thenReturn(1L)
                .thenReturn(1L)
                .thenReturn(0L);
        when(hashGenerationService.popPendingHashes(anyInt())).thenReturn(hashes);
        
        batchChainScheduler.scheduledBatchChain();
        
        verify(hashGenerationService, atLeast(1)).removeFromPendingQueue("hash1");
    }
}
