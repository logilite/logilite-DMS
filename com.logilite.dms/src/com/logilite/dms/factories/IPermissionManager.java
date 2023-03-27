package com.logilite.dms.factories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.I_DMS_Permission;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSPermission;

public interface IPermissionManager
{
	/**
	 * Create permission
	 * 
	 * @param content                 - Current Content
	 * @param mapColumnValue          - Map of ColumnName and Value to create permission entry
	 * @param isCreateForChildContent - Is create permission for the child content
	 */
	public boolean createPermission(MDMSContent content, HashMap<String, Object> mapColumnValue, boolean isCreateForChildContent);

	/**
	 * Update existing permission
	 * 
	 * @param permission     - Current permission to change
	 * @param content        - Current Content
	 * @param mapColumnValue - Map of ColumnName and Value to create permission entry
	 */
	public void updatePermission(MDMSPermission permission, MDMSContent content, HashMap<String, Object> mapColumnValue);

	/**
	 * Auto create content permission based on parent content
	 * 
	 * @param  DMS_Content_ID
	 * @return                True if permission is created
	 */
	public boolean autoCreateContentPermission(int DMS_Content_ID);

	/**
	 * Check Content permission allowed to give User/Role
	 * 
	 * @param  content
	 * @param  AD_Role_ID
	 * @param  AD_User_ID
	 * @return            True if content is accessible to the User/Role
	 */
	public boolean isContentPermissionGranted(I_DMS_Content content, int AD_Role_ID, int AD_User_ID);

	/**
	 * From the map list items to filter based on permission records
	 * 
	 * @param  versionsMap - Map of Version and Association
	 * @return             Map of Version and Association - Based on permission filtered
	 */
	public HashMap<I_DMS_Version, I_DMS_Association> getFilteredVersionList(HashMap<I_DMS_Version, I_DMS_Association> versionsMap);

	/**
	 * From the version set items to filter based on permission records
	 * 
	 * @param  contentSet - Set of the contents
	 * @return            List of content based on permission filtered
	 */
	public ArrayList<I_DMS_Content> getFilteredContentList(Set<I_DMS_Content> contentSet);

	/**
	 * Grant the permission to the current content based on parent content permissions if result is true
	 * 
	 * @param content
	 * @param parentContent
	 * @param isCreateForChildContent
	 */
	public void grantChildPermissionFromParentContent(I_DMS_Content content, I_DMS_Content parentContent, boolean isCreateForChildContent);

	/**
	 * Create Navigation Permission based on any new permission created then check if parent content
	 * haven't exists then call this method.
	 * -----
	 * Create a navigation permission if User1 grant the access of leaf directory or content to
	 * User2 then parent hierarchy should be navigable to top to bottom for User2.
	 * -----
	 * For example: A_Dir > B_Dir > C_Dir
	 * --> If User1 grant the permission to User2 for C_Dir then for User2 also navigable to C_Dir
	 * via using A_Dir & B_Dir
	 * --> So Here we creating A_Dir & B_Dir Navigation permission
	 * 
	 * @param permission
	 */
	public void createNavigationPermission(I_DMS_Permission permission);

	/**
	 * Validate permission
	 * 
	 * @param mapColumnValue - Map of ColumnName and Value to create permission entry
	 */
	public void validatePermissionDataFromMap(HashMap<String, Object> mapColumnValue);

	/*
	 * Use for checking content permission related access
	 */

	/**
	 * Initiate the content to get its permission and based on that Read, Write, Delete, Navigation
	 * and AllPermission flag updated
	 * 
	 * @param content - DMS Content
	 */
	public void initContentPermission(I_DMS_Content content);

	public boolean isRead();

	public boolean isWrite();

	public boolean isDelete();

	public boolean isNavigation();

	public boolean isAllPermission();

}
