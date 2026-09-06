package com.cundong.izhihu.http;

/**
 * 请求响应接口
 */
public interface ResponseListener {

	/**
	 * 请求正常完成
	 * 
	 * @param response
	 */
	public void onComplete(String response);

	/**
	 * 请求过程发生异常
	 * 
	 * @param e
	 */
	public void onFail(Exception e);
}