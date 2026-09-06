package com.cundong.izhihu.util;

/** Stable, article-scoped representation of the reader's scroll position. */
public final class ReaderProgress {

    public static final int MAX_PROGRESS = 10000;

    private ReaderProgress() {
    }

    public static String preferenceKey(long articleId) {
        return "reader_progress_" + Math.max(0L, articleId);
    }

    public static int clamp(int progress) {
        return Math.max(0, Math.min(MAX_PROGRESS, progress));
    }

    public static int fromScrollPosition(int scrollY, int maximumScrollY) {
        if (scrollY <= 0 || maximumScrollY <= 0) {
            return 0;
        }
        return clamp(Math.round((scrollY / (float) maximumScrollY) * MAX_PROGRESS));
    }
}
