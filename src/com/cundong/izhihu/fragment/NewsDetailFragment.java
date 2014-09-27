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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.Toast;

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
import com.cundong.izhihu.util.MD5Util;
import com.cundong.izhihu.util.SDCardUtils;

public class NewsDetailFragment extends BaseFragment implements
		ResponseListener {

	private ProgressBar mProgressBar;
	private WebView mWebView;

	private long mNewsId = 0;
	private String mDetailContent = null;
	private ArrayList<String> mDetailImageList = new ArrayList<String>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle = getArguments();
		mNewsId = bundle != null ? bundle.getLong("id") : 0;

		new LoadCacheDetailTask().executeOnExecutor(
				MyAsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
		
		new GetNewsDetailTask(this).executeOnExecutor(
				MyAsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(mNewsId));
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
		    	Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();  
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

			setWebView(content, true);
		}

		setWebViewShown(true);
	}

	@Override
	public void onFail(final Exception e) {
		setWebViewShown(true);

		dealException(e);
	}

	private void setWebView(String content, boolean isUpdateMode) {

		if (!isAdded()) {
			return;
		}
		
		if (isUpdateMode) {
			if (!TextUtils.isEmpty(content)
					&& !TextUtils.isEmpty(mDetailContent)
					&& content.equals(mDetailContent)) {
				return;
			}
		}
		
		mDetailContent = content;
		
		NewsDetailEntity detailEntity = (NewsDetailEntity) GsonUtils.getEntity(
				content, NewsDetailEntity.class);
		
		if (detailEntity == null || TextUtils.isEmpty(detailEntity.body)) {
			return;
		}
		
		String html = AssetsUtils.loadText(getActivity(), Constants.TEMPLATE_DEF_URL);
		html = html.replace("{content}", detailEntity.body);
		
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"img-wrap\">")
				.append("<h1 class=\"headline-title\">")
				.append(detailEntity.title).append("</h1>")
				.append("<span class=\"img-source\">")
				.append(detailEntity.image_source).append("</span>")
				.append("<img src=\"").append(detailEntity.image)
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

			String localImgPath = SDCardUtils.getExternalCacheDir(getActivity())+ MD5Util.encrypt(imgUrl) + ".jpg";
			e.attr("src_link", "file://" + localImgPath);
			e.attr("ori_link", imgUrl);
			
			if (!imgUrl.equals(mDetailImageList.get(0)) && !imgUrl.equals(mDetailImageList.get(1))) {
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

			mDetailContent = result;
			
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
			Intent intent = new Intent("android.intent.action.VIEW", Uri
					.parse(url));
			startActivity(intent);
			
			return true;
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {

			super.onPageFinished(view, url);

			mLogger.i("onPageFinished : " + url);
			
			String urlStrArray[] = new String[mDetailImageList.size()];
			mDetailImageList.toArray(urlStrArray);
			
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
							
							String url = "img_replace_all();";
							
							
							if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
								// In KitKat+ you should use the evaluateJavascript method
					            mWebView.evaluateJavascript(url, new ValueCallback<String>() {
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
					                                    Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
					                                    Log.i("@Cundong", "msg:"+msg);
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
								 mWebView.loadUrl( "javascript:" + url);
							}
						}

						@Override
						public void onProgressUpdate(String value) {
							
							if (!isAdded()) {
								return;
							}
							
							String url = "img_replace_by_url('" + value + "')";
							
							if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
							    mWebView.evaluateJavascript(url, new ValueCallback<String>(){

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
					                                    Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
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
							    mWebView.loadUrl("javascript:" + url);
							}
						}

						@Override
						public void onFail(Exception e) {
							e.printStackTrace();
						}
					}).executeOnExecutor(MyAsyncTask.DOWNLOAD_THREAD_POOL_EXECUTOR, urlStrArray);
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
}