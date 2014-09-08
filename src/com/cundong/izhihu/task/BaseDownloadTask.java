package com.cundong.izhihu.task;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

public abstract class BaseDownloadTask extends MyAsyncTask<String, Void, String> {

	protected ResponseListener mListener = null;
	
	public BaseDownloadTask(ResponseListener listener) {
		mListener = listener;
	}
	
	protected String getUrl(String url) throws IOException {

		HttpClient client = new DefaultHttpClient();

		HttpParams params = client.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 5 * 1000);
		HttpConnectionParams.setSoTimeout(params, 5 * 1000);

		try {
			HttpResponse httpResponse = client.execute(new HttpGet(url));
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
			}
		} finally {
			client.getConnectionManager().shutdown();
		}

		return "";
	}
}