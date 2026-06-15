package com.tailoris.api.admin.dto;

import com.tailoris.common.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询请求")
public class UserQueryRequest extends PageRequest {

    @Schema(description = "关键词（用户名/手机号/昵称）")
    private String keyword;

    @Schema(description = "状态：0-正常，1-冻结")
    private Integer status;
}
