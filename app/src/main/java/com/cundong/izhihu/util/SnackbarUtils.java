package com.cundong.izhihu.util;

import android.app.Activity;
import android.view.View;

import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;

/**
 * Small helper around {@link Snackbar}.
 *
 * Replaces the Crouton library (unmaintained since 2015, and it attached its
 * own view to the ActionBarSherlock-era window decor). Material 3 Snackbars
 * are anchored to the activity content view instead.
 */
public final class SnackbarUtils {

	private SnackbarUtils() {
	}

	public static void show(Activity activity, @StringRes int messageRes) {
		make(activity, messageRes, false);
	}

	public static void showError(Activity activity, @StringRes int messageRes) {
		make(activity, messageRes, true);
	}

	private static void make(Activity activity, @StringRes int messageRes, boolean isError) {
		if (activity == null || activity.isFinishing()) {
			return;
		}

		View root = activity.findViewById(android.R.id.content);
		if (root == null) {
			return;
		}

		Snackbar snackbar = Snackbar.make(root, messageRes, Snackbar.LENGTH_SHORT);

		if (isError) {
			snackbar.setBackgroundTint(
					com.google.android.material.color.MaterialColors.getColor(
							root, com.google.android.material.R.attr.colorErrorContainer));
			snackbar.setTextColor(
					com.google.android.material.color.MaterialColors.getColor(
							root, com.google.android.material.R.attr.colorOnErrorContainer));
		}

		snackbar.show();
	}
}
