package com.cundong.izhihu;

import java.io.InputStream;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.cundong.izhihu.http.HttpUtils;

/**
 * 类说明：	Glide 全局配置
 *
 * 取代原先 ZhihuApplication.initImageLoader() 里的 ImageLoaderConfiguration。
 * 两点对齐旧配置：
 *   1. 磁盘缓存维持 50MB（原 diskCacheSize(50 * 1024 * 1024)）；
 *   2. 网络请求复用 HttpUtils 的 OkHttpClient 单例，避免图片加载另起一套
 *      连接池——UIL 当年用的是独立的 HttpURLConnection。
 *
 * 原配置里的 Md5FileNameGenerator、denyCacheImageMultipleSizesInMemory、
 * LIFO 队列在 Glide 中都有对应的内建实现（缓存 key 由 Glide 自行计算，
 * 内存缓存按尺寸区分，请求调度默认后进先出），无需显式声明。
 */
@GlideModule
public class ZhihuGlideModule extends AppGlideModule {

	private static final int DISK_CACHE_SIZE = 50 * 1024 * 1024;

	@Override
	public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
		builder.setDiskCache(new InternalCacheDiskCacheFactory(context, DISK_CACHE_SIZE));
	}

	@Override
	public void registerComponents(@NonNull Context context, @NonNull Glide glide,
			@NonNull Registry registry) {
		registry.replace(GlideUrl.class, InputStream.class,
				new OkHttpUrlLoader.Factory(HttpUtils.getInstance(context)));
	}

	/**
	 * 本工程没有使用 Glide 的清单解析（manifest parsing），关闭它可以省去
	 * 启动时对 AndroidManifest 的一次扫描。
	 */
	@Override
	public boolean isManifestParsingEnabled() {
		return false;
	}
}
