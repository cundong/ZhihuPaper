package com.cundong.izhihu.task;

import android.text.TextUtils;

import com.cundong.izhihu.ZhihuApplication;

public abstract class BaseGetNewsTask extends BaseDownloadTask {

	public BaseGetNewsTask(ResponseListener listener) {
		super(listener);
	}

	protected boolean isRefreshSuccess = true;

	protected boolean isContentSame = false;
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		mListener.onPre();
	}

	@Override
	protected void onPostExecute(String content) {
		super.onPostExecute(content);

		// 写数据库
		if (isRefreshSuccess && !isContentSame) {
			// new SaveNewsListTask(mDate,
			// content).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
		}

		mListener.onComplete(content, isRefreshSuccess, isContentSame);
	}

	protected boolean checkIsContentSame(String date, String input) {
		
		String content = ZhihuApplication.getDataSource().getContent(date);
		
		if (TextUtils.isEmpty(content)||TextUtils.isEmpty(input)) {
			return false;
		}

		return input.equals(content);
	}
}