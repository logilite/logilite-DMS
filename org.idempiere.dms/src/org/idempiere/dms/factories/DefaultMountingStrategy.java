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

import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.MDMSContent;

public class DefaultMountingStrategy implements IMountingStrategy
{

	private final static String					DMS_MOUNTING_BASE				= MSysConfig.getValue("DMS_MOUNTING_BASE", "Attachment");

	private final static String					DMS_MOUNTING_ARCHIVE_BASE		= MSysConfig.getValue("DMS_MOUNTING_ARCHIVE_BASE", "Archive");

	private final static String					SQL_GET_ARCHIVE_BASE_CONTENT	= "SELECT DMS_Content_ID FROM DMS_Content WHERE name = ? AND AD_Client_ID= ? AND ContentBaseType ='DIR' AND ParentUrl IS NULL";

	private final static String					SQL_GET_DMSCONTENT				= "SELECT dc.DMS_Content_ID FROM DMS_Content dc "
																						+ " INNER JOIN DMS_Association da ON (dc.DMS_Content_ID = da.DMS_Content_ID) "
																						+ " WHERE dc.Name = ? AND dc.IsMounting = 'Y' AND da.AD_Table_ID = ? AND da.Record_ID = ?";

	private static CCache<String, MDMSContent>	mountingParentCache				= new CCache<String, MDMSContent>(null, "MountingParentCache", 50, false);

	private String								fileSeprator					= Utils.getStorageProviderFileSeparator();

	@Override
	public String getMountingPath(String Table_Name, int Record_ID)
	{
		return fileSeprator + DMS_MOUNTING_BASE + fileSeprator + Table_Name + fileSeprator + Record_ID;
	}

	@Override
	public String getMountingPath(PO po)
	{
		return fileSeprator + DMS_MOUNTING_BASE + fileSeprator + po.get_TableName() + fileSeprator + po.get_ID();
	}

	@Override
	public MDMSContent getMountingParent(String Table_Name, int Record_ID)
	{
		return getMountingParent(MTable.getTable_ID(Table_Name), Record_ID);
	}

	@Override
	public MDMSContent getMountingParent(int AD_Table_ID, int Record_ID)
	{
		String key = "" + AD_Table_ID + "_" + Record_ID;
		if (mountingParentCache.containsKey(key))
			return mountingParentCache.get(key);

		int DMS_Content_ID = DB.getSQLValue(null, SQL_GET_DMSCONTENT, String.valueOf(Record_ID), AD_Table_ID, Record_ID);

		if (DMS_Content_ID > 0)
		{
			MDMSContent content = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
			mountingParentCache.put(key, content);
			return content;
		}

		return null;
	}

	@Override
	public MDMSContent getMountingParent(PO po)
	{
		int DMS_Content_ID = DB.getSQLValue(null, SQL_GET_DMSCONTENT, String.valueOf(po.get_ID()), MTable.getTable_ID(po.get_TableName()), po.get_ID());

		if (DMS_Content_ID > 0)
		{
			return new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
		}

		return null;
	}

	@Override
	public MDMSContent getMountingParentForArchive()
	{
		int DMS_Content_ID = DB.getSQLValue(null, SQL_GET_ARCHIVE_BASE_CONTENT, DMS_MOUNTING_ARCHIVE_BASE, Env.getAD_Client_ID(Env.getCtx()));

		if (DMS_Content_ID > 0)
		{
			return new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
		}

		return null;
	}
}
