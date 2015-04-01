package com.cundong.izhihu.task;

import java.io.IOException;

import android.content.Context;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.entity.NewsListEntity;
import com.cundong.izhihu.util.GsonUtils;
import com.cundong.izhihu.util.ListUtils;
import com.cundong.izhihu.util.ZhihuUtils;

/**
 * 类说明： 	从服务器下载最新新闻列表，Task
 * 
 * @date 	2014-9-15
 * @version 1.0
 */
public class GetLatestNewsTask extends BaseGetNewsTask {

	public GetLatestNewsTask(Context context, ResponseListener listener) {
		super(context, listener);
	}
	
	@Override
	protected NewsListEntity doInBackground(String... params) {

		String oldContent = null, newContent = null;

		String date = null;

		NewsListEntity newsListEntity = null;
		
		try {
			newContent = getUrl(Constants.Url.URL_LATEST);
			newsListEntity = (NewsListEntity) GsonUtils.getEntity(newContent, NewsListEntity.class);
			
			date = newsListEntity != null ? newsListEntity.date : null;
			
			oldContent = ((NewsDataSource) getDataSource()).getContent(date);
			
			isRefreshSuccess = !ListUtils.isEmpty(newsListEntity.stories);	
		} catch (IOException e) {
			e.printStackTrace();
			
			this.isRefreshSuccess = false;
			this.mException = e;
		} catch (Exception e) {
			e.printStackTrace();

			this.isRefreshSuccess = false;
			this.mException = e;
		}
		
		isContentSame = checkIsContentSame(oldContent, newContent);
		
		if (isRefreshSuccess && !isContentSame) {
			((NewsDataSource) getDataSource()).insertOrUpdateNewsList(Constants.NEWS_LIST, date, newContent);
		}
		
		if (newsListEntity != null) {
			ZhihuUtils.setReadStatus4NewsList(newsListEntity.stories);
		}
		
		return newsListEntity;
	}
}