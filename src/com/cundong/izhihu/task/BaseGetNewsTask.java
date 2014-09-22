package com.cundong.izhihu.task;

import android.text.TextUtils;
import android.widget.Toast;

public abstract class BaseGetNewsTask extends BaseDownloadTask {

	public BaseGetNewsTask(ResponseListener listener) {
		super(listener);
	}

	protected boolean isRefreshSuccess = true;

	protected boolean isContentSame = false;
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		//网络判断  
//        if (!NetworkUtils.isNetworkAvailable(context)) {  
//            cancel(true);  
//            Toast.makeText(context, ToastMessage.NO_NET.value, Toast.LENGTH_SHORT).show();  
//            showRefreshView();  
//        } 
        
		mListener.onPreExecute();
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
		
		if (isRefreshSuccess) {
			mListener.onPostExecute(content, isRefreshSuccess, isContentSame);
		} else {
			mListener.onFail(e);
		}
	}

	protected boolean checkIsContentSame(String oldContent, String newContent) {
		
		if (TextUtils.isEmpty(oldContent)||TextUtils.isEmpty(newContent)) {
			return false;
		}

		return oldContent.equals(newContent);
	}
}