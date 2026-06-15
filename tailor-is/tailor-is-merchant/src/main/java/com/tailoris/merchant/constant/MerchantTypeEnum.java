package com.tailoris.merchant.constant;

import lombok.Getter;

@Getter
public enum MerchantTypeEnum {

    PERSONAL_STYLIST(1, "个人版师店", "个人版师入驻，提供定制打版服务"),
    STUDIO(2, "工作室店", "工作室入驻，提供团队化定制服务"),
    BRAND_ENTERPRISE(3, "品牌企业店", "品牌企业入驻，提供批量生产服务"),
    SUPPLY_CHAIN(4, "供应链商户店", "供应链商户入驻，提供面料/辅料/加工服务");

    private final int code;
    private final String name;
    private final String description;

    MerchantTypeEnum(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static MerchantTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MerchantTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return fromCode(code) != null;
    }
}