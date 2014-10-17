package com.cundong.izhihu.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.util.PhoneUtils;

/**
 * 类说明： 	用于Android4.0+的设置页Fragment
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
@SuppressLint("NewApi")
public class PrefsFragment extends PreferenceFragment implements
		Preference.OnPreferenceClickListener,
		Preference.OnPreferenceChangeListener {
	
	private static final String PREFERENCES_ABOUT = "about";
	private static final String PREFERENCE_VERSION = "version";
	private static final String PREFERENCE_NOIMAGE_NOWIFI = "noimage_nowifi?";
	private static final String PREFERENCE_DARK_THEME = "dark_theme?";
	
	private OnPreChangeListener mListener = null;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnPreChangeListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnPreChangeListener");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.prefs);

		findPreference(PREFERENCES_ABOUT).setOnPreferenceClickListener(this);
		findPreference(PREFERENCE_VERSION).setOnPreferenceClickListener(this);
		findPreference(PREFERENCE_NOIMAGE_NOWIFI).setOnPreferenceChangeListener(this);
		findPreference(PREFERENCE_DARK_THEME).setOnPreferenceChangeListener(this);
		
		boolean noImgnoWifi = PreferenceManager.getDefaultSharedPreferences(
				getActivity()).getBoolean(PREFERENCE_NOIMAGE_NOWIFI, false);
		
		findPreference(PREFERENCE_NOIMAGE_NOWIFI).setDefaultValue(noImgnoWifi);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference.getKey().equals(PREFERENCES_ABOUT)) {
			showDialog(false);
		} else if (preference.getKey().equals(PREFERENCE_VERSION)) {
			showDialog(true);
		}

		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(PREFERENCE_NOIMAGE_NOWIFI)) {
			
			if (newValue instanceof Boolean) {
				Boolean boolVal = (Boolean) newValue;

				SharedPreferences mPerferences = PreferenceManager
						.getDefaultSharedPreferences(getActivity());
				
				SharedPreferences.Editor mEditor = mPerferences.edit();
				mEditor.putBoolean(PREFERENCE_NOIMAGE_NOWIFI, boolVal);
				mEditor.commit();
			}

			return true;
		} else if (preference.getKey().equals(PREFERENCE_DARK_THEME)) {
			
			if (newValue instanceof Boolean) {
				Boolean boolVal = (Boolean) newValue;

				SharedPreferences mPerferences = PreferenceManager
						.getDefaultSharedPreferences(getActivity());
				
				SharedPreferences.Editor mEditor = mPerferences.edit();
				mEditor.putBoolean(PREFERENCE_DARK_THEME, boolVal);
				mEditor.commit();
				
				mListener.onChanged(boolVal);
			}
			
			return true;
		}

		return false;
	}
	
	private void showDialog( boolean isVersion ) {
		final Dialog dialog = new Dialog(getActivity());
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.setContentView(R.layout.dialog_version);

		TextView textView = (TextView) dialog.findViewById(R.id.dialog_text);
		textView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				dialog.dismiss();
			}
		});
		
		if (isVersion) {
			String data = getResources().getString(R.string.setting_aboutme_version);
			
			data = String.format(data,
					PhoneUtils.getApplicationName(getActivity()),
					PhoneUtils.getPackageInfo(getActivity()).versionName);
			
			textView.setText(data);
		} else {
			
			String title = new StringBuilder().append(PhoneUtils.getApplicationName(getActivity()) ).append("<br/>").toString();
			String subTitle = new StringBuilder().append(getResources().getString(R.string.app_sub_name)).append("<br/>").toString();
			String author = new StringBuilder().append("@").append(getResources().getString(R.string.app_author)).toString();
			
			String githubUrl = new StringBuilder().append("<a href='")
					.append(Constants.GITGUB_PROJECT)
					.append("'>")
					.append(Constants.GITGUB_PROJECT)
					.append("</a>")
					.append("<br/>")
					.toString();

			String data = getResources().getString(R.string.setting_aboutme_text);
			
			data = String.format(data, 
					title,
					subTitle, 
					githubUrl, 
					author);

			CharSequence charSequence = Html.fromHtml(data);

			textView.setText(charSequence);
		}

		textView.setMovementMethod(LinkMovementMethod.getInstance());

		dialog.show();
	}
	
	public static interface OnPreChangeListener {
		public void onChanged( boolean result );
	}
}