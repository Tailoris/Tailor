package com.tailoris.design.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tailoris.design.entity.DesignComment;

import java.util.List;

public interface DesignCommentService {

    DesignComment addComment(DesignComment comment);

    void deleteComment(Long id);

    IPage<DesignComment> getCommentsByDesignId(Long designId, Integer current, Integer size);

    List<DesignComment> getChildComments(Long parentId);

    void toggleCommentLike(Long commentId);
}