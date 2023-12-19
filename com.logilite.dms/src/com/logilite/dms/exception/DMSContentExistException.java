package com.logilite.dms.exception;

/**
 * DMS Content Already Exist Exception
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class DMSContentExistException extends RuntimeException
{
	private static final long	serialVersionUID	= -7744129701010263504L;

	private int					contentID			= 0;

	/**
	 * Constructor
	 * 
	 * @param message
	 */
	public DMSContentExistException(String message)
	{
		super(message);
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
