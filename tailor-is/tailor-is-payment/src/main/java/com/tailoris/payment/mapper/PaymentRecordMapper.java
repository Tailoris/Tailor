package com.tailoris.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.payment.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
