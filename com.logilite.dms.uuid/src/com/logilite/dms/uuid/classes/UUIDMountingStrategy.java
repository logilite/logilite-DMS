package com.logilite.dms.uuid.classes;

import java.io.File;
import java.util.UUID;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IMountingStrategy;
import com.logilite.dms.model.FileStorageUtil;
import com.logilite.dms.model.IFileStorageProvider;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSAssociationType;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSVersion;
import com.logilite.dms.util.Utils;
import com.logilite.dms.uuid.util.UtilsUUID;

/**
 * UUID Mounting Strategy
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class UUIDMountingStrategy implements IMountingStrategy
{

	protected static CCache<String, MDMSContent> mountingParentCache = new CCache<String, MDMSContent>(null, "MountingParentUUIDCache", 50, false);

	@Override
	public String getMountingPath(PO po)
	{
		return this.getMountingPath(po.get_TableName(), po.get_ID());
	}

	@Override
	public String getMountingPath(String Table_Name, int Record_ID)
	{
		MDMSContent content = getMountingParent(Table_Name, Record_ID);

		return DMSConstant.FILE_SEPARATOR	+ Utils.getDMSMountingBase(Env.getAD_Client_ID(Env.getCtx()))
				+ DMSConstant.FILE_SEPARATOR + Table_Name
				+ DMSConstant.FILE_SEPARATOR + MDMSVersion.getLatestVersion(content).getDMS_Version_UU();
	}

	@Override
	public MDMSContent getMountingParent(String Table_Name, int Record_ID)
	{
		if (Util.isEmpty(Table_Name, true))
			return null;
		return getMountingParent(MTable.get(Env.getCtx(), Table_Name).getAD_Table_ID(), Record_ID);
	}

	/**
	 * Method to get Mounting parent with transaction
	 */
	@Override
	public MDMSContent getMountingParent(String Table_Name, int Record_ID, String trxName)
	{
		if (Util.isEmpty(Table_Name, true))
			return null;
		return getMountingParent(MTable.get(Env.getCtx(), Table_Name).getAD_Table_ID(), Record_ID, trxName);
	}

	@Override
	public MDMSContent getMountingParent(int AD_Table_ID, int Record_ID)
	{
		String key = AD_Table_ID + "_" + Record_ID;
		if (mountingParentCache.containsKey(key))
			return mountingParentCache.get(key);

		int DMS_Content_ID = DB.getSQLValue(null, DMSConstant.SQL_GET_MOUNTING_CONTENT_FOR_TABLE,
											Utils.getDMSMountingBase(Env.getAD_Client_ID(Env.getCtx())), AD_Table_ID, Record_ID);

		if (DMS_Content_ID > 0)
		{
			MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(DMS_Content_ID, null);
			mountingParentCache.put(key, content);
			return content;
		}
		return null;
	} // getMountingParent

	/**
	 * Method to get Mounting parent with transaction
	 */
	@Override
	public MDMSContent getMountingParent(int AD_Table_ID, int Record_ID, String trxName)
	{
		int DMS_Content_ID = DB.getSQLValue(trxName, DMSConstant.SQL_GET_MOUNTING_CONTENT_FOR_TABLE,
											Utils.getDMSMountingBase(Env.getAD_Client_ID(Env.getCtx())), AD_Table_ID, Record_ID);

		if (DMS_Content_ID > 0)
		{
			MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(DMS_Content_ID, trxName);
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

	@Override
	public MDMSContent getMountingParentForArchive(int AD_Table_ID, int Record_ID, int Process_ID)
	{
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		String archiveBase = Utils.getDMSMountingArchiveBase(AD_Client_ID);
		String trxName = Trx.createTrxName("DMSArcMnt_");
		Trx trx = Trx.get(trxName, true);
		//
		MDMSContent content = null;

		try
		{
			int mountingContentID = DB.getSQLValue(trxName, DMSConstant.SQL_GET_ROOT_MOUNTING_BASE_CONTENT, archiveBase, AD_Client_ID);
			if (mountingContentID <= 0)
			{
				mountingContentID = MDMSContent.create(archiveBase, MDMSContent.CONTENTBASETYPE_Directory, null, true, trxName);
				MDMSAssociation.create(mountingContentID, 0, 0, 0, 0, trxName);
				MDMSVersion.create(mountingContentID, archiveBase, 0, null, trxName);
			}

			if (Process_ID > 0)
			{
				MProcess process = MProcess.get(Env.getCtx(), Process_ID);
				String pValue = process.getValue();

				int processContentID = DB.getSQLValue(trxName, DMSConstant.SQL_GET_SUB_MOUNTING_BASE_CNT_PROCESS, AD_Client_ID, mountingContentID, pValue);
				if (processContentID <= 0)
				{
					processContentID = MDMSContent.create(	pValue, MDMSContent.CONTENTBASETYPE_Directory,
															DMSConstant.FILE_SEPARATOR + archiveBase, true, trxName);
					MDMSAssociation.create(processContentID, mountingContentID, 0, 0, MDMSAssociationType.PARENT_ID, trxName);
					MDMSVersion.create(processContentID, pValue, 0, null, trxName);
				}

				content = new MDMSContent(Env.getCtx(), processContentID, trxName);
			}
			else
			{
				String tableName = MTable.getTableName(Env.getCtx(), AD_Table_ID);

				int tableNameContentID = DB.getSQLValue(trxName, DMSConstant.SQL_GET_SUB_MOUNTING_BASE_CONTENT,
														AD_Client_ID, mountingContentID, AD_Table_ID, tableName);

				if (tableNameContentID <= 0)
				{
					tableNameContentID = MDMSContent.create(tableName, MDMSContent.CONTENTBASETYPE_Directory,
															DMSConstant.FILE_SEPARATOR + archiveBase, true, trxName);
					MDMSAssociation.create(tableNameContentID, mountingContentID, 0, AD_Table_ID, MDMSAssociationType.PARENT_ID, trxName);
					MDMSVersion.create(tableNameContentID, tableName, 0, null, trxName);
				}

				int recordContentID = DB.getSQLValue(	trxName, DMSConstant.SQL_GET_SUB_MOUNTING_BASE_CONTENT + " AND a.Record_ID = ?", AD_Client_ID,
														tableNameContentID, AD_Table_ID, String.valueOf(Record_ID), Record_ID);

				if (recordContentID <= 0)
				{
					String uuid = UUID.randomUUID().toString();

					String parentURL = DMSConstant.FILE_SEPARATOR + archiveBase + DMSConstant.FILE_SEPARATOR + tableName;
					recordContentID = MDMSContent.create(String.valueOf(Record_ID), MDMSContent.CONTENTBASETYPE_Directory, parentURL, true, trxName);
					MDMSAssociation.create(recordContentID, tableNameContentID, Record_ID, AD_Table_ID, MDMSAssociationType.PARENT_ID, trxName);
					UtilsUUID.createVersionUU(uuid, recordContentID, 0, null, trxName);
				}

				// if record related content is not found then return it's parent table content
				if (recordContentID <= 0)
					content = new MDMSContent(Env.getCtx(), tableNameContentID, trxName);
				if (recordContentID > 0)
					content = new MDMSContent(Env.getCtx(), recordContentID, trxName);
			}
			trx.commit(true);
		}
		catch (Exception e)
		{
			trx.rollback();
			throw new AdempiereException("UUID Parent Mounting Archive failed. Error: " + e.getLocalizedMessage(), e);
		}
		finally
		{
			trx.close();
		}

		// Reload po without trx
		content.load(null);
		//
		return content;
	} // getMountingParentForArchive

	/**
	 * Initialize Mounting Content
	 * 
	 * @param mountingBaseName
	 * @param table_Name
	 * @param Record_ID
	 * @param AD_Table_ID
	 */
	public void initiateMountingContent(String mountingBaseName, String table_Name, int Record_ID, int AD_Table_ID, String trxName)
	{
		boolean isNewTrx = Util.isEmpty(trxName, true);
		if (isNewTrx)
			trxName = Trx.createTrxName("DMSMount_");
		Trx mountTrx = Trx.get(trxName, true);

		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		try
		{
			/**
			 * Base Mounting
			 */
			IFileStorageProvider fileStorageProvider = FileStorageUtil.get(AD_Client_ID, false);
			String baseDir = fileStorageProvider.getBaseDirectory(null);
			File file = new File(baseDir + DMSConstant.FILE_SEPARATOR + mountingBaseName);

			int mountingContentID = DB.getSQLValue(trxName, DMSConstant.SQL_GET_ROOT_MOUNTING_BASE_CONTENT, mountingBaseName, AD_Client_ID);

			if (!file.exists())
			{
				file.mkdirs();
			}

			// Check if already DMS content created for Mounting Base Folder but storage moved or
			// something happen to prevent to create another content for same
			if (mountingContentID <= 0)
			{
				mountingContentID = MDMSContent.create(mountingBaseName, MDMSContent.CONTENTBASETYPE_Directory, null, true, trxName);
				MDMSAssociation.create(mountingContentID, 0, 0, 0, 0, trxName);
				MDMSVersion.create(mountingContentID, mountingBaseName, 0, file, trxName);
			}

			/**
			 * Table Name Mounting
			 */
			int tableNameContentID = 0;
			if (!Util.isEmpty(table_Name))
			{
				file = new File(baseDir + DMSConstant.FILE_SEPARATOR + mountingBaseName + DMSConstant.FILE_SEPARATOR + table_Name);

				tableNameContentID = DB.getSQLValue(trxName, DMSConstant.SQL_GET_SUB_MOUNTING_BASE_CONTENT, AD_Client_ID, mountingContentID,
													AD_Table_ID, table_Name);
				if (!file.exists())
				{
					file.mkdirs();
				}

				// Check if already DMS content created for Table
				if (tableNameContentID <= 0)
				{
					tableNameContentID = MDMSContent.create(table_Name, MDMSContent.CONTENTBASETYPE_Directory,
															DMSConstant.FILE_SEPARATOR + mountingBaseName, true, trxName);
					MDMSAssociation.create(tableNameContentID, mountingContentID, 0, AD_Table_ID, MDMSAssociationType.PARENT_ID, trxName);
					MDMSVersion.create(tableNameContentID, table_Name, 0, file, trxName);
				}
			}

			/**
			 * Record_ID Mounting
			 */
			if (tableNameContentID > 0 && Record_ID > 0)
			{
				// Check if already DMS content created for Record
				int recordContentID = DB.getSQLValue(	trxName, DMSConstant.SQL_GET_SUB_MOUNTING_BASE_CONTENT + " AND a.Record_ID = ?", AD_Client_ID,
														tableNameContentID, AD_Table_ID, String.valueOf(Record_ID), Record_ID);
				if (recordContentID <= 0)
				{
					String uuid = UUID.randomUUID().toString();

					file = new File(baseDir + DMSConstant.FILE_SEPARATOR + mountingBaseName
									+ DMSConstant.FILE_SEPARATOR + table_Name
									+ DMSConstant.FILE_SEPARATOR + uuid);

					if (!file.exists())
					{
						file.mkdirs();
					}
					String parentURL = DMSConstant.FILE_SEPARATOR + mountingBaseName + DMSConstant.FILE_SEPARATOR + table_Name;
					recordContentID = MDMSContent.create(String.valueOf(Record_ID), MDMSContent.CONTENTBASETYPE_Directory, parentURL, true, trxName);
					MDMSAssociation.create(recordContentID, tableNameContentID, Record_ID, AD_Table_ID, MDMSAssociationType.PARENT_ID, trxName);
					UtilsUUID.createVersionUU(uuid, recordContentID, 0, file, trxName);
				}
			}
			if (isNewTrx)
				mountTrx.commit(true);
		}
		catch (Exception e)
		{
			if (isNewTrx)
				mountTrx.rollback();
			throw new AdempiereException("Default Mounting is not created,\n Error: " + e.getLocalizedMessage(), e);
		}
		finally
		{
			if (isNewTrx)
				mountTrx.close();
		}
	} // initiateMountingContent

}
