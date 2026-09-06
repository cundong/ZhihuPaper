package com.cundong.izhihu.http;

import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.entity.NewsListEntity;
import com.cundong.izhihu.http.NewsApiClient.InvalidPayloadException;
import com.cundong.izhihu.http.NewsApiClient.Payload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NewsApiClientTest {

    @Test
    public void parseNewsList_acceptsCompleteExpectedPayload() throws Exception {
        String json = "{\"date\":\"20260812\",\"stories\":[{"
                + "\"title\":\"A story\",\"share_url\":\"https://daily.zhihu.com/story/1\","
                + "\"images\":[\"https://pic.example.com/1.jpg\"],\"type\":0,\"id\":1}]}";

        Payload<NewsListEntity> payload = NewsApiClient.parseNewsList(json, "20260812");

        assertEquals("20260812", payload.entity.date);
        assertEquals(1, payload.entity.stories.size());
        assertEquals(1L, payload.entity.stories.get(0).id);
    }

    @Test(expected = InvalidPayloadException.class)
    public void parseNewsList_rejectsUnexpectedDateBeforeStorage() throws Exception {
        String json = "{\"date\":\"20260811\",\"stories\":[{"
                + "\"title\":\"A story\",\"share_url\":\"https://daily.zhihu.com/story/1\","
                + "\"images\":[],\"type\":0,\"id\":1}]}";

        NewsApiClient.parseNewsList(json, "20260812");
    }

    @Test
    public void parseNewsDetail_acceptsMatchingBody() throws Exception {
        String json = "{\"body\":\"<p>Readable</p>\",\"title\":\"A story\","
                + "\"share_url\":\"https://daily.zhihu.com/story/9\",\"type\":0,"
                + "\"id\":9,\"css\":[],\"js\":[]}";

        Payload<NewsDetailEntity> payload = NewsApiClient.parseNewsDetail(json, 9L);

        assertEquals(9L, payload.entity.id);
        assertEquals("<p>Readable</p>", payload.entity.body);
    }

    @Test(expected = InvalidPayloadException.class)
    public void parseNewsDetail_rejectsMissingBodyBeforeStorage() throws Exception {
        String json = "{\"title\":\"A story\",\"type\":0,\"id\":9}";

        NewsApiClient.parseNewsDetail(json, 9L);
    }

    @Test(expected = InvalidPayloadException.class)
    public void parseNewsDetail_rejectsMismatchedArticleId() throws Exception {
        String json = "{\"body\":\"<p>Readable</p>\",\"title\":\"A story\","
                + "\"type\":0,\"id\":10}";

        NewsApiClient.parseNewsDetail(json, 9L);
    }

    @Test(expected = InvalidPayloadException.class)
    public void parseNewsList_rejectsNonWebImageUrls() throws Exception {
        String json = "{\"date\":\"20260812\",\"stories\":[{"
                + "\"title\":\"A story\",\"images\":[\"file:///private/data\"],"
                + "\"type\":0,\"id\":1}]}";

        NewsApiClient.parseNewsList(json, "20260812");
    }
}
