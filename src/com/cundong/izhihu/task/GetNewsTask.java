package com.cundong.izhihu.task;

import java.io.IOException;
import java.util.ArrayList;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;

public class GetNewsTask extends BaseGetNewsTask {

	public GetNewsTask(String date, ResponseListener listener) {
		super(date, listener);
	}

	@Override
	protected String doInBackground(String... params) {
		ArrayList<NewsEntity> resultList = new ArrayList<NewsEntity>();
		
		String content = null;
		
		try {
			content = getUrl( Constants.Url.URL_LATEST );
			ZhihuApplication.getDataSource().insertOrUpdateNewsList(mDate, content);
		} catch (IOException e) {
			e.printStackTrace();
			
			isRefreshSuccess = false;
			mListener.onFail(e);
		}
		
		isContentSame = checkIsContentSame(resultList);

		return content;
	}
}