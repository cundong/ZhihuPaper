package com.cundong.izhihu.http;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;

import com.cundong.izhihu.exception.ZhihuIOException;
import com.cundong.izhihu.exception.ZhihuOtherException;
import com.cundong.izhihu.util.Logger;

/**
 * 类说明： 	HttpClient工具类
 * 
 * @date 	2014-1-4
 * @version 1.0
 */
public class HttpClientUtils {

	private static final String CHARSET = HTTP.UTF_8;

	/** 
     * 最大连接数 
     */  
    public final static int MAX_TOTAL_CONNECTIONS = 200;  
    
    /** 
     * 每个路由最大连接数 
     */  
    public final static int MAX_ROUTE_CONNECTIONS = 200;  

    /** 
     * 获取连接的最大等待时间
     */  
    public final static int RETRIVE_CONNECT_TIMEOUT = 6 * 1000;  
    
	private static final int RETRIED_TIME = 3;

	private static final int COMMON_TIMEOUT = 6 * 1000;

	private static Logger mLogger = Logger.getLogger();

	private static volatile DefaultHttpClient customerHttpClient = null;

	private HttpClientUtils() {

	}

	public static HttpClient getInstance(Context context) {
		if (customerHttpClient == null) {
			synchronized (HttpClientUtils.class) {
				if (customerHttpClient == null) {
					HttpParams params = new BasicHttpParams();

					// 设置一些基本参数
					HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
					HttpProtocolParams.setContentCharset(params, CHARSET);
					HttpProtocolParams.setUseExpectContinue(params, true);
					HttpProtocolParams.setUserAgent(params, System
							.getProperties().getProperty("http.agent")
							+ " Mozilla/5.0 Firefox/26.0");

					/* 连接超时 */
					HttpConnectionParams.setConnectionTimeout(params, COMMON_TIMEOUT);

					/* 请求超时 */
					HttpConnectionParams.setSoTimeout(params, COMMON_TIMEOUT);

					// 支持http和https两种模式
					SchemeRegistry schReg = new SchemeRegistry();
					schReg.register(new Scheme("http", PlainSocketFactory
							.getSocketFactory(), 80));
					schReg.register(new Scheme("https", getSSLSocketFactory(),
							443));

					// 使用线程安全的连接管理来创建HttpClient
					ClientConnectionManager conMgr = new ThreadSafeClientConnManager(
							params, schReg);

					customerHttpClient = new DefaultHttpClient(conMgr, params);
					customerHttpClient.setHttpRequestRetryHandler(requestRetryHandler);

					ConnManagerParams.setMaxTotalConnections(params, MAX_TOTAL_CONNECTIONS);  
					
					// 设置每个路由最大连接数  
			        ConnPerRouteBean connPerRoute = new ConnPerRouteBean(MAX_ROUTE_CONNECTIONS);  
			        ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
			        
			        // 设置获取连接的最大等待时间  
			        ConnManagerParams.setTimeout(params, RETRIVE_CONNECT_TIMEOUT);  
			        
					ConnectivityManager manager = (ConnectivityManager) context
							.getSystemService(Context.CONNECTIVITY_SERVICE);

					NetworkInfo networkinfo = manager.getActiveNetworkInfo();
					String net = networkinfo != null ? networkinfo
							.getExtraInfo() : null;

					// wifi的值为空
					if (!TextUtils.isEmpty(net)) {
						String proxyHost = getDefaultHost();

						if (!TextUtils.isEmpty(proxyHost)) {
							HttpHost proxy = new HttpHost(proxyHost, getDefaultPort(), "http");
							
							customerHttpClient.getParams().setParameter(
									ConnRoutePNames.DEFAULT_PROXY, proxy);
						}
					}
					
				}
			}
		}
		return customerHttpClient;
	}

	private static SSLSocketFactory getSSLSocketFactory() {
		
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());

			trustStore.load(null, null);

			SSLSocketFactory sslSocketFactory = new TrustAllSSLSocketFactory(
					trustStore);
			sslSocketFactory
					.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			return sslSocketFactory;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 异常自动恢复处理, 使用HttpRequestRetryHandler接口实现请求的异常恢复
	 * 
	 */
	private static HttpRequestRetryHandler requestRetryHandler = new HttpRequestRetryHandler() {

		@Override
		public boolean retryRequest(IOException exception, int executionCount,
				HttpContext context) {
			// 设置恢复策略，在发生异常时候将自动重试N次
			if (executionCount >= RETRIED_TIME) {
				// Do not retry if over max retry count
				Logger.getLogger().e(
						"Do not retry if over max retry count:"
								+ executionCount);
				return false;
			}

			if (exception instanceof NoHttpResponseException) {
				// Retry if the server dropped connection on us
				Logger.getLogger()
						.i("Retry if the server dropped connection on us:exception instanceof NoHttpResponseException");
				return true;
			}

			if (exception instanceof SSLHandshakeException) {
				// Do not retry on SSL handshake exception
				Logger.getLogger().e(
						"Do not retry on SSL handshake SSLHandshakeException ");
				return false;
			}

			HttpRequest request = (HttpRequest) context
					.getAttribute(ExecutionContext.HTTP_REQUEST);

			boolean idempotent = (request instanceof HttpEntityEnclosingRequest);
			if (!idempotent) {
				// Retry if the request is considered idempotent
				Logger.getLogger().i(
						"Retry if the request is considered idempotent");
				return true;
			}

			return false;
		}
	};

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
		
		if (params != null) {
			if (url.contains("?")) {
				url = url + "&" + UrlUtils.encodeUrl(params);
			} else {
				url = url + "?" + UrlUtils.encodeUrl(params);
			}
		}

		Logger.getLogger().d("GET:" + url);

		HttpGet request = new HttpGet(url);
		HttpClient httpClient = getInstance(context);

		// 解决：HttpClient WARNING: Cookie rejected: Illegal domain attribute
		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
				CookiePolicy.BROWSER_COMPATIBILITY);

		String response = "";

		try {
			response = httpClient.execute(request, new BasicResponseHandler());

			if (!TextUtils.isEmpty(response)) {
				responseListener.onComplete(response);
			} else {
				responseListener.onComplete("");
			}
		} catch (IOException e) {

			e.printStackTrace();

			if (e instanceof ConnectTimeoutException) {
				mLogger.e("error ConnectTimeoutException.");
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

		if (params != null) {
			if (url.contains("?")) {
				url = url + "&" + UrlUtils.encodeUrl(params);
			} else {
				url = url + "?" + UrlUtils.encodeUrl(params);
			}
		}

		Logger.getLogger().d("GET:" + url);

		HttpGet request = new HttpGet(url);
		HttpClient httpClient = getInstance(context);

		// 解决：HttpClient WARNING: Cookie rejected: Illegal domain attribute
		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
				CookiePolicy.BROWSER_COMPATIBILITY);
		
		return httpClient.execute(request, new BasicResponseHandler());
	}

	/**
	 * get a stream from web
	 * 
	 * @param context
	 * @param url
	 * @param params
	 */
	public static InputStream getStream(Context context, String url, Bundle params)
			throws IOException, Exception {

		if (params != null) {
			if (url.contains("?")) {
				url = url + "&" + UrlUtils.encodeUrl(params);
			} else {
				url = url + "?" + UrlUtils.encodeUrl(params);
			}
		}

		Logger.getLogger().d("GET:" + url);

		HttpGet request = new HttpGet(url);
		HttpClient httpClient = getInstance(context);

		// 解决：HttpClient WARNING: Cookie rejected: Illegal domain attribute
		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
				CookiePolicy.BROWSER_COMPATIBILITY);

		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
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
			ArrayList<NameValuePair> params, ResponseListener responseListener) {

		try {
			mLogger.d("POST:" + url);

			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params,
					CHARSET);

			HttpPost request = new HttpPost(url);

			request.setEntity(entity);

			HttpClient client = getInstance(context);
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

				HttpEntity resEntity = response.getEntity();

				String ret = (resEntity == null) ? null : EntityUtils.toString(
						resEntity, CHARSET);
				responseListener.onComplete(ret);
			} else {
				responseListener.onComplete("");
			}
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
	 * @param context
	 * @param url
	 * @param params
	 */
	public static InputStream post(Context context, String url,
			ArrayList<NameValuePair> params) {
		
		InputStream in = null;

		try {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params,
					CHARSET);

			HttpPost request = new HttpPost(url);

			request.setEntity(entity);

			HttpClient client = getInstance(context);
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

				HttpEntity resEntity = response.getEntity();

				in = (resEntity == null) ? null : resEntity.getContent();
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
			HttpPost request = new HttpPost(url);

			StringEntity entity = new StringEntity(jsonString);

			request.setEntity(entity);

			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");

			mLogger.d("POST:" + url);
			mLogger.d("BODY:" + jsonString);

			HttpClient client = getInstance(context);
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity resEntity = response.getEntity();
				String ret = (resEntity == null) ? null : EntityUtils.toString(
						resEntity, CHARSET);
				responseListener.onComplete(ret);
			} else {
				responseListener.onComplete("");
			}
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

		HttpPost request = new HttpPost(url);
		request.setEntity(new ByteArrayEntity(bytes));

		try {

			mLogger.d("POST:" + url);

			HttpClient client = getInstance(context);
			HttpResponse response = client.execute(request);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity resEntity = response.getEntity();
				String ret = (resEntity == null) ? null : EntityUtils.toString(
						resEntity, CHARSET);
				responseListener.onComplete(ret);
			} else {
				responseListener.onComplete("");
			}
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

	@SuppressWarnings("deprecation")
	private static String getDefaultHost() {
		return android.net.Proxy.getDefaultHost();
	}

	@SuppressWarnings("deprecation")
	private static int getDefaultPort() {
		return android.net.Proxy.getDefaultPort();
	}
}