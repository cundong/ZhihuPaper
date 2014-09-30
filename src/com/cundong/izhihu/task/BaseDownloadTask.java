package com.cundong.izhihu.task;

import java.io.IOException;

import android.content.Context;

import com.cundong.izhihu.http.HttpClientUtils;

public abstract class BaseDownloadTask extends
		MyAsyncTask<String, String, String> {

	protected Context mContext = null;

	protected ResponseListener mListener = null;
	protected Exception e = null;

	public BaseDownloadTask(Context context, ResponseListener listener) {
		mContext = context;
		mListener = listener;
	}

	protected String getUrl(String url) throws IOException, Exception {
		return HttpClientUtils.get(mContext, url, null);
	}
}