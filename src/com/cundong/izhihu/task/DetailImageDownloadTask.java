package com.cundong.izhihu.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.text.TextUtils;

import com.cundong.izhihu.http.HttpClientUtils;
import com.cundong.izhihu.util.FileUtils;
import com.cundong.izhihu.util.Logger;
import com.cundong.izhihu.util.SDCardUtils;
import com.cundong.izhihu.util.StreamUtils;
import com.cundong.izhihu.util.ZhihuUtils;

/**
 * 类说明： 	新闻详情页，WebView中图片列表下载Task
 * 
 * @date 	2014-9-15
 * @version 1.0
 */
public class DetailImageDownloadTask extends BaseGetNewsTask {

	public DetailImageDownloadTask(Context context, ResponseListener listener) {
		super(context, listener);
	}

	@Override
	protected String doInBackground(String... params) {

		String externalCacheDir = SDCardUtils.getExternalCacheDir(mContext);
		
		if ( params.length == 0 || TextUtils.isEmpty(externalCacheDir) )
			return null;
	
		File file = null;
		for (String param : params) {
			
			if (TextUtils.isEmpty(param)) {
				Logger.getLogger().e("no download, the image url is empty");
				continue;
			}

			String filePath = ZhihuUtils.getCacheImgFilePath(mContext, param);
			file = new File(filePath);
			
			boolean needDownload = true;
			
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				long fileSize = FileUtils.getFileSize(filePath);
				
				if (fileSize == 0) {
					// need re download
				} else {
					needDownload = false;
				}
			}
			
			if (needDownload) {
				InputStream in = null;
				OutputStream out = null;
				
				// from web
				try {
					in = HttpClientUtils.getStream(mContext, param, null);
					out = new FileOutputStream(file);

					StreamUtils.copy(in, out);
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					StreamUtils.close(out);
					StreamUtils.close(in);
				}
			} else {
				// no need download
			}
			
			publishProgress(param);
		}

		return null;
	}

	@Override
	protected void onPostExecute(String content) {
		super.onPostExecute(content);

		mListener.onPostExecute(content, true, false);

	}

	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);

		mListener.onProgressUpdate(values[0]);
	}
}