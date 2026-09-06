package com.cundong.izhihu.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EvernoteShareUtilsTest {

    @Test public void formatsAnArticleAsANoteReadyToSave() {
        assertEquals("一篇值得保存的文章\n\nhttps://daily.zhihu.com/story/42",
                EvernoteShareUtils.buildShareText(" 一篇值得保存的文章 ",
                        " https://daily.zhihu.com/story/42 "));
    }

    @Test public void preservesUsefulPartialArticleInformation() {
        assertEquals("只有标题", EvernoteShareUtils.buildShareText("只有标题", null));
        assertEquals("https://daily.zhihu.com/story/42",
                EvernoteShareUtils.buildShareText(null, "https://daily.zhihu.com/story/42"));
        assertEquals("", EvernoteShareUtils.buildShareText("  ", "  "));
    }

    @Test public void identifiesBothChineseAndInternationalEvernoteLabels() {
        assertTrue(EvernoteShareUtils.isEvernoteAppLabel("印象笔记"));
        assertTrue(EvernoteShareUtils.isEvernoteAppLabel("Evernote"));
        assertFalse(EvernoteShareUtils.isEvernoteAppLabel("Google Keep"));
    }
}
