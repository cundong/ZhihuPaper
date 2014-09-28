package com.cundong.izhihu.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.R;
import com.cundong.izhihu.fragment.NewsDetailImageFragment;

public class NewsDetailImageActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Bundle bundle = new Bundle();
		bundle.putString("imageUrl", getIntent().getStringExtra("imageUrl"));

		// Add the Sample Fragment if there is one
		Fragment newFragment = getFragment();
		newFragment.setArguments(bundle);

		if (newFragment != null) {
			getSupportFragmentManager().beginTransaction()
					.replace(android.R.id.content, newFragment).commit();
		}
	}

	@Override
	protected Fragment getFragment() {
		return new NewsDetailImageFragment();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.detail_image, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_first:
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}