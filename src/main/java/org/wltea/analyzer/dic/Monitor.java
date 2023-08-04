package org.wltea.analyzer.dic;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.cfg.DefaultConfig;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class Monitor implements Runnable {
	private static final Logger log = LogManager.getLogger(Monitor.class);

	private static CloseableHttpClient httpclient = HttpClients.createDefault();

	/*
	 * 上次更改时间
	 */
	private String last_modified;
	/*
	 * 资源属性
	 */
	private String eTags;

	/*
	 * 请求地址
	 */
	private String location;

	/**远程扩展词典自动刷新时间间隔，单位：秒*/
	private long remoteExtDictRefreshInterval;

	public Monitor(String location, long remoteExtDictRefreshInterval) {
		this.location = location;
		this.last_modified = null;
		this.eTags = null;
		this.remoteExtDictRefreshInterval = remoteExtDictRefreshInterval;
	}

	public Monitor(String location) {
		this(location, DefaultConfig.DEFAULT_REMOTE_EXT_DICT_REFRESH_INTERVAL);
	}

	public void run() {
		AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
			this.runUnprivileged();
			return null;
		});
	}

	/**
	 * 监控流程：
	 *  ①向词库服务器发送Head请求
	 *  ②从响应中获取Last-Modify、ETags字段值，判断是否变化
	 *  ③如果未变化，休眠1min，返回第①步
	 * 	④如果有变化，重新加载词典
	 *  ⑤休眠1min，返回第①步
	 */

	public void runUnprivileged() {
		log.info("Begin to load remote ext-dict:[{}] with the fixed refreshInterval:[{}]", location, remoteExtDictRefreshInterval);
		//超时设置
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(10*1000)
				.setConnectTimeout(10*1000).setSocketTimeout(30*1000).build();
		HttpHead head = new HttpHead(location);
		head.setConfig(requestConfig);

		//设置请求头
		if (last_modified != null) {
			head.setHeader("If-Modified-Since", last_modified);
		}
		if (eTags != null) {
			head.setHeader("If-None-Match", eTags);
		}

		CloseableHttpResponse response = null;
		try {
			response = httpclient.execute(head);
			//返回200 才做操作
			if(response.getStatusLine().getStatusCode() == 200){
				if (((response.getLastHeader("Last-Modified")!=null) && !response.getLastHeader("Last-Modified").getValue().equalsIgnoreCase(last_modified))
						||((response.getLastHeader("ETag")!=null) && !response.getLastHeader("ETag").getValue().equalsIgnoreCase(eTags))) {
					// 远程词库有更新,需要重新加载词典，并修改last_modified,eTags
					Dictionary.getSingleton().reLoadMainDict();
					last_modified = response.getLastHeader("Last-Modified")==null?null:response.getLastHeader("Last-Modified").getValue();
					eTags = response.getLastHeader("ETag")==null?null:response.getLastHeader("ETag").getValue();
					log.info("The remote ext-dict:[{}] had been loaded with the fixed refreshInterval:[{}] successfully.", location, remoteExtDictRefreshInterval);
				}
			} else if (response.getStatusLine().getStatusCode() == 304) {
				//没有修改，不做操作
				log.info("The remote dictionary:[{}] had been modified.", location);
			} else{
				log.info("remote_ext_dict: {} return bad code {}" , location , response.getStatusLine().getStatusCode() );
			}
		} catch (Exception e) {
			log.error("remote_ext_dict {} error!",e , location);
		} finally{
			try {
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

}
