package org.wltea.analyzer.pinyin.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.wltea.analyzer.lucene.IKTokenizer;
import org.wltea.analyzer.utils.Constant;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * 自定义拼音分词器
 * 兼容Lucene6.x
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
     * 是否开启edgesNGram模式
     */
    private boolean edgesNGram;

    public PinyinAnalyzer() {
        this(Constant.DEFAULT_IK_USE_SMART);
    }

    public PinyinAnalyzer(boolean useSmart) {
        this(Constant.DEFAULT_MIN_GRAM, Constant.DEFAULT_MAX_GRAM, Constant.DEFAULT_EDGES_GRAM, useSmart, Constant.DEFAULT_NGRAM_CHINESE);
    }

    public PinyinAnalyzer(int minGram) {
        this(minGram, Constant.DEFAULT_MAX_GRAM, Constant.DEFAULT_EDGES_GRAM, Constant.DEFAULT_IK_USE_SMART, Constant.DEFAULT_NGRAM_CHINESE, Constant.DEFAULT_NGRAM_NUMBER);
    }

    public PinyinAnalyzer(int minGram, boolean useSmart) {
        this(minGram, Constant.DEFAULT_MAX_GRAM, Constant.DEFAULT_EDGES_GRAM, useSmart, Constant.DEFAULT_NGRAM_CHINESE);
    }

    public PinyinAnalyzer(int minGram, int maxGram) {
        this(minGram, maxGram, Constant.DEFAULT_EDGES_GRAM);
    }

    public PinyinAnalyzer(int minGram, int maxGram, boolean edgesNGram) {
        this(minGram, maxGram, edgesNGram, Constant.DEFAULT_IK_USE_SMART);
    }

    public PinyinAnalyzer(int minGram, int maxGram, boolean edgesNGram, boolean useSmart) {
        this(minGram, maxGram, edgesNGram, useSmart, Constant.DEFAULT_NGRAM_CHINESE);
    }

    public PinyinAnalyzer(int minGram, int maxGram, boolean edgesNGram, boolean useSmart,
                          boolean nGramChinese) {
        this(minGram, maxGram, edgesNGram, useSmart, nGramChinese, Constant.DEFAULT_NGRAM_NUMBER);
    }

    public PinyinAnalyzer(int minGram, int maxGram, boolean edgesNGram, boolean useSmart,
                          boolean nGramChinese, boolean nGramNumber) {
        super();
        this.minGram = minGram;
        this.maxGram = maxGram;
        this.edgesNGram = edgesNGram;
        this.useSmart = useSmart;
        this.nGramChinese = nGramChinese;
        this.nGramNumber = nGramNumber;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        //Reader reader = new BufferedReader(new StringReader(fieldName));
        Tokenizer tokenizer = new IKTokenizer(useSmart);
        //转拼音
        TokenStream tokenStream = new PinyinTokenFilter(tokenizer,
                Constant.DEFAULT_SHORT_PINYIN, Constant.DEFAULT_PINYIN_ALL, Constant.DEFAULT_MIN_TERM_LRNGTH);
        //对拼音进行NGram处理
        if (edgesNGram) {
            tokenStream = new PinyinEdgeNGramTokenFilter(tokenStream, this.minGram,
                    this.maxGram, this.nGramChinese, this.nGramNumber);
        } else {
            tokenStream = new PinyinNGramTokenFilter(tokenStream, this.minGram,
                    this.maxGram, this.nGramChinese, this.nGramNumber);
        }
        return new TokenStreamComponents(tokenizer, tokenStream);
    }
}
