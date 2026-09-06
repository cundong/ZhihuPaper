package com.cundong.izhihu.task;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import android.content.Context;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.db.NewsFavoriteDataSource;
import com.cundong.izhihu.entity.LibraryItemEntity;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.http.NewsApiClient;
import com.cundong.izhihu.http.NewsApiClient.Payload;
import com.google.gson.JsonObject;

/**
 * 类说明： 	下载新闻详情页内容，Task
 * 
 * @date 	2014-9-7
 * @version 1.0
 */
public class GetNewsDetailTask extends BaseGetContentTask {

	public GetNewsDetailTask(Context context, ResponseListener listener) {
		super(context, listener);
	}

	@Override
	protected String doInBackground(List<String> params) {

		if (params == null || params.isEmpty()) {
			isRefreshSuccess = false;
			mException = new IllegalArgumentException("Missing news id");
			return null;
		}

		final long expectedId;
		try {
			expectedId = Long.parseLong(params.get(0));
			if (expectedId <= 0) {
				throw new NumberFormatException("News id must be positive");
			}
		} catch (Exception e) {
			isRefreshSuccess = false;
			mException = new IllegalArgumentException("Invalid news id", e);
			return null;
		}

		NewsDataSource dataSource = (NewsDataSource) getDataSource();
		String cacheKey = "detail_" + expectedId;
		String oldContent = null;
		try {
			oldContent = dataSource.getContent(cacheKey);
		} catch (Exception cacheReadException) {
			cacheReadException.printStackTrace();
		}
		Payload<NewsDetailEntity> networkPayload;
		try {
			String content = getUrl(Constants.Url.URL_DETAIL + expectedId);
			networkPayload = NewsApiClient.parseNewsDetail(content, expectedId);
		} catch (Exception e) {
			e.printStackTrace();
			return useGoodCache(oldContent, expectedId, e);
		}

		try {
			isContentSame = checkIsContentSame(oldContent, networkPayload.rawJson);
			if (!isContentSame) {
				dataSource.insertOrUpdateNewsList(Constants.NEWS_DETAIL,
						cacheKey, networkPayload.rawJson);
			}
		} catch (Exception storageException) {
			// A validated network response remains usable even if cache persistence fails.
			storageException.printStackTrace();
		}

		isRefreshSuccess = true;
		mException = null;
		return networkPayload.rawJson;
	}

	private String useGoodCache(String cachedContent, long expectedId,
			Exception networkException) {
		try {
			Payload<NewsDetailEntity> cached = NewsApiClient.parseNewsDetail(
					cachedContent, expectedId);
			isRefreshSuccess = true;
			isContentSame = true;
			mException = null;
			return cached.rawJson;
		} catch (Exception cacheException) {
			try {
				LibraryItemEntity libraryItem = getLibraryDataSource()
						.getLibraryItem(String.valueOf(expectedId));
				Payload<NewsDetailEntity> libraryPayload = buildLibraryPayload(
						libraryItem, expectedId);
				isRefreshSuccess = true;
				isContentSame = false;
				mException = null;
				return libraryPayload.rawJson;
			} catch (Exception libraryException) {
				isRefreshSuccess = false;
				mException = networkException != null ? networkException : cacheException;
				return null;
			}
		}
	}

	/**
	 * Rebuilds the existing detail callback contract from the library's raw article HTML.
	 * The synthesized JSON is passed through the same strict validator used for network/cache data,
	 * so a blank body/title, mismatched ID or malformed optional URL cannot enter the reader.
	 */
	static Payload<NewsDetailEntity> buildLibraryPayload(LibraryItemEntity item, long expectedId)
			throws NewsApiClient.InvalidPayloadException {
		JsonObject json = new JsonObject();
		if (item != null && item.newsId != null) {
			try {
				if (Long.parseLong(item.newsId) == expectedId) {
					json.addProperty("id", expectedId);
					json.addProperty("title", item.title);
					json.addProperty("body", item.body);
					addOptionalWebUrl(json, "image", item.logo);
					addOptionalWebUrl(json, "share_url", item.shareUrl);
				}
			} catch (NumberFormatException ignored) {
				// The strict parser below reports one consistent invalid-payload failure.
			}
		}
		return NewsApiClient.parseNewsDetail(json.toString(), expectedId);
	}

	private static void addOptionalWebUrl(JsonObject json, String name, String value) {
		if (value == null || value.trim().length() == 0) {
			return;
		}
		try {
			URI uri = new URI(value.trim());
			String scheme = uri.getScheme();
			if (("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
					&& uri.getHost() != null && uri.getHost().length() > 0
					&& uri.getUserInfo() == null) {
				json.addProperty(name, value.trim());
			}
		} catch (URISyntaxException ignored) {
			// Optional stale metadata must not make an otherwise valid offline body unusable.
		}
	}

	protected NewsFavoriteDataSource getLibraryDataSource() {
		return ZhihuApplication.getNewsFavoriteDataSource();
	}
}
