package org.wltea.analyzer.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.*;

/**
 * pinyin4j工具类[pinyin4j并不能自动联系上下文判断多音字的正确拼音，
 * 如：重庆，不能返回chongqing,只能返回zhongqing和chongqing，让用户自己人工去选择正确拼音]
 *
 * @author Lanxiaowei
 */
public class Pinyin4jUtil {
    private static final String SEPERATOR = ",";
    /**
     * 大写形式
     */
    private static final boolean UPPER_CASE = false;
    /**
     * 音调输出形式
     */
    private static final int TONE_TYPE = 0;

    private static final int V_CHAR_TYPE = 0;

    /**
     * 汉字转拼音(全拼)
     *
     * @param chinese   汉字
     * @param upperCase 是否转成大写
     * @param toneType  是否输出声调
     * @param vCharType u字母输出类型
     * @param seperator 多个拼音分隔符
     * @return
     */
    public static String getPinyin(String chinese, boolean upperCase,
                                   int toneType, int vCharType, String seperator) {
        StringBuffer pinyinName = new StringBuffer();
        char[] nameChar = chinese.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = getOutputFormat(upperCase, toneType, vCharType);
        for (int i = 0; i < nameChar.length; i++) {
            if (nameChar[i] > 128) {
                try {
                    // 取得当前汉字的所有全拼  
                    String[] strs = PinyinHelper.toHanyuPinyinStringArray(
                            nameChar[i], defaultFormat);
                    if (strs != null) {
                        for (int j = 0; j < strs.length; j++) {
                            pinyinName.append(strs[j]);
                            if (j != strs.length - 1) {
                                pinyinName.append(seperator);
                            }
                        }
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            } else {
                pinyinName.append(nameChar[i]);
            }
            pinyinName.append(" ");
        }
        return parseTheChineseByObject(discountTheChinese(pinyinName.toString(), seperator), seperator);
    }

    /**
     * 汉字转拼音(全拼)
     *
     * @param chinese   汉字
     * @param upperCase 是否转成大写
     * @param toneType  是否输出声调
     * @param vCharType u字母输出类型
     * @return
     */
    public static String getPinyin(String chinese, boolean upperCase,
                                   int toneType, int vCharType) {
        return getPinyin(chinese, upperCase, toneType, vCharType, SEPERATOR);
    }

    /**
     * 汉字转拼音(全拼)
     *
     * @param chinese   汉字
     * @param upperCase 是否转成大写
     * @param toneType  是否输出声调
     * @return
     */
    public static String getPinyin(String chinese, boolean upperCase,
                                   int toneType) {
        return getPinyin(chinese, upperCase, toneType, V_CHAR_TYPE, SEPERATOR);
    }

    /**
     * 汉字转拼音(全拼)
     *
     * @param chinese   汉字
     * @param upperCase 是否转成大写
     * @return
     */
    public static String getPinyin(String chinese, boolean upperCase) {
        return getPinyin(chinese, upperCase, TONE_TYPE, V_CHAR_TYPE, SEPERATOR);
    }

    /**
     * 汉字转拼音(全拼)
     *
     * @param chinese   汉字
     * @param seperator 分隔符
     * @return
     */
    public static String getPinyin(String chinese, String seperator) {
        return getPinyin(chinese, UPPER_CASE, TONE_TYPE, V_CHAR_TYPE, seperator);
    }

    /**
     * 汉字转拼音(全拼)
     *
     * @param chinese 汉字
     * @return
     */
    public static String getPinyin(String chinese) {
        return getPinyin(chinese, UPPER_CASE, TONE_TYPE, V_CHAR_TYPE, SEPERATOR);
    }

    /**
     * 获取汉字简拼
     *
     * @param chinese   汉字
     * @param upperCase 是否转成大写
     * @param seperator 分隔符
     * @return
     */
    public static String getPinyinShort(String chinese, boolean upperCase, String seperator) {
        StringBuffer pinyinName = new StringBuffer();
        char[] nameChar = chinese.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = getOutputFormat(upperCase);
        try {
            for (int i = 0; i < nameChar.length; i++) {
                if (nameChar[i] > 128) {
                    // 取得当前汉字的所有全拼
                    String[] strs = PinyinHelper.toHanyuPinyinStringArray(
                            nameChar[i], defaultFormat);
                    if (strs != null) {
                        for (int j = 0; j < strs.length; j++) {
                            // 取首字母  
                            pinyinName.append(strs[j].charAt(0));
                            if (j != strs.length - 1) {
                                pinyinName.append(seperator);
                            }
                        }
                    }
                } else {
                    pinyinName.append(nameChar[i]);
                }
                pinyinName.append(" ");
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            e.printStackTrace();
        }
        return parseTheChineseByObject(discountTheChinese(pinyinName.toString(), seperator), seperator);
    }

    /**
     * 获取汉字简拼
     *
     * @param chinese   全拼拼音
     * @param upperCase 是否转成大写
     * @return
     */
    public static String getPinyinShort(String chinese, boolean upperCase) {
        return getPinyinShort(chinese, upperCase, SEPERATOR);
    }

    /**
     * 获取汉字简拼
     *
     * @param chinese 全拼拼音
     * @return
     */
    public static String getPinyinShort(String chinese) {
        return getPinyinShort(chinese, UPPER_CASE, SEPERATOR);
    }

    /**
     * 获取汉字全拼
     *
     * @param chinese
     * @param upperCase
     * @param toneType
     * @param vCharType
     * @return
     */
    public static Collection<String> getPinyinCollection(String chinese, boolean upperCase,
                                                         int toneType, int vCharType) {
        List<String[]> pinyinList = new ArrayList<String[]>();
        HanyuPinyinOutputFormat defaultFormat = getOutputFormat(upperCase, toneType, vCharType);
        try {
            for (int i = 0; i < chinese.length(); i++) {
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(
                        chinese.charAt(i), defaultFormat);
                if (pinyinArray != null && pinyinArray.length > 0) {
                    pinyinList.add(pinyinArray);
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            e.printStackTrace();
        }

        Set<String> pinyins = null;
        for (String[] array : pinyinList) {
            if (pinyins == null || pinyins.isEmpty()) {
                pinyins = new HashSet<String>();
                for (String charPinpin : array) {
                    pinyins.add(charPinpin);
                }
            } else {
                Set<String> pres = pinyins;
                pinyins = new HashSet<String>();
                for (String pre : pres) {
                    for (String charPinyin : array) {
                        pinyins.add(pre + charPinyin);
                    }
                }
            }
        }
        return pinyins;
    }

    public static Collection<String> getPinyinCollection(String chinese, boolean upperCase,
                                                         int toneType) {
        return getPinyinCollection(chinese, upperCase, toneType, V_CHAR_TYPE);
    }

    public static Collection<String> getPinyinCollection(String chinese, boolean upperCase) {
        return getPinyinCollection(chinese, upperCase, TONE_TYPE, V_CHAR_TYPE);
    }

    public static Collection<String> getPinyinCollection(String chinese) {
        return getPinyinCollection(chinese, UPPER_CASE, TONE_TYPE, V_CHAR_TYPE);
    }

    public static Collection<String> getPinyinShortCollection(String chinese, boolean upperCase,
                                                              int toneType, int vCharType) {
        List<String[]> pinyinList = new ArrayList<String[]>();
        HanyuPinyinOutputFormat defaultFormat = getOutputFormat(upperCase, toneType, vCharType);
        try {
            for (int i = 0; i < chinese.length(); i++) {
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(
                        chinese.charAt(i), defaultFormat);
                if (pinyinArray != null && pinyinArray.length > 0) {
                    pinyinList.add(pinyinArray);
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            e.printStackTrace();
        }
        Set<String> sorts = new HashSet<String>();
        Set<String> pinyins = null;
        for (String[] array : pinyinList) {
            if (pinyins == null || pinyins.isEmpty()) {
                pinyins = new HashSet<String>();
                for (String charPinpin : array) {
                    pinyins.add(charPinpin.substring(0, 1));
                    sorts.add(charPinpin.substring(0, 1));
                }
            } else {
                Set<String> pres = pinyins;
                pinyins = new HashSet<String>();
                for (String pre : pres) {
                    for (String charPinyin : array) {
                        pinyins.add(pre + charPinyin.substring(0, 1));
                        sorts.add(pre + charPinyin.substring(0, 1));
                    }
                }
            }
        }
        return sorts;
    }

    public static Collection<String> getPinyinShortCollection(String chinese, boolean upperCase,
                                                              int toneType) {
        return getPinyinShortCollection(chinese, upperCase, toneType, V_CHAR_TYPE);
    }

    public static Collection<String> getPinyinShortCollection(String chinese, boolean upperCase) {
        return getPinyinShortCollection(chinese, upperCase, TONE_TYPE, V_CHAR_TYPE);
    }

    public static Collection<String> getPinyinShortCollection(String chinese) {
        return getPinyinShortCollection(chinese, UPPER_CASE, TONE_TYPE, V_CHAR_TYPE);
    }

    /**
     * Default Format 默认输出格式
     *
     * @return
     */
    public static HanyuPinyinOutputFormat getOutputFormat() {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);// 小写
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);// 不显示音调
        format.setVCharType(HanyuPinyinVCharType.WITH_V);// V显示
        return format;
    }

    public static HanyuPinyinOutputFormat getOutputFormat(boolean upperCase) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(upperCase ? HanyuPinyinCaseType.UPPERCASE
                : HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);// 不显示音调
        format.setVCharType(HanyuPinyinVCharType.WITH_V);// V显示
        return format;
    }

    public static HanyuPinyinOutputFormat getOutputFormat(boolean upperCase,
                                                          int toneType, int vCharType) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(upperCase ? HanyuPinyinCaseType.UPPERCASE
                : HanyuPinyinCaseType.LOWERCASE);
        if (toneType == 0) {
            format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        } else if (toneType == 1) {
            format.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        } else if (toneType == 2) {
            format.setToneType(HanyuPinyinToneType.WITH_TONE_NUMBER);
        } else {
            format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        }

        if (vCharType == 0) {
            format.setVCharType(HanyuPinyinVCharType.WITH_V);
        } else if (vCharType == 1) {
            format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
        } else if (vCharType == 2) {
            format.setVCharType(HanyuPinyinVCharType.WITH_U_AND_COLON);
        } else {
            format.setVCharType(HanyuPinyinVCharType.WITH_V);
        }
        return format;
    }

    /**
     * 判断字符串是否全部由数字组成
     *
     * @param str
     * @return
     */
    public static boolean isNumeric(String str) {
        if (null == str || str.length() == 0) {
            return false;
        }
        for (int i = str.length(); --i >= 0; ) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否全部由数字组成
     *
     * @param str
     * @return
     */
    public static boolean isLetter(String str) {
        if (null == str || str.length() == 0) {
            return false;
        }
        for (int i = str.length(); --i >= 0; ) {
            if (!Character.isAlphabetic(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<Map<String, Integer>> discountTheChinese(String theStr, String seperator) {
        // 去除重复拼音后的拼音列表
        List<Map<String, Integer>> mapList = new ArrayList<Map<String, Integer>>();
        // 用于处理每个字的多音字，去掉重复
        Map<String, Integer> onlyOne = null;
        String[] firsts = theStr.split(" ");
        // 读出每个汉字的拼音
        for (String str : firsts) {
            onlyOne = new Hashtable<String, Integer>();
            String[] china = str.split(seperator);
            // 多音字处理
            for (String s : china) {
                Integer count = onlyOne.get(s);
                if (count == null) {
                    onlyOne.put(s, new Integer(1));
                } else {
                    onlyOne.remove(s);
                    count++;
                    onlyOne.put(s, count);
                }
            }
            mapList.add(onlyOne);
        }
        return mapList;
    }

    /**
     * 解析并组合拼音，对象合并方案
     *
     * @return
     */
    private static String parseTheChineseByObject(List<Map<String, Integer>> list, String seperator) {
        // 用于统计每一次,集合组合数据
        Map<String, Integer> first = null;
        // 遍历每一组集合
        for (int i = 0; i < list.size(); i++) {
            // 每一组集合与上一次组合的Map
            Map<String, Integer> temp = new Hashtable<String, Integer>();
            // 第一次循环，first为空
            if (first != null) {
                // 取出上次组合与此次集合的字符，并保存
                for (String s : first.keySet()) {
                    for (String s1 : list.get(i).keySet()) {
                        String str = s + s1;
                        temp.put(str, 1);
                    }
                }
                // 清理上一次组合数据
                if (temp != null && temp.size() > 0) {
                    first.clear();
                }
            } else {
                for (String s : list.get(i).keySet()) {
                    String str = s;
                    temp.put(str, 1);
                }
            }
            // 保存组合数据以便下次循环使用
            if (temp != null && temp.size() > 0) {
                first = temp;
            }
        }
        String returnStr = "";
        if (first != null) {
            // 遍历取出组合字符串
            for (String str : first.keySet()) {
                returnStr += (str + seperator);
            }
        }
        if (returnStr.length() > 0) {
            returnStr = returnStr.substring(0, returnStr.length() - 1);
        }
        return returnStr;
    }

    public static void main(String[] args) {
        String s = "重庆";
        String pinyin = getPinyin(s, ",");
        System.out.println(pinyin);
        String jianpin = getPinyinShort(s, true, ",");
        System.out.println(jianpin);

        Collection<String> collection = getPinyinCollection(s);
        for (String str : collection) {
            System.out.println(str);
        }
    }
}
