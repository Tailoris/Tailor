package com.tailoris.copyright.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.copyright.dto.AuthorizationRequest;
import com.tailoris.copyright.dto.CopyrightRegisterRequest;
import com.tailoris.copyright.dto.CopyrightRegisterResponse;
import com.tailoris.copyright.dto.CopyrightVerifyRequest;
import com.tailoris.copyright.dto.CopyrightVerifyResponse;
import com.tailoris.copyright.entity.CopyrightAuthorization;
import com.tailoris.copyright.entity.CopyrightRecord;
import com.tailoris.copyright.mapper.CopyrightAuthorizationMapper;
import com.tailoris.copyright.mapper.CopyrightRecordMapper;
import com.tailoris.copyright.blockchain.BlockchainClient;
import com.tailoris.copyright.blockchain.BlockchainClientRouter;
import com.tailoris.copyright.blockchain.CertificatePdfGenerator;
import com.tailoris.copyright.entity.CrCertificateFile;
import com.tailoris.copyright.mapper.CrCertificateFileMapper;
import com.tailoris.copyright.service.SimilarityCheckService;
import com.tailoris.copyright.service.SimilarityCheckService.SimilarityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CopyrightServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class CopyrightServiceImplTest {

    @Mock
    private CopyrightRecordMapper copyrightRecordMapper;

    @Mock
    private CopyrightAuthorizationMapper copyrightAuthorizationMapper;

    @Mock
    private CrCertificateFileMapper certificateFileMapper;

    @Mock
    private BlockchainClientRouter blockchainRouter;

    @Mock
    private BlockchainClient blockchainClient;

    @Mock
    private CertificatePdfGenerator certificateGenerator;

    @Mock
    private SimilarityCheckService similarityCheckService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CopyrightServiceImpl copyrightService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("注册版权成功 — 返回证书信息")
    void testRegister_Success() {
        CopyrightRegisterRequest request = new CopyrightRegisterRequest();
        request.setWorkName("测试设计");
        request.setWorkType(1);
        request.setFileHash("testhashcontent");
        request.setFileType("PNG");
        request.setFileSize(1024L);
        request.setFileUrl("http://example.com/design.png");

        SimilarityResult simResult = new SimilarityResult();
        simResult.setScore(10.0);
        simResult.setMethod("pHash");
        when(similarityCheckService.preCheck(anyString(), anyString(), anyString())).thenReturn(simResult);
        when(blockchainRouter.defaultClient()).thenReturn(blockchainClient);
        // 上链失败走 catch 分支
        lenient().when(blockchainClient.submitEvidence(any())).thenThrow(new RuntimeException("chain error"));
        lenient().when(certificateGenerator.sign(anyString(), anyString())).thenReturn("sig");

        CopyrightRegisterResponse response = copyrightService.register(request);

        assertNotNull(response);
        assertTrue(response.getFileHash().length() > 0);
        assertNotNull(response.getWorkTitle());
        assertEquals("测试设计", response.getWorkTitle());
    }

    @Test
    @DisplayName("重复文件哈希拒绝注册")
    void testRegister_DuplicateHash() {
        CopyrightRegisterRequest request = new CopyrightRegisterRequest();
        request.setWorkName("重复设计");
        request.setWorkType(2);
        request.setFileHash("samehashcontent");
        request.setFileType("JPG");
        request.setFileSize(2048L);
        request.setFileUrl("http://example.com/duplicate.jpg");

        when(copyrightRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () ->
                copyrightService.register(request));
        verify(copyrightRecordMapper, never()).insert(any(CopyrightRecord.class));
    }

    @Test
    @DisplayName("验证版权存证成功 — 匹配到已有记录")
    void testVerify_Matched() {
        CopyrightVerifyRequest request = new CopyrightVerifyRequest();
        request.setFileHash("existinghash");

        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setFileHash("existinghash");
        record.setUserId(100L);
        record.setWorkName("已存证作品");
        record.setBlockchainCertNo("CR-ABC123DEF456");
        record.setRegisteredAt(java.time.LocalDateTime.now());

        when(copyrightRecordMapper.selectByHash("existinghash")).thenReturn(record);

        CopyrightVerifyResponse response = copyrightService.verify(request);

        assertNotNull(response);
        assertTrue(response.isMatched());
        assertNotNull(response.getCertificateNo());
        assertEquals("CR-ABC123DEF456", response.getCertificateNo());
        assertEquals("已存证作品", response.getWorkTitle());
        assertEquals(String.valueOf(100L), response.getAuthorName());
    }

    @Test
    @DisplayName("验证未知文件版权 — 未匹配")
    void testVerify_NotMatched() {
        CopyrightVerifyRequest request = new CopyrightVerifyRequest();
        request.setFileHash("unknownhash");

        when(copyrightRecordMapper.selectByHash("unknownhash")).thenReturn(null);

        CopyrightVerifyResponse response = copyrightService.verify(request);

        assertNotNull(response);
        assertFalse(response.isMatched());
        assertNull(response.getCertificateNo());
    }

    @Test
    @DisplayName("版权授权成功 — 生成LIC号")
    void testAuthorize_Success() {
        CopyrightRecord copyright = new CopyrightRecord();
        copyright.setId(100L);
        copyright.setUserId(200L);
        copyright.setStatus(1);

        AuthorizationRequest request = new AuthorizationRequest();
        request.setCopyrightId(100L);
        request.setLicenseeId(300L);
        request.setLicenseType(1);
        request.setScope(1);
        request.setAuthorizedProducts("服装");
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2027, 5, 31));
        request.setLicenseFee(new BigDecimal("5000.00"));
        request.setRoyaltyRate(new BigDecimal("0.05"));

        when(copyrightRecordMapper.selectById(100L)).thenReturn(copyright);
        when(copyrightAuthorizationMapper.insert(any(CopyrightAuthorization.class))).thenReturn(1);

        CopyrightAuthorization result = copyrightService.authorize(200L, request);

        assertNotNull(result);
        assertTrue(result.getLicenseNo().startsWith("LIC"));
        assertEquals(100L, result.getCopyrightId());
        assertEquals(200L, result.getLicensorId());
        assertEquals(300L, result.getLicenseeId());
        assertEquals(1, result.getStatus());
    }

    @Test
    @DisplayName("版权未存证时禁止授权")
    void testAuthorize_NotCertified() {
        CopyrightRecord copyright = new CopyrightRecord();
        copyright.setId(100L);
        copyright.setStatus(0);

        AuthorizationRequest request = new AuthorizationRequest();
        request.setCopyrightId(100L);
        request.setLicenseeId(300L);
        request.setLicenseType(1);
        request.setScope(1);
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2027, 5, 31));

        when(copyrightRecordMapper.selectById(100L)).thenReturn(copyright);

        assertThrows(BusinessException.class, () ->
                copyrightService.authorize(200L, request));
        verify(copyrightAuthorizationMapper, never()).insert(any(com.tailoris.copyright.entity.CopyrightAuthorization.class));
    }

    @Test
    @DisplayName("获取版权详情成功")
    void testGetCopyrightDetail_Success() {
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setWorkName("测试作品");
        record.setFileHash("testhash");

        when(copyrightRecordMapper.selectById(1L)).thenReturn(record);

        CopyrightRecord result = copyrightService.getCopyrightDetail(1L);

        assertNotNull(result);
        assertEquals("测试作品", result.getWorkName());
        assertEquals("testhash", result.getFileHash());
    }

    @Test
    @DisplayName("获取不存在版权抛异常")
    void testGetCopyrightDetail_NotFound() {
        when(copyrightRecordMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                copyrightService.getCopyrightDetail(99L));
    }
}
