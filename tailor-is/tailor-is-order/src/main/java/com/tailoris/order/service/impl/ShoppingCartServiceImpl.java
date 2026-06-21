package com.tailoris.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.dto.CartAddRequest;
import com.tailoris.order.dto.CartUpdateRequest;
import com.tailoris.order.entity.ShoppingCart;
import com.tailoris.order.mapper.ShoppingCartMapper;
import com.tailoris.order.service.ShoppingCartService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartMapper shoppingCartMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToCart(Long userId, CartAddRequest request) {
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId)
                .eq(ShoppingCart::getSkuId, request.getSkuId());
        ShoppingCart existingCart = shoppingCartMapper.selectOne(queryWrapper);

        if (existingCart != null) {
            existingCart.setQuantity(existingCart.getQuantity() + request.getQuantity());
            // BE-M-33: 更新价格快照（以最新加购价格为准）
            if (request.getPriceSnapshot() != null) {
                existingCart.setPriceSnapshot(request.getPriceSnapshot());
            }
            shoppingCartMapper.updateById(existingCart);
            log.info("更新购物车商品数量, userId: {}, skuId: {}, newQuantity: {}",
                    userId, request.getSkuId(), existingCart.getQuantity());
        } else {
            ShoppingCart cart = new ShoppingCart();
            cart.setUserId(userId);
            cart.setProductId(request.getProductId());
            cart.setSkuId(request.getSkuId());
            cart.setQuantity(request.getQuantity());
            cart.setChecked(1);
            // BE-M-33: 保存价格快照，下单时使用，避免价格为 null 导致金额为 0
            cart.setPriceSnapshot(request.getPriceSnapshot());
            shoppingCartMapper.insert(cart);
            log.info("添加商品到购物车, userId: {}, skuId: {}, quantity: {}",
                    userId, request.getSkuId(), request.getQuantity());
        }

        evictCartCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCart(Long userId, Long cartId, CartUpdateRequest request) {
        ShoppingCart cart = shoppingCartMapper.selectById(cartId);
        if (cart == null) {
            throw new BusinessException("购物车商品不存在");
        }
        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该购物车商品");
        }

        if (request.getQuantity() != null) {
            cart.setQuantity(request.getQuantity());
        }
        if (request.getChecked() != null) {
            cart.setChecked(request.getChecked());
        }

        shoppingCartMapper.updateById(cart);
        evictCartCache(userId);
        log.info("更新购物车, userId: {}, cartId: {}", userId, cartId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCartItem(Long userId, Long cartId) {
        ShoppingCart cart = shoppingCartMapper.selectById(cartId);
        if (cart == null) {
            throw new BusinessException("购物车商品不存在");
        }
        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该购物车商品");
        }

        shoppingCartMapper.deleteById(cartId);
        evictCartCache(userId);
        log.info("删除购物车商品, userId: {}, cartId: {}", userId, cartId);
    }

    @Override
    public List<ShoppingCart> listCart(Long userId) {
        String cacheKey = OrderConstants.CART_CACHE_KEY_PREFIX + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            try {
                List<ShoppingCart> carts = objectMapper.readValue(cached,
                        new TypeReference<List<ShoppingCart>>() {});
                if (carts != null && !carts.isEmpty()) {
                    return carts;
                }
            } catch (JsonProcessingException e) {
                log.warn("Redis缓存反序列化失败, key: {}", cacheKey);
            }
        }

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId)
                .orderByDesc(ShoppingCart::getUpdateTime);
        List<ShoppingCart> carts = shoppingCartMapper.selectList(queryWrapper);

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(carts),
                    OrderConstants.CART_CACHE_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JsonProcessingException e) {
            log.warn("Redis缓存序列化失败, key: {}", cacheKey);
        }

        return carts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ShoppingCart> batchCheckout(Long userId, List<Long> cartIds) {
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId)
                .in(ShoppingCart::getId, cartIds);
        List<ShoppingCart> carts = shoppingCartMapper.selectList(queryWrapper);

        if (carts.isEmpty()) {
            throw new BusinessException("选中的购物车商品不存在");
        }

        return carts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        shoppingCartMapper.delete(queryWrapper);
        evictCartCache(userId);
        log.info("清空购物车, userId: {}", userId);
    }

    private void evictCartCache(Long userId) {
        stringRedisTemplate.delete(OrderConstants.CART_CACHE_KEY_PREFIX + userId);
    }

    @Override
    public void clearCartCache(Long userId) {
        evictCartCache(userId);
    }
}
