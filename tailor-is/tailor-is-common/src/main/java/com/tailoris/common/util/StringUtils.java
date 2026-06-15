package com.tailoris.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

public final class StringUtils {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private StringUtils() {
    }

    public static boolean isBlank(CharSequence str) {
        return str == null || str.toString().trim().isEmpty();
    }

    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }

    public static boolean isBlank(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotBlank(Collection<?> collection) {
        return !isBlank(collection);
    }

    public static boolean isBlank(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotBlank(Map<?, ?> map) {
        return !isBlank(map);
    }

    public static boolean isBlank(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNotBlank(Object[] array) {
        return !isBlank(array);
    }

    public static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    public static String emptyToNull(String str) {
        return isBlank(str) ? null : str;
    }

    public static String defaultIfBlank(String str, String defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }

    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return isNotBlank(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isNumeric(String str) {
        return isNotBlank(str) && NUMBER_PATTERN.matcher(str).matches();
    }

    public static boolean isInteger(String str) {
        return isNotBlank(str) && str.matches("^-?\\d+$");
    }

    public static boolean isUuid(String str) {
        return isNotBlank(str) && UUID_PATTERN.matcher(str).matches();
    }

    public static boolean containsChinese(String str) {
        return isNotBlank(str) && CHINESE_PATTERN.matcher(str).find();
    }

    public static int getChineseCharCount(String str) {
        if (isBlank(str)) {
            return 0;
        }
        int count = 0;
        for (char c : str.toCharArray()) {
            if (CHINESE_PATTERN.matcher(String.valueOf(c)).matches()) {
                count++;
            }
        }
        return count;
    }

    public static String toCamelCase(String str, boolean lowerCaseFirst) {
        if (isBlank(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_' || c == '-') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(lowerCaseFirst && sb.length() == 0 ? Character.toLowerCase(c) : c);
                }
            }
        }
        return sb.toString();
    }

    public static String toUnderScoreCase(String str) {
        if (isBlank(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String repeat(String str, int count) {
        if (isBlank(str) || count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static String truncate(String str, int maxLength, String suffix) {
        if (isBlank(str) || str.length() <= maxLength) {
            return str;
        }
        suffix = nullToEmpty(suffix);
        return str.substring(0, maxLength - suffix.length()) + suffix;
    }
}
