package com.cundong.izhihu.fragment;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.task.GetNewsDetailTask;
import com.cundong.izhihu.task.MyAsyncTask;
import com.cundong.izhihu.task.ResponseListener;
import com.cundong.izhihu.util.AssetsUtils;
import com.cundong.izhihu.util.GsonUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

@SuppressLint("SetJavaScriptEnabled")
public class NewsDetailFragment extends BaseFragment implements ResponseListener {
	
	private ViewGroup mImageWrapView;
	private ImageView mImageView;
	private TextView mTitleView;
	
	private ProgressBar mProgressBar;
	private WebView mWebView;
	
	private long mNewsId = 0;
	
	private ImageLoader imageLoader = ImageLoader.getInstance();
    
    private DisplayImageOptions options = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.news_detail_title_def)
            .showImageOnFail(R.drawable.news_detail_title_def)
            .showImageForEmptyUri(R.drawable.news_detail_title_def)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .build();
    
    private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle bundle = getArguments();
		mNewsId = bundle != null ? bundle.getLong("id") : 0;
		
		new LoadCacheDetailTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
		new GetNewsDetailTask(this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

		mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress);
		
		mPullToRefreshLayout = (PullToRefreshLayout) rootView.findViewById(R.id.ptr_layout);
		
		mTitleView = (TextView) rootView.findViewById(R.id.news_title);
		mImageWrapView = (ViewGroup) rootView.findViewById(R.id.detail_image_wrap);
		
		mImageView = (ImageView) rootView.findViewById(R.id.detail_image);
		mWebView = (WebView) rootView.findViewById(R.id.webview);
		
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);// 设置缓存模式
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.getSettings().setJavaScriptEnabled(true);

		// 支持通过js打开新的窗口
		mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		
		return rootView;
	}
	
	@Override
	protected void doRefresh() {
		new GetNewsDetailTask(this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
	}
	
	@Override
	public void onPre() {
		
	}

	@Override
	public void onComplete(String content, boolean isRefreshSuccess,
			boolean isContentSame) {
		if (isAdded()) {
			
			// Notify PullToRefreshLayout that the refresh has finished
			mPullToRefreshLayout.setRefreshComplete();
			
			NewsDetailEntity detailEntity = (NewsDetailEntity)GsonUtils.getEntity(content, NewsDetailEntity.class);
			
			setWebView(detailEntity);
			
		} else {
			
		}
	}

	@Override
	public void onFail(final Exception e) {
		
	}
	
	private void setWebView( NewsDetailEntity detailEntity ){
		
		if (detailEntity == null || TextUtils.isEmpty(detailEntity.body)) {
			return;
		}
		
		String html = AssetsUtils.loadText(getActivity(), "test.html");
		html = html.replace("$BODY_CONTENT$", detailEntity.body);
		html = html.replace("<div class=\"img-place-holder\">", "<div>");

		if (!TextUtils.isEmpty(detailEntity.image)) {
			mImageWrapView.setVisibility(View.VISIBLE);
			mTitleView.setText(detailEntity.title);
			imageLoader.displayImage(detailEntity.image, mImageView, options,
					animateFirstListener);
		} else {
			mImageWrapView.setVisibility(View.GONE);
		}
		
		mWebView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
	}
	
	private static class AnimateFirstDisplayListener extends
		SimpleImageLoadingListener {
		
		static final List<String> displayedImages = Collections
				.synchronizedList(new LinkedList<String>());
		
		@Override
		public void onLoadingComplete(String imageUri, View view,
				Bitmap loadedImage) {
			if (loadedImage != null) {
				ImageView imageView = (ImageView) view;
				boolean firstDisplay = !displayedImages.contains(imageUri);
				if (firstDisplay) {
					FadeInBitmapDisplayer.animate(imageView, 500);
					displayedImages.add(imageUri);
				}
			}
		}
	}
	
	private class LoadCacheDetailTask extends MyAsyncTask<String, Void, String> {
		
		@Override
		protected String doInBackground(String... params) {
			String key = "detail_" + params[0];
			return ZhihuApplication.getDataSource().getContent(key);
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			
			if (isAdded()) {
				if (!TextUtils.isEmpty(result)) {
					NewsDetailEntity detailEntity = (NewsDetailEntity)GsonUtils.getEntity(result, NewsDetailEntity.class);
					setWebView(detailEntity);
				} 
			}
		}
	}
}