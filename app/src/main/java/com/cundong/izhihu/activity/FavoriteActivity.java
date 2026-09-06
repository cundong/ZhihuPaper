package com.cundong.izhihu.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.adapter.NewsAdapter;
import com.cundong.izhihu.db.NewsFavoriteDataSource;
import com.cundong.izhihu.entity.LibraryItemEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.task.BackgroundTask;
import com.cundong.izhihu.util.ListUtils;
import com.cundong.izhihu.util.ReadingQueue;

/**
 * 类说明： 	收藏夹，Activity
 * 
 * @date 	2014-10-10
 * @version 1.0
 */
public class FavoriteActivity extends BaseActivity {
	
	private ListView mListView;
	private View mEmptyContainer;
	private NewsAdapter mAdapter = null;
	private ActionMode mActionMode;
	
	private ArrayList<NewsEntity> mNewsList = null;
	private String mSearchQuery = "";
	private int mLibraryFilter = NewsFavoriteDataSource.LIBRARY_FILTER_ALL;
	private int mLoadGeneration;
	private static final long SEARCH_DEBOUNCE_MILLIS = 250L;
	private final Handler mSearchHandler = new Handler(Looper.getMainLooper());
	private LoadLibraryTask mLoadTask;
	private final Runnable mSearchRunnable = new Runnable() {
		@Override
		public void run() {
			loadLibrary();
		}
	};
	private final ActivityResultLauncher<Intent> detailLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(), result -> loadLibrary());
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setupToolbar(R.layout.activity_favorite, true);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(R.string.library_title);
		}
		
		mListView = (ListView) findViewById(R.id.list);
		mEmptyContainer = findViewById(R.id.empty_container);
		((TextView) findViewById(R.id.empty_title)).setText(R.string.library_empty_title);
		((TextView) findViewById(R.id.empty_subtitle)).setText(R.string.library_empty_subtitle);
		SearchView searchView = findViewById(R.id.library_search);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				mSearchQuery = query == null ? "" : query.trim();
				loadLibrary();
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				mSearchQuery = newText == null ? "" : newText.trim();
				mSearchHandler.removeCallbacks(mSearchRunnable);
				mSearchHandler.postDelayed(mSearchRunnable, SEARCH_DEBOUNCE_MILLIS);
				return true;
			}
		});

		MaterialButtonToggleGroup filters = findViewById(R.id.library_filters);
		filters.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
			if (!isChecked) {
				return;
			}
			if (checkedId == R.id.library_filter_favorites) {
				mLibraryFilter = NewsFavoriteDataSource.LIBRARY_FILTER_FAVORITES;
			} else if (checkedId == R.id.library_filter_read_later) {
				mLibraryFilter = NewsFavoriteDataSource.LIBRARY_FILTER_READ_LATER;
			} else {
				mLibraryFilter = NewsFavoriteDataSource.LIBRARY_FILTER_ALL;
			}
			if (mActionMode != null) {
				mActionMode.finish();
			}
			loadLibrary();
		});
		mListView.setOnItemClickListener( new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				
				NewsEntity newsEntity = mNewsList!=null ? mNewsList.get(position) : null;
				
				if (newsEntity == null)
					return;
				
				if (mActionMode == null) {
					
					Intent intent = new Intent();
					intent.putExtra("id", newsEntity.id);
					intent.putExtra("newsEntity", newsEntity);

					ArrayList<ReadingQueue.Item> queueItems =
							new ArrayList<ReadingQueue.Item>();
					for (NewsEntity item : mNewsList) {
						if (item == null || item.isTag || item.id <= 0L) {
							continue;
						}
						String image = ListUtils.isEmpty(item.images) ? null : item.images.get(0);
						queueItems.add(new ReadingQueue.Item(item.id, item.title,
								item.share_url, image));
					}
					ReadingQueue.install(queueItems);
					ReadingQueue.Window window = ReadingQueue.around(newsEntity.id, null, null);
					ReadingQueue.put(intent, ReadingQueue.EXTRA_PREVIOUS, window.getPrevious());
					ReadingQueue.put(intent, ReadingQueue.EXTRA_NEXT, window.getNext());
					
					intent.setClass(mInstance, NewsDetailActivity.class);
						detailLauncher.launch(intent);
				} else {
					
					// add or remove selection for current list item
					onListItemCheck(position);
				}
			}
		});
		
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView,
					View view, int position, long id) {
				if (mLibraryFilter != NewsFavoriteDataSource.LIBRARY_FILTER_FAVORITES) {
					return false;
				}
				onListItemCheck(position);
				return true;
			}
		});
		
		loadLibrary();
	}
	
	private void onListItemCheck(int position) {
		
		mAdapter.toggleSelection(position);
		boolean hasCheckedItems = mAdapter.getSelectedCount() > 0;

		if (hasCheckedItems && mActionMode == null) {
			// there are some selected items, start the actionMode
			mActionMode = startSupportActionMode(new ActionModeCallback());
		} else if (!hasCheckedItems && mActionMode != null) {
			// there no selected items, finish the actionMode
			mActionMode.finish();
		}
		
		if (mActionMode != null) {
			mActionMode.setTitle(getString(R.string.fav_selected_count, mAdapter.getSelectedCount()));
		}	
	}

	private void updateEmptyState() {
		boolean hasContent = mNewsList != null && !mNewsList.isEmpty();
		mListView.setVisibility(hasContent ? View.VISIBLE : View.GONE);
		mEmptyContainer.setVisibility(hasContent ? View.GONE : View.VISIBLE);
		if (!hasContent) {
			boolean isFiltered = !TextUtils.isEmpty(mSearchQuery)
					|| mLibraryFilter != NewsFavoriteDataSource.LIBRARY_FILTER_ALL;
			((TextView) findViewById(R.id.empty_title)).setText(isFiltered
					? R.string.library_no_results_title : R.string.library_empty_title);
			((TextView) findViewById(R.id.empty_subtitle)).setText(isFiltered
					? R.string.library_no_results_subtitle : R.string.library_empty_subtitle);
		}
	}

	private class ActionModeCallback implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.contextual_list_view, menu);
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			final ArrayList<String> selectedIds = new ArrayList<String>();
			Set<Long> selected = mAdapter.getSelectedNewsIds();
			for (Long selectedId : selected) {
				if (selectedId != null && selectedId > 0L) {
					selectedIds.add(String.valueOf(selectedId));
				}
			}
			mode.finish();
			executeActivityTask(new BackgroundTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(List<Void> params) {
					for (String newsId : selectedIds) {
						ZhihuApplication.getNewsFavoriteDataSource()
								.deleteFromFavorite(newsId);
					}
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					loadLibrary();
				}
			});
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			
			mAdapter.clearSelection();
			mActionMode = null;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.favorite, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_clear) {
			confirmClearFavorites();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void confirmClearFavorites() {
		new MaterialAlertDialogBuilder(this)
				.setTitle(R.string.library_clear_title)
				.setMessage(R.string.library_clear_message)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.library_clear_confirm, (dialog, which) ->
						executeActivityTask(new BackgroundTask<Void, Void, Void>() {
							@Override
							protected Void doInBackground(List<Void> params) {
								ZhihuApplication.getNewsFavoriteDataSource()
										.deleteFromFavorite();
								return null;
							}

							@Override
							protected void onPostExecute(Void result) {
								android.widget.Toast.makeText(FavoriteActivity.this,
										R.string.library_clear_done,
										android.widget.Toast.LENGTH_SHORT).show();
								loadLibrary();
							}
						}))
				.show();
	}

	private void loadLibrary() {
		mSearchHandler.removeCallbacks(mSearchRunnable);
		cancelActivityTask(mLoadTask);
		int generation = ++mLoadGeneration;
		mLoadTask = trackActivityTask(
				new LoadLibraryTask(generation, mSearchQuery, mLibraryFilter));
		mLoadTask.executeOnExecutor(BackgroundTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	protected void onDestroy() {
		mSearchHandler.removeCallbacksAndMessages(null);
		super.onDestroy();
	}

	private class LoadLibraryTask extends BackgroundTask<String, Void, ArrayList<LibraryItemEntity>> {
		private final int generation;
		private final String query;
		private final int filter;

		LoadLibraryTask(int generation, String query, int filter) {
			this.generation = generation;
			this.query = query;
			this.filter = filter;
		}

		@Override
		protected ArrayList<LibraryItemEntity> doInBackground(java.util.List<String> params) {
			return ZhihuApplication.getNewsFavoriteDataSource()
					.searchLibrary(query, filter);
		}
		
		@Override
		protected void onPostExecute(ArrayList<LibraryItemEntity> result) {
			forgetActivityTask(this);
			if (generation != mLoadGeneration || isFinishing()) {
				return;
			}
			mNewsList = toNewsList(result);
			// A query/filter change ends contextual selection. Keeping identity-based
			// selections hidden across a new result set would still be surprising.
			if (mActionMode != null) {
				mActionMode.finish();
			}
			
			if (mAdapter == null) {
				mAdapter = new NewsAdapter(mInstance, mNewsList);
				mAdapter.setFavoriteFlag(true);
				mListView.setAdapter(mAdapter);
			} else {
				mAdapter.updateData(mNewsList);
			}

			updateEmptyState();
		}
	}

	private ArrayList<NewsEntity> toNewsList(ArrayList<LibraryItemEntity> items) {
		ArrayList<NewsEntity> news = new ArrayList<NewsEntity>();
		if (items == null) {
			return news;
		}
		for (LibraryItemEntity item : items) {
			try {
				NewsEntity entity = new NewsEntity();
				entity.id = Long.parseLong(item.newsId);
				entity.title = TextUtils.isEmpty(item.title)
						? getString(R.string.library_untitled_item, item.newsId) : item.title;
				entity.share_url = item.shareUrl;
				if (!TextUtils.isEmpty(item.logo)) {
					entity.images = new ArrayList<String>();
					entity.images.add(item.logo);
				}
				news.add(entity);
			} catch (NumberFormatException ignored) {
				// Deep-link story IDs are numeric; malformed legacy rows remain stored
				// but cannot be opened by NewsDetailActivity.
			}
		}
		return news;
	}
}
