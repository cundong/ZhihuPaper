package com.cundong.izhihu.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;

/**
 * 类说明：  手机工具类
 * 
 * @date 	2012-2-27
 * @version 1.0
 */
public class PhoneUtils {

	// 模拟器的imei号
	private static final String EMULATOR_IMIE = "000000000000000";

	// 模拟器的手机号码
	private static final String EMULATOR_PHONE_NUMBER = "15555215554";

	/**
	* IMEI号、MAC地址，在有些手机上竟然是动态的！！
	* 修改逻辑：
	* IMEI号、MAC地址，都改为取两遍，如果两次获取的不同，则不使用，改为使用一个randomUUID（该ID生成之后存在本地）
	* 获得手机IMEI
	* 获取串号失败，则使用MAC地址代替，再失败，则返回随机UUID
	* deviceId = UUID.randomUUID().toString();
	* @param context
	* @return
	*/
	public static String getIMEI(Context context) {

		SharedPreferences sp = context.getSharedPreferences("gamehall_imei",
				Context.MODE_PRIVATE);

		String localImei = sp.getString("imei", "");
		if (sp.contains("imei") && !TextUtils.isEmpty(localImei)) {
			return localImei;
		}
		else {

			Editor editor = sp.edit();

			String imei = "";

			String imei0 = getRealIMEI(context);
			String imei1 = getRealIMEI(context);

			// 两次取到的一样，并且不为空，并且不为模拟器的默认值
			if (!"".equals(imei0) && !"".equals(imei1)
					&& !EMULATOR_IMIE.equals(imei0)
					&& !EMULATOR_IMIE.equals(imei1) && imei0.equals(imei1)) {
				imei = imei0;
			}
			else {
				// 取不到、两次获取的不一样，则随机一个出来
				imei = UUID.randomUUID().toString();
			}
			editor.putString("imei", imei);
			editor.commit();
			return imei;
		}
	}

	private static String getRealIMEI(Context context) {

		String deviceId = "";

		TelephonyManager telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);

		deviceId = telephonyManager.getDeviceId();

		if (TextUtils.isEmpty(deviceId)) {
			String mac = getMAC(context);
			if (!TextUtils.isEmpty(mac)) {
				deviceId = mac;
			}
		}

		return deviceId;
	}

	private static String getMAC(Context context) {

		WifiManager wifi = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		WifiInfo info = wifi.getConnectionInfo();

		return info != null ? info.getMacAddress() : "";
	}

	/**
	 * 获取手机IP地址
	 * 获取失败，返回"127.0.0.1"
	 * @return
	 */
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
		}
		catch (SocketException ex) {
			ex.printStackTrace();
		}
		return "127.0.0.1";
	}

	/**
	 * 最低支持的SDK版本
	 * @return
	 */
	public static int getSDKVersion() {
		return Build.VERSION.SDK_INT;
	}

	/**
	 * 当前程序版本获取
	 * @param context
	 * @return
	 */
	public static PackageInfo getPackageInfo(Context context) {
		PackageInfo packInfo = null;
		PackageManager pm = context.getPackageManager();
		try {
			packInfo = pm.getPackageInfo(context.getPackageName(), 0);
		}
		catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return packInfo;
	}

	public static String getApplicationName(Context context) {  
        PackageManager packageManager = null;  
        ApplicationInfo applicationInfo = null;  
        try {  
            packageManager = context.getPackageManager();  
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);  
        } catch (PackageManager.NameNotFoundException e) {  
            applicationInfo = null;  
        }  
        String applicationName =   
        (String) packageManager.getApplicationLabel(applicationInfo);  
        return applicationName;  
    } 
	
	/**
	 * 获取手机号码
	 * @param mContext
	 * @return
	 */
	public static String getMobileNumber(Context mContext) {
		TelephonyManager phoneMgr = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		String mobileNumber = phoneMgr.getLine1Number();

		if (!TextUtils.isEmpty(mobileNumber)) {
			if (EMULATOR_PHONE_NUMBER.equals(mobileNumber)) {
				mobileNumber = "";
			}
		}

		return TextUtils.isEmpty(mobileNumber) ? "" : mobileNumber;
	}

	/**
	 * 获取手机设备描述（包括品牌、型号等）
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
	 * 获取状态栏高度
	 * ldpi=.75, mdpi=1, hdpi=1.5, xhdpi=2
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
	public static int getScreenWidth(Activity instance){
		DisplayMetrics dm = new DisplayMetrics();
		instance.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
	}
}