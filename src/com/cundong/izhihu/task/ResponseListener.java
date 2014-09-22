package com.cundong.izhihu.task;

public interface ResponseListener {
	
	public void onPreExecute();

	public void onPostExecute(String content, boolean isRefreshSuccess, boolean isContentSame);

	public void onProgressUpdate(String value);
	
	public void onFail(Exception e);
}