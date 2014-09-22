package com.cundong.izhihu.exception;

/**
 * 类说明： MyCos自定义异常类
 * 
 * @author Cundong
 * @date 2014-1-11
 * @version 1.0
 */
public class MyCosException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// 未知错误
	public static final String UNKONWEN_ERORR = "-1";

	// 连接超时
	public static final String CONNECTED_ERORR = "-1001";

	// 解析JSON错误
	public static final String PARSE_ERORR = "-1002";

	protected String errorCode = UNKONWEN_ERORR;

	public MyCosException(String msg) {
		super(msg);
	}

	public MyCosException(String msg, Exception cause) {
		super(msg, cause);
	}

	public String getErrorCode() {
		return errorCode;
	}
}