package com.cundong.izhihu.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.graphics.BitmapFactory;

import java.lang.ref.WeakReference;

/**
 * 类说明： 	将图片保存至系统图库Task
 * 
 * @date 	2014-2-7
 * @version 1.0
 */
public class ImageToGalleryTask extends BackgroundTask<String, Void, Boolean> {

	private final Context applicationContext;
	private final WeakReference<Callback> callbackReference;
	
	public ImageToGalleryTask(Context context, Callback callback) {
		applicationContext = context.getApplicationContext();
		callbackReference = new WeakReference<Callback>(callback);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		Callback callback = callbackReference.get();
		if (callback != null) {
			callback.onSaveStarted();
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		Callback callback = callbackReference.get();
		if (callback != null) {
			callback.onSaveFinished(Boolean.TRUE.equals(result));
		}
	}

	@Override
	protected Boolean doInBackground(List<String> params) {
		
		if (params.isEmpty())
				return false;
			
		return saveImage2Gallery(applicationContext, params.get(0));
	}

	public void clearCallback() {
		callbackReference.clear();
	}
	
	/**
	 * 将图片保存至系统图库
	 * 
	 * @param context
	 * @param imagePath
	 * @return
	 */
	private boolean saveImage2Gallery(Context context, String imagePath) {
		File source = new File(imagePath);
		if (!source.isFile()) {
			return false;
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			return saveImageToLegacyGallery(context, imagePath);
		}
		String mimeType = detectMimeType(source);
		if (mimeType == null) {
			return false;
		}
		String fileName = "zhihupaper-" + System.currentTimeMillis() + extensionFor(mimeType);
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
		values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
		values.put(MediaStore.Images.Media.RELATIVE_PATH,
				Environment.DIRECTORY_PICTURES + "/ZhihuPaper");
		values.put(MediaStore.Images.Media.IS_PENDING, 1);

		Uri destination = context.getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		if (destination == null) {
			return false;
		}
		try (FileInputStream input = new FileInputStream(source);
			 OutputStream output = context.getContentResolver().openOutputStream(destination)) {
			if (output == null) {
				throw new IOException("Unable to open gallery destination");
			}
			byte[] buffer = new byte[16 * 1024];
			for (int read; (read = input.read(buffer)) != -1; ) {
				output.write(buffer, 0, read);
			}
			values.clear();
			values.put(MediaStore.Images.Media.IS_PENDING, 0);
				if (context.getContentResolver().update(destination, values, null, null) <= 0) {
					context.getContentResolver().delete(destination, null, null);
					return false;
				}
				return true;
		} catch (IOException e) {
			context.getContentResolver().delete(destination, null, null);
			return false;
		}
	} 

	@SuppressWarnings("deprecation")
	private boolean saveImageToLegacyGallery(Context context, String imagePath) {
		try {
			return MediaStore.Images.Media.insertImage(context.getContentResolver(), imagePath,
					"ZhihuPaper", "") != null;
		} catch (java.io.FileNotFoundException | SecurityException e) {
			return false;
		}
	}

	private String detectMimeType(File source) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(source.getAbsolutePath(), options);
		return options.outWidth > 0 && options.outHeight > 0 ? options.outMimeType : null;
	}

	private String extensionFor(String mimeType) {
		if ("image/png".equalsIgnoreCase(mimeType)) {
			return ".png";
		}
		if ("image/webp".equalsIgnoreCase(mimeType)) {
			return ".webp";
		}
		if ("image/gif".equalsIgnoreCase(mimeType)) {
			return ".gif";
		}
		return ".jpg";
	}

	public interface Callback {
		void onSaveStarted();
		void onSaveFinished(boolean success);
	}
}
