package com.cundong.izhihu.util;

import java.net.URI;
import java.util.Locale;

/** Pure-Java URL checks so deep links can be regression-tested without Android. */
public final class NewsUrlUtils {

    private NewsUrlUtils() {
    }

    public static long extractStoryId(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return 0L;
        }
        try {
            URI uri = new URI(rawUrl);
            String scheme = lower(uri.getScheme());
            String host = lower(uri.getHost());
            String[] segments = trimPath(uri.getPath()).split("/");
            boolean webStory = ("https".equals(scheme) || "http".equals(scheme))
                    && "daily.zhihu.com".equals(host)
                    && segments.length == 2
                    && "story".equals(segments[0]);
            boolean appStory = "zhihudaily".equals(scheme)
                    && "story".equals(host)
                    && segments.length == 1;
            return (webStory || appStory) ? positiveLong(segments[segments.length - 1]) : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public static boolean isHttpUrl(String rawUrl) {
        try {
            URI uri = new URI(rawUrl);
            String scheme = lower(uri.getScheme());
            return ("https".equals(scheme) || "http".equals(scheme)) && uri.getHost() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String trimPath(String path) {
        if (path == null) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static long positiveLong(String value) {
        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : 0L;
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }
}
