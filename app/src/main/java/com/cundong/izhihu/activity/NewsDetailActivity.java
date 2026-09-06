package com.cundong.izhihu.activity;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.ShareActionProvider;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.entity.LibraryItemEntity;
import com.cundong.izhihu.entity.HighlightEntity;
import com.cundong.izhihu.fragment.NewsDetailFragment;
import com.cundong.izhihu.fragment.NewsDetailFragment.OnContentLoadListener;
import com.cundong.izhihu.task.BackgroundTask;
import com.cundong.izhihu.util.ListUtils;
import com.cundong.izhihu.util.EvernoteShareUtils;
import com.cundong.izhihu.util.NewsUrlUtils;
import com.cundong.izhihu.util.ArticleHtmlRenderer;
import com.cundong.izhihu.util.LibraryInputUtils;
import com.cundong.izhihu.util.ReadingQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * 类说明： 	新闻详情页，Activity
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
public class NewsDetailActivity extends BaseActivity implements OnContentLoadListener {

	private Menu mOptionsMenu;
	private MenuItem mFavActionItem;
	private MaterialButton mPreviousButton;
	private MaterialButton mNextButton;
	private List<ArticleHtmlRenderer.Section> mSections =
			new ArrayList<ArticleHtmlRenderer.Section>();
	private ReadingQueue.Item mPreviousItem;
	private ReadingQueue.Item mNextItem;
	
	private long mNewsId = 0;
	private NewsEntity mNewsEntity = null;
	private NewsDetailEntity mNewsDetailEntity = null;
	
	private boolean isInFavorite = false;
	private FavoriteStatusGetTask mFavoriteStatusTask;
	private BackgroundTask<Void, Void, Boolean> mFavoriteMutationTask;
	private int mFavoriteGeneration;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// activity_detail.xml stacks the toolbar over the content, which is what
		// Window.FEATURE_ACTION_BAR_OVERLAY used to do under ActionBarSherlock.
		setupToolbar(R.layout.activity_detail, true);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("");
		}
		mPreviousButton = findViewById(R.id.continuous_previous);
		mNextButton = findViewById(R.id.continuous_next);
		mPreviousButton.setOnClickListener(view -> navigateToNeighbor(mPreviousItem, false));
		mNextButton.setOnClickListener(view -> navigateToNeighbor(mNextItem, true));

		if (savedInstanceState == null) {
			mPreviousItem = ReadingQueue.get(getIntent(), ReadingQueue.EXTRA_PREVIOUS);
			mNextItem = ReadingQueue.get(getIntent(), ReadingQueue.EXTRA_NEXT);
			
			/**
			 * deal such scheme: <a href="http://daily.zhihu.com/story/4115152">go</>
			 * 
			 * AndroidMainfext.xml config:
			 * <data android:scheme="http" android:host="daily.zhihu.com" android:pathPattern="/story/.*" />
			 */
			Uri data = getIntent().getData();
			mNewsId = NewsUrlUtils.extractStoryId(data == null ? null : data.toString());
			if (mNewsId == 0L) {
				mNewsId = getIntent().getLongExtra("id", 0);
				mNewsEntity = getSerializableExtra(getIntent(), "newsEntity", NewsEntity.class);
			}
		} else {
			mNewsEntity = getSerializable(savedInstanceState, "newsEntity", NewsEntity.class);
			mNewsId = savedInstanceState.getLong("newsID");
			mPreviousItem = ReadingQueue.get(savedInstanceState, ReadingQueue.EXTRA_PREVIOUS);
			mNextItem = ReadingQueue.get(savedInstanceState, ReadingQueue.EXTRA_NEXT);
		}

		if (mNewsId <= 0L) {
			Toast.makeText(this, R.string.fav_add_fail, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		updateReadingWindow(mPreviousItem, mNextItem);
		updateContinuousNavigation();
		
		if (savedInstanceState == null) {
			Bundle bundle = new Bundle();
			bundle.putLong("id", mNewsId);

			Fragment newFragment = getFragment();
			newFragment.setArguments(bundle);

			setContentFragment(newFragment);
		}

		loadFavoriteStatus();
		executeActivityTask(new MarkArticleReadTask(mNewsId));
	}

	@SuppressWarnings("deprecation")
	private static <T extends java.io.Serializable> T getSerializableExtra(
			Intent intent, String key, Class<T> type) {
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
				? intent.getSerializableExtra(key, type)
				: type.cast(intent.getSerializableExtra(key));
	}

	@SuppressWarnings("deprecation")
	private static <T extends java.io.Serializable> T getSerializable(
			Bundle bundle, String key, Class<T> type) {
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
				? bundle.getSerializable(key, type)
				: type.cast(bundle.getSerializable(key));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		updateCreateMenu();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putLong("newsID", mNewsId);
		outState.putSerializable("newsEntity", mNewsEntity);
		ReadingQueue.put(outState, ReadingQueue.EXTRA_PREVIOUS, mPreviousItem);
		ReadingQueue.put(outState, ReadingQueue.EXTRA_NEXT, mNextItem);
	}

	@Override
	protected Fragment getFragment() {
		return new NewsDetailFragment();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_item_library_action_bar) {
			loadAndShowLibraryEditor();
			return true;
		}

		if (item.getItemId() == R.id.menu_item_toc_action_bar) {
			showTableOfContents();
			return true;
		}

		if (item.getItemId() == R.id.menu_item_evernote_action_bar) {
			shareToEvernote();
			return true;
		}

		if (item.getItemId() == R.id.menu_item_fav_action_bar) {
			toggleFavorite();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		mOptionsMenu = menu;

		// Inflate your menu.
		getMenuInflater().inflate(R.menu.share_action_provider, menu);

		// Set file with share history to the provider and set the share intent.
		MenuItem shareActionItem = menu.findItem(R.id.menu_item_share_action_provider_action_bar);
		ShareActionProvider actionProvider =
				(ShareActionProvider) MenuItemCompat.getActionProvider(shareActionItem);
		if (actionProvider != null) {
			actionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
			actionProvider.setShareIntent(prepareIntent());
		}

		mFavActionItem = menu.findItem(R.id.menu_item_fav_action_bar);
		MenuItem tocItem = menu.findItem(R.id.menu_item_toc_action_bar);
		tocItem.setVisible(mSections.size() >= 2);

		if (isInFavorite) {
			mFavActionItem.setIcon(R.drawable.ic_action_favorite_filled);
			mFavActionItem.setTitle(R.string.actionbar_item_fav_cancel);
		} else {
			mFavActionItem.setIcon(R.drawable.ic_action_favorite_outline);
			mFavActionItem.setTitle(R.string.actionbar_item_fav_add);
		}

		return true;
	}
	
	private void updateCreateMenu() {
		invalidateOptionsMenu();
	}

	private void updateReadingWindow(ReadingQueue.Item fallbackPrevious,
			ReadingQueue.Item fallbackNext) {
		ReadingQueue.Window window = ReadingQueue.around(
				mNewsId, fallbackPrevious, fallbackNext);
		mPreviousItem = window.getPrevious();
		mNextItem = window.getNext();
	}

	private void updateContinuousNavigation() {
		if (mPreviousButton == null || mNextButton == null) {
			return;
		}
		mPreviousButton.setEnabled(mPreviousItem != null);
		mPreviousButton.setContentDescription(mPreviousItem == null
				? getString(R.string.continuous_first_boundary)
				: getString(R.string.continuous_previous_description,
						safeContinuousTitle(mPreviousItem)));
		mNextButton.setEnabled(mNextItem != null);
		mNextButton.setContentDescription(mNextItem == null
				? getString(R.string.continuous_last_boundary)
				: getString(R.string.continuous_next_description,
						safeContinuousTitle(mNextItem)));
	}

	private String safeContinuousTitle(ReadingQueue.Item item) {
		if (item == null) {
			return "";
		}
		return TextUtils.isEmpty(item.getTitle())
				? String.valueOf(item.getId()) : item.getTitle();
	}

	private void navigateToNeighbor(ReadingQueue.Item target, boolean movingNext) {
		if (target == null || target.getId() <= 0L || target.getId() == mNewsId) {
			return;
		}

		ReadingQueue.Item oldCurrent = currentQueueItem();
		mNewsId = target.getId();
		mNewsEntity = toNewsEntity(target);
		mNewsDetailEntity = null;
		mSections = new ArrayList<ArticleHtmlRenderer.Section>();
		isInFavorite = false;

		updateReadingWindow(movingNext ? oldCurrent : null,
				movingNext ? null : oldCurrent);
		updateContinuousNavigation();
		updateCreateMenu();

		Bundle bundle = new Bundle();
		bundle.putLong("id", mNewsId);
		Fragment fragment = getFragment();
		fragment.setArguments(bundle);
		setContentFragment(fragment);

		loadFavoriteStatus();
		executeActivityTask(new MarkArticleReadTask(mNewsId));
	}

	private ReadingQueue.Item currentQueueItem() {
		String image = mNewsDetailEntity != null ? mNewsDetailEntity.image
				: mNewsEntity == null || ListUtils.isEmpty(mNewsEntity.images)
				? null : mNewsEntity.images.get(0);
		return new ReadingQueue.Item(mNewsId, getArticleTitle(), getArticleShareUrl(), image);
	}

	private NewsEntity toNewsEntity(ReadingQueue.Item item) {
		NewsEntity entity = new NewsEntity();
		entity.id = item.getId();
		entity.title = item.getTitle();
		entity.share_url = item.getShareUrl();
		if (!TextUtils.isEmpty(item.getImage())) {
			entity.images = new ArrayList<String>();
			entity.images.add(item.getImage());
		}
		return entity;
	}

	private Intent prepareIntent() {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		String title = getArticleTitle();
		String shareText = EvernoteShareUtils.buildShareText(title, getArticleShareUrl());
		if (!TextUtils.isEmpty(title)) {
			shareIntent.putExtra(Intent.EXTRA_TITLE, title);
		}
		if (!TextUtils.isEmpty(shareText)) {
			shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
		}
		return shareIntent;
	}

	/** Opens 印象笔记 directly when available; a standard chooser is the safe fallback. */
	private void shareToEvernote() {
		Intent shareIntent = prepareIntent();
		if (TextUtils.isEmpty(shareIntent.getStringExtra(Intent.EXTRA_TEXT))) {
			Toast.makeText(this, R.string.evernote_share_unavailable, Toast.LENGTH_SHORT).show();
			return;
		}

		ResolveInfo evernoteTarget = findEvernoteShareTarget(shareIntent);
		try {
			if (evernoteTarget != null && evernoteTarget.activityInfo != null) {
				shareIntent.setPackage(evernoteTarget.activityInfo.packageName);
				startActivity(shareIntent);
			} else {
				startActivity(Intent.createChooser(shareIntent,
						getString(R.string.evernote_share_chooser)));
			}
		} catch (ActivityNotFoundException ignored) {
			startActivity(Intent.createChooser(prepareIntent(),
					getString(R.string.evernote_share_chooser)));
		}
	}

	private ResolveInfo findEvernoteShareTarget(Intent shareIntent) {
		List<ResolveInfo> targets = getPackageManager().queryIntentActivities(shareIntent, 0);
		for (ResolveInfo target : targets) {
			if (EvernoteShareUtils.isEvernoteAppLabel(target.loadLabel(getPackageManager()))) {
				return target;
			}
		}
		return null;
	}

	private String getArticleTitle() {
		return mNewsDetailEntity != null ? mNewsDetailEntity.title
				: mNewsEntity == null ? null : mNewsEntity.title;
	}

	private String getArticleShareUrl() {
		return mNewsDetailEntity != null ? mNewsDetailEntity.share_url
				: mNewsEntity == null ? null : mNewsEntity.share_url;
	}

	private void loadFavoriteStatus() {
		cancelActivityTask(mFavoriteStatusTask);
		mFavoriteStatusTask = trackActivityTask(
				new FavoriteStatusGetTask(mNewsId, ++mFavoriteGeneration));
		mFavoriteStatusTask.executeOnExecutor(BackgroundTask.THREAD_POOL_EXECUTOR);
	}

	private void toggleFavorite() {
		if (mFavoriteMutationTask != null
				&& mFavoriteMutationTask.getStatus() != BackgroundTask.Status.FINISHED) {
			return;
		}
		final long requestedNewsId = mNewsId;
		final int generation = ++mFavoriteGeneration;
		final boolean add = !isInFavorite;
		final String title = getArticleTitle();
		final String shareUrl = getArticleShareUrl();
		final String image = mNewsDetailEntity != null ? mNewsDetailEntity.image
				: mNewsEntity == null || ListUtils.isEmpty(mNewsEntity.images)
				? null : mNewsEntity.images.get(0);
		if (add && (TextUtils.isEmpty(title) || TextUtils.isEmpty(shareUrl))) {
			Toast.makeText(this, R.string.fav_add_fail, Toast.LENGTH_SHORT).show();
			return;
		}
		if (mFavActionItem != null) {
			mFavActionItem.setEnabled(false);
		}
		mFavoriteMutationTask = trackActivityTask(new BackgroundTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(List<Void> params) {
				String id = String.valueOf(requestedNewsId);
				return add
						? ZhihuApplication.getNewsFavoriteDataSource()
								.add2Favorite(id, title, image, shareUrl)
						: ZhihuApplication.getNewsFavoriteDataSource().deleteFromFavorite(id);
			}

			@Override
			protected void onPostExecute(Boolean changed) {
				forgetActivityTask(this);
				if (requestedNewsId != mNewsId || generation != mFavoriteGeneration
						|| isFinishing() || isDestroyed()) {
					return;
				}
				if (Boolean.TRUE.equals(changed)) {
					isInFavorite = add;
					Toast.makeText(NewsDetailActivity.this,
							add ? R.string.fav_add_success : R.string.fav_cancel_success,
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(NewsDetailActivity.this, R.string.fav_add_fail,
							Toast.LENGTH_SHORT).show();
				}
				updateCreateMenu();
			}
		});
		mFavoriteMutationTask.executeOnExecutor(BackgroundTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onComplete(NewsDetailEntity newsDetailEntity) {
		// A previous fragment may finish after the reader has already moved on.
		// Never let that stale response replace the active article's share/library state.
		if (newsDetailEntity == null || newsDetailEntity.id != mNewsId) {
			return;
		}
		mNewsDetailEntity = newsDetailEntity;
		updateCreateMenu();
	}

	@Override
	public void onTableOfContentsChanged(List<ArticleHtmlRenderer.Section> sections) {
		// onComplete is invoked immediately before this callback. If the detail did not match,
		// this table of contents also belongs to a fragment that is no longer active.
		if (mNewsDetailEntity == null || mNewsDetailEntity.id != mNewsId) {
			return;
		}
		mSections = sections == null
				? new ArrayList<ArticleHtmlRenderer.Section>()
				: new ArrayList<ArticleHtmlRenderer.Section>(sections);
		updateCreateMenu();
	}

	private void showTableOfContents() {
		if (mSections.size() < 2) {
			return;
		}
		CharSequence[] titles = new CharSequence[mSections.size()];
		for (int i = 0; i < mSections.size(); i++) {
			titles[i] = mSections.get(i).getTitle();
		}
		new MaterialAlertDialogBuilder(this)
				.setTitle(R.string.reader_toc_title)
				.setItems(titles, (dialog, which) -> {
					Fragment fragment = getSupportFragmentManager()
							.findFragmentById(R.id.content_container);
					if (fragment instanceof NewsDetailFragment && which >= 0
							&& which < mSections.size()) {
						((NewsDetailFragment) fragment).scrollToSection(
								mSections.get(which).getId());
					}
				})
				.show();
	}

	private void loadAndShowLibraryEditor() {
		final long requestedNewsId = mNewsId;
		executeActivityTask(new BackgroundTask<Void, Void, LibraryItemEntity>() {
			@Override
			protected LibraryItemEntity doInBackground(java.util.List<Void> params) {
				return ZhihuApplication.getNewsFavoriteDataSource()
						.getLibraryItem(String.valueOf(requestedNewsId));
			}

			@Override
			protected void onPostExecute(LibraryItemEntity result) {
				if (!isFinishing() && requestedNewsId == mNewsId) {
					showLibraryEditor(result);
				}
			}
		});
	}

	private void showLibraryEditor(LibraryItemEntity currentItem) {
		View content = LayoutInflater.from(this).inflate(R.layout.dialog_library_item, null);
		SwitchMaterial readLater = content.findViewById(R.id.library_read_later);
		EditText note = content.findViewById(R.id.library_note);
		EditText tags = content.findViewById(R.id.library_tags);
		content.findViewById(R.id.library_highlights).setOnClickListener(
				view -> loadAndShowHighlights());
		if (currentItem != null) {
			readLater.setChecked(currentItem.readLater);
			note.setText(currentItem.note);
			tags.setText(LibraryInputUtils.formatTags(currentItem.tags));
		}

		new MaterialAlertDialogBuilder(this)
				.setTitle(R.string.library_action_edit)
				.setView(content)
				.setNeutralButton(R.string.library_quote_action,
						(dialog, which) -> requestQuoteFromArticle())
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.library_save, (dialog, which) ->
						saveLibraryItem(readLater.isChecked(),
								note.getText() == null ? "" : note.getText().toString(),
								tags.getText() == null ? "" : tags.getText().toString()))
				.show();
	}

	private void loadAndShowHighlights() {
		final long requestedNewsId = mNewsId;
		executeActivityTask(new BackgroundTask<Void, Void, ArrayList<HighlightEntity>>() {
			@Override
			protected ArrayList<HighlightEntity> doInBackground(List<Void> params) {
				return ZhihuApplication.getNewsFavoriteDataSource()
						.getHighlights(String.valueOf(requestedNewsId));
			}

			@Override
			protected void onPostExecute(ArrayList<HighlightEntity> highlights) {
				if (!isFinishing() && requestedNewsId == mNewsId) {
					showHighlights(highlights);
				}
			}
		});
	}

	private void showHighlights(final ArrayList<HighlightEntity> highlights) {
		if (highlights == null || highlights.isEmpty()) {
			new MaterialAlertDialogBuilder(this)
					.setTitle(R.string.library_highlights_title)
					.setMessage(R.string.library_highlights_empty)
					.setPositiveButton(android.R.string.ok, null)
					.show();
			return;
		}
		CharSequence[] labels = new CharSequence[highlights.size()];
		for (int i = 0; i < highlights.size(); i++) {
			HighlightEntity highlight = highlights.get(i);
			labels[i] = TextUtils.isEmpty(highlight.note)
					? highlight.quote : highlight.quote + "\n— " + highlight.note;
		}
		new MaterialAlertDialogBuilder(this)
				.setTitle(R.string.library_highlights_title)
				.setItems(labels, (dialog, which) -> confirmDeleteHighlight(highlights.get(which)))
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void confirmDeleteHighlight(final HighlightEntity highlight) {
		new MaterialAlertDialogBuilder(this)
				.setMessage(highlight.quote)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.library_highlight_delete, (dialog, which) ->
						executeActivityTask(new BackgroundTask<Void, Void, Boolean>() {
							@Override
							protected Boolean doInBackground(List<Void> params) {
								return ZhihuApplication.getNewsFavoriteDataSource()
										.deleteHighlight(highlight.id);
							}

							@Override
							protected void onPostExecute(Boolean deleted) {
								if (Boolean.TRUE.equals(deleted)) {
									Toast.makeText(NewsDetailActivity.this,
											R.string.library_highlight_deleted,
											Toast.LENGTH_SHORT).show();
								}
							}
						}))
				.show();
	}

	private void saveLibraryItem(boolean readLater, String note, String tagsText) {
		final long newsId = mNewsId;
		final String title = getArticleTitle();
		final String shareUrl = getArticleShareUrl();
		final String logo = mNewsDetailEntity != null ? mNewsDetailEntity.image
				: mNewsEntity == null || ListUtils.isEmpty(mNewsEntity.images)
				? null : mNewsEntity.images.get(0);
		final String body = mNewsDetailEntity == null ? null : mNewsDetailEntity.body;
		final List<String> tags = LibraryInputUtils.parseTags(tagsText);

		executeActivityTask(new BackgroundTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(java.util.List<Void> params) {
				String id = String.valueOf(newsId);
				return ZhihuApplication.getNewsFavoriteDataSource()
						.saveLibraryItemAtomically(id, title, logo, shareUrl,
								readLater, note, tags, body);
			}

			@Override
			protected void onPostExecute(Boolean saved) {
				Toast.makeText(NewsDetailActivity.this, saved
						? R.string.library_saved : R.string.library_save_failed,
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void requestQuoteFromArticle() {
		NewsDetailFragment fragment = getDetailFragment();
		if (fragment == null) {
			Toast.makeText(this, R.string.library_quote_empty, Toast.LENGTH_SHORT).show();
			return;
		}
		fragment.requestSelectedText(selectedText -> {
			String quote = LibraryInputUtils.normalizeQuote(selectedText);
			if (TextUtils.isEmpty(quote)) {
				Toast.makeText(this, R.string.library_quote_empty, Toast.LENGTH_SHORT).show();
				return;
			}
			showQuoteEditor(quote);
		});
	}

	private void showQuoteEditor(final String quote) {
		View content = LayoutInflater.from(this).inflate(R.layout.dialog_library_quote, null);
		((TextView) content.findViewById(R.id.library_quote)).setText(quote);
		EditText note = content.findViewById(R.id.library_quote_note);
		new MaterialAlertDialogBuilder(this)
				.setTitle(R.string.library_quote_action)
				.setView(content)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.library_save, (dialog, which) ->
						saveQuote(quote, note.getText() == null
								? "" : note.getText().toString()))
				.show();
	}

	private void saveQuote(final String quote, final String note) {
		final long newsId = mNewsId;
		final String title = getArticleTitle();
		final String shareUrl = getArticleShareUrl();
		final String image = mNewsDetailEntity == null ? null : mNewsDetailEntity.image;
		executeActivityTask(new BackgroundTask<Void, Void, Long>() {
			@Override
			protected Long doInBackground(java.util.List<Void> params) {
				String id = String.valueOf(newsId);
				ZhihuApplication.getNewsFavoriteDataSource().ensureItem(
						id, title, image, shareUrl);
				return ZhihuApplication.getNewsFavoriteDataSource()
						.addHighlight(id, quote, note, -1, -1);
			}

			@Override
			protected void onPostExecute(Long highlightId) {
				Toast.makeText(NewsDetailActivity.this,
						highlightId != null && highlightId > 0
								? R.string.library_quote_saved : R.string.library_save_failed,
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	private NewsDetailFragment getDetailFragment() {
		Fragment fragment = getSupportFragmentManager()
				.findFragmentById(R.id.content_container);
		return fragment instanceof NewsDetailFragment ? (NewsDetailFragment) fragment : null;
	}
	
	//获取当前新闻是否已被收藏过
	private class FavoriteStatusGetTask extends BackgroundTask<Void, Void, Boolean> {
		private final long requestedNewsId;
		private final int generation;

		FavoriteStatusGetTask(long requestedNewsId, int generation) {
			this.requestedNewsId = requestedNewsId;
			this.generation = generation;
		}

		@Override
		protected Boolean doInBackground(java.util.List<Void> params) {

			return ZhihuApplication.getNewsFavoriteDataSource()
					.isInFavorite(String.valueOf(requestedNewsId));
		}

		@Override
		protected void onPostExecute(Boolean result) {
			forgetActivityTask(this);
			if (requestedNewsId == mNewsId && generation == mFavoriteGeneration
					&& !isFinishing() && !isDestroyed()) {
				isInFavorite = result;
				updateCreateMenu();
			}
		}
	}

	private static class MarkArticleReadTask extends BackgroundTask<Void, Void, Boolean> {
		private final long newsId;

		MarkArticleReadTask(long newsId) {
			this.newsId = newsId;
		}

		@Override
		protected Boolean doInBackground(java.util.List<Void> params) {
			return ZhihuApplication.getNewsReadDataSource().readNews(String.valueOf(newsId));
		}
	}
}
