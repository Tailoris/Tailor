package com.tailoris.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.knowledge.entity.KnowledgeComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeCommentMapper extends BaseMapper<KnowledgeComment> {
}