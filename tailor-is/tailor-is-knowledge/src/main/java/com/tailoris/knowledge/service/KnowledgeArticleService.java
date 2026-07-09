package com.tailoris.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tailoris.knowledge.dto.ArticleQueryRequest;
import com.tailoris.knowledge.entity.KnowledgeArticle;

import java.util.List;

public interface KnowledgeArticleService {

    KnowledgeArticle createArticle(KnowledgeArticle article);

    KnowledgeArticle updateArticle(Long id, KnowledgeArticle article);

    void deleteArticle(Long id);

    KnowledgeArticle getArticleById(Long id);

    IPage<KnowledgeArticle> getArticlePage(ArticleQueryRequest queryRequest);

    List<KnowledgeArticle> getRecommendArticles(Integer limit);

    List<KnowledgeArticle> getHotArticles(Integer limit);

    List<KnowledgeArticle> getTopArticles(Integer limit);

    List<KnowledgeArticle> getArticlesByCategory(Long categoryId, Integer limit);

    void incrementViewCount(Long id);

    void incrementLikeCount(Long id);
}