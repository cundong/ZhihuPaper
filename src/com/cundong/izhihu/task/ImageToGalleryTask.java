package com.cundong.izhihu.task;

import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * 类说明： 	将图片保存至系统图库Task
 * 
 * @date 	2014-2-7
 * @version 1.0
 */
public class ImageToGalleryTask extends MyAsyncTask<String, Void, String> {

	private Activity mInstance;
	
	public ImageToGalleryTask(Activity instance) {
		mInstance = instance;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		Crouton.makeText(mInstance, "开始保存图片", Style.INFO).show();
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		
		if (!TextUtils.isEmpty(result) && result.equals("success")) {
			Crouton.makeText(mInstance, "图片已保存至相册", Style.INFO).show();
		} else {
			Crouton.makeText(mInstance, "图片保存失败", Style.INFO).show();
		}
	}

	@Override
	protected String doInBackground(String... params) {
		
		if (params.length == 0)
			return null;
		
		boolean result = saveImage2Gallery(mInstance, params[0]);
		
		return result ? "success" : "fail";
	}
	
	/**
	 * 将图片保存至系统图库
	 * 
	 * @param context
	 * @param imagePath
	 * @return
	 */
	private boolean saveImage2Gallery(Context context, String imagePath) {
		
		boolean result = true;
		
	    try {
	    	// 插入到系统图库
	        MediaStore.Images.Media.insertImage(context.getContentResolver(), imagePath, "title", "desc..");
	        
	        // 通知图库更新
		    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + imagePath)));
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	        
	        result = false;
	    }
	    
	    return result;
	} 
}