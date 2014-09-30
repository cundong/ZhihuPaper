package com.cundong.izhihu;

public final class Constants {
	
	public static final class Url {

		// 获取最新新闻
		public static final String URL_LATEST = "http://news-at.zhihu.com/api/3/news/latest";

		// 获取新闻详情
		public static final String URL_DETAIL = "http://news-at.zhihu.com/api/3/news/";

		// 获取过往新闻
		public static final String URLDEFORE = "http://news.at.zhihu.com/api/3/news/before/";
	}

	// 默认模板路径
	public static final String TEMPLATE_DEF_URL = "www/template.html";

	//
	public static final String GITGUB_PROJECT = "https://github.com/cundong/ZhihuPaper";

	//
	public static final String GITHUB_NAME = "https://github.com/cundong";
	
	//startActivity requestCode
	public static final int REQUESTCODE_SETTING = 8009;
}