package com.cundong.izhihu.task;

import android.content.Context;
import android.text.TextUtils;

public abstract class BaseGetNewsTask extends BaseDownloadTask {

	public BaseGetNewsTask(Context context, ResponseListener listener) {
		super(context, listener);
	}

	protected boolean isRefreshSuccess = true;

	protected boolean isContentSame = false;
	
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

		//如果当前任务已经取消了，则直接返回  
        if(isCancelled()){  
            return;  
        }  
        
		// 写数据库
		if (isRefreshSuccess && !isContentSame) {
			// new SaveNewsListTask(mDate,
			// content).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
		}
		
		if (mListener != null) {
			if (isRefreshSuccess) {
				mListener.onPostExecute(content, isRefreshSuccess, isContentSame);
			} else {
				mListener.onFail(e);
			}
		}
	}

	protected boolean checkIsContentSame(String oldContent, String newContent) {
		
		if (TextUtils.isEmpty(oldContent)||TextUtils.isEmpty(newContent)) {
			return false;
		}

		return oldContent.equals(newContent);
	}
}