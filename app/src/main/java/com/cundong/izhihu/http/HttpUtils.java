package com.cundong.izhihu.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.cundong.izhihu.exception.ZhihuIOException;
import com.cundong.izhihu.exception.ZhihuOtherException;
import com.cundong.izhihu.util.Logger;

import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 类说明：	HTTP 工具类（OkHttp 实现）
 *
 * 历史：原实现基于 Apache HttpClient（DefaultHttpClient + ThreadSafeClientConnManager）。
 * Apache HttpClient 在 API 23 起被移出平台，SDK 里的 org.apache.http.legacy.jar 只是
 * 一个所有方法都抛 RuntimeException("Stub!") 的桩，因此曾经不得不打包真实的
 * httpclient 4.1.3（并为此关掉 lint 的 DuplicatePlatformClasses）。现已整体迁移到
 * OkHttp：连接池、超时、重试、gzip、HTTP/2 与现代 TLS 都由 OkHttp 负责，
 * 对外方法签名保持不变，调用方无需改动。
 *
 * @date	2014-1-4
 * @version	2.0
 */
public class HttpUtils {

	private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

	private static final MediaType MEDIA_TYPE_OCTET_STREAM = MediaType.parse("application/octet-stream");

	/**
	 * 最大连接数（OkHttp 的调度上限，等价于旧的 MAX_TOTAL_CONNECTIONS）
	 */
	public final static int MAX_TOTAL_CONNECTIONS = 16;

	/**
	 * 每个 host 的最大并发请求数（等价于旧的 MAX_ROUTE_CONNECTIONS）
	 */
	public final static int MAX_ROUTE_CONNECTIONS = 6;

	/**
	 * 空闲连接保活时长
	 */
	private final static int KEEP_ALIVE_DURATION_MINUTES = 5;

	/**
	 * 空闲连接池容量
	 */
	private final static int MAX_IDLE_CONNECTIONS = 5;

	private static final int CONNECT_TIMEOUT = 10 * 1000;
	private static final int IO_TIMEOUT = 20 * 1000;

	private static Logger mLogger = Logger.getLogger();

	private static volatile OkHttpClient sClient = null;

	private HttpUtils() {

	}

	/**
	 * 获取全局共享的 OkHttpClient。
	 *
	 * OkHttpClient 自身就是连接池与线程池的持有者，官方建议全进程共享单例，
	 * 这一点和旧代码里共享 DefaultHttpClient 的意图一致。
	 *
	 * @param context 保留该参数是为了兼容既有调用方，OkHttp 无需 Context
	 */
	public static OkHttpClient getInstance(Context context) {
		if (sClient == null) {
			synchronized (HttpUtils.class) {
				if (sClient == null) {
					String userAgent = System.getProperties().getProperty("http.agent")
							+ " Mozilla/5.0 Firefox/26.0";

					ConnectionPool connectionPool = new ConnectionPool(MAX_IDLE_CONNECTIONS,
							KEEP_ALIVE_DURATION_MINUTES, TimeUnit.MINUTES);

					OkHttpClient client = new OkHttpClient.Builder()
							.connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
							.readTimeout(IO_TIMEOUT, TimeUnit.MILLISECONDS)
							.writeTimeout(IO_TIMEOUT, TimeUnit.MILLISECONDS)
							.connectionPool(connectionPool)
							// 旧的 HttpRequestRetryHandler 会在连接被服务端断开等 IO 异常时重试；
							// OkHttp 的 retryOnConnectionFailure 覆盖同样的场景（换路由/换连接重投），
							// 且天然只对幂等重试安全的情况生效。
							.retryOnConnectionFailure(true)
							.addInterceptor(new UserAgentInterceptor(userAgent))
							.build();

					// This client is also used by Glide. Six requests per host keeps
					// thumbnail loading responsive while preventing the old 200-way
					// offline fan-out from starving foreground traffic.
					client.dispatcher().setMaxRequests(MAX_TOTAL_CONNECTIONS);
					client.dispatcher().setMaxRequestsPerHost(MAX_ROUTE_CONNECTIONS);

					// 代理：旧代码在非 WiFi 下读取 android.net.Proxy.getDefaultHost()（API 11 起已废弃，
					// 且在现代系统上恒返回 null）。OkHttp 默认走 ProxySelector.getDefault()，
					// 会自动遵循系统/运营商代理设置，因此这里不再手工设置。

					sClient = client;
				}
			}
		}
		return sClient;
	}

	/**
	 * 统一给请求补上 UA。旧实现通过 HttpProtocolParams.setUserAgent 设置。
	 */
	private static class UserAgentInterceptor implements okhttp3.Interceptor {

		private final String mUserAgent;

		UserAgentInterceptor(String userAgent) {
			mUserAgent = userAgent;
		}

		@Override
		public Response intercept(Chain chain) throws IOException {
			Request request = chain.request().newBuilder()
					.header("User-Agent", mUserAgent)
					.build();
			return chain.proceed(request);
		}
	}

	private static String buildUrl(String url, Bundle params) {
		if (params == null) {
			return url;
		}

		String query = UrlUtils.encodeUrl(params);
		if (TextUtils.isEmpty(query)) {
			return url;
		}

		return url.contains("?") ? (url + "&" + query) : (url + "?" + query);
	}

	/**
	 * 执行请求并把 body 读成字符串。
	 *
	 * 等价于旧实现里的 BasicResponseHandler：非 2xx 抛 IOException，
	 * body 为空时返回空串。
	 */
	private static String executeForString(OkHttpClient client, Request request) throws IOException {
		Call call = client.newCall(request);

		Response response = call.execute();
		try {
			ResponseBody body = response.body();

			if (!response.isSuccessful()) {
				throw new IOException("Unexpected response code: " + response.code()
						+ " for " + request.url());
			}

			return body == null ? "" : body.string();
		} finally {
			response.close();
		}
	}

	/**
	 * get请求
	 *
	 * @param context
	 * @param url
	 * @param params
	 * @param responseListener
	 */
	public static void get(final Context context, String url, Bundle params,
			ResponseListener responseListener) {

		url = buildUrl(url, params);

		Logger.getLogger().d("GET:" + url);

		try {
			Request request = new Request.Builder().url(url).get().build();

			String response = executeForString(getInstance(context), request);

			if (!TextUtils.isEmpty(response)) {
				responseListener.onComplete(response);
			} else {
				responseListener.onComplete("");
			}
		} catch (IOException e) {

			e.printStackTrace();

			if (e instanceof SocketTimeoutException) {
				mLogger.e("error SocketTimeoutException.");
			}

			mLogger.e("error begin.");
			mLogger.e("error params:");

			mLogger.e("error.url(GET):" + url);

			mLogger.e("error.getMessage:" + e.getMessage());

			mLogger.e("error end.");

			responseListener.onFail(new ZhihuIOException(
					"request url IOException", e));
		} catch (Exception e) {

			e.printStackTrace();
			mLogger.e("error:" + e.getMessage());

			responseListener.onFail(new ZhihuOtherException(
					"request url Exception", e));
		}
	}

	/**
	 * get请求，返回字符串
	 *
	 * @param context
	 * @param url
	 * @param params
	 */
	public static String get(Context context, String url, Bundle params)
			throws IOException, Exception {

		url = buildUrl(url, params);

		Logger.getLogger().d("GET:" + url);

		Request request = new Request.Builder().url(url).get().build();

		return executeForString(getInstance(context), request);
	}

	/**
	 * get a stream from web
	 *
	 * 注意：返回的流由调用方负责关闭，关闭该流即归还底层连接。
	 *
	 * @param context
	 * @param url
	 * @param params
	 */
	public static InputStream getStream(Context context, String url, Bundle params)
			throws IOException, Exception {

		url = buildUrl(url, params);

		Logger.getLogger().d("GET:" + url);

		Request request = new Request.Builder().url(url).get().build();

		Response response = getInstance(context).newCall(request).execute();

		ResponseBody body = response.body();

		if (!response.isSuccessful() || body == null) {
			response.close();
			throw new IOException("Unexpected response code: " + response.code()
					+ " for " + url);
		}

		return body.byteStream();
	}

	/**
	 * post请求
	 *
	 * @param context
	 * @param url
	 * @param params
	 * @param responseListener
	 */
	public static void post(Context context, String url,
			Map<String, String> params, ResponseListener responseListener) {

		try {
			mLogger.d("POST:" + url);

			FormBody.Builder formBuilder = new FormBody.Builder();
			if (params != null) {
				for (Map.Entry<String, String> entry : params.entrySet()) {
					if (entry.getKey() != null && entry.getValue() != null) {
						formBuilder.add(entry.getKey(), entry.getValue());
					}
				}
			}

			Request request = new Request.Builder()
					.url(url)
					.post(formBuilder.build())
					.build();

			responseListener.onComplete(executeForString(getInstance(context), request));
		} catch (IOException e) {

			e.printStackTrace();

			mLogger.e("error.url(POST):" + url);

			responseListener.onFail(new ZhihuIOException(
					"request url IOException", e));
		} catch (Exception e) {

			e.printStackTrace();
			mLogger.e("error:" + e.getMessage());
			responseListener.onFail(new ZhihuOtherException(
					"request url Exception", e));
		}
	}

	/**
	 * post请求，获取一个stream
	 *
	 * 注意：返回的流由调用方负责关闭。
	 *
	 * @param context
	 * @param url
	 * @param params
	 */
	public static InputStream post(Context context, String url,
			Map<String, String> params) {

		InputStream in = null;

		try {
			FormBody.Builder formBuilder = new FormBody.Builder();
			if (params != null) {
				for (Map.Entry<String, String> entry : params.entrySet()) {
					if (entry.getKey() != null && entry.getValue() != null) {
						formBuilder.add(entry.getKey(), entry.getValue());
					}
				}
			}

			Request request = new Request.Builder()
					.url(url)
					.post(formBuilder.build())
					.build();

			Response response = getInstance(context).newCall(request).execute();
			ResponseBody body = response.body();

			if (response.isSuccessful() && body != null) {
				in = body.byteStream();
			} else {
				response.close();
			}
		} catch (IOException e) {

			e.printStackTrace();
			mLogger.e("error:" + e.getMessage());
		} catch (Exception e) {

			e.printStackTrace();
			mLogger.e("error:" + e.getMessage());
		}

		return in;
	}

	/**
	 * post一个字符串到服务器
	 *
	 * @param context
	 * @param url
	 * @param jsonString
	 * @param responseListener
	 */
	public static void post(Context context, String url, String jsonString,
			ResponseListener responseListener) {

		try {
			mLogger.d("POST:" + url);
			mLogger.d("BODY:" + jsonString);

			Request request = new Request.Builder()
					.url(url)
					.header("Accept", "application/json")
					.post(RequestBody.create(jsonString, MEDIA_TYPE_JSON))
					.build();

			responseListener.onComplete(executeForString(getInstance(context), request));
		} catch (IOException e) {

			e.printStackTrace();

			mLogger.e("error begin.");
			mLogger.e("error POST BODY:");

			mLogger.e("error.url(POST):" + url);

			mLogger.e("error.getMessage:" + e.getMessage());

			mLogger.e("error end.");

			responseListener.onFail(new ZhihuIOException(
					"request url IOException", e));
		} catch (Exception e) {

			e.printStackTrace();
			mLogger.e("error:" + e.getMessage());
			responseListener.onFail(new ZhihuOtherException(
					"request url Exception", e));
		}
	}

	/**
	 * post请求一个byte[] 数组到服务器
	 *
	 * @param context
	 * @param url
	 * @param bytes
	 * @param responseListener
	 */
	public static void post(Context context, String url, byte[] bytes,
			ResponseListener responseListener) {

		try {
			mLogger.d("POST:" + url);

			Request request = new Request.Builder()
					.url(url)
					.post(RequestBody.create(bytes, MEDIA_TYPE_OCTET_STREAM))
					.build();

			responseListener.onComplete(executeForString(getInstance(context), request));
		} catch (IOException e) {

			e.printStackTrace();

			mLogger.e("error.url(POST):" + url);
			responseListener.onFail(new ZhihuIOException(
					"request url IOException", e));
		} catch (Exception e) {

			e.printStackTrace();
			mLogger.e("error:" + e.getMessage());
			responseListener.onFail(new ZhihuOtherException(
					"request url Exception", e));
		}
	}
}
