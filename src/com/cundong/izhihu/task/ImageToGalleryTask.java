package com.cundong.izhihu.task;

import java.io.FileNotFoundException;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * 类说明： 	将图片保存至系统图库Task
 * 
 * @date 	2014-2-7
 * @version 1.0
 */
public class ImageToGalleryTask extends BaseGetNewsTask {

	private Context mContext;
	
	public ImageToGalleryTask(ResponseListener listener) {
		super(listener);
	}

	public ImageToGalleryTask(Context context, ResponseListener listener) {
		super(listener);
		
		mContext = context;
	}
	
	@Override
	protected String doInBackground(String... params) {
		
		if (params.length == 0)
			return null;
		
		boolean result = saveImage2Gallery(mContext, params[0]);
		
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