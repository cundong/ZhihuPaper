package com.cundong.izhihu.entity;

import androidx.work.Data;

/**
 * Mutable accounting for one offline synchronization run.
 *
 * <p>The terminal states deliberately describe offline availability, rather
 * than whether every byte came from the network:</p>
 * <ul>
 *     <li>{@link #STATUS_COMPLETE}: all four feeds, every unique article and
 *     every referenced image are available. A validated cache fallback still
 *     counts as available.</li>
 *     <li>{@link #STATUS_PARTIAL}: at least one article is available, but one
 *     or more requested items failed.</li>
 *     <li>{@link #STATUS_FAILED}: no usable feed or no usable article exists.</li>
 * </ul>
 */
public final class OfflineSyncSummary {

    public static final String STATUS_COMPLETE = "complete";
    public static final String STATUS_PARTIAL = "partial";
    public static final String STATUS_FAILED = "failed";

    public static final String STAGE_QUEUED = "queued";
    public static final String STAGE_LISTS = "lists";
    public static final String STAGE_ARTICLES = "articles";
    public static final String STAGE_IMAGES = "images";
    public static final String STAGE_FINISHED = "finished";
    public static final String STAGE_STOPPED = "stopped";

    public static final String KEY_STATUS = "offline_status";
    public static final String KEY_STAGE = "offline_stage";
    public static final String KEY_COMPLETED = "offline_completed";
    public static final String KEY_TOTAL = "offline_total";
    public static final String KEY_FAILED = "offline_failed";
    public static final String KEY_SOURCE_TOTAL = "offline_source_total";
    public static final String KEY_SOURCE_AVAILABLE = "offline_source_available";
    public static final String KEY_CACHE_FALLBACKS = "offline_cache_fallbacks";
    public static final String KEY_STORY_TOTAL = "offline_story_total";
    public static final String KEY_STORY_AVAILABLE = "offline_story_available";
    public static final String KEY_IMAGE_TOTAL = "offline_image_total";
    public static final String KEY_IMAGE_AVAILABLE = "offline_image_available";
    public static final String KEY_REQUEST_DATE = "offline_request_date";

    private final int sourceTotal;
    private final String requestDate;

    private int sourceAvailable;
    private int sourceFailed;
    private int storyTotal;
    private int storyAvailable;
    private int storyFailed;
    private int imageTotal;
    private int imageAvailable;
    private int imageFailed;
    private int cacheFallbacks;
    private int workerFailures;

    public OfflineSyncSummary(int sourceTotal, String requestDate) {
        this.sourceTotal = Math.max(0, sourceTotal);
        this.requestDate = requestDate == null ? "" : requestDate;
    }

    public void recordSourceAvailable(boolean fromCache) {
        sourceAvailable++;
        if (fromCache) {
            cacheFallbacks++;
        }
    }

    public void recordSourceFailed() {
        sourceFailed++;
    }

    public void setStoryTotal(int storyTotal) {
        this.storyTotal = Math.max(0, storyTotal);
    }

    public void recordStoryAvailable(boolean fromCache) {
        storyAvailable++;
        if (fromCache) {
            cacheFallbacks++;
        }
    }

    public void recordStoryFailed() {
        storyFailed++;
    }

    public void setImageTotal(int imageTotal) {
        this.imageTotal = Math.max(0, imageTotal);
    }

    public void recordImageAvailable() {
        imageAvailable++;
    }

    public void recordImageFailed() {
        imageFailed++;
    }

    public void recordWorkerFailure() {
        workerFailures++;
    }

    public int getSourceAvailable() {
        return sourceAvailable;
    }

    public int getStoryTotal() {
        return storyTotal;
    }

    public int getStoryAvailable() {
        return storyAvailable;
    }

    public int getImageTotal() {
        return imageTotal;
    }

    public int getImageAvailable() {
        return imageAvailable;
    }

    public int getCacheFallbacks() {
        return cacheFallbacks;
    }

    public int getFailedCount() {
        return sourceFailed + storyFailed + imageFailed + workerFailures;
    }

    public String resolveStatus() {
        if (sourceAvailable == 0 || storyTotal == 0 || storyAvailable == 0) {
            return STATUS_FAILED;
        }
        if (getFailedCount() == 0
                && sourceAvailable == sourceTotal
                && storyAvailable == storyTotal
                && imageAvailable == imageTotal) {
            return STATUS_COMPLETE;
        }
        return STATUS_PARTIAL;
    }

    public Data progressData(String stage, int completed, int total) {
        return baseData(new Data.Builder(), stage, completed, total)
                .putString(KEY_STATUS, "")
                .build();
    }

    public Data outputData(String stage) {
        return baseData(new Data.Builder(), stage, terminalCompleted(), terminalTotal())
                .putString(KEY_STATUS, resolveStatus())
                .build();
    }

    private Data.Builder baseData(Data.Builder builder, String stage, int completed, int total) {
        return builder
                .putString(KEY_STAGE, stage == null ? "" : stage)
                .putInt(KEY_COMPLETED, Math.max(0, completed))
                .putInt(KEY_TOTAL, Math.max(0, total))
                .putInt(KEY_FAILED, getFailedCount())
                .putInt(KEY_SOURCE_TOTAL, sourceTotal)
                .putInt(KEY_SOURCE_AVAILABLE, sourceAvailable)
                .putInt(KEY_CACHE_FALLBACKS, cacheFallbacks)
                .putInt(KEY_STORY_TOTAL, storyTotal)
                .putInt(KEY_STORY_AVAILABLE, storyAvailable)
                .putInt(KEY_IMAGE_TOTAL, imageTotal)
                .putInt(KEY_IMAGE_AVAILABLE, imageAvailable)
                .putString(KEY_REQUEST_DATE, requestDate);
    }

    private int terminalCompleted() {
        return sourceAvailable + storyAvailable + imageAvailable;
    }

    private int terminalTotal() {
        return sourceTotal + storyTotal + imageTotal;
    }
}
