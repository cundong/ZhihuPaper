package com.cundong.izhihu.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;

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

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;

import com.cundong.izhihu.exception.MyCosIOException;
import com.cundong.izhihu.exception.MyCosOtherException;
import com.cundong.izhihu.util.Logger;

/**
 * 类说明： HttpClient工具类
 * 
 * @date 2014-1-4
 * @version 1.0
 */
public class HttpClientUtils {

	private static final String CHARSET = HTTP.UTF_8;

	private static final int RETRIED_TIME = 3;
	
	private static DefaultHttpClient customerHttpClient = null;

	private static Logger mLogger = null;

	private HttpClientUtils() {
		
	}
	
	public static synchronized HttpClient getInstance(Context context) {
		if (null == customerHttpClient) {

			HttpParams params = new BasicHttpParams();

			// 设置一些基本参数
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, CHARSET);
			HttpProtocolParams.setUseExpectContinue(params, true);
			HttpProtocolParams.setUserAgent(params, System.getProperties().getProperty("http.agent") + " Mozilla/5.0 Firefox/26.0");

			// 超时设置
			/* 从连接池中取连接的超时时间 */
			ConnManagerParams.setTimeout(params, 10 * 1000);

			/* 连接超时 */
			HttpConnectionParams.setConnectionTimeout(params, 10 * 1000);

			/* 请求超时 */
			HttpConnectionParams.setSoTimeout(params, 10 * 1000);

			// 支持http和https两种模式
			SchemeRegistry schReg = new SchemeRegistry();
			schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schReg.register(new Scheme("https", getSSLSocketFactory(), 443));

			// 使用线程安全的连接管理来创建HttpClient
			ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);

			customerHttpClient = new DefaultHttpClient(conMgr, params);
			customerHttpClient.setHttpRequestRetryHandler(requestRetryHandler);
			ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo networkinfo = manager.getActiveNetworkInfo();
			String net = networkinfo != null ? networkinfo.getExtraInfo() : null;

			// wifi的值为空
			if (!TextUtils.isEmpty(net)) {
				String proxyHost = getDefaultHost();

				if (!TextUtils.isEmpty(proxyHost)) {
					HttpHost proxy = new HttpHost(proxyHost, getDefaultPort(), "http");

					customerHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
				}
			}
		}
		return customerHttpClient;
	}

	private static SSLSocketFactory getSSLSocketFactory() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

			trustStore.load(null, null);

			SSLSocketFactory sslSocketFactory = new TrustAllSSLSocketFactory(trustStore);
			sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

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
		public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
			// 设置恢复策略，在发生异常时候将自动重试N次
			if (executionCount >= RETRIED_TIME) {
				// Do not retry if over max retry count
				Logger.getLogger().e("Do not retry if over max retry count:" + executionCount);
				return false;
			}

			if (exception instanceof NoHttpResponseException) {
				// Retry if the server dropped connection on us
				Logger.getLogger().i("Retry if the server dropped connection on us:exception instanceof NoHttpResponseException");
				return true;
			}

			if (exception instanceof SSLHandshakeException) {
				// Do not retry on SSL handshake exception
				Logger.getLogger().e("Do not retry on SSL handshake SSLHandshakeException ");
				return false;
			}

			HttpRequest request = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);

			boolean idempotent = (request instanceof HttpEntityEnclosingRequest);
			if (!idempotent) {
				// Retry if the request is considered idempotent
				Logger.getLogger().i("Retry if the request is considered idempotent");
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
	 * @throws NetworkErrorException 
	 */
	public static void request(final Context context, String url, Bundle params, ResponseListener responseListener){

		mLogger = Logger.getLogger();

		if (params != null) {
			if (url.contains("?")) {
				url = url + "&" + UrlUtils.encodeUrl(params);
			} else {
				url = url + "?" + UrlUtils.encodeUrl(params);
			}
		}
		
		Logger.getLogger().d( "GET:" + url);
		
		HttpGet request = new HttpGet(url);
		HttpClient httpClient = getInstance(context);

		// 解决：HttpClient WARNING: Cookie rejected: Illegal domain attribute
		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

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
			
			if(e instanceof ConnectTimeoutException){
				mLogger.e("error ConnectTimeoutException.");
			}
			
			mLogger.e("error begin.");
			mLogger.e("error params:");
			
			mLogger.e( "error.url(GET):" + url);
			
			mLogger.e("error.getMessage:" + e.getMessage());
			
			mLogger.e("error end.");
			
			responseListener.onFail(new MyCosIOException("request url IOException", e));
		} catch (Exception e) {

			e.printStackTrace();
			mLogger.e("error:" + e.getMessage());
			
			responseListener.onFail(new MyCosOtherException("request url Exception", e));
		}
	}

	/**
	 * get请求
	 * 
	 * @param context
	 * @param url
	 * @param params
	 * @param responseListener
	 * @throws IOException
	 * @throws Exception
	 */
	public static InputStream request(Context context, String url, Bundle params) throws IOException, Exception {

		if (params != null) {
			if (url.contains("?")) {
				url = url + "&" + UrlUtils.encodeUrl(params);
			} else {
				url = url + "?" + UrlUtils.encodeUrl(params);
			}
		}
		
		Logger.getLogger().d( "GET:" + url);
		
		HttpGet request = new HttpGet(url);
		HttpClient httpClient = getInstance(context);

		// 解决：HttpClient WARNING: Cookie rejected: Illegal domain attribute
		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

		HttpResponse response;
		InputStream in = null;
		response = httpClient.execute(request);
		in = response.getEntity().getContent();

		return in;
	}

	/**
	 * post请求
	 * 
	 * @param context
	 * @param url
	 * @param params
	 * @param responseListener
	 */
	public static void post(Context context, String url, ArrayList<NameValuePair> params, 
			ResponseListener responseListener) {

		mLogger = Logger.getLogger();

		try {
			
			mLogger.d( "POST:" + url);
			
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, CHARSET);

			HttpPost request = new HttpPost(url);

			request.setEntity(entity);

			HttpClient client = getInstance(context);
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				
				HttpEntity resEntity = response.getEntity();
				
				String ret = (resEntity == null) ? null : EntityUtils.toString(resEntity, CHARSET);
				responseListener.onComplete(ret);
			} else {
				responseListener.onComplete("");
			}
		} catch (IOException e) {

			e.printStackTrace();
			
			mLogger.e("error begin.");
			mLogger.e("error params:");
			
			mLogger.e( "error.url(POST):" + url);
			
			mLogger.e("error.getMessage:" + e.getMessage());
			
			mLogger.e("error end.");
			
			responseListener.onFail(new MyCosIOException("request url IOException", e));
		} catch (Exception e) {

			e.printStackTrace();
			mLogger.e("error:" + e.getMessage());
			responseListener.onFail(new MyCosOtherException("request url Exception", e));
		}
	}

	public static InputStream post(Context context, String url, ArrayList<NameValuePair> params) {

		mLogger = Logger.getLogger();

		InputStream in = null;
		
		try {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, CHARSET);

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
	 * post string
	 * 
	 * @param context
	 * @param url
	 * @param responseListener
	 */
	public static void post(Context context, String url, String jsonString, ResponseListener responseListener) {
		mLogger = Logger.getLogger();

		try {
			HttpPost request = new HttpPost(url);

			StringEntity entity = new StringEntity(jsonString);

			request.setEntity(entity);

			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");

			mLogger.d( "POST:" + url);
			mLogger.d( "BODY:" + jsonString);
			
			HttpClient client = getInstance(context);
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity resEntity = response.getEntity();
				String ret = (resEntity == null) ? null : EntityUtils.toString(resEntity, CHARSET);
				responseListener.onComplete(ret);
			} else {
				responseListener.onComplete("");
			}
		} catch (IOException e) {

			e.printStackTrace();
			
			mLogger.e("error begin.");
			mLogger.e("error POST BODY:");

			mLogger.e( "error.url(POST):" + url);
			
			mLogger.e("error.getMessage:" + e.getMessage());
			
			mLogger.e("error end.");
			
			responseListener.onFail(new MyCosIOException("request url IOException", e));
		} catch (Exception e) {

			e.printStackTrace();
			mLogger.e("error:" + e.getMessage());
			responseListener.onFail(new MyCosOtherException("request url Exception", e));
		}
	}

	/**
	 * post请求
	 * 
	 * @param context
	 * @param url
	 * @param bytes
	 * @param responseListener
	 */
	public static void post(Context context, String url, byte[] bytes, ResponseListener responseListener) {

		HttpPost request = new HttpPost(url);
		request.setEntity(new ByteArrayEntity(bytes));

		try {
			
			mLogger.d( "POST:" + url);
			
			HttpClient client = getInstance(context);
			HttpResponse response = client.execute(request);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity resEntity = response.getEntity();
				String ret = (resEntity == null) ? null : EntityUtils.toString(resEntity, CHARSET);
				responseListener.onComplete(ret);
			} else {
				responseListener.onComplete("");
			}
		} catch (IOException e) {

			e.printStackTrace();
			
			mLogger.e("error begin.");
			mLogger.e("error POST BODY:");

			mLogger.e( "error.url(POST):" + url);
			
			mLogger.e("error.getMessage:" + e.getMessage());
			
			mLogger.e("error end.");
			
			responseListener.onFail(new MyCosIOException("request url IOException", e));
		} catch (Exception e) {

			e.printStackTrace();
			mLogger.e("error:" + e.getMessage());
			responseListener.onFail(new MyCosOtherException("request url Exception", e));
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

	/**
	 * 获取Http响应头字段
	 * 
	 * @param http
	 * @return
	 */
	public static LinkedHashMap<String, String> getHttpResponseHeader(HttpURLConnection http) {
		LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
		for (int i = 0;; i++) {
			String mine = http.getHeaderField(i);
			if (mine == null)
				break;
			header.put(http.getHeaderFieldKey(i), mine);
		}
		return header;
	}

	/**
	 * 从Http响应头字段中获取ETag
	 * 
	 * @param http
	 * @return
	 */
	public static String getETag(HttpURLConnection http) {
		LinkedHashMap<String, String> headerMap = getHttpResponseHeader(http);
		for (LinkedHashMap.Entry<String, String> entry : headerMap.entrySet()) {
			String key = entry.getKey() != null ? entry.getKey() : "";
			if (key.equals("ETag")) {
				return entry.getValue();
			}
		}
		return "";
	}
}