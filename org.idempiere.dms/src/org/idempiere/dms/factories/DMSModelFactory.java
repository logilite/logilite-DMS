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
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.idempiere.model.MDMSMimeType;
import org.idempiere.model.MDMSPermission;
import org.idempiere.model.MDMSStatus;
import org.idempiere.model.MDMSSubstitute;
import org.idempiere.model.MDMSVersion;

public class DMSModelFactory implements IModelFactory
{

	@Override
	public Class<?> getClass(String tableName)
	{
		if (tableName.equals(MDMSMimeType.Table_Name))
			return MDMSMimeType.class;
		else if (tableName.equals(MDMSContent.Table_Name))
			return MDMSContent.class;
		else if (tableName.equals(MDMSAssociation.Table_Name))
			return MDMSAssociation.class;
		else if (tableName.equals(MDMSAssociationType.Table_Name))
			return MDMSAssociationType.class;
		else if (tableName.equals(MDMSContentType.Table_Name))
			return MDMSContentType.class;
		else if (tableName.equals(MDMSStatus.Table_Name))
			return MDMSStatus.class;
		else if (tableName.equals(MDMSSubstitute.Table_Name))
			return MDMSSubstitute.class;
		else if (tableName.equals(MDMSVersion.Table_Name))
			return MDMSVersion.class;
		else if (tableName.equals(MDMSPermission.Table_Name))
			return MDMSPermission.class;

		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName)
	{
		if (tableName.equals(MDMSMimeType.Table_Name))
			return new MDMSMimeType(Env.getCtx(), Record_ID, trxName);
		else if (tableName.equals(MDMSContent.Table_Name))
			return new MDMSContent(Env.getCtx(), Record_ID, trxName);
		else if (tableName.equals(MDMSAssociation.Table_Name))
			return new MDMSAssociation(Env.getCtx(), Record_ID, trxName);
		else if (tableName.equals(MDMSAssociationType.Table_Name))
			return new MDMSAssociationType(Env.getCtx(), Record_ID, trxName);
		else if (tableName.equals(MDMSContentType.Table_Name))
			return new MDMSContentType(Env.getCtx(), Record_ID, trxName);
		else if (tableName.equals(MDMSStatus.Table_Name))
			return new MDMSStatus(Env.getCtx(), Record_ID, trxName);
		else if (tableName.equals(MDMSSubstitute.Table_Name))
			return new MDMSSubstitute(Env.getCtx(), Record_ID, trxName);
		else if (tableName.equals(MDMSVersion.Table_Name))
			return new MDMSVersion(Env.getCtx(), Record_ID, trxName);
		else if (tableName.equals(MDMSPermission.Table_Name))
			return new MDMSPermission(Env.getCtx(), Record_ID, trxName);

		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName)
	{
		if (tableName.equals(MDMSMimeType.Table_Name))
			return new MDMSMimeType(Env.getCtx(), rs, trxName);
		else if (tableName.equals(MDMSContent.Table_Name))
			return new MDMSContent(Env.getCtx(), rs, trxName);
		else if (tableName.equals(MDMSAssociation.Table_Name))
			return new MDMSAssociation(Env.getCtx(), rs, trxName);
		else if (tableName.equals(MDMSAssociationType.Table_Name))
			return new MDMSAssociationType(Env.getCtx(), rs, trxName);
		else if (tableName.equals(MDMSContentType.Table_Name))
			return new MDMSContentType(Env.getCtx(), rs, trxName);
		else if (tableName.equals(MDMSStatus.Table_Name))
			return new MDMSStatus(Env.getCtx(), rs, trxName);
		else if (tableName.equals(MDMSSubstitute.Table_Name))
			return new MDMSSubstitute(Env.getCtx(), rs, trxName);
		else if (tableName.equals(MDMSVersion.Table_Name))
			return new MDMSVersion(Env.getCtx(), rs, trxName);
		else if (tableName.equals(MDMSPermission.Table_Name))
			return new MDMSPermission(Env.getCtx(), rs, trxName);

		return null;
	}
}
