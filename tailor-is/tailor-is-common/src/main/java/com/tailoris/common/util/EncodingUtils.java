package com.tailoris.common.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class EncodingUtils {

    private EncodingUtils() {
    }

    public static boolean isValidUtf8(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return true;
        }

        int i = 0;
        while (i < bytes.length) {
            int byteValue = bytes[i] & 0xFF;

            if ((byteValue & 0x80) == 0) {
                i++;
            } else if ((byteValue & 0xE0) == 0xC0) {
                if (i + 1 >= bytes.length) return false;
                int secondByte = bytes[i + 1] & 0xFF;
                if ((secondByte & 0xC0) != 0x80) return false;
                if (byteValue == 0xC0 || byteValue == 0xC1) return false;
                i += 2;
            } else if ((byteValue & 0xF0) == 0xE0) {
                if (i + 2 >= bytes.length) return false;
                int secondByte = bytes[i + 1] & 0xFF;
                int thirdByte = bytes[i + 2] & 0xFF;
                if ((secondByte & 0xC0) != 0x80 || (thirdByte & 0xC0) != 0x80) return false;
                if (byteValue == 0xE0 && (secondByte & 0xE0) == 0x80) return false;
                i += 3;
            } else if ((byteValue & 0xF8) == 0xF0) {
                if (i + 3 >= bytes.length) return false;
                int secondByte = bytes[i + 1] & 0xFF;
                int thirdByte = bytes[i + 2] & 0xFF;
                int fourthByte = bytes[i + 3] & 0xFF;
                if ((secondByte & 0xC0) != 0x80 || (thirdByte & 0xC0) != 0x80 || (fourthByte & 0xC0) != 0x80) {
                    return false;
                }
                if (byteValue > 0xF4) return false;
                i += 4;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean containsMojibake(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (c == '\uFFFD') {
                return true;
            }

            if (c >= '\uD800' && c <= '\uDFFF') {
                return true;
            }

            if (c == '\u00C2' || c == '\u00C3' || c == '\u00E2' || c == '\u00E9') {
                if (i + 1 < str.length()) {
                    char nextChar = str.charAt(i + 1);
                    if (nextChar >= '\u0080' && nextChar <= '\u00BF') {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\uFFFD') {
                continue;
            }
            if (c >= '\uD800' && c <= '\uDFFF') {
                continue;
            }
            if (c >= 0x0000 && c <= 0x001F && c != 0x0009 && c != 0x000A && c != 0x000D) {
                continue;
            }
            if (c == '\u007F') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static String convertToUtf8(String input, Charset sourceCharset) {
        if (input == null) {
            return null;
        }
        byte[] bytes = input.getBytes(sourceCharset);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String detectCharset(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "UTF-8";
        }

        if (isValidUtf8(bytes)) {
            return "UTF-8";
        }

        String utf8Str = new String(bytes, StandardCharsets.UTF_8);
        if (containsMojibake(utf8Str)) {
            byte[] gbkBytes = utf8Str.getBytes(StandardCharsets.ISO_8859_1);
            if (isValidUtf8(gbkBytes)) {
                return "GBK";
            }
        }

        return "UNKNOWN";
    }
}