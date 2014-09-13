package com.cundong.izhihu.task;

import java.io.IOException;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;

public class GetNewsTask extends BaseGetNewsTask {

	public GetNewsTask(ResponseListener listener) {
		super(listener);
	}
	
	@Override
	protected String doInBackground(String... params) {
		
		String oldContent = ZhihuApplication.getDataSource().getContent(params[0]);
		
		String newContent = null;
		
		try {
			newContent = getUrl( Constants.Url.URL_LATEST );
			ZhihuApplication.getDataSource().insertOrUpdateNewsList(params[0], newContent);
		} catch (IOException e) {
			e.printStackTrace();
			
			isRefreshSuccess = false;
			mListener.onFail(e);
		}
		
		isContentSame = checkIsContentSame(oldContent, newContent);

		return newContent;
	}
}