package com.tailoris.common.util;

public final class DesensitizeUtils {

    private static final String PHONE_MASK = "****";
    private static final String ID_CARD_MASK = "********";
    private static final String EMAIL_MASK = "****";
    private static final String BANK_CARD_MASK = "****";
    private static final String ADDRESS_MASK = "********";

    private DesensitizeUtils() {
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + PHONE_MASK + phone.substring(phone.length() - 4);
    }

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 4) + ID_CARD_MASK + idCard.substring(idCard.length() - 4);
    }

    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + BANK_CARD_MASK + bankCard.substring(bankCard.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= 2) {
            return email;
        }
        return localPart.charAt(0) + EMAIL_MASK + localPart.charAt(localPart.length() - 1) + domain;
    }

    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "**" + name.charAt(name.length() - 1);
    }

    public static String maskAddress(String address) {
        if (address == null || address.length() < 6) {
            return address;
        }
        return address.substring(0, 6) + ADDRESS_MASK;
    }

    public static String maskCustom(String input, int prefixLength, int suffixLength) {
        if (input == null || input.length() < prefixLength + suffixLength) {
            return input;
        }
        String prefix = input.substring(0, prefixLength);
        String suffix = input.substring(input.length() - suffixLength);
        int maskLength = input.length() - prefixLength - suffixLength;
        StringBuilder mask = new StringBuilder(maskLength);
        for (int i = 0; i < maskLength; i++) {
            mask.append('*');
        }
        return prefix + mask + suffix;
    }
}
