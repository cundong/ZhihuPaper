package com.cundong.izhihu.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cundong.izhihu.R;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class NewsAdapter extends BaseAdapter {

	private LayoutInflater mInflater;

	private ArrayList<NewsEntity> mNewsList;

	private ImageLoader imageLoader = ImageLoader.getInstance();
    
    private DisplayImageOptions options = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.ic_launcher)
            .showImageOnFail(R.drawable.ic_launcher)
            .showImageForEmptyUri(R.drawable.ic_launcher)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .build();
    
    private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();
    
	public NewsAdapter(Context context, ArrayList<NewsEntity> newsList) {
		this.mInflater = LayoutInflater.from(context);
		initData(newsList);
	}

	public void initData(ArrayList<NewsEntity> newsList){
		mNewsList = newsList;
	}
	
	@Override
	public int getCount() {
		return mNewsList != null ? mNewsList.size() : 0;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHolder viewHolder = null;
		
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.list_item, parent, false);
			
			viewHolder = new ViewHolder();
			viewHolder.newsImageView = (ImageView) convertView.findViewById(R.id.list_item_image);
			viewHolder.newsTitleView = (TextView) convertView.findViewById(R.id.list_item_title);
			
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		final NewsEntity newsEntity = mNewsList.get(position);
		viewHolder.newsTitleView.setText(newsEntity.title);
		
		imageLoader.displayImage(newsEntity.images.get(0), viewHolder.newsImageView, options, animateFirstListener);
		
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
	
	private final static class ViewHolder {
		ImageView newsImageView;
		TextView newsTitleView;
	}
}