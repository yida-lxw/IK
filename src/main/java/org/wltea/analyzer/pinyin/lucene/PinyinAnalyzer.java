package org.wltea.analyzer.pinyin.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.wltea.analyzer.lucene.IKTokenizer;
import org.wltea.analyzer.utils.Constant;

/**
 * 自定义拼音分词器
 * 兼容Lucene 8.x
 *
 * @author Lanxiaowei
 */
public class PinyinAnalyzer extends Analyzer {
    private int minGram;
    private int maxGram;
    private boolean useSmart;
    /**
     * 是否需要对中文进行NGram[默认为false]
     */
    private boolean nGramChinese;
    /**
     * 是否需要对纯数字进行NGram[默认为false]
     */
    private boolean nGramNumber;
    /**
     * 是否开启edgesNGram模式(若不开启edgesNGram模式，则默认就是NGram模式)
     */
    private boolean edgesNGram;

    /**
     * 是否开启NGram或edgesNGram，若不开启，则只汉字转拼音，不进行NGram和edgesNGram切分
     */
    private boolean enableNGramOrEdgesNGram;

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
     * 中文词组长度过滤，默认超过2位长度的中文才转换拼音
     */
    private int minTermLength;

    public PinyinAnalyzer() {
        this(Constant.DEFAULT_IK_USE_SMART);
    }

    public PinyinAnalyzer(boolean useSmart) {
        this(useSmart, Constant.DEFAULT_ENABLE_EDGES_NGRAM_OR_NGRAM);
    }

    public PinyinAnalyzer(boolean useSmart, boolean enableNGramOrEdgesNGram) {
        this(useSmart, enableNGramOrEdgesNGram, Constant.DEFAULT_EDGES_NGRAM);
    }

    public PinyinAnalyzer(boolean useSmart, boolean enableNGramOrEdgesNGram, boolean edgesNGram) {
        this(Constant.DEFAULT_MIN_GRAM, Constant.DEFAULT_MAX_GRAM, enableNGramOrEdgesNGram, edgesNGram, useSmart);
    }

    public PinyinAnalyzer(int minGram) {
        this(minGram, Constant.DEFAULT_IK_USE_SMART);
    }

    public PinyinAnalyzer(int minGram, boolean useSmart) {
        this(minGram, Constant.DEFAULT_MAX_GRAM, Constant.DEFAULT_ENABLE_EDGES_NGRAM_OR_NGRAM, Constant.DEFAULT_EDGES_NGRAM, useSmart);
    }

    public PinyinAnalyzer(int minGram, int maxGram, boolean enableNGramOrEdgesNGram) {
        this(minGram, maxGram, enableNGramOrEdgesNGram, Constant.DEFAULT_EDGES_NGRAM);
    }

    public PinyinAnalyzer(int minGram, int maxGram, boolean enableNGramOrEdgesNGram, boolean edgesNGram) {
        this(minGram, maxGram, enableNGramOrEdgesNGram, edgesNGram, Constant.DEFAULT_IK_USE_SMART);
    }

    public PinyinAnalyzer(int minGram, int maxGram, boolean enableNGramOrEdgesNGram, boolean edgesNGram, boolean useSmart) {
        this(minGram, maxGram, enableNGramOrEdgesNGram, edgesNGram, useSmart, Constant.DEFAULT_NGRAM_CHINESE);
    }

    public PinyinAnalyzer(int minGram, int maxGram, boolean enableNGramOrEdgesNGram, boolean edgesNGram, boolean useSmart,
                          boolean nGramChinese) {
        this(minGram, maxGram, enableNGramOrEdgesNGram, edgesNGram, useSmart, nGramChinese, Constant.DEFAULT_NGRAM_NUMBER);
    }

    public PinyinAnalyzer(int minGram, int maxGram, boolean enableNGramOrEdgesNGram, boolean edgesNGram,
                          boolean useSmart,
                          boolean nGramChinese, boolean nGramNumber) {
        super();
        this.minGram = minGram;
        this.maxGram = maxGram;
        this.enableNGramOrEdgesNGram = enableNGramOrEdgesNGram;
        this.edgesNGram = edgesNGram;
        this.useSmart = useSmart;
        this.nGramChinese = nGramChinese;
        this.nGramNumber = nGramNumber;
        this.outChinese = Constant.DEFAULT_OUT_CHINESE;
        this.shortPinyin = Constant.DEFAULT_SHORT_PINYIN;
        this.pinyinAll = Constant.DEFAULT_PINYIN_ALL;
        this.minTermLength = Constant.DEFAULT_MIN_TERM_LRNGTH;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        //Reader reader = new BufferedReader(new StringReader(fieldName));
        Tokenizer tokenizer = new IKTokenizer(useSmart);
        //转拼音
        TokenStream tokenStream = new PinyinTokenFilter(tokenizer, this.shortPinyin, this.pinyinAll, this.outChinese,
                this.minTermLength);
        //NGram和EdgeNGram开关，若设置为false即未启用，则不会进一步进行NGram或EdgeNGram切分处理
        if (enableNGramOrEdgesNGram) {
            //对拼音进行NGram处理
            if (edgesNGram) {
                tokenStream = new PinyinEdgeNGramTokenFilter(tokenStream, this.minGram,
                        this.maxGram, this.nGramChinese, this.nGramNumber);
            } else {
                tokenStream = new PinyinNGramTokenFilter(tokenStream, this.minGram,
                        this.maxGram, this.nGramChinese, this.nGramNumber);
            }
        }
        return new TokenStreamComponents(tokenizer, tokenStream);
    }

    public int getMinGram() {
        return minGram;
    }

    public void setMinGram(int minGram) {
        this.minGram = minGram;
    }

    public int getMaxGram() {
        return maxGram;
    }

    public void setMaxGram(int maxGram) {
        this.maxGram = maxGram;
    }

    public boolean isUseSmart() {
        return useSmart;
    }

    public void setUseSmart(boolean useSmart) {
        this.useSmart = useSmart;
    }

    public boolean isnGramChinese() {
        return nGramChinese;
    }

    public void setnGramChinese(boolean nGramChinese) {
        this.nGramChinese = nGramChinese;
    }

    public boolean isnGramNumber() {
        return nGramNumber;
    }

    public void setnGramNumber(boolean nGramNumber) {
        this.nGramNumber = nGramNumber;
    }

    public boolean isEdgesNGram() {
        return edgesNGram;
    }

    public void setEdgesNGram(boolean edgesNGram) {
        this.edgesNGram = edgesNGram;
    }

    public boolean isEnableNGramOrEdgesNGram() {
        return enableNGramOrEdgesNGram;
    }

    public void setEnableNGramOrEdgesNGram(boolean enableNGramOrEdgesNGram) {
        this.enableNGramOrEdgesNGram = enableNGramOrEdgesNGram;
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