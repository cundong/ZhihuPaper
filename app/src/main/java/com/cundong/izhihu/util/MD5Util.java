package com.cundong.izhihu.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 类说明： MD5工具类
 * 
 * @date 2013-6-19
 * @version 1.0
 */
public class MD5Util {

	public static String encrypt(String source) {

		String target = "";
		if (source == null)
			source = "";

		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(source.getBytes());
			byte b[] = md.digest();

			int i;

			StringBuffer buf = new StringBuffer("");
			for (int offset = 0; offset < b.length; offset++) {
				i = b[offset];
				if (i < 0)
					i += 256;
				if (i < 16)
					buf.append("0");
				buf.append(Integer.toHexString(i));
			}
			target = buf.toString();

		} catch (NoSuchAlgorithmException e) {
		}

		return target;
	}
}