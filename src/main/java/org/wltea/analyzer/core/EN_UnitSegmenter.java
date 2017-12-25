/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
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
package org.wltea.analyzer.core;

import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * 英文单位子分词器
 */
class EN_UnitSegmenter implements ISegmenter {

    //子分词器标签
    static final String SEGMENTER_NAME = "EN_UNIT_SEGMENTER";

    //阿拉伯数字
    private static String Arabic_Num = "0123456789";

    private static Set<Character> ArabicNumberChars = new HashSet<Character>();

    static {
        char[] an = Arabic_Num.toCharArray();
        for (char ac : an) {
            ArabicNumberChars.add(ac);
        }
    }

    /*
     * 词元的开始位置，
     * 同时作为子分词器状态标识
     * 当start > -1 时，标识当前的分词器正在处理字符
     */
    private int nStart;
    /*
     * 记录词元结束位置
     * end记录的是在词元中最后一个出现的合理的数词结束
     */
    private int nEnd;

    //待处理的英文单位hit队列
    private List<Hit> countHits;


    EN_UnitSegmenter() {
        nStart = -1;
        nEnd = -1;
        this.countHits = new LinkedList<Hit>();
    }

    /**
     * 分词
     */
    @Override
    public void analyze(AnalyzeContext context) {
        //处理英文单位
        this.processENUnit(context);

        //判断是否锁定缓冲区
        if (this.nStart == -1 && this.nEnd == -1 &&
                countHits.isEmpty()) {
            //对缓冲区解锁
            context.unlockBuffer(SEGMENTER_NAME);
        } else {
            context.lockBuffer(SEGMENTER_NAME);
        }
    }

    /**
     * 重置子分词器状态
     */
    @Override
    public void reset() {
        nStart = -1;
        nEnd = -1;
        countHits.clear();
    }

    /**
     * 处理英文单位
     * @param context
     */
    private void processENUnit(AnalyzeContext context) {
        // 判断是否需要启动量词扫描
        if (!this.needEnUnitScan(context)) {
            return;
        }
        //首先必须是英文字母
        if (CharacterUtil.CHAR_ENGLISH == context.getCurrentCharType()) {
            handleCountHits(context, false);
        } else {
            //输入的不是英文字符
            //清空未成形的量词
            this.countHits.clear();
        }

        //缓冲区数据已经读完，还有尚未输出的量词
        if (context.isBufferConsumed()) {
            //清空未成形的量词
            handleCountHits(context, true);
            //this.countHits.clear();
        }
    }

    private void handleCountHits(AnalyzeContext context, boolean end) {
        //优先处理countHits中的hit
        if (!this.countHits.isEmpty()) {
            //处理词段队列
            Hit[] tmpArray = this.countHits.toArray(new Hit[this.countHits.size()]);
            for (Hit hit : tmpArray) {
                hit = Dictionary.getSingleton().matchWithHit(context.getSegmentBuff(), context.getCursor(), hit);
                if (hit.isMatch()) {
                    //输出当前的词
                    String text = String.valueOf(context.getSegmentBuff(), hit.getBegin(), hit.getLength());
                    hit = Dictionary.getSingleton().matchInENUnitDict(context.getSegmentBuff(), hit.getBegin(), hit.getLength());
                    if (null != hit && hit.isMatch()) {
                        Lexeme newLexeme = new Lexeme(context.getBufferOffset(), hit.getBegin(), hit.getLength(), Lexeme.TYPE_EN_UNIT);
                        context.addLexeme(newLexeme);
                    }
                } else if (hit.isUnmatch()) {
                    //hit不是词，移除
                    this.countHits.remove(hit);
                }
            }
        }

        //*********************************
        //对当前指针位置的字符进行单字匹配
        Hit singleCharHit = Dictionary.getSingleton().matchInENUnitDict(context.getSegmentBuff(), context.getCursor(), 1);
        //首字母是英文单位
        if (singleCharHit.isMatch()) {
            //输出当前的词
            Lexeme newLexeme = new Lexeme(context.getBufferOffset(), context.getCursor(), 1, Lexeme.TYPE_EN_UNIT);
            context.addLexeme(newLexeme);

            //同时也是词前缀
            if (singleCharHit.isPrefix()) {
                //前缀匹配则放入hit列表
                this.countHits.add(singleCharHit);
            }
        }
        //首字母为英文单位前缀
        else if (singleCharHit.isPrefix()) {
            //前缀匹配则放入hit列表
            this.countHits.add(singleCharHit);
        }
        if (end) {
            this.countHits.clear();
        }
    }

    /**
     * 判断是否需要扫描英文单位
     * @return
     */
    private boolean needEnUnitScan(AnalyzeContext context) {
		/*if(nStart != -1 && nEnd != -1){
			//正在处理英文单位
			return true;
		}*/
        //找到一个相邻的阿拉伯数字，因为一般英文单位前面一般是阿拉伯数字
        if (!context.getOrgLexemes().isEmpty()) {
            Lexeme l = context.getOrgLexemes().peekLast();
            //如果当前英文单位词语的前面一个词是阿拉伯数字
            if (Lexeme.TYPE_ARABIC == l.getLexemeType() &&
                    CharacterUtil.CHAR_ENGLISH == context.getCurrentCharType()) {
                return true;
                //如果这两个词是紧挨在一起的，那么说明此时可以将这两个词组合到一起
				/*if(l.getBegin() + l.getLength() == context.getCursor()){
					return true;
				}*/
            } else {
                if (CharacterUtil.CHAR_ENGLISH == context.getCurrentCharType()) {
                    Hit hit = Dictionary.getSingleton()
                            .matchInENUnitDict(context.getSegmentBuff(), l.getBegin(), 1);
                    //如果前一个词是英文单位的前缀，那么接下来的字符有可能是英文单位的后续部分
                    if (hit.isPrefix()) {
                        return true;
                    }
                } else {
                    char currentChar = context.getCurrentChar();
                    if (currentChar == '℃' || currentChar == '℉') {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 添加英文单位词元到结果集
     * @param context
     */
    private void outputEnUnitLexeme(AnalyzeContext context) {
        if (nStart > -1 && nEnd > -1) {
            //输出英文单位
            Lexeme newLexeme = new Lexeme(context.getBufferOffset(), nStart, nEnd - nStart + 1, Lexeme.TYPE_EN_UNIT);
            context.addLexeme(newLexeme);

        }
    }
}
