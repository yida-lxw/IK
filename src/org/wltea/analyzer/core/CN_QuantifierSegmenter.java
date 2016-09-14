/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 * 
 */
package org.wltea.analyzer.core;

import java.util.*;

import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;

/**
 * 
 * 中文数量词子分词器
 */
class CN_QuantifierSegmenter implements ISegmenter{
	
	//子分词器标签
	static final String SEGMENTER_NAME = "QUAN_SEGMENTER";

	//链接符号
	private static final char[] Symbolic_Link = new char[]{'#' , '&' , '+' , '-' , '.' , '@' , '_'};
	
	//中文数字
	private static String Chn_Num = "一二两三四五六七八九十零壹贰叁肆伍陆柒捌玖卅廿";

	//中文十进制
	private static String Denary_Name = "十百千万亿兆拾佰仟萬億";

	//中文数字
	private static Set<Character> ChnNumberChars = new HashSet<Character>();

	//十进制位
	private static Set<Character> DenaryChars = new HashSet<Character>();
	static {
		char[] ca = Chn_Num.toCharArray();
		for(char nChar : ca){
			ChnNumberChars.add(nChar);
		}

		char[] dn = Denary_Name.toCharArray();
		for(char dc : dn){
			DenaryChars.add(dc);
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

	private int dnStart;

	private int dnEnd;

	private int start_arabic_unit_num;
	private int end_arabic_unit_num;
	private int start_arabic_unit_cn;
	private int end_arabic_unit_cn;
	private boolean matche;

	private int start_arabic_count;
	private int end_arabic_count;

	private int start_cnum_unit;
	private int end_cnum_unit;

	private int start_cnum_count;
	private int end_cnum_count;

	//待处理的量词hit队列
	private List<Hit> countHits;
	
	
	CN_QuantifierSegmenter(){
		nStart = -1;
		nEnd = -1;
		dnStart = -1;
		dnEnd = -1;

		start_arabic_unit_num = -1;
		end_arabic_unit_num = -1;
		start_arabic_unit_cn = -1;
		end_arabic_unit_cn = -1;
		matche = false;
		this.countHits  = new LinkedList<Hit>();
	}
	
	/**
	 * 分词
	 */
	public void analyze(AnalyzeContext context) {
		//处理中文十进制
		this.processDenary(context);
		//处理中文数字
		this.processCNumber(context);
		//处理中文单位量词
		this.processCount(context);
		//处理阿拉伯数字+中文量词，比如：24小时  7天
		this.processArabicUnit(context);
		
		//判断是否锁定缓冲区
		if(this.nStart == -1 && this.nEnd == -1	&&
				this.dnStart == -1 && this.dnEnd == -1 &&
				countHits.isEmpty()){
			//对缓冲区解锁
			context.unlockBuffer(SEGMENTER_NAME);
		}else{
			context.lockBuffer(SEGMENTER_NAME);
		}
	}
	

	/**
	 * 重置子分词器状态
	 */
	public void reset() {
		nStart = -1;
		nEnd = -1;
		dnStart = -1;
		dnEnd = -1;
		countHits.clear();
	}

	/**
	 * 处理阿中文十进制
	 */
	private void processDenary(AnalyzeContext context){
		if(dnStart == -1 && dnEnd == -1){//初始状态
			//如果是中文十进制
			if(CharacterUtil.CHAR_CHINESE == context.getCurrentCharType()
					&& DenaryChars.contains(context.getCurrentChar())){
				//记录中文十进制的起始、结束位置
				dnStart = context.getCursor();
				dnEnd = context.getCursor();
			}
		}else {//正在处理状态
			if(CharacterUtil.CHAR_CHINESE == context.getCurrentCharType()
					&& DenaryChars.contains(context.getCurrentChar())){
				//记录中文十进制的结束位置
				dnEnd = context.getCursor();
			} else {
				//输出中文十进制
				this.outputDenaryLexeme(context);
				//重置头尾指针
				dnStart = -1;
				dnEnd = -1;
			}
		}

		//缓冲区已经用完，还有尚未输出的数词
		if(context.isBufferConsumed()){
			if(dnStart != -1 && dnEnd != -1){
				//输出中文十进制
				outputDenaryLexeme(context);
				//重置头尾指针
				dnStart = -1;
				dnEnd = -1;
			}
		}
	}
	
	/**
	 * 处理中文数字
	 */
	private void processCNumber(AnalyzeContext context){
		if(nStart == -1 && nEnd == -1){//初始状态
			if(CharacterUtil.CHAR_CHINESE == context.getCurrentCharType() 
					&& ChnNumberChars.contains(context.getCurrentChar())){
				//记录数词的起始、结束位置
				nStart = context.getCursor();
				nEnd = context.getCursor();
			}
		}else{
			//输出中文数字
			this.outputChineseNumLexeme(context);
			//重置头尾指针
			nStart = -1;
			nEnd = -1;
		}
		
		//缓冲区已经用完，还有尚未输出的数词
		if(context.isBufferConsumed()){
			if(nStart != -1 && nEnd != -1){
				//输出中文数字
				outputChineseNumLexeme(context);
				//重置头尾指针
				nStart = -1;
				nEnd = -1;
			}
		}	
	}
	
	/**
	 * 处理中文量词
	 * @param context
	 */
	private void processCount(AnalyzeContext context){
		// 判断是否需要启动量词扫描
		if(!this.needCountScan(context)){
			return;
		}
		
		if(CharacterUtil.CHAR_CHINESE == context.getCurrentCharType()){
			//优先处理countHits中的hit
			if(!this.countHits.isEmpty()){
				//处理词段队列
				Hit[] tmpArray = this.countHits.toArray(new Hit[this.countHits.size()]);
				for(Hit hit : tmpArray){
					hit = Dictionary.getSingleton().matchWithHit(context.getSegmentBuff(), context.getCursor() , hit);
					if(hit.isMatch()){
						//输出当前的词
						Lexeme newLexeme = new Lexeme(context.getBufferOffset() , hit.getBegin() , context.getCursor() - hit.getBegin() + 1 , Lexeme.TYPE_COUNT);
						context.addLexeme(newLexeme);
						if(!hit.isPrefix()){//不是词前缀，hit不需要继续匹配，移除
							this.countHits.remove(hit);
						}
					}else if(hit.isUnmatch()){
						//hit不是词，移除
						this.countHits.remove(hit);
					}					
				}
			}				

			//*********************************
			//对当前指针位置的字符进行单字匹配
			Hit singleCharHit = Dictionary.getSingleton().matchInQuantifierDict(context.getSegmentBuff(), context.getCursor(), 1);
			if(singleCharHit.isMatch()){//首字成量词词
				//输出当前的词
				Lexeme newLexeme = new Lexeme(context.getBufferOffset() , context.getCursor() , 1 , Lexeme.TYPE_COUNT);
				context.addLexeme(newLexeme);

				//同时也是词前缀
				if(singleCharHit.isPrefix()){
					//前缀匹配则放入hit列表
					this.countHits.add(singleCharHit);
				}
			}else if(singleCharHit.isPrefix()){//首字为量词前缀
				//前缀匹配则放入hit列表
				this.countHits.add(singleCharHit);
			}
		}else{
			//输入的不是中文字符
			//清空未成形的量词
			this.countHits.clear();
		}
		
		//缓冲区数据已经读完，还有尚未输出的量词
		if(context.isBufferConsumed()){
			//清空未成形的量词
			this.countHits.clear();
		}
	}

	/**
	 * 处理阿拉伯数字+中文单位混合输出
	 * 如：24小时 365天
	 * @param context
	 * @return
	 */
	private boolean processArabicUnit(AnalyzeContext context){
		boolean needLock = false;
		//当前的分词器尚未开始处理字符
		if(this.start_arabic_unit_num == -1){
			if(CharacterUtil.CHAR_ARABIC == context.getCurrentCharType()){
				//记录起始指针的位置,标明分词器进入处理状态
				this.start_arabic_unit_num = context.getCursor();
				this.end_arabic_unit_num = start_arabic_unit_num;
			}
		} else {
			//当前的分词器正在处理字符
			if(CharacterUtil.CHAR_ARABIC == context.getCurrentCharType()){
				//记录下可能的结束位置
				this.end_arabic_unit_num = context.getCursor();
				if(this.start_arabic_unit_cn != -1) {
					this.end_arabic_unit_cn = context.getCursor();
					if(this.end_arabic_unit_cn != -1 && this.start_arabic_unit_cn != -1) {
						Hit singleCharHit = Dictionary.getSingleton().matchInQuantifierDict(context.getSegmentBuff(),
								this.start_arabic_unit_cn, this.end_arabic_unit_cn - this.start_arabic_unit_cn + 1);
						if(!singleCharHit.isMatch() && !singleCharHit.isPrefix()){
							this.start_arabic_unit_cn = -1;
							this.end_arabic_unit_cn = -1;
							this.start_arabic_unit_num = -1;
							this.end_arabic_unit_num = -1;
						} else if(singleCharHit.isMatch()) {
							this.matche = true;
						}
					}
				}
			} else if(CharacterUtil.CHAR_CHINESE == context.getCurrentCharType()) {
				if(this.start_arabic_unit_cn == -1) {
					this.start_arabic_unit_cn = context.getCursor();
					this.end_arabic_unit_cn = start_arabic_unit_cn;
					this.end_arabic_unit_num = start_arabic_unit_cn;
					if(this.end_arabic_unit_cn != -1 && this.start_arabic_unit_cn != -1) {
						Hit singleCharHit = Dictionary.getSingleton().matchInQuantifierDict(context.getSegmentBuff(),
								this.start_arabic_unit_cn, 1);
						if (!singleCharHit.isMatch() && !singleCharHit.isPrefix()) {
							this.start_arabic_unit_cn = -1;
							this.end_arabic_unit_cn = -1;
							this.start_arabic_unit_num = -1;
							this.end_arabic_unit_num = -1;
						} else if(singleCharHit.isMatch()) {
							this.matche = true;
						}
					}
				} else {
					this.end_arabic_unit_cn = context.getCursor();
					//查询中文单位词典，如果当前字符不是中文单位的前缀，那么直接重置位置信息
					if(this.end_arabic_unit_cn != -1 && this.start_arabic_unit_cn != -1) {
						Hit singleCharHit = Dictionary.getSingleton().matchInQuantifierDict(context.getSegmentBuff(),
								this.start_arabic_unit_cn,this.end_arabic_unit_cn - this.start_arabic_unit_cn + 1);
						//既不是中文单位，也不是中文单位的前缀，那么说明不需要组合
						if(!singleCharHit.isMatch() && !singleCharHit.isPrefix()){
							if(this.matche) {
								Lexeme newLexeme = new Lexeme(context.getBufferOffset() ,
										this.start_arabic_unit_num , this.end_arabic_unit_cn - this.start_arabic_unit_num,
										Lexeme.TYPE_ARABIC_COUNT);
								context.addLexeme(newLexeme);
							}
							this.start_arabic_unit_cn = -1;
							this.end_arabic_unit_cn = -1;
							this.start_arabic_unit_num = -1;
							this.end_arabic_unit_num = -1;
						} else if(singleCharHit.isMatch()) {
							this.matche = true;
						}
					}
				}
			} else{
				this.start_arabic_unit_cn = -1;
				this.end_arabic_unit_cn = -1;
				this.start_arabic_unit_num = -1;
				this.end_arabic_unit_num = -1;
			}
		}

		//判断缓冲区是否已经读完
		if(context.isBufferConsumed()) {
			if(this.start_arabic_unit_cn != -1 && this.end_arabic_unit_cn != -1 &&
					this.start_arabic_unit_num != -1 && this.end_arabic_unit_num != -1 && this.matche){
				Lexeme newLexeme = new Lexeme(context.getBufferOffset() ,
						this.start_arabic_unit_num , this.end_arabic_unit_cn - this.start_arabic_unit_num + 1 ,
						Lexeme.TYPE_ARABIC_COUNT);
				context.addLexeme(newLexeme);
				this.start_arabic_unit_cn = -1;
				this.end_arabic_unit_cn = -1;
				this.start_arabic_unit_num = -1;
				this.end_arabic_unit_num = -1;
			}
		}

		//判断是否锁定缓冲区
		if(this.start_arabic_unit_cn == -1 && this.end_arabic_unit_cn == -1 &&
				this.start_arabic_unit_num == -1 && this.end_arabic_unit_num == -1){
			//对缓冲区解锁
			needLock = false;
		}else{
			needLock = true;
		}
		return needLock;
	}

	/**
	 * 判断是否是链接符号
	 * @param input
	 * @return
	 */
	private boolean isSymbolicLink(char input){
		int index = Arrays.binarySearch(Symbolic_Link, input);
		return index >= 0;
	}

	/**
	 * 判断是否需要扫描量词
	 * @return
	 */
	private boolean needCountScan(AnalyzeContext context){
		if((nStart != -1 && nEnd != -1 ) || (dnStart != -1 && dnEnd != -1 )){
			//正在处理中文数词,或者正在处理量词
			return true;
		}
		//找到一个相邻的数词
		if(!context.getOrgLexemes().isEmpty()){
			Lexeme l = context.getOrgLexemes().peekLast();
			if(Lexeme.TYPE_CNUM == l.getLexemeType() ||  Lexeme.TYPE_ARABIC == l.getLexemeType()){
				return true;
				/*if(l.getBegin() + l.getLength() == context.getCursor()){
					return true;
				}*/
			}
		}
		return false;
	}
	
	/**
	 * 添加中文数字词元到结果集
	 * @param context
	 */
	private void outputChineseNumLexeme(AnalyzeContext context){
		if(nStart > -1 && nEnd > -1){
			//输出中文数字
			Lexeme newLexeme = new Lexeme(context.getBufferOffset() , nStart , nEnd - nStart + 1 , Lexeme.TYPE_CNUM);
			context.addLexeme(newLexeme);
			
		}
	}

	/**
	 * 添加中文十进制词元到结果集
	 * @param context
	 */
	private void outputDenaryLexeme(AnalyzeContext context){
		if(dnStart > -1 && dnEnd > -1){
			//输出中文十进制
			Lexeme newLexeme = new Lexeme(context.getBufferOffset() , dnStart , dnEnd - dnStart + 1 , Lexeme.TYPE_DENARY);
			context.addLexeme(newLexeme);
		}
	}
}
