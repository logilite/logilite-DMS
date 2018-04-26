/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package org.idempiere.dms.factories;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSMimeType;

public class ModelFactory implements IModelFactory
{

	@Override
	public Class<?> getClass(String tableName)
	{
		if (tableName.equals(MDMSMimeType.Table_Name))
			return MDMSMimeType.class;
		if (tableName.equals(MDMSContent.Table_Name))
			return MDMSContent.class;
		
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName)
	{
		if (tableName.equals(MDMSMimeType.Table_Name))
			return new MDMSMimeType(Env.getCtx(), Record_ID, trxName);
		if (tableName.equals(MDMSContent.Table_Name))
			return new MDMSContent(Env.getCtx(), Record_ID, trxName);
		
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName)
	{
		if (tableName.equals(MDMSMimeType.Table_Name))
			return new MDMSMimeType(Env.getCtx(), rs, trxName);
		if (tableName.equals(MDMSContent.Table_Name))
			return new MDMSContent(Env.getCtx(), rs, trxName);
		
		return null;
	}

}
