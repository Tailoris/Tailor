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

    /** BE-H-11: 原子入金 - 避免竞态条件 */
    @Update("UPDATE escrow_account SET balance = balance + #{amount} WHERE merchant_id = #{merchantId}")
    int depositAtomic(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    /** BE-H-11: 原子出金 - 避免竞态条件 */
    @Update("UPDATE escrow_account SET balance = balance - #{amount} WHERE merchant_id = #{merchantId} AND (balance - frozen_amount) >= #{amount}")
    int releaseAtomic(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    /** BE-H-11: 原子冻结 - 避免竞态条件 */
    @Update("UPDATE escrow_account SET frozen_amount = frozen_amount + #{amount} WHERE merchant_id = #{merchantId} AND (balance - frozen_amount) >= #{amount}")
    int freezeAtomic(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    /** BE-H-11: 原子解冻 - 避免竞态条件 */
    @Update("UPDATE escrow_account SET frozen_amount = frozen_amount - #{amount} WHERE merchant_id = #{merchantId} AND frozen_amount >= #{amount}")
    int unfreezeAtomic(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);
}