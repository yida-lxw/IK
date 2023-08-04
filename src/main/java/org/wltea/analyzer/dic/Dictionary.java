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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.cfg.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 词典管理类,单子模式
 */
public class Dictionary {
    private static final Logger log = LogManager.getLogger(Dictionary.class);

    /**定时更新IK词库的线程池*/
    private static ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);

    private static final int BUFFER_SIZE = 2048;

    private static final String DEFAULT_CHARSET = "UTF-8";

    /*
     * 词典单子实例
     */
    private static volatile Dictionary singleton;

    /*
     * 主词典对象
     */
    private AtomicReference<DictSegment> _MainDictAtomicReference;

    /*
     * 停止词词典
     */
    private AtomicReference<DictSegment> _StopWordDictAtomicReference;
    /*
     * 中文量词词典
     */
    private AtomicReference<DictSegment> _QuantifierDictAtomicReference;

    //英文单位词典
    private AtomicReference<DictSegment> _EnUnitDictAtomicReference;

    /**
     * 配置对象
     */
    private Configuration cfg;

    private Dictionary(Configuration cfg) {
        //初始化字典有关的原子变量
        _MainDictAtomicReference = new AtomicReference<>();
        _StopWordDictAtomicReference = new AtomicReference<>();
        _QuantifierDictAtomicReference = new AtomicReference<>();
        _EnUnitDictAtomicReference = new AtomicReference<>();

        cfg.setEnableRemoteDict();
        cfg.setRemoteExtDictRefreshInterval();
        this.cfg = cfg;
        loadAllDicts(this);
    }

    /**
     * 加载IK分词器的所有词典文件
     */
    private void loadAllDicts(Dictionary dictionary) {
        //加载主扩展词库词典文件
        dictionary.loadMainDict();
        //加载停用词词典文件
        dictionary.loadStopWordDict();
        _MainDictAtomicReference.set(dictionary._MainDictAtomicReference.get());
        this._StopWordDictAtomicReference.set(dictionary.getStopWordDict());
        //加载英文单位词典文件
        this._EnUnitDictAtomicReference.set(dictionary.loadCustomDict(dictionary.cfg.getEnglishUnitDicionary(), dictionary.getEnUnitDict()));
        //加载中文量词词典文件
        this._QuantifierDictAtomicReference.set(dictionary.loadCustomDict(dictionary.cfg.getQuantifierDicionary(), dictionary.getQuantifierDict()));
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
                    if(cfg.enableRemoteDict()) {
                        long remoteExtDictRefreshInterval = cfg.remoteExtDictRefreshInterval();
                        // 建立监控线程
                        for (String location : cfg.getRemoteExtDictionarys()) {
                            // 10 秒是初始延迟， 60是间隔时间，单位秒
                            threadPool.scheduleAtFixedRate(new Monitor(location), 10, remoteExtDictRefreshInterval, TimeUnit.SECONDS);
                        }
                        for (String location : cfg.getRemoteExtStopWordDictionarys()) {
                            threadPool.scheduleAtFixedRate(new Monitor(location), 10, remoteExtDictRefreshInterval, TimeUnit.SECONDS);
                        }
                    }
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
                    singleton.getMainDict().fillSegment(word.trim().toLowerCase().toCharArray());
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
                    singleton.getMainDict().disableSegment(word.trim().toLowerCase().toCharArray());
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
        return singleton.getMainDict().match(charArray);
    }

    /**
     * 检索匹配主词典
     * @param charArray
     * @param begin
     * @param length
     * @return Hit 匹配结果描述
     */
    public Hit matchInMainDict(char[] charArray, int begin, int length) {
        return singleton.getMainDict().match(charArray, begin, length);
    }

    /**
     * 检索匹配量词词典
     * @param charArray
     * @param begin
     * @param length
     * @return Hit 匹配结果描述
     */
    public Hit matchInQuantifierDict(char[] charArray, int begin, int length) {
        return singleton.getQuantifierDict().match(charArray, begin, length);
    }

    /**
     * 检索匹配英文单位词典
     * @param charArray
     * @param begin
     * @param length
     * @return Hit 匹配结果描述
     */
    public Hit matchInENUnitDict(char[] charArray, int begin, int length) {
        return singleton.getEnUnitDict().match(charArray, begin, length);
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
        return singleton.getQuantifierDict().match(charArray, begin, length).isMatch();
    }

    /**
     * 判断是否为停用词
     * @param charArray
     * @param begin
     * @param length
     * @return
     */
    public boolean isStopWord(char[] charArray, int begin, int length) {
        return singleton.getStopWordDict().match(charArray, begin, length).isMatch();
    }

    /**
     * 加载主词典及扩展词典
     */
    private void loadMainDict() {
        //建立一个主词典实例
        this._MainDictAtomicReference.set(new DictSegment((char) 0));
        //读取主词典文件
        String mainDictPath = this.cfg.getMainDictionary();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(mainDictPath);
        String mainDictName = getDicFileName(mainDictPath);

        if (inputStream == null) {
            throw new RuntimeException("Main Dictionary:{" + mainDictName + "} not found!!!");
        }
        readDict(inputStream, this.getMainDict(), "主词典", mainDictName);
        //加载扩展词典
        this.loadExtDict();
    }

    /**
     * 加载用户配置的扩展词典到主词库表
     */
    private void loadExtDict() {
        //加载扩展词典配置
        List<String> extDictFiles = this.cfg.getExtDictionarys();
        if (extDictFiles != null) {
            InputStream inputStream = null;
            for (String extDictName : extDictFiles) {
                //读取扩展词典文件
                log.info("加载扩展词典:" + extDictName);
                inputStream = this.getClass().getClassLoader().getResourceAsStream(extDictName);
                //如果找不到扩展的字典，则忽略
                if (inputStream == null) {
                    continue;
                }
                readDict(inputStream, this.getMainDict(), "扩展词典", extDictName);
            }
        }
    }

    /**
     * 加载用户扩展的停止词词典
     */
    private void loadStopWordDict() {
        //建立一个主词典实例
        this._StopWordDictAtomicReference.set(new DictSegment((char) 0));
        //加载扩展停止词典
        List<String> extStopWordDictFiles = this.cfg.getExtStopWordDictionarys();
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
                readDict(inputStream, this.getStopWordDict(), "扩展停用词词典", extStopWordDictName);
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
        readDict(inputStream, dictSegment, "自定义词典", dicFileName);
        return dictSegment;
    }

    public void reLoadMainDict() {
        log.info("start to reload ik dict.");
        // 新开一个实例加载词典，减少加载过程对当前词典使用的影响
        Dictionary newDict = new Dictionary(this.cfg);
        this.loadAllDicts(newDict);
        log.info("reload ik dict finished.");
    }

    /**
     * 读取词典文件到词典树中
     *
     * @param inputStream          文件输入流
     * @param dictSegment 词典树分段
     * @throws IOException 读取异常
     */
    private void readDict(InputStream inputStream, DictSegment dictSegment, String dictFileName, String dictNameCN) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CHARSET), BUFFER_SIZE);
            log.info("加载" + dictNameCN + "：" + dictFileName);
            String theWord = null;
            do {
                theWord = bufferedReader.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    dictSegment.fillSegment(theWord.trim().toLowerCase().toCharArray());
                }
            } while (theWord != null);
        } catch (Exception e) {
            log.error("Loading [" + dictFileName + "] occur exception.");
        } finally {
            closeInputStream(inputStream);
        }
    }

    private void closeInputStream(InputStream inputStream) {
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (IOException e) {
            log.error("Closing InputStream occur exception.");
        }
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

    public DictSegment getMainDict() {
        return this._MainDictAtomicReference.get();
    }

    public DictSegment getStopWordDict() {
        return this._MainDictAtomicReference.get();
    }

    public DictSegment getQuantifierDict() {
        return this._QuantifierDictAtomicReference.get();
    }

    public DictSegment getEnUnitDict() {
        return this._EnUnitDictAtomicReference.get();
    }
}
