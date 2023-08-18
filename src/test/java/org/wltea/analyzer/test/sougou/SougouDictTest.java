package org.wltea.analyzer.test.sougou;

import java.io.IOException;

public class SougouDictTest {
    public static void main(String[] args) {
        //单个scel文件转化
        FileProcessing scel = new SougouScelFileProcessing();
        //scel.parseFile("./resolver/src/main/java/cn/ucmed/constant/药品名称大全.scel", "./resolver/src/main/java/cn/ucmed/constant/药品名称大全.txt", true);

        //多个scel文件转化为一个txt (格式：拼音字母 词)
        try {
            scel.parseFiles("/Users/yida/Downloads/sougouDicts/", "/Users/yida/Downloads/sougouDicts/all.txt", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //多个scel文件转化为多个txt文件, 转化后文件的存储位置
        //scel.setTargetDir("/Users/ST_iOS/Desktop/test/ciku/多对多");
        //scel.parseFile("/Users/ST_iOS/Desktop/test/ciku", false);
    }
}
