package com.tailoris.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.common.util.StringUtils;
import com.tailoris.knowledge.dto.ArticleQueryRequest;
import com.tailoris.knowledge.entity.KnowledgeArticle;
import com.tailoris.knowledge.entity.KnowledgeCategory;
import com.tailoris.knowledge.mapper.KnowledgeArticleMapper;
import com.tailoris.knowledge.mapper.KnowledgeCategoryMapper;
import com.tailoris.knowledge.service.KnowledgeArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeArticleServiceImpl extends ServiceImpl<KnowledgeArticleMapper, KnowledgeArticle> implements KnowledgeArticleService {

    private final KnowledgeCategoryMapper knowledgeCategoryMapper;

    @Override
    @Transactional
    public KnowledgeArticle createArticle(KnowledgeArticle article) {
        article.setArticleNo("KNA" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        article.setStatus(1);
        article.setViewCount(0L);
        article.setLikeCount(0L);
        article.setCommentCount(0L);
        article.setIsTop(article.getIsTop() != null ? article.getIsTop() : 0);
        article.setIsRecommend(article.getIsRecommend() != null ? article.getIsRecommend() : 0);
        
        if (article.getCategoryId() != null) {
            KnowledgeCategory category = knowledgeCategoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                article.setCategoryName(category.getName());
                category.setArticleCount(category.getArticleCount() + 1);
                knowledgeCategoryMapper.updateById(category);
            }
        }
        
        save(article);
        return article;
    }

    @Override
    @Transactional
    public KnowledgeArticle updateArticle(Long id, KnowledgeArticle article) {
        KnowledgeArticle existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("文章不存在");
        }
        
        Long oldCategoryId = existing.getCategoryId();
        article.setId(id);
        article.setArticleNo(existing.getArticleNo());
        
        if (article.getCategoryId() != null && !article.getCategoryId().equals(oldCategoryId)) {
            KnowledgeCategory newCategory = knowledgeCategoryMapper.selectById(article.getCategoryId());
            if (newCategory != null) {
                article.setCategoryName(newCategory.getName());
                newCategory.setArticleCount(newCategory.getArticleCount() + 1);
                knowledgeCategoryMapper.updateById(newCategory);
            }
            if (oldCategoryId != null) {
                KnowledgeCategory oldCategory = knowledgeCategoryMapper.selectById(oldCategoryId);
                if (oldCategory != null) {
                    oldCategory.setArticleCount(Math.max(0, oldCategory.getArticleCount() - 1));
                    knowledgeCategoryMapper.updateById(oldCategory);
                }
            }
        }
        
        updateById(article);
        return getById(id);
    }

    @Override
    @Transactional
    public void deleteArticle(Long id) {
        KnowledgeArticle article = getById(id);
        if (article != null && article.getCategoryId() != null) {
            KnowledgeCategory category = knowledgeCategoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                category.setArticleCount(Math.max(0, category.getArticleCount() - 1));
                knowledgeCategoryMapper.updateById(category);
            }
        }
        removeById(id);
    }

    @Override
    public KnowledgeArticle getArticleById(Long id) {
        KnowledgeArticle article = getById(id);
        if (article != null) {
            incrementViewCount(id);
        }
        return article;
    }

    @Override
    public IPage<KnowledgeArticle> getArticlePage(ArticleQueryRequest queryRequest) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = buildQueryWrapper(queryRequest);
        Page<KnowledgeArticle> page = new Page<>(queryRequest.getCurrent(), queryRequest.getSize());
        
        if (StringUtils.isNotBlank(queryRequest.getSortBy())) {
            String sortColumn = convertSortField(queryRequest.getSortBy());
            if ("asc".equalsIgnoreCase(queryRequest.getSortOrder())) {
                wrapper.last("ORDER BY " + sortColumn + " ASC");
            } else {
                wrapper.last("ORDER BY " + sortColumn + " DESC");
            }
        } else {
            wrapper.last("ORDER BY is_top DESC, created_at DESC");
        }
        
        return page(page, wrapper);
    }

    @Override
    public List<KnowledgeArticle> getRecommendArticles(Integer limit) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getStatus, 1);
        wrapper.eq(KnowledgeArticle::getIsRecommend, 1);
        wrapper.orderByDesc(KnowledgeArticle::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return list(wrapper);
    }

    @Override
    public List<KnowledgeArticle> getHotArticles(Integer limit) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getStatus, 1);
        wrapper.orderByDesc(KnowledgeArticle::getViewCount);
        wrapper.last("LIMIT " + limit);
        return list(wrapper);
    }

    @Override
    public List<KnowledgeArticle> getTopArticles(Integer limit) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getStatus, 1);
        wrapper.eq(KnowledgeArticle::getIsTop, 1);
        wrapper.orderByDesc(KnowledgeArticle::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return list(wrapper);
    }

    @Override
    public List<KnowledgeArticle> getArticlesByCategory(Long categoryId, Integer limit) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getStatus, 1);
        wrapper.eq(KnowledgeArticle::getCategoryId, categoryId);
        wrapper.orderByDesc(KnowledgeArticle::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return list(wrapper);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        KnowledgeArticle article = getById(id);
        if (article != null) {
            article.setViewCount(article.getViewCount() + 1);
            updateById(article);
        }
    }

    @Override
    @Transactional
    public void incrementLikeCount(Long id) {
        KnowledgeArticle article = getById(id);
        if (article != null) {
            article.setLikeCount(article.getLikeCount() + 1);
            updateById(article);
        }
    }

    private LambdaQueryWrapper<KnowledgeArticle> buildQueryWrapper(ArticleQueryRequest query) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getStatus, 1);
        
        if (StringUtils.isNotBlank(query.getTitle())) {
            wrapper.like(KnowledgeArticle::getTitle, query.getTitle());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(KnowledgeArticle::getCategoryId, query.getCategoryId());
        }
        if (query.getIsTop() != null) {
            wrapper.eq(KnowledgeArticle::getIsTop, query.getIsTop());
        }
        if (query.getIsRecommend() != null) {
            wrapper.eq(KnowledgeArticle::getIsRecommend, query.getIsRecommend());
        }
        
        return wrapper;
    }

    private String convertSortField(String sortBy) {
        return switch (sortBy) {
            case "viewCount" -> "view_count";
            case "likeCount" -> "like_count";
            case "commentCount" -> "comment_count";
            case "createTime" -> "created_at";
            default -> "created_at";
        };
    }
}