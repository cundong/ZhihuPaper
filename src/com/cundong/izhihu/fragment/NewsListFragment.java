package com.cundong.izhihu.fragment;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
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
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.task.BaseGetNewsListTask;
import com.cundong.izhihu.task.BaseGetNewsListTask.ResponseListener;
import com.cundong.izhihu.task.GetNewsTask;
import com.cundong.izhihu.task.MyAsyncTask;
import com.cundong.izhihu.util.GsonUtils;
import com.cundong.izhihu.util.ZhihuUtils;

public class NewsListFragment extends BaseFragment implements ResponseListener, OnItemClickListener {

	private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
	
	private ListView mListView;
	private ProgressBar mProgressBar;
	private NewsAdapter mAdapter = null;
	
	private ArrayList<NewsEntity> mNewsList = null;
	private String mNewsLatest = null;
	
	private Calendar mCalendar = Calendar.getInstance();

	//上次listView滚动到最下方时，itemId
	private int mListViewPreLast = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mCalendar.add(Calendar.DAY_OF_YEAR, 1);
		
		mNewsLatest = mSimpleDateFormat.format(mCalendar.getTime());
		
		new LoadCacheNewsTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, mNewsLatest);
		
		new GetNewsTask(getActivity(), this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, mNewsLatest);
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
												// last item
						
						mCalendar.add(Calendar.DAY_OF_YEAR, -1);
						
						String formatedDate = mSimpleDateFormat.format(mCalendar.getTime());
						
						Log.d("@Cundong", "Last!!" + formatedDate );
						
						new GetMoreNewsTask(getActivity(), null).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, formatedDate);
						
						mListViewPreLast = lastItem;
					}
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				
			}
		});
	}

	private void setListShown(boolean isListViewShown){
		mListView.setVisibility( isListViewShown ? View.VISIBLE : View.GONE);
		mProgressBar.setVisibility( isListViewShown ? View.GONE : View.VISIBLE);
	}
	
	//读取本地缓存展示
	private class LoadCacheNewsTask extends MyAsyncTask<String, Void, ArrayList<NewsEntity>> {

		@Override
		protected ArrayList<NewsEntity> doInBackground(String... params) {
			ArrayList<NewsEntity> newsList = ZhihuApplication.getDataSource().getNewsList(params[0]);
			ZhihuUtils.setReadStatus4NewsList(newsList);
			return newsList;
		}

		@Override
		protected void onPostExecute(ArrayList<NewsEntity> result) {
			super.onPostExecute(result);
			
			if (isAdded()) {
				if (result != null && !result.isEmpty()) {

					mNewsList = result;
					
					if (mAdapter == null) {
						mAdapter = new NewsAdapter(getActivity(), mNewsList);
						mListView.setAdapter(mAdapter);
					} else {
						mAdapter.updateData(mNewsList);
						mAdapter.notifyDataSetChanged();
					}
				}
			}
		}
	}
	
	//下载旧闻
	private class GetMoreNewsTask extends BaseGetNewsListTask {

		public GetMoreNewsTask(Context context, ResponseListener listener) {
			super(context, listener);
		}
		
		@Override
		protected ArrayList<NewsEntity> doInBackground(String... params) {
			
			if (params.length == 0)
				return null;
			
			String theKey = params[0];
			String oldContent = ZhihuApplication.getDataSource().getContent(theKey);
			
			if(!TextUtils.isEmpty(oldContent)) {
				ArrayList<NewsEntity> newsList = GsonUtils.getNewsList(oldContent);
				ZhihuUtils.setReadStatus4NewsList(newsList);
				return newsList;
			} else {
				String newContent = null;

				try {
					newContent = getUrl(Constants.Url.URLDEFORE + theKey);
					ZhihuApplication.getDataSource().insertOrUpdateNewsList(theKey, newContent);
				} catch (IOException e) {
					e.printStackTrace();
					
					this.isRefreshSuccess = false;
					this.e = e;
				} catch (Exception e) {
					e.printStackTrace();

					this.isRefreshSuccess = false;
					this.e = e;
				}
				
				ArrayList<NewsEntity> newsList = GsonUtils.getNewsList(newContent);
				ZhihuUtils.setReadStatus4NewsList(newsList);
				return newsList;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<NewsEntity> resultList) {
			super.onPostExecute(resultList);
			
			if (isAdded()) {
				if (mNewsList != null) {
					
					if (resultList != null) {
						mNewsList.addAll(resultList);
						
						mAdapter.updateData(mNewsList);
					}
				}
			}
		}
	}
	
	@Override
	public void onPreExecute() {
		
	}

	@Override
	public void onPostExecute(ArrayList<NewsEntity> resultList, boolean isRefreshSuccess, boolean isContentSame) {
		
		if (isAdded()) {
			
			// Notify PullToRefreshLayout that the refresh has finished
			mPullToRefreshLayout.setRefreshComplete();
			
			if (getView() != null) {
				// Show the list again
				setListShown(true);
			}
			
	        if (isRefreshSuccess && !isContentSame) {
	        	mNewsList = resultList;
	        	
				if (mAdapter != null) {
					mAdapter.updateData(mNewsList);
				} else {
					mAdapter = new NewsAdapter(getActivity(), mNewsList);
					mListView.setAdapter(mAdapter);
				}
	        }
		}
	}

	@Override
	public void onFail(Exception e) {
		
		dealException(e);
	}

	@Override
	protected void doRefresh() {
		
		// Hide the list
		setListShown( mNewsList==null ||mNewsList.isEmpty() ? false : true );
		
		new GetNewsTask(getActivity(), this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, mNewsLatest);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		NewsEntity newsEntity = mNewsList!=null ? mNewsList.get(position) : null;
		
		if (newsEntity == null)
			return;
		
		Intent intent = new Intent();
		intent.putExtra("id", newsEntity.id);
		intent.putExtra("newsEntity", newsEntity);
		
		intent.setClass(getActivity(), NewsDetailActivity.class);
		getActivity().startActivity(intent);
		
		//设置已读标识
		boolean setReadFlag = ZhihuApplication.getNewsReadDataSource().readNews(String.valueOf(newsEntity.id));
		
		if (setReadFlag) {
			ZhihuUtils.setReadStatus4NewsEntity(mNewsList, newsEntity);
			mAdapter.updateData(mNewsList);
		}
	}

	@Override
	public void onProgressUpdate(String value) {
		
	}
	
	public void updateList() {
		new LoadCacheNewsTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, mNewsLatest);
	}
}