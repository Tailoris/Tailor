package com.tailoris.merchant.dto;

import com.tailoris.common.validator.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "商家入驻申请请求")
public class MerchantApplyRequest {

    @Schema(description = "商家类型：1-个人，2-企业，3-个体工商户")
    @NotNull(message = "商家类型不能为空")
    private Integer merchantType;

    @Schema(description = "企业/公司名称")
    @Size(max = 128, message = "公司名称不能超过128个字符")
    private String companyName;

    @Schema(description = "营业执照号/统一社会信用代码")
    @Size(max = 64, message = "营业执照号不能超过64个字符")
    private String licenseNo;

    @Schema(description = "联系人姓名")
    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 64, message = "联系人姓名不能超过64个字符")
    private String contactName;

    @Schema(description = "联系人电话")
    @PhoneNumber(message = "联系人电话格式不正确")
    private String contactPhone;

    @Schema(description = "联系人邮箱")
    private String contactEmail;

    @Schema(description = "所在省份")
    private String province;

    @Schema(description = "所在城市")
    private String city;

    @Schema(description = "所在区县")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经营范围")
    private String businessScope;

    @Schema(description = "资质证件URL列表")
    private List<@Valid QualificationItem> qualifications;

    @Data
    @Schema(description = "资质证件项")
    public static class QualificationItem {

        @Schema(description = "证件类型：1-营业执照，2-税务登记证，3-组织机构代码证，4-食品经营许可证，5-其他")
        @NotNull(message = "证件类型不能为空")
        private Integer certType;

        @Schema(description = "证件名称")
        @NotBlank(message = "证件名称不能为空")
        private String certName;

        @Schema(description = "证件编号")
        private String certNo;

        @Schema(description = "证件图片URL")
        @NotBlank(message = "证件图片不能为空")
        private String certUrl;

        @Schema(description = "证件正面图片URL")
        private String certFrontUrl;

        @Schema(description = "证件反面图片URL")
        private String certBackUrl;
    }
}
