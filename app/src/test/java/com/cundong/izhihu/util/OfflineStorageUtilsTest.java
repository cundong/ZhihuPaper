package com.cundong.izhihu.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OfflineStorageUtilsTest {

	@Test public void formatsStorageForPeople() {
		assertEquals("0 B", OfflineStorageUtils.formatBytes(0));
		assertEquals("1.0 KB", OfflineStorageUtils.formatBytes(1024));
		assertEquals("1.5 MB", OfflineStorageUtils.formatBytes(1572864));
	}

	@Test public void acceptsRasterImageContentTypes() {
		assertTrue(ArticleImageCache.isSupportedImageContentType("image", "jpeg"));
		assertTrue(ArticleImageCache.isSupportedImageContentType("IMAGE", "webp"));
	}

	@Test public void rejectsNonImageAndSvgContentTypes() {
		assertFalse(ArticleImageCache.isSupportedImageContentType("text", "html"));
		assertFalse(ArticleImageCache.isSupportedImageContentType("image", "svg+xml"));
		assertFalse(ArticleImageCache.isSupportedImageContentType(null, null));
	}
}
