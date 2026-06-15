package com.tailoris.supply.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.supply.dto.SupplyPostRequest;
import com.tailoris.supply.entity.SupplyDemandPost;

public interface SupplyService {

    SupplyDemandPost createPost(Long userId, SupplyPostRequest request);

    SupplyDemandPost updatePost(Long userId, Long postId, SupplyPostRequest request);

    void deletePost(Long userId, Long postId);

    SupplyDemandPost getPostDetail(Long postId);

    PageResponse<SupplyDemandPost> listPosts(PageRequest pageRequest, Integer postType, Long categoryId, String city);

    PageResponse<SupplyDemandPost> listUserPosts(Long userId, PageRequest pageRequest);
}
