package com.cundong.izhihu.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 类说明： 字符串操作类
 * 
 * @author Cundong
 * @date Feb 7, 2012 2:51:58 AM
 * @version 1.0
 */
public class StringUtils {
	public static final String[] EMPTY_STRING = new String[0];

	/**
	 * 判断给定字符串是否空白串。<br>
	 * 空白串是指由空格、制表符、回车符、换行符组成的字符串<br>
	 * 若输入字符串为null或空字符串，返回true
	 * 
	 * @param input
	 * @return boolean
	 */
	public static boolean isBlank(String input) {
		if (input == null || "".equals(input))
			return true;

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
				return false;
			}
		}
		return true;
	}

	/**
	 * 若输入为null，则返回空字符串；否则返回字符串自身
	 * 
	 * @param
	 * @return
	 */
	public static String getNotNull(String input) {
		return (input == null ? "" : input);
	}

	public static String getNotNull(String input, String defaultValue) {
		return (isBlank(input) ? defaultValue : input);
	}

	/**
	 * 截取字符串末尾空格
	 * 
	 * @param input
	 * @return
	 */
	public static String trim(String input) {
		if (isBlank(input))
			return "";
		return input.trim();
	}

	/**
	 * 根据指定分隔符分割字符串，返回结果数组。<br>
	 * 处理规则：<br>
	 * 若输入为null，则返回null；<br>
	 * 否则若输入为空字符串，则返回空数组；<br>
	 * 否则若分隔符为null或空字符串，则返回包含原字符串本身的数组；<br>
	 * 
	 * @param input
	 *            输入字符串
	 * @param separator
	 *            分隔符
	 * @return 结果数组（注意：包括空字符串）
	 */
	public static String[] split(String input, String separator) {
		if (input == null)
			return null;
		if (input.equals(""))
			return EMPTY_STRING;
		if (separator == null || "".equals(separator))
			return new String[] { input };

		int cursor = 0; // 游标
		int lastPos = 0; // 指向上一个分隔符后第一个字符
		ArrayList<String> list = new ArrayList<String>();

		while ((cursor = input.indexOf(separator, cursor)) != -1) {

			String token = input.substring(lastPos, cursor);
			list.add(token);

			lastPos = cursor + separator.length();

			cursor = lastPos;
		}

		if (lastPos < input.length())
			list.add(input.substring(lastPos));

		return (String[]) list.toArray(new String[list.size()]);
	}

	/**
	 * 字符串替换
	 * 
	 * @param source
	 * @param oldstring
	 * @param newstring
	 * @param caseInsensive
	 * @return
	 */
	public static String replaceString(String source, String oldstring, String newstring, boolean caseInsensive) {
		Matcher matcher = null;

		// 区分大小写
		if (caseInsensive) {
			matcher = Pattern.compile(oldstring, Pattern.CASE_INSENSITIVE).matcher(source);
		} else {
			matcher = Pattern.compile(oldstring).matcher(source);
		}

		return matcher.replaceAll(newstring);
	}

	/**
	 * 清除字符串末尾的特定字符<br>
	 * 若字符串末尾并非给定字符，则什么都不做<br>
	 * 注意：该方法改变了传入的StringBuffer参数的值
	 * 
	 * @param sb
	 *            字符串缓存
	 * @param tail
	 *            用户给定字符
	 * @return 字符串缓存对象的字符串表示
	 */
	public static String trimTail(StringBuffer sb, char tail) {
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) == tail)
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * 根据指定分隔符分割字符串，返回结果数组。<br>
	 * 处理规则：<br>
	 * 若输入为null，则返回null；<br>
	 * 否则若输入为空字符串，则返回空数组；<br>
	 * 否则若分隔符为null或空字符串，则返回包含原字符串本身的数组；<br>
	 * 注意：返回结果中过滤掉空字符串
	 * 
	 * @param input
	 *            输入字符串(数字字符串)
	 * @param separator
	 *            分隔符
	 * @return 结果数组（注意：不包括空字符串）
	 */
	public static Integer[] splitInt(String input, String separator) {
		if (input == null)
			return null;
		if (input.equals(""))
			return null;
		if (separator == null || "".equals(separator))
			return null;

		int cursor = 0; // 游标
		int lastPos = 0; // 指向上一个分隔符后第一个字符
		ArrayList<Integer> list = new ArrayList<Integer>();

		while ((cursor = input.indexOf(separator, cursor)) != -1) {

			if (cursor > lastPos) {// 滤掉空字符串
				int token = Integer.parseInt(input.substring(lastPos, cursor));
				list.add(token);
			}

			lastPos = cursor + separator.length();

			cursor = lastPos;
		}

		if (lastPos < input.length())
			list.add(Integer.parseInt(input.substring(lastPos)));

		Integer[] iStrToI = new Integer[list.size()];
		for (int i = 0; i < list.size(); i++) {
			iStrToI[i] = Integer.parseInt(list.get(i).toString());
		}
		return iStrToI;
	}

	/**
	 * 字符串的转义(处理特殊字符)
	 * 
	 * @param input
	 * @return
	 */
	public static String StringToString(String input) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.toCharArray()[i];
			switch (c) {
			case '\'':
				sb.append("\\\'");
				break;
			case '\"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '/':
				sb.append("\\/");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return sb.toString();
	}

	/**
	 * list 转换为 string
	 * 
	 * @param list
	 * @param flag
	 * @return
	 */
	public static String listToString(ArrayList<String> list, String flag) {
		String strMsg = "";
		int listSize = list.size();
		if (listSize > 0) {
			for (int i = 0; i < listSize; i++) {
				if (i == listSize - 1) {
					strMsg = strMsg + list.get(i).toString();
				} else {
					strMsg = strMsg + list.get(i).toString() + flag;
				}
			}
		} else {
			strMsg = "";
		}
		return strMsg;
	}

	/**
	 * 字符串转换为arraylist(必须符合这样的格式([{"a":"a","b":"b"}，{"a":"a","b":"b"}，{}]))
	 * 
	 * @param strList
	 * @return (成功为 arrayList 失败 Exception)
	 * 
	 */
	public static ArrayList<Object> strToList(String strList) {

		JSONObject oJsonObj = null;
		ArrayList<Object> listStr = new ArrayList<Object>();
		int iStriList = strList.length(); // 长度
		String strDemoList = strList.substring(1, iStriList - 1);// 干掉 []
		String[] arrInfo = StringUtils.split(strDemoList, "},");
		int iArrInfo = arrInfo.length;
		for (int i = 0; i < iArrInfo; i++) {
			HashMap<String, String> paperMsg = new HashMap<String, String>();
			try {
				if (!arrInfo[i].endsWith("}")) {
					oJsonObj = new JSONObject(arrInfo[i] + "}");
				} else {
					oJsonObj = new JSONObject(arrInfo[i]);
				}
				Iterator<?> iter2 = oJsonObj.keys();
				while (iter2.hasNext()) {
					String key = iter2.next().toString();
					paperMsg.put(key, oJsonObj.get(key).toString());
				}
				listStr.add(paperMsg);// 添加list
			} catch (JSONException e) {

				// 出现异常 清空List
				listStr.clear();
				listStr.add(0, "Exception");
			}
		}
		return listStr;
	}

	/**
	 * InputStream转为字符串
	 * 
	 * @param in
	 * @return
	 */
	public static String toString(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] buffer = new byte[1024];
		for (int i; (i = in.read(buffer)) != -1;) {
			out.append(new String(buffer, 0, i));
		}
		return out.toString();
	}

	/**
	 * 对字符串中的中文进行编码
	 * 
	 * @param inputUrl
	 * @return
	 */
	public static String encodeUrl(String inputUrl) {

		if (isBlank(inputUrl))
			return inputUrl;

		char[] charArray = inputUrl.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			if ((charArray[i] >= 0x4e00) && (charArray[i] <= 0x9fbb)) {
				inputUrl = inputUrl.replaceFirst(String.valueOf(charArray[i]), URLEncoder.encode(String.valueOf(charArray[i])));

			}
		}
		return inputUrl;
	}

	/**
	 * 截取字符串
	 * 
	 * @param inputUrl
	 * @param length
	 * @return
	 */
	public static String subString(String inputUrl, int length) {
		if (isBlank(inputUrl))
			return inputUrl;

		if (inputUrl.length() > length) {
			return inputUrl.substring(0, length);
		} else {
			return inputUrl;
		}
	}
}