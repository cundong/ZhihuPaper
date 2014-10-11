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
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cundong.izhihu.R;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.util.NetWorkHelper;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

/**
 * 类说明： 	新闻列表 Adapter
 * 
 * @date 	2014-9-7
 * @version 1.0
 */
public class NewsAdapter extends SimpleBaseAdapter<NewsEntity> {

	private ImageLoader mImageLoader = ImageLoader.getInstance();

	private ImageLoadingListener mAnimateFirstListener = new AnimateFirstDisplayListener();

	private int titleColorNorId, titleReadColorId, listItemDefaultImageId;
	
	//用于存储日期
	private String mDate = "";
	
	private DisplayImageOptions mOptions = null;
	
	public NewsAdapter(Context context, ArrayList<NewsEntity> list) {
		super(context, list);
		
		initStyle();
	}

	public NewsAdapter(Context context, ArrayList<NewsEntity> list, String date) {
		super(context, list);
		this.mDate = date;
		
		initStyle();
	}
	
	private void initStyle() {
		Resources.Theme theme = mContext.getTheme();
		TypedArray typedArray = null;
		
		SharedPreferences mPerferences = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		
		if (mPerferences.getBoolean("dark_theme?", false)) {
			typedArray = theme.obtainStyledAttributes(R.style.Theme_Daily_AppTheme_Dark, 
					new int[] { R.attr.listItemTextNorColor, R.attr.listItemTextReadColor, R.attr.listItemDefaultImage });
		} else {
			typedArray = theme.obtainStyledAttributes(R.style.Theme_Daily_AppTheme_Light, 
					new int[] { R.attr.listItemTextNorColor, R.attr.listItemTextReadColor, R.attr.listItemDefaultImage }); 
		}
		
		titleColorNorId = typedArray.getResourceId(0, 0);
		titleReadColorId = typedArray.getResourceId(1, 0);
		listItemDefaultImageId = typedArray.getResourceId(2, 0);
		
		typedArray.recycle();
		
		mOptions = new DisplayImageOptions.Builder()
			.showImageOnLoading( mContext.getResources().getDrawable(listItemDefaultImageId) )
			.showImageOnFail( mContext.getResources().getDrawable(listItemDefaultImageId) )
			.showImageForEmptyUri( mContext.getResources().getDrawable(listItemDefaultImageId) )
			.cacheInMemory(true)
			.cacheOnDisk(true)
			.considerExifParams(true)
			.build();
	}
	
	public void updateData(ArrayList<NewsEntity> newsList) {
		mDataList = newsList;
		this.notifyDataSetChanged();
	}
	
	@Override
	public int getItemResourceId() {
		return R.layout.list_item;
	}

	@Override
	public View getItemView(int position, View convertView, ViewHolder holder) {

		ImageView newsImageView = (ImageView) holder.getView(R.id.list_item_image);
		TextView newsTitleView = (TextView) holder.getView(R.id.list_item_title);

		final NewsEntity newsEntity = mDataList.get(position);
		newsTitleView.setText( newsEntity.title );
		
		newsTitleView.setTextColor( newsEntity.is_read ? mContext.getResources().getColor(titleReadColorId) : mContext.getResources().getColor(titleColorNorId) );
		
		if (NetWorkHelper.isMobile(mContext) && PreferenceManager.getDefaultSharedPreferences(
				mContext).getBoolean("noimage_nowifi?", false) ) {
			newsImageView.setVisibility(View.GONE);
		} else {
			if (newsEntity.images != null && newsEntity.images.size() >= 1) {
				
				newsImageView.setVisibility(View.VISIBLE);
				mImageLoader.displayImage(newsEntity.images.get(0), newsImageView,
						mOptions, mAnimateFirstListener);
			} else {
				newsImageView.setVisibility(View.GONE);
			}
		}
		
		return convertView;
	}
	
	private static class AnimateFirstDisplayListener extends
			SimpleImageLoadingListener {

		static final List<String> displayedImages = Collections
				.synchronizedList(new LinkedList<String>());

		@Override
		public void onLoadingComplete(String imageUri, View view,
				Bitmap loadedImage) {
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