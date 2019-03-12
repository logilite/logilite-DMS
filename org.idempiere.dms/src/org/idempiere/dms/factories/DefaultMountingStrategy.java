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

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.MDMSContent;

public class DefaultMountingStrategy implements IMountingStrategy
{

	private static CCache<String, MDMSContent>	mountingParentCache	= new CCache<String, MDMSContent>(null, "MountingParentCache", 50, false);

	@Override
	public String getMountingPath(PO po)
	{
		return this.getMountingPath(po.get_TableName(), po.get_ID());
	}

	@Override
	public String getMountingPath(String Table_Name, int Record_ID)
	{
		return DMSConstant.FILE_SEPARATOR + Utils.getDMSMountingBase(Env.getAD_Client_ID(Env.getCtx())) + DMSConstant.FILE_SEPARATOR + Table_Name
				+ DMSConstant.FILE_SEPARATOR + Record_ID;
	}

	@Override
	public MDMSContent getMountingParent(PO po)
	{
		return this.getMountingParent(po.get_Table_ID(), po.get_ID());
	}

	@Override
	public MDMSContent getMountingParent(String Table_Name, int Record_ID)
	{
		return getMountingParent(MTable.get(Env.getCtx(), Table_Name).getAD_Table_ID(), Record_ID);
	}

	@Override
	public MDMSContent getMountingParent(int AD_Table_ID, int Record_ID)
	{
		String key = AD_Table_ID + "_" + Record_ID;
		if (mountingParentCache.containsKey(key))
			return mountingParentCache.get(key);

		int DMS_Content_ID = DB.getSQLValue(null, DMSConstant.SQL_GET_MOUNTING_CONTENT_FOR_TABLE, String.valueOf(Record_ID), AD_Table_ID, Record_ID);

		if (DMS_Content_ID > 0)
		{
			MDMSContent content = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
			mountingParentCache.put(key, content);
			return content;
		}
		return null;
	} // getMountingParent

	@Override
	public MDMSContent getMountingParentForArchive()
	{
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		int DMS_Content_ID = DB.getSQLValue(null, DMSConstant.SQL_GET_MOUNTING_BASE_CONTENT, Utils.getDMSMountingArchiveBase(AD_Client_ID), AD_Client_ID);

		if (DMS_Content_ID > 0)
			return new MDMSContent(Env.getCtx(), DMS_Content_ID, null);

		return null;
	} // getMountingParentForArchive
}
