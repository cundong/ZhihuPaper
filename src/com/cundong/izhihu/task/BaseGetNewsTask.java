package com.cundong.izhihu.task;

import java.util.ArrayList;

import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;

public abstract class BaseGetNewsTask extends BaseDownloadTask {

	public BaseGetNewsTask(ResponseListener listener) {
		super(listener);
		// TODO Auto-generated constructor stub
	}

	protected boolean isRefreshSuccess = true;

	protected boolean isContentSame = false;

	protected String mDate;



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

	protected boolean checkIsContentSame(ArrayList<NewsEntity> externalNewsList) {

		ArrayList<NewsEntity> currentList = ZhihuApplication.getDataSource()
				.getNewsList(mDate);

		if (externalNewsList == null || currentList == null
				|| externalNewsList.isEmpty() || currentList.isEmpty()) {
			return false;
		}

		return externalNewsList.equals(currentList);
	}
}