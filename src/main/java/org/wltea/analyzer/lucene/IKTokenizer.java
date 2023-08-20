/**
 * IK 中文分词  版本 5.0.1
 * IK Analyzer release 5.0.1
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 */
package org.wltea.analyzer.lucene;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.IOException;

/**
 * IKTokenizer
 * 兼容Lucene 6.x版本
 */
public final class IKTokenizer extends Tokenizer {

    //IK分词器实现
    private IKSegmenter _IKImplement;
    //词元文本属性
    private final CharTermAttribute termAtt;
    //词元位移属性
    private final OffsetAttribute offsetAtt;
    //词元分类属性（该属性分类参考org.wltea.analyzer.core.Lexeme中的分类常量）
    private final TypeAttribute typeAtt;
    //记录最后一个词元的结束位置
    private int endPosition;

    private Version version = Version.LATEST;

    public IKTokenizer() {
        //默认细粒度切分算法
        this(false);
    }

    public IKTokenizer(boolean useSmart) {
        offsetAtt = addAttribute(OffsetAttribute.class);
        termAtt = addAttribute(CharTermAttribute.class);
        typeAtt = addAttribute(TypeAttribute.class);
        _IKImplement = new IKSegmenter(input, useSmart);
    }

    public IKTokenizer(AttributeFactory factory, boolean useSmart) {
        super(factory);
        offsetAtt = addAttribute(OffsetAttribute.class);
        termAtt = addAttribute(CharTermAttribute.class);
        typeAtt = addAttribute(TypeAttribute.class);
        _IKImplement = new IKSegmenter(input, useSmart);
    }

    @Override
    public boolean incrementToken() throws IOException {
        //清除所有的词元属性
        clearAttributes();
        Lexeme nextLexeme = _IKImplement.next();
        if (null == nextLexeme) {
            //返回false表示Token已经遍历完了
            return false;
        }
        return setAttributes(nextLexeme);
    }

    /**
     * 为Lexeme设置Token属性
     * @param nextLexeme
     * @return
     */
    private boolean setAttributes(Lexeme nextLexeme) {
        //将Lexeme转成Attributes
        //设置词元文本
        termAtt.append(nextLexeme.getLexemeText());
        //设置词元长度
        termAtt.setLength(nextLexeme.getLength());
        //设置词元位移
        offsetAtt.setOffset(nextLexeme.getBeginPosition(), nextLexeme.getEndPosition());
        //记录分词的最后位置
        endPosition = nextLexeme.getEndPosition();
        //记录词元分类
        typeAtt.setType(nextLexeme.getLexemeTypeString());
        //返会true告知还有下个词元
        return true;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        _IKImplement.reset(input);
    }

    public final void end() throws IOException {
        super.end();
        int finalOffset = correctOffset(this.endPosition);
        offsetAtt.setOffset(finalOffset, finalOffset);
    }
}
