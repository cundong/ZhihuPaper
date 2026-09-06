package com.cundong.izhihu.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies process-recreation state contains no obfuscation-sensitive objects. */
@RunWith(AndroidJUnit4.class)
public class ReadingQueueStateTest {

    @Test
    public void savedStateUsesOnlyPrimitiveFields() {
        Bundle state = new Bundle();
        ReadingQueue.Item original = new ReadingQueue.Item(42L, "可恢复",
                "https://daily.zhihu.com/story/42", "https://example.com/42.jpg");

        ReadingQueue.put(state, ReadingQueue.EXTRA_NEXT, original);
        ReadingQueue.Item restored = ReadingQueue.get(state, ReadingQueue.EXTRA_NEXT);

        assertEquals(original, restored);
        assertEquals("可恢复", restored.getTitle());
        for (String key : state.keySet()) {
            Object value = state.get(key);
            assertTrue(value instanceof Long || value instanceof String);
        }
    }
}
