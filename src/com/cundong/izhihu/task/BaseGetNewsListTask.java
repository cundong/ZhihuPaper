package com.cundong.izhihu.task;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.text.TextUtils;

import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.http.HttpClientUtils;

/**
 * 类说明： 	从服务器下载新闻列表，base Task
 * 
 * @date 	2014-9-7
 * @version 1.0
 */
public abstract class BaseGetNewsListTask extends MyAsyncTask<String, String, ArrayList<NewsEntity>> {
	
	protected Context mContext = null;

	protected ResponseListener mListener = null;
	
	protected Exception e = null;
	
	protected boolean isRefreshSuccess = true;

	protected boolean isContentSame = false;
	
	public BaseGetNewsListTask(Context context, ResponseListener listener) {
		mContext = context;
		mListener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		if (mListener != null) {
			mListener.onPreExecute();
		}
	}

	@Override
	protected void onPostExecute(ArrayList<NewsEntity> resultList) {
		super.onPostExecute(resultList);

		// 如果当前任务已经取消了，则直接返回
		if (isCancelled()) {
			return;
		}

		// 写数据库
		if (isRefreshSuccess && !isContentSame) {
			// new SaveNewsListTask(mDate,
			// content).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
		}

		if (mListener != null) {
			if (isRefreshSuccess) {
				mListener.onPostExecute(resultList, isRefreshSuccess, isContentSame);
			} else {
				mListener.onFail(e);
			}
		}
	}

	protected boolean checkIsContentSame(String oldContent, String newContent) {
		
		if (TextUtils.isEmpty(oldContent) || TextUtils.isEmpty(newContent)) {
			return false;
		}

		return oldContent.equals(newContent);
	}
	
	protected String getUrl(String url) throws IOException, Exception {
		return HttpClientUtils.get(mContext, url, null);
	}
	
	public static interface ResponseListener {
		
		public void onPreExecute();

		public void onPostExecute(ArrayList<NewsEntity> resultList, boolean isRefreshSuccess, boolean isContentSame);

		public void onProgressUpdate(String value);
		
		public void onFail(Exception e);
	}
}