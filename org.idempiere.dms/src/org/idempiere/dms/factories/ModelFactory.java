package org.idempiere.dms.factories;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.idempiere.model.MDMSMimeType;

public class ModelFactory implements IModelFactory
{

	@Override
	public Class<?> getClass(String tableName)
	{
		if (tableName.equals(MDMSMimeType.Table_Name))
			return MDMSMimeType.class;
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName)
	{
		if (tableName.equals(MDMSMimeType.Table_Name))
			return new MDMSMimeType(Env.getCtx(), Record_ID, trxName);
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName)
	{
		if (tableName.equals(MDMSMimeType.Table_Name))
			return new MDMSMimeType(Env.getCtx(), rs, trxName);
		return null;
	}

}
