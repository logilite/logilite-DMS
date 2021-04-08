package org.idempiere.dms.util;

import java.util.List;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.DMSPermission;
import org.idempiere.dms.factories.IPermission;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSPermission;

public class DMSPermissionUtils
{
	public static MDMSPermission createPermission(DMSPermission dmsPermission, MDMSContent content, boolean isCreateForSubContent)
	{
		IPermission permission = DMSFactoryUtils.getContentPermissionValidator();
		if (permission != null)
			return permission.createPermission(dmsPermission, content, isCreateForSubContent);
		return null;
	}

	public static MDMSPermission createContentPermission(MDMSContent content)
	{
		IPermission permission = DMSFactoryUtils.getContentPermissionValidator();
		if (permission != null)
			return permission.createContentPermission(content);
		return null;
	}
	
	public static boolean validateContentPermission(MDMSContent content)
	{
		IPermission permission = DMSFactoryUtils.getContentPermissionValidator();
		if (permission != null)
			return permission.validateContentPermission(content);
		return true;
	}

	public static void givePermissionBaseOnParentContent(MDMSContent content, MDMSContent parentContent)
	{
		List <MDMSPermission> destPermissionList = getContentPermissionList(parentContent);

		if (destPermissionList == null)
			return;
		
		for (MDMSPermission permission : destPermissionList)
		{
			int permissionID = getPermissionIDByUserRole(content.getDMS_Content_ID(), permission.getAD_Role_ID(), permission.getAD_User_ID());

			MDMSPermission cutPermission = (MDMSPermission) MTable.get(content.getCtx(), MDMSPermission.Table_ID).getPO(permissionID, content.get_TrxName());
			PO.copyValues(permission, cutPermission);
			cutPermission.setDMS_Owner_ID(Env.getAD_User_ID(content.getCtx()));
			cutPermission.setDMS_Content_ID(content.getDMS_Content_ID());
			cutPermission.setIsAllPermission(false);
			cutPermission.saveEx();
		}
	}

	public static int getPermissionIDByUserRole(Integer dms_Content_ID, Integer ad_Role_ID, Integer ad_User_ID)
	{
		String sql = "SELECT DMS_Permission_ID FROM DMS_Permission WHERE DMS_Content_ID = ?  ";
		// role
		if (ad_Role_ID != null && ad_Role_ID > 0)
			sql += " AND AD_Role_ID = " + ad_Role_ID.intValue();
		else if (ad_Role_ID == null)
			sql += " AND AD_Role_ID IS NULL ";
		// user
		if (ad_User_ID != null && ad_User_ID > 0)
			sql += " AND AD_User_ID = " + ad_User_ID.intValue();
		else if (ad_Role_ID == null)
			sql += " AND AD_User_ID IS NULL ";

		int permissionID = DB.getSQLValue(null, sql, dms_Content_ID);

		return permissionID < 0 ? 0 : permissionID;
	}

	public static List <MDMSPermission> getContentPermissionList(MDMSContent destContent)
	{
		if (destContent == null)
			return null;

		return new Query(destContent.getCtx(), MTable.get(destContent.getCtx(), MDMSPermission.Table_ID), MDMSPermission.COLUMNNAME_DMS_Content_ID + " = ?", destContent.get_TrxName())
						.setOnlyActiveRecords(true).setParameters(destContent.getDMS_Content_ID()).list();
	}
}
