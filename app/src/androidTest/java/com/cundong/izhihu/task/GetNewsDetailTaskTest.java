package com.cundong.izhihu.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.db.BaseDataSource;
import com.cundong.izhihu.db.DatabaseHelper;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.db.NewsFavoriteDataSource;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.http.NewsApiClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class GetNewsDetailTaskTest {

	private Context context;
	private String databaseName;
	private DatabaseHelper helper;
	private NewsDataSource detailCache;
	private NewsFavoriteDataSource library;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		databaseName = "zhihu-paper-detail-fallback-" + UUID.randomUUID() + ".db";
		context.deleteDatabase(databaseName);
		helper = DatabaseHelper.createForTesting(context, databaseName);
		detailCache = new NewsDataSource(helper);
		library = new NewsFavoriteDataSource(helper);
	}

	@After
	public void tearDown() {
		if (helper != null) {
			helper.close();
		}
		context.deleteDatabase(databaseName);
	}

	@Test
	public void networkAndInvalidDetailCacheFallBackToMatchingLibraryHtml() throws Exception {
		assertTrue(library.saveLibraryItemAtomically("42", "Library title",
				"https://pic.example.com/42.jpg", "https://daily.zhihu.com/story/42",
				true, "note", Arrays.asList("offline"), "<p>Saved HTML</p>"));
		detailCache.insertOrUpdateNewsList(Constants.NEWS_DETAIL, "detail_42",
				"{\"id\":42,\"title\":\"missing body\"}");

		FailingNetworkTask task = new FailingNetworkTask(context, detailCache, library);
		String result = task.run("42");

		assertTrue(task.wasSuccessful());
		NewsApiClient.Payload<NewsDetailEntity> payload =
				NewsApiClient.parseNewsDetail(result, 42L);
		assertEquals(42L, payload.entity.id);
		assertEquals("Library title", payload.entity.title);
		assertEquals("<p>Saved HTML</p>", payload.entity.body);
	}

	@Test
	public void fallbackDoesNotUseAnotherLibraryArticleOrMalformedCache() {
		assertTrue(library.saveLibraryItemAtomically("43", "Wrong article",
				null, null, true, "", Collections.<String>emptyList(),
				"<p>Wrong body</p>"));
		detailCache.insertOrUpdateNewsList(Constants.NEWS_DETAIL, "detail_42", "not-json");

		FailingNetworkTask task = new FailingNetworkTask(context, detailCache, library);

		assertNull(task.run("42"));
		assertFalse(task.wasSuccessful());
	}

	private static final class FailingNetworkTask extends GetNewsDetailTask {
		private final NewsDataSource detailCache;
		private final NewsFavoriteDataSource library;

		FailingNetworkTask(Context context, NewsDataSource detailCache,
				NewsFavoriteDataSource library) {
			super(context, null);
			this.detailCache = detailCache;
			this.library = library;
		}

		String run(String newsId) {
			return doInBackground(Collections.singletonList(newsId));
		}

		boolean wasSuccessful() {
			return isRefreshSuccess;
		}

		@Override
		protected String getUrl(String url) throws IOException {
			throw new IOException("offline");
		}

		@Override
		protected BaseDataSource getDataSource() {
			return detailCache;
		}

		@Override
		protected NewsFavoriteDataSource getLibraryDataSource() {
			return library;
		}
	}
}
