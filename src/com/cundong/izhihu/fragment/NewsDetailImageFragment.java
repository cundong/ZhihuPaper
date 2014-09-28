package com.cundong.izhihu.fragment;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cundong.izhihu.R;

public class NewsDetailImageFragment extends BaseFragment {

	private static final String IMAGE_URL = "com.cundong.izhihu.fragment.NewsDetailImageFragment.imageUrl";
	
	private ImageView mImageView;
	
	private String mImageUrl = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState == null) {
			Bundle bundle = getArguments();
			mImageUrl = bundle != null ? bundle.getString("imageUrl") : "";
		} else {
			mImageUrl = savedInstanceState.getString(IMAGE_URL);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(IMAGE_URL, mImageUrl);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_detail_image,
				container, false);
		
		mPullToRefreshLayout = (PullToRefreshLayout) rootView.findViewById(R.id.ptr_layout);
		
		mImageView = (ImageView) rootView.findViewById(R.id.imageview);

		return rootView;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);
		
		Bitmap bitmap = BitmapFactory.decodeFile(mImageUrl);
		
		if(bitmap!=null) {
			mImageView.setImageBitmap(bitmap);
		}
	}
}
