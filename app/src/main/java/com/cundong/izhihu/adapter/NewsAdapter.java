package com.cundong.izhihu.adapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import androidx.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import androidx.core.view.ViewCompat;
import com.cundong.izhihu.R;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.util.ListUtils;
import com.cundong.izhihu.util.NetWorkHelper;
import com.cundong.izhihu.util.ZhihuUtils;

/**
 * 类说明： 新闻列表 Adapter
 * 
 * @date 2014-9-7
 * @version 1.0
 */
public class NewsAdapter extends MultiViewTypeBaseAdapter<NewsEntity> {

	// 带图item
	private static final int TYPE_0 = 0;

	// 不带图item
	private static final int TYPE_1 = 1;

	// tag
	private static final int TYPE_2 = 2;

	private int titleColorNorId, titleReadColorId, listItemDefaultImageId;

	// 是否当前为收藏夹Adapter
	private boolean mFavoriteFalg = false;

	private RequestOptions mRequestOptions = null;

	/** Selection is keyed by story identity, never by a mutable filtered-list position. */
	private final Set<Long> mSelectedItemIds = new HashSet<Long>();

	public NewsAdapter(Context context, ArrayList<NewsEntity> list) {
		super(context, list);

		initStyle();
	}

	private void initStyle() {
		Resources.Theme theme = mContext.getTheme();
		TypedArray typedArray = null;

		SharedPreferences mPerferences = PreferenceManager.getDefaultSharedPreferences(mContext);

		if (mPerferences.getBoolean("dark_theme?", false)) {
			typedArray = theme.obtainStyledAttributes(R.style.Theme_Daily_AppTheme_Dark, new int[] { R.attr.listItemTextNorColor, R.attr.listItemTextReadColor, R.attr.listItemDefaultImage });
		} else {
			typedArray = theme.obtainStyledAttributes(R.style.Theme_Daily_AppTheme_Light, new int[] { R.attr.listItemTextNorColor, R.attr.listItemTextReadColor, R.attr.listItemDefaultImage });
		}

		titleColorNorId = typedArray.getResourceId(0, 0);
		titleReadColorId = typedArray.getResourceId(1, 0);
		listItemDefaultImageId = typedArray.getResourceId(2, 0);

		typedArray.recycle();

		// UIL 的 DisplayImageOptions 等价物。cacheInMemory/cacheOnDisk 在 Glide 中
		// 默认开启，considerExifParams 由 Glide 的 Downsampler 自动处理，因此这里
		// 只需声明占位图与磁盘缓存策略。
		mRequestOptions = new RequestOptions()
				.placeholder(listItemDefaultImageId)
				.error(listItemDefaultImageId)
				.fallback(listItemDefaultImageId)
				.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
				.centerCrop();
	}

	public void setFavoriteFlag(boolean favoriteFalg) {
		this.mFavoriteFalg = favoriteFalg;
	}

	public void toggleSelection(int position) {
		NewsEntity item = getNewsItem(position);
		if (item == null || item.id <= 0L) {
			return;
		}
		if (!mSelectedItemIds.add(item.id)) {
			mSelectedItemIds.remove(item.id);
		}
		notifyDataSetChanged();
	}

	private NewsEntity getNewsItem(int position) {
		return position >= 0 && position < mDataList.size() ? mDataList.get(position) : null;
	}

	public void clearSelection() {
		mSelectedItemIds.clear();
		notifyDataSetChanged();
	}

	public int getSelectedCount() {
		return mSelectedItemIds.size();
	}

	public Set<Long> getSelectedNewsIds() {
		return new HashSet<Long>(mSelectedItemIds);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return mFavoriteFalg;
	}

	@Override
	public boolean isEnabled(int position) {
		return getItemViewType(position) != TYPE_2;
	}

	@Override
	public int getViewTypeCount() {
		if (mFavoriteFalg) {
			return 2;
		} else {
			return 3;
		}
	}

	@Override
	public int getItemViewType(int position) {

		NewsEntity newsEntity = mDataList.get(position);

		if (newsEntity.isTag) {
			return TYPE_2;
		} else {
			if (NetWorkHelper.isMobile(mContext) && PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("noimage_nowifi?", false)) {
				return TYPE_1;
			} else {
				if (!ListUtils.isEmpty(newsEntity.images)) {
					return TYPE_0;
				} else {
					return TYPE_1;
				}
			}
		}
	}

	@Override
	public int getItemResourceId(int type) {

		switch (type) {
		case TYPE_0:
			return R.layout.list_item;
		case TYPE_1:
			return R.layout.list_item_no_image;
		case TYPE_2:
		default:
			return R.layout.list_date_item;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		int type = getItemViewType(position);

		ViewHolder holder0 = null;
		ViewHolder holder1 = null;
		ViewHolder holder2 = null;

		switch (type) {
		case TYPE_0: {

			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(getItemResourceId(type), parent, false);
				holder0 = new ViewHolder(convertView);
				convertView.setTag(holder0);
			} else {
				holder0 = (ViewHolder) convertView.getTag();
			}

			return getItemView(position, convertView, holder0, type);
		}
		case TYPE_1: {
			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(getItemResourceId(type), parent, false);
				holder1 = new ViewHolder(convertView);
				convertView.setTag(holder1);
			} else {
				holder1 = (ViewHolder) convertView.getTag();
			}

			return getItemView(position, convertView, holder1, type);
		}
		case TYPE_2: {
			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(getItemResourceId(type), parent, false);
				holder2 = new ViewHolder(convertView);
				convertView.setTag(holder2);
			} else {
				holder2 = (ViewHolder) convertView.getTag();
			}

			return getItemView(position, convertView, holder2, type);
		}
		}

		return null;
	}

	@Override
	public View getItemView(int position, View convertView, ViewHolder holder, int type) {
		final NewsEntity newsEntity = mDataList.get(position);

		switch (type) {
		case TYPE_0: {
			ImageView newsImageView = (ImageView) holder.getView(R.id.list_item_image);
			TextView newsTitleView = (TextView) holder.getView(R.id.list_item_title);

			newsTitleView.setText(newsEntity.title);
			newsTitleView.setLineSpacing(0f, 1.12f);

			if (mFavoriteFalg) {

			} else {
				newsTitleView.setTextColor(ContextCompat.getColor(mContext,
						newsEntity.is_read ? titleReadColorId : titleColorNorId));
			}
			applyReadingAccessibility(convertView, newsTitleView, newsEntity);

			newsImageView.setVisibility(View.VISIBLE);
			// 原实现用 AnimateFirstDisplayListener + FadeInBitmapDisplayer 做首次淡入，
			// 并自行维护一个永不清理的 displayedImages 列表（内存泄漏）。Glide 的
			// crossFade 过渡内置了同样的效果，且缓存命中时自动跳过动画。
			Glide.with(mContext)
					.load(newsEntity.images.get(0))
					.apply(mRequestOptions)
					.transition(DrawableTransitionOptions.withCrossFade(500))
					.into(newsImageView);

			convertView.setBackgroundColor(mSelectedItemIds.contains(newsEntity.id)
					? ContextCompat.getColor(mContext, R.color.listview_multi_sel_bg) : Color.TRANSPARENT);

			break;
		}
		case TYPE_1: {
			TextView newsTitleView = (TextView) holder.getView(R.id.list_item_title);

			newsTitleView.setText(newsEntity.title);
			newsTitleView.setLineSpacing(0f, 1.12f);

			if (mFavoriteFalg) {

			} else {
				newsTitleView.setTextColor(ContextCompat.getColor(mContext,
						newsEntity.is_read ? titleReadColorId : titleColorNorId));
			}
			applyReadingAccessibility(convertView, newsTitleView, newsEntity);

			convertView.setBackgroundColor(mSelectedItemIds.contains(newsEntity.id)
					? ContextCompat.getColor(mContext, R.color.listview_multi_sel_bg) : Color.TRANSPARENT);

			break;
		}
		case TYPE_2: {
			TextView dateView = (TextView) holder.getView(R.id.date_text);
			dateView.setText(ZhihuUtils.getDateTag(mContext, newsEntity.title));
			convertView.setClickable(false);
			convertView.setFocusable(true);
			ViewCompat.setAccessibilityHeading(dateView, true);
			break;
		}
		}

		return convertView;
	}

	private void applyReadingAccessibility(View row, TextView titleView,
			NewsEntity newsEntity) {
		// Read state is conveyed by color and an explicit semantic state. Keeping
		// full opacity avoids making already-read stories illegible in large text
		// and high contrast modes.
		titleView.setAlpha(1f);
		row.setContentDescription(newsEntity.title);
		if (mFavoriteFalg) {
			ViewCompat.setStateDescription(row, null);
			return;
		}
		String state = mContext.getString(newsEntity.is_read
				? R.string.reader_read_state : R.string.reader_unread_state);
		ViewCompat.setStateDescription(row, state);
	}
}
