package org.wltea.analyzer.utils;

public class StringUtils {
    public static boolean containsChinese(String s) {
        if ((s == null) || ("".equals(s.trim()))) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (isChinese(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isChinese(char a) {
        int v = a;
        return (v >= 19968) && (v <= 171941);
    }

    /**
     * 是否全部是汉字[包含一个非汉字字符即返回false]
     *
     * @param strName
     * @return
     */
    public static boolean isChinese(String strName) {
        char[] ch = strName.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (!isChinese(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串中是否含有中文
     *
     * @param s
     * @return
     */
    public static int chineseCharCount(String s) {
        int count = 0;
        if ((null == s) || ("".equals(s.trim())))
            return count;
        for (int i = 0; i < s.length(); i++) {
            if (isChinese(s.charAt(i)))
                count++;
        }
        return count;
    }

    /**
     * 判断字符串是否由纯数字组成
     *
     * @param str
     * @return
     */
    public static boolean isNumeric(String str) {
        boolean flag = true;

        if (null == str || str.length() == 0) {
            flag = false;
        } else {
            for (int i = str.length(); --i >= 0; ) {
                if (!Character.isDigit(str.charAt(i))) {
                    flag = false;
                }
            }
        }
        return flag;
    }
}
