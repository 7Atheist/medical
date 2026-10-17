package com.geriatric.assessment.util;

import java.time.LocalDate;
import java.time.Period;

/**
 * 中国大陆居民身份证号（18位，GB 11643-1999）解析与校验工具。
 *
 * <p>方案要求：以"姓名＋身份证号"实名登录后，自动从身份证号解析出生日期、
 * 年龄、性别，构建老年用户画像。本工具负责：</p>
 * <ul>
 *   <li>格式与校验位合法性校验（避免乱输）</li>
 *   <li>解析出生日期（第7~14位）</li>
 *   <li>解析性别（第17位：奇数为男、偶数为女）</li>
 *   <li>计算年龄</li>
 * </ul>
 */
public final class IdCardUtil {

    /** 前17位加权因子（GB 11643-1999） */
    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    /** 校验码映射表 */
    private static final char[] CHECK_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private IdCardUtil() {
    }

    /**
     * 校验18位身份证号格式与校验位是否合法。
     *
     * @return true 合法，false 不合法
     */
    public static boolean isValid(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return false;
        }
        String num = idCard.substring(0, 17);
        for (char c : num.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        // 校验出生日期合法性
        try {
            LocalDate birth = parseBirthDate(idCard);
            if (birth.isAfter(LocalDate.now())) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        // 校验位校验
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (num.charAt(i) - '0') * WEIGHT[i];
        }
        char expected = CHECK_CODE[sum % 11];
        char actual = Character.toUpperCase(idCard.charAt(17));
        return expected == actual;
    }

    /** 解析出生日期（第7~14位：YYYYMMDD） */
    public static LocalDate parseBirthDate(String idCard) {
        String y = idCard.substring(6, 10);
        String m = idCard.substring(10, 12);
        String d = idCard.substring(12, 14);
        return LocalDate.of(Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d));
    }

    /** 解析性别：1男，2女（第17位奇数=男，偶数=女） */
    public static int parseGender(String idCard) {
        int digit = idCard.charAt(16) - '0';
        return (digit % 2 == 1) ? 1 : 2;
    }

    /** 按当前日期计算年龄（周岁） */
    public static int calcAge(LocalDate birth) {
        return Period.between(birth, LocalDate.now()).getYears();
    }

    /**
     * 身份证号脱敏展示：保留前6位与后4位，中间以 * 代替。
     * 例如 440102199001011234 -> 440102********1234
     */
    public static String mask(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard == null ? "" : idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
}
