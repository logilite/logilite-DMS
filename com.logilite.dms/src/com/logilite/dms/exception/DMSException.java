package com.logilite.dms.exception;

/**
 * DMS Exception handling
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class DMSException extends RuntimeException
{
	private static final long	serialVersionUID	= -7140640158883541543L;

	private int					contentID			= 0;
	private Exception			exception;

	public Exception getException()
	{
		return exception;
	}

	public void setException(Exception exception)
	{
		this.exception = exception;
	}

	public int getContentID()
	{
		return contentID;
	}

	public void setContentID(int contentID)
	{
		this.contentID = contentID;
	}
}
