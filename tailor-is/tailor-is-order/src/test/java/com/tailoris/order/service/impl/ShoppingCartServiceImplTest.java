package com.tailoris.order.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.order.dto.CartAddRequest;
import com.tailoris.order.dto.CartUpdateRequest;
import com.tailoris.order.entity.ShoppingCart;
import com.tailoris.order.mapper.ShoppingCartMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ShoppingCartServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceImplTest {

    @Mock
    private ShoppingCartMapper shoppingCartMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ShoppingCartServiceImpl shoppingCartService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("添加购物车 - 新增商品成功")
    void testAddToCart_NewItem() {
        Long userId = 1L;
        CartAddRequest request = new CartAddRequest();
        request.setProductId(100L);
        request.setSkuId(200L);
        request.setQuantity(2);

        when(shoppingCartMapper.selectOne(any())).thenReturn(null);
        when(shoppingCartMapper.insert(any(ShoppingCart.class))).thenReturn(1);

        assertDoesNotThrow(() -> shoppingCartService.addToCart(userId, request));

        verify(shoppingCartMapper).insert((ShoppingCart) argThat(cart -> {
                ShoppingCart s = (ShoppingCart) cart;
                return s.getUserId().equals(userId)
                        && s.getProductId().equals(request.getProductId())
                        && s.getSkuId().equals(request.getSkuId())
                        && s.getQuantity().equals(request.getQuantity());
        }));
        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("添加购物车 - 已存在商品则追加数量")
    void testAddToCart_ExistingItem() {
        Long userId = 1L;
        CartAddRequest request = new CartAddRequest();
        request.setProductId(100L);
        request.setSkuId(200L);
        request.setQuantity(3);

        ShoppingCart existingCart = buildCartItem(10L, userId, 100L, 200L, 5);
        when(shoppingCartMapper.selectOne(any())).thenReturn(existingCart);
        when(shoppingCartMapper.updateById(any(ShoppingCart.class))).thenReturn(1);

        assertDoesNotThrow(() -> shoppingCartService.addToCart(userId, request));

        verify(shoppingCartMapper).updateById((ShoppingCart) argThat(cart -> {
                ShoppingCart s = (ShoppingCart) cart;
                return s.getQuantity().equals(8);
        }));
        verify(shoppingCartMapper, never()).insert(any(ShoppingCart.class));
    }

    @Test
    @DisplayName("更新购物车 - 修改数量和选中状态成功")
    void testUpdateCart_Success() {
        Long userId = 1L;
        Long cartId = 10L;
        CartUpdateRequest request = new CartUpdateRequest();
        request.setQuantity(10);
        request.setChecked(0);

        ShoppingCart cart = buildCartItem(cartId, userId, 100L, 200L, 3);
        when(shoppingCartMapper.selectById(cartId)).thenReturn(cart);
        when(shoppingCartMapper.updateById(any(ShoppingCart.class))).thenReturn(1);

        assertDoesNotThrow(() -> shoppingCartService.updateCart(userId, cartId, request));

        verify(shoppingCartMapper).updateById((ShoppingCart) argThat(c -> {
                ShoppingCart s = (ShoppingCart) c;
                return s.getQuantity().equals(10) && s.getChecked().equals(0);
        }));
    }

    @Test
    @DisplayName("更新购物车 - 商品不存在则抛异常")
    void testUpdateCart_NotFound() {
        Long userId = 1L;
        Long cartId = 99L;
        CartUpdateRequest request = new CartUpdateRequest();
        request.setQuantity(5);

        when(shoppingCartMapper.selectById(cartId)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shoppingCartService.updateCart(userId, cartId, request));
        assertEquals("购物车商品不存在", ex.getMessage());
    }

    @Test
    @DisplayName("删除购物车 - 移除商品成功")
    void testDeleteCartItem_Success() {
        Long userId = 1L;
        Long cartId = 10L;

        ShoppingCart cart = buildCartItem(cartId, userId, 100L, 200L, 2);
        when(shoppingCartMapper.selectById(cartId)).thenReturn(cart);
        when(shoppingCartMapper.deleteById(cartId)).thenReturn(1);

        assertDoesNotThrow(() -> shoppingCartService.deleteCartItem(userId, cartId));

        verify(shoppingCartMapper).deleteById(cartId);
        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("获取购物车列表 - 缓存未命中则查库")
    void testListCart_FromDatabase() throws JsonProcessingException {
        Long userId = 1L;
        when(valueOperations.get(anyString())).thenReturn(null);

        List<ShoppingCart> carts = Arrays.asList(
                buildCartItem(10L, userId, 100L, 200L, 3),
                buildCartItem(11L, userId, 101L, 201L, 1)
        );
        when(shoppingCartMapper.selectList(any())).thenReturn(carts);
        when(objectMapper.writeValueAsString(any())).thenReturn("[{}]");

        List<ShoppingCart> result = shoppingCartService.listCart(userId);

        assertEquals(2, result.size());
        verify(shoppingCartMapper).selectList(any());
        verify(objectMapper).writeValueAsString(any());
    }

    @Test
    @DisplayName("清空购物车 - 删除用户所有购物车商品成功")
    void testClearCart_Success() {
        Long userId = 1L;
        when(shoppingCartMapper.delete(any())).thenReturn(2);

        assertDoesNotThrow(() -> shoppingCartService.clearCart(userId));

        verify(shoppingCartMapper).delete(any());
        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("删除购物车 - 商品不存在抛出异常")
    void testDeleteCartItem_NotFound() {
        Long userId = 1L;
        Long cartId = 99L;

        when(shoppingCartMapper.selectById(cartId)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shoppingCartService.deleteCartItem(userId, cartId));
        assertEquals("购物车商品不存在", ex.getMessage());
    }

    @Test
    @DisplayName("删除购物车 - 无权操作抛出异常")
    void testDeleteCartItem_NoPermission() {
        Long userId = 1L;
        Long cartId = 10L;

        ShoppingCart cart = buildCartItem(cartId, 999L, 100L, 200L, 2);
        when(shoppingCartMapper.selectById(cartId)).thenReturn(cart);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shoppingCartService.deleteCartItem(userId, cartId));
        assertEquals("无权操作该购物车商品", ex.getMessage());
    }

    @Test
    @DisplayName("更新购物车 - 无权操作抛出异常")
    void testUpdateCart_NoPermission() {
        Long userId = 1L;
        Long cartId = 10L;
        CartUpdateRequest request = new CartUpdateRequest();
        request.setQuantity(5);

        ShoppingCart cart = buildCartItem(cartId, 999L, 100L, 200L, 3);
        when(shoppingCartMapper.selectById(cartId)).thenReturn(cart);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shoppingCartService.updateCart(userId, cartId, request));
        assertEquals("无权操作该购物车商品", ex.getMessage());
    }

    @Test
    @DisplayName("批量结算 - 选中的购物车不存在抛出异常")
    void testBatchCheckout_NotFound() {
        Long userId = 1L;
        when(shoppingCartMapper.selectList(any())).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shoppingCartService.batchCheckout(userId, Arrays.asList(1L, 2L)));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("获取购物车列表 - 缓存命中直接返回")
    void testListCart_CacheHit() throws JsonProcessingException {
        Long userId = 1L;
        List<ShoppingCart> carts = Arrays.asList(buildCartItem(10L, userId, 100L, 200L, 3));

        when(valueOperations.get(anyString())).thenReturn("[{\"id\":10}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(carts);

        List<ShoppingCart> result = shoppingCartService.listCart(userId);

        assertEquals(1, result.size());
        verify(shoppingCartMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("获取购物车列表 - 缓存反序列化失败降级查库")
    void testListCart_CacheDeserializationFailure() throws JsonProcessingException {
        Long userId = 1L;
        List<ShoppingCart> carts = Arrays.asList(buildCartItem(10L, userId, 100L, 200L, 3));

        when(valueOperations.get(anyString())).thenReturn("invalid-json");
        when(shoppingCartMapper.selectList(any())).thenReturn(carts);
        when(objectMapper.writeValueAsString(any())).thenReturn("[{}]");

        List<ShoppingCart> result = shoppingCartService.listCart(userId);

        assertEquals(1, result.size());
        verify(shoppingCartMapper).selectList(any());
    }

    private ShoppingCart buildCartItem(Long id, Long userId, Long productId,
                                       Long skuId, Integer quantity) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(id);
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setSkuId(skuId);
        cart.setQuantity(quantity);
        cart.setChecked(1);
        return cart;
    }
}