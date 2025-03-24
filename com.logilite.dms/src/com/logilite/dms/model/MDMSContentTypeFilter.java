package com.logilite.dms.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * DMS Content type filter use to filter out Content type based on Table_ID
 * 
 * @author Logilite Technology
 * @author Bhautik Panchal
 */
public class MDMSContentTypeFilter extends X_DMS_ContentType_Filter
{

	private static final long								serialVersionUID	= 5218105347767574761L;

	private static CCache<Integer, MDMSContentTypeFilter>	s_caCache			= new CCache<>(Table_Name, 10, 5);

	public MDMSContentTypeFilter(Properties ctx, int DMS_ContentType_Filter_ID, String trxName)
	{
		super(ctx, DMS_ContentType_Filter_ID, trxName);
	}

	public MDMSContentTypeFilter(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public static CCache<Integer, MDMSContentTypeFilter> getMDMSContentTypeFilterList(int tableID)
	{
		int[] ids = DB.getIDsEx(null, "SELECT DMS_ContentType_Filter_ID FROM DMS_ContentType_Filter WHERE AD_Table_ID = ? AND IsActive = 'Y'", tableID);
		for (int i = 0; i < ids.length; i++)
		{
			if (s_caCache.get(ids[i]) == null)
			{
				MDMSContentTypeFilter dmsFilter = new MDMSContentTypeFilter(Env.getCtx(), ids[i], null);
				s_caCache.put(dmsFilter.get_ID(), dmsFilter);
			}
		}
		return s_caCache;
	} // getMDMSContentTypeFilterList
}
