package com.cundong.izhihu.task;

import java.io.IOException;

import android.content.Context;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;

/**
 * 类说明： 	下载新闻详情页内容，Task
 * 
 * @date 	2014-9-7
 * @version 1.0
 */
public class GetNewsDetailTask extends BaseGetNewsTask {

	public GetNewsDetailTask(Context context, ResponseListener listener) {
		super(context, listener);
	}

	@Override
	protected String doInBackground(String... params) {

		String content = null;

		try {
			content = getUrl(Constants.Url.URL_DETAIL + params[0]);
			ZhihuApplication.getDataSource().insertOrUpdateNewsList(
					"detail_" + params[0], content);
		} catch (IOException e) {
			e.printStackTrace();

			isRefreshSuccess = false;
			this.e = e;
		} catch (Exception e) {
			e.printStackTrace();
			
			isRefreshSuccess = false;
			this.e = e;
		}

		isContentSame = checkIsContentSame(params[0], content);

		return content;
	}
}