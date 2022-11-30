package org.idempiere.dms.factories;

import java.util.HashMap;
import java.util.List;

import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Version;

public interface IContentTypeAccess
{

	/**
	 * Get content list based on role wise filtered through ContentType Access
	 * 
	 * @param  AD_Role_ID
	 * @return            List of accessible DMS_Content_IDs
	 */
	public List<Integer> getAccessedContentsRoleWise(int AD_Role_ID);

	/**
	 * Get content list based on role wise filtered through ContentType Access and dynamic where
	 * clause
	 * 
	 * @param  AD_Role_ID
	 * @param  DynamicWhereClause
	 * @return                    List of accessible DMS_Content_IDs
	 */
	public List<Integer> getAccessedContentsRoleWise(int AD_Role_ID, String DynamicWhereClause);

	/**
	 * Return accessible map data
	 * 
	 * @param  contentMap - Input map of version & association
	 * @return            - Map of accessible items per role wise
	 */
	public HashMap<I_DMS_Version, I_DMS_Association> getFilteredContentList(HashMap<I_DMS_Version, I_DMS_Association> contentMap);

}
