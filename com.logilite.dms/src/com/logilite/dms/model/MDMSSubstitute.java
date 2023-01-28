package com.logilite.dms.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.Env;

/**
 * DMS Substitute Configuration
 * 
 * @since 2020-Sep-15
 */
public class MDMSSubstitute extends X_DMS_Substitute
{

	/**
	 * 
	 */
	private static final long						serialVersionUID	= 13438943L;

	/**
	 * Cache expires in 30 Days
	 */
	private static CCache<Integer, MDMSSubstitute>	s_cache				= new CCache<Integer, MDMSSubstitute>(Table_Name, 2, 60 * 24 * 30);

	public MDMSSubstitute(Properties ctx, int DMS_Substitute_ID, String trxName)
	{
		super(ctx, DMS_Substitute_ID, trxName);
	}

	public MDMSSubstitute(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	/**
	 * get DMS Substitute record based on TableID
	 */
	public static MDMSSubstitute get(int tableID)
	{
		Integer key = Integer.valueOf(tableID);

		MDMSSubstitute retValue = (MDMSSubstitute) s_cache.get(key);
		if (!s_cache.containsKey(key) && retValue == null)
		{
			Query query = new Query(Env.getCtx(), Table_Name, " AD_Table_ID = ? ", null);
			query.setOnlyActiveRecords(true);
			query.setParameters(tableID);
			retValue = query.first();

			s_cache.put(key, retValue);
			// Note: null object store in cache otherwise it call query every time and looks this
			// configuration change rarely.
		}

		return retValue;
	} // get
}
