package org.idempiere.dms.factories;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.DMSPermission;
import org.idempiere.dms.util.DMSOprUtils;
import org.idempiere.dms.util.DMSPermissionUtils;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSPermission;

public class DMSPermissionValidator implements IPermission
{

	private static final String	SQL_CONTENT_PERMISSION_ID	= "SELECT DMS_Permission_ID FROM DMS_Permission WHERE DMS_Content_ID = ?  	"
																	+ "	AND (COALESCE(AD_Role_ID, 0) = 0 OR COALESCE(AD_Role_ID,0) = ?) "
																	+ "	AND (COALESCE(AD_User_ID,0) = 0  OR COALESCE(AD_User_ID,0) = ?) "
																	+ "	ORDER BY AD_Role_ID, AD_User_ID;								";

	boolean						isRead						= true;
	boolean						isWrite						= true;
	boolean						isDelete					= true;
	boolean						isNavigation				= true;
	boolean						isAllPermission				= true;

	@Override
	public MDMSPermission createPermission(DMSPermission permission, MDMSContent content, boolean isCreateForSubContent)
	{
		int permissionID = DMSPermissionUtils.getPermissionIDByUserRole(content.getDMS_Content_ID(), permission.getAD_Role_ID(), permission.getAD_User_ID());
		MDMSPermission dmsPermission = new MDMSPermission(Env.getCtx(), permissionID, null);
		dmsPermission.setAD_Org_ID(permission.getAD_Org_ID());
		dmsPermission.setDMS_Content_ID(permission.getDMS_Content_ID());
		dmsPermission.setDMS_Owner_ID(permission.getDMS_Owner_ID());
		if (permission.getAD_Role_ID() != null)
			dmsPermission.setAD_Role_ID(permission.getAD_Role_ID());
		if (permission.getAD_User_ID() != null)
			dmsPermission.setAD_User_ID(permission.getAD_User_ID());
		dmsPermission.setIsRead(permission.IsRead());
		dmsPermission.setIsWrite(permission.IsWrite());
		dmsPermission.setIsDelete(permission.IsDelete());
		dmsPermission.setIsNavigation(MDMSContent.CONTENTBASETYPE_Directory.equals(content.getContentBaseType()) && permission.IsNavigation());
		dmsPermission.setIsAllPermission(permission.IsAllPermission());
		dmsPermission.saveEx();

		// Create Permission for the sub content
		if (isCreateForSubContent)
		{
			createPermissionForSubContent(permission, content);
		}

		return dmsPermission;
	}

	@Override
	public MDMSPermission createContentPermission(MDMSContent content)
	{
		int permissionID = getLoginRoleUserPermission(content.getDMS_Content_ID(), content.get_TrxName(), content.getCtx());
		MDMSPermission permission = new MDMSPermission(Env.getCtx(), permissionID, content.get_TrxName());
		permission.setAD_Org_ID(content.getAD_Org_ID());
		permission.setDMS_Content_ID(content.getDMS_Content_ID());

		if (content.isMounting())
		{
			permission.setDMS_Owner_ID(100);
			permission.setIsRead(false);
			permission.setIsWrite(false);
			permission.setIsDelete(false);
			permission.setIsNavigation(false);
			permission.setIsAllPermission(true);
		}
		else
		{
			permission.setDMS_Owner_ID(content.getCreatedBy());
			permission.setAD_User_ID(content.getCreatedBy());
			permission.setAD_Role_ID(Env.getAD_Role_ID(Env.getCtx()));
			permission.setDMS_Owner_ID(content.getCreatedBy());
			permission.setIsRead(true);
			permission.setIsWrite(true);
			permission.setIsDelete(true);
			permission.setIsNavigation(MDMSContent.CONTENTBASETYPE_Directory.equals(content.getContentBaseType()));
			permission.setIsAllPermission(false);
		}
		permission.saveEx();
		return permission;
	}

	@Override
	public boolean validateContentPermission(MDMSContent content)
	{
		int no = DB.getSQLValue(content.get_TrxName(), "SELECT COUNT(1) FROM DMS_Permission WHERE DMS_Content_ID = ? ", content.getDMS_Content_ID());
		if (no <= 0)
			return true;
		no = DB.getSQLValue(content.get_TrxName(), "SELECT COUNT(1) FROM DMS_Permission WHERE DMS_Content_ID = ? AND IsAllPermission = 'Y'AND AD_User_ID IS NULL AND AD_Role_ID IS NULL", content.getDMS_Content_ID());
		if (no > 0)
			return true;
		if (getLoginRoleUserPermission(content.getDMS_Content_ID(), content.get_TrxName(), content.getCtx()) > 0)
			return true;

		return false;
	}

	@Override
	public void initContentPermission(MDMSContent content)
	{
		int permissionID = getPermissionIDByContent(content);
		if (permissionID > 0)
		{
			MDMSPermission permission = (MDMSPermission) MTable.get(content.getCtx(), MDMSPermission.Table_ID).getPO(permissionID, content.get_TrxName());
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
	}

	@Override
	public boolean isRead( )
	{
		return isRead;
	}

	@Override
	public boolean isWrite( )
	{
		return isWrite;
	}

	@Override
	public boolean isDelete( )
	{
		return isDelete;
	}

	@Override
	public boolean isNavigation( )
	{
		return isNavigation;
	}

	@Override
	public boolean isAllPermission( )
	{
		return isAllPermission;
	}

	private int getPermissionIDByContent(MDMSContent content)
	{
		int permissionID = getLoginRoleUserPermission(content.getDMS_Content_ID(), content.get_TrxName(), content.getCtx());

		if (permissionID <= 0)
		{
			MDMSAssociation parentAssociation = MDMSAssociation.getParentAssociationFromContent(content.getDMS_Content_ID(), true, content.get_TrxName());
			while (parentAssociation != null && parentAssociation.getDMS_Content_Related_ID() > 0)
			{
				MDMSContent parentContent = new MDMSContent(Env.getCtx(), parentAssociation.getDMS_Content_Related_ID(), null);
				if (MDMSContent.CONTENTBASETYPE_Directory.equals(parentContent.getContentBaseType()))
				{
					permissionID = getLoginRoleUserPermission(content.getDMS_Content_ID(), content.get_TrxName(), content.getCtx());
					if (permissionID <= 0)
						parentAssociation = MDMSAssociation.getParentAssociationFromContent(parentContent.getDMS_Content_ID(), true, content.get_TrxName());
					else
						break;
				}
				else
					break;
			}
		}
		return permissionID;
	}// getPermissionIDByContent

	private int getLoginRoleUserPermission(int content_ID, String trxName, Properties ctx)
	{
		int sqlPermission_ID = DB.getSQLValue(trxName, SQL_CONTENT_PERMISSION_ID, content_ID, Env.getAD_Role_ID(ctx), Env.getAD_User_ID(ctx));
		return sqlPermission_ID < 0 ? 0 : sqlPermission_ID;
	}// getLoginRoleUserPermission

	private void createPermissionForSubContent(DMSPermission parentPermission, MDMSContent content)
	{
		HashMap <I_DMS_Content, I_DMS_Association> relatedContentMap = DMSOprUtils.getRelatedContents(content, true, content.get_TrxName());
		for (Map.Entry <I_DMS_Content, I_DMS_Association> entry : relatedContentMap.entrySet())
		{
			MDMSContent dmsContent = (MDMSContent) entry.getKey();

			int permissionID = DMSPermissionUtils.getPermissionIDByUserRole(dmsContent.getDMS_Content_ID(), parentPermission.getAD_Role_ID(), parentPermission.getAD_User_ID());
			MDMSPermission permission = new MDMSPermission(Env.getCtx(), permissionID, null);
			permission.setAD_Org_ID(parentPermission.getAD_Org_ID());
			permission.setDMS_Content_ID(dmsContent.getDMS_Content_ID());
			permission.setDMS_Owner_ID(parentPermission.getDMS_Owner_ID());
			if (parentPermission.getAD_Role_ID() != null)
				permission.setAD_Role_ID(parentPermission.getAD_Role_ID());
			if (parentPermission.getAD_User_ID() != null)
				permission.setAD_User_ID(parentPermission.getAD_User_ID());
			permission.setIsRead(parentPermission.IsRead());
			permission.setIsWrite(parentPermission.IsWrite());
			permission.setIsDelete(parentPermission.IsDelete());
			permission.setIsNavigation(MDMSContent.CONTENTBASETYPE_Directory.equals(dmsContent.getContentBaseType()) && parentPermission.IsNavigation());
			permission.setIsAllPermission(parentPermission.IsAllPermission());
			permission.saveEx();

			if (MDMSContent.CONTENTBASETYPE_Directory.equals(dmsContent.getContentBaseType()))
			{
				createPermissionForSubContent(parentPermission, dmsContent);
			}
		}
	}// createPermissionForSubContent
}
