package com.cundong.izhihu.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Process-local lightweight reading queue.
 *
 * <p>The list is deliberately not serialized into an Intent. Launchers pass only the previous and
 * next {@link Item}; while the process is alive this queue supplies further neighbors, and after a
 * process recreation the two serialized neighbors remain a safe one-step fallback.</p>
 */
public final class ReadingQueue {

    public static final String EXTRA_PREVIOUS = "continuous_previous";
    public static final String EXTRA_NEXT = "continuous_next";

    private static final String SUFFIX_ID = "_id";
    private static final String SUFFIX_TITLE = "_title";
    private static final String SUFFIX_SHARE_URL = "_share_url";
    private static final String SUFFIX_IMAGE = "_image";

    private static final Object LOCK = new Object();
    private static List<Item> items = new ArrayList<Item>();

    private ReadingQueue() {
    }

    public static void install(Collection<Item> source) {
        ArrayList<Item> normalized = new ArrayList<Item>();
        Set<Long> seenIds = new HashSet<Long>();
        if (source != null) {
            for (Item item : source) {
                if (item != null && item.id > 0L && seenIds.add(item.id)) {
                    normalized.add(item);
                }
            }
        }
        synchronized (LOCK) {
            items = normalized;
        }
    }

    public static Window around(long currentId, Item fallbackPrevious, Item fallbackNext) {
        synchronized (LOCK) {
            for (int index = 0; index < items.size(); index++) {
                if (items.get(index).id == currentId) {
                    return new Window(index > 0 ? items.get(index - 1) : null,
                            index + 1 < items.size() ? items.get(index + 1) : null);
                }
            }
        }
        return new Window(fallbackPrevious, fallbackNext);
    }

    public static void clear() {
        synchronized (LOCK) {
            items = new ArrayList<Item>();
        }
    }

    /** Stores only primitives/Strings, so saved state never depends on an obfuscated class name. */
    public static void put(android.os.Bundle bundle, String key, Item item) {
        if (bundle == null || key == null) {
            return;
        }
        if (item == null) {
            bundle.remove(key + SUFFIX_ID);
            bundle.remove(key + SUFFIX_TITLE);
            bundle.remove(key + SUFFIX_SHARE_URL);
            bundle.remove(key + SUFFIX_IMAGE);
            return;
        }
        bundle.putLong(key + SUFFIX_ID, item.id);
        bundle.putString(key + SUFFIX_TITLE, item.title);
        bundle.putString(key + SUFFIX_SHARE_URL, item.shareUrl);
        bundle.putString(key + SUFFIX_IMAGE, item.image);
    }

    public static Item get(android.os.Bundle bundle, String key) {
        if (bundle == null || key == null) {
            return null;
        }
        long id = bundle.getLong(key + SUFFIX_ID, 0L);
        return id > 0L ? new Item(id, bundle.getString(key + SUFFIX_TITLE),
                bundle.getString(key + SUFFIX_SHARE_URL), bundle.getString(key + SUFFIX_IMAGE))
                : null;
    }

    public static void put(android.content.Intent intent, String key, Item item) {
        if (intent == null) {
            return;
        }
        android.os.Bundle values = new android.os.Bundle();
        put(values, key, item);
        intent.putExtras(values);
    }

    public static Item get(android.content.Intent intent, String key) {
        return intent == null ? null : get(intent.getExtras(), key);
    }

    public static final class Window {
        private final Item previous;
        private final Item next;

        private Window(Item previous, Item next) {
            this.previous = previous;
            this.next = next;
        }

        public Item getPrevious() {
            return previous;
        }

        public Item getNext() {
            return next;
        }
    }

    /** Exactly the fields allowed to cross the list/detail boundary. */
    public static final class Item {
        private final long id;
        private final String title;
        private final String shareUrl;
        private final String image;

        public Item(long id, String title, String shareUrl, String image) {
            this.id = id;
            this.title = title;
            this.shareUrl = shareUrl;
            this.image = image;
        }

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getShareUrl() {
            return shareUrl;
        }

        public String getImage() {
            return image;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Item && ((Item) other).id == id;
        }

        @Override
        public int hashCode() {
            return (int) (id ^ (id >>> 32));
        }
    }
}
