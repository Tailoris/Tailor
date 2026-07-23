package com.tailoris.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.design.entity.DesignComment;
import com.tailoris.design.mapper.DesignCommentMapper;
import com.tailoris.design.service.DesignCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignCommentServiceImpl extends ServiceImpl<DesignCommentMapper, DesignComment> implements DesignCommentService {

    @Override
    @Transactional
    public DesignComment addComment(DesignComment comment) {
        comment.setLikeCount(0);
        comment.setIsTop(0);
        save(comment);
        return comment;
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        removeById(id);
    }

    @Override
    public IPage<DesignComment> getCommentsByDesignId(Long designId, Integer current, Integer size) {
        LambdaQueryWrapper<DesignComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DesignComment::getDesignId, designId);
        wrapper.eq(DesignComment::getParentId, 0);
        wrapper.orderByDesc(DesignComment::getCreateTime);
        
        Page<DesignComment> page = new Page<>(current, size);
        return page(page, wrapper);
    }

    @Override
    public List<DesignComment> getChildComments(Long parentId) {
        LambdaQueryWrapper<DesignComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DesignComment::getParentId, parentId);
        wrapper.orderByAsc(DesignComment::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public void toggleCommentLike(Long commentId) {
        DesignComment comment = getById(commentId);
        if (comment != null) {
            comment.setLikeCount(comment.getLikeCount() + 1);
            updateById(comment);
        }
    }
}