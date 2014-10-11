package com.cundong.izhihu.fragment;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.activity.NewsDetailImageActivity;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.task.DetailImageDownloadTask;
import com.cundong.izhihu.task.GetNewsDetailTask;
import com.cundong.izhihu.task.MyAsyncTask;
import com.cundong.izhihu.task.ResponseListener;
import com.cundong.izhihu.util.AssetsUtils;
import com.cundong.izhihu.util.GsonUtils;
import com.cundong.izhihu.util.NetWorkHelper;
import com.cundong.izhihu.util.ZhihuUtils;

/**
 * 类说明： 	新闻详情页Fragment
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
public class NewsDetailFragment extends BaseFragment implements
		ResponseListener {
	
	private static final String ID = "com.cundong.izhihu.fragment.NewsDetailFragment.id";
	
	private ProgressBar mProgressBar;
	private WebView mWebView;

	private long mNewsId = 0;
	private NewsDetailEntity mNewsDetailEntity = null;
	private ArrayList<String> mDetailImageList = new ArrayList<String>();
	
	private OnContentLoadListener mListener = null;
		
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnContentLoadListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnContentLoadListener");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			Bundle bundle = getArguments();
			mNewsId = bundle != null ? bundle.getLong("id") : 0;
		} else {
			mNewsId = savedInstanceState.getLong(ID);
		}
		
		new LoadCacheDetailTask().executeOnExecutor(
				MyAsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
		
		new GetNewsDetailTask(getActivity(), this).executeOnExecutor(
				MyAsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(ID, mNewsId);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
		
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress);
		mPullToRefreshLayout = (PullToRefreshLayout) rootView.findViewById(R.id.ptr_layout);
		mWebView = (WebView) rootView.findViewById(R.id.webview);

		setUpWebViewDefaults(mWebView);

		mWebView.setWebViewClient(mWebViewClient);
		
		return rootView;
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	private void setUpWebViewDefaults(WebView webView) {
		
		mWebView.addJavascriptInterface(new JavaScriptObject(getActivity()), "injectedObject");
		
		// 设置缓存模式
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		mWebView.getSettings().setJavaScriptEnabled(true);
		
		
		// Use WideViewport and Zoom out if there is no viewport defined
		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.getSettings().setLoadWithOverviewMode(true);
				
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		
		// 支持通过js打开新的窗口
		mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		
		mWebView.setWebChromeClient(new WebChromeClient() {
			 
		    @Override
		    public boolean onJsAlert(WebView view, String url, String message,
		            final JsResult result) {
		    	//Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();  
	            result.cancel();  
	            return true; 
		    }
		 
		    @Override
		    public boolean onJsConfirm(WebView view, String url,
		            String message, final JsResult result) {
		    	
		        return true;
		    }
		});
	}
	
	private void setWebViewShown(boolean shown) {
		mWebView.setVisibility(shown ? View.VISIBLE : View.GONE);
		mProgressBar.setVisibility(shown ? View.GONE : View.VISIBLE);
	}

	@Override
	protected void doRefresh() {
		if (isAdded()) {
			new GetNewsDetailTask(getActivity(), this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
		}
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

			 if (isRefreshSuccess && !isContentSame && !TextUtils.isEmpty(content)) {
				 setWebView(content, true);
			 }
		}

		setWebViewShown(true);
	}

	@Override
	public void onFail(final Exception e) {
		setWebViewShown(true);

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
		
		//tell the activity, mNewsDetailEntity is okey
		mListener.onComplete(mNewsDetailEntity);
		
		String html = AssetsUtils.loadText(getActivity(), Constants.TEMPLATE_DEF_URL);
		html = html.replace("{content}", mNewsDetailEntity.body);
		
		//是否夜间模式
		SharedPreferences mPerferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		html = html.replace("{nightTheme}", mPerferences.getBoolean("dark_theme?", false) ? "true" : "false");
		
		String headerDef = "file:///android_asset/www/news_detail_header_def.jpg";
		
		if (NetWorkHelper.isMobile(getActivity()) && PreferenceManager.getDefaultSharedPreferences(
				getActivity()).getBoolean("noimage_nowifi?", false) ) {
			
		} else {
			headerDef = mNewsDetailEntity.image;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"img-wrap\">")
				.append("<h1 class=\"headline-title\">")
				.append(mNewsDetailEntity.title).append("</h1>")
				.append("<span class=\"img-source\">")
				.append(mNewsDetailEntity.image_source).append("</span>")
				.append("<img src=\"").append(headerDef)
				.append("\" alt=\"\">")
				.append("<div class=\"img-mask\"></div>");
		
		html = html.replace("<div class=\"img-place-holder\">", sb.toString());
		String resultHTML = replaceImgTagFromHTML(html);
		
		mWebView.loadDataWithBaseURL(null, resultHTML, "text/html", "UTF-8", null);
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

			String localImgPath = ZhihuUtils.getCacheImgFilePath(getActivity(), imgUrl);
			
			e.attr("src_link", "file://" + localImgPath);
			e.attr("ori_link", imgUrl);
			
			if(!imgUrl.equals(mDetailImageList.get(0))) {
				e.attr("src", "");
			}

			if (!imgUrl.equals(mDetailImageList.get(0)) && !e.attr("class").equals("avatar") ) {
				e.attr("onclick", "openImage('" + localImgPath + "')");
			}
		}

		return doc.html();
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
					setWebView(result, false);
				} else {
					setWebViewShown(false);
				}
			}
		}
	}

	@Override
	public void onProgressUpdate(String value) {
		
	}
	
	@SuppressLint("NewApi")
	private WebViewClient mWebViewClient = new WebViewClient() {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
			startActivity(intent);
			
			return true;
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {

			super.onPageFinished(view, url);

			mLogger.i("onPageFinished : " + url);
			
			String urlStrArray[] = new String[mDetailImageList.size()];
			mDetailImageList.toArray(urlStrArray);
			
			if( !isAdded() ) {
				return;
			}
			
			if (NetWorkHelper.isMobile(getActivity()) && PreferenceManager.getDefaultSharedPreferences(
					getActivity()).getBoolean("noimage_nowifi?", false) ) {
				// 无图模式
				
			} else {
				new DetailImageDownloadTask(getActivity(),
						new ResponseListener() {

							@Override
							public void onPreExecute() {
								
							}

							@Override
							public void onPostExecute(String content,
									boolean isRefreshSuccess,
									boolean isContentSame) {
								
								if (!isAdded()) {
									return;
								}
								
								String javascript = "img_replace_all();";
								
								if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
									// In KitKat+ you should use the evaluateJavascript method
						            mWebView.evaluateJavascript(javascript, new ValueCallback<String>() {
						                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
						                @Override
						                public void onReceiveValue(String s) {
						                    JsonReader reader = new JsonReader(new StringReader(s));

						                    // Must set lenient to parse single values
						                    reader.setLenient(true);

						                    try {
						                        if(reader.peek() != JsonToken.NULL) {
						                            if(reader.peek() == JsonToken.STRING) {
						                                String msg = reader.nextString();
						                                if(msg != null) {
//						                                    Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
						                                }
						                            }
						                        }
						                    } catch (IOException e) {
						                        Log.e("TAG", "MainActivity: IOException", e);
						                    } finally {
						                        try {
						                            reader.close();
						                        } catch (IOException e) {
						                            // NOOP
						                        }
						                    }
						                }
						            });
								} else {
									 mWebView.loadUrl( "javascript:" + javascript);
								}
							}

							@Override
							public void onProgressUpdate(String value) {
								
								if (!isAdded()) {
									return;
								}
								
								String javascript = "img_replace_by_url('" + value + "')";
								
								if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
								    mWebView.evaluateJavascript(javascript, new ValueCallback<String>(){
								    	
										@Override
										public void onReceiveValue(String s) {
						                    JsonReader reader = new JsonReader(new StringReader(s));

						                    // Must set lenient to parse single values
						                    reader.setLenient(true);

						                    try {
						                        if(reader.peek() != JsonToken.NULL) {
						                            if(reader.peek() == JsonToken.STRING) {
						                                String msg = reader.nextString();
						                                if(msg != null) {
//						                                    Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
						                                }
						                            }
						                        }
						                    } catch (IOException e) {
						                        Log.e("TAG", "MainActivity: IOException", e);
						                    } finally {
						                        try {
						                            reader.close();
						                        } catch (IOException e) {
						                            // NOOP
						                        }
						                    }
										}
								    	
								    });
								} else {
								    mWebView.loadUrl("javascript:" + javascript);
								}
							}

							@Override
							public void onFail(Exception e) {
								e.printStackTrace();
							}
						}).executeOnExecutor(MyAsyncTask.DOWNLOAD_THREAD_POOL_EXECUTOR, urlStrArray);
			}
		}
	};
	
	public static class JavaScriptObject {

		private Activity mInstance;

		public JavaScriptObject(Activity instance) {
			mInstance = instance;
		}
		
		@JavascriptInterface 
		public void openImage(String url) {
			
			if (mInstance != null && !mInstance.isFinishing()) {
				
				Intent intent = new Intent(mInstance, NewsDetailImageActivity.class);
				intent.putExtra("imageUrl", url);
				
				mInstance.startActivity(intent);
			}
		}
	}
	
	/**
	 * WebView正文加载成功之后的回调接口
	 * 
	 */
	public interface OnContentLoadListener {
		public void onComplete(NewsDetailEntity newsDetailEntity);
	}
}