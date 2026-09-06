package com.cundong.izhihu.util;

import java.util.Locale;

/** Small, Android-free rules for preparing a readable note from a news story. */
public final class EvernoteShareUtils {

    private EvernoteShareUtils() {
    }

    public static String buildShareText(String title, String shareUrl) {
        String cleanTitle = trimOrEmpty(title);
        String cleanUrl = trimOrEmpty(shareUrl);
        if (cleanTitle.isEmpty()) {
            return cleanUrl;
        }
        if (cleanUrl.isEmpty()) {
            return cleanTitle;
        }
        return cleanTitle + "\n\n" + cleanUrl;
    }

    public static boolean isEvernoteAppLabel(CharSequence appLabel) {
        if (appLabel == null) {
            return false;
        }
        String normalized = appLabel.toString().trim().toLowerCase(Locale.ROOT);
        return normalized.contains("印象笔记") || normalized.contains("evernote");
    }

    private static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
