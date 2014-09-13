package com.cundong.izhihu.task;

import java.io.IOException;
import java.util.ArrayList;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsListEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.util.GsonUtils;

public class OfflineDownloadTask extends BaseGetNewsTask {

	public OfflineDownloadTask(ResponseListener listener) {
		super(listener);
	}
	
	@Override
	protected String doInBackground(String... params) {

		String content = null;

		try {
			content = getUrl(Constants.Url.URL_LATEST);
			
			NewsListEntity newsListEntity = (NewsListEntity)GsonUtils.getEntity(content, NewsListEntity.class);
			ArrayList<NewsEntity> stories = newsListEntity.stories;
			
			if (stories != null && stories.size() != 0) {

				for (NewsEntity newsEntity : stories) {
					String detailContent = getUrl(Constants.Url.URL_DETAIL + newsEntity.id);
					
					//TODO 提取 detailContent 中所有的img结点
					ZhihuApplication.getDataSource().insertOrUpdateNewsList("detail_" + newsEntity.id, detailContent);
				}
				
				return "success";
			}
		} catch (IOException e) {
			e.printStackTrace();

			isRefreshSuccess = false;
			mListener.onFail(e);
		}

		return null;
	}
}