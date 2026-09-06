package com.cundong.izhihu.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Text normalization shared by the reading-library editor and its tests. */
public final class LibraryInputUtils {

    private static final int MAX_QUOTE_LENGTH = 1200;

    private LibraryInputUtils() {
    }

    public static List<String> parseTags(String value) {
        LinkedHashMap<String, String> unique = new LinkedHashMap<String, String>();
        if (value != null) {
            String[] parts = value.split("[,，]");
            for (String part : parts) {
                String tag = part.trim();
                if (tag.length() > 0) {
                    String key = tag.toLowerCase(Locale.US);
                    if (!unique.containsKey(key)) {
                        unique.put(key, tag);
                    }
                }
            }
        }
        return new ArrayList<String>(unique.values());
    }

    public static String formatTags(Collection<String> tags) {
        StringBuilder result = new StringBuilder();
        if (tags != null) {
            for (String tag : tags) {
                if (tag == null || tag.trim().length() == 0) {
                    continue;
                }
                if (result.length() > 0) {
                    result.append('，');
                }
                result.append(tag.trim());
            }
        }
        return result.toString();
    }

    public static String normalizeQuote(String value) {
        String quote = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return quote.length() > MAX_QUOTE_LENGTH
                ? quote.substring(0, MAX_QUOTE_LENGTH) : quote;
    }
}
