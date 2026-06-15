package com.tailoris.copyright.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "版权登记请求 - 增强版（CR-004 完整证据链）")
public class CopyrightRegisterRequest {

    @NotBlank(message = "作品名称不能为空")
    @Schema(description = "作品名称")
    private String workName;

    @NotNull(message = "作品类型不能为空")
    @Schema(description = "作品类型:1-图案设计 2-服装设计 3-印花 4-绣花 5-版式 6-其他")
    private Integer workType;

    @NotBlank(message = "文件哈希不能为空")
    @Schema(description = "文件SHA-256哈希")
    private String fileHash;

    @NotBlank(message = "文件类型不能为空")
    @Schema(description = "文件扩展名")
    private String fileType;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @NotBlank(message = "文件URL不能为空")
    @Schema(description = "原始文件存储URL")
    private String fileUrl;

    @Schema(description = "缩略图URL")
    private String thumbnailUrl;

    @Schema(description = "作品描述")
    private String description;

    @Schema(description = "关联商品ID")
    private Long productId;

    /** 增强字段：CR-004 完整证据链 */
    @Schema(description = "作者真实姓名")
    private String authorRealName;

    @Schema(description = "作者身份证号(AES加密传输)")
    private String authorIdCard;

    @Schema(description = "作者手机号(AES加密传输)")
    private String authorPhone;

    @Schema(description = "创作开始时间")
    private LocalDateTime creationStartTime;

    @Schema(description = "创作结束时间")
    private LocalDateTime creationEndTime;

    /** 增强字段：CR-007 IP作品非商用标注 */
    @Schema(description = "是否商用:0否1是,默认1")
    private Integer isCommercial;

    @Schema(description = "许可类型:1=个人 2=企业 3=非商用 4=CC-BY 5=CC-BY-NC")
    private Integer licenseType;

    @Schema(description = "许可说明")
    private String licenseText;

    @Schema(description = "是否启用强制水印")
    private Integer watermarkEnabled;
}
