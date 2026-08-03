package com.tailoris.api.admin.dto;

import com.tailoris.api.merchant.entity.Merchant;
import com.tailoris.api.merchant.entity.MerchantCategoryRel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商家详情响应")
public class MerchantDetailResponse extends Merchant {

    @Schema(description = "店铺信息")
    private Map<String, Object> shopInfo;

    @Schema(description = "资质列表")
    private List<?> qualifications;

    @Schema(description = "保证金信息")
    private Object qualityDeposit;

    @Schema(description = "账户汇总：totalIncome总收入/totalWithdraw总提现/balance余额/frozen冻结/pending待结算")
    private Object accountSummary;

    @Schema(description = "经营分类列表")
    private List<MerchantCategoryRel> categories;
}
