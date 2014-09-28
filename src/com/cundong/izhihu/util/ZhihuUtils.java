package com.cundong.izhihu.util;

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
}