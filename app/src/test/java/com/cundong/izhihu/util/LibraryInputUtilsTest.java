package com.cundong.izhihu.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class LibraryInputUtilsTest {

    @Test public void parsesHumanFriendlyCommaSeparatedTags() {
        assertEquals(Arrays.asList("Android", "阅读", "AI"),
                LibraryInputUtils.parseTags(" Android, 阅读，AI,android, ,阅读 "));
        assertEquals(Collections.emptyList(), LibraryInputUtils.parseTags("， , "));
    }

    @Test public void formatsTagsForEditing() {
        assertEquals("Android，阅读", LibraryInputUtils.formatTags(
                Arrays.asList("Android", "阅读")));
        assertEquals("", LibraryInputUtils.formatTags(null));
    }

    @Test public void boundsSelectionsBeforeSavingAQuote() {
        assertEquals("一段值得保存的摘录", LibraryInputUtils.normalizeQuote(
                "  一段值得保存的摘录  "));
        assertEquals("", LibraryInputUtils.normalizeQuote("   "));
    }
}
