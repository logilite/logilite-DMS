package org.idempiere.model;

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
	 * Cache expiere 60 * 24 * 30
	 */
	private static CCache<Integer, MDMSSubstitute>	s_cache				= new CCache<Integer, MDMSSubstitute>(Table_Name, 2, 43200);

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
		Integer key = new Integer(tableID);

		MDMSSubstitute retValue = (MDMSSubstitute) s_cache.get(key);
		if (retValue == null)
		{
			retValue = (MDMSSubstitute) new Query(Env.getCtx(), Table_Name, " AD_Table_ID = ? ", null)
							.setOnlyActiveRecords(true)
							.setParameters(tableID)
							.first();
			s_cache.put(key, retValue);
		}
		return retValue;

	} // get
}
