package com.cundong.izhihu.entity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OfflineSyncSummaryTest {

    @Test
    public void resolveStatus_completeWhenEveryRequestedItemIsAvailable() {
        OfflineSyncSummary summary = new OfflineSyncSummary(4, "2026-08-12");
        for (int i = 0; i < 4; i++) {
            summary.recordSourceAvailable(i == 3);
        }
        summary.setStoryTotal(2);
        summary.recordStoryAvailable(false);
        summary.recordStoryAvailable(true);
        summary.setImageTotal(1);
        summary.recordImageAvailable();

        assertEquals(OfflineSyncSummary.STATUS_COMPLETE, summary.resolveStatus());
        assertEquals(2, summary.getCacheFallbacks());
    }

    @Test
    public void resolveStatus_partialWhenSomeArticleFails() {
        OfflineSyncSummary summary = new OfflineSyncSummary(4, "2026-08-12");
        for (int i = 0; i < 4; i++) {
            summary.recordSourceAvailable(false);
        }
        summary.setStoryTotal(2);
        summary.recordStoryAvailable(false);
        summary.recordStoryFailed();

        assertEquals(OfflineSyncSummary.STATUS_PARTIAL, summary.resolveStatus());
        assertEquals(1, summary.getFailedCount());
    }

    @Test
    public void resolveStatus_failedWithoutUsableArticle() {
        OfflineSyncSummary summary = new OfflineSyncSummary(4, "2026-08-12");
        summary.recordSourceAvailable(false);
        summary.setStoryTotal(1);
        summary.recordStoryFailed();

        assertEquals(OfflineSyncSummary.STATUS_FAILED, summary.resolveStatus());
    }
}
