package com.cundong.izhihu.exception;

/**
 * 类说明： 服务器返回的异常情况封装类
 * 
 * @author Cundong
 * @date 2013-6-16
 * @version 1.0
 */
public class MyCosServerException extends MyCosException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String errorMsg = "";

	public MyCosServerException(String msg) {
		super(msg);
	}

	public MyCosServerException(String msg, String errorCode) {
		super(msg);
		this.errorCode = errorCode;
	}

	public MyCosServerException(String msg, String errorCode, String errorMsg) {
		super(msg);
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}

	public MyCosServerException(String msg, Exception cause) {
		super(msg, cause);
	}

	public MyCosServerException(String msg, Exception cause, String errorCode) {
		super(msg, cause);
		this.errorCode = errorCode;
	}

	public String getErrorMsg() {
		return errorMsg;
	}
}