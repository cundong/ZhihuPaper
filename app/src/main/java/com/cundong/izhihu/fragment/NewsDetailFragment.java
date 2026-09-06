package com.cundong.izhihu.fragment;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.webkit.WebViewAssetLoader;
import androidx.core.content.ContextCompat;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.activity.NewsDetailImageActivity;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.task.DetailImageDownloadTask;
import com.cundong.izhihu.task.GetNewsDetailTask;
import com.cundong.izhihu.task.BackgroundTask;
import com.cundong.izhihu.task.ResponseListener;
import com.cundong.izhihu.util.AssetsUtils;
import com.cundong.izhihu.util.ArticleHtmlRenderer;
import com.cundong.izhihu.util.GsonUtils;
import com.cundong.izhihu.util.NetWorkHelper;
import com.cundong.izhihu.util.NewsUrlUtils;
import com.cundong.izhihu.util.ReaderProgress;
import com.cundong.izhihu.util.ZhihuUtils;
import com.cundong.izhihu.http.NewsApiClient;

/**
 * 类说明： 	新闻详情页Fragment
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
public class NewsDetailFragment extends BaseFragment implements
		ResponseListener {
	
	private ProgressBar mProgressBar;
	private WebView mWebView;
	private View mErrorContainer;
	private WebViewAssetLoader mAssetLoader;
	private static final String APP_ASSET_HOST = "appassets.androidplatform.net";
	private static final String APP_ASSET_BASE_URL = "https://" + APP_ASSET_HOST + "/assets/www/";

	private long mNewsId = 0;
	private NewsDetailEntity mNewsDetailEntity = null;
	private ArrayList<String> mDetailImageList = new ArrayList<String>();
	private ArrayList<ArticleHtmlRenderer.Section> mSections =
			new ArrayList<ArticleHtmlRenderer.Section>();
	private boolean mHasContent;
	private boolean mProgressRestored;
	private int mPendingProgress;
	private DetailImageDownloadTask mImageDownloadTask;
	private LoadCacheDetailTask mCacheDetailTask;
	private GetNewsDetailTask mDetailTask;
	// Keeps the weak task callback alive exactly as long as this view lifecycle.
	private ResponseListener mImageDownloadListener;
	
	private OnContentLoadListener mListener = null;
		
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		try {
			mListener = (OnContentLoadListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString()
					+ " must implement OnContentLoadListener");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
		
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress);
		mPullToRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.ptr_layout);
		mWebView = (WebView) rootView.findViewById(R.id.webview);
		mErrorContainer = rootView.findViewById(R.id.detail_error_container);
		rootView.findViewById(R.id.detail_retry).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View retryView) {
				doRefresh();
			}
		});
		java.io.File imageCacheDir = ZhihuUtils.getDetailImageCacheDir(getActivity());
		mAssetLoader = new WebViewAssetLoader.Builder()
				.addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(getActivity()))
				.addPathHandler("/cache/", new WebViewAssetLoader.InternalStoragePathHandler(
						getActivity(), imageCacheDir))
				.build();

		setUpWebViewDefaults(mWebView);
		mPendingProgress = loadReaderProgress();

		mWebView.setWebViewClient(mWebViewClient);
		startCacheDetailLoad();
		startDetailLoad();
		
		return rootView;
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	private void setUpWebViewDefaults(WebView webView) {
		
		// 设置缓存模式
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setDomStorageEnabled(true);
		mWebView.getSettings().setDefaultTextEncodingName("utf-8");
		mWebView.getSettings().setLoadsImagesAutomatically(true);

		// Local assets and cached images are served from a dedicated HTTPS origin.
		// Remote article HTML must not receive file/content access or a Java bridge.
		mWebView.getSettings().setAllowFileAccess(false);
		mWebView.getSettings().setAllowContentAccess(false);
		mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

		// Use WideViewport and Zoom out if there is no viewport defined
		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.getSettings().setLoadWithOverviewMode(false);
		mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
		mWebView.getSettings().setSupportZoom(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.getSettings().setDisplayZoomControls(false);
		mWebView.getSettings().setTextZoom(ArticleHtmlRenderer.textZoomForFontScale(
				getResources().getConfiguration().fontScale));
				
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		
		mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
		
		mWebView.setWebChromeClient(new WebChromeClient() {
			 
		    @Override
		    public boolean onConsoleMessage(android.webkit.ConsoleMessage cm) {
		        if (com.cundong.izhihu.BuildConfig.DEBUG) {
		            mLogger.d("WebView console [" + cm.messageLevel() + "] "
		                    + cm.message() + " @" + cm.lineNumber());
		        }
		        return true;
		    }
		});
		
		//设置webView背景
		Resources.Theme theme = getActivity().getTheme();
		TypedArray typedArray = null;
		
		SharedPreferences mPerferences = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		
		if (mPerferences.getBoolean("dark_theme?", false)) {
			typedArray = theme.obtainStyledAttributes(R.style.Theme_Daily_AppTheme_Dark, 
					new int[] { R.attr.webViewBackground });
		} else {
			typedArray = theme.obtainStyledAttributes(R.style.Theme_Daily_AppTheme_Light, 
					new int[] { R.attr.webViewBackground }); 
		}
		
		mWebView.setBackgroundColor(ContextCompat.getColor(requireContext(),
				typedArray.getResourceId(0, 0)));
		typedArray.recycle();
		
		mWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
	}
	
	private void setWebViewShown(boolean shown) {
		mWebView.setVisibility(shown ? View.VISIBLE : View.GONE);
		mProgressBar.setVisibility(shown ? View.GONE : View.VISIBLE);
		mErrorContainer.setVisibility(View.GONE);
	}

	private void showDetailError() {
		finishRefresh();
		mProgressBar.setVisibility(View.GONE);
		mWebView.setVisibility(View.GONE);
		mErrorContainer.setVisibility(View.VISIBLE);
	}

	@Override
	protected void doRefresh() {
		if (isAdded()) {
			startDetailLoad();
		}
	}

	private void startCacheDetailLoad() {
		cancelViewTask(mCacheDetailTask);
		mCacheDetailTask = trackViewTask(new LoadCacheDetailTask());
		mCacheDetailTask.executeOnExecutor(
				BackgroundTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
	}

	private void startDetailLoad() {
		if (mDetailTask != null) {
			mDetailTask.clearListener();
			cancelViewTask(mDetailTask);
		}
		mDetailTask = trackViewTask(new GetNewsDetailTask(requireContext(), this));
		mDetailTask.executeOnExecutor(
				BackgroundTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
	}

	@Override
	public void onPreExecute() {
		if (isAdded() && !mHasContent && mProgressBar != null) {
			mProgressBar.setVisibility(View.VISIBLE);
			mWebView.setVisibility(View.GONE);
			mErrorContainer.setVisibility(View.GONE);
		}
	}

	@Override
	public void onPostExecute(String content) {
		if (isAdded()) {
			
			// Notify PullToRefreshLayout that the refresh has finished
			finishRefresh();

			if (!TextUtils.isEmpty(content)) {
				setWebView(content, true);
			}
			if (!mHasContent) {
				showDetailError();
			}
		}
	}

	@Override
	public void onFail(final Exception e) {
		if (!isAdded() || mWebView == null) {
			return;
		}
		if (!mHasContent) {
			showDetailError();
		} else {
			setWebViewShown(true);
		}

		dealException(e);
	}

	/**
	 * 设置WebView内容
	 * 
	 * @param content
	 * @param isUpdateMode 是否为刷新操作
	 */
	private void setWebView(String content, boolean isUpdateMode) {

		if (!isAdded()) {
			return;
		}
		
		if (isUpdateMode) {
			if (TextUtils.isEmpty(content)) {
				return;
			}
		}
		
		mNewsDetailEntity = (NewsDetailEntity) GsonUtils.getEntity(
				content, NewsDetailEntity.class);
		
		if (mNewsDetailEntity == null || TextUtils.isEmpty(mNewsDetailEntity.body)) {
			return;
		}

		mDetailImageList.clear();
		
		//tell the activity, mNewsDetailEntity is okey
		mListener.onComplete(mNewsDetailEntity);
		
		ArticleHtmlRenderer.RenderedArticle renderedArticle =
				ArticleHtmlRenderer.render(mNewsDetailEntity.body);
		mSections = new ArrayList<ArticleHtmlRenderer.Section>(
				renderedArticle.getSections());
		mListener.onTableOfContentsChanged(mSections);

		String html = AssetsUtils.loadText(getActivity(), Constants.TEMPLATE_DEF_URL);
		html = html.replace("{content}", renderedArticle.getBodyHtml());
		
		//是否夜间模式
		SharedPreferences mPerferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		html = html.replace("{nightTheme}", mPerferences.getBoolean("dark_theme?", false) ? "true" : "false");
		
		String headerDef = APP_ASSET_BASE_URL + "news_detail_header_def_v3.jpg";
		
		if (NetWorkHelper.isMobile(getActivity()) && PreferenceManager.getDefaultSharedPreferences(
				getActivity()).getBoolean("noimage_nowifi?", false) ) {
			
		} else if (NewsUrlUtils.isHttpUrl(mNewsDetailEntity.image)) {
			headerDef = mNewsDetailEntity.image;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"img-wrap\">")
				.append("<h1 class=\"headline-title\">")
				.append(htmlEncode(mNewsDetailEntity.title)).append("</h1>")
				.append("<span class=\"img-source\">")
				.append(htmlEncode(mNewsDetailEntity.image_source)).append("</span>")
				.append("<img class=\"detail-hero\" src=\"").append(headerDef)
				.append("\" alt=\"\">")
				.append("<div class=\"img-mask\"></div>");
		
		html = html.replace("<div class=\"img-place-holder\">", sb.toString());
		String resultHTML = replaceImgTagFromHTML(html);
		
		mWebView.loadDataWithBaseURL(APP_ASSET_BASE_URL, resultHTML,
				"text/html", "UTF-8", null);
		mHasContent = true;
		setWebViewShown(true);
	}

	private String htmlEncode(String value) {
		return TextUtils.htmlEncode(value == null ? "" : value);
	}
	
	/**
	 * 替换html中的<img标签的属性
	 * 
	 * @param html
	 * @return
	 */
	private String replaceImgTagFromHTML(String html) {
		
		Document doc = Jsoup.parse(html);

		Elements es = doc.getElementsByTag("img");

		for (Element e : es) {
			String imgUrl = e.attr("src");
			mDetailImageList.add(imgUrl);

			String fileName = ZhihuUtils.getCacheImgFileName(imgUrl);

			e.attr("src_link", ZhihuUtils.getCacheImgAssetUrl(imgUrl));
			e.attr("ori_link", imgUrl);
			
			boolean isHeroImage = e.hasClass("detail-hero");
			if (!isHeroImage) {
				e.attr("src", "");
			}

			if (!isHeroImage && !e.hasClass("avatar")) {
				e.attr("onclick", "openImage('" + fileName + "'); return false;");
			}
		}

		return doc.html();
	}
	
	private class LoadCacheDetailTask extends BackgroundTask<String, Void, String> {
		private long requestedId;

		@Override
		protected String doInBackground(java.util.List<String> params) {
			if (params == null || params.isEmpty()) {
				return null;
			}
			try {
				requestedId = Long.parseLong(params.get(0));
				if (requestedId <= 0L) {
					return null;
				}
				String result = ZhihuApplication.getDataSource()
						.getContent("detail_" + requestedId);
				return NewsApiClient.parseNewsDetail(result, requestedId).rawJson;
			} catch (Exception invalidOrMissingCache) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			forgetViewTask(this);
			if (isAdded() && requestedId == mNewsId) {
				if (!TextUtils.isEmpty(result)) {
					setWebViewShown(true);
					setWebView(result, false);
				} else {
					setWebViewShown(false);
				}
			}
		}
	}

	private WebViewClient mWebViewClient = new WebViewClient() {

		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
			return mAssetLoader.shouldInterceptRequest(request.getUrl());
		}

		@Override
		@SuppressWarnings("deprecation") // Required fallback callback for API 21-23.
		public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
			return mAssetLoader.shouldInterceptRequest(Uri.parse(url));
		}

		@Override
		@SuppressWarnings("deprecation") // Required fallback callback for API 21-23.
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Uri uri = Uri.parse(url);
			if (APP_ASSET_HOST.equals(uri.getHost())) {
				return false;
			}
			if ("zhihupaper-image".equals(uri.getScheme()) && "open".equals(uri.getHost())) {
				openCachedImage(uri.getQueryParameter("file"));
				return true;
			}
			if (NewsUrlUtils.isHttpUrl(url)) {
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
					startActivity(intent);
				}
			}
			return true;
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {

			super.onPageFinished(view, url);

			if (com.cundong.izhihu.BuildConfig.DEBUG) {
				mLogger.i("Article page finished");
			}
			restoreReaderProgressWhenReady();
			
			String urlStrArray[] = new String[mDetailImageList.size()];
			mDetailImageList.toArray(urlStrArray);
			
			if( !isAdded() ) {
				return;
			}
			
			if (NetWorkHelper.isMobile(getActivity()) && PreferenceManager.getDefaultSharedPreferences(
					getActivity()).getBoolean("noimage_nowifi?", false) ) {
				// 无图模式
				
			} else {
				if (mImageDownloadTask != null) {
					mImageDownloadTask.clearListener();
					mImageDownloadTask.cancel(true);
				}
				mImageDownloadListener = new ResponseListener() {

							@Override
							public void onPreExecute() {
								
							}

							@Override
							public void onPostExecute(String content) {
								// Each successfully cached image is revealed from
								// onProgressUpdate. Do not point failed or built-in images
								// at cache files that were never published.
							}

							@Override
							public void onProgressUpdate(String value) {
								
								if (!isAdded()) {
									return;
								}
								
								runJavascript("img_replace_by_url(" + JSONObject.quote(value) + ");");
							}

							@Override
							public void onFail(Exception e) {
								e.printStackTrace();
							}
						};
				mImageDownloadTask = new DetailImageDownloadTask(
						getActivity(), mImageDownloadListener);
				mImageDownloadTask.executeOnExecutor(
						BackgroundTask.DOWNLOAD_THREAD_POOL_EXECUTOR, urlStrArray);
			}
		}
	};
	
	private void runJavascript(String javascript) {
		if (!isAdded() || mWebView == null) {
			return;
		}
		mWebView.evaluateJavascript(javascript, null);
	}

	public void scrollToSection(String sectionId) {
		if (!TextUtils.isEmpty(sectionId)) {
			runJavascript("scrollToReaderSection(" + JSONObject.quote(sectionId) + ");");
		}
	}

	public interface SelectedTextCallback {
		void onSelectedText(String selectedText);
	}

	/** Returns the current WebView selection without exposing a JavaScript bridge. */
	public void requestSelectedText(final SelectedTextCallback callback) {
		if (!isAdded() || mWebView == null || callback == null) {
			if (callback != null) {
				callback.onSelectedText("");
			}
			return;
		}
		mWebView.evaluateJavascript(
				"(window.getSelection ? window.getSelection().toString() : '')",
				value -> {
					String selection = "";
					try {
						selection = new JSONArray("[" + value + "]").getString(0);
					} catch (JSONException ignored) {
						mLogger.w("Unable to decode article selection");
					}
					callback.onSelectedText(selection);
				});
	}

	private boolean shouldRestoreReaderProgress() {
		return PreferenceManager.getDefaultSharedPreferences(getActivity())
				.getBoolean("reader_restore_progress", true);
	}

	private int loadReaderProgress() {
		if (!shouldRestoreReaderProgress()) {
			return 0;
		}
		return PreferenceManager.getDefaultSharedPreferences(getActivity())
				.getInt(ReaderProgress.preferenceKey(mNewsId), 0);
	}

	private void restoreReaderProgressWhenReady() {
		if (mProgressRestored || mPendingProgress <= 0 || mWebView == null) {
			return;
		}
		mProgressRestored = true;
		mWebView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mWebView == null) {
							return;
						}
						int maxScroll = getMaximumScrollY();
						if (maxScroll <= 0) {
							return;
						}
						mWebView.scrollTo(0, Math.round(maxScroll
								* (mPendingProgress / (float) ReaderProgress.MAX_PROGRESS)));
						if (mWebView.getViewTreeObserver().isAlive()) {
							mWebView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
						}
					}
				});
	}

	private void saveReaderProgress() {
		if (!isAdded() || mWebView == null || !mHasContent || !shouldRestoreReaderProgress()) {
			return;
		}
		int maxScroll = getMaximumScrollY();
		int progress = ReaderProgress.fromScrollPosition(mWebView.getScrollY(), maxScroll);
		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
				.putInt(ReaderProgress.preferenceKey(mNewsId), progress).apply();
	}

	private int getMaximumScrollY() {
		return mWebView == null ? 0 : maximumScrollY(mWebView);
	}

	@SuppressWarnings("deprecation") // No public replacement exposes WebView's rendered content scale.
	private static int maximumScrollY(WebView webView) {
		return Math.max(0, Math.round(webView.getContentHeight() * webView.getScale())
				- webView.getHeight());
	}

	@Override
	public void onPause() {
		saveReaderProgress();
		super.onPause();
	}

	@Override
	public void onDestroyView() {
		saveReaderProgress();
		if (mDetailTask != null) {
			mDetailTask.clearListener();
		}
		if (mImageDownloadTask != null) {
			mImageDownloadTask.clearListener();
			mImageDownloadTask.cancel(true);
			mImageDownloadTask = null;
		}
		mImageDownloadListener = null;
		mCacheDetailTask = null;
		mDetailTask = null;
		if (mWebView != null) {
			mWebView.stopLoading();
			mWebView.setWebChromeClient(null);
			mWebView.setWebViewClient(null);
			mWebView.destroy();
			mWebView = null;
		}
		mProgressBar = null;
		mErrorContainer = null;
		mAssetLoader = null;
		mPullToRefreshLayout = null;
		super.onDestroyView();
	}

	@Override
	public void onDetach() {
		mListener = null;
		super.onDetach();
	}

	private void openCachedImage(String fileName) {
		if (!isAdded() || !ZhihuUtils.isCacheImgFileName(fileName)) {
			return;
		}
		Intent intent = new Intent(getActivity(), NewsDetailImageActivity.class);
		intent.putExtra("imageUrl", new java.io.File(ZhihuUtils.getDetailImageCacheDir(getActivity()), fileName).getAbsolutePath());
		startActivity(intent);
	}
	
	/**
	 * WebView正文加载成功之后的回调接口
	 * 
	 */
	public interface OnContentLoadListener {
		public void onComplete(NewsDetailEntity newsDetailEntity);
		public void onTableOfContentsChanged(List<ArticleHtmlRenderer.Section> sections);
	}

	@Override
	public void onProgressUpdate(String value) {
		
	}

	@Override
	protected void onRestoreState(Bundle savedInstanceState) {
		mNewsId = savedInstanceState.getLong("id");
	}

	@Override
	protected void onSaveState(Bundle outState) {
		outState.putLong("id", mNewsId);
	}

	@Override
	protected void onFirstTimeLaunched() {
		Bundle bundle = getArguments();
		mNewsId = bundle != null ? bundle.getLong("id") : 0;
	}
}
