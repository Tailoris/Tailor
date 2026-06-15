package com.tailoris.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "会员等级设置请求")
public class MemberLevelRequest {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;

    @NotNull(message = "店铺ID不能为空")
    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "会员等级：1-普通，2-银卡，3-金卡，4-钻石")
    private Integer level;
}
