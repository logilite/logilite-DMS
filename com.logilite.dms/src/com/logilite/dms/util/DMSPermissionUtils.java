package com.logilite.dms.util;

import org.compiere.model.MSysConfig;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IPermissionManager;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.I_DMS_Permission;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSPermission;

/**
 * DMS Permission Utils
 */
public class DMSPermissionUtils
{

	public static boolean isPermissionAllowed()
	{
		return MSysConfig.getBooleanValue(DMSConstant.DMS_ALLOW_PERMISSION_WISE_FILTER, false, Env.getAD_Client_ID(Env.getCtx()));
	} // isPermissionAllowed

	public static boolean createContentPermission(int DMS_Content_ID)
	{
		IPermissionManager permission = DMSFactoryUtils.getPermissionFactory();
		if (permission != null)
			return permission.autoCreateContentPermission(DMS_Content_ID);
		return false;
	} // createContentPermission

	public static boolean isContentPermissionGranted(I_DMS_Content content, int roleID, int userID)
	{
		IPermissionManager permission = DMSFactoryUtils.getPermissionFactory();
		if (permission != null)
			return permission.isContentPermissionGranted(content, roleID, userID);
		return true;
	} // isContentPermissionGranted

	public static int getPermissionIDByUserRole(Integer DMS_Content_ID, Integer AD_Role_ID, Integer AD_User_ID, String whereclause)
	{
		String sql = "SELECT DMS_Permission_ID FROM DMS_Permission WHERE DMS_Content_ID = ?  ";

		// role
		if (AD_Role_ID != null && AD_Role_ID > 0)
			sql += " AND AD_Role_ID = " + AD_Role_ID.intValue();
		else if (AD_Role_ID == null)
			sql += " AND AD_Role_ID IS NULL ";

		// user
		if (AD_User_ID != null && AD_User_ID > 0)
			sql += " AND AD_User_ID = " + AD_User_ID.intValue();
		else if (AD_User_ID == null)
			sql += " AND AD_User_ID IS NULL ";

		if (!Util.isEmpty(whereclause, true))
			sql += " " + whereclause;

		//
		int permissionID = DB.getSQLValue(null, sql, DMS_Content_ID);

		return permissionID < 0 ? 0 : permissionID;
	} // getPermissionIDByUserRole

	public static int getPermissionByContentOrItsParent(I_DMS_Content content, int roleID, int userID)
	{
		I_DMS_Permission permission = MDMSPermission.getPermissionForGivenParams(content, roleID, userID);

		if (permission == null)
		{
			MDMSAssociation parentAssociation = MDMSAssociation.getParentAssociationFromContent(content.getDMS_Content_ID(), true, null);
			while (parentAssociation != null && parentAssociation.getDMS_Content_Related_ID() > 0)
			{
				MDMSContent parentContent = new MDMSContent(Env.getCtx(), parentAssociation.getDMS_Content_Related_ID(), null);
				if (MDMSContent.CONTENTBASETYPE_Directory.equals(parentContent.getContentBaseType()))
				{
					permission = MDMSPermission.getPermissionForGivenParams(content, roleID, userID);
					if (permission == null)
					{
						parentAssociation = MDMSAssociation.getParentAssociationFromContent(parentContent.getDMS_Content_ID(), true, null);
					}
					else
					{
						break;
					}
				}
				else
				{
					break;
				}
			}
		}
		return permission == null ? 0 : permission.getDMS_Permission_ID();
	} // getPermissionByContentOrItsParent

}
