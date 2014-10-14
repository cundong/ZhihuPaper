package com.cundong.izhihu.util;

import android.util.Log;

public class Logger {

	/**
	 * log tag
	 */
	private String tag = "Logger";// application name

	//TODO 配置Log打开或关闭
	/**
	 * debug or not
	 */
	private static boolean debug = false;

	private static Logger instance = new Logger();

	private Logger() {

	}

	public static Logger getLogger() {
		return instance;
	}

	private String getFunctionName() {
		StackTraceElement[] sts = Thread.currentThread().getStackTrace();

		if (sts == null) {
			return null;
		}

		for (StackTraceElement st : sts) {
			if (st.isNativeMethod()) {
				continue;
			}

			if (st.getClassName().equals(Thread.class.getName())) {
				continue;
			}

			if (st.getClassName().equals(this.getClass().getName())) {
				continue;
			}

			return "[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + "): " + st.getFileName() + ":" + st.getLineNumber() + "]";
		}

		return null;
	}

	private String createMessage(String msg) {
		String functionName = getFunctionName();
		String message = (functionName == null ? msg : (functionName + " - " + msg));
		return message;
	}

	/**
	 * log.i
	 */
	public void i(String msg) {
		if (debug) {
			String message = createMessage(msg);
			Log.i(tag, message);
		}
	}

	/**
	 * log.v
	 */
	public void v(String msg) {
		if (debug) {
			String message = createMessage(msg);
			Log.v(tag, message);
		}
	}

	/**
	 * log.d
	 */
	public void d(String msg) {
		if (debug) {
			String message = createMessage(msg);
			Log.d(tag, message);
		}
	}

	/**
	 * log.e
	 */
	public void e(String msg) {
//		if (debug) {
			String message = createMessage(msg);
			Log.e(tag, message);
//		}
	}

	/**
	 * log.error
	 */
	public void error(Exception e) {
		if (debug) {
			StringBuffer sb = new StringBuffer();
			String name = getFunctionName();
			StackTraceElement[] sts = e.getStackTrace();

			if (name != null) {
				sb.append(name + " - " + e + "\r\n");
			} else {
				sb.append(e + "\r\n");
			}
			if (sts != null && sts.length > 0) {
				for (StackTraceElement st : sts) {
					if (st != null) {
						sb.append("[ " + st.getFileName() + ":" + st.getLineNumber() + " ]\r\n");
					}
				}
			}
			
			Log.e(tag, sb.toString());
		}
	}

	/**
	 * log.d
	 */
	public void w(String msg) {
		if (debug) {
			String message = createMessage(msg);
			Log.w(tag, message);
		}
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * set debug
	 */
	public static void setDebug(boolean d) {
		debug = d;
	}
	
	public static boolean isDebug(){
		return debug;
	}
}