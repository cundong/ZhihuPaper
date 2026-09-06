package com.cundong.izhihu.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NewsUrlUtilsTest {

    @Test public void acceptsOnlyKnownStoryDeepLinks() {
        assertEquals(4115152L, NewsUrlUtils.extractStoryId("https://daily.zhihu.com/story/4115152"));
        assertEquals(4115152L, NewsUrlUtils.extractStoryId(
                "https://daily.zhihu.com/story/4115152?utm_source=test#answer"));
        assertEquals(4115152L, NewsUrlUtils.extractStoryId(
                "HTTP://DAILY.ZHIHU.COM/story/4115152"));
        assertEquals(4115152L, NewsUrlUtils.extractStoryId("zhihudaily://story/4115152"));
        assertEquals(0L, NewsUrlUtils.extractStoryId("https://evil.example/story/4115152"));
        assertEquals(0L, NewsUrlUtils.extractStoryId("https://daily.zhihu.com/story/not-a-number"));
        assertEquals(0L, NewsUrlUtils.extractStoryId("https://daily.zhihu.com/story/4115152/extra"));
        assertEquals(0L, NewsUrlUtils.extractStoryId("https://daily.zhihu.com.evil.example/story/4115152"));
        assertEquals(0L, NewsUrlUtils.extractStoryId("https://daily.zhihu.com@evil.example/story/4115152"));
        assertEquals(0L, NewsUrlUtils.extractStoryId("zhihudaily://theme/4115152"));
    }

    @Test public void allowsOnlyHttpFamilyForExternalNavigation() {
        assertTrue(NewsUrlUtils.isHttpUrl("https://www.zhihu.com"));
        assertTrue(NewsUrlUtils.isHttpUrl("http://daily.zhihu.com/story/1"));
        assertFalse(NewsUrlUtils.isHttpUrl("javascript:alert(1)"));
        assertFalse(NewsUrlUtils.isHttpUrl("intent://scan/#Intent;scheme=zxing;end"));
    }
}
