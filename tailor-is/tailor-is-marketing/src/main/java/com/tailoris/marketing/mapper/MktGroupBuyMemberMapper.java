package com.tailoris.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.marketing.entity.MktGroupBuyMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MktGroupBuyMemberMapper extends BaseMapper<MktGroupBuyMember> {
    void updateBatch(@Param("list") List<MktGroupBuyMember> list);
}
