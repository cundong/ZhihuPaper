package com.cundong.izhihu.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import android.content.Context;

/**
 * AssetsUtils
 * 
 * @author cundong 2014-9-5
 */
public class AssetsUtils {

	private static final String ENCODING = "utf-8";

	public static String loadText(Context context, String assetFielPath){
		InputStream is = null;
		try {
			is = context.getResources().getAssets().open(assetFielPath);
			String textfile = convertStreamToString(is);
		    return textfile;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		return null;
	}
	
	public static String convertStreamToString(InputStream is)
			throws IOException {
		Writer writer = new StringWriter();

		char[] buffer = new char[2048];
		try {
			Reader reader = new BufferedReader(new InputStreamReader(is,
					ENCODING));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
		} finally {
			is.close();
		}
		String text = writer.toString();
		return text;
	}
}