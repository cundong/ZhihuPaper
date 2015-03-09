package com.cundong.izhihu.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import com.cundong.izhihu.Constants.Url;
import com.cundong.izhihu.R;
import com.cundong.izhihu.http.HttpClientUtils;
import com.cundong.izhihu.task.MyAsyncTask;
import com.cundong.izhihu.util.SDCardUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

/**
 * 启动页面
 * 
 * @author gonglei
 * 
 */
public class SplashActivity extends Activity {

	private static final String TAG = "SplashActivity";
	private Context context;
	private Handler mHandler = new Handler();
	private String mImageUrl;
	private View view;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		view = View.inflate(this, R.layout.activity_splash, null);
		setContentView(view);

		context = this;
		mImageUrl = SDCardUtils.getExternalCacheDir(context) + File.separator
				+ "launcher_image";
		initData();

		Animation animation = AnimationUtils.loadAnimation(this, R.anim.alpha);
		view.startAnimation(animation);

		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				mHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						goHome();
					}
				}, 500);
			}
		});
	}

	/**
	 * 获取图片更新View
	 */
	@SuppressLint("NewApi")
	private void initData() {
		Bitmap bitmap = getImageFromLocal(mImageUrl);
		if (bitmap != null) {
			Drawable drawable = new BitmapDrawable(bitmap);
			view.setBackground(drawable);
			Log.d(TAG, "local image");
		} else {
			view.setBackground(getResources().getDrawable(
					R.drawable.default_splash));
			Log.d(TAG, "default");
		}
	}

	/**
	 * 获取手机分辨率拼接URL
	 * 
	 * @return
	 */
	private String initUrl() {
		String splashUrl = null;
		int width = getResources().getDisplayMetrics().widthPixels;
		if (width == 480) {
			splashUrl = Url.URL_LAUNCHER + "480*728";
		} else if (width == 720) {
			splashUrl = Url.URL_LAUNCHER + "720*1184";
		} else if (width == 1080) {
			splashUrl = Url.URL_LAUNCHER + "1080*1776";
		}
		Log.d(TAG, "width = " + width + ", url = " + splashUrl);
		return splashUrl;
	}

	/**
	 * 启动图片加载，跳转至主页
	 */
	private void goHome() {
		new LoadImageTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR,
				initUrl());
		Intent intent = new Intent();
		intent.setClass(this, MainActivity.class);
		startActivity(intent);
		finish();
	}

	/**
	 * 图片下载的Task
	 * 
	 * @author gonglei
	 * 
	 */
	private class LoadImageTask extends MyAsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			String result = null;
			if (params[0] != null) {
				try {
					result = HttpClientUtils.get(SplashActivity.this,
							params[0], null);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			Log.d(TAG, "result = " + result);
			if (result == null) {
				return;
			}
			JSONObject object;
			String imageUrl = null;
			try {
				object = new JSONObject(result);
				imageUrl = object.getString("img");
				ImageLoader loader = ImageLoader.getInstance();
				loader.loadImage(imageUrl, new SimpleImageLoadingListener() {

					@Override
					public void onLoadingComplete(String imageUri, View view,
							Bitmap loadedImage) {
						// TODO Auto-generated method stub
						Log.d(TAG, "load complete");
						saveImage(mImageUrl, loadedImage);
						super.onLoadingComplete(imageUri, view, loadedImage);
					}

					@Override
					public void onLoadingFailed(String imageUri, View view,
							FailReason failReason) {
						// TODO Auto-generated method stub
						Log.d(TAG, "load failed");
						super.onLoadingFailed(imageUri, view, failReason);
					}

				});
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			super.onPostExecute(result);
		}

	}

	/**
	 * 保存图片到本地
	 * 
	 * @param imagePath
	 * @param bm
	 */
	private void saveImage(String imagePath, Bitmap bm) {

		if (bm == null || imagePath == null || "".equals(imagePath)) {
			return;
		}

		File f = new File(imagePath);
		if (f.exists()) {
			f.delete();
		}
		try {
			File parentFile = f.getParentFile();
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
			f.createNewFile();
			FileOutputStream fos;
			fos = new FileOutputStream(f);
			bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.close();
		} catch (FileNotFoundException e) {
			f.delete();
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			f.delete();
		}

	}

	/**
	 * 从本地获取图片
	 * 
	 * @param imagePath
	 * @return
	 */
	private Bitmap getImageFromLocal(String imagePath) {
		File file = new File(imagePath);
		if (file.exists()) {
			Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
			file.setLastModified(System.currentTimeMillis());
			return bitmap;
		}
		return null;
	}

}
