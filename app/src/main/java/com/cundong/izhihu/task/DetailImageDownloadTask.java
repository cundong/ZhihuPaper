package com.cundong.izhihu.task;

import android.content.Context;
import android.text.TextUtils;

import com.cundong.izhihu.util.ArticleImageCache;
import com.cundong.izhihu.util.Logger;
import com.cundong.izhihu.util.OfflineStorageUtils;

import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Downloads article images through the same bounded cache used by offline sync.
 * The task owns only an application Context and a weak UI callback; the Fragment
 * remains the lifecycle owner and cancels this task when its view is destroyed.
 */
public final class DetailImageDownloadTask
		extends BackgroundTask<String, String, String> {

	private static final String APP_ASSET_PREFIX =
			"https://appassets.androidplatform.net/";

	private final ArticleImageCache imageCache;
	private final WeakReference<ResponseListener> listenerReference;
	private final long contentGeneration;

	public DetailImageDownloadTask(Context context, ResponseListener listener) {
		Context applicationContext = context.getApplicationContext();
		imageCache = OfflineStorageUtils.getArticleImageCache(applicationContext);
		listenerReference = new WeakReference<ResponseListener>(listener);
		contentGeneration = OfflineStorageUtils.captureContentGeneration();
	}

	@Override
	protected void onPreExecute() {
		ResponseListener listener = listenerReference.get();
		if (listener != null) {
			listener.onPreExecute();
		}
	}

	@Override
	protected String doInBackground(List<String> params) {
		if (params.isEmpty()) {
			return null;
		}

		imageCache.maintain();
		for (String imageUrl : params) {
			if (shouldStop()) {
				break;
			}
			if (TextUtils.isEmpty(imageUrl)) {
				Logger.getLogger().e("no download, the image url is empty");
				continue;
			}
			// The built-in fallback already lives under WebViewAssetLoader and
			// must never be sent through the network image cache.
			if (imageUrl.startsWith(APP_ASSET_PREFIX)) {
				continue;
			}

			try {
				imageCache.download(imageUrl,
						new ArticleImageCache.CancellationChecker() {
							@Override
							public boolean isStopped() {
								return shouldStop();
							}
						});
				if (!shouldStop()) {
					publishProgress(imageUrl);
				}
			} catch (InterruptedIOException e) {
				if (shouldStop()) {
					break;
				}
				Logger.getLogger().error(e);
			} catch (Exception e) {
				Logger.getLogger().error(e);
			}
		}

		if (!shouldStop()) {
			imageCache.maintain();
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(String value) {
		ResponseListener listener = listenerReference.get();
		if (listener != null) {
			listener.onProgressUpdate(value);
		}
	}

	@Override
	protected void onPostExecute(String content) {
		ResponseListener listener = listenerReference.get();
		if (listener != null) {
			listener.onPostExecute(content);
		}
	}

	/** Detaches the lifecycle callback before cancellation/teardown. */
	public void clearListener() {
		listenerReference.clear();
	}

	private boolean shouldStop() {
		return isCancelled()
				|| Thread.currentThread().isInterrupted()
				|| !OfflineStorageUtils.isContentGenerationCurrent(contentGeneration);
	}
}
