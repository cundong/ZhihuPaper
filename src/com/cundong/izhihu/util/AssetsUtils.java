package com.cundong.izhihu.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import android.content.Context;
import android.content.res.AssetManager;

/**
 * AssetsUtils
 * 
 * @author cundong 2014-9-5
 */
public class AssetsUtils {

	private static final String DEFAULT_VALUE = "0";

	// 配置信息路径
	private static final String ASSET_PATH = "chart/config.properties";

	private static Properties urlProps = null;

	public static void loadProperties(Context context) {
		Properties props = new Properties();

		AssetManager asset = context.getAssets();

		InputStream in = null;

		try {
			in = asset.open(ASSET_PATH);
			props.load(in);
			urlProps = props;
		}
		catch (IOException e) {
			Logger.getLogger().e("no " + ASSET_PATH + " file, use default 0.");
		}
		finally {
			if (in != null) {
				StreamUtils.close(in);
			}
		}
	}
	
	public static String get(Context context, String key) {

		if (urlProps == null) {
			loadProperties(context);
		}

		return urlProps != null ? urlProps.getProperty(key,
				DEFAULT_VALUE) : DEFAULT_VALUE;
	}
	
	/**
	 * 从Asset目录中读取文本文件，解析为json
	 * 
	 * @param congtext
	 * @param fileUrl
	 * @return
	 */
	public static String loadJSONFromAsset(Context context, String fileUrl){
		String json = null;
	    try {

	        InputStream is = context.getAssets().open(fileUrl);

	        int size = is.available();

	        byte[] buffer = new byte[size];

	        is.read(buffer);
	        
	        is.close();

	        json = new String(buffer, "UTF-8");
	    } catch (IOException ex) {
	        ex.printStackTrace();
	        return null;
	    }
	    
	    return json;
	}
}