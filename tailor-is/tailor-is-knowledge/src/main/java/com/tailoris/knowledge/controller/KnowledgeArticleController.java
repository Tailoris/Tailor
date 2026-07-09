package com.tailoris.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.knowledge.dto.ArticleQueryRequest;
import com.tailoris.knowledge.dto.ArticleRequest;
import com.tailoris.knowledge.entity.KnowledgeArticle;
import com.tailoris.knowledge.service.KnowledgeArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/article")
@RequiredArgsConstructor
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;

    @PostMapping
    public Result<KnowledgeArticle> createArticle(@Valid @RequestBody ArticleRequest request) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverImage(request.getCoverImage());
        article.setCategoryId(request.getCategoryId());
        article.setTagIds(request.getTagIds());
        article.setIsTop(request.getIsTop());
        article.setIsRecommend(request.getIsRecommend());
        article.setSourceType(request.getSourceType());
        article.setSourceUrl(request.getSourceUrl());
        article.setAuthorId(1L);
        article.setAuthorName("admin");
        
        KnowledgeArticle created = knowledgeArticleService.createArticle(article);
        return Result.success(created);
    }

    @PutMapping("/{id}")
    public Result<KnowledgeArticle> updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverImage(request.getCoverImage());
        article.setCategoryId(request.getCategoryId());
        article.setTagIds(request.getTagIds());
        article.setIsTop(request.getIsTop());
        article.setIsRecommend(request.getIsRecommend());
        article.setSourceType(request.getSourceType());
        article.setSourceUrl(request.getSourceUrl());
        
        KnowledgeArticle updated = knowledgeArticleService.updateArticle(id, article);
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        knowledgeArticleService.deleteArticle(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<KnowledgeArticle> getArticleById(@PathVariable Long id) {
        KnowledgeArticle article = knowledgeArticleService.getArticleById(id);
        return Result.success(article);
    }

    @GetMapping("/page")
    public Result<PageResponse<KnowledgeArticle>> getArticlePage(@ModelAttribute ArticleQueryRequest queryRequest) {
        IPage<KnowledgeArticle> page = knowledgeArticleService.getArticlePage(queryRequest);
        return Result.success(PageResponse.of(page));
    }

    @GetMapping("/recommend")
    public Result<List<KnowledgeArticle>> getRecommendArticles(@RequestParam(defaultValue = "10") Integer limit) {
        List<KnowledgeArticle> articles = knowledgeArticleService.getRecommendArticles(limit);
        return Result.success(articles);
    }

    @GetMapping("/hot")
    public Result<List<KnowledgeArticle>> getHotArticles(@RequestParam(defaultValue = "10") Integer limit) {
        List<KnowledgeArticle> articles = knowledgeArticleService.getHotArticles(limit);
        return Result.success(articles);
    }

    @GetMapping("/top")
    public Result<List<KnowledgeArticle>> getTopArticles(@RequestParam(defaultValue = "10") Integer limit) {
        List<KnowledgeArticle> articles = knowledgeArticleService.getTopArticles(limit);
        return Result.success(articles);
    }

    @GetMapping("/category/{categoryId}")
    public Result<List<KnowledgeArticle>> getArticlesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<KnowledgeArticle> articles = knowledgeArticleService.getArticlesByCategory(categoryId, limit);
        return Result.success(articles);
    }

    @PostMapping("/{id}/like")
    public Result<Void> incrementLikeCount(@PathVariable Long id) {
        knowledgeArticleService.incrementLikeCount(id);
        return Result.success();
    }
}