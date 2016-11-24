package org.wltea.analyzer.pinyin.solr;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.wltea.analyzer.pinyin.lucene.PinyinTokenFilter;
import org.wltea.analyzer.utils.Constant;

import java.util.Map;

/**
 * PinyinTokenFilter工厂类
 * 兼容Solr6.x
 *
 * @author Lanxiaowei
 */
public class PinyinTokenFilterFactory extends TokenFilterFactory {
    /**
     * 是否输出原中文
     */
    private boolean outChinese;
    /**
     * 是否只转换简拼
     */
    private boolean shortPinyin;
    /**
     * 是否转换全拼+简拼
     */
    private boolean pinyinAll;
    /**
     * 中文词组长度过滤，默认超过minTermLength长度的中文才转换拼音
     */
    private int minTermLength;

    public PinyinTokenFilterFactory(Map<String, String> args) {
        super(args);
        this.outChinese = getBoolean(args, "outChinese", Constant.DEFAULT_OUT_CHINESE);
        this.shortPinyin = getBoolean(args, "shortPinyin", Constant.DEFAULT_SHORT_PINYIN);
        this.pinyinAll = getBoolean(args, "pinyinAll", Constant.DEFAULT_PINYIN_ALL);
        this.minTermLength = getInt(args, "minTermLength", Constant.DEFAULT_MIN_TERM_LRNGTH);
    }

    public TokenFilter create(TokenStream input) {
        return new PinyinTokenFilter(input, this.shortPinyin, this.outChinese,
                this.minTermLength);
    }

    public boolean isOutChinese() {
        return outChinese;
    }

    public void setOutChinese(boolean outChinese) {
        this.outChinese = outChinese;
    }

    public boolean isShortPinyin() {
        return shortPinyin;
    }

    public void setShortPinyin(boolean shortPinyin) {
        this.shortPinyin = shortPinyin;
    }

    public boolean isPinyinAll() {
        return pinyinAll;
    }

    public void setPinyinAll(boolean pinyinAll) {
        this.pinyinAll = pinyinAll;
    }

    public int getMinTermLength() {


        return minTermLength;
    }

    public void setMinTermLength(int minTermLength) {
        this.minTermLength = minTermLength;
    }
}
