/**
 * 
 */
package com.logilite.dms.factories;

/**
 * User of Upload Content dialog design as per requirement wise
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public interface IDMSUploadContent
{

	public boolean isCancel();

	/**
	 * @return content ID of the uploaded document
	 */
	public Integer[] getUploadedDocContentIDs();

}
