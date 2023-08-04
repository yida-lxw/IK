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
package org.wltea.analyzer.dic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wltea.analyzer.cfg.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;

/**
 * 词典管理类,单子模式
 */
public class Dictionary {
    private static final Log log = LogFactory.getLog(Dictionary.class);

    private static final int BUFFER_SIZE = 2048;

    private static final String DEFAULT_CHARSET = "UTF-8";

    /*
     * 词典单子实例
     */
    private static Dictionary singleton;

    /*
     * 主词典对象
     */
    private DictSegment _MainDict;

    /*
     * 停止词词典
     */
    private DictSegment _StopWordDict;
    /*
     * 中文量词词典
     */
    private DictSegment _QuantifierDict;

    //英文单位词典
    private DictSegment _EN_UNIT_Dict;

    /**
     * 配置对象
     */
    private Configuration cfg;

    private Dictionary(Configuration cfg) {
        this.cfg = cfg;
        //加载主扩展词库词典文件
        this.loadMainDict();
        //加载停用词词典文件
        this.loadStopWordDict();
        //加载英文单位词典文件
        _EN_UNIT_Dict = this.loadCustomDict(this.cfg.getEnglishUnitDicionary(), _EN_UNIT_Dict);
        //加载中文量词词典文件
        _QuantifierDict = this.loadCustomDict(this.cfg.getQuantifierDicionary(), _QuantifierDict);
    }

    /**
     * 词典初始化
     * 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
     * 只有当Dictionary类被实际调用时，才会开始载入词典，
     * 这将延长首次分词操作的时间
     * 该方法提供了一个在应用加载阶段就初始化字典的手段
     * @return Dictionary
     */
    public static Dictionary initial(Configuration cfg) {
        if (singleton == null) {
            synchronized (Dictionary.class) {
                if (singleton == null) {
                    singleton = new Dictionary(cfg);
                    return singleton;
                }
            }
        }
        return singleton;
    }

    /**
     * 获取词典单子实例
     * @return Dictionary 单例对象
     */
    public static Dictionary getSingleton() {
        if (singleton == null) {
            throw new IllegalStateException("词典尚未初始化，请先调用initial方法");
        }
        return singleton;
    }

    /**
     * 批量加载新词条
     * @param words Collection<String>词条列表
     */
    public void addWords(Collection<String> words) {
        if (words != null) {
            for (String word : words) {
                if (word != null) {
                    //批量加载词条到主内存词典中
                    singleton._MainDict.fillSegment(word.trim().toLowerCase().toCharArray());
                }
            }
        }
    }

    /**
     * 批量移除（屏蔽）词条
     * @param words
     */
    public void disableWords(Collection<String> words) {
        if (words != null) {
            for (String word : words) {
                if (word != null) {
                    //批量屏蔽词条
                    singleton._MainDict.disableSegment(word.trim().toLowerCase().toCharArray());
                }
            }
        }
    }

    /**
     * 检索匹配主词典
     * @param charArray
     * @return Hit 匹配结果描述
     */
    public Hit matchInMainDict(char[] charArray) {
        return singleton._MainDict.match(charArray);
    }

    /**
     * 检索匹配主词典
     * @param charArray
     * @param begin
     * @param length
     * @return Hit 匹配结果描述
     */
    public Hit matchInMainDict(char[] charArray, int begin, int length) {
        return singleton._MainDict.match(charArray, begin, length);
    }

    /**
     * 检索匹配量词词典
     * @param charArray
     * @param begin
     * @param length
     * @return Hit 匹配结果描述
     */
    public Hit matchInQuantifierDict(char[] charArray, int begin, int length) {
        return singleton._QuantifierDict.match(charArray, begin, length);
    }

    /**
     * 检索匹配英文单位词典
     * @param charArray
     * @param begin
     * @param length
     * @return Hit 匹配结果描述
     */
    public Hit matchInENUnitDict(char[] charArray, int begin, int length) {
        return singleton._EN_UNIT_Dict.match(charArray, begin, length);
    }

    /**
     * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
     * @param charArray
     * @param currentIndex
     * @param matchedHit
     * @return Hit
     */
    public Hit matchWithHit(char[] charArray, int currentIndex, Hit matchedHit) {
        DictSegment dictSegment = matchedHit.getMatchedDictSegment();
        return dictSegment.match(charArray, currentIndex, 1, matchedHit);
    }


    /**
     * 判断是否为计量单位
     * @param charArray
     * @param begin
     * @param length
     * @return boolean
     */
    public boolean isCNUnitWord(char[] charArray, int begin, int length) {
        return singleton._QuantifierDict.match(charArray, begin, length).isMatch();
    }

    public boolean isStopWord(char[] charArray, int begin, int length) {
        return singleton._StopWordDict.match(charArray, begin, length).isMatch();
    }

    /**
     * 加载主词典及扩展词典
     */
    private void loadMainDict() {
        //建立一个主词典实例
        _MainDict = new DictSegment((char) 0);
        //读取主词典文件
        String mainDictPath = cfg.getMainDictionary();
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(mainDictPath);
        String mainDictName = getDicFileName(mainDictPath);
        if (is == null) {
            throw new RuntimeException("Main Dictionary:{" + mainDictName + "} not found!!!");
        }

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, DEFAULT_CHARSET), BUFFER_SIZE);
            log.info("加载主词典:" + mainDictName);
            String theWord = null;
            do {
                theWord = bufferedReader.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    _MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                }
            } while (theWord != null);

        } catch (IOException ioe) {
            log.error("Loading Main Dictionary occur exception.");
        } finally {
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
                log.error("[loadMainDict]Closing InputStream occur exception.");
            }
        }
        //加载扩展词典
        this.loadExtDict();
    }

    /**
     * 加载用户配置的扩展词典到主词库表
     */
    private void loadExtDict() {
        //加载扩展词典配置
        List<String> extDictFiles = cfg.getExtDictionarys();
        if (extDictFiles != null) {
            InputStream is = null;
            for (String extDictName : extDictFiles) {
                //读取扩展词典文件
                log.info("加载扩展词典:" + extDictName);
                is = this.getClass().getClassLoader().getResourceAsStream(extDictName);
                //如果找不到扩展的字典，则忽略
                if (is == null) {
                    continue;
                }
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, DEFAULT_CHARSET),  BUFFER_SIZE);
                    String theWord = null;
                    do {
                        theWord = bufferedReader.readLine();
                        if (theWord != null && !"".equals(theWord.trim())) {
                            //加载扩展词典数据到主内存词典中
                            _MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                        }
                    } while (theWord != null);

                } catch (IOException ioe) {
                    log.error("Loading Extension Dictionary occur exception.");
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                            is = null;
                        }
                    } catch (IOException e) {
                        log.error("[loadExtDict]Closing InputStream occur exception.");
                    }
                }
            }
        }
    }

    /**
     * 加载用户扩展的停止词词典
     */
    private void loadStopWordDict() {
        //建立一个主词典实例
        _StopWordDict = new DictSegment((char) 0);
        //加载扩展停止词典
        List<String> extStopWordDictFiles = cfg.getExtStopWordDictionarys();
        if (extStopWordDictFiles != null) {
            InputStream inputStream = null;
            for (String extStopWordDictName : extStopWordDictFiles) {
                log.info("加载扩展停用词词典：" + extStopWordDictName);
                //读取扩展词典文件
                inputStream = this.getClass().getClassLoader().getResourceAsStream(extStopWordDictName);
                //如果找不到扩展的字典，则忽略
                if (inputStream == null) {
                    continue;
                }
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CHARSET), BUFFER_SIZE);
                    String theWord = null;
                    do {
                        theWord = bufferedReader.readLine();
                        if (theWord != null && !"".equals(theWord.trim())) {
                            //加载扩展停止词典数据到内存中
                            _StopWordDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                        }
                    } while (theWord != null);

                } catch (IOException ioe) {
                    log.error("[loadStopWordDict]Loading Extension Stop word Dictionary occur exception.");
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                            inputStream = null;
                        }
                    } catch (IOException e) {
                        log.error("[loadStopWordDict]Closing InputStream occur exception.");
                    }
                }
            }
        }
    }

    /**
     * 加载自定义词典文件
     */
    private DictSegment loadCustomDict(String dicPath, DictSegment dictSegment) {
        //建立一个量词典实例
        if (null == dictSegment) {
            dictSegment = new DictSegment((char) 0);
        }

        //读取量词词典文件
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(dicPath);
        String dicFileName = getDicFileName(dicPath);
        if (inputStream == null) {
            throw new RuntimeException("自定义词典:{" + dicFileName + "} not found!!!");
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CHARSET), BUFFER_SIZE);
            log.info("加载自定义词典：" + dicFileName);
            String theWord = null;
            do {
                theWord = bufferedReader.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    dictSegment.fillSegment(theWord.trim().toLowerCase().toCharArray());
                }
            } while (theWord != null);

        } catch (IOException ioe) {
            log.error("Loading " + dicFileName + " occur exception.");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
            } catch (IOException e) {
                log.error("[loadCustomDict]Closing InputStream occur exception.");
            }
        }
        return dictSegment;
    }

    //获取字典文件的文件名称
    private String getDicFileName(String dicPath) {
        if (null == dicPath || "".equals(dicPath)) {
            return "";
        }
        int index = dicPath.lastIndexOf("/");
        if (index == -1) {
            return dicPath;
        }
        return dicPath.substring(index + 1);
    }
}
