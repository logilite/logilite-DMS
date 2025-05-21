package com.logilite.dms.model;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * DMS Content type filter use to filter out Content type based on Table_ID
 * 
 * @author Logilite Technology
 * @author Bhautik Panchal
 */
public class MDMSContentTypeFilter extends X_DMS_ContentType_Filter
{

	private static final long serialVersionUID = 5218105347767574761L;

	public MDMSContentTypeFilter(Properties ctx, int DMS_ContentType_Filter_ID, String trxName)
	{
		super(ctx, DMS_ContentType_Filter_ID, trxName);
	}

	public MDMSContentTypeFilter(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	/**
	 * Get active content type filters for system and current client.
	 *
	 * @return List of content type filters
	 */
	public static List<MDMSContentTypeFilter> getAllContentTypeFilterList()
	{
		Query query = new Query(Env.getCtx(), MTable.get(Env.getCtx(), Table_ID), "AD_Client_ID IN (0, ?)", null);
		return query.setParameters(Env.getAD_Client_ID(Env.getCtx()))
					.setOnlyActiveRecords(true)
					.list();
	} // getAllContentTypeFilterList

}
