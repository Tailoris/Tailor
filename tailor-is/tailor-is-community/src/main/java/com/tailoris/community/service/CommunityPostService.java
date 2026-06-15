package com.tailoris.community.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.community.dto.PostCreateRequest;
import com.tailoris.community.entity.CommunityPost;

public interface CommunityPostService {

    CommunityPost createPost(Long userId, PostCreateRequest request);

    CommunityPost updatePost(Long userId, Long postId, PostCreateRequest request);

    void deletePost(Long userId, Long postId);

    CommunityPost getPostDetail(Long postId);

    PageResponse<CommunityPost> listPosts(PageRequest pageRequest, Long categoryId, Integer type, String tag);

    PageResponse<CommunityPost> listUserPosts(Long userId, PageRequest pageRequest);

    void auditPost(Long postId, Integer status, Long auditBy, String auditRemark);

    void setTop(Long postId, Integer isTop);

    void setEssence(Long postId, Integer isEssence);
}
