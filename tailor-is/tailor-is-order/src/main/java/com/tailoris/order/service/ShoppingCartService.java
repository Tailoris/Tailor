package com.tailoris.order.service;

import com.tailoris.order.dto.CartAddRequest;
import com.tailoris.order.dto.CartUpdateRequest;
import com.tailoris.order.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    void addToCart(Long userId, CartAddRequest request);

    void updateCart(Long userId, Long cartId, CartUpdateRequest request);

    void deleteCartItem(Long userId, Long cartId);

    List<ShoppingCart> listCart(Long userId);

    List<ShoppingCart> batchCheckout(Long userId, List<Long> cartIds);

    void clearCart(Long userId);

    void clearCartCache(Long userId);
}
