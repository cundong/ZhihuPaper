package com.cundong.izhihu.util;

import java.util.ArrayList;

import com.cundong.izhihu.entity.NewsListEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.google.gson.Gson;

public class GsonUtils {

	public static ArrayList<NewsEntity> getNewsList(String content) {
		
		Gson gson = new Gson();

		try {
			NewsListEntity newsListEntity = gson.fromJson(content, NewsListEntity.class);
			return newsListEntity != null ? newsListEntity.stories : null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
