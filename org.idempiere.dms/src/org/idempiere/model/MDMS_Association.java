package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MDMS_Association extends X_DMS_Association
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public MDMS_Association(Properties ctx, int DMS_Association_ID, String trxName)
	{
		super(ctx, DMS_Association_ID, trxName);
	}

	public MDMS_Association(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

}
