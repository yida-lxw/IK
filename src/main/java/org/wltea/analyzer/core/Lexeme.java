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

/**
 * IK词元对象 
 */
public class Lexeme implements Comparable<Lexeme>, Cloneable {
    //lexemeType常量
    //未知
    public static final int TYPE_UNKNOWN = 0;
    //英文
    public static final int TYPE_ENGLISH = 1;
    //阿拉伯数字
    public static final int TYPE_ARABIC = 2;
    //英文数字混合
    public static final int TYPE_LETTER = 3;
    //中文词元
    public static final int TYPE_CNWORD = 4;
    //中文单字
    public static final int TYPE_CNCHAR = 64;
    //日韩文字
    public static final int TYPE_OTHER_CJK = 8;
    //中文数字
    public static final int TYPE_CNUM = 16;
    //中文量词
    public static final int TYPE_COUNT = 32;
    //中文数字+量词
    public static final int TYPE_CQUAN = 48;
    //中文十进制
    public static final int TYPE_DENARY = 128;
    //英文单位
    public static final int TYPE_EN_UNIT = 256;

    //阿拉伯数字+中文十进制,比如: 2万
    public static final int TYPE_ARABIC_DENARY = 512;
    //中文数字+中文十进制,比如: 十万
    public static final int TYPE_CNUM_DENARY = 1024;
    //阿拉伯数字+中文量词,比如: 2天
    public static final int TYPE_ARABIC_COUNT = 2048;
    //中文数字+中文量词,比如: 十天
    public static final int TYPE_CNUM_COUNT = 5096;
    //阿拉伯数字+英文单位,比如: 160cm
    public static final int TYPE_ARABIC_EN_UNIT = 10192;

    //词元的起始位移
    private int offset;
    //词元的相对起始位置
    private int begin;
    //词元的长度
    private int length;
    //词元文本
    private String lexemeText;
    //词元类型
    private int lexemeType;

    public Lexeme(int begin, int length, int lexemeType) {
        this.begin = begin;
        if (length < 0) {
            throw new IllegalArgumentException("length < 0");
        }
        this.length = length;
        this.lexemeType = lexemeType;
    }

    public Lexeme(int offset, int begin, int length, int lexemeType) {
        this.offset = offset;
        this.begin = begin;
        if (length < 0) {
            throw new IllegalArgumentException("length < 0，the lexemeText:{" + this.lexemeText + "}," +
                    "offset:{" + offset + "},begin:{" + begin + "},length:{" + length + "}");
        }
        this.length = length;
        this.lexemeType = lexemeType;
    }

    /*
     * 判断词元相等算法
     * 起始位置偏移、起始位置、终止位置相同
     * @see java.lang.Object#equals(Object o)
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (o instanceof Lexeme) {
            Lexeme other = (Lexeme) o;
            if (this.offset == other.getOffset()
                    && this.begin == other.getBegin()
                    && this.length == other.getLength()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /*
     * 词元哈希编码算法
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int absBegin = getBeginPosition();
        int absEnd = getEndPosition();
        return (absBegin * 37) + (absEnd * 31) + ((absBegin * absEnd) % getLength()) * 11;
    }

    /*
     * 词元在排序集合中的比较算法
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Lexeme other) {
        //起始位置优先
        if (this.begin < other.getBegin()) {
            return -1;
        } else if (this.begin == other.getBegin()) {
            //词元长度优先
            if (this.length > other.getLength()) {
                return -1;
            } else if (this.length == other.getLength()) {
                return 0;
            } else {//this.length < other.getLength()
                return 1;
            }

        } else {//this.begin > other.getBegin()
            return 1;
        }
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getBegin() {
        return begin;
    }

    /**
     * 获取词元在文本中的起始位置
     * @return int
     */
    public int getBeginPosition() {
        return offset + begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    /**
     * 获取词元在文本中的结束位置
     * @return int
     */
    public int getEndPosition() {
        return offset + begin + length;
    }

    /**
     * 获取词元的字符长度
     * @return int
     */
    public int getLength() {
        return this.length;
    }

    public void setLength(int length) {
        if (this.length < 0) {
            throw new IllegalArgumentException("length < 0");
        }
        this.length = length;
    }

    /**
     * 获取词元的文本内容
     * @return String
     */
    public String getLexemeText() {
        if (lexemeText == null) {
            return "";
        }
        return lexemeText;
    }

    public void setLexemeText(String lexemeText) {
        if (lexemeText == null) {
            this.lexemeText = "";
            this.length = 0;
        } else {
            this.lexemeText = lexemeText;
            this.length = lexemeText.length();
        }
    }

    /**
     * 获取词元类型
     * @return int
     */
    public int getLexemeType() {
        return lexemeType;
    }

    /**
     * 获取词元类型标示字符串
     * @return String
     */
    public String getLexemeTypeString() {
        switch (lexemeType) {

            case TYPE_ENGLISH:
                return "TYPE_ENGLISH";

            case TYPE_ARABIC:
                return "TYPE_ARABIC";

            case TYPE_LETTER:
                return "TYPE_LETTER";

            case TYPE_CNWORD:
                return "TYPE_CN_WORD";

            case TYPE_CNCHAR:
                return "TYPE_CN_CHAR";

            case TYPE_OTHER_CJK:
                return "TYPE_OTHER_CJK";

            case TYPE_COUNT:
                return "TYPE_COUNT";

            case TYPE_CNUM:
                return "TYPE_CNUM";

            case TYPE_CQUAN:
                return "TYPE_CQUAN";

            case TYPE_DENARY:
                return "TYPE_DENARY";

            case TYPE_EN_UNIT:
                return "TYPE_EN_UNIT";
            case TYPE_ARABIC_DENARY:
                return "TYPE_ARABIC_DENARY";
            case TYPE_CNUM_DENARY:
                return "TYPE_CNUM_DENARY";
            case TYPE_ARABIC_COUNT:
                return "TYPE_ARABIC_COUNT";
            case TYPE_CNUM_COUNT:
                return "TYPE_CNUM_COUNT";
            case TYPE_ARABIC_EN_UNIT:
                return "TYPE_ARABIC_EN_UNIT";

            default:
                return "UNKONW";
        }
    }

    public void setLexemeType(int lexemeType) {
        this.lexemeType = lexemeType;
    }

    /**
     * 合并两个相邻的词元
     * @param l
     * @param lexemeType
     * @return boolean 词元是否成功合并
     */
    public boolean append(Lexeme l, int lexemeType) {
        if (l != null && this.getEndPosition() == l.getBeginPosition()) {
            this.length += l.getLength();
            this.lexemeType = lexemeType;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuffer strbuf = new StringBuffer();
        strbuf.append(this.getBeginPosition()).append("-").append(this.getEndPosition());
        strbuf.append(" : ").append(this.lexemeText).append(" : \t");
        strbuf.append(this.getLexemeTypeString());
        return strbuf.toString();
    }

    /**
     * 对象克隆
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    protected Lexeme clone() {
        Lexeme lexeme = null;
        try {
            lexeme = (Lexeme) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return lexeme;
    }
}
