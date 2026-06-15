package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.merchant.dto.ShopDecorationRequest;
import com.tailoris.merchant.dto.ShopUpdateRequest;
import com.tailoris.merchant.entity.MerchantShop;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantShopMapper;
import com.tailoris.merchant.service.MerchantShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.tailoris.merchant.constant.MerchantConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantShopServiceImpl implements MerchantShopService {

    private final MerchantShopMapper merchantShopMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public MerchantShop createShop(Long merchantId, ShopUpdateRequest request) {
        MerchantShop shop = new MerchantShop();
        shop.setMerchantId(merchantId);
        shop.setShopName(request.getShopName());
        shop.setShopLogo(request.getShopLogo());
        shop.setShopBanner(request.getShopBanner());
        shop.setShopDesc(request.getShopDesc());
        shop.setShopStatus(SHOP_STATUS_DECORATING);
        shop.setDecorationConfig(request.getDecorationConfig());
        shop.setShopTheme(request.getShopTheme());
        shop.setAnnouncement(request.getAnnouncement());
        shop.setContactService(request.getContactService());
        shop.setProvince(request.getProvince());
        shop.setCity(request.getCity());
        shop.setDistrict(request.getDistrict());
        shop.setAddress(request.getAddress());
        shop.setLongitude(request.getLongitude());
        shop.setLatitude(request.getLatitude());
        shop.setShopRating(5.00);
        shop.setFollowerCount(0);
        shop.setProductCount(0);
        shop.setSalesCount(0);

        merchantShopMapper.insert(shop);
        log.info("店铺创建成功, merchantId: {}, shopId: {}", merchantId, shop.getId());
        return shop;
    }

    @Override
    public void updateShop(Long merchantId, Long shopId, ShopUpdateRequest request) {
        MerchantShop shop = merchantShopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException("店铺不存在");
        }
        if (!shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权操作该店铺");
        }

        if (StringUtils.hasText(request.getShopName())) {
            shop.setShopName(request.getShopName());
        }
        if (request.getShopLogo() != null) {
            shop.setShopLogo(request.getShopLogo());
        }
        if (request.getShopBanner() != null) {
            shop.setShopBanner(request.getShopBanner());
        }
        if (request.getShopDesc() != null) {
            shop.setShopDesc(request.getShopDesc());
        }
        if (request.getDecorationConfig() != null) {
            shop.setDecorationConfig(request.getDecorationConfig());
        }
        if (request.getShopTheme() != null) {
            shop.setShopTheme(request.getShopTheme());
        }
        if (request.getAnnouncement() != null) {
            shop.setAnnouncement(request.getAnnouncement());
        }
        if (request.getContactService() != null) {
            shop.setContactService(request.getContactService());
        }
        if (request.getProvince() != null) {
            shop.setProvince(request.getProvince());
        }
        if (request.getCity() != null) {
            shop.setCity(request.getCity());
        }
        if (request.getDistrict() != null) {
            shop.setDistrict(request.getDistrict());
        }
        if (request.getAddress() != null) {
            shop.setAddress(request.getAddress());
        }
        if (request.getLongitude() != null) {
            shop.setLongitude(request.getLongitude());
        }
        if (request.getLatitude() != null) {
            shop.setLatitude(request.getLatitude());
        }

        merchantShopMapper.updateById(shop);
        stringRedisTemplate.delete(REDIS_KEY_SHOP_INFO + shopId);
        log.info("店铺更新成功, shopId: {}", shopId);
    }

    @Override
    public void updateShopStatus(Long merchantId, Long shopId, Integer shopStatus) {
        MerchantShop shop = merchantShopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException("店铺不存在");
        }
        if (!shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权操作该店铺");
        }

        shop.setShopStatus(shopStatus);
        merchantShopMapper.updateById(shop);
        stringRedisTemplate.delete(REDIS_KEY_SHOP_INFO + shopId);
        log.info("店铺状态更新成功, shopId: {}, shopStatus: {}", shopId, shopStatus);
    }

    @Override
    public MerchantShop getShopInfo(Long shopId) {
        String cacheKey = REDIS_KEY_SHOP_INFO + shopId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, MerchantShop.class);
            } catch (Exception e) {
                log.warn("Redis反序列化失败, key: {}", cacheKey);
            }
        }

        MerchantShop shop = merchantShopMapper.selectById(shopId);
        if (shop != null) {
            try {
                stringRedisTemplate.opsForValue().set(
                        cacheKey,
                        objectMapper.writeValueAsString(shop),
                        REDIS_EXPIRE_MINUTES,
                        TimeUnit.MINUTES
                );
            } catch (JsonProcessingException e) {
                log.warn("Redis序列化失败, key: {}", cacheKey);
            }
        }
        return shop;
    }

    @Override
    public List<MerchantShop> listShopsByMerchant(Long merchantId) {
        LambdaQueryWrapper<MerchantShop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MerchantShop::getMerchantId, merchantId)
                .orderByDesc(MerchantShop::getCreateTime);
        return merchantShopMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantShop saveDecoration(Long merchantId, ShopDecorationRequest request) {
        MerchantShop shop = merchantShopMapper.selectById(request.getShopId());
        if (shop == null) {
            throw new MerchantBusinessException("店铺不存在");
        }
        if (!shop.getMerchantId().equals(merchantId)) {
            throw new MerchantBusinessException("无权操作该店铺");
        }

        // 主题色格式校验
        if (request.getShopTheme() != null && !request.getShopTheme().isEmpty()) {
            if (!request.getShopTheme().matches("^#[0-9A-Fa-f]{6}$")) {
                throw new MerchantBusinessException("主题色格式错误，应为 #RRGGBB 格式");
            }
        }

        if (request.getShopLogo() != null) shop.setShopLogo(request.getShopLogo());
        if (request.getShopBanner() != null) shop.setShopBanner(request.getShopBanner());
        if (request.getShopDesc() != null) shop.setShopDesc(request.getShopDesc());
        if (request.getShopTheme() != null) shop.setShopTheme(request.getShopTheme());
        if (request.getAnnouncement() != null) shop.setAnnouncement(request.getAnnouncement());
        if (request.getContactService() != null) shop.setContactService(request.getContactService());

        // 装修配置（JSON）
        try {
            String config = objectMapper.writeValueAsString(buildDecorationConfig(request));
            shop.setDecorationConfig(config);
        } catch (JsonProcessingException e) {
            throw new MerchantBusinessException("装修配置序列化失败: " + e.getMessage());
        }

        merchantShopMapper.updateById(shop);
        stringRedisTemplate.delete(REDIS_KEY_SHOP_INFO + request.getShopId());
        log.info("店铺装修配置保存: shopId={}, merchantId={}", request.getShopId(), merchantId);
        return shop;
    }

    @Override
    public Map<String, Object> getDecoration(Long shopId) {
        MerchantShop shop = getShopInfo(shopId);
        if (shop == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("shopId", shop.getId());
        result.put("shopName", shop.getShopName());
        result.put("shopLogo", shop.getShopLogo());
        result.put("shopBanner", shop.getShopBanner());
        result.put("shopDesc", shop.getShopDesc());
        result.put("shopTheme", shop.getShopTheme());
        result.put("announcement", shop.getAnnouncement());
        result.put("contactService", shop.getContactService());
        result.put("shopStatus", shop.getShopStatus());

        // 解析装修配置
        if (shop.getDecorationConfig() != null && !shop.getDecorationConfig().isEmpty()) {
            try {
                Map<String, Object> decoration = objectMapper.readValue(
                        shop.getDecorationConfig(), new TypeReference<Map<String, Object>>() {});
                result.putAll(decoration);
            } catch (Exception e) {
                log.warn("装修配置解析失败: shopId={}, err={}", shopId, e.getMessage());
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> previewDecoration(Long shopId) {
        Map<String, Object> decoration = getDecoration(shopId);
        decoration.put("previewMode", true);
        decoration.put("previewTime", System.currentTimeMillis());
        return decoration;
    }

    // ============================================================
    // 私有方法
    // ============================================================

    private Map<String, Object> buildDecorationConfig(ShopDecorationRequest request) {
        Map<String, Object> config = new HashMap<>();
        if (request.getDecorationConfig() != null) {
            // 用户提供的原始配置
            try {
                Map<String, Object> userConfig = objectMapper.readValue(
                        request.getDecorationConfig(), new TypeReference<Map<String, Object>>() {});
                config.putAll(userConfig);
            } catch (Exception e) {
                log.warn("用户装修配置解析失败: {}", e.getMessage());
            }
        }
        if (request.getNavItems() != null) {
            config.put("navItems", request.getNavItems());
        }
        if (request.getHomeModules() != null) {
            config.put("homeModules", request.getHomeModules());
        }
        config.put("version", "1.0");
        return config;
    }
}
