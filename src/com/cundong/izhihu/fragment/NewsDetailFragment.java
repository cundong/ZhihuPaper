package com.cundong.izhihu.fragment;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cundong.izhihu.R;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.task.GetNewsDetailTask;
import com.cundong.izhihu.task.MyAsyncTask;
import com.cundong.izhihu.task.ResponseListener;
import com.cundong.izhihu.util.GsonUtils;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

@SuppressLint("SetJavaScriptEnabled")
public class NewsDetailFragment extends BaseFragment implements ResponseListener {
	
	private WebView mWebView;
	
	private long mNewsId = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle bundle = getArguments();
		mNewsId = bundle != null ? bundle.getLong("id") : 0;
		
		//String content = ZhihuApplication.getDataSource().getContent("detail_" + mNewsId);
		
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

		mPullToRefreshLayout = (PullToRefreshLayout) rootView.findViewById(R.id.ptr_layout);
		mWebView = (WebView) rootView.findViewById(R.id.webview);
		
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);// 设置缓存模式
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.getSettings().setJavaScriptEnabled(true);

		// 支持通过js打开新的窗口
		mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		
		// WebView不重新打开新浏览器
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				
				view.loadUrl(url);
				return true;
			}
		});

//		mWebView.loadUrl( Constants.URL_DETAIL );
		
		return rootView;
	}
	
	@Override
	protected void doRefresh() {
		
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
			
			//TODO content
			
			NewsDetailEntity detailEntity = (NewsDetailEntity)GsonUtils.getEntity(content, NewsDetailEntity.class);
			mWebView.loadUrl(detailEntity.share_url);
		}else{
			
		}
	}

	@Override
	public void onFail(final Exception e) {
		if (isAdded()) {
			getActivity().runOnUiThread(new Runnable() {
				public void run() {

					Crouton.makeText(getActivity(), "error:" + e!=null && e.getCause()!=null ? e.getCause().toString() : "NULL", Style.ALERT).show();
					
				}
			});
		} else {

		}
	}
}