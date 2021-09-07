package org.idempiere.dms.factories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.SystemIDs;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.util.DMSOprUtils;
import org.idempiere.dms.util.DMSPermissionUtils;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Permission;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSPermission;

/**
 * Default DMS Permission implementation based on directory structure wise accessibility
 */
public class DefaultPermission implements IPermissionManager
{
	protected boolean isRead          = true;
	protected boolean isWrite         = true;
	protected boolean isDelete        = true;
	protected boolean isNavigation    = true;
	protected boolean isAllPermission = true;

	@Override
	public boolean createPermission(MDMSContent content, HashMap<String, Object> map, boolean isCreateForChildContent)
	{
		int permissionID = getPermissionIDByUserRoleFromPermissionMap(content, map, null);

		boolean isAllPermission = false;
		if (map.containsKey(MDMSPermission.COLUMNNAME_IsAllPermission))
			isAllPermission = (boolean) map.get(MDMSPermission.COLUMNNAME_IsAllPermission);

		if (isAllPermission)
		{
			map.put(MDMSPermission.COLUMNNAME_AD_Role_ID, 0);
			map.put(MDMSPermission.COLUMNNAME_AD_User_ID, 0);
		}

		MDMSPermission permission = (MDMSPermission) MTable.get(content.getCtx(), MDMSPermission.Table_ID).getPO(permissionID, null);

		/*
		 * if permission already exists for the current content then no need to create from
		 * parent/child content permission.
		 * ex: Creating navigation permission, Allow root directory then all child hierarchy
		 * accessible
		 */
		if (permissionID <= 0)
		{
			setPermissionValueFromMap(permission, content, map);
		}
		// Create Permission for the sub content
		if (isCreateForChildContent)
		{
			createPermissionForSubContent(content, map);
		}
		return true;
	} // createPermission

	@Override
	public void updatePermission(MDMSPermission permission, MDMSContent content, HashMap<String, Object> mapColumnValue)
	{
		setPermissionValueFromMap(permission, content, mapColumnValue);
	} // updatePermission

	protected void setPermissionValueFromMap(MDMSPermission permission, MDMSContent content, HashMap<String, Object> map)
	{
		// DMS_Content_ID
		permission.setDMS_Content_ID(content.getDMS_Content_ID());

		// AD_Org_ID
		if (map.containsKey(MDMSPermission.COLUMNNAME_AD_Org_ID))
			permission.setAD_Org_ID((Integer) map.get(MDMSPermission.COLUMNNAME_AD_Org_ID));
		else
			permission.setAD_Org_ID(Env.getAD_Org_ID(content.getCtx()));

		// DMS_Owner_ID
		if (map.containsKey(MDMSPermission.COLUMNNAME_DMS_Owner_ID))
			permission.setDMS_Owner_ID((Integer) map.get(MDMSPermission.COLUMNNAME_DMS_Owner_ID));
		else
			permission.setDMS_Owner_ID(Env.getAD_User_ID(content.getCtx()));

		// AD_Role_ID
		if (map.containsKey(MDMSPermission.COLUMNNAME_AD_Role_ID) && (Integer) map.get(MDMSPermission.COLUMNNAME_AD_Role_ID) != 0)
			permission.setAD_Role_ID((Integer) map.get(MDMSPermission.COLUMNNAME_AD_Role_ID));
		else
			permission.set_ValueOfColumn(MDMSPermission.COLUMNNAME_AD_Role_ID, null);

		// AD_User_ID
		if (map.containsKey(MDMSPermission.COLUMNNAME_AD_User_ID) && (Integer) map.get(MDMSPermission.COLUMNNAME_AD_User_ID) != 0)
			permission.setAD_User_ID((Integer) map.get(MDMSPermission.COLUMNNAME_AD_User_ID));
		else
			permission.set_ValueOfColumn(MDMSPermission.COLUMNNAME_AD_User_ID, null);

		// IsRead
		if (map.containsKey(MDMSPermission.COLUMNNAME_IsRead))
			permission.setIsRead((Boolean) map.get(MDMSPermission.COLUMNNAME_IsRead));
		else
			permission.setIsRead(false);

		// IsWrite
		if (map.containsKey(MDMSPermission.COLUMNNAME_IsWrite))
			permission.setIsWrite((Boolean) map.get(MDMSPermission.COLUMNNAME_IsWrite));
		else
			permission.setIsWrite(false);

		// IsDelete
		if (map.containsKey(MDMSPermission.COLUMNNAME_IsDelete))
			permission.setIsDelete((Boolean) map.get(MDMSPermission.COLUMNNAME_IsDelete));
		else
			permission.setIsDelete(false);

		// IsNavigation
		if (map.containsKey(MDMSPermission.COLUMNNAME_IsNavigation))
			permission.setIsNavigation(	(Boolean) map.get(MDMSPermission.COLUMNNAME_IsNavigation)
										&& MDMSContent.CONTENTBASETYPE_Directory.equals(content.getContentBaseType()));
		else
			permission.setIsNavigation(content.isMounting());

		// IsAllPermission
		if (map.containsKey(MDMSPermission.COLUMNNAME_IsAllPermission))
			permission.setIsAllPermission((Boolean) map.get(MDMSPermission.COLUMNNAME_IsAllPermission));
		else
			permission.setIsAllPermission(false);

		// Save
		permission.saveEx();
	} // setPermissionValueFromMap

	protected void createPermissionForSubContent(MDMSContent parentContent, HashMap<String, Object> mapColumnValue)
	{
		HashMap<I_DMS_Content, I_DMS_Association> relatedContentMap = DMSOprUtils.getRelatedContents(parentContent, true, parentContent.get_TrxName());

		ArrayList<I_DMS_Content> filteredContentList = getFilteredContentList(relatedContentMap.keySet());

		for (I_DMS_Content content : filteredContentList)
		{
			createPermission((MDMSContent) content, mapColumnValue, true);
		}
	} // createPermissionForSubContent

	@Override
	public boolean autoCreateContentPermission(int DMS_Content_ID)
	{
		/*
		 * Parent content exists and non mounting content and also its permission entries exists
		 * then clone that permission, no need to create automatically
		 */
		if (ignoreIfParentPermissionExists(DMS_Content_ID))
		{
			MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(DMS_Content_ID, null);
			MDMSPermission permission = (MDMSPermission) MDMSPermission.getPermissionForGivenParams(content, Env.getAD_Role_ID(content.getCtx()), Env.getAD_User_ID(content.getCtx()));

			if (permission == null)
			{
				permission = (MDMSPermission) MTable.get(content.getCtx(), MDMSPermission.Table_ID).getPO(0, null);
				permission.setAD_Org_ID(content.getAD_Org_ID());
				permission.setDMS_Content_ID(content.getDMS_Content_ID());
				if (content.isMounting())
				{
					permission.setDMS_Owner_ID(SystemIDs.USER_SUPERUSER);
					permission.setIsRead(false);
					permission.setIsWrite(false);
					permission.setIsDelete(false);
					permission.setIsNavigation(true);
					permission.setIsAllPermission(false);
				}
				else
				{
					permission.setDMS_Owner_ID(content.getCreatedBy());
					permission.setAD_User_ID(content.getCreatedBy());
					permission.setDMS_Owner_ID(content.getCreatedBy());
					permission.setIsRead(true);
					permission.setIsWrite(true);
					permission.setIsDelete(true);
					permission.setIsNavigation(false);
					permission.setIsAllPermission(false);
				}
				permission.saveEx();
			}
		}
		else
		{
			return false;
		}

		return true;
	} // autoCreateContentPermission

	public boolean ignoreIfParentPermissionExists(int content_ID)
	{
		int parentContentID = DB.getSQLValue(null, DMSConstant.SQL_GET_PARENT_CONTENT_ID_FROM_CONTENT, content_ID);
		if (parentContentID > 0)
		{
			// AllPermission flag exists then ignore to create any kind of permission
			boolean isAllPermissionExists = DB.getSQLValue(null, DMSConstant.SQL_COUNT_PERMISSION_ENTRIES + " AND IsAllPermission = 'Y' ", parentContentID) > 0;
			if (isAllPermissionExists)
				return false;

			// Check parent of parent permission if not found the permission
			int permissionCount = DB.getSQLValue(null, DMSConstant.SQL_COUNT_PERMISSION_ENTRIES, parentContentID);
			if (permissionCount <= 0)
				return ignoreIfParentPermissionExists(parentContentID);
		}
		return true;
	} // ignoreIfParentPermissionExists

	@Override
	public boolean isContentPermissionGranted(I_DMS_Content content, int roleID, int userID)
	{
		MDMSContent dmsContent = (MDMSContent) content;
		int no = DB.getSQLValue(dmsContent.get_TrxName(), DMSConstant.SQL_COUNT_PERMISSION_ENTRIES, content.getDMS_Content_ID());
		if (no <= 0)
			return true;

		// AND AD_User_ID IS NULL AND AD_Role_ID IS NULL,
		no = DB.getSQLValue(dmsContent.get_TrxName(), DMSConstant.SQL_COUNT_PERMISSION_ENTRIES + " AND IsAllPermission = 'Y' ", content.getDMS_Content_ID());
		if (no > 0)
			return true;

		if (MDMSPermission.getPermissionForGivenParams(dmsContent, roleID, userID) != null)
			return true;

		return false;
	} // isContentPermissionGranted

	@Override
	public void createNavigationPermission(I_DMS_Permission permission)
	{
		MDMSContent content = (MDMSContent) permission.getDMS_Content();
		if (!content.isMounting())
		{
			int prntContentID = DB.getSQLValue(null, DMSConstant.SQL_GET_PARENT_CONTENT_ID_NON_MOUNTING, content.getDMS_Content_ID());
			if (prntContentID > 0)
			{
				int parentAccessiblePermissionID = getPermissionIDByUserRoleFromDMSPermission(permission, prntContentID, " AND IsActive = 'Y' ");

				if (parentAccessiblePermissionID <= 0)
				{
					int parentNavPermissionID = getPermissionIDByUserRoleFromDMSPermission(permission, prntContentID, " AND IsActive = 'Y' AND IsNavigation = 'Y' ");

					if (parentNavPermissionID <= 0)
					{
						HashMap<String, Object> map = new HashMap<String, Object>();

						fillMapFromDMSPermission(permission, map);

						//
						MDMSContent parentContent = (MDMSContent) MTable.get(content.getCtx(), MDMSContent.Table_Name).getPO(prntContentID, null);
						//
						createPermission((MDMSContent) parentContent, map, false);
					}
				}
			}
		}
	} // createNavigationPermission

	@Override
	public void grantChildPermissionFromParentContent(I_DMS_Content content, I_DMS_Content parentContent)
	{
		MDMSPermission[] arrayParentPermission = (MDMSPermission[]) MDMSPermission.getAllPermissionForContent((MDMSContent) parentContent);

		if (arrayParentPermission == null)
			return;

		for (MDMSPermission parentPermission : arrayParentPermission)
		{
			if (MDMSContent.CONTENTBASETYPE_Content.equals(content.getContentBaseType()) && isOnlyNavigationPermission(parentPermission))
				continue;

			int permissionID = getPermissionIDByUserRoleFromDMSPermission(parentPermission, content.getDMS_Content_ID(), " AND IsNavigation = 'N' ");

			if (permissionID <= 0)
			{
				MDMSPermission newPermission = (MDMSPermission) MTable	.get(((PO) content).getCtx(), MDMSPermission.Table_ID)
																		.getPO(permissionID, ((PO) content).get_TrxName());
				PO.copyValues(parentPermission, newPermission);

				newPermission.setIsNavigation(parentPermission.isNavigation() && MDMSContent.CONTENTBASETYPE_Directory.equals(content.getContentBaseType()));
				newPermission.setDMS_Owner_ID(Env.getAD_User_ID(((PO) content).getCtx()));
				newPermission.setDMS_Content_ID(content.getDMS_Content_ID());
				newPermission.setIsAllPermission(false);
				newPermission.saveEx();
			}
		}
	} // grantPermissionFromParentContent

	@Override
	public HashMap<I_DMS_Version, I_DMS_Association> getFilteredVersionList(HashMap<I_DMS_Version, I_DMS_Association> contentMap)
	{
		MRole role = MRole.getDefault();
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		boolean isDMSAdmin = role.get_ValueAsBoolean(DMSConstant.COLUMNNAME_IS_DMS_ADMIN);

		//
		HashMap<I_DMS_Version, I_DMS_Association> mapFiltered = new HashMap<I_DMS_Version, I_DMS_Association>();

		Iterator<Entry<I_DMS_Version, I_DMS_Association>> it = contentMap.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<I_DMS_Version, I_DMS_Association> mapEntry = (Entry<I_DMS_Version, I_DMS_Association>) it.next();
			if (isDMSAdmin || DMSPermissionUtils.isContentPermissionGranted(mapEntry.getKey().getDMS_Content(), role.getAD_Role_ID(), AD_User_ID))
			{
				mapFiltered.put(mapEntry.getKey(), mapEntry.getValue());
			}
		}
		return mapFiltered;
	} // getFilteredContentList

	@Override
	public ArrayList<I_DMS_Content> getFilteredContentList(Set<I_DMS_Content> contentSet)
	{
		MRole role = MRole.getDefault();
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		boolean isDMSAdmin = role.get_ValueAsBoolean(DMSConstant.COLUMNNAME_IS_DMS_ADMIN);

		//
		ArrayList<I_DMS_Content> list = new ArrayList<I_DMS_Content>();

		for (I_DMS_Content content : contentSet)
		{
			if (isDMSAdmin || isContentPermissionGranted(content, role.getAD_Role_ID(), AD_User_ID))
			{
				list.add(content);
			}
		}
		return list;
	} // getFilteredContentList

	@Override
	public void validatePermissionDataFromMap(HashMap<String, Object> mapColumnValue)
	{
		boolean isDMSAdmin = MRole.getDefault().get_ValueAsBoolean(DMSConstant.COLUMNNAME_IS_DMS_ADMIN);
		if (((Integer) mapColumnValue.get(MDMSPermission.COLUMNNAME_AD_Role_ID) == 0
				&& (Integer) mapColumnValue.get(MDMSPermission.COLUMNNAME_AD_User_ID) == 0))
		{
			if (isDMSAdmin && !(Boolean) mapColumnValue.get(MDMSPermission.COLUMNNAME_IsAllPermission))
				throw new AdempiereException("Please select any one option from Role / User / All Permission");
			else if (!isDMSAdmin)
				throw new AdempiereException("Please select Role / User");
		}
	}

	/**
	 * @param  permission
	 * @return
	 */
	private boolean isOnlyNavigationPermission(MDMSPermission permission)
	{
		return permission.isNavigation() && !permission.isRead() && !permission.isWrite() && !permission.isDelete();
	}// isOnlyNavigationPermission

	/**
	 * fill the map value from DMS Permission
	 * 
	 * @param permission
	 * @param map
	 */
	protected void fillMapFromDMSPermission(I_DMS_Permission permission, HashMap<String, Object> map)
	{
		map.put(MDMSPermission.COLUMNNAME_AD_Org_ID, permission.getAD_Org_ID());
		map.put(MDMSPermission.COLUMNNAME_DMS_Owner_ID, permission.getDMS_Owner_ID());
		map.put(MDMSPermission.COLUMNNAME_AD_Role_ID, permission.getAD_Role_ID());
		map.put(MDMSPermission.COLUMNNAME_AD_User_ID, permission.getAD_User_ID());
		map.put(MDMSPermission.COLUMNNAME_IsRead, false);
		map.put(MDMSPermission.COLUMNNAME_IsWrite, false);
		map.put(MDMSPermission.COLUMNNAME_IsDelete, false);
		map.put(MDMSPermission.COLUMNNAME_IsNavigation, true);
		map.put(MDMSPermission.COLUMNNAME_IsAllPermission, false);

	}// fillMapFromDMSPermission

	/**
	 * @param  content
	 * @param  map
	 * @param  whereclause
	 * @return
	 */
	protected int getPermissionIDByUserRoleFromPermissionMap(I_DMS_Content content, HashMap<String, Object> map, String whereclause)
	{
		int roleID = (int) map.get(MDMSPermission.COLUMNNAME_AD_Role_ID);
		int userID = (int) map.get(MDMSPermission.COLUMNNAME_AD_User_ID);

		return DMSPermissionUtils.getPermissionIDByUserRole(content.getDMS_Content_ID(), roleID, userID, whereclause);
	}// getPermissionIDByUserRoleFromPermissionMap

	/**
	 * @param  permission
	 * @param  contentID
	 * @param  whereclause
	 * @return
	 */
	protected int getPermissionIDByUserRoleFromDMSPermission(I_DMS_Permission permission, Integer contentID, String whereclause)
	{
		return DMSPermissionUtils.getPermissionIDByUserRole(contentID, permission.getAD_Role_ID(), permission.getAD_User_ID(), whereclause);
	}// getPermissionIDByUserRoleFromDMSPermission

	@Override
	public void initContentPermission(I_DMS_Content content)
	{
		int permissionID = DMSPermissionUtils.getPermissionByContentOrItsParent(content, Env.getAD_Role_ID(Env.getCtx()), Env.getAD_User_ID(Env.getCtx()));
		if (permissionID > 0)
		{
			MDMSPermission permission = (MDMSPermission) MTable.get(Env.getCtx(), MDMSPermission.Table_ID).getPO(permissionID, null);
			isRead = permission.isRead() || permission.isAllPermission();
			isWrite = permission.isWrite() || permission.isAllPermission();
			isDelete = permission.isDelete() || permission.isAllPermission();
			isNavigation = permission.isNavigation() || permission.isAllPermission();
			isAllPermission = permission.isAllPermission();
		}
		else
		{
			isRead = true;
			isWrite = true;
			isDelete = true;
			isNavigation = true;
			isAllPermission = true;
		}
	} // initContentPermission

	@Override
	public boolean isRead()
	{
		return isRead;
	}

	@Override
	public boolean isWrite()
	{
		return isWrite;
	}

	@Override
	public boolean isDelete()
	{
		return isDelete;
	}

	@Override
	public boolean isNavigation()
	{
		return isNavigation;
	}

	@Override
	public boolean isAllPermission()
	{
		return isAllPermission;
	}

}
