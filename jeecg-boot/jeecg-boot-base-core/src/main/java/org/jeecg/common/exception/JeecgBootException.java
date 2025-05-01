package org.jeecg.common.exception;

import org.jeecg.common.constant.CommonConstant;

/**
 * @Description: jeecg-boot自定义异常
 * @author: jeecg-boot
 */
public class JeecgBootException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * 返回给前端的错误code
	 */
	private int errCode = CommonConstant.SC_INTERNAL_SERVER_ERROR_500;

	public JeecgBootException(String message){
		super(message);
	}

	public JeecgBootException(int statusCode, String message){
		super(message);
		this.errCode = statusCode;
	}

	public JeecgBootException(String message, int errCode){
		super(message);
		this.errCode = errCode;
	}

	public int getErrCode() {
		return errCode;
	}

	public JeecgBootException(Throwable cause)
	{
		super(cause);
	}
	
	public JeecgBootException(String message,Throwable cause)
	{
		super(message,cause);
	}

	/**
	 *
	 *  因为默认异常会在该方法中递归方式抓取线程堆栈信息，这个过程开销极大。
	 * 	对于已知的业务异常其实并不需要这些堆栈信息,因此重写了该方法，禁止抓取线程堆栈，以便提供一个轻量级的异常类
	 * add by huweizhi
	 * @return
	 */
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

}
