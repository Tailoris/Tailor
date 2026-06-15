package com.tailoris.common.util;

public final class LogMaskUtils {

    private LogMaskUtils() {
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, atIndex);
        String suffix = email.substring(atIndex);
        if (prefix.length() <= 2) {
            return "*" + suffix;
        }
        return prefix.charAt(0) + "***" + suffix;
    }

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }

    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + name.substring(1, name.length() - 1).replaceAll(".", "*") + name.charAt(name.length() - 1);
    }

    public static String maskOrderNo(String orderNo) {
        if (orderNo == null || orderNo.length() <= 6) {
            return orderNo;
        }
        return orderNo.substring(0, 3) + "***" + orderNo.substring(orderNo.length() - 3);
    }

    /**
     * 通用字符串脱敏（保留前2后2）.
     *
     * <p>适用于 openid、unionid、token 等无固定长度的敏感标识。</p>
     */
    public static String maskString(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}