package com.cundong.izhihu.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.Context;

import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;

public class ZhihuUtils {
	
	/**
	 * 获取listview item 日期
	 * 
	 * @param dateStr
	 * @return
	 */
	public static String getDateTag(Context context, String dateStr) {
		
		String currentDate = DateUtils.getCurrentDate(DateUtils.MMDD);
		
		String pre = DateUtils.getFormatTime(dateStr, DateUtils.YYYYMMDD, DateUtils.MMDD);

		Date date = DateUtils.getFormatTimeDate(dateStr, DateUtils.YYYYMMDD, DateUtils.MMDD);

		String week = DateUtils.getWeekOfDate(date);
		
		return currentDate.equals(pre) ? context.getString(R.string.listview_hotnews) : new StringBuilder().append(pre).append(" ").append(week).toString();
	}
	
	/**
	 * 将 "20141105" 转为 "20141106"
	 * 
	 * @param dateStr
	 * @return
	 */
	public static String getAddedDate(String dateStr) {

		Date date = DateUtils.str2Date(dateStr, DateUtils.YYYYMMDD);

		SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DAY_OF_YEAR, 1);

		return mSimpleDateFormat.format(cal.getTime());
	}
	
	/**
	 * 获取昨天的日期
	 * 
	 * @param dateStr
	 * @return
	 */
	public static String getBeforeDate(String dateStr) {

		Date date = DateUtils.str2Date(dateStr, DateUtils.YYYYMMDD);

		SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DAY_OF_YEAR, -1);

		return mSimpleDateFormat.format(cal.getTime());
	}
	
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