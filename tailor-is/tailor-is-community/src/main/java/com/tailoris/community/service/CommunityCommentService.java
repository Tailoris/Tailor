package com.tailoris.community.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.community.dto.CommentRequest;
import com.tailoris.community.entity.CommunityComment;

public interface CommunityCommentService {

    CommunityComment createComment(Long userId, CommentRequest request);

    PageResponse<CommunityComment> listComments(Long postId, PageRequest pageRequest);

    java.util.List<CommunityComment> listReplies(Long parentId);

    void deleteComment(Long userId, Long commentId);

    PageResponse<CommunityComment> listUserComments(Long userId, PageRequest pageRequest);
}
