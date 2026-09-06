package com.cundong.izhihu.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;

import com.cundong.izhihu.R;
import com.cundong.izhihu.fragment.NewsDetailImageFragment;
import com.cundong.izhihu.task.ImageToGalleryTask;
import com.cundong.izhihu.task.BackgroundTask;
import com.cundong.izhihu.util.SnackbarUtils;

/**
 * 类说明： 	新闻详情页中图片，点击后展示Activity
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
public class NewsDetailImageActivity extends BaseActivity {

	private String mImageUrl = null;
	private ImageToGalleryTask mSaveTask;
	private boolean permissionRequestPending;
	private final ActivityResultLauncher<String> storagePermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
				permissionRequestPending = false;
				if (granted) {
					saveImage();
				} else {
					SnackbarUtils.showError(this, R.string.image_save_permission_denied);
				}
			});

	@Override
	protected int resolveThemeResId(boolean darkTheme) {
		return darkTheme
				? R.style.Theme_Daily_ImagePreview_Dark
				: R.style.Theme_Daily_ImagePreview_Light;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		permissionRequestPending = savedInstanceState != null
				&& savedInstanceState.getBoolean("storagePermissionPending");

		setupToolbar(R.layout.activity_detail_image, true);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(R.string.detail_image_title);
		}
		
		if (savedInstanceState == null) {
			mImageUrl = getIntent().getStringExtra("imageUrl");
		} else {
			mImageUrl = savedInstanceState.getString("imageUrl");
		}

		if (savedInstanceState == null) {
			Bundle bundle = new Bundle();
			bundle.putString("imageUrl", mImageUrl);

			Fragment newFragment = getFragment();
			newFragment.setArguments(bundle);

			setContentFragment(newFragment);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("imageUrl", mImageUrl);
		outState.putBoolean("storagePermissionPending", permissionRequestPending);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected Fragment getFragment() {
		return new NewsDetailImageFragment();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.detail_image, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_first) {

			requestImageSave();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void requestImageSave() {
		if (permissionRequestPending || (mSaveTask != null
				&& mSaveTask.getStatus() != BackgroundTask.Status.FINISHED)) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				&& Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
				&& ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
						!= PackageManager.PERMISSION_GRANTED) {
			if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				new MaterialAlertDialogBuilder(this)
						.setMessage(R.string.image_save_permission_rationale)
						.setPositiveButton(android.R.string.ok, (dialog, which) -> requestStoragePermission())
						.setNegativeButton(android.R.string.cancel, null)
						.show();
			} else {
				requestStoragePermission();
			}
			return;
		}
		saveImage();
	}

	private void requestStoragePermission() {
		if (!permissionRequestPending) {
			permissionRequestPending = true;
			storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}
	}

	private void saveImage() {

				if (mSaveTask == null
						|| mSaveTask.getStatus() == BackgroundTask.Status.FINISHED) {
					mSaveTask = trackActivityTask(new ImageToGalleryTask(this,
							new ImageToGalleryTask.Callback() {
								@Override
								public void onSaveStarted() {
									SnackbarUtils.show(NewsDetailImageActivity.this,
											R.string.image_save_doing);
								}

								@Override
								public void onSaveFinished(boolean success) {
									if (success) {
										SnackbarUtils.show(NewsDetailImageActivity.this,
												R.string.image_save_done);
									} else {
										SnackbarUtils.showError(NewsDetailImageActivity.this,
												R.string.image_save_fail);
									}
								}
							}));
					mSaveTask.executeOnExecutor(BackgroundTask.THREAD_POOL_EXECUTOR, mImageUrl);
				}

	}

	@Override
	protected void onDestroy() {
		if (mSaveTask != null) {
			mSaveTask.clearCallback();
		}
		super.onDestroy();
	}
}
