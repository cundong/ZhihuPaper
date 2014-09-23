package com.cundong.izhihu.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.util.DialogUtils;
import com.cundong.izhihu.util.DialogUtils.DialogCallBack;
import com.cundong.izhihu.util.PhoneUtils;

@SuppressLint("NewApi")
public class PrefsFragment extends PreferenceFragment implements
		Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.prefs);

		findPreference("clear_cache").setOnPreferenceClickListener(this);
		findPreference("about").setOnPreferenceClickListener(this);
		findPreference("version").setOnPreferenceClickListener(this);
		
		findPreference("using_client?").setOnPreferenceClickListener(this);
		findPreference("using_client?").setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference.getKey().equals("clear_cache")) {

			DialogUtils.dialogBuilder(getActivity(), "清除缓存", "是否要清除缓存？",
					new DialogCallBack() {

						@Override
						public void onComplete() {
							// Crouton.makeText( getActivity(), "开始清除缓存",
							// Style.INFO).show();
							//
							// //1.删数据库
							// boolean clearCache = FileUtils.deleteFile(
							// SDCardUtils.getExternalCacheDir(getActivity()) );
							//
							// //2.删文件
							//
							// if(clearCache) {
							// Crouton.makeText( getActivity(), "清除缓存成功",
							// Style.INFO).show();
							// } else {
							// Crouton.makeText( getActivity(), "清除缓存失败",
							// Style.INFO).show();
							// }
						}

						@Override
						public void onCancel() {

						}
					});
		} else if (preference.getKey().equals("about")) {
			Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(Constants.GITGUB));
			startActivity(intent);
		} else if (preference.getKey().equals("version")) {
			showDialog();
		}

		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		
		if (preference == findPreference("using_client?")) {

			CheckBoxPreference acceptproxy_pref = (CheckBoxPreference) findPreference("using_client?");
			acceptproxy_pref.setChecked(!acceptproxy_pref.isChecked());

			return false;
		}
		
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
        sb.append(PhoneUtils.getApplicationName(getActivity())).append("\n")
        	.append("Version:" )
        	.append(PhoneUtils.getPackageInfo(getActivity()).versionName)
        	.append("\n")
        	.append("by @Cundong");
       
        textView.setText(sb.toString());

        dialog.show();
    }
}