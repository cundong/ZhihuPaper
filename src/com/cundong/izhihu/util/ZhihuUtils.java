package com.cundong.izhihu.util;

import java.util.ArrayList;

import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;

import android.content.Context;

public class ZhihuUtils {

	/**
	 * 根据一个图片的url获取其在本机缓存中的存储url
	 * 
	 * from:
	 * https://avatars3.githubusercontent.com/u/3348483
	 * to:
	 * /storage/emulated/0/Android/data/com.cundong.izhihu/cache/f72a0397abad8edbd98a8d2a701464ee.jpg
	 * 
	 * @param context
	 * @param imageUrl
	 * @return
	 */
	public static String getCacheImgFilePath(Context context, String imageUrl) {

		return SDCardUtils.getExternalCacheDir(context)
				+ MD5Util.encrypt(imageUrl) + ".bin";
	}
	
	/**
	 * 为newsList设置已读、未读标示
	 * 
	 * @param newsList
	 */
	public static void setReadStatus4NewsList(ArrayList<NewsEntity> newsList) {
		if (newsList == null || newsList.isEmpty())
			return;
		
		//已读新闻列表
		ArrayList<String> readList = ZhihuApplication.getNewsReadDataSource().getNewsReadList();
		
		int size = newsList.size();
		for(int i=0; i<size; i++) {
			newsList.get(i).is_read = readList.contains(String.valueOf(newsList.get(i).id));
		}
	}
	
	/**
	 * 为List中的某一个newsEntity设置已读、未读标示
	 * 
	 * @param newsEntity
	 */
	public static void setReadStatus4NewsEntity(ArrayList<NewsEntity> newsList, NewsEntity newsEntity) {
		
		if (newsList == null || newsList.isEmpty() || newsEntity == null)
			return;
		
		int size = newsList.size();
		for(int i=0; i<size; i++) {
			
			if (newsEntity.id == newsList.get(i).id) {
				newsList.get(i).is_read = true;
			}
		}
	}
}