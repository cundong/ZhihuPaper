package com.cundong.izhihu.activity;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.adapter.NewsAdapter;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.task.MyAsyncTask;

/**
 * 类说明： 	收藏夹，Activity
 * 
 * @date 	2014-10-10
 * @version 1.0
 */
public class FavoriteActivity extends BaseActivity {
	
	private static final int REQUESTCODE_DETAIL = 8010;
	
	private ListView mListView;
	private NewsAdapter mAdapter = null;
	private ActionMode mActionMode;
	
	private ArrayList<NewsEntity> mNewsList = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.fragment_main);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(R.string.actionbar_title_fav);
		
		mInstance = this;
		
		mListView = (ListView) findViewById(R.id.list);
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
					
					intent.setClass(mInstance, NewsDetailActivity.class);
					startActivityForResult(intent, REQUESTCODE_DETAIL);
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
				onListItemCheck(position);
				return true;
			}
		});
		
		new LoadCacheNewsTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	private void onListItemCheck(int position) {
		
		mAdapter.toggleSelection(position);
		boolean hasCheckedItems = mAdapter.getSelectedCount() > 0;

		if (hasCheckedItems && mActionMode == null) {
			// there are some selected items, start the actionMode
			mActionMode = startActionMode(new ActionModeCallback());
		} else if (!hasCheckedItems && mActionMode != null) {
			// there no selected items, finish the actionMode
			mActionMode.finish();
		}
		
		if (mActionMode != null) {
			mActionMode.setTitle(String.valueOf(mAdapter.getSelectedCount()) + " selected");
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
			
			ArrayList<NewsEntity> tempList = new ArrayList<NewsEntity>();
			tempList.addAll(mNewsList);
			
			SparseBooleanArray selected = mAdapter.getSelectedIds();
			
			for (int i = 0; i < selected.size(); i++) {
				if (selected.valueAt(i)) {
					NewsEntity selectedItem = (NewsEntity) mAdapter.getItem(selected.keyAt(i));
					tempList.remove(selectedItem);
					ZhihuApplication.getNewsFavoriteDataSource().deleteFromFavorite(String.valueOf(selectedItem.id));
				}
			}
			
			mNewsList = tempList;
			mAdapter.updateData(mNewsList);
			
			// close action mode
			mode.finish();
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			
			mAdapter.clearSelection();
			mActionMode = null;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.favorite, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_clear:
			ZhihuApplication.getNewsFavoriteDataSource().deleteFromFavorite();
			new LoadCacheNewsTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == REQUESTCODE_DETAIL) {
			new LoadCacheNewsTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private class LoadCacheNewsTask extends MyAsyncTask<String, Void, ArrayList<NewsEntity>> {

		@Override
		protected ArrayList<NewsEntity> doInBackground(String... params) {
			
			return ZhihuApplication.getNewsFavoriteDataSource().getFavoriteList();
		}
		
		@Override
		protected void onPostExecute(ArrayList<NewsEntity> result) {
			super.onPostExecute(result);
			
			if (result != null) {

				mNewsList = result;
				
				if (mAdapter == null) {
					mAdapter = new NewsAdapter(mInstance, mNewsList);
					mAdapter.setFavoriteFlag(true);
					mListView.setAdapter(mAdapter);
				} else {
					mAdapter.updateData(mNewsList);
				}
			}
		}
	}
}