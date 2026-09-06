package com.cundong.izhihu.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.WorkInfo;

import com.cundong.izhihu.R;
import com.cundong.izhihu.entity.OfflineSyncSummary;
import com.cundong.izhihu.fragment.NewsListFragment;
import com.cundong.izhihu.task.OfflineSyncScheduler;
import com.cundong.izhihu.util.DateUtils;
import com.cundong.izhihu.util.PreferencesUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.UUID;

/** Main news screen and lifecycle-aware owner of offline sync feedback. */
public class MainActivity extends BaseActivity {

    private static final String STATE_EXPECTED_WORK_ID = "offline_expected_work_id";
    private static final String STATE_OBSERVED_WORK_ID = "offline_observed_work_id";
    private static final String STATE_HANDLED_WORK_ID = "offline_handled_work_id";

    private UUID mExpectedWorkId;
    private UUID mObservedWorkId;
    private String mHandledTerminalWorkId;
    private String mLastProgressSignature;
    private Snackbar mOfflineSnackbar;
    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> refreshTheme());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        restoreOfflineState(savedInstanceState);
        setupToolbar(R.layout.activity_main, false);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        if (savedInstanceState == null) {
            setContentFragment(getFragment());
        }

        observeOfflineSync();
        // Enqueue on every fresh app visit until a complete run has marked the
        // day. WorkManager itself waits for unmetered connectivity, so leaving
        // Wi-Fi temporarily no longer loses the automatic download request.
        if (!PreferencesUtils.getBoolean(getApplicationContext(),
                DateUtils.getCurrentDate(), false)
                && mExpectedWorkId == null && mObservedWorkId == null) {
            mExpectedWorkId = OfflineSyncScheduler.enqueueAutomatic(
                    getApplicationContext());
        }
    }

    @Override
    protected Fragment getFragment() {
        return new NewsListFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_download) {
            startManualOfflineSync();
            return true;
        } else if (itemId == R.id.action_favorite) {
            startActivity(new Intent(this, FavoriteActivity.class));
            return true;
        } else if (itemId == R.id.action_setting) {
            settingsLauncher.launch(new Intent(this, OtherPrefsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshTheme() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (isDarkTheme != preferences.getBoolean(PREF_DARK_THEME, false)) {
            recreateActivity();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        putUuid(outState, STATE_EXPECTED_WORK_ID, mExpectedWorkId);
        putUuid(outState, STATE_OBSERVED_WORK_ID, mObservedWorkId);
        outState.putString(STATE_HANDLED_WORK_ID, mHandledTerminalWorkId);
    }

    @Override
    protected void onDestroy() {
        if (mOfflineSnackbar != null) {
            mOfflineSnackbar.dismiss();
            mOfflineSnackbar = null;
        }
        super.onDestroy();
    }

    private void startManualOfflineSync() {
        mExpectedWorkId = OfflineSyncScheduler.enqueueManual(getApplicationContext());
        mObservedWorkId = null;
        mHandledTerminalWorkId = null;
        mLastProgressSignature = null;
        showOfflineMessage(getString(R.string.offline_sync_queued), false, true);
    }

    private void observeOfflineSync() {
        OfflineSyncScheduler.observe(getApplicationContext()).observe(this,
                new Observer<List<WorkInfo>>() {
                    @Override
                    public void onChanged(List<WorkInfo> workInfos) {
                        WorkInfo workInfo = findRelevantWork(workInfos);
                        if (workInfo == null) {
                            return;
                        }
                        if (!workInfo.getState().isFinished()) {
                            mObservedWorkId = workInfo.getId();
                        }
                        renderOfflineWork(workInfo);
                    }
                });
    }

    private WorkInfo findRelevantWork(List<WorkInfo> workInfos) {
        if (workInfos == null || workInfos.isEmpty()) {
            return null;
        }
        WorkInfo expected = findById(workInfos, mExpectedWorkId);
        if (expected != null) {
            return expected;
        }
        WorkInfo observed = findById(workInfos, mObservedWorkId);
        if (observed != null) {
            return observed;
        }

        WorkInfo queued = null;
        for (WorkInfo workInfo : workInfos) {
            if (workInfo.getState() == WorkInfo.State.RUNNING) {
                return workInfo;
            }
            if (!workInfo.getState().isFinished()) {
                queued = workInfo;
            }
        }
        // Do not replay an unrelated historical terminal result when opening
        // the screen. Terminal results are rendered only after this instance
        // has observed or requested that exact work id.
        return queued;
    }

    private WorkInfo findById(List<WorkInfo> workInfos, UUID id) {
        if (id == null) {
            return null;
        }
        for (WorkInfo workInfo : workInfos) {
            if (id.equals(workInfo.getId())) {
                return workInfo;
            }
        }
        return null;
    }

    private void renderOfflineWork(WorkInfo workInfo) {
        WorkInfo.State state = workInfo.getState();
        if (state == WorkInfo.State.RUNNING) {
            renderProgress(workInfo.getProgress());
            return;
        }
        if (state == WorkInfo.State.SUCCEEDED || state == WorkInfo.State.FAILED
                || state == WorkInfo.State.CANCELLED) {
            renderTerminal(workInfo);
        }
    }

    private void renderProgress(Data progress) {
        String stage = progress.getString(OfflineSyncSummary.KEY_STAGE);
        int completed = progress.getInt(OfflineSyncSummary.KEY_COMPLETED, 0);
        int total = progress.getInt(OfflineSyncSummary.KEY_TOTAL, 0);
        int failed = progress.getInt(OfflineSyncSummary.KEY_FAILED, 0);
        String signature = String.valueOf(stage) + ':' + completed + ':' + total + ':' + failed;
        if (signature.equals(mLastProgressSignature)) {
            return;
        }
        mLastProgressSignature = signature;

        int messageRes;
        if (OfflineSyncSummary.STAGE_LISTS.equals(stage)) {
            messageRes = R.string.offline_sync_lists_progress;
        } else if (OfflineSyncSummary.STAGE_ARTICLES.equals(stage)) {
            messageRes = R.string.offline_sync_articles_progress;
        } else if (OfflineSyncSummary.STAGE_IMAGES.equals(stage)) {
            messageRes = R.string.offline_sync_images_progress;
        } else {
            showOfflineMessage(getString(R.string.offline_download_doing), false, true);
            return;
        }
        showOfflineMessage(getString(messageRes, completed, total, failed), false, true);
    }

    private void renderTerminal(WorkInfo workInfo) {
        String workId = workInfo.getId().toString();
        if (workId.equals(mHandledTerminalWorkId)) {
            return;
        }
        mHandledTerminalWorkId = workId;
        mObservedWorkId = workInfo.getId();
        mExpectedWorkId = null;
        mLastProgressSignature = null;

        Data output = workInfo.getOutputData();
        String status = output.getString(OfflineSyncSummary.KEY_STATUS);
        int failed = output.getInt(OfflineSyncSummary.KEY_FAILED, 0);
        int storyTotal = output.getInt(OfflineSyncSummary.KEY_STORY_TOTAL, 0);
        int storyAvailable = output.getInt(OfflineSyncSummary.KEY_STORY_AVAILABLE, 0);
        int imageAvailable = output.getInt(OfflineSyncSummary.KEY_IMAGE_AVAILABLE, 0);
        int cacheFallbacks = output.getInt(OfflineSyncSummary.KEY_CACHE_FALLBACKS, 0);

        if (workInfo.getState() == WorkInfo.State.CANCELLED) {
            showOfflineMessage(getString(R.string.offline_sync_cancelled), true, false);
		} else if (OfflineSyncSummary.STATUS_COMPLETE.equals(status)) {
			// The durable Worker owns the completion marker under the same lock as
			// settings cleanup. An Activity observer must never replay a stale terminal
			// result and recreate a marker after the user has cleared offline data.
			showOfflineMessage(getString(R.string.offline_sync_complete,
                    storyAvailable, imageAvailable, cacheFallbacks), false, false);
        } else if (OfflineSyncSummary.STATUS_PARTIAL.equals(status)) {
            showOfflineMessage(getString(R.string.offline_sync_partial,
                    storyAvailable, storyTotal, failed), true, false);
        } else {
            showOfflineMessage(getString(R.string.offline_sync_failed,
                    Math.max(1, failed)), true, false);
        }
    }

    private void showOfflineMessage(String message, boolean isError, boolean progress) {
        if (isFinishing()) {
            return;
        }
        View root = findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        if (progress && mOfflineSnackbar != null && mOfflineSnackbar.isShown()) {
            mOfflineSnackbar.setText(message);
            return;
        }
        if (mOfflineSnackbar != null && mOfflineSnackbar.isShown()) {
            mOfflineSnackbar.dismiss();
        }
        mOfflineSnackbar = Snackbar.make(root, message,
                progress ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT);
        if (isError) {
            mOfflineSnackbar.setBackgroundTint(MaterialColors.getColor(
                    root, com.google.android.material.R.attr.colorErrorContainer));
            mOfflineSnackbar.setTextColor(MaterialColors.getColor(
                    root, com.google.android.material.R.attr.colorOnErrorContainer));
        }
        mOfflineSnackbar.show();
    }

    private void restoreOfflineState(Bundle state) {
        if (state == null) {
            return;
        }
        mExpectedWorkId = parseUuid(state.getString(STATE_EXPECTED_WORK_ID));
        mObservedWorkId = parseUuid(state.getString(STATE_OBSERVED_WORK_ID));
        mHandledTerminalWorkId = state.getString(STATE_HANDLED_WORK_ID);
    }

    private void putUuid(Bundle state, String key, UUID id) {
        if (id != null) {
            state.putString(key, id.toString());
        }
    }

    private UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
