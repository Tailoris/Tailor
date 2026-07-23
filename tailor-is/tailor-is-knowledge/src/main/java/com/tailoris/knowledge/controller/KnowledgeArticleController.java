package com.tailoris.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.knowledge.dto.ArticleQueryRequest;
import com.tailoris.knowledge.dto.ArticleRequest;
import com.tailoris.knowledge.entity.KnowledgeArticle;
import com.tailoris.knowledge.service.KnowledgeArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(RoutePortConstant.ROUTE_KNOWLEDGE + "/article")
@RequiredArgsConstructor
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;

    @Operation(summary = "创建文章", description = "创建新的知识文章")
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

    @Operation(summary = "更新文章", description = "更新指定知识文章")
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

    @Operation(summary = "删除文章", description = "删除指定知识文章")
    @DeleteMapping("/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        knowledgeArticleService.deleteArticle(id);
        return Result.success();
    }

    @Operation(summary = "文章详情", description = "获取知识文章详情")
    @GetMapping("/{id}")
    public Result<KnowledgeArticle> getArticleById(@PathVariable Long id) {
        KnowledgeArticle article = knowledgeArticleService.getArticleById(id);
        return Result.success(article);
    }

    @Operation(summary = "文章分页查询", description = "分页查询知识文章列表")
    @GetMapping("/page")
    public Result<PageResponse<KnowledgeArticle>> getArticlePage(@ModelAttribute ArticleQueryRequest queryRequest) {
        IPage<KnowledgeArticle> page = knowledgeArticleService.getArticlePage(queryRequest);
        return Result.success(new PageResponse<>(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }

    @Operation(summary = "推荐文章", description = "获取推荐文章列表")
    @GetMapping("/recommend")
    public Result<List<KnowledgeArticle>> getRecommendArticles(@RequestParam(defaultValue = "10") Integer limit) {
        List<KnowledgeArticle> articles = knowledgeArticleService.getRecommendArticles(limit);
        return Result.success(articles);
    }

    @Operation(summary = "热门文章", description = "获取热门文章列表")
    @GetMapping("/hot")
    public Result<List<KnowledgeArticle>> getHotArticles(@RequestParam(defaultValue = "10") Integer limit) {
        List<KnowledgeArticle> articles = knowledgeArticleService.getHotArticles(limit);
        return Result.success(articles);
    }

    @Operation(summary = "置顶文章", description = "获取置顶文章列表")
    @GetMapping("/top")
    public Result<List<KnowledgeArticle>> getTopArticles(@RequestParam(defaultValue = "10") Integer limit) {
        List<KnowledgeArticle> articles = knowledgeArticleService.getTopArticles(limit);
        return Result.success(articles);
    }

    @Operation(summary = "分类文章", description = "获取指定分类下的文章列表")
    @GetMapping("/category/{categoryId}")
    public Result<List<KnowledgeArticle>> getArticlesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<KnowledgeArticle> articles = knowledgeArticleService.getArticlesByCategory(categoryId, limit);
        return Result.success(articles);
    }

    @Operation(summary = "文章点赞", description = "增加文章点赞数")
    @PostMapping("/{id}/like")
    public Result<Void> incrementLikeCount(@PathVariable Long id) {
        knowledgeArticleService.incrementLikeCount(id);
        return Result.success();
    }
}