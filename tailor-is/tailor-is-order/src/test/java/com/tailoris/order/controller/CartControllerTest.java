package com.tailoris.order.controller;

import com.tailoris.common.result.Result;
import com.tailoris.order.dto.CartAddRequest;
import com.tailoris.order.dto.CartUpdateRequest;
import com.tailoris.order.entity.ShoppingCart;
import com.tailoris.order.service.ShoppingCartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CartController 测试")
@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private ShoppingCartService shoppingCartService;

    @InjectMocks
    private CartController cartController;

    @Test
    @DisplayName("添加到购物车成功")
    void testAddToCart_Success() {
        Long userId = 1L;
        CartAddRequest request = new CartAddRequest();
        request.setProductId(100L);
        request.setSkuId(200L);
        request.setQuantity(2);

        doNothing().when(shoppingCartService).addToCart(eq(userId), any(CartAddRequest.class));

        Result<Void> result = cartController.addToCart(userId, request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(shoppingCartService).addToCart(eq(userId), any(CartAddRequest.class));
    }

    @Test
    @DisplayName("更新购物车商品成功")
    void testUpdateCart_Success() {
        Long userId = 1L;
        Long cartId = 10L;
        CartUpdateRequest request = new CartUpdateRequest();
        request.setQuantity(3);
        request.setChecked(1);

        doNothing().when(shoppingCartService).updateCart(eq(userId), eq(cartId), any(CartUpdateRequest.class));

        Result<Void> result = cartController.updateCart(userId, cartId, request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(shoppingCartService).updateCart(eq(userId), eq(cartId), any(CartUpdateRequest.class));
    }

    @Test
    @DisplayName("删除购物车商品成功")
    void testDeleteCartItem_Success() {
        Long userId = 1L;
        Long cartId = 10L;

        doNothing().when(shoppingCartService).deleteCartItem(userId, cartId);

        Result<Void> result = cartController.deleteCartItem(userId, cartId);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(shoppingCartService).deleteCartItem(userId, cartId);
    }

    @Test
    @DisplayName("查询购物车列表成功")
    void testListCart_Success() {
        Long userId = 1L;

        ShoppingCart cart1 = new ShoppingCart();
        cart1.setId(1L);
        cart1.setUserId(userId);
        cart1.setProductId(100L);
        cart1.setQuantity(2);

        ShoppingCart cart2 = new ShoppingCart();
        cart2.setId(2L);
        cart2.setUserId(userId);
        cart2.setProductId(101L);
        cart2.setQuantity(1);

        List<ShoppingCart> carts = Arrays.asList(cart1, cart2);
        when(shoppingCartService.listCart(userId)).thenReturn(carts);

        Result<List<ShoppingCart>> result = cartController.listCart(userId);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        verify(shoppingCartService).listCart(userId);
    }

    @Test
    @DisplayName("批量结算成功")
    void testBatchCheckout_Success() {
        Long userId = 1L;
        List<Long> cartIds = Arrays.asList(1L, 2L, 3L);

        ShoppingCart cart1 = new ShoppingCart();
        cart1.setId(1L);
        cart1.setUserId(userId);

        ShoppingCart cart2 = new ShoppingCart();
        cart2.setId(2L);
        cart2.setUserId(userId);

        List<ShoppingCart> carts = Arrays.asList(cart1, cart2);
        when(shoppingCartService.batchCheckout(userId, cartIds)).thenReturn(carts);

        Result<List<ShoppingCart>> result = cartController.batchCheckout(userId, cartIds);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        verify(shoppingCartService).batchCheckout(userId, cartIds);
    }
}
