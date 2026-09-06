package com.cundong.izhihu.fragment;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
import android.widget.TextView;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.activity.NewsDetailActivity;
import com.cundong.izhihu.adapter.NewsAdapter;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.entity.NewsListEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.http.NewsApiClient;
import com.cundong.izhihu.http.NewsApiClient.Payload;
import com.cundong.izhihu.task.BaseGetNewsTask;
import com.cundong.izhihu.task.BaseGetNewsTask.ResponseListener;
import com.cundong.izhihu.task.GetLatestNewsTask;
import com.cundong.izhihu.task.BackgroundTask;
import com.cundong.izhihu.util.ListUtils;
import com.cundong.izhihu.util.ReadingQueue;
import com.cundong.izhihu.util.ZhihuUtils;

public class NewsListFragment extends BaseFragment implements ResponseListener, OnItemClickListener {

	private ListView mListView;
	private View mEmptyContainer;
	private ProgressBar mProgressBar;
	private NewsAdapter mAdapter = null;
	
	private ArrayList<NewsEntity> mNewsList = null;
	
	//上次listView滚动到最下方时，itemId
	private int mListViewPreLast = 0;
	private String mCurrentDate = null;
	private LoadCacheNewsTask mCacheTask;
	private GetLatestNewsTask mLatestTask;
	private GetMoreNewsTask mMoreTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_main, container, false);
		
		mPullToRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.ptr_layout);
		mListView = (ListView) view.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
		mEmptyContainer = view.findViewById(R.id.empty_container);
		((TextView) view.findViewById(R.id.empty_title)).setText(R.string.error_list_title);
		((TextView) view.findViewById(R.id.empty_subtitle)).setText(R.string.error_list_subtitle);
		view.findViewById(R.id.empty_retry).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View retryView) {
				doRefresh();
			}
		});
		
		return view;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		startCacheLoad();
		startLatestLoad();

		mListView.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				
				final int lastItem = firstVisibleItem + visibleItemCount;
				
				if (totalItemCount > 0 && lastItem == totalItemCount
						&& !TextUtils.isEmpty(mCurrentDate)) {
					if (mListViewPreLast != lastItem) { // to avoid multiple calls for
						String requestedDate = ZhihuUtils.getBeforeDate(mCurrentDate);
						if (mMoreTask == null
								|| mMoreTask.getStatus() == BackgroundTask.Status.FINISHED) {
							mMoreTask = trackViewTask(new GetMoreNewsTask(requireContext(), null));
							mMoreTask.executeOnExecutor(
									BackgroundTask.THREAD_POOL_EXECUTOR, requestedDate);
						}
						
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
		mEmptyContainer.setVisibility(View.GONE);
		mProgressBar.setVisibility(isListViewShown ? View.GONE : View.VISIBLE);
	}

	private void showEmptyState(int titleRes, int subtitleRes) {
		mListView.setVisibility(View.GONE);
		mProgressBar.setVisibility(View.GONE);
		mEmptyContainer.setVisibility(View.VISIBLE);
		((TextView) getView().findViewById(R.id.empty_title)).setText(titleRes);
		((TextView) getView().findViewById(R.id.empty_subtitle)).setText(subtitleRes);
	}
	
	//读取缓存中的最新新闻
	private class LoadCacheNewsTask extends BackgroundTask<String, Void, NewsListEntity> {

		@Override
		protected NewsListEntity doInBackground(java.util.List<String> params) {

			NewsDataSource dataSource = ZhihuApplication.getDataSource();
			NewsListEntity candidate = dataSource.getLatestNews();
			if (candidate == null || TextUtils.isEmpty(candidate.date)) {
				return null;
			}
			try {
				String cachedContent = dataSource.getContent(candidate.date);
				Payload<NewsListEntity> cached = NewsApiClient.parseNewsList(
						cachedContent, candidate.date);
				ZhihuUtils.setReadStatus4NewsList(cached.entity.stories);
				return cached.entity;
			} catch (Exception ignored) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(NewsListEntity result) {
			forgetViewTask(this);
			if(!isAdded() || getView() == null)
				return;
			
			if (result != null && !ListUtils.isEmpty(result.stories)
					&& (TextUtils.isEmpty(mCurrentDate)
							|| result.date.compareTo(mCurrentDate) >= 0)) {
				mCurrentDate = result.date;
				
				NewsEntity tagNewsEntity = new NewsEntity();
				tagNewsEntity.isTag = true;
				tagNewsEntity.title = result.date;
				
				mNewsList = new ArrayList<NewsEntity>();
				mNewsList.add(tagNewsEntity);
				mNewsList.addAll(result.stories);
				
				setAdapter(mNewsList);
			} else if (mNewsList == null || mNewsList.isEmpty()) {
				showEmptyState(R.string.error_list_title, R.string.error_list_subtitle);
			}
		}
	}
	
	//下载过往的新闻
	private class GetMoreNewsTask extends BaseGetNewsTask {

		public GetMoreNewsTask(Context context, ResponseListener listener) {
			super(context, listener);
		}
		
		@Override
		protected NewsListEntity doInBackground(java.util.List<String> params) {
			
			if (params == null || params.isEmpty()) {
				isRefreshSuccess = false;
				mException = new IllegalArgumentException("Missing feed date");
				return null;
			}
			
			String theKey = params.get(0);
			if (TextUtils.isEmpty(theKey) || !theKey.matches("\\d{8}")) {
				isRefreshSuccess = false;
				mException = new IllegalArgumentException("Invalid feed date");
				return null;
			}
			NewsDataSource dataSource = (NewsDataSource) getDataSource();
			String oldContent = null;
			try {
				oldContent = dataSource.getContent(theKey);
			} catch (Exception cacheReadException) {
				cacheReadException.printStackTrace();
			}

			if (!TextUtils.isEmpty(oldContent)) {
				try {
					Payload<NewsListEntity> cached = NewsApiClient.parseNewsList(
							oldContent, theKey);
					isRefreshSuccess = true;
					isContentSame = true;
					mException = null;
					ZhihuUtils.setReadStatus4NewsList(cached.entity.stories);
					return cached.entity;
				} catch (Exception ignored) {
					// A legacy or corrupt row is not a last-good cache. Try the source.
				}
			}

			try {
				String newContent = getUrl(Constants.Url.URLDEFORE
						+ ZhihuUtils.getAddedDate(theKey));
				Payload<NewsListEntity> network = NewsApiClient.parseNewsList(
						newContent, theKey);
				try {
					isContentSame = checkIsContentSame(oldContent, network.rawJson);
					if (!isContentSame) {
						dataSource.insertOrUpdateNewsList(Constants.NEWS_LIST,
								theKey, network.rawJson);
					}
				} catch (Exception storageException) {
					// Keep the validated response readable even if cache persistence fails.
					storageException.printStackTrace();
				}
				isRefreshSuccess = true;
				mException = null;
				ZhihuUtils.setReadStatus4NewsList(network.entity.stories);
				return network.entity;
			} catch (Exception e) {
				e.printStackTrace();
				isRefreshSuccess = false;
				mException = e;
				return null;
			}
		}

		@Override
		protected void onPostExecute(NewsListEntity result) {
			super.onPostExecute(result);
			forgetViewTask(this);

			if(!isAdded())
				return;

			if (!isRefreshSuccess) {
				mListViewPreLast = 0;
				if (mListener == null) {
					NewsListFragment.this.onFail(mException);
				}
				return;
			}
			
			setListShown(true);

			mListViewPreLast = 0;
			
			if (mNewsList == null) {
				mNewsList = new ArrayList<NewsEntity>();
			}
			
			if (result != null && !ListUtils.isEmpty(result.stories)) {
				mCurrentDate = result.date;
				
				NewsEntity tagNewsEntity = new NewsEntity();
				tagNewsEntity.isTag = true;
				tagNewsEntity.title = result.date;
				mNewsList.add(tagNewsEntity);
				mNewsList.addAll(result.stories);
				
				setAdapter(mNewsList);
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
	public void onPostExecute(NewsListEntity result) {
		if(!isAdded())
			return;
		
		// Notify PullToRefreshLayout that the refresh has finished
		finishRefresh();
					
		if (getView() != null) {
			// Show the list again
			setListShown(true);
		}
		
		if (result != null && !ListUtils.isEmpty(result.stories)) {
			mNewsList = new ArrayList<NewsEntity>();

			NewsEntity tagNewsEntity = new NewsEntity();
			tagNewsEntity.isTag = true;
			tagNewsEntity.title = result.date;
			mNewsList.add(tagNewsEntity);

			mNewsList.addAll(result.stories);

			mCurrentDate = result.date;

			setAdapter(mNewsList);
		} else if (getView() != null) {
			showEmptyState(R.string.error_list_title, R.string.error_list_subtitle);
		}
	}
	
	@Override
	public void onFail(Exception e) {
		
		if (getView() != null) {
			if (mNewsList == null || mNewsList.isEmpty()) {
				showEmptyState(R.string.error_list_title, R.string.error_list_subtitle);
			} else {
				setListShown(true);
			}
		}
		
		dealException(e);
	}

	@Override
	protected void doRefresh() {
		
		// Hide the list
		setListShown( mNewsList==null ||mNewsList.isEmpty() ? false : true );
		
		startLatestLoad();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		NewsEntity newsEntity = mNewsList != null ? mNewsList.get(position) : null;

		if (newsEntity == null)
			return;
		if (newsEntity.isTag) {
			return;
		}

		Intent intent = new Intent();
		intent.putExtra("id", newsEntity.id);
		intent.putExtra("newsEntity", newsEntity);

		ArrayList<ReadingQueue.Item> queueItems = new ArrayList<ReadingQueue.Item>();
		for (NewsEntity item : mNewsList) {
			if (item == null || item.isTag || item.id <= 0L) {
				continue;
			}
			String image = ListUtils.isEmpty(item.images) ? null : item.images.get(0);
			queueItems.add(new ReadingQueue.Item(item.id, item.title, item.share_url, image));
		}
		ReadingQueue.install(queueItems);
		ReadingQueue.Window window = ReadingQueue.around(newsEntity.id, null, null);
		ReadingQueue.put(intent, ReadingQueue.EXTRA_PREVIOUS, window.getPrevious());
		ReadingQueue.put(intent, ReadingQueue.EXTRA_NEXT, window.getNext());

		intent.setClass(getActivity(), NewsDetailActivity.class);
		startActivity(intent);
		
		SetReadFlagTask task = trackViewTask(new SetReadFlagTask(newsEntity));
		task.executeOnExecutor(BackgroundTask.THREAD_POOL_EXECUTOR);
	}
	
	public void updateList() {
		startCacheLoad();
	}

	private void startCacheLoad() {
		cancelViewTask(mCacheTask);
		mCacheTask = trackViewTask(new LoadCacheNewsTask());
		mCacheTask.executeOnExecutor(BackgroundTask.THREAD_POOL_EXECUTOR);
	}

	private void startLatestLoad() {
		if (mLatestTask != null) {
			mLatestTask.clearListener();
			cancelViewTask(mLatestTask);
		}
		mLatestTask = trackViewTask(new GetLatestNewsTask(requireContext(), this));
		mLatestTask.executeOnExecutor(BackgroundTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onDestroyView() {
		if (mLatestTask != null) {
			mLatestTask.clearListener();
		}
		if (mMoreTask != null) {
			mMoreTask.clearListener();
		}
		mCacheTask = null;
		mLatestTask = null;
		mMoreTask = null;
		mListView = null;
		mEmptyContainer = null;
		mProgressBar = null;
		mPullToRefreshLayout = null;
		super.onDestroyView();
	}

	@Override
	protected void onRestoreState(Bundle savedInstanceState) {

	}

	@Override
	protected void onSaveState(Bundle outState) {

	}

	@Override
	protected void onFirstTimeLaunched() {

	}
	
	private class SetReadFlagTask extends BackgroundTask<String, Void, Boolean> {

		private NewsEntity mNewsEntity;

		public SetReadFlagTask(NewsEntity newsEntity) {
			mNewsEntity = newsEntity;
		}

		@Override
		protected Boolean doInBackground(java.util.List<String> params) {
			return ZhihuApplication.getNewsReadDataSource().readNews(String.valueOf(mNewsEntity.id));
		}

		@Override
		protected void onPostExecute(Boolean result) {
			forgetViewTask(this);
			if (Boolean.TRUE.equals(result)) {
				ZhihuUtils.setReadStatus4NewsEntity(mNewsList, mNewsEntity);
				mAdapter.updateData(mNewsList);
			}
		}
	}
}
