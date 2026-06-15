package com.tailoris.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * 身份证号校验工具 - USR-004.
 *
 * <p>🔒 USR-004: 提供身份证号格式与校验位验证，遵循 GB 11643-1999。</p>
 *
 * <h3>校验规则</h3>
 * <ol>
 *   <li>18位数字（最后一位可为 X/x）</li>
 *   <li>前6位地区码有效范围</li>
 *   <li>7-14位出生日期合法</li>
 *   <li>最后一位校验位通过加权计算</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public final class IdCardValidator {

    /** 18位身份证正则（前17位数字 + 末位数字或X） */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");

    /** 加权因子 */
    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /** 校验码对照表 */
    private static final char[] CHECK_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private IdCardValidator() {
    }

    /**
     * 校验身份证号是否合法.
     *
     * @param idCard 身份证号
     * @return true=合法，false=非法
     */
    public static boolean isValid(String idCard) {
        if (idCard == null) {
            return false;
        }
        idCard = idCard.trim().toUpperCase();
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return false;
        }
        return isValidBirthDate(idCard) && isValidCheckCode(idCard);
    }

    /**
     * 提取出生日期.
     */
    public static LocalDate extractBirthDate(String idCard) {
        if (idCard == null || idCard.length() < 14) {
            return null;
        }
        try {
            String dateStr = idCard.substring(6, 14);
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 提取性别（奇数=男，偶数=女）.
     */
    public static Integer extractGender(String idCard) {
        if (idCard == null || idCard.length() < 17) {
            return null;
        }
        char genderDigit = idCard.charAt(16);
        return (genderDigit - '0') % 2 == 1 ? 1 : 2;
    }

    /**
     * 提取地区码（前6位）.
     */
    public static String extractRegionCode(String idCard) {
        if (idCard == null || idCard.length() < 6) {
            return null;
        }
        return idCard.substring(0, 6);
    }

    private static boolean isValidBirthDate(String idCard) {
        try {
            String dateStr = idCard.substring(6, 14);
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            // 出生日期不能晚于今天
            return !date.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static boolean isValidCheckCode(String idCard) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * WEIGHT[i];
        }
        char expected = CHECK_CODE[sum % 11];
        char actual = idCard.charAt(17);
        return expected == actual;
    }

    /**
     * 身份证号脱敏（保留前4后4）.
     */
    public static String mask(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
}
