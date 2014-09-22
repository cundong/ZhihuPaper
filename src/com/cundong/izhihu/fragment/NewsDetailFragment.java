package com.cundong.izhihu.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.task.DetailImageDownloadTask;
import com.cundong.izhihu.task.GetNewsDetailTask;
import com.cundong.izhihu.task.MyAsyncTask;
import com.cundong.izhihu.task.ResponseListener;
import com.cundong.izhihu.util.AssetsUtils;
import com.cundong.izhihu.util.GsonUtils;
import com.cundong.izhihu.util.MD5Util;
import com.cundong.izhihu.util.SDCardUtils;
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
		
		// 设置缓存模式
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.getSettings().setBlockNetworkImage(true);

		// 支持通过js打开新的窗口
		mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		mWebView.setWebViewClient( new WebViewClient(){

			
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				
				Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
				startActivity(intent);
				return true;
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				
				String urlStrArray[] = new String[mImageList.size() + 1];  
				mImageList.toArray(urlStrArray);  
				
				new DetailImageDownloadTask(getActivity(), new ResponseListener(){

					@Override
					public void onPreExecute() {
						
					}

					@Override
					public void onPostExecute(String content,
							boolean isRefreshSuccess, boolean isContentSame) {
//						mWebView.loadUrl("javascript:(function(){" +    
//		                        "var objs = document.getElementsByTagName(\"img\"); " +     
//		                                "for(var i=0;i<objs.length;i++)  " +     
//		                        "{"    
//		                                + "    var imgSrc = objs[i].getAttribute(\"src_link\"); " +  
//		                                "    objs[i].setAttribute(\"src\",imgSrc);" +  
//		                        "}" +     
//		                        "})()");  
					}

					@Override
					public void onProgressUpdate(String value) {
						String url = "javascript:(function(){"
								+ "var objs = document.getElementsByTagName(\"img\"); "
								+ "for(var i=0;i<objs.length;i++)  " + "{"
								+ "    var imgSrc = objs[i].getAttribute(\"src_link\"); "
								+ "    var imgOriSrc = objs[i].getAttribute(\"ori_link\"); "
								+ " if(imgOriSrc == \"" + value + "\"){ "
								+ "    objs[i].setAttribute(\"src\",imgSrc);}" + "}" + "})()";

						mLogger.i("url:" + url);
						mWebView.loadUrl(url);
					}

					@Override
					public void onFail(Exception e) {
						 
					}
					
				}).executeOnExecutor(MyAsyncTask.DOWNLOAD_THREAD_POOL_EXECUTOR, urlStrArray);
			}
		});
		
		return rootView;
	}
	
	private void setWebViewShown(boolean shown){
		mWebView.setVisibility(shown ? View.VISIBLE : View.GONE);
		mProgressBar.setVisibility(shown ? View.GONE : View.VISIBLE);
	}
	
	@Override
	protected void doRefresh() {
		new GetNewsDetailTask(this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
	}
	
	@Override
	public void onPreExecute() {
		
	}

	@Override
	public void onPostExecute(String content, boolean isRefreshSuccess,
			boolean isContentSame) {
		if (isAdded()) {
			
			// Notify PullToRefreshLayout that the refresh has finished
			mPullToRefreshLayout.setRefreshComplete();
			
			NewsDetailEntity detailEntity = (NewsDetailEntity)GsonUtils.getEntity(content, NewsDetailEntity.class);
			
			setWebView(detailEntity);
			
		} else {
			
		}
		
		setWebViewShown(true);
	}

	@Override
	public void onFail(final Exception e) {
		setWebViewShown(true);
		
		dealException(e);
	}
	
	private void setWebView( NewsDetailEntity detailEntity ){
		
		if (detailEntity == null || TextUtils.isEmpty(detailEntity.body)) {
			return;
		}
		
		String html = AssetsUtils.loadText(getActivity(), "test.html");
		html = html.replace("$BODY_CONTENT$", detailEntity.body);
		html = html.replace("<div class=\"img-place-holder\">", "<div>");

//		html = html.replace("<div class=\"img-place-holder\">", "<div class=\"img-place-holder\"><img height=\"200px\" width=\"100%\"  src=\"http://pic1.zhimg.com/18907a793f00d554fe846464ff69f373.jpg\"/>");
		
//		if (!TextUtils.isEmpty(detailEntity.image)) {
//			mImageWrapView.setVisibility(View.VISIBLE);
//			mTitleView.setText(detailEntity.title);
//			imageLoader.displayImage(detailEntity.image, mImageView, options,
//					animateFirstListener);
//		} else {
			mImageWrapView.setVisibility(View.GONE);
//		}
		
		
		mWebView.loadDataWithBaseURL(null, replaceImage(html), "text/html", "utf-8", null);
	}
	
	ArrayList<String> mImageList = new ArrayList<String>();  
	
	private String replaceImage(String html) {
		
		String Js2JavaInterfaceName = "JsUseJava";
		
		Document doc = Jsoup.parse(html);

		Elements es = doc.getElementsByTag("img");

		for (Element e : es) {
			String imgUrl = e.attr("src");
			mImageList.add(imgUrl);
			String imgName;
			File file = new File(imgUrl);
			imgName = file.getName();
			if (imgName.endsWith(".gif")) {
				e.remove();
			} else {

				String localImgPath = SDCardUtils
						.getExternalCacheDir(getActivity())
						+ MD5Util.encrypt(imgUrl) + ".jpg";
				
				mLogger.i("localImgPath-->" + localImgPath);

				String filePath = "file:///mnt/sdcard/" + imgName;
				//e.attr("src", "file:///android_asset/ic_launcher.png");
				e.attr("src_link", "file://" + localImgPath);
				e.attr("ori_link", imgUrl);
				String str = "window." + Js2JavaInterfaceName + ".setImgSrc('"
						+ filePath + "')";
				e.attr("onclick", str);
			}
		}

		
		return doc.html();
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
					
					setWebViewShown(true);
					NewsDetailEntity detailEntity = (NewsDetailEntity)GsonUtils.getEntity(result, NewsDetailEntity.class);
					setWebView(detailEntity);
				} else {
					setWebViewShown(false);
				}
			}
		}
	}

	@Override
	public void onProgressUpdate(String value) {
		
	}
}