package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MDMSAssociation extends X_DMS_Association
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public MDMSAssociation(Properties ctx, int DMS_Association_ID, String trxName)
	{
		super(ctx, DMS_Association_ID, trxName);
	}

	public MDMSAssociation(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

}
