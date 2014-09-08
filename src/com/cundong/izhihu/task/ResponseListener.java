package com.cundong.izhihu.task;


public interface ResponseListener {
	
	public void onPre();

	public void onComplete(String content, boolean isRefreshSuccess, boolean isContentSame);

	public void onFail(final Exception e);
}