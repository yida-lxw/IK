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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 英文字符及阿拉伯数字子分词器
 */
class LetterSegmenter implements ISegmenter {

	//子分词器标签
	static final String SEGMENTER_NAME = "LETTER_SEGMENTER";
	//链接符号
	private static final char[] Letter_Connector = new char[]{'#', '&', '+', '-', '.', '@', '_'};

	//数字符号
	private static final char[] Num_Connector = new char[]{',', '.'};

    // 存储混合字符串中的连接符位置
    private Queue<Integer> connectorPositions = new LinkedList<>();

	/*
	 * 词元的开始位置，
	 * 同时作为子分词器状态标识
	 * 当start > -1 时，标识当前的分词器正在处理字符
	 */
	private int start;
	/*
	 * 记录词元结束位置
	 * end记录的是在词元中最后一个出现的Letter但非Sign_Connector的字符的位置
	 */
	private int end;

	/*
	 * 字母起始位置
	 */
	private int englishStart;

	/*
	 * 字母结束位置
	 */
	private int englishEnd;

	/*
	 * 阿拉伯数字起始位置
	 */
	private int arabicStart;

	/*
	 * 阿拉伯数字结束位置
	 */
	private int arabicEnd;

	public LetterSegmenter() {
		Arrays.sort(Letter_Connector);
		Arrays.sort(Num_Connector);
		this.start = -1;
		this.end = -1;
		this.englishStart = -1;
		this.englishEnd = -1;
		this.arabicStart = -1;
		this.arabicEnd = -1;
	}

	@Override
	public void analyze(AnalyzeContext context) {
		boolean bufferLockFlag = false;
		//处理英文字母
		bufferLockFlag = this.processEnglishLetter(context) || bufferLockFlag;
		//处理阿拉伯字母
		bufferLockFlag = this.processArabicLetter(context) || bufferLockFlag;
		//处理混合字母(这个要放最后处理，可以通过QuickSortSet排除重复)
		bufferLockFlag = this.processMixLetter(context) || bufferLockFlag;

		//判断是否锁定缓冲区
		if (bufferLockFlag) {
			context.lockBuffer(SEGMENTER_NAME);
		} else {
			//对缓冲区解锁
			context.unlockBuffer(SEGMENTER_NAME);
		}
	}

	@Override
	public void reset() {
		this.start = -1;
		this.end = -1;
		this.englishStart = -1;
		this.englishEnd = -1;
		this.arabicStart = -1;
		this.arabicEnd = -1;
	}

	/**
	 * 处理数字字母混合输出
	 * 如：windos2000 | linliangyi2005@gmail.com
	 *
	 * @param context
	 * @return
	 */
	private boolean processMixLetter(AnalyzeContext context) {
		boolean needLock = false;
        char currentChar = context.getCurrentChar();
        int currentCharType = context.getCurrentCharType();
        int cursor = context.getCursor();

        if (this.start == -1) {
            // 当前的分词器尚未开始处理字符
            if (currentCharType == CharacterUtil.CHAR_ARABIC ||
                    currentCharType == CharacterUtil.CHAR_ENGLISH) {
                // 记录起始指针的位置,标明分词器进入处理状态
                this.start = cursor;
                this.end = cursor;
                this.connectorPositions.clear();
			}
        } else {
            // 当前的分词器正在处理字符
            if (currentCharType == CharacterUtil.CHAR_ARABIC ||
                    currentCharType == CharacterUtil.CHAR_ENGLISH) {
                // 记录下可能的结束位置
                this.end = cursor;
            } else if (currentCharType == CharacterUtil.CHAR_USELESS &&
                    this.isLetterConnector(currentChar)) {
                // 记录连接符位置
                this.connectorPositions.add(cursor);
                // 记录下可能的结束位置
                this.end = cursor;
			} else {
                // 遇到非Letter字符，输出词元
                outputMixLexemes(context);
				this.start = -1;
				this.end = -1;
			}
		}

        // 判断缓冲区是否已经读完
		if (context.isBufferConsumed()) {
			if (this.start != -1 && this.end != -1) {
                outputMixLexemes(context);
				this.start = -1;
				this.end = -1;
			}
		}

        // 判断是否锁定缓冲区
		if (this.start == -1 && this.end == -1) {
			needLock = false;
		} else {
			needLock = true;
		}
		return needLock;
	}

    /**
     * 输出混合字符串的所有可能词元
     */
    private void outputMixLexemes(AnalyzeContext context) {
        if (this.start < 0 || this.end < this.start) {
            return;
        }

        int bufferOffset = context.getBufferOffset();
        int length = this.end - this.start + 1;
        char[] segmentBuff = context.getSegmentBuff();

        // 1. 输出整个混合字符串
        Lexeme fullLexeme = new Lexeme(bufferOffset, this.start, length, Lexeme.TYPE_LETTER);
        context.addLexeme(fullLexeme);

        // 2. 输出包含连接符的中间部分组合
        // 创建一个列表来存储所有连接符位置
        List<Integer> connectors = new ArrayList<>(this.connectorPositions);

        // 输出从开始到每个连接符加后面内容的组合
        for (int i = 0; i < connectors.size(); i++) {
            // 从开始位置到当前连接符+1位置（包含连接符和后面的部分）
            for (int j = i + 1; j <= connectors.size(); j++) {
                int endPos = (j < connectors.size()) ? connectors.get(j) : this.end;
                int subLength = endPos - this.start;
                if (shouldOutputLexeme(segmentBuff, this.start, subLength)) {
                    Lexeme subLexeme = new Lexeme(bufferOffset, this.start, subLength, Lexeme.TYPE_LETTER);
                    context.addLexeme(subLexeme);
                }
            }
        }

        // 3. 输出所有子组合（从开始到每个连接符之前）
        int subStart = this.start;
        for (int connectorPos : this.connectorPositions) {
            int subLength = connectorPos - subStart;
            if (shouldOutputLexeme(segmentBuff, subStart, subLength)) {
                Lexeme subLexeme = new Lexeme(bufferOffset, subStart, subLength, Lexeme.TYPE_LETTER);
                context.addLexeme(subLexeme);
            }
            subStart = connectorPos + 1;
        }

        // 4. 输出最后一个子部分（如果有）
        if (subStart <= this.end) {
            int subLength = this.end - subStart + 1;
            if (shouldOutputLexeme(segmentBuff, subStart, subLength)) {
                Lexeme subLexeme = new Lexeme(bufferOffset, subStart, subLength, Lexeme.TYPE_LETTER);
                context.addLexeme(subLexeme);
            }
        }
    }

	/**
	 * 处理纯英文字母输出
	 *
	 * @param context
	 * @return
	 */
	private boolean processEnglishLetter(AnalyzeContext context) {
		boolean needLock = false;

		if (this.englishStart == -1) {//当前的分词器尚未开始处理英文字符
			if (CharacterUtil.CHAR_ENGLISH == context.getCurrentCharType()) {
				//记录起始指针的位置,标明分词器进入处理状态
				this.englishStart = context.getCursor();
				this.englishEnd = this.englishStart;
			}
		} else {//当前的分词器正在处理英文字符
			if (CharacterUtil.CHAR_ENGLISH == context.getCurrentCharType()) {
				//记录当前指针位置为结束位置
				this.englishEnd = context.getCursor();
			} else {
				if (this.englishEnd >= this.englishStart) {
                    //添加词元之前，检查是否应该输出
                    int length = this.englishEnd - this.englishStart + 1;
                    if (shouldOutputLexeme(context.getSegmentBuff(), this.englishStart, length)) {
                        Lexeme newLexeme = new Lexeme(context.getBufferOffset(), this.englishStart, length, Lexeme.TYPE_ENGLISH);
                        context.addLexeme(newLexeme);
                    }
				}
				this.englishStart = -1;
				this.englishEnd = -1;
			}
		}

		//判断缓冲区是否已经读完
		if (context.isBufferConsumed()) {
			if (this.englishStart != -1 && this.englishEnd != -1) {
                //添加词元之前，检查是否应该输出
                int length = this.englishEnd - this.englishStart + 1;
                if (shouldOutputLexeme(context.getSegmentBuff(), this.englishStart, length)) {
                    //判断它是不是英文单位
                    Hit shit = Dictionary.getSingleton().matchInENUnitDict(context.getSegmentBuff(), englishStart, length);
                    int lexemeType = 0;
                    if (shit.isMatch()) {
                        lexemeType = Lexeme.TYPE_EN_UNIT;
                    } else {
                        lexemeType = Lexeme.TYPE_ENGLISH;
                    }
                    Lexeme newLexeme = new Lexeme(context.getBufferOffset(), this.englishStart, length, lexemeType);
					context.addLexeme(newLexeme);
				}
				this.englishStart = -1;
				this.englishEnd = -1;
			}
		}

		//判断是否锁定缓冲区
		if (this.englishStart == -1 && this.englishEnd == -1) {
			//对缓冲区解锁
			needLock = false;
		} else {
			needLock = true;
		}
		return needLock;
	}

	/**
	 * 处理阿拉伯数字输出
	 *
	 * @param context
	 * @return
	 */
	private boolean processArabicLetter(AnalyzeContext context) {
		boolean needLock = false;

		if (this.arabicStart == -1) {//当前的分词器尚未开始处理数字字符
			if (CharacterUtil.CHAR_ARABIC == context.getCurrentCharType()) {
				//记录起始指针的位置,标明分词器进入处理状态
				this.arabicStart = context.getCursor();
				this.arabicEnd = this.arabicStart;
			}
		} else {//当前的分词器正在处理数字字符
			if (CharacterUtil.CHAR_ARABIC == context.getCurrentCharType()) {
				//记录当前指针位置为结束位置
				this.arabicEnd = context.getCursor();
			} else if (CharacterUtil.CHAR_USELESS != context.getCurrentCharType()
					|| !this.isNumConnector(context.getCurrentChar())) {
				if (this.arabicEnd >= this.arabicStart) {
                    //添加词元之前，检查是否应该输出
                    int length = this.arabicEnd - this.arabicStart + 1;
                    if (shouldOutputLexeme(context.getSegmentBuff(), this.arabicStart, length)) {
                        Lexeme newLexeme = new Lexeme(context.getBufferOffset(), this.arabicStart, length, Lexeme.TYPE_ARABIC);
                        context.addLexeme(newLexeme);
                    }
				}

				this.arabicStart = -1;
				this.arabicEnd = -1;
			}
		}

		//判断缓冲区是否已经读完
		if (context.isBufferConsumed()) {
			if (this.arabicStart != -1 && this.arabicEnd != -1) {
				if (this.arabicEnd >= this.arabicStart) {
                    //添加词元之前，检查是否应该输出
                    int length = this.arabicEnd - this.arabicStart + 1;
                    if (shouldOutputLexeme(context.getSegmentBuff(), this.arabicStart, length)) {
                        Lexeme newLexeme = new Lexeme(context.getBufferOffset(), this.arabicStart, length, Lexeme.TYPE_ARABIC);
                        context.addLexeme(newLexeme);
                    }
				}
				this.arabicStart = -1;
				this.arabicEnd = -1;
			}
		}

		//判断是否锁定缓冲区
		if (this.arabicStart == -1 && this.arabicEnd == -1) {
			//对缓冲区解锁
			needLock = false;
		} else {
			needLock = true;
		}
		return needLock;
	}

	/**
	 * 判断是否是字母连接符号
	 *
	 * @param input
	 * @return
	 */
	private boolean isLetterConnector(char input) {
		int index = Arrays.binarySearch(Letter_Connector, input);
		return index >= 0;
	}

	/**
	 * 判断是否是数字连接符号
	 *
	 * @param input
	 * @return
	 */
	private boolean isNumConnector(char input) {
		int index = Arrays.binarySearch(Num_Connector, input);
		return index >= 0;
    }

    /**
     * 判断是否应该输出词元
     * 规则：长度大于1的词元，或者虽然长度为1但命中词典的词元
     *
     * @param segmentBuff 缓冲区
     * @param start       起始位置
     * @param length      长度
     * @return 是否应该输出
     */
    private boolean shouldOutputLexeme(char[] segmentBuff, int start, int length) {
        // 长度大于1的词元直接输出
        if (length > 1) {
            return true;
        }
        // 对于长度为1的词元，只有在命中词典时才输出
        if (length == 1) {
            // 检查是否命中主词典
            Hit hit = Dictionary.getSingleton().matchInMainDict(segmentBuff, start, length);
            if (hit.isMatch()) {
                return true;
            }
            // 检查是否命中英文单位词典
            hit = Dictionary.getSingleton().matchInENUnitDict(segmentBuff, start, length);
            return hit.isMatch();
        }
        return false;
	}
}
