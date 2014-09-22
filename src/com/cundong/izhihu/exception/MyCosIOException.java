package com.cundong.izhihu.exception;

/**
 * 类说明： MyCos IO类异常
 * 
 * @author Cundong
 * @date 2013-6-16
 * @version 1.0
 */
public class MyCosIOException extends MyCosException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MyCosIOException(String msg) {
		super(msg);

		this.errorCode = CONNECTED_ERORR;
	}

	public MyCosIOException(String msg, Exception cause) {
		super(msg, cause);

		this.errorCode = CONNECTED_ERORR;
	}

}