package com.cundong.izhihu.task;

import java.io.IOException;

import android.content.Context;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;

/**
 * 类说明： 	新闻列表数据下载Task
 * 
 * @date 	2014-9-15
 * @version 1.0
 */
public class GetNewsTask extends BaseGetNewsTask {

	public GetNewsTask(Context context, ResponseListener listener) {
		super(context, listener);
	}

	@Override
	protected String doInBackground(String... params) {

		if (params.length == 0)
			return null;
		
		String theKey = params[0];
		String oldContent = ZhihuApplication.getDataSource().getContent(theKey);

		String newContent = null;

		try {
			newContent = getUrl(Constants.Url.URL_LATEST);
			ZhihuApplication.getDataSource().insertOrUpdateNewsList(theKey, newContent);
		} catch (IOException e) {
			e.printStackTrace();
			
			this.isRefreshSuccess = false;
			this.e = e;
		} catch (Exception e) {
			e.printStackTrace();

			this.isRefreshSuccess = false;
			this.e = e;
		}
		
		isContentSame = checkIsContentSame(oldContent, newContent);

		return newContent;
	}
}