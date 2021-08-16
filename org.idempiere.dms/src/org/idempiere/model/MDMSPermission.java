package org.idempiere.model;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.constant.DMSConstant;

public class MDMSPermission extends X_DMS_Permission
{
	/**
	 * 
	 */
	private static final long				serialVersionUID	= 8104520178505137205L;

	/** Cache for permission based on ContentID, RoleID, UserID */
	private static CCache<String, Integer>	cache_permission	= new CCache<String, Integer>("cache_permission", 100, 30);

	public MDMSPermission(Properties ctx, int DMS_Permission_ID, String trxName)
	{
		super(ctx, DMS_Permission_ID, trxName);
	}

	public MDMSPermission(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public static I_DMS_Permission[] getAllPermissionForContent(MDMSContent content)
	{
		Query query = new Query(content.getCtx(), Table_Name, " DMS_Content_ID = ? ", null);
		query.setClient_ID();
		query.setOnlyActiveRecords(true);
		query.setParameters(content.getDMS_Content_ID());
		List<MDMSPermission> permissionList = query.list();
		return permissionList.toArray(new MDMSPermission[permissionList.size()]);
	} // getAllPermissionForContent

	/**
	 * Get Permission record for the content with role/user
	 * 
	 * @param  content
	 * @param  roleID
	 * @param  userID
	 * @return         DMS_Permission_ID
	 */
	public static I_DMS_Permission getPermissionForGivenParams(I_DMS_Content content, int roleID, int userID)
	{
		return getPermissionForGivenParams(DMSConstant.SQL_GET_PERMISSION_ID_FROM_CONTENT,content, roleID, userID);
	}
	
	/**
	 * Get Permission record for the content with role/user
	 * 
	 * @param sqlPermission
	 * @param content
	 * @param roleID
	 * @param userID
	 * @return DMS_Permission_ID
	 */
	public static I_DMS_Permission getPermissionForGivenParams(String sqlPermission, I_DMS_Content content, int roleID, int userID)
	{
		MDMSPermission permission = null;
		String key = content.getDMS_Content_ID() + "_" + roleID + "_" + userID;
		if (cache_permission.containsKey(key))
		{
			permission = (MDMSPermission) MTable.get(Env.getCtx(), MDMSPermission.Table_ID).getPO(cache_permission.get(key), null);
			if (permission != null)
				return permission;

			cache_permission.remove(key);
		}

		int permissionID = DB.getSQLValue(null, sqlPermission, content.getDMS_Content_ID(), roleID, userID);

		permissionID = permissionID < 0 ? 0 : permissionID;
		if (permissionID > 0)
		{
			cache_permission.put(key, permissionID);
			permission = (MDMSPermission) MTable.get(Env.getCtx(), MDMSPermission.Table_ID).getPO(permissionID, null);
			if (permission != null)
				return permission;
		}

		return permission;
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{
		super.afterSave(newRecord, success);

		// if (newRecord)
		// {
		// System.out.println( "Permission Saved: " + get_ID()
		// + ", cntID=" + getDMS_Content_ID()
		// + ", Role=" + getAD_Role().getName()
		// + ", CreatedByID=" + getCreatedBy()
		// + ", UserID=" + getAD_User_ID()
		// + ", User=" + getAD_User().getName()
		// + ", Read=" + isRead()
		// + ", Write=" + isWrite()
		// + ", Delete=" + isDelete()
		// + ", Navigation=" + isNavigation()
		// + ", AllPermission=" + isAllPermission());
		// }
		return true;
	}
}
