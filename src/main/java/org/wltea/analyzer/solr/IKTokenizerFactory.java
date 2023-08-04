package org.wltea.analyzer.solr;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;
import org.wltea.analyzer.lucene.IKTokenizer;

import java.util.Map;

/**
 * IKTokenizer工厂类，兼容Lucene&Solr6.x
 */
public class IKTokenizerFactory extends TokenizerFactory {
    public IKTokenizerFactory(Map<String, String> args) {
        super(args);
        //默认细粒度切分
        useSmart = getBoolean(args, "useSmart", false);
    }

    private boolean useSmart;

    @Override
    public Tokenizer create(AttributeFactory attributeFactory) {
        Tokenizer tokenizer = new IKTokenizer(attributeFactory, useSmart);
        return tokenizer;
    }
}
