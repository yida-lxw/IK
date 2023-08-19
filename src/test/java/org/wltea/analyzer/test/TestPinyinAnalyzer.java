package org.wltea.analyzer.test;

import org.wltea.analyzer.pinyin.lucene.PinyinAnalyzer;
import org.wltea.analyzer.utils.AnalyzerUtils;

import java.io.IOException;

/**
 * Created by Lanxiaowei
 * Craated on 2016/11/24 19:25
 * 测试拼音分词器
 */
public class TestPinyinAnalyzer {
    public static void main(String[] args) throws IOException {
        String text = "渣男陈赫为什么总是上头条，污染了我的眼睛知道吗？";
        PinyinAnalyzer analyzer = new PinyinAnalyzer(false, false);
        //开启长拼音模式
        analyzer.setShortPinyin(false);
        //false=只返回全拼或简拼(至于到底是返回全拼还是简拼，由shortPinyin变量控制), true=全拼+简拼
        analyzer.setPinyinAll(false);
        AnalyzerUtils.displayTokens(analyzer, text);
    }
}
