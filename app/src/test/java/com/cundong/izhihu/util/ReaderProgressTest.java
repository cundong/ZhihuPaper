package com.cundong.izhihu.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReaderProgressTest {

    @Test public void createsArticleScopedPreferenceKeys() {
        assertEquals("reader_progress_42", ReaderProgress.preferenceKey(42L));
        assertEquals("reader_progress_0", ReaderProgress.preferenceKey(-5L));
    }

    @Test public void normalizesPersistedProgress() {
        assertEquals(0, ReaderProgress.clamp(-1));
        assertEquals(4231, ReaderProgress.clamp(4231));
        assertEquals(ReaderProgress.MAX_PROGRESS, ReaderProgress.clamp(20000));
    }

    @Test public void convertsScrollOffsetsToStableProgress() {
        assertEquals(0, ReaderProgress.fromScrollPosition(0, 0));
        assertEquals(5000, ReaderProgress.fromScrollPosition(450, 900));
        assertEquals(ReaderProgress.MAX_PROGRESS,
                ReaderProgress.fromScrollPosition(1100, 900));
    }
}
