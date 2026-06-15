package com.tailoris.supply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.supply.dto.SupplyPostRequest;
import com.tailoris.supply.entity.SupplyDemandPost;
import com.tailoris.supply.mapper.SupplyDemandPostMapper;
import com.tailoris.supply.service.SupplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplyServiceImpl implements SupplyService {

    private final SupplyDemandPostMapper supplyDemandPostMapper;

    @Override
    @Transactional
    public SupplyDemandPost createPost(Long userId, SupplyPostRequest request) {
        SupplyDemandPost post = new SupplyDemandPost();
        post.setId(SnowflakeIdGenerator.getInstance().nextId());
        post.setUserId(userId);
        post.setPostType(request.getPostType());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setImages(request.getImages());
        post.setCategoryId(request.getCategoryId());
        post.setMaterialType(request.getMaterialType());
        post.setQuantity(request.getQuantity());
        post.setUnitPrice(request.getUnitPrice());
        post.setLocation(request.getLocation());
        post.setProvince(request.getProvince());
        post.setCity(request.getCity());
        post.setContactName(request.getContactName());
        post.setContactPhone(request.getContactPhone());
        post.setContactWechat(request.getContactWechat());
        post.setExpireDate(request.getExpireDate());
        post.setViewCount(0);
        post.setContactCount(0);
        post.setStatus(1);
        post.setIsTop(request.getIsTop() != null ? request.getIsTop() : 0);
        post.setIsUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : 0);
        supplyDemandPostMapper.insert(post);
        return post;
    }

    @Override
    @Transactional
    public SupplyDemandPost updatePost(Long userId, Long postId, SupplyPostRequest request) {
        SupplyDemandPost post = supplyDemandPostMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) {
            throw new BusinessException("帖子不存在");
        }
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setImages(request.getImages());
        post.setCategoryId(request.getCategoryId());
        post.setMaterialType(request.getMaterialType());
        post.setQuantity(request.getQuantity());
        post.setUnitPrice(request.getUnitPrice());
        post.setLocation(request.getLocation());
        post.setProvince(request.getProvince());
        post.setCity(request.getCity());
        post.setContactName(request.getContactName());
        post.setContactPhone(request.getContactPhone());
        post.setContactWechat(request.getContactWechat());
        post.setExpireDate(request.getExpireDate());
        post.setIsTop(request.getIsTop() != null ? request.getIsTop() : 0);
        post.setIsUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : 0);
        supplyDemandPostMapper.updateById(post);
        return post;
    }

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        SupplyDemandPost post = supplyDemandPostMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) {
            throw new BusinessException("帖子不存在");
        }
        post.setStatus(3);
        supplyDemandPostMapper.updateById(post);
    }

    @Override
    public SupplyDemandPost getPostDetail(Long postId) {
        SupplyDemandPost post = supplyDemandPostMapper.selectById(postId);
        if (post != null && post.getStatus() == 1) {
            post.setViewCount(post.getViewCount() + 1);
            supplyDemandPostMapper.updateById(post);
        }
        return post;
    }

    @Override
    public PageResponse<SupplyDemandPost> listPosts(PageRequest pageRequest, Integer postType, Long categoryId, String city) {
        Page<SupplyDemandPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<SupplyDemandPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplyDemandPost::getStatus, 1);
        if (postType != null) {
            wrapper.eq(SupplyDemandPost::getPostType, postType);
        }
        if (categoryId != null) {
            wrapper.eq(SupplyDemandPost::getCategoryId, categoryId);
        }
        if (city != null) {
            wrapper.eq(SupplyDemandPost::getCity, city);
        }
        wrapper.orderByDesc(SupplyDemandPost::getIsTop);
        wrapper.orderByDesc(SupplyDemandPost::getCreateTime);
        Page<SupplyDemandPost> result = supplyDemandPostMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public PageResponse<SupplyDemandPost> listUserPosts(Long userId, PageRequest pageRequest) {
        Page<SupplyDemandPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<SupplyDemandPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplyDemandPost::getUserId, userId);
        wrapper.ne(SupplyDemandPost::getStatus, 3);
        wrapper.orderByDesc(SupplyDemandPost::getCreateTime);
        Page<SupplyDemandPost> result = supplyDemandPostMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }
}
