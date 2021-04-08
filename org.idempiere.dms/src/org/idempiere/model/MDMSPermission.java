package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Env;

public class MDMSPermission extends X_DMS_Permission
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8104520178505137205L;

	public MDMSPermission(Properties ctx, int DMS_Permission_ID, String trxName)
	{
		super(ctx, DMS_Permission_ID, trxName);
	}

	public MDMSPermission(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public static MDMSPermission getPermission(MDMSContent content)
	{
		return new MDMSPermission(Env.getCtx(), getPermissionID(content), content.get_TrxName());
	}

	public static int getPermissionID(MDMSContent content)
	{
		String sql = "SELECT " + COLUMNNAME_DMS_Permission_ID + " FROM " + Table_Name + " WHERE " + COLUMNNAME_DMS_Content_ID + " = ? ";
		int prtmissioID = DB.getSQLValue(content.get_TrxName(), sql + " AND " + COLUMNNAME_AD_User_ID + " = ? ", content.getDMS_Content_ID(), Env.getAD_User_ID(content.getCtx()));
		if (prtmissioID <= 0)
			prtmissioID = DB.getSQLValue(content.get_TrxName(), sql + " AND " + COLUMNNAME_AD_Client_ID + " = ? ", content.getDMS_Content_ID(), Env.getAD_Client_ID(content.getCtx()));

		return prtmissioID;
	}

}
