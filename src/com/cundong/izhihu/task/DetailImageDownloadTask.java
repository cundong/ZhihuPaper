package com.cundong.izhihu.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.text.TextUtils;

import com.cundong.izhihu.http.HttpClientUtils;
import com.cundong.izhihu.util.Logger;
import com.cundong.izhihu.util.SDCardUtils;
import com.cundong.izhihu.util.StreamUtils;
import com.cundong.izhihu.util.ZhihuUtils;

public class DetailImageDownloadTask extends BaseGetNewsTask {

	private Context mContext;
	private String mExternalCacheDir = null;
	
	public DetailImageDownloadTask(ResponseListener listener) {
		super(listener);
	}

	public DetailImageDownloadTask(Context context, ResponseListener listener) {
		super(listener);
		mContext = context;
		
		mExternalCacheDir = SDCardUtils.getExternalCacheDir(mContext);
	}

	@Override
	protected String doInBackground(String... params) {

		if ( params.length == 0 || TextUtils.isEmpty(mExternalCacheDir) )
			return null;
	
		File file = null;
		for (String param : params) {
			
			if (TextUtils.isEmpty(param)) {
				Logger.getLogger().e("NO download, the image url is null");
				continue;
			}
			
			File folder = new File(mExternalCacheDir);
			if (!folder.exists()) {
				folder.mkdirs();
			}

			String filePath = ZhihuUtils.getCacheImgFilePath(mContext, param);
			file = new File(filePath);
			
			if( !file.exists() || file.length() == 0 ) {

				try {
					
					file.getParentFile().mkdirs();
					file.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				InputStream in = null;
				OutputStream out = null;

				// from web
				try {
					in = HttpClientUtils.request(mContext, param, null);
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
				Logger.getLogger().i("no download, image is exist:" + file.getAbsolutePath());
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