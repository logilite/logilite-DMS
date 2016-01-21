package org.idempiere.dms.factories;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.idempiere.model.MDMS_MimeType;

public class ModelFactory implements IModelFactory
{

	@Override
	public Class<?> getClass(String tableName)
	{
		if (tableName.equals(MDMS_MimeType.Table_Name))
			return MDMS_MimeType.class;
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName)
	{
		if (tableName.equals(MDMS_MimeType.Table_Name))
			return new MDMS_MimeType(Env.getCtx(), Record_ID, trxName);
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName)
	{
		if (tableName.equals(MDMS_MimeType.Table_Name))
			return new MDMS_MimeType(Env.getCtx(), rs, trxName);
		return null;
	}

}
