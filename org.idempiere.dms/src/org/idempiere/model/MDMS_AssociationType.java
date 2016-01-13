package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MDMS_AssociationType extends X_DMS_AssociationType
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public MDMS_AssociationType(Properties ctx, int DMS_AssociationType_ID, String trxName)
	{
		super(ctx, DMS_AssociationType_ID, trxName);
	}

	public MDMS_AssociationType(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

}
