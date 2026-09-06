package com.cundong.izhihu.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.cundong.izhihu.R;
import com.cundong.izhihu.task.BackgroundTask;
import com.cundong.izhihu.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for every screen.
 *
 * Migrated from ActionBarSherlock's SherlockFragmentActivity to AppCompat:
 * the themes are now Material 3 *.NoActionBar, so each subclass supplies a
 * layout containing a Toolbar and calls {@link #setupToolbar}.
 */
public abstract class BaseActivity extends AppCompatActivity {

	protected static final String PREF_DARK_THEME = "dark_theme?";

	protected AppCompatActivity mInstance = null;

	protected Logger mLogger = Logger.getLogger();

	protected boolean isDarkTheme = false;

	private final List<BackgroundTask<?, ?, ?>> activityTasks =
			new ArrayList<BackgroundTask<?, ?, ?>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		isDarkTheme = preferences.getBoolean(PREF_DARK_THEME, false);

			setTheme(resolveThemeResId(isDarkTheme));

		super.onCreate(savedInstanceState);
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(
				getWindow(), getWindow().getDecorView());
		insetsController.setAppearanceLightStatusBars(!isDarkTheme);
		insetsController.setAppearanceLightNavigationBars(!isDarkTheme);

			mInstance = this;
		}

	/** Lets screens with a genuinely different window surface avoid repainting it in their root view. */
	protected int resolveThemeResId(boolean darkTheme) {
		return darkTheme ? R.style.Theme_Daily_AppTheme_Dark : R.style.Theme_Daily_AppTheme_Light;
	}

	/**
	 * Inflates {@code layoutRes}, promotes its {@code R.id.toolbar} to the
	 * support action bar and optionally shows the Up affordance.
	 */
	protected Toolbar setupToolbar(@LayoutRes int layoutRes, boolean showHomeAsUp) {
		setContentView(layoutRes);
		applySystemBarInsets(findViewById(android.R.id.content));

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(showHomeAsUp);
		}

		return toolbar;
	}

	private void applySystemBarInsets(View root) {
		final int initialLeft = root.getPaddingLeft();
		final int initialTop = root.getPaddingTop();
		final int initialRight = root.getPaddingRight();
		final int initialBottom = root.getPaddingBottom();
		ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
			Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
			view.setPadding(initialLeft + bars.left, initialTop + bars.top,
					initialRight + bars.right, initialBottom + bars.bottom);
			return windowInsets;
		});
		ViewCompat.requestApplyInsets(root);
	}

	/**
	 * Replaces the content container with {@code fragment}. Subclasses used to
	 * target android.R.id.content directly; with an explicit Toolbar in the
	 * layout the fragment must go into its own container instead.
	 */
	protected void setContentFragment(Fragment fragment) {
		if (fragment == null) {
			return;
		}

		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.content_container, fragment)
				.commit();
	}

	@Nullable
	protected Fragment getFragment() {
		return null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void recreateActivity() {
		// Posted so the click that toggled the preference finishes dispatching
		// before the activity is torn down.
		getWindow().getDecorView().post(new Runnable() {

			@Override
			public void run() {
				recreate();
			}
		});
	}

	/** Registers work whose UI callbacks must not outlive this Activity. */
	protected final <T extends BackgroundTask<?, ?, ?>> T trackActivityTask(T task) {
		if (task != null) {
			activityTasks.add(task);
		}
		return task;
	}

	/** Tracks and starts a no-argument task in one expression. */
	protected final <T extends BackgroundTask<Void, ?, ?>> T executeActivityTask(T task) {
		trackActivityTask(task);
		task.executeOnExecutor(BackgroundTask.THREAD_POOL_EXECUTOR);
		return task;
	}

	protected final void forgetActivityTask(BackgroundTask<?, ?, ?> task) {
		activityTasks.remove(task);
	}

	protected final void cancelActivityTask(BackgroundTask<?, ?, ?> task) {
		if (task != null) {
			task.cancel(true);
			activityTasks.remove(task);
		}
	}

	@Override
	protected void onDestroy() {
		for (BackgroundTask<?, ?, ?> task :
				new ArrayList<BackgroundTask<?, ?, ?>>(activityTasks)) {
			task.cancel(true);
		}
		activityTasks.clear();
		super.onDestroy();
	}
}
