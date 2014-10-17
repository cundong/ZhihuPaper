package com.cundong.izhihu.task;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.http.HttpClientUtils;
import com.cundong.izhihu.util.GsonUtils;
import com.cundong.izhihu.util.ListUtils;
import com.cundong.izhihu.util.ZhihuUtils;

/**
 * 类说明： 	从服务器下载新闻列表，Task
 * 
 * @date 	2014-9-15
 * @version 1.0
 */
public class GetNewsTask extends BaseGetNewsListTask {

	public GetNewsTask(Context context, ResponseListener listener) {
		super(context, listener);
	}

	@Override
	protected ArrayList<NewsEntity> doInBackground(String... params) {

		if (params.length == 0)
			return null;
		
		String theKey = params[0];
		String oldContent = ZhihuApplication.getDataSource().getContent(theKey);

		String newContent = null;
		ArrayList<NewsEntity> newsList = null;
		
		try {
			newContent = getUrl(Constants.Url.URL_LATEST);
			newsList = GsonUtils.getNewsList(newContent);
			
			isRefreshSuccess = !ListUtils.isEmpty(newsList);
			
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
		
		if (isRefreshSuccess && !isContentSame) {
			ZhihuApplication.getDataSource().insertOrUpdateNewsList(theKey, newContent);
		}
		
		ZhihuUtils.setReadStatus4NewsList(newsList);
    	
		return newsList;
	}
	
	protected String getUrl(String url) throws IOException, Exception {
		return HttpClientUtils.get(mContext, url, null);
	}
}