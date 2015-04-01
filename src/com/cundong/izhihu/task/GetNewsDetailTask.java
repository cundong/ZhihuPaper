package com.cundong.izhihu.task;

import java.io.IOException;

import android.content.Context;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.util.GsonUtils;

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
	protected String doInBackground(String... params) {

		String content = null;

		try {
			content = getUrl(Constants.Url.URL_DETAIL + params[0]);
			
			NewsDetailEntity newsDetailEntity = (NewsDetailEntity) GsonUtils.getEntity(content, NewsDetailEntity.class);
			
			isRefreshSuccess = newsDetailEntity != null;
		} catch (IOException e) {
			e.printStackTrace();

			isRefreshSuccess = false;
			this.mException = e;
		} catch (Exception e) {
			e.printStackTrace();
			
			isRefreshSuccess = false;
			this.mException = e;
		}

		isContentSame = checkIsContentSame(params[0], content);
		
		if (isRefreshSuccess && !isContentSame) {
			((NewsDataSource) getDataSource()).insertOrUpdateNewsList(Constants.NEWS_DETAIL, 
					"detail_" + params[0], content);
		}
		
		return content;
	}
}