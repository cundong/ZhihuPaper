package com.cundong.izhihu.util;

import java.io.File;

import android.content.Context;
import android.os.Environment;

/**
 * 类说明： SD卡工具类
 * 
 * @date 2012-2-7
 * @version 1.0
 */
public class SDCardUtils {
	
	/**
	 * SD卡是否挂载
	 * 
	 * @return
	 */
	public static boolean isMounted() {
		String status = Environment.getExternalStorageState();
		return status.equals(Environment.MEDIA_MOUNTED) ? true : false;
	}
	
	/**
	 * 获取拓展存储的绝对路径
	 * 
	 * @param context
	 * @return
	 */
	public static String getExternalCacheDir(Context context) {

		StringBuilder sb = new StringBuilder();
		
		if (context == null)
			return null;
		
		if (!isMounted()) {
			sb.append(context.getCacheDir()).append(File.separator);
		}
		
		File file = context.getExternalCacheDir();

		// In some case, even the sd card is mounted,
		// getExternalCacheDir will return null
		// may be it is nearly full.
		
		if (file != null) {
			sb.append(file.getAbsolutePath()).append(File.separator);
		} else {
			sb.append(Environment.getExternalStorageDirectory().getPath()).append("/Android/data/").append(context.getPackageName())
					.append("/cache/").append(File.separator).toString();
		}
		
		return sb.toString();
	}
}