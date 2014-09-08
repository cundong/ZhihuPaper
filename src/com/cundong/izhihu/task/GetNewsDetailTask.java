package com.cundong.izhihu.task;

import java.io.IOException;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;

public class GetNewsDetailTask extends BaseDownloadTask {

	public GetNewsDetailTask(ResponseListener listener) {
		super(listener);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String doInBackground(String... params) {
		
		String content = null;

		try {
			
			content = getUrl(Constants.Url.URL_DETAIL + params[0] );
			
			ZhihuApplication.getDataSource().insertOrUpdateNewsList("detail_"+params[0], content);
			
		} catch (IOException e) {
			e.printStackTrace();
			
			mListener.onFail(e);
		}

		return null;
	}
}