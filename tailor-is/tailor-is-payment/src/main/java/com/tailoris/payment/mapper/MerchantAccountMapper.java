package com.tailoris.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.payment.entity.MerchantAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface MerchantAccountMapper extends BaseMapper<MerchantAccount> {

    /** BE-H-12: 原子增加待结算金额 - 避免竞态条件 */
    @Update("UPDATE merchant_account SET pending_amount = pending_amount + #{amount} WHERE merchant_id = #{merchantId}")
    int addPendingAmountAtomic(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    /** BE-H-12: 原子结算转账 - 待结算→可提现 - 避免竞态条件 */
    @Update("UPDATE merchant_account SET pending_amount = pending_amount - #{amount}, withdrawable_balance = withdrawable_balance + #{amount}, total_settlement = total_settlement + #{amount} WHERE merchant_id = #{merchantId} AND pending_amount >= #{amount}")
    int settleAtomic(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);
}
