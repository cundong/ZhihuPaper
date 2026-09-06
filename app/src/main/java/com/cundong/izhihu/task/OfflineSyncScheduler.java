package com.cundong.izhihu.task;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.cundong.izhihu.entity.OfflineSyncSummary;
import com.cundong.izhihu.util.DateUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;

/** Owns the single process-wide offline work identity and its policies. */
public final class OfflineSyncScheduler {

    public static final String UNIQUE_WORK_NAME = "zhihu_paper_offline_sync";
    public static final String WORK_TAG = "zhihu_paper_offline_sync_tag";
    public static final String KEY_TRIGGER = "offline_trigger";
    public static final String TRIGGER_AUTOMATIC = "automatic";
    public static final String TRIGGER_MANUAL = "manual";

    private OfflineSyncScheduler() {
    }

    /** Automatic work waits for an unmetered network and never replaces a run. */
    public static UUID enqueueAutomatic(Context context) {
        return enqueue(context, NetworkType.UNMETERED, ExistingWorkPolicy.KEEP,
                TRIGGER_AUTOMATIC);
    }

    /** A user action accepts any connected network and replaces stale queued work. */
    public static UUID enqueueManual(Context context) {
        return enqueue(context, NetworkType.CONNECTED, ExistingWorkPolicy.REPLACE,
                TRIGGER_MANUAL);
    }

    public static LiveData<List<WorkInfo>> observe(Context context) {
        Context applicationContext = context.getApplicationContext();
        return WorkManager.getInstance(applicationContext)
                .getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME);
    }

    /**
     * Cancels the complete unique-work chain and waits until WorkManager has
     * persisted that cancellation. The caller must still take the shared
     * OfflineStorageUtils mutation boundary before deleting content, because a
     * running Worker observes cancellation cooperatively.
     */
    public static boolean cancelAndAwait(Context context) {
        Context applicationContext = context.getApplicationContext();
        Operation operation = WorkManager.getInstance(applicationContext)
                .cancelUniqueWork(UNIQUE_WORK_NAME);
        try {
            operation.getResult().get();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            return false;
        }
    }

    private static UUID enqueue(Context context, NetworkType networkType,
            ExistingWorkPolicy policy, String trigger) {
        Context applicationContext = context.getApplicationContext();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build();
        Data input = new Data.Builder()
                .putString(KEY_TRIGGER, trigger)
                .putString(OfflineSyncSummary.KEY_REQUEST_DATE, DateUtils.getCurrentDate())
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(OfflineSyncWorker.class)
                .setConstraints(constraints)
                .setInputData(input)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL,
                        30L, TimeUnit.SECONDS)
                .addTag(WORK_TAG)
                .build();
        WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, policy, request);
        return request.getId();
    }
}
