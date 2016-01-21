package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DB;

public class MDMS_MimeType extends X_DMS_MimeType
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -3814987450064904684L;

	public MDMS_MimeType(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public MDMS_MimeType(Properties ctx, int DMS_MimeType_ID, String trxName)
	{
		super(ctx, DMS_MimeType_ID, trxName);
	}

	public boolean beforeSave(boolean newRecord)
	{
		int count = DB.getSQLValue(null, "SELECT count(*) FROM DMS_MimeType WHERE MimeType ilike '" + getMimeType()
				+ "' OR FileExtension ilike '" + getFileExtension() + "'");
		if (count != 0)
		{
			throw new AdempiereException("MimeType and File Extension must be unique.");
		}
		return true;
	}
}
