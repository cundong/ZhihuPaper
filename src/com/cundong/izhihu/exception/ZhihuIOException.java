package com.cundong.izhihu.exception;

/**
 * 类说明： IO类异常
 * 
 * @author 	Cundong
 * @date 	2014-6-16
 * @version 1.0
 */
public class ZhihuIOException extends ZhihuException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ZhihuIOException(String msg) {
		super(msg);

		this.errorCode = CONNECTED_ERORR;
	}

	public ZhihuIOException(String msg, Exception cause) {
		super(msg, cause);

		this.errorCode = CONNECTED_ERORR;
	}

}