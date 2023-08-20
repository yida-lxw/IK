package org.wltea.analyzer.pinyin.solr;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.wltea.analyzer.pinyin.lucene.PinyinEdgeNGramTokenFilter;
import org.wltea.analyzer.pinyin.lucene.PinyinNGramTokenFilter;
import org.wltea.analyzer.utils.Constant;

import java.util.Map;

/**
 * PinyinNGramTokenFilter工厂类
 * 兼容Lucene 4.x
 *
 * @author Lanxiaowei
 */
public class PinyinNGramTokenFilterFactory extends TokenFilterFactory {
    private int minGram;
    private int maxGram;
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

    public PinyinNGramTokenFilterFactory(Map<String, String> args) {
        super(args);

        this.minGram = getInt(args, "minGram", Constant.DEFAULT_MIN_GRAM);
        this.maxGram = getInt(args, "maxGram", Constant.DEFAULT_MAX_GRAM);
        this.edgesNGram = getBoolean(args, "edgesNGram", Constant.DEFAULT_EDGES_NGRAM);
        this.nGramChinese = getBoolean(args, "nGramChinese", Constant.DEFAULT_NGRAM_CHINESE);
        this.nGramNumber = getBoolean(args, "nGramNumber", Constant.DEFAULT_NGRAM_NUMBER);
    }

    public TokenFilter create(TokenStream input) {
        if (edgesNGram) {
            return new PinyinEdgeNGramTokenFilter(input, this.minGram, this.maxGram,
                    this.nGramChinese, this.nGramNumber);
        }
        return new PinyinNGramTokenFilter(input, this.minGram, this.maxGram,
                this.nGramChinese, this.nGramNumber);
    }
}