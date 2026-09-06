package com.cundong.izhihu.task;

import java.io.IOException;

import android.content.Context;
import android.text.TextUtils;

import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.db.BaseDataSource;
import com.cundong.izhihu.http.HttpUtils;

/**
 * 类说明： 	从服务器下载内容，base Task
 * 
 * @date 	2014-9-7
 * @version 1.0
 */
public abstract class BaseGetContentTask extends BackgroundTask<String, String, String> {
	
	protected Context mContext = null;

	protected ResponseListener mListener = null;
	
	protected Exception mException = null;
	
	protected boolean isRefreshSuccess = true;

	protected boolean isContentSame = false;
	
	public BaseGetContentTask(Context context, ResponseListener listener) {
		mContext = context == null ? null : context.getApplicationContext();
		mListener = listener;
	}

	/** Releases UI callbacks; lifecycle owners should also cancel running work. */
	public final void clearListener() {
		mListener = null;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		if (mListener != null) {
			mListener.onPreExecute();
		}
	}

	@Override
	protected void onPostExecute(String content) {
		super.onPostExecute(content);

		// 如果当前任务已经取消了，则直接返回
		if (isCancelled()) {
			return;
		}

		if (mListener != null) {
			if (isRefreshSuccess) {
				mListener.onPostExecute(content);
			} else {
				mListener.onFail(mException);
			}
		}
	}

	protected boolean checkIsContentSame(String oldContent, String newContent) {
		
		if (TextUtils.isEmpty(oldContent)||TextUtils.isEmpty(newContent)) {
			return false;
		}

		return oldContent.equals(newContent);
	}
	
	protected String getUrl(String url) throws IOException, Exception {
		return HttpUtils.get(mContext, url, null);
	}
	
	protected BaseDataSource getDataSource() {
		return ZhihuApplication.getDataSource();
	}
}
