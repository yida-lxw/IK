package org.wltea.analyzer.pinyin.lucene;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.wltea.analyzer.utils.Constant;
import org.wltea.analyzer.utils.Pinyin4jUtil;
import org.wltea.analyzer.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * 拼音过滤器[负责将汉字转换为拼音]
 * 兼容Lucene 8.x版本
 *
 * @author Lanxiaowei
 */
public class PinyinTokenFilter extends TokenFilter {
    /**
     * 是否输出原中文
     */
    private boolean isOutChinese;
    /**
     * 是否只转换简拼
     */
    private boolean shortPinyin;
    /**
     * 是否转换全拼+简拼
     */
    private boolean pinyinAll;
    /**
     * 中文词组长度过滤，默认超过2位长度的中文才转换拼音
     */
    private int minTermLength;

    /**
     * 词元输入缓存
     */
    private char[] curTermBuffer;
    /**
     * 词元输入长度
     */
    private int curTermLength;

    private final CharTermAttribute termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);
    /**
     * 位置增量属性
     */
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    /**
     * 当前输入是否已输出
     */
    private boolean hasCurOut;
    /**
     * 拼音结果集
     */
    private Collection<String> terms;
    /**
     * 拼音结果集迭代器
     */
    private Iterator<String> termIte;

    public PinyinTokenFilter(TokenStream input) {
        this(input, Constant.DEFAULT_MIN_TERM_LRNGTH);
    }

    public PinyinTokenFilter(TokenStream input, int minTermLength) {
        this(input, Constant.DEFAULT_SHORT_PINYIN, Constant.DEFAULT_PINYIN_ALL, minTermLength);
    }

    public PinyinTokenFilter(TokenStream input, boolean shortPinyin) {
        this(input, shortPinyin, Constant.DEFAULT_PINYIN_ALL);
    }

    public PinyinTokenFilter(TokenStream input, boolean shortPinyin, boolean pinyinAll) {
        this(input, shortPinyin, pinyinAll, Constant.DEFAULT_MIN_TERM_LRNGTH);
    }

    public PinyinTokenFilter(TokenStream input, boolean shortPinyin, boolean pinyinAll, int minTermLength) {
        this(input, shortPinyin, pinyinAll, Constant.DEFAULT_OUT_CHINESE, minTermLength);
    }

    public PinyinTokenFilter(TokenStream input, boolean shortPinyin, boolean pinyinAll,
                             boolean isOutChinese, int minTermLength) {
        super(input);
        this.minTermLength = minTermLength;
        if (this.minTermLength < 1) {
            this.minTermLength = 1;
        }
        this.isOutChinese = isOutChinese;
        this.shortPinyin = shortPinyin;
        this.pinyinAll = pinyinAll;
        // 偏移量属性
        addAttribute(OffsetAttribute.class);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        while (true) {
            // 开始处理或上一输入词元已被处理完成
            if (this.curTermBuffer == null) {
                // 获取下一词元输入
                if (!this.input.incrementToken()) {
                    // 没有后继词元输入，处理完成，返回false，结束上层调用
                    return false;
                }
                // 缓存词元输入
                this.curTermBuffer = ((char[]) this.termAtt.buffer().clone());
                this.curTermLength = this.termAtt.length();
            }
            String chinese = this.termAtt.toString();
            // 处理原输入词元
            if ((this.isOutChinese) && (!this.hasCurOut) && (this.termIte == null)) {
                // 准许输出原中文词元且当前没有输出原输入词元且还没有处理拼音结果集
                // 标记以保证下次循环不会输出
                this.hasCurOut = true;
                // 写入原输入词元
                this.termAtt.copyBuffer(this.curTermBuffer, 0,
                        this.curTermLength);
                this.posIncrAtt.setPositionIncrement(this.posIncrAtt.getPositionIncrement());
                this.typeAtt.setType(StringUtils.isNumeric(chinese) ? "numeric_original" :
                        (StringUtils.containsChinese(chinese) ? "chinese_original" : "normal_word"));
                return true;
            }

            String type = this.typeAtt.type();
            // 若包含中文且中文字符长度不小于限定的最小长度minTermLength
            if (StringUtils.chineseCharCount(chinese) >= this.minTermLength) {
                // 如果需要全拼+简拼
                if (this.pinyinAll) {
                    Collection<String> quanpinColl = Pinyin4jUtil.getPinyinCollection(chinese);
                    quanpinColl.addAll(Pinyin4jUtil.getPinyinShortCollection(chinese));
                    this.terms = quanpinColl;
                } else {
                    // 简拼 or 全拼，二选一
                    this.terms = this.shortPinyin ?
                            Pinyin4jUtil.getPinyinShortCollection(chinese) :
                            Pinyin4jUtil.getPinyinCollection(chinese);
                }

                if (this.terms != null) {
                    this.termIte = this.terms.iterator();
                }
            } else {
                if (null != type && ("numeric_original".equals(type) ||
                        "normal_word".equals(type))) {
                    Collection<String> coll = new ArrayList<String>();
                    coll.add(chinese);
                    this.terms = coll;
                    if (this.terms != null) {
                        this.termIte = this.terms.iterator();
                    }
                }
            }
            if (this.termIte != null) {
                // 有拼音结果集且未处理完成
                while (this.termIte.hasNext()) {
                    String pinyin = this.termIte.next();
                    this.termAtt.copyBuffer(pinyin.toCharArray(), 0, pinyin.length());
                    //同义词的原理
                    this.posIncrAtt.setPositionIncrement(0);
                    this.typeAtt.setType(this.shortPinyin ? "short_pinyin" : "pinyin");
                    return true;
                }
            }
            // 没有中文或转换拼音失败，不用处理，
            // 清理缓存，下次取新词元
            this.curTermBuffer = null;
            this.termIte = null;
            this.hasCurOut = false;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
    }
}
