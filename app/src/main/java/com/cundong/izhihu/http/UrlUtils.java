package com.cundong.izhihu.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.os.Bundle;
import android.text.TextUtils;

import com.cundong.izhihu.util.MD5Util;
import com.cundong.izhihu.util.StringUtils;

public class UrlUtils {

	/**
	 * 将用户请求的url转化为key值，通过这个key值，可以读取数据库中的缓存数据
	 * 注意：需要清除url中的token，这样token即使变化了，也能读取到缓存
	 * 
	 * @param url
	 * @return
	 */
	public static String getUrlKey(String url) {

		if(TextUtils.isEmpty(url))
			return "";
		
		StringBuilder sb = new StringBuilder();

		String[] array = StringUtils.split(url, "&");
		for (int i = 0; i < array.length; i++) {
			if (!array[i].startsWith("token")) {
				sb.append(array[i]);
			}
		}
		
		return MD5Util.encrypt(sb.toString());
	}
	
	/**
	 * 将Key-value转换成用&号链接的URL查询参数形式。
	 * 
	 * @param parameters
	 * @return
	 */
	public static String encodeUrl(Bundle parameters) {
		if (parameters == null) {
			return "";
		}
		ArrayList<String> list = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String key : parameters.keySet()) {
			list.add(key);
		}
		Collections.sort(list);
		for (String key : list) {
			if (first)
				first = false;
			else
				sb.append("&");
			if (parameters.getString(key) != null) {
				try {
					sb.append(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(parameters.getString(key), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();
	}

	/**
	 * 将用&号链接的URL参数转换成key-value形式。
	 * 
	 * @param s
	 * @return
	 */
	public static Bundle decodeUrl(String s) {
		Bundle params = new Bundle();
		if (s != null) {
			String array[] = s.split("&");
			for (String parameter : array) {
				String[] paramArr = parameter.split("=");
				if(paramArr!=null&&paramArr.length==2){
					try {
						params.putString(URLDecoder.decode(paramArr[0], "UTF-8"), URLDecoder.decode(paramArr[1], "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return params;
	}

	public static Bundle map2Bundle(HashMap<String, String> requestParams) {
		Bundle bundle = new Bundle();
		Iterator<Entry<String, String>> entryKeyIterator = requestParams.entrySet().iterator();
		while (entryKeyIterator.hasNext()) {
			Entry<String, String> entry = entryKeyIterator.next();
			String key = entry.getKey();
			String value = entry.getValue();
			bundle.putString(key, value);
		}

		return bundle;
	}
}