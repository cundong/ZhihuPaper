package com.cundong.izhihu.util;

import android.content.Context;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Reports and clears replaceable offline data without touching user-owned library state. */
public final class OfflineStorageUtils {

	private static final Object OFFLINE_CONTENT_LOCK = new Object();
	private static final AtomicLong CONTENT_GENERATION = new AtomicLong();

	private static volatile ArticleImageCache sArticleImageCache;

	private OfflineStorageUtils() {
	}

	public static long getOfflineBytes(Context context) {
		return sizeOf(ZhihuUtils.getDetailImageCacheDir(context));
	}

	/**
	 * Returns the one process-wide cache coordinator. Its synchronized download
	 * and maintenance methods therefore serialize foreground and WorkManager
	 * publication/eviction decisions against the same 100 MiB budget.
	 */
	public static ArticleImageCache getArticleImageCache(Context context) {
		if (sArticleImageCache == null) {
			synchronized (OfflineStorageUtils.class) {
				if (sArticleImageCache == null) {
					sArticleImageCache = new ArticleImageCache(
							context.getApplicationContext());
				}
			}
		}
		return sArticleImageCache;
	}

	/** Captures the cache generation owned by a foreground download batch. */
	public static long captureContentGeneration() {
		return CONTENT_GENERATION.get();
	}

	/** A clear invalidates older foreground batches before they can publish again. */
	public static boolean isContentGenerationCurrent(long generation) {
		return CONTENT_GENERATION.get() == generation;
	}

	/**
	 * Serializes all durable offline writes with clearOfflineContent(). WorkManager
	 * cancellation is cooperative, so the clear flow also waits on this boundary
	 * before deleting data or announcing success.
	 */
	public static <T> T runSerializedOfflineMutation(OfflineMutation<T> mutation) {
		synchronized (OFFLINE_CONTENT_LOCK) {
			return mutation.run();
		}
	}

	public static String formatBytes(long bytes) {
		if (bytes < 1024L) {
			return bytes + " B";
		}
		double value = bytes / 1024d;
		if (value < 1024d) {
			return String.format(Locale.getDefault(), "%.1f KB", value);
		}
		return String.format(Locale.getDefault(), "%.1f MB", value / 1024d);
	}

	/** Clears article images and replaceable list/detail JSON only. */
	public static boolean clearOfflineContent(final Context context) {
		final Context applicationContext = context.getApplicationContext();
		return runSerializedOfflineMutation(new OfflineMutation<Boolean>() {
			@Override
			public Boolean run() {
				ArticleImageCache imageCache = getArticleImageCache(applicationContext);
				synchronized (imageCache) {
					// Any foreground batch that was created before this point must
					// stop before starting or publishing another image.
					CONTENT_GENERATION.incrementAndGet();
					boolean filesCleared = deleteChildren(
							ZhihuUtils.getDetailImageCacheDir(applicationContext));
					if (ZhihuApplication.getDataSource() != null) {
						ZhihuApplication.getDataSource().deleteContentByType(
								Constants.NEWS_LIST);
						ZhihuApplication.getDataSource().deleteContentByType(
								Constants.NEWS_DETAIL);
					}
					boolean markerCleared = PreferencesUtils.putBoolean(
							applicationContext, DateUtils.getCurrentDate(), false);
					return filesCleared && markerCleared;
				}
			}
		});
	}

	public interface OfflineMutation<T> {
		T run();
	}

	private static long sizeOf(File file) {
		if (file == null || !file.exists()) {
			return 0L;
		}
		if (file.isFile()) {
			return file.length();
		}
		long total = 0L;
		File[] children = file.listFiles();
		if (children != null) {
			for (File child : children) {
				total += sizeOf(child);
			}
		}
		return total;
	}

	private static boolean deleteChildren(File directory) {
		if (directory == null || !directory.exists()) {
			return true;
		}
		boolean success = true;
		File[] children = directory.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isDirectory()) {
					success &= deleteChildren(child);
				}
				if (!child.delete()) {
					success = false;
				}
			}
		}
		return success;
	}
}
