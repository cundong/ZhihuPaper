package com.cundong.izhihu.util;

import org.junit.After;
import org.junit.Test;

import java.util.Arrays;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ReadingQueueTest {

    @After public void clearQueue() {
        ReadingQueue.clear();
    }

    @Test public void exposesCorrectNeighborsAtEveryBoundary() {
        ReadingQueue.Item first = item(1, "第一篇");
        ReadingQueue.Item second = item(2, "第二篇");
        ReadingQueue.Item third = item(3, "第三篇");
        ReadingQueue.install(Arrays.asList(first, second, third));

        ReadingQueue.Window firstWindow = ReadingQueue.around(1, null, null);
        assertNull(firstWindow.getPrevious());
        assertEquals(second, firstWindow.getNext());

        ReadingQueue.Window middleWindow = ReadingQueue.around(2, null, null);
        assertEquals(first, middleWindow.getPrevious());
        assertEquals(third, middleWindow.getNext());

        ReadingQueue.Window lastWindow = ReadingQueue.around(3, null, null);
        assertEquals(second, lastWindow.getPrevious());
        assertNull(lastWindow.getNext());
    }

    @Test public void ignoresInvalidAndDuplicateRowsWithoutChangingOrder() {
        ReadingQueue.install(Arrays.asList(
                item(0, "日期标签"), item(5, "保留"), item(5, "重复"), item(6, "下一篇")));

        ReadingQueue.Window window = ReadingQueue.around(5, null, null);
        assertNull(window.getPrevious());
        assertEquals(6L, window.getNext().getId());
        assertEquals("下一篇", window.getNext().getTitle());
    }

    @Test public void usesOnlyImmediateFallbackNeighborsAfterProcessRestoration() {
        ReadingQueue.Item previous = item(8, "上一篇");
        ReadingQueue.Item next = item(10, "下一篇");

        ReadingQueue.Window window = ReadingQueue.around(9, previous, next);

        assertEquals(previous, window.getPrevious());
        assertEquals(next, window.getNext());
    }

    private static ReadingQueue.Item item(long id, String title) {
        return new ReadingQueue.Item(id, title,
                "https://daily.zhihu.com/story/" + id, "https://example.com/" + id + ".jpg");
    }
}
