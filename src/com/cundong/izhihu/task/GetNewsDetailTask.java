package com.cundong.izhihu.task;

import java.io.IOException;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;

public class GetNewsDetailTask extends BaseGetNewsTask {

	public GetNewsDetailTask(ResponseListener listener) {
		super(listener);
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
		}

		isContentSame = checkIsContentSame(params[0], content);

		return content;
	}
}