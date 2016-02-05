package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class MDMSAssociationType extends X_DMS_AssociationType
{
	/**
	 * 
	 */
	private static final long				serialVersionUID	= 1L;
	private static CCache<Integer, Integer>	s_cache				= new CCache<Integer, Integer>("DMSVersionType", 2);

	public MDMSAssociationType(Properties ctx, int DMS_AssociationType_ID, String trxName)
	{
		super(ctx, DMS_AssociationType_ID, trxName);
	}

	public MDMSAssociationType(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public static int getVersionType()
	{
		Integer versionTypeID = s_cache.get(Env.getAD_Client_ID(Env.getCtx()));

		if (versionTypeID != null)
			return versionTypeID;

		versionTypeID = DB.getSQLValue(null,
				"SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE name ilike 'Version'");

		s_cache.put(Env.getAD_Client_ID(Env.getCtx()), versionTypeID);

		return versionTypeID;
	}

}
