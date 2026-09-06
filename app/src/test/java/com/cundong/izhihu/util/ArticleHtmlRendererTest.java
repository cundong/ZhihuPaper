package com.cundong.izhihu.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArticleHtmlRendererTest {

    @Test public void sanitizesExecutableArticleMarkup() {
        ArticleHtmlRenderer.RenderedArticle result = ArticleHtmlRenderer.render(
                "<h2>安全阅读</h2>"
                        + "<script>alert(1)</script>"
                        + "<p onclick='steal()'>正文</p>"
                        + "<a href='java&#x0A;script:alert(2)'>危险链接</a>"
                        + "<img src='https://example.com/a.jpg' onerror='steal()'>");

        assertFalse(result.getBodyHtml().contains("<script"));
        assertFalse(result.getBodyHtml().contains("onclick"));
        assertFalse(result.getBodyHtml().contains("onerror"));
        assertFalse(result.getBodyHtml().toLowerCase().contains("javascript:"));
        assertTrue(result.getBodyHtml().contains("https://example.com/a.jpg"));
    }

    @Test public void exposesStableSectionsForArticleTableOfContents() {
        ArticleHtmlRenderer.RenderedArticle result = ArticleHtmlRenderer.render(
                "<h2>第一章</h2><p>甲</p>"
                        + "<div class='question-title'>第二章</div><p>乙</p>"
                        + "<h3>第三章</h3>");

        assertEquals(3, result.getSections().size());
        assertEquals("第一章", result.getSections().get(0).getTitle());
        assertEquals("reader-section-1", result.getSections().get(0).getId());
        assertTrue(result.getBodyHtml().contains("id=\"reader-section-1\""));
        assertTrue(result.getBodyHtml().contains("id=\"reader-section-2\""));
        assertTrue(result.getBodyHtml().contains("id=\"reader-section-3\""));
    }

    @Test public void clampsSystemFontScaleToReadableWebViewRange() {
        assertEquals(100, ArticleHtmlRenderer.textZoomForFontScale(0.85f));
        assertEquals(130, ArticleHtmlRenderer.textZoomForFontScale(1.30f));
        assertEquals(200, ArticleHtmlRenderer.textZoomForFontScale(2.40f));
    }

    @Test public void removesEmbeddedDocumentsAndDangerousInlineStyles() {
        ArticleHtmlRenderer.RenderedArticle result = ArticleHtmlRenderer.render(
                "<iframe src='https://example.com'></iframe>"
                        + "<object data='https://example.com/a'></object>"
                        + "<p style='background:url(javascript:alert(1))'>正文</p>");

        assertFalse(result.getBodyHtml().contains("iframe"));
        assertFalse(result.getBodyHtml().contains("object"));
        assertFalse(result.getBodyHtml().contains("style="));
    }

    @Test public void removesUnsafeLocalAndIntentUrls() {
        ArticleHtmlRenderer.RenderedArticle result = ArticleHtmlRenderer.render(
                "<a href='intent://open'>intent</a>"
                        + "<img src='file:///private/data'>"
                        + "<a href='https://example.com/read'>safe</a>");

        assertFalse(result.getBodyHtml().contains("intent://"));
        assertFalse(result.getBodyHtml().contains("file:///"));
        assertTrue(result.getBodyHtml().contains("https://example.com/read"));
    }
}
