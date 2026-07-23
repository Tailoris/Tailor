package com.tailoris.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.common.util.StringUtils;
import com.tailoris.design.dto.DesignQueryRequest;
import com.tailoris.design.entity.Design;
import com.tailoris.design.entity.DesignCollection;
import com.tailoris.design.entity.DesignLike;
import com.tailoris.design.mapper.DesignCollectionMapper;
import com.tailoris.design.mapper.DesignLikeMapper;
import com.tailoris.design.mapper.DesignMapper;
import com.tailoris.design.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DesignServiceImpl extends ServiceImpl<DesignMapper, Design> implements DesignService {

    private final DesignLikeMapper designLikeMapper;
    private final DesignCollectionMapper designCollectionMapper;

    @Override
    @Transactional
    public Design createDesign(Design design) {
        design.setDesignNo("DSN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        design.setStatus(1);
        design.setLikeCount(0L);
        design.setViewCount(0L);
        design.setCopyrightStatus(0);
        save(design);
        return design;
    }

    @Override
    @Transactional
    public Design updateDesign(Long id, Design design) {
        Design existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("设计不存在");
        }
        design.setId(id);
        design.setDesignNo(existing.getDesignNo());
        updateById(design);
        return getById(id);
    }

    @Override
    @Transactional
    public void deleteDesign(Long id) {
        removeById(id);
    }

    @Override
    public Design getDesignById(Long id) {
        Design design = getById(id);
        if (design != null) {
            incrementViewCount(id);
        }
        return design;
    }

    @Override
    public IPage<Design> getDesignPage(DesignQueryRequest queryRequest) {
        LambdaQueryWrapper<Design> wrapper = buildQueryWrapper(queryRequest);
        Page<Design> page = new Page<>(queryRequest.getCurrent(), queryRequest.getSize());
        
        if (StringUtils.isNotBlank(queryRequest.getSortBy())) {
            String sortColumn = convertSortField(queryRequest.getSortBy());
            if ("asc".equalsIgnoreCase(queryRequest.getSortOrder())) {
                wrapper.last("ORDER BY " + sortColumn + " ASC");
            } else {
                wrapper.last("ORDER BY " + sortColumn + " DESC");
            }
        } else {
            wrapper.last("ORDER BY created_at DESC");
        }
        
        return page(page, wrapper);
    }

    @Override
    public List<Design> getHotDesigns(Integer limit) {
        LambdaQueryWrapper<Design> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Design::getStatus, 1);
        wrapper.orderByDesc(Design::getLikeCount);
        wrapper.last("LIMIT " + limit);
        return list(wrapper);
    }

    @Override
    public List<Design> getNewDesigns(Integer limit) {
        LambdaQueryWrapper<Design> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Design::getStatus, 1);
        wrapper.orderByDesc(Design::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return list(wrapper);
    }

    @Override
    public List<Design> getDesignsByUserId(Long userId) {
        LambdaQueryWrapper<Design> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Design::getUserId, userId);
        wrapper.eq(Design::getStatus, 1);
        wrapper.orderByDesc(Design::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        Design design = getById(id);
        if (design != null) {
            design.setViewCount(design.getViewCount() + 1);
            updateById(design);
        }
    }

    @Override
    @Transactional
    public boolean toggleLike(Long designId, Long userId) {
        LambdaQueryWrapper<DesignLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DesignLike::getDesignId, designId);
        wrapper.eq(DesignLike::getUserId, userId);
        DesignLike existing = designLikeMapper.selectOne(wrapper);
        
        Design design = getById(designId);
        if (design == null) {
            throw new RuntimeException("设计不存在");
        }
        
        if (existing != null) {
            designLikeMapper.deleteById(existing.getId());
            design.setLikeCount(Math.max(0, design.getLikeCount() - 1));
            updateById(design);
            return false;
        } else {
            DesignLike like = new DesignLike();
            like.setDesignId(designId);
            like.setUserId(userId);
            designLikeMapper.insert(like);
            design.setLikeCount(design.getLikeCount() + 1);
            updateById(design);
            return true;
        }
    }

    @Override
    @Transactional
    public boolean toggleCollection(Long designId, Long userId) {
        LambdaQueryWrapper<DesignCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DesignCollection::getDesignId, designId);
        wrapper.eq(DesignCollection::getUserId, userId);
        DesignCollection existing = designCollectionMapper.selectOne(wrapper);
        
        if (existing != null) {
            designCollectionMapper.deleteById(existing.getId());
            return false;
        } else {
            DesignCollection collection = new DesignCollection();
            collection.setDesignId(designId);
            collection.setUserId(userId);
            designCollectionMapper.insert(collection);
            return true;
        }
    }

    @Override
    public boolean checkIsLiked(Long designId, Long userId) {
        LambdaQueryWrapper<DesignLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DesignLike::getDesignId, designId);
        wrapper.eq(DesignLike::getUserId, userId);
        return designLikeMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean checkIsCollected(Long designId, Long userId) {
        LambdaQueryWrapper<DesignCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DesignCollection::getDesignId, designId);
        wrapper.eq(DesignCollection::getUserId, userId);
        return designCollectionMapper.selectCount(wrapper) > 0;
    }

    private LambdaQueryWrapper<Design> buildQueryWrapper(DesignQueryRequest query) {
        LambdaQueryWrapper<Design> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Design::getStatus, 1);
        
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(Design::getName, query.getName());
        }
        if (StringUtils.isNotBlank(query.getDesignType())) {
            wrapper.eq(Design::getDesignType, query.getDesignType());
        }
        if (StringUtils.isNotBlank(query.getStyleCategory())) {
            wrapper.eq(Design::getStyleCategory, query.getStyleCategory());
        }
        if (StringUtils.isNotBlank(query.getColor())) {
            wrapper.eq(Design::getColor, query.getColor());
        }
        if (StringUtils.isNotBlank(query.getMaterial())) {
            wrapper.eq(Design::getMaterial, query.getMaterial());
        }
        if (StringUtils.isNotBlank(query.getDifficulty())) {
            wrapper.eq(Design::getDifficulty, query.getDifficulty());
        }
        if (query.getIsFree() != null) {
            wrapper.eq(Design::getIsFree, query.getIsFree());
        }
        if (query.getUserId() != null) {
            wrapper.eq(Design::getUserId, query.getUserId());
        }
        if (query.getMerchantId() != null) {
            wrapper.eq(Design::getMerchantId, query.getMerchantId());
        }
        
        return wrapper;
    }

    private String convertSortField(String sortBy) {
        return switch (sortBy) {
            case "likeCount" -> "like_count";
            case "viewCount" -> "view_count";
            case "price" -> "price";
            case "createTime" -> "created_at";
            default -> "created_at";
        };
    }
}