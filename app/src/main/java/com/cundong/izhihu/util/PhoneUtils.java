package com.cundong.izhihu.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;

/**
 * 类说明： 手机工具类
 * 
 * @date 2012-2-27
 * @version 1.0
 */
public class PhoneUtils {

	// 说明：原先的 getIMEI/getRealIMEI/getMAC/getMobileNumber 已随 umeng 埋点一起移除。
	// 它们依赖 READ_PHONE_STATE 与 ACCESS_WIFI_STATE：TelephonyManager#getDeviceId()
	// 在 API 29+ 直接抛 SecurityException，WifiInfo#getMacAddress() 从 API 23 起
	// 恒返回 02:00:00:00:00:00。应用本身从未使用这些标识，两个权限也已从
	// AndroidManifest 中删除。

	/**
	 * 获取手机IP地址 获取失败，返回"127.0.0.1"
	 * 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static String getIPAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						String ip = Formatter.formatIpAddress(inetAddress
								.hashCode());
						return ip;
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		return "127.0.0.1";
	}

	/**
	 * 最低支持的SDK版本
	 * 
	 * @return
	 */
	public static int getSDKVersion() {
		return Build.VERSION.SDK_INT;
	}

	/**
	 * 当前程序版本获取
	 * 
	 * @param context
	 * @return
	 */
	public static PackageInfo getPackageInfo(Context context) {
		PackageInfo packInfo = null;
		PackageManager pm = context.getPackageManager();
		try {
			packInfo = pm.getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return packInfo;
	}

	public static String getApplicationName(Context context) {
		PackageManager packageManager = null;
		ApplicationInfo applicationInfo = null;
		try {
			packageManager = context.getPackageManager();
			applicationInfo = packageManager.getApplicationInfo(
					context.getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			applicationInfo = null;
		}
		String applicationName = (String) packageManager
				.getApplicationLabel(applicationInfo);
		return applicationName;
	}

	/**
	 * 获取手机设备描述（包括品牌、型号等）
	 * 
	 * @param
	 * @return
	 */
	public static String getMobileInfo(Context mContext) {

		StringBuffer sb = new StringBuffer();
		sb.append(android.os.Build.MANUFACTURER).append(" ")
				.append(Build.MODEL).append(" ").append(Build.VERSION.RELEASE);
		return sb.toString();
	}

	/**
	 * 获取状态栏高度 ldpi=.75, mdpi=1, hdpi=1.5, xhdpi=2
	 */
	public static int getStatusBarHeight(Activity instance) {
		int statusBarHeight = (int) Math.ceil(25 * instance.getResources()
				.getDisplayMetrics().density);
		return statusBarHeight;
	}

	/**
	 * 获取屏幕宽度
	 * 
	 * @param instance
	 */
	public static int getScreenWidth(Activity instance) {
		int contentWidth = instance.getWindow().getDecorView().getWidth();
		return contentWidth > 0 ? contentWidth
				: instance.getResources().getDisplayMetrics().widthPixels;
	}
}
