package com.cundong.izhihu.exception;

/**
 * 类说明： 字符串解析JSON时异常
 * 
 * @author Cundong
 * @date 2013-6-16
 * @version 1.0
 */
public class MyCosParseException extends MyCosException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MyCosParseException(String msg) {
		super(msg);

		this.errorCode = PARSE_ERORR;
	}

	public MyCosParseException(String msg, Exception cause) {
		super(msg, cause);

		this.errorCode = PARSE_ERORR;
	}

}