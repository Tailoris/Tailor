package com.tailoris.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.payment.entity.EscrowAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface EscrowAccountMapper extends BaseMapper<EscrowAccount> {

    EscrowAccount selectByMerchantId(@Param("merchantId") Long merchantId);

    @Update("UPDATE escrow_account SET balance = #{balance}, frozen_amount = #{frozenAmount} WHERE id = #{id}")
    int updateBalance(@Param("id") Long id,
                      @Param("balance") BigDecimal balance,
                      @Param("frozenAmount") BigDecimal frozenAmount);
}