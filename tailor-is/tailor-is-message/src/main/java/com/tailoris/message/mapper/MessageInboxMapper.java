package com.tailoris.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.message.entity.MessageInbox;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageInboxMapper extends BaseMapper<MessageInbox> {
}
