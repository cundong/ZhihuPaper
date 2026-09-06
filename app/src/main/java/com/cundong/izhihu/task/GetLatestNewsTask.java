package com.cundong.izhihu.task;

import java.util.List;

import android.content.Context;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.entity.NewsListEntity;
import com.cundong.izhihu.http.NewsApiClient;
import com.cundong.izhihu.http.NewsApiClient.Payload;
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
	protected NewsListEntity doInBackground(List<String> params) {
		NewsDataSource dataSource = (NewsDataSource) getDataSource();
		Payload<NewsListEntity> networkPayload;

		try {
			String newContent = getUrl(Constants.Url.URL_LATEST);
			networkPayload = NewsApiClient.parseNewsList(newContent, null);
		} catch (Exception e) {
			e.printStackTrace();
			return useLatestGoodCache(dataSource, e);
		}

		String date = networkPayload.entity.date;
		try {
			String oldContent = dataSource.getContent(date);
			isContentSame = checkIsContentSame(oldContent, networkPayload.rawJson);
			if (!isContentSame) {
				dataSource.insertOrUpdateNewsList(Constants.NEWS_LIST,
						date, networkPayload.rawJson);
			}
		} catch (Exception storageException) {
			// A validated network response remains usable even if cache persistence fails.
			storageException.printStackTrace();
		}

		isRefreshSuccess = true;
		mException = null;
		ZhihuUtils.setReadStatus4NewsList(networkPayload.entity.stories);
		return networkPayload.entity;
	}

	private NewsListEntity useLatestGoodCache(NewsDataSource dataSource,
			Exception networkException) {
		for (String cachedContent : dataSource.getRecentNewsListContents(14)) {
			try {
				Payload<NewsListEntity> cached = NewsApiClient.parseNewsList(cachedContent, null);
				isRefreshSuccess = true;
				isContentSame = true;
				mException = null;
				ZhihuUtils.setReadStatus4NewsList(cached.entity.stories);
				return cached.entity;
			} catch (Exception ignoredCorruptRow) {
				// Keep walking backwards until a strict-valid cache row is found.
			}
		}
		isRefreshSuccess = false;
		mException = networkException;
		return null;
	}
}
