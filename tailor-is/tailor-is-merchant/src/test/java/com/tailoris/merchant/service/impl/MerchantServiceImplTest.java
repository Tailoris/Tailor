package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.merchant.constant.MerchantConstants;
import com.tailoris.merchant.dto.MerchantApplyRequest;
import com.tailoris.merchant.dto.MerchantAuditRequest;
import com.tailoris.merchant.dto.MerchantQueryRequest;
import com.tailoris.merchant.entity.Merchant;
import com.tailoris.merchant.entity.MerchantQualification;
import com.tailoris.merchant.mapper.MerchantMapper;
import com.tailoris.merchant.mapper.MerchantQualificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("商家服务单元测试")
@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private MerchantQualificationMapper merchantQualificationMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private MerchantServiceImpl merchantService;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchant = new Merchant();
        merchant.setId(1L);
        merchant.setUserId(100L);
        merchant.setMerchantType(MerchantConstants.MERCHANT_TYPE_ENTERPRISE);
        merchant.setCompanyName("测试公司");
        merchant.setLicenseNo("91110000MA01234567");
        merchant.setContactName("张三");
        merchant.setContactPhone("13800138000");
        merchant.setStatus(MerchantConstants.MERCHANT_STATUS_NORMAL);
        merchant.setAuditStatus(MerchantConstants.MERCHANT_AUDIT_STATUS_APPROVED);
    }

    @Test
    @DisplayName("申请入驻：重复申请应抛异常")
    void testApplyJoin_DuplicateApplication() {
        MerchantApplyRequest request = new MerchantApplyRequest();
        request.setMerchantType(MerchantConstants.MERCHANT_TYPE_ENTERPRISE);
        request.setCompanyName("测试公司");

        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant);

        assertThrows(BusinessException.class, () -> merchantService.applyJoin(100L, request));
    }

    @Test
    @DisplayName("申请入驻：成功创建申请")
    void testApplyJoin_Success() {
        MerchantApplyRequest request = new MerchantApplyRequest();
        request.setMerchantType(MerchantConstants.MERCHANT_TYPE_ENTERPRISE);
        request.setCompanyName("测试公司");
        request.setLicenseNo("91110000MA01234567");
        request.setContactName("张三");
        request.setContactPhone("13800138000");

        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(merchantMapper.insert(any(Merchant.class))).thenReturn(1);

        assertDoesNotThrow(() -> merchantService.applyJoin(100L, request));
        verify(merchantMapper).insert(any(Merchant.class));
    }

    @Test
    @DisplayName("审核：商家不存在应抛异常")
    void testAudit_MerchantNotFound() {
        MerchantAuditRequest request = new MerchantAuditRequest();
        request.setMerchantId(999L);
        request.setAuditStatus(MerchantConstants.MERCHANT_AUDIT_STATUS_APPROVED);

        when(merchantMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> merchantService.audit(request));
    }

    @Test
    @DisplayName("审核通过：更新商家状态")
    void testAudit_Approve() {
        MerchantAuditRequest request = new MerchantAuditRequest();
        request.setMerchantId(1L);
        request.setAuditStatus(MerchantConstants.MERCHANT_AUDIT_STATUS_APPROVED);
        request.setAuditRemark("审核通过");

        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        assertDoesNotThrow(() -> merchantService.audit(request));
        assertEquals(MerchantConstants.MERCHANT_STATUS_NORMAL, merchant.getStatus());
        assertNotNull(merchant.getJoinTime());
    }

    @Test
    @DisplayName("审核拒绝：保持待审核状态")
    void testAudit_Reject() {
        MerchantAuditRequest request = new MerchantAuditRequest();
        request.setMerchantId(1L);
        request.setAuditStatus(MerchantConstants.MERCHANT_AUDIT_STATUS_REJECTED);
        request.setAuditRemark("资料不全");

        merchant.setStatus(MerchantConstants.MERCHANT_STATUS_PENDING);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        assertDoesNotThrow(() -> merchantService.audit(request));
        assertEquals(MerchantConstants.MERCHANT_STATUS_PENDING, merchant.getStatus());
    }

    @Test
    @DisplayName("获取商家信息：缓存命中")
    void testGetMerchantInfo_CacheHit() throws Exception {
        String cachedJson = "{\"id\":1,\"userId\":100}";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(cachedJson);
        when(objectMapper.readValue(cachedJson, Merchant.class)).thenReturn(merchant);

        Merchant result = merchantService.getMerchantInfo(100L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(merchantMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("获取商家信息：缓存未命中")
    void testGetMerchantInfo_CacheMiss() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        Merchant result = merchantService.getMerchantInfo(100L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("通过用户ID获取商家：不存在返回null")
    void testGetMerchantByUserId_NotFound() {
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Merchant result = merchantService.getMerchantByUserId(999L);

        assertNull(result);
    }
}
