package com.cundong.izhihu.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cundong.izhihu.R;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.util.ListUtils;
import com.cundong.izhihu.util.NetWorkHelper;
import com.cundong.izhihu.util.ZhihuUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

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

	private ImageLoader mImageLoader = ImageLoader.getInstance();

	private ImageLoadingListener mAnimateFirstListener = new AnimateFirstDisplayListener();

	private int titleColorNorId, titleReadColorId, listItemDefaultImageId;

	// 是否当前为收藏夹Adapter
	private boolean mFavoriteFalg = false;

	private DisplayImageOptions mOptions = null;

	private SparseBooleanArray mSelectedItemsIds = null;

	public NewsAdapter(Context context, ArrayList<NewsEntity> list) {
		super(context, list);

		this.mSelectedItemsIds = new SparseBooleanArray();

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

		mOptions = new DisplayImageOptions.Builder().showImageOnLoading(mContext.getResources().getDrawable(listItemDefaultImageId)).showImageOnFail(mContext.getResources().getDrawable(listItemDefaultImageId)).showImageForEmptyUri(mContext.getResources().getDrawable(listItemDefaultImageId)).cacheInMemory(true).cacheOnDisk(true).considerExifParams(true).build();
	}

	public void setFavoriteFlag(boolean favoriteFalg) {
		this.mFavoriteFalg = favoriteFalg;
	}

	public void toggleSelection(int position) {
		selectView(position, !mSelectedItemsIds.get(position));
	}

	public void selectView(int position, boolean noSelect) {
		if (noSelect)
			mSelectedItemsIds.put(position, true);
		else
			mSelectedItemsIds.delete(position);

		notifyDataSetChanged();
	}

	public void clearSelection() {
		mSelectedItemsIds = new SparseBooleanArray();
		notifyDataSetChanged();
	}

	public int getSelectedCount() {
		return mSelectedItemsIds.size();
	}

	public SparseBooleanArray getSelectedIds() {
		return mSelectedItemsIds;
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

			if (mFavoriteFalg) {

			} else {
				newsTitleView.setTextColor(newsEntity.is_read ? mContext.getResources().getColor(titleReadColorId) : mContext.getResources().getColor(titleColorNorId));
			}

			newsImageView.setVisibility(View.VISIBLE);
			mImageLoader.displayImage(newsEntity.images.get(0), newsImageView, mOptions, mAnimateFirstListener);

			convertView.setBackgroundColor(mSelectedItemsIds.get(position) ? mContext.getResources().getColor(R.color.listview_multi_sel_bg) : Color.TRANSPARENT);

			break;
		}
		case TYPE_1: {
			TextView newsTitleView = (TextView) holder.getView(R.id.list_item_title);

			newsTitleView.setText(newsEntity.title);

			if (mFavoriteFalg) {

			} else {
				newsTitleView.setTextColor(newsEntity.is_read ? mContext.getResources().getColor(titleReadColorId) : mContext.getResources().getColor(titleColorNorId));
			}

			convertView.setBackgroundColor(mSelectedItemsIds.get(position) ? mContext.getResources().getColor(R.color.listview_multi_sel_bg) : Color.TRANSPARENT);

			break;
		}
		case TYPE_2: {
			TextView dateView = (TextView) holder.getView(R.id.date_text);
			dateView.setText(ZhihuUtils.getDateTag(mContext, newsEntity.title));
			break;
		}
		}

		return convertView;
	}

	private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener {

		static final List<String> displayedImages = Collections.synchronizedList(new LinkedList<String>());

		@Override
		public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
			if (loadedImage != null) {
				ImageView imageView = (ImageView) view;
				boolean firstDisplay = !displayedImages.contains(imageUri);
				if (firstDisplay) {
					FadeInBitmapDisplayer.animate(imageView, 500);
					displayedImages.add(imageUri);
				}
			}
		}
	}
}