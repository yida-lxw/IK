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
package org.wltea.analyzer.cfg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;

/**
 * Configuration 默认实现
 * 2012-5-8
 *
 */
public class DefaultConfig implements Configuration {
    private static final Logger log = LogManager.getLogger(DefaultConfig.class);

    /**远程扩展词典自动刷新的时间间隔,单位：秒,默认值为60*/
    public static final long DEFAULT_REMOTE_EXT_DICT_REFRESH_INTERVAL = 60L;

    /*
     * 分词器默认字典路径
     */
    private static final String PATH_DIC_MAIN = "config/main2012.dic";
    private static final String PATH_DIC_QUANTIFIER = "config/quantifier.dic";

    //英文单位字典文件加载路径
    private static final String PATH_DIC_EN_UNIT = "config/en_unit.dic";

    /*
     * 分词器配置文件路径
     */
    private static final String FILE_NAME = "IKAnalyzer.cfg.xml";
    //配置属性——扩展字典
    private static final String EXT_DICT = "ext_dict";
    //配置属性——远程扩展字典
    private final static  String REMOTE_EXT_DICT = "remote_ext_dict";
    //配置属性——扩展停用词词典
    private static final String EXT_STOP = "ext_stopwords";
    //配置属性——远程扩展停用词词典
    private static final String REMOTE_EXT_STOPWORD_DICT = "remote_ext_stopwords";

    //配置属性——是否启用远程扩展词典
    private static final String ENABLE_REMOTE_DICT = "enable_remote_dict";

    //配置属性——远程扩展词典自动刷新时间间隔，单位：秒，默认60s
    private static final String REMOTE_EXT_DICT_REFRESH_INTERVAL = "remote_ext_dict_refresh_interval";

    private Properties props;
    /*
     * 是否使用smart方式分词
     */
    private boolean useSmart;

    /**
     * 是否启用远程词典加载
     */
    private boolean enableRemoteDict;

    /**远程扩展词典的刷新时间间隔，单位：秒*/
    private long remoteExtDictRefreshInterval;

    /**
     * 返回单例
     * @return Configuration单例
     */
    public static Configuration getInstance() {
        return new DefaultConfig();
    }

    /*
     * 初始化配置文件
     */
    private DefaultConfig() {
        props = new Properties();
        InputStream input = this.getClass().getClassLoader().getResourceAsStream(FILE_NAME);
        if (input != null) {
            try {
                props.loadFromXML(input);
            } catch (InvalidPropertiesFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 返回useSmart标志位
     * useSmart =true ，分词器使用智能切分策略， =false则使用细粒度切分
     * @return useSmart
     */
    @Override
    public boolean useSmart() {
        return useSmart;
    }

    /**
     * 设置useSmart标志位
     * useSmart =true ，分词器使用智能切分策略， =false则使用细粒度切分
     * @param useSmart
     */
    @Override
    public void setUseSmart(boolean useSmart) {
        this.useSmart = useSmart;
    }

    /**
     * 设置是否启用远程词典加载
     * @return
     */
    @Override
    public boolean enableRemoteDict() {
        return enableRemoteDict;
    }

    /**
     * 从配置文件中读取enableRemoteDict配置项的值
     */
    @Override
    public void setEnableRemoteDict() {
        String enableRemoteDict = props.getProperty(ENABLE_REMOTE_DICT);
        this.enableRemoteDict = (null != enableRemoteDict && !"".equals(enableRemoteDict) &&
                ("true".equalsIgnoreCase(enableRemoteDict) || "yes".equalsIgnoreCase(enableRemoteDict) ||
                        "on".equalsIgnoreCase(enableRemoteDict) || "ok".equalsIgnoreCase(enableRemoteDict) ||
                        "1".equalsIgnoreCase(enableRemoteDict)));
    }

    /**
     * 获取远程扩展词典刷新的时间间隔(单位:秒)
     * @return
     */
    @Override
    public long remoteExtDictRefreshInterval() {
        return remoteExtDictRefreshInterval;
    }

    @Override
    public void setRemoteExtDictRefreshInterval() {
        String remoteExtDictRefreshInterval = props.getProperty(REMOTE_EXT_DICT_REFRESH_INTERVAL);
        if(null == remoteExtDictRefreshInterval || "".equalsIgnoreCase(remoteExtDictRefreshInterval)) {
            this.remoteExtDictRefreshInterval = DEFAULT_REMOTE_EXT_DICT_REFRESH_INTERVAL;
        } else {
            try {
                this.remoteExtDictRefreshInterval = Long.parseLong(remoteExtDictRefreshInterval);
            } catch (Exception e) {
                log.error("The configured parameter:[{}] value:[{}] cannot be converted to an Long type, it will be set to default value:[{}]",
                        REMOTE_EXT_DICT_REFRESH_INTERVAL, remoteExtDictRefreshInterval, DEFAULT_REMOTE_EXT_DICT_REFRESH_INTERVAL);
                this.remoteExtDictRefreshInterval = DEFAULT_REMOTE_EXT_DICT_REFRESH_INTERVAL;
            }
        }
    }

    /**
     * 获取主词典路径
     *
     * @return String 主词典路径
     */
    @Override
    public String getMainDictionary() {
        return PATH_DIC_MAIN;
    }

    /**
     * 获取量词词典路径
     * @return String 量词词典路径
     */
    @Override
    public String getQuantifierDicionary() {
        return PATH_DIC_QUANTIFIER;
    }

    /**
     * 获取英文单位词典路径
     * @return String 英文单位词典文件路径
     */
    @Override
    public String getEnglishUnitDicionary() {
        return PATH_DIC_EN_UNIT;
    }

    /**
     * 获取扩展字典配置路径
     * @return List<String> 相对类加载器的路径
     */
    @Override
    public List<String> getExtDictionarys() {
        List<String> extDictFiles = new ArrayList<String>(2);
        String extDictCfg = props.getProperty(EXT_DICT);
        if (extDictCfg != null) {
            //使用;分割多个扩展字典配置
            String[] filePaths = extDictCfg.split(";");
            if (filePaths != null) {
                for (String filePath : filePaths) {
                    if (filePath != null && !"".equals(filePath.trim())) {
                        extDictFiles.add(filePath.trim());
                    }
                }
            }
        }
        return extDictFiles;
    }

    /**
     * 获取远程扩展词词典的URL路径
     * @return
     */
    @Override
    public List<String> getRemoteExtDictionarys() {
        List<String> remoteExtDictFiles = new ArrayList<String>(2);
        String remoteExtDictCfg = props.getProperty(REMOTE_EXT_DICT);
        if (remoteExtDictCfg != null) {
            String[] filePaths = remoteExtDictCfg.split(";");
            for (String filePath : filePaths) {
                if (filePath != null && !"".equals(filePath.trim())) {
                    remoteExtDictFiles.add(filePath);

                }
            }
        }
        return remoteExtDictFiles;
    }

    /**
     * 获取远程扩展停用词词典的URL路径
     * @return
     */
    @Override
    public List<String> getRemoteExtStopWordDictionarys() {
        List<String> remoteExtStopWordDictFiles = new ArrayList<String>(2);
        String remoteExtStopWordDictCfg = props.getProperty(REMOTE_EXT_STOPWORD_DICT);
        if (remoteExtStopWordDictCfg != null) {
            String[] filePaths = remoteExtStopWordDictCfg.split(";");
            for (String filePath : filePaths) {
                if (filePath != null && !"".equals(filePath.trim())) {
                    remoteExtStopWordDictFiles.add(filePath);
                }
            }
        }
        return remoteExtStopWordDictFiles;
    }


    /**
     * 获取扩展停止词典配置路径
     * @return List<String> 相对类加载器的路径
     */
    public List<String> getExtStopWordDictionarys() {
        List<String> extStopWordDictFiles = new ArrayList<String>(2);
        String extStopWordDictCfg = props.getProperty(EXT_STOP);
        if (extStopWordDictCfg != null) {
            //使用;分割多个扩展字典配置
            String[] filePaths = extStopWordDictCfg.split(";");
            if (filePaths != null) {
                for (String filePath : filePaths) {
                    if (filePath != null && !"".equals(filePath.trim())) {
                        extStopWordDictFiles.add(filePath.trim());
                    }
                }
            }
        }
        return extStopWordDictFiles;
    }
}
