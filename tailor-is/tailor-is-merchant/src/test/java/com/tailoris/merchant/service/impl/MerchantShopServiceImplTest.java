package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.merchant.constant.MerchantConstants;
import com.tailoris.merchant.dto.ShopDecorationRequest;
import com.tailoris.merchant.dto.ShopUpdateRequest;
import com.tailoris.merchant.entity.MerchantShop;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantShopMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("商家店铺服务单元测试")
@ExtendWith(MockitoExtension.class)
class MerchantShopServiceImplTest {

    @Mock
    private MerchantShopMapper merchantShopMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private MerchantShopServiceImpl merchantShopService;

    private MerchantShop shop;

    @BeforeEach
    void setUp() {
        shop = new MerchantShop();
        shop.setId(1L);
        shop.setMerchantId(100L);
        shop.setShopName("测试店铺");
        shop.setShopLogo("https://example.com/logo.png");
        shop.setShopStatus(MerchantConstants.SHOP_STATUS_OPEN);
        shop.setShopRating(5.0);
        shop.setFollowerCount(100);
        shop.setProductCount(50);
        shop.setSalesCount(1000);
    }

    @Test
    @DisplayName("创建店铺：成功创建")
    void testCreateShop_Success() {
        ShopUpdateRequest request = new ShopUpdateRequest();
        request.setShopName("新店铺");
        request.setShopLogo("https://example.com/logo.png");

        when(merchantShopMapper.insert(any(MerchantShop.class))).thenReturn(1);

        MerchantShop result = merchantShopService.createShop(100L, request);

        assertNotNull(result);
        assertEquals("新店铺", result.getShopName());
        assertEquals(MerchantConstants.SHOP_STATUS_DECORATING, result.getShopStatus());
        verify(merchantShopMapper).insert(any(MerchantShop.class));
    }

    @Test
    @DisplayName("更新店铺：店铺不存在应抛异常")
    void testUpdateShop_NotFound() {
        ShopUpdateRequest request = new ShopUpdateRequest();
        when(merchantShopMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> merchantShopService.updateShop(100L, 999L, request));
    }

    @Test
    @DisplayName("更新店铺：无权操作应抛异常")
    void testUpdateShop_Unauthorized() {
        ShopUpdateRequest request = new ShopUpdateRequest();
        shop.setMerchantId(200L);
        when(merchantShopMapper.selectById(1L)).thenReturn(shop);

        assertThrows(BusinessException.class, () -> merchantShopService.updateShop(100L, 1L, request));
    }

    @Test
    @DisplayName("更新店铺：成功更新")
    void testUpdateShop_Success() {
        ShopUpdateRequest request = new ShopUpdateRequest();
        request.setShopName("更新后的店铺");

        when(merchantShopMapper.selectById(1L)).thenReturn(shop);
        when(merchantShopMapper.updateById(any(MerchantShop.class))).thenReturn(1);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        assertDoesNotThrow(() -> merchantShopService.updateShop(100L, 1L, request));
        assertEquals("更新后的店铺", shop.getShopName());
    }

    @Test
    @DisplayName("更新店铺状态：成功更新")
    void testUpdateShopStatus_Success() {
        when(merchantShopMapper.selectById(1L)).thenReturn(shop);
        when(merchantShopMapper.updateById(any(MerchantShop.class))).thenReturn(1);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        assertDoesNotThrow(() -> merchantShopService.updateShopStatus(100L, 1L, MerchantConstants.SHOP_STATUS_SUSPENDED));
        assertEquals(MerchantConstants.SHOP_STATUS_SUSPENDED, shop.getShopStatus());
    }

    @Test
    @DisplayName("获取店铺信息：缓存命中")
    void testGetShopInfo_CacheHit() throws Exception {
        String cachedJson = "{\"id\":1,\"shopName\":\"测试店铺\"}";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(cachedJson);
        when(objectMapper.readValue(cachedJson, MerchantShop.class)).thenReturn(shop);

        MerchantShop result = merchantShopService.getShopInfo(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(merchantShopMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("获取店铺信息：缓存未命中")
    void testGetShopInfo_CacheMiss() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        when(merchantShopMapper.selectById(1L)).thenReturn(shop);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        MerchantShop result = merchantShopService.getShopInfo(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("列出商家店铺：成功返回")
    void testListShopsByMerchant_Success() {
        when(merchantShopMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(shop));

        List<MerchantShop> result = merchantShopService.listShopsByMerchant(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("保存装修：店铺不存在应抛异常")
    void testSaveDecoration_ShopNotFound() {
        ShopDecorationRequest request = new ShopDecorationRequest();
        request.setShopId(999L);
        when(merchantShopMapper.selectById(999L)).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> merchantShopService.saveDecoration(100L, request));
    }

    @Test
    @DisplayName("保存装修：主题色格式错误应抛异常")
    void testSaveDecoration_InvalidTheme() {
        ShopDecorationRequest request = new ShopDecorationRequest();
        request.setShopId(1L);
        request.setShopTheme("invalid-color");
        when(merchantShopMapper.selectById(1L)).thenReturn(shop);

        assertThrows(MerchantBusinessException.class, () -> merchantShopService.saveDecoration(100L, request));
    }

    @Test
    @DisplayName("保存装修：成功保存")
    void testSaveDecoration_Success() throws JsonProcessingException {
        ShopDecorationRequest request = new ShopDecorationRequest();
        request.setShopId(1L);
        request.setShopTheme("#FF5733");
        request.setShopLogo("https://example.com/new-logo.png");

        when(merchantShopMapper.selectById(1L)).thenReturn(shop);
        when(merchantShopMapper.updateById(any(MerchantShop.class))).thenReturn(1);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        MerchantShop result = merchantShopService.saveDecoration(100L, request);

        assertNotNull(result);
        assertEquals("#FF5733", result.getShopTheme());
    }

    @Test
    @DisplayName("获取装修：店铺不存在返回空Map")
    void testGetDecoration_ShopNotFound() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        when(merchantShopMapper.selectById(999L)).thenReturn(null);

        Map<String, Object> result = merchantShopService.getDecoration(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("预览装修：包含预览模式标记")
    void testPreviewDecoration_HasPreviewMode() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        when(merchantShopMapper.selectById(1L)).thenReturn(shop);

        Map<String, Object> result = merchantShopService.previewDecoration(1L);

        assertNotNull(result);
        assertTrue((Boolean) result.get("previewMode"));
    }
}
