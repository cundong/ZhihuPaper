package com.cundong.izhihu.task;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.entity.NewsListEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.entity.OfflineSyncSummary;
import com.cundong.izhihu.http.NewsApiClient;
import com.cundong.izhihu.http.NewsApiClient.Payload;
import com.cundong.izhihu.util.ArticleImageCache;
import com.cundong.izhihu.util.DateUtils;
import com.cundong.izhihu.util.OfflineStorageUtils;
import com.cundong.izhihu.util.PreferencesUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.InterruptedIOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Serial offline pipeline: four validated feeds, deduplicated article bodies,
 * then bounded image downloads. It intentionally performs no parallel network
 * fan-out, making cancellation, progress and failure accounting deterministic.
 */
public final class OfflineSyncWorker extends Worker {

    private static final int SOURCE_COUNT = 4;

    private final Context applicationContext;
    private final NewsApiClient.StopChecker stopChecker;
    private final ArticleImageCache.CancellationChecker imageCancellationChecker;

    public OfflineSyncWorker(@NonNull Context context,
            @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
        applicationContext = context.getApplicationContext();
        stopChecker = new NewsApiClient.StopChecker() {
            @Override
            public boolean isStopped() {
                return OfflineSyncWorker.this.isStopped();
            }
        };
        imageCancellationChecker = new ArticleImageCache.CancellationChecker() {
            @Override
            public boolean isStopped() {
                return OfflineSyncWorker.this.isStopped();
            }
        };
    }

    @NonNull
    @Override
    public Result doWork() {
        return OfflineStorageUtils.runSerializedOfflineMutation(
                new OfflineStorageUtils.OfflineMutation<Result>() {
                    @Override
                    public Result run() {
                        return doSerializedWork();
                    }
                });
    }

    /**
     * The complete pipeline owns the same mutation boundary as settings clear.
     * Cancellation can therefore finish at any checkpoint, but clear cannot
     * delete content until the Worker has stopped writing and released it.
     */
    private Result doSerializedWork() {
        String requestDate = getInputData().getString(OfflineSyncSummary.KEY_REQUEST_DATE);
        if (isBlank(requestDate)) {
            requestDate = DateUtils.getCurrentDate();
        }
        OfflineSyncSummary summary = new OfflineSyncSummary(SOURCE_COUNT, requestDate);
        ArticleImageCache imageCache = OfflineStorageUtils.getArticleImageCache(
                applicationContext);
        try {
            throwIfStopped();
            imageCache.maintain();

            NewsDataSource dataSource = ZhihuApplication.getDataSource();
            if (dataSource == null) {
                throw new IllegalStateException("News data source is not initialized");
            }
            NewsApiClient apiClient = new NewsApiClient(applicationContext, stopChecker);

            List<SourceRequest> sources = buildSources(requestDate);
            List<NewsEntity> stories = loadAndDeduplicateStories(
                    sources, apiClient, dataSource, summary);
            summary.setStoryTotal(stories.size());
            publishProgress(summary, OfflineSyncSummary.STAGE_ARTICLES, 0, stories.size());

            LinkedHashSet<String> imageUrls = new LinkedHashSet<String>();
            int articleCompleted = 0;
            for (NewsEntity story : stories) {
                throwIfStopped();
                Payload<NewsDetailEntity> detail = loadArticle(
                        story.id, apiClient, dataSource, summary);
                if (detail != null) {
                    collectImageUrls(detail.entity, imageUrls);
                }
                articleCompleted++;
                publishProgress(summary, OfflineSyncSummary.STAGE_ARTICLES,
                        articleCompleted, stories.size());
            }

            summary.setImageTotal(imageUrls.size());
            publishProgress(summary, OfflineSyncSummary.STAGE_IMAGES, 0, imageUrls.size());
            int imageCompleted = 0;
            for (String imageUrl : imageUrls) {
                throwIfStopped();
                try {
                    imageCache.download(imageUrl, imageCancellationChecker);
                    summary.recordImageAvailable();
                } catch (InterruptedIOException e) {
                    if (isStopped()) {
                        throw new StoppedException();
                    }
                    summary.recordImageFailed();
                } catch (Exception e) {
                    summary.recordImageFailed();
                }
                imageCompleted++;
                publishProgress(summary, OfflineSyncSummary.STAGE_IMAGES,
                        imageCompleted, imageUrls.size());
            }

            imageCache.maintain();
            String status = summary.resolveStatus();
            Data output = summary.outputData(OfflineSyncSummary.STAGE_FINISHED);
            if (OfflineSyncSummary.STATUS_COMPLETE.equals(status)) {
                // The completion marker is written by the durable worker so it
                // remains correct even when no Activity is alive to observe it.
                PreferencesUtils.putBoolean(applicationContext, requestDate, true);
                return Result.success(output);
            }
            if (OfflineSyncSummary.STATUS_PARTIAL.equals(status)) {
                return Result.success(output);
            }
            return retryOrFail(output);
        } catch (StoppedException e) {
            summary.recordWorkerFailure();
            imageCache.maintain();
            return Result.failure(summary.outputData(OfflineSyncSummary.STAGE_STOPPED));
        } catch (Exception e) {
            summary.recordWorkerFailure();
            imageCache.maintain();
            return retryOrFail(summary.outputData(OfflineSyncSummary.STAGE_FINISHED));
        }
    }

    private Result retryOrFail(Data output) {
        // WorkManager applies the scheduler's exponential backoff. Bound retries
        // so a broken upstream cannot keep waking the app forever.
        return getRunAttemptCount() < 2 ? Result.retry() : Result.failure(output);
    }

    private List<NewsEntity> loadAndDeduplicateStories(List<SourceRequest> sources,
            NewsApiClient apiClient, NewsDataSource dataSource,
            OfflineSyncSummary summary) throws StoppedException {
        List<NewsEntity> result = new ArrayList<NewsEntity>();
        Set<Long> seenIds = new HashSet<Long>();
        Set<String> seenUrls = new HashSet<String>();
        int completed = 0;
        publishProgress(summary, OfflineSyncSummary.STAGE_LISTS, 0, sources.size());
        for (SourceRequest source : sources) {
            throwIfStopped();
            Payload<NewsListEntity> payload = loadFeed(
                    source, apiClient, dataSource, summary);
            if (payload != null) {
                for (NewsEntity story : payload.entity.stories) {
                    long id = story.id;
                    String normalizedUrl = normalizeIdentityUrl(story.share_url);
                    if (seenIds.contains(id)
                            || (!isBlank(normalizedUrl) && seenUrls.contains(normalizedUrl))) {
                        continue;
                    }
                    seenIds.add(id);
                    if (!isBlank(normalizedUrl)) {
                        seenUrls.add(normalizedUrl);
                    }
                    result.add(story);
                }
            }
            completed++;
            publishProgress(summary, OfflineSyncSummary.STAGE_LISTS,
                    completed, sources.size());
        }
        return result;
    }

    private Payload<NewsListEntity> loadFeed(SourceRequest source,
            NewsApiClient apiClient, NewsDataSource dataSource,
            OfflineSyncSummary summary) throws StoppedException {
        try {
            Payload<NewsListEntity> network = apiClient.fetchNewsList(
                    source.url, source.expectedDate);
            throwIfStopped();
            dataSource.insertOrUpdateNewsList(Constants.NEWS_LIST,
                    network.entity.date, network.rawJson);
            summary.recordSourceAvailable(false);
            return network;
        } catch (InterruptedIOException e) {
            if (isStopped()) {
                throw new StoppedException();
            }
        } catch (Exception ignored) {
        }

        throwIfStopped();
        try {
            String cachedJson = dataSource.getContent(source.expectedDate);
            Payload<NewsListEntity> cached = NewsApiClient.parseNewsList(
                    cachedJson, source.expectedDate);
            summary.recordSourceAvailable(true);
            return cached;
        } catch (Exception ignored) {
            summary.recordSourceFailed();
            return null;
        }
    }

    private Payload<NewsDetailEntity> loadArticle(long storyId,
            NewsApiClient apiClient, NewsDataSource dataSource,
            OfflineSyncSummary summary) throws StoppedException {
        String cacheKey = "detail_" + storyId;
        try {
            Payload<NewsDetailEntity> network = apiClient.fetchNewsDetail(
                    Constants.Url.URL_DETAIL + storyId, storyId);
            throwIfStopped();
            dataSource.insertOrUpdateNewsList(Constants.NEWS_DETAIL,
                    cacheKey, network.rawJson);
            summary.recordStoryAvailable(false);
            return network;
        } catch (InterruptedIOException e) {
            if (isStopped()) {
                throw new StoppedException();
            }
        } catch (Exception ignored) {
        }

        throwIfStopped();
        try {
            String cachedJson = dataSource.getContent(cacheKey);
            Payload<NewsDetailEntity> cached = NewsApiClient.parseNewsDetail(cachedJson, storyId);
            summary.recordStoryAvailable(true);
            return cached;
        } catch (Exception ignored) {
            summary.recordStoryFailed();
            return null;
        }
    }

    private void collectImageUrls(NewsDetailEntity detail, Set<String> imageUrls) {
        addRemoteImage(detail.image, imageUrls);
        Elements images = Jsoup.parseBodyFragment(detail.body).getElementsByTag("img");
        for (Element image : images) {
            addRemoteImage(image.attr("src"), imageUrls);
        }
    }

    private void addRemoteImage(String url, Set<String> imageUrls) {
        if (isBlank(url)) {
            return;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("//")
                || trimmed.regionMatches(true, 0, "https://", 0, 8)
                || trimmed.regionMatches(true, 0, "http://", 0, 7)) {
            imageUrls.add(trimmed);
        }
    }

    private List<SourceRequest> buildSources(String requestDate) {
        Date baseDate = parseRequestDate(requestDate);
        String day0 = compactDate(baseDate, 0);
        String day1 = compactDate(baseDate, -1);
        String day2 = compactDate(baseDate, -2);
        String day3 = compactDate(baseDate, -3);

        List<SourceRequest> sources = new ArrayList<SourceRequest>(SOURCE_COUNT);
        sources.add(new SourceRequest(Constants.Url.URL_LATEST, day0));
        sources.add(new SourceRequest(Constants.Url.URLDEFORE + day0, day1));
        sources.add(new SourceRequest(Constants.Url.URLDEFORE + day1, day2));
        sources.add(new SourceRequest(Constants.Url.URLDEFORE + day2, day3));
        return sources;
    }

    private Date parseRequestDate(String requestDate) {
        SimpleDateFormat format = new SimpleDateFormat(DateUtils.YYYY_MM_DD, Locale.US);
        format.setLenient(false);
        try {
            return format.parse(requestDate);
        } catch (ParseException e) {
            return new Date();
        }
    }

    private String compactDate(Date baseDate, int dayOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(baseDate);
        calendar.add(Calendar.DAY_OF_YEAR, dayOffset);
        return new SimpleDateFormat(DateUtils.YYYYMMDD, Locale.US)
                .format(calendar.getTime());
    }

    private String normalizeIdentityUrl(String url) {
        if (isBlank(url)) {
            return "";
        }
        String result = url.trim();
        int fragment = result.indexOf('#');
        if (fragment >= 0) {
            result = result.substring(0, fragment);
        }
        while (result.endsWith("/") && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private void publishProgress(OfflineSyncSummary summary, String stage,
            int completed, int total) {
        setProgressAsync(summary.progressData(stage, completed, total));
    }

    private void throwIfStopped() throws StoppedException {
        if (isStopped()) {
            throw new StoppedException();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static final class SourceRequest {
        final String url;
        final String expectedDate;

        SourceRequest(String url, String expectedDate) {
            this.url = url;
            this.expectedDate = expectedDate;
        }
    }

    private static final class StoppedException extends Exception {
    }
}
