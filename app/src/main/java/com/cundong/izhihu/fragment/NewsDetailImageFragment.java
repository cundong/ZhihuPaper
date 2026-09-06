package com.cundong.izhihu.fragment;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.cundong.izhihu.R;

import java.io.File;

public class NewsDetailImageFragment extends BaseFragment {

	private ImageView mImageView;
	
	private String mImageUrl = null;
	
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_detail_image,
				container, false);
		
		mImageView = (ImageView) rootView.findViewById(R.id.imageview);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// 原实现用 BitmapFactory.decodeFile 一次性全尺寸解码，遇到知乎的大图
		// （常见 1440w）容易 OOM，且解码发生在主线程。交给 Glide 后由它按
		// 目标 View 尺寸采样、在后台线程解码，并随 Fragment 生命周期取消。
		if (mImageUrl != null && !mImageUrl.isEmpty()) {
			Glide.with(this)
					.load(new File(mImageUrl))
					.into(mImageView);
		}
	}

	@Override
	protected void onRestoreState(Bundle savedInstanceState) {
		mImageUrl = savedInstanceState.getString("imageUrl");
	}

	@Override
	protected void onSaveState(Bundle outState) {
		outState.putString("imageUrl", mImageUrl);
	}

	@Override
	protected void onFirstTimeLaunched() {
		Bundle bundle = getArguments();
		mImageUrl = bundle != null ? bundle.getString("imageUrl") : "";
	}
}
