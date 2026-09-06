package com.cundong.izhihu.exception;

/**
 * 类说明： 	其他异常
 * 
 * @author 	Cundong
 * @date 	2014-6-16
 * @version 1.0
 */
public class ZhihuOtherException extends ZhihuException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ZhihuOtherException(String msg) {
		super(msg);
	}

	public ZhihuOtherException(String msg, Exception cause) {
		super(msg, cause);
	}

}