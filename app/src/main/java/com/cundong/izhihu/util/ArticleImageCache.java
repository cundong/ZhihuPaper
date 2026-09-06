package com.cundong.izhihu.util;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.cundong.izhihu.http.HttpUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Bounded, atomically-published cache for article images. */
public final class ArticleImageCache {

    public static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
    public static final long MAX_CACHE_BYTES = 100L * 1024L * 1024L;

    private static final long STALE_PART_AGE_MILLIS = 60L * 60L * 1000L;
    private static final String OFFLINE_PART_SUFFIX = ".offline.part";

    private final Context applicationContext;

    public ArticleImageCache(Context context) {
        applicationContext = context.getApplicationContext();
    }

    /** Removes abandoned temporary files and enforces the 100 MiB LRU cap. */
    public synchronized void maintain() {
        File directory = ZhihuUtils.getDetailImageCacheDir(applicationContext);
        cleanStaleParts(directory);
        trimToFit(directory, 0L);
    }

    /**
     * Makes one image available under the filename expected by the article
     * renderer. The final file is never exposed until the complete response is
     * flushed and renamed from the same directory.
     */
    public synchronized DownloadResult download(String originalUrl,
            CancellationChecker cancellationChecker) throws IOException {
        throwIfStopped(cancellationChecker);
        if (isBlank(originalUrl)) {
            throw new IOException("Empty image URL");
        }

        File target = ZhihuUtils.getCacheImgFile(applicationContext, originalUrl);
        File directory = target.getParentFile();
        if (directory == null || (!directory.exists() && !directory.mkdirs())) {
            throw new IOException("Unable to create image cache directory");
        }

        if (isUsable(target)) {
            touch(target);
            return DownloadResult.CACHED;
        }
        if (target.exists() && !target.delete()) {
            throw new IOException("Unable to remove invalid image cache entry");
        }

        File part = new File(target.getAbsolutePath() + OFFLINE_PART_SUFFIX);
        if (part.exists() && !part.delete()) {
            throw new IOException("Unable to remove abandoned image part");
        }

        String requestUrl = normalizeRequestUrl(originalUrl);
        Request request = new Request.Builder()
                .url(requestUrl)
                .header("Accept", "image/*")
                .get()
                .build();
        Call call = HttpUtils.getInstance(applicationContext).newCall(request);
        Response response = null;
        try {
            response = call.execute();
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("Unexpected image response code " + response.code());
            }
            MediaType contentType = body.contentType();
            if (!isSupportedImageContentType(contentType == null ? null : contentType.type(),
                    contentType == null ? null : contentType.subtype())) {
                throw new IOException("Unexpected image content type");
            }
            long declaredLength = body.contentLength();
            if (declaredLength > MAX_IMAGE_BYTES) {
                throw new IOException("Image exceeds 10 MiB limit");
            }

            long actualLength = writeLimited(body.byteStream(), part, call,
                    cancellationChecker);
            if (actualLength <= 0L) {
                throw new IOException("Empty image response");
            }
            if (!isDecodableImage(part)) {
                throw new IOException("Response is not a decodable image");
            }

            throwIfStopped(cancellationChecker);
            trimToFit(directory, actualLength);

            // The foreground reader may have won the race while this request
            // was in flight. Prefer its already-published complete file.
            if (isUsable(target)) {
                touch(target);
                return DownloadResult.CACHED;
            }
            if (target.exists() && !target.delete()) {
                throw new IOException("Unable to replace invalid image cache entry");
            }
            if (!part.renameTo(target)) {
                throw new IOException("Unable to atomically publish image cache entry");
            }
            touch(target);
            return DownloadResult.DOWNLOADED;
        } finally {
            if (response != null) {
                response.close();
            }
            if (part.exists()) {
                // Only this class uses the .offline.part suffix, so this cannot
                // disrupt the foreground image loader.
                part.delete();
            }
        }
    }

    private long writeLimited(InputStream input, File part, Call call,
            CancellationChecker cancellationChecker) throws IOException {
        FileOutputStream fileOutput = null;
        BufferedOutputStream output = null;
        try {
            fileOutput = new FileOutputStream(part, false);
            output = new BufferedOutputStream(fileOutput, 16 * 1024);
            byte[] buffer = new byte[16 * 1024];
            long total = 0L;
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (isStopped(cancellationChecker)) {
                    call.cancel();
                    throw new InterruptedIOException("Offline sync was stopped");
                }
                total += read;
                if (total > MAX_IMAGE_BYTES) {
                    call.cancel();
                    throw new IOException("Image exceeds 10 MiB limit");
                }
                output.write(buffer, 0, read);
            }
            output.flush();
            fileOutput.getFD().sync();
            return total;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            } else if (fileOutput != null) {
                try {
                    fileOutput.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void cleanStaleParts(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        long staleBefore = System.currentTimeMillis() - STALE_PART_AGE_MILLIS;
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".part")
                    && (file.getName().endsWith(OFFLINE_PART_SUFFIX)
                    || file.lastModified() < staleBefore)) {
                file.delete();
            }
        }
    }

    private void trimToFit(File directory, long incomingBytes) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        List<File> cacheFiles = new ArrayList<File>();
        long total = 0L;
        for (File file : files) {
            if (file.isFile() && ZhihuUtils.isCacheImgFileName(file.getName())) {
                cacheFiles.add(file);
                total += Math.max(0L, file.length());
            }
        }
        Collections.sort(cacheFiles, new Comparator<File>() {
            @Override
            public int compare(File first, File second) {
                if (first.lastModified() == second.lastModified()) {
                    return first.getName().compareTo(second.getName());
                }
                return first.lastModified() < second.lastModified() ? -1 : 1;
            }
        });
        for (File file : cacheFiles) {
            if (total + incomingBytes <= MAX_CACHE_BYTES) {
                break;
            }
            long length = Math.max(0L, file.length());
            if (file.delete()) {
                total -= length;
            }
        }
    }

    private boolean isUsable(File file) {
        return file.isFile() && file.length() > 0L && file.length() <= MAX_IMAGE_BYTES
                && isDecodableImage(file);
    }

    static boolean isSupportedImageContentType(String type, String subtype) {
        return type != null && subtype != null && "image".equalsIgnoreCase(type)
                && !"svg+xml".equalsIgnoreCase(subtype);
    }

    private static boolean isDecodableImage(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return options.outWidth > 0 && options.outHeight > 0 && options.outMimeType != null;
    }

    private void touch(File file) {
        file.setLastModified(System.currentTimeMillis());
    }

    private String normalizeRequestUrl(String originalUrl) throws IOException {
        String value = originalUrl.trim();
        if (value.startsWith("//")) {
            value = "https:" + value;
        } else if (value.regionMatches(true, 0, "http://", 0, 7)) {
            // The app disables cleartext traffic. Zhihu's image CDNs support
            // HTTPS, so safely upgrade legacy article markup before requesting.
            value = "https://" + value.substring(7);
        }
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || isBlank(uri.getHost()) || uri.getUserInfo() != null) {
                throw new IOException("Unsafe image URL");
            }
            return value;
        } catch (URISyntaxException e) {
            throw new IOException("Malformed image URL", e);
        }
    }

    private static void throwIfStopped(CancellationChecker checker)
            throws InterruptedIOException {
        if (isStopped(checker)) {
            throw new InterruptedIOException("Offline sync was stopped");
        }
    }

    private static boolean isStopped(CancellationChecker checker) {
        return checker != null && checker.isStopped();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    public interface CancellationChecker {
        boolean isStopped();
    }

    public enum DownloadResult {
        CACHED,
        DOWNLOADED
    }
}
