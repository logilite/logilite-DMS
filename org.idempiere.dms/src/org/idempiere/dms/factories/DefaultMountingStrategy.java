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

import java.io.File;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.util.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSVersion;

public class DefaultMountingStrategy implements IMountingStrategy
{

	protected static CCache<String, MDMSContent> mountingParentCache = new CCache<String, MDMSContent>(null, "MountingParentCache", 50, false);

	@Override
	public String getMountingPath(PO po)
	{
		return this.getMountingPath(po.get_TableName(), po.get_ID());
	}

	@Override
	public String getMountingPath(String Table_Name, int Record_ID)
	{
		return DMSConstant.FILE_SEPARATOR	+ Utils.getDMSMountingBase(Env.getAD_Client_ID(Env.getCtx())) + DMSConstant.FILE_SEPARATOR + Table_Name
				+ DMSConstant.FILE_SEPARATOR + Record_ID;
	}

	@Override
	public MDMSContent getMountingParent(String Table_Name, int Record_ID)
	{
		if (Util.isEmpty(Table_Name, true))
			return null;
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
		int DMS_Content_ID = DB.getSQLValue(null, DMSConstant.SQL_GET_ROOT_MOUNTING_BASE_CONTENT, Utils.getDMSMountingArchiveBase(AD_Client_ID), AD_Client_ID);

		if (DMS_Content_ID > 0)
			return new MDMSContent(Env.getCtx(), DMS_Content_ID, null);

		return null;
	} // getMountingParentForArchive

	/**
	 * Initialize Mounting Content
	 * 
	 * @param mountingBaseName
	 * @param table_Name
	 * @param Record_ID
	 * @param AD_Table_ID
	 */
	public void initiateMountingContent(String mountingBaseName, String table_Name, int Record_ID, int AD_Table_ID)
	{
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());

		/**
		 * Base Mounting
		 */
		IFileStorageProvider fileStorageProvider = FileStorageUtil.get(AD_Client_ID, false);
		String baseDir = fileStorageProvider.getBaseDirectory(null);
		File file = new File(baseDir + DMSConstant.FILE_SEPARATOR + mountingBaseName);

		int mountingContentID = DB.getSQLValue(null, DMSConstant.SQL_GET_ROOT_MOUNTING_BASE_CONTENT, mountingBaseName, AD_Client_ID);

		if (!file.exists())
		{
			file.mkdirs();
		}

		// Check if already DMS content created for Mounting Base Folder but storage moved or
		// something happen to prevent to create another content for same
		if (mountingContentID <= 0)
		{
			mountingContentID = MDMSContent.create(mountingBaseName, MDMSContent.CONTENTBASETYPE_Directory, null, true);
			MDMSAssociation.create(mountingContentID, 0, 0, 0, 0, null);
			MDMSVersion.create(mountingContentID, mountingBaseName, 0, file, null);
		}

		/**
		 * Table Name Mounting
		 */
		int tableNameContentID = 0;
		if (!Util.isEmpty(table_Name))
		{
			file = new File(baseDir + DMSConstant.FILE_SEPARATOR + mountingBaseName + DMSConstant.FILE_SEPARATOR + table_Name);

			tableNameContentID = DB.getSQLValue(null, DMSConstant.SQL_GET_SUB_MOUNTING_BASE_CONTENT, AD_Client_ID, mountingContentID, AD_Table_ID, table_Name);
			if (!file.exists())
			{
				file.mkdirs();
			}

			// Check if already DMS content created for Table
			if (tableNameContentID <= 0)
			{
				tableNameContentID = MDMSContent.create(table_Name, MDMSContent.CONTENTBASETYPE_Directory, DMSConstant.FILE_SEPARATOR + mountingBaseName, true);
				MDMSAssociation.create(tableNameContentID, mountingContentID, 0, AD_Table_ID, MDMSAssociationType.PARENT_ID, null);
				MDMSVersion.create(tableNameContentID, table_Name, 0, file, null);
			}
		}

		/**
		 * Record_ID Mounting
		 */
		if (tableNameContentID > 0 && Record_ID > 0)
		{
			file = new File(baseDir + DMSConstant.FILE_SEPARATOR + mountingBaseName + DMSConstant.FILE_SEPARATOR + table_Name + DMSConstant.FILE_SEPARATOR
							+ Record_ID);
			if (!file.exists())
			{
				file.mkdirs();
			}

			// Check if already DMS content created for Record
			int recordContentID = DB.getSQLValue(	null, DMSConstant.SQL_GET_SUB_MOUNTING_BASE_CONTENT + " AND a.Record_ID = ?", AD_Client_ID, tableNameContentID,
													AD_Table_ID, String.valueOf(Record_ID), Record_ID);
			if (recordContentID <= 0)
			{
				String parentURL = DMSConstant.FILE_SEPARATOR + mountingBaseName + DMSConstant.FILE_SEPARATOR + table_Name;
				recordContentID = MDMSContent.create(String.valueOf(Record_ID), MDMSContent.CONTENTBASETYPE_Directory, parentURL, true);
				MDMSAssociation.create(recordContentID, tableNameContentID, Record_ID, AD_Table_ID, MDMSAssociationType.PARENT_ID, null);
				MDMSVersion.create(recordContentID, String.valueOf(Record_ID), 0, file, null);
			}
		}
	} // initiateMountingContent
}
