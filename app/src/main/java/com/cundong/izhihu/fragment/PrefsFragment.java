package com.cundong.izhihu.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.task.BackgroundTask;
import com.cundong.izhihu.task.OfflineSyncScheduler;
import com.cundong.izhihu.util.AssetsUtils;
import com.cundong.izhihu.util.OfflineStorageUtils;
import com.cundong.izhihu.util.PhoneUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;

/**
 * 类说明： 	设置页Fragment
 *
 * Migrated from the framework android.preference.PreferenceFragment (deprecated
 * in API 29) to androidx.preference.PreferenceFragmentCompat, which themes
 * itself from the Material 3 app theme.
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
public class PrefsFragment extends PreferenceFragmentCompat implements
		Preference.OnPreferenceClickListener,
		Preference.OnPreferenceChangeListener {
	
	private static final String PREFERENCES_ABOUT = "about";
	private static final String PREFERENCE_VERSION = "version";
	private static final String PREFERENCE_NOIMAGE_NOWIFI = "noimage_nowifi?";
	private static final String PREFERENCE_DARK_THEME = "dark_theme?";
	private static final String PREFERENCE_OFFLINE_STORAGE = "offline_storage";
	private static final String PREFERENCE_PRIVACY = "privacy";
	private static final String PREFERENCE_LICENSES = "licenses";
	
	private OnPreChangeListener mListener = null;
	
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		try {
			mListener = (OnPreChangeListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString()
					+ " must implement OnPreChangeListener");
		}
	}

	@Override
	public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
		setPreferencesFromResource(R.xml.prefs, rootKey);

		findPreference(PREFERENCES_ABOUT).setOnPreferenceClickListener(this);
		findPreference(PREFERENCE_VERSION).setOnPreferenceClickListener(this);
		findPreference(PREFERENCE_NOIMAGE_NOWIFI).setOnPreferenceChangeListener(this);
		findPreference(PREFERENCE_DARK_THEME).setOnPreferenceChangeListener(this);
		findPreference(PREFERENCE_OFFLINE_STORAGE).setOnPreferenceClickListener(this);
		findPreference(PREFERENCE_PRIVACY).setOnPreferenceClickListener(this);
		findPreference(PREFERENCE_LICENSES).setOnPreferenceClickListener(this);
		refreshOfflineStorageSummary();
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference.getKey().equals(PREFERENCES_ABOUT)) {
			showDialog(false);
		} else if (preference.getKey().equals(PREFERENCE_VERSION)) {
			showDialog(true);
		} else if (preference.getKey().equals(PREFERENCE_OFFLINE_STORAGE)) {
			showOfflineStorageDialog();
		} else if (preference.getKey().equals(PREFERENCE_PRIVACY)) {
			showAssetDocument("docs/privacy.txt", R.string.setting_privacy_title);
		} else if (preference.getKey().equals(PREFERENCE_LICENSES)) {
			showAssetDocument("docs/third_party_notices.txt", R.string.setting_licenses_title);
		}

		return false;
	}

	private void refreshOfflineStorageSummary() {
		Preference preference = findPreference(PREFERENCE_OFFLINE_STORAGE);
		if (preference != null) {
			preference.setSummary(getString(R.string.setting_offline_storage_size,
					OfflineStorageUtils.formatBytes(
							OfflineStorageUtils.getOfflineBytes(requireContext()))));
		}
	}

	private void showOfflineStorageDialog() {
		String size = OfflineStorageUtils.formatBytes(
				OfflineStorageUtils.getOfflineBytes(requireContext()));
		new MaterialAlertDialogBuilder(requireContext())
				.setTitle(R.string.setting_offline_storage_title)
				.setMessage(getString(R.string.setting_offline_storage_dialog, size))
				.setNegativeButton(R.string.dialog_close, null)
				.setPositiveButton(R.string.setting_offline_storage_clear, (dialog, which) -> {
					Context applicationContext = requireContext().getApplicationContext();
					new ClearOfflineContentTask(applicationContext, this)
							.executeOnExecutor(BackgroundTask.THREAD_POOL_EXECUTOR);
				})
				.show();
	}

	private void showAssetDocument(String assetPath, int titleRes) {
		String content = AssetsUtils.loadText(requireContext(), assetPath);
		if (content == null || content.trim().length() == 0) {
			content = getString(R.string.setting_document_unavailable);
		}
		new MaterialAlertDialogBuilder(requireContext())
				.setTitle(titleRes)
				.setMessage(content)
				.setPositiveButton(R.string.dialog_close, null)
				.show();
	}

	private static final class ClearOfflineContentTask
			extends BackgroundTask<Void, Void, Boolean> {

		private final Context applicationContext;
		private final WeakReference<PrefsFragment> fragmentReference;

		ClearOfflineContentTask(Context applicationContext, PrefsFragment fragment) {
			this.applicationContext = applicationContext;
			fragmentReference = new WeakReference<PrefsFragment>(fragment);
		}

		@Override
		protected Boolean doInBackground(java.util.List<Void> params) {
			if (!OfflineSyncScheduler.cancelAndAwait(applicationContext)) {
				return false;
			}
			try {
				return OfflineStorageUtils.clearOfflineContent(applicationContext);
			} catch (RuntimeException e) {
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			PrefsFragment fragment = fragmentReference.get();
			if (fragment != null && fragment.isAdded()) {
				fragment.refreshOfflineStorageSummary();
			}
			android.widget.Toast.makeText(applicationContext,
					Boolean.TRUE.equals(result)
							? R.string.setting_offline_storage_cleared
							: R.string.setting_offline_storage_clear_failed,
					android.widget.Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(PREFERENCE_NOIMAGE_NOWIFI)) {
			
			if (newValue instanceof Boolean) {
				Boolean boolVal = (Boolean) newValue;

				SharedPreferences preferences = PreferenceManager
						.getDefaultSharedPreferences(requireContext());

				preferences.edit().putBoolean(PREFERENCE_NOIMAGE_NOWIFI, boolVal).apply();
			}

			return true;
		} else if (preference.getKey().equals(PREFERENCE_DARK_THEME)) {
			
			if (newValue instanceof Boolean) {
				Boolean boolVal = (Boolean) newValue;

				SharedPreferences preferences = PreferenceManager
						.getDefaultSharedPreferences(requireContext());

				preferences.edit().putBoolean(PREFERENCE_DARK_THEME, boolVal).apply();

				if (mListener != null) {
					mListener.onChanged(boolVal);
				}
			}
			
			return true;
		}

		return false;
	}
	
	private void showDialog(boolean isVersion) {
		View content = LayoutInflater.from(requireContext())
				.inflate(R.layout.dialog_version, null, false);

		TextView textView = content.findViewById(R.id.dialog_text);

		if (isVersion) {
			String data = getString(R.string.setting_aboutme_version);
			
			data = String.format(data,
					PhoneUtils.getApplicationName(requireContext()),
					PhoneUtils.getPackageInfo(requireContext()).versionName);
			
			textView.setText(data);
		} else {
			
			String title = PhoneUtils.getApplicationName(requireContext()) + "<br/>";
			String subTitle = getString(R.string.app_sub_name) + "<br/>";
			String author = "@" + getString(R.string.app_author);
			
			String githubUrl = "<a href='" + Constants.GITGUB_PROJECT + "'>"
					+ Constants.GITGUB_PROJECT + "</a><br/>";

			String data = getString(R.string.setting_aboutme_text);
			
			data = String.format(data, 
					title,
					subTitle, 
					githubUrl, 
					author);

			textView.setText(HtmlCompat.fromHtml(data, HtmlCompat.FROM_HTML_MODE_LEGACY));
		}

		textView.setMovementMethod(LinkMovementMethod.getInstance());

		new MaterialAlertDialogBuilder(requireContext())
				.setView(content)
				.setPositiveButton(R.string.dialog_close, null)
				.show();
	}
	
	public interface OnPreChangeListener {
		void onChanged(boolean result);
	}
}
