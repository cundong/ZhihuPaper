package com.cundong.izhihu.fragment;

import java.io.IOException;
import java.util.ArrayList;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.activity.NewsDetailActivity;
import com.cundong.izhihu.adapter.NewsAdapter;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.entity.NewsListEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.task.BaseGetContentTask;
import com.cundong.izhihu.task.GetLatestNewsTask;
import com.cundong.izhihu.task.MyAsyncTask;
import com.cundong.izhihu.task.ResponseListener;
import com.cundong.izhihu.util.GsonUtils;
import com.cundong.izhihu.util.ListUtils;
import com.cundong.izhihu.util.ZhihuUtils;

public class NewsListFragment extends BaseFragment implements ResponseListener, OnItemClickListener {

	private ListView mListView;
	private ProgressBar mProgressBar;
	private NewsAdapter mAdapter = null;
	
	private ArrayList<NewsEntity> mNewsList = null;
	
	//上次listView滚动到最下方时，itemId
	private int mListViewPreLast = 0;
	private String mCurrentDate = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		new LoadCacheNewsTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
		
		new GetLatestNewsTask(getActivity(), this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_main, container, false);
		
		mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
		mListView = (ListView) view.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
		
		return view;
	}
	
	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);
		
		mListView.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				
				final int lastItem = firstVisibleItem + visibleItemCount;
				
				if (lastItem == totalItemCount) {
					if (mListViewPreLast != lastItem) { // to avoid multiple calls for
						
						mCurrentDate = ZhihuUtils.getBeforeDate(mCurrentDate);
						
						new GetMoreNewsTask(getActivity(), null).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, mCurrentDate);
						
						mListViewPreLast = lastItem;
					}
				}
			}
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				
			}
		});
	}

	private void setAdapter(ArrayList<NewsEntity> newsList) {
		if (mAdapter == null) {
			mAdapter = new NewsAdapter(getActivity(), newsList);
			mListView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(newsList);
		}
	}

	private void setListShown(boolean isListViewShown) {
		mListView.setVisibility(isListViewShown ? View.VISIBLE : View.GONE);
		mProgressBar.setVisibility(isListViewShown ? View.GONE : View.VISIBLE);
	}
	
	//读取缓存中的最新新闻
	private class LoadCacheNewsTask extends MyAsyncTask<String, Void, NewsListEntity> {

		@Override
		protected NewsListEntity doInBackground(String... params) {

			NewsListEntity latestNewsEntity = ZhihuApplication.getDataSource().getLatestNews();
			
			if (latestNewsEntity != null) {
				mCurrentDate = latestNewsEntity.date;
				ZhihuUtils.setReadStatus4NewsList(latestNewsEntity.stories);
			}
			
			return latestNewsEntity;
		}

		@Override
		protected void onPostExecute(NewsListEntity result) {
			super.onPostExecute(result);
			
			if (isAdded()) {
				if (result != null && !ListUtils.isEmpty(result.stories)) {
					
					NewsEntity tagNewsEntity = new NewsEntity();
					tagNewsEntity.isTag = true;
					tagNewsEntity.title = result.date;
					
					mNewsList = new ArrayList<NewsEntity>();
					mNewsList.add(tagNewsEntity);
					mNewsList.addAll(result.stories);
					
					setAdapter(mNewsList);
				}
			}
		}
	}
	
	//下载过往的新闻
	private class GetMoreNewsTask extends BaseGetContentTask {

		public GetMoreNewsTask(Context context, ResponseListener listener) {
			super(context, listener);
		}
		
		@Override
		protected String doInBackground(String... params) {
			
			if (params.length == 0)
				return null;
			
			String theKey = params[0];
			
			String oldContent = ((NewsDataSource) getDataSource()).getContent(theKey);
			
			NewsListEntity newsListEntity = null;
			
			if (!TextUtils.isEmpty(oldContent)) {
				newsListEntity = (NewsListEntity) GsonUtils.getEntity(oldContent, NewsListEntity.class);
				if (newsListEntity != null) {
					ZhihuUtils.setReadStatus4NewsList(newsListEntity.stories);
				}
				return oldContent;
			} else {
				
				String newContent = null;
				
				try {
					newContent = getUrl(Constants.Url.URLDEFORE + ZhihuUtils.getAddedDate(theKey));
	
					newsListEntity = (NewsListEntity)GsonUtils.getEntity(newContent, NewsListEntity.class);
					
					isRefreshSuccess = !ListUtils.isEmpty(newsListEntity.stories);
				} catch (IOException e) {
					e.printStackTrace();
					
					this.isRefreshSuccess = false;
					this.mException = e;
				} catch (Exception e) {
					e.printStackTrace();

					this.isRefreshSuccess = false;
					this.mException = e;
				}
				
				isContentSame = checkIsContentSame(oldContent, newContent);
				
				if (isRefreshSuccess && !isContentSame) {
					((NewsDataSource) getDataSource()).insertOrUpdateNewsList(Constants.NEWS_LIST, theKey, newContent);
				}
				
				if (newsListEntity != null) {
					ZhihuUtils.setReadStatus4NewsList(newsListEntity.stories);
				}
				
				return newContent;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			if (isAdded()) {

				setListShown(true);

				mListViewPreLast = 0;
				
				if (mNewsList == null) {
					mNewsList = new ArrayList<NewsEntity>();
				}
				
				NewsListEntity newsListEntity = (NewsListEntity) GsonUtils.getEntity(result, NewsListEntity.class);
				if (newsListEntity != null && !ListUtils.isEmpty(newsListEntity.stories)) {
					
					NewsEntity tagNewsEntity = new NewsEntity();
					tagNewsEntity.isTag = true;
					tagNewsEntity.title = newsListEntity.date;
					mNewsList.add(tagNewsEntity);
					mNewsList.addAll(newsListEntity.stories);
					
					setAdapter(mNewsList);
				}
				
			}
		}
	}
	
	@Override
	public void onPreExecute() {
		
	}

	@Override
	public void onProgressUpdate(String value) {
		
	}
	
	@Override
	public void onPostExecute(String content) {
		if(!isAdded())
			return;
		
		// Notify PullToRefreshLayout that the refresh has finished
		mPullToRefreshLayout.setRefreshComplete();
					
		if (getView() != null) {
			// Show the list again
			setListShown(true);
		}
		
		NewsListEntity newsListEntity = (NewsListEntity) GsonUtils.getEntity(content, NewsListEntity.class);
		if (newsListEntity != null) {
			mNewsList = new ArrayList<NewsEntity>();

			NewsEntity tagNewsEntity = new NewsEntity();
			tagNewsEntity.isTag = true;
			tagNewsEntity.title = newsListEntity.date;
			mNewsList.add(tagNewsEntity);

			mNewsList.addAll(newsListEntity.stories);

			mCurrentDate = newsListEntity.date;

			setAdapter(mNewsList);
		}
	}
	
	@Override
	public void onFail(Exception e) {
		
		if (getView() != null) {
			// Show the list again
			setListShown(true);
		}
		
		dealException(e);
	}

	@Override
	protected void doRefresh() {
		
		// Hide the list
		setListShown( mNewsList==null ||mNewsList.isEmpty() ? false : true );
		
		new GetLatestNewsTask(getActivity(), this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		NewsEntity newsEntity = mNewsList != null ? mNewsList.get(position) : null;

		if (newsEntity == null)
			return;

		Intent intent = new Intent();
		intent.putExtra("id", newsEntity.id);
		intent.putExtra("newsEntity", newsEntity);

		intent.setClass(getActivity(), NewsDetailActivity.class);
		getActivity().startActivity(intent);

		// 设置已读标识
		boolean setReadFlag = ZhihuApplication.getNewsReadDataSource().readNews(String.valueOf(newsEntity.id));

		if (setReadFlag) {
			ZhihuUtils.setReadStatus4NewsEntity(mNewsList, newsEntity);
			mAdapter.updateData(mNewsList);
		}
	}
	
	public void updateList() {
		new LoadCacheNewsTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
	}
}