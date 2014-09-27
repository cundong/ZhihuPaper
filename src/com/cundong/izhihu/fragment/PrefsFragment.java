package com.cundong.izhihu.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.util.PhoneUtils;

@SuppressLint("NewApi")
public class PrefsFragment extends PreferenceFragment implements
		Preference.OnPreferenceClickListener,
		Preference.OnPreferenceChangeListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.prefs);

		findPreference("about").setOnPreferenceClickListener(this);
		findPreference("version").setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference.getKey().equals("about")) {
			Intent intent = new Intent("android.intent.action.VIEW",
					Uri.parse(Constants.GITGUB_PROJECT));
			startActivity(intent);
		} else if (preference.getKey().equals("version")) {
			showDialog();
		}

		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {

		return false;
	}

	private void showDialog() {
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

		StringBuilder sb = new StringBuilder();
		sb.append(PhoneUtils.getApplicationName(getActivity())).append("<br/>")
				.append("Version:")
				.append(PhoneUtils.getPackageInfo(getActivity()).versionName)
				.append("<br/>")
				.append("by <a href='")
				.append(Constants.GITHUB_NAME)
				.append("'>@Cundong</a>");

		CharSequence charSequence = Html.fromHtml(sb.toString());

		textView.setText(charSequence);
		textView.setMovementMethod(LinkMovementMethod.getInstance());

		dialog.show();
	}
}