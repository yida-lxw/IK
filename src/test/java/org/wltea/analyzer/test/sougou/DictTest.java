package org.wltea.analyzer.test.sougou;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author yida
 * @package org.wltea.analyzer.test.sougou
 * @date 2023-10-21 13:17
 * @description Type your description over here.
 */
public class DictTest {
	public static void main(String[] args) throws Exception {
		String sourceDictTestPath = "C:\\Users\\Administrator\\Desktop\\analysis-ik\\config\\main2012.dic";
		String targetDictTestPath = "C:\\Users\\Administrator\\Desktop\\analysis-ik\\config\\main2023.dic";
		List<String> allLiines = Files.readAllLines(Paths.get(sourceDictTestPath));
		Set<String> lineSet = new TreeSet<>();
		for (String line : allLiines) {
			lineSet.add(line);
		}
		writeToTargetFile(lineSet, targetDictTestPath);

	}

	/**
	 * 将内容写入目标文件
	 *
	 * @param set            词库合集
	 * @param outputFilePath 目标文件
	 */
	private static void writeToTargetFile(Set<String> set, String outputFilePath) {
		File outputFile = new File(outputFilePath);
		StringBuffer buff = new StringBuffer();
		for (String content : set) {
			buff.append(content);
			buff.append("\r\n");
		}
		String content = buff.toString();

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(outputFile);
			out.write(content.getBytes());

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
