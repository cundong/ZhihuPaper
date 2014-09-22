package com.cundong.izhihu.exception;

/**
 * 类说明： 其他异常
 * 
 * @author Cundong
 * @date 2013-6-16
 * @version 1.0
 */
public class MyCosOtherException extends MyCosException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MyCosOtherException(String msg) {
		super(msg);
	}

	public MyCosOtherException(String msg, Exception cause) {
		super(msg, cause);
	}

}