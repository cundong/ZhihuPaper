package com.cundong.izhihu.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.util.PhoneUtils;

/**
 * 类说明： 	用于兼容Android2.3的设置页
 * 
 * by:http://stackoverflow.com/questions/10186697/preferenceactivity-android-4-0-and-earlier
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
@SuppressWarnings("deprecation")
public class PrefsActivity extends SherlockPreferenceActivity implements
		OnPreferenceClickListener, OnPreferenceChangeListener {

	private static final String PREFERENCES_ABOUT = "about";
	private static final String PREFERENCE_VERSION = "version";
	private static final String PREFERENCE_NOIMAGE_NOWIFI = "noimage_nowifi?";
	private static final String PREFERENCE_DARK_THEME = "dark_theme?";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		SharedPreferences mPerferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean isDarkTheme = mPerferences.getBoolean("dark_theme?", false); 
		
		if (isDarkTheme) {
			setTheme(R.style.Theme_Daily_AppTheme_Dark);
		} else {
			setTheme(R.style.Theme_Daily_AppTheme_Light);
		}
		
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.prefs);

		findPreference(PREFERENCES_ABOUT).setOnPreferenceClickListener(this);
		findPreference(PREFERENCE_VERSION).setOnPreferenceClickListener(this);
		findPreference(PREFERENCE_NOIMAGE_NOWIFI)
				.setOnPreferenceChangeListener(this);
		findPreference(PREFERENCE_DARK_THEME)
				.setOnPreferenceChangeListener(this);
		
		boolean noImgnoWifi = PreferenceManager.getDefaultSharedPreferences(
				this).getBoolean(PREFERENCE_NOIMAGE_NOWIFI, false);

		findPreference(PREFERENCE_NOIMAGE_NOWIFI).setDefaultValue(noImgnoWifi);
		
		boolean darkTheme = PreferenceManager.getDefaultSharedPreferences(
				this).getBoolean(PREFERENCE_DARK_THEME, false);
		
		findPreference(PREFERENCE_DARK_THEME).setDefaultValue(darkTheme);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference.getKey().equals(PREFERENCES_ABOUT)) {
			Intent intent = new Intent("android.intent.action.VIEW",
					Uri.parse(Constants.GITGUB_PROJECT));
			startActivity(intent);
		} else if (preference.getKey().equals(PREFERENCE_VERSION)) {
			showDialog();
		}

		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(PREFERENCE_NOIMAGE_NOWIFI)) {
			
			if (newValue instanceof Boolean) {
				Boolean boolVal = (Boolean) newValue;

				SharedPreferences mPerferences = PreferenceManager
						.getDefaultSharedPreferences(this);
				
				SharedPreferences.Editor mEditor = mPerferences.edit();
				mEditor.putBoolean(PREFERENCE_NOIMAGE_NOWIFI, boolVal);
				mEditor.commit();
			}

			return true;
		} else if (preference.getKey().equals(PREFERENCE_DARK_THEME)) {
			
			if (newValue instanceof Boolean) {
				Boolean boolVal = (Boolean) newValue;

				SharedPreferences mPerferences = PreferenceManager
						.getDefaultSharedPreferences(this);
				
				SharedPreferences.Editor mEditor = mPerferences.edit();
				mEditor.putBoolean(PREFERENCE_DARK_THEME, boolVal);
				mEditor.commit();
				
				recreateActivity();
			}
			
			return true;
		}

		return false;
	}
	
	private void showDialog() {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.setContentView(R.layout.dialog_version);

		TextView textView = (TextView) dialog.findViewById(R.id.dialog_text);
		textView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				dialog.dismiss();
			}
		});

		StringBuilder sb = new StringBuilder();
		sb.append(PhoneUtils.getApplicationName(this)).append("<br/>")
				.append("Version:")
				.append(PhoneUtils.getPackageInfo(this).versionName)
				.append("<br/>").append("by <a href='")
				.append(Constants.GITHUB_NAME).append("'>@Cundong</a>");

		CharSequence charSequence = Html.fromHtml(sb.toString());

		textView.setText(charSequence);
		textView.setMovementMethod(LinkMovementMethod.getInstance());

		dialog.show();
	}
	
	@SuppressLint("NewApi")
	public void recreateActivity() {

		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
					finish();
					startActivity(getIntent());
				} else {
					recreate();
				}
			}
		}, 1);
	}
}