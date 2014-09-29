package com.cundong.izhihu.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
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

public class NewsAdapter extends SimpleBaseAdapter<NewsEntity> {

	private ImageLoader mImageLoader = ImageLoader.getInstance();

	private ImageLoadingListener mAnimateFirstListener = new AnimateFirstDisplayListener();

	private DisplayImageOptions mOptions = new DisplayImageOptions.Builder()
			.showImageOnLoading(R.drawable.ic_launcher)
			.showImageOnFail(R.drawable.ic_launcher)
			.showImageForEmptyUri(R.drawable.ic_launcher).cacheInMemory(true)
			.cacheOnDisk(true).considerExifParams(true).build();
	
	public NewsAdapter(Context context, ArrayList<NewsEntity> list) {
		super(context, list);
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

		ImageView newsImageView = (ImageView) holder
				.getView(R.id.list_item_image);
		TextView newsTitleView = (TextView) holder
				.getView(R.id.list_item_title);

		final NewsEntity newsEntity = mDataList.get(position);
		newsTitleView.setText(newsEntity.title);

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