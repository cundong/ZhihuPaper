package com.cundong.izhihu.task;

/**
 * 类说明： 	Task的回调接口
 * 
 * @date 	2014-9-15
 * @version 1.0
 */
public interface ResponseListener {

	public void onPreExecute();

	public void onPostExecute(String content);

	public void onProgressUpdate(String value);

	public void onFail(Exception e);
}