package com.cundong.izhihu.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

@SuppressWarnings("deprecation") // API 21-22 requires NetworkInfo; newer devices use NetworkCapabilities.
public final class NetWorkHelper {

	private static final String TYPE_MOBILE = "1";
	private static final String TYPE_WIFI = "2";
	private static final String TYPE_UNKNOWN = "Unknown";

	private NetWorkHelper() {
	}

	/**
	 * 判断是不是wifi网络状态
	 * 
	 * @param paramContext
	 * @return
	 */
	public static boolean isWifi(Context paramContext) {
		return TYPE_WIFI.equals(getNetType(paramContext)[0]);
	}

	/**
	 * 判断是不是2/3G网络状态
	 * 
	 * @param paramContext
	 * @return
	 */
	public static boolean isMobile(Context paramContext) {
		return TYPE_MOBILE.equals(getNetType(paramContext)[0]);
	}

	public static boolean isNetAvailable(Context paramContext) {
		String type = getNetType(paramContext)[0];
		return TYPE_MOBILE.equals(type) || TYPE_WIFI.equals(type);
	}

	/**
	 * 获取当前网络状态 返回2代表wifi,1代表2G/3G
	 * 
	 * @param paramContext
	 * @return
	 */
	public static String[] getNetType(Context paramContext) {
		String[] arrayOfString = { TYPE_UNKNOWN, TYPE_UNKNOWN };
		ConnectivityManager localConnectivityManager = (ConnectivityManager) paramContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (localConnectivityManager == null) {
			return arrayOfString;
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return getLegacyNetType(localConnectivityManager, arrayOfString);
		}
		Network activeNetwork = localConnectivityManager.getActiveNetwork();
		NetworkCapabilities capabilities = activeNetwork == null ? null
				: localConnectivityManager.getNetworkCapabilities(activeNetwork);
		if (capabilities == null
				|| !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
			return arrayOfString;
		}
		if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
			arrayOfString[0] = TYPE_WIFI;
		} else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
			arrayOfString[0] = TYPE_MOBILE;
			arrayOfString[1] = "Cellular";
		}
		return arrayOfString;
	}

	private static String[] getLegacyNetType(ConnectivityManager manager, String[] result) {
		android.net.NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
		if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
			return result;
		}
		if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			result[0] = TYPE_WIFI;
		} else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
			result[0] = TYPE_MOBILE;
			result[1] = activeNetworkInfo.getSubtypeName();
		}
		return result;
	}
}
