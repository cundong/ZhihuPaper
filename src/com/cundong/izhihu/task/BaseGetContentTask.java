package com.cundong.izhihu.task;

import java.io.IOException;

import com.cundong.izhihu.http.HttpClientUtils;

import android.content.Context;
import android.text.TextUtils;

/**
 * 类说明： 	从服务器下载内容，base Task
 * 
 * @date 	2014-9-7
 * @version 1.0
 */
public abstract class BaseGetContentTask extends MyAsyncTask<String, String, String> {
	
	protected Context mContext = null;

	protected ResponseListener mListener = null;
	protected Exception e = null;
	
	protected boolean isRefreshSuccess = true;

	protected boolean isContentSame = false;
	
	public BaseGetContentTask(Context context, ResponseListener listener) {
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
				mListener.onPostExecute(content);
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
	
	protected String getUrl(String url) throws IOException, Exception {
		return HttpClientUtils.get(mContext, url, null);
	}
}