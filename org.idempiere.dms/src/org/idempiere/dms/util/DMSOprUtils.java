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

package org.idempiere.dms.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.compiere.Adempiere;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.idempiere.model.MDMSVersion;

/**
 * Util for DMS related operations.
 * <ul>
 * Operation Like
 * <li>Create Directory
 * <li>Create File
 * <li>File Store and Thumbnail
 * <li>Paste Physical Copied Folder / Content
 * <li>Paste Copy FileContent / DirContent
 * <li>Paste Cut Content
 * <li>Rename Content / Directory
 * <li>Create Link
 * <li>Create Index for Linkable Content
 * <li>Delete Content [ AS In Active ]
 * <li>Delete Content with Physical Document
 * <li>etc...
 * </ul>
 * 
 * @author Sachin Bhimani
 */
public class DMSOprUtils
{

	/**
	 * Create File, if directory not exists then create
	 * 
	 * @param  dms
	 * @param  dirPath
	 * @param  file
	 * @param  fileName
	 * @param  desc
	 * @param  contentType
	 * @param  attributeMap
	 * @param  AD_Table_ID
	 * @param  Record_ID
	 * @param  isVersion
	 * @return              New contentID
	 */
	public static int addFile(	DMS dms, String dirPath, File file, String fileName, String desc, String contentType, Map<String, String> attributeMap,
								int AD_Table_ID, int Record_ID, boolean isVersion)
	{
		int asiID = 0;
		int contentTypeID = 0;
		MDMSContent dirContent = dms.getMountingStrategy().getMountingParent(AD_Table_ID, Record_ID);

		fileName = Utils.validateFileName(dirContent, file, fileName, isVersion);

		String trxName = Trx.createTrxName("AddFiles");
		Trx trx = Trx.get(trxName, true);

		// Create Directory folder hierarchy OR get leaf DMS-Content
		if (!Util.isEmpty(dirPath, true) && !dirPath.equals(DMSConstant.FILE_SEPARATOR))
		{
			dirContent = dms.createDirHierarchy(dirPath, dirContent, dms.getSubstituteTableInfo().getOriginTable_ID(),
												dms.getSubstituteTableInfo().getOriginRecord_ID(), trx.getTrxName());
		}

		if (!isVersion && contentType != null)
		{
			contentTypeID = MDMSContentType.getContentTypeIDFromName(contentType, dms.AD_Client_ID);

			MDMSContentType cType = new MDMSContentType(Env.getCtx(), contentTypeID, trx.getTrxName());
			if (attributeMap != null && !attributeMap.isEmpty())
				asiID = Utils.createOrUpdateASI(attributeMap, 0, cType.getM_AttributeSet_ID(), trx.getTrxName());
		}

		try
		{
			trx.commit(true);
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Error while committing transaction:" + e.getLocalizedMessage(), e);
		}
		//
		return addFile(dms, dirContent, file, fileName, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, isVersion);

	}// addFile

	/**
	 * Add File
	 * 
	 * @param  dms
	 * @param  parentContent
	 * @param  file
	 * @param  fileName
	 * @param  desc
	 * @param  contentTypeID
	 * @param  asiID
	 * @param  AD_Table_ID
	 * @param  Record_ID
	 * @param  isVersion
	 * @return               New ContentID
	 */
	public static int addFile(	DMS dms, MDMSContent parentContent, File file, String fileName, String desc, int contentTypeID, int asiID, int AD_Table_ID,
								int Record_ID, boolean isVersion)
	{
		boolean isError = false;
		fileName = Utils.validateFileName(parentContent, file, fileName, isVersion);

		String trxName = Trx.createTrxName("UploadFile");
		Trx trx = Trx.get(trxName, true);

		// Create Content, Association, Store File & Thumbnail generate
		try
		{
			return createContentAssociationFileStoreAndThumnail(dms, parentContent, file, fileName, desc, contentTypeID, asiID, AD_Table_ID, Record_ID,
																isVersion, trx.getTrxName());
		}
		catch (Exception e)
		{
			isError = true;
			throw new AdempiereException("Upload Content Failure:\n" + e.getLocalizedMessage());
		}
		finally
		{
			if (trx != null)
			{
				if (isError)
				{
					trx.rollback();
				}
				else
				{
					trx.commit();
				}
				trx.close();
			}
		}
	} // addFile

	/**
	 * Create DMS Content, Association, File store in StorageProvider and Thumbnail generate
	 * 
	 * @param  dms
	 * @param  parentContent
	 * @param  file
	 * @param  fileName
	 * @param  desc
	 * @param  contentTypeID
	 * @param  asiID
	 * @param  AD_Table_ID
	 * @param  Record_ID
	 * @param  isVersion
	 * @param  trxName
	 * @return               New ContentID
	 */
	public static int createContentAssociationFileStoreAndThumnail(	DMS dms, MDMSContent parentContent, File file, String fileName, String desc,
																	int contentTypeID, int asiID, int AD_Table_ID, int Record_ID, boolean isVersion,
																	String trxName)
	{
		int seqNo = 0;
		int DMS_Content_Related_ID = 0;
		int DMS_AssociationType_ID = 0;
		String actualFileName = fileName;
		String parentURL;
		int dms_content_id = 0;

		if (isVersion)
		{
			actualFileName = dms.getActualFileOrDirName(DMSConstant.CONTENT_FILE, parentContent, null, null, MDMSAssociationType.TYPE_VERSION,
														DMSConstant.OPERATION_CREATE);
			fileName = parentContent.getName();
			parentURL = parentContent.getParentURL();
			asiID = parentContent.getM_AttributeSetInstance_ID();
			contentTypeID = parentContent.getDMS_ContentType_ID();
			DMS_Content_Related_ID = parentContent.getDMS_Content_Related_ID();
			DMS_AssociationType_ID = 0;

			seqNo = DB.getSQLValue(trxName, DMSConstant.SQL_GET_VERSION_SEQ_NO, parentContent.get_ID(), dms.AD_Client_ID);
		}
		else
		{
			String format = Utils.getFileExtension(fileName);
			if (format == null)
				format = Utils.getFileExtension(file.getName());
			if (format == null)
				throw new AdempiereException("Did not found file extension: " + fileName + " " + file.getName());

			if (!fileName.endsWith(format))
			{
				actualFileName = dms.getActualFileOrDirName(DMSConstant.CONTENT_FILE, parentContent, fileName, format, MDMSAssociationType.TYPE_PARENT,
															DMSConstant.OPERATION_CREATE);
				fileName = fileName + format;
			}
			else
			{
				actualFileName = dms.getActualFileOrDirName(DMSConstant.CONTENT_FILE, parentContent, FilenameUtils.getBaseName(fileName), format,
															MDMSAssociationType.TYPE_PARENT, DMSConstant.OPERATION_CREATE);
			}
			parentURL = dms.getPathFromContentManager(MDMSVersion.getLatestVersion(parentContent));

			if (parentContent != null && parentContent.getDMS_Content_ID() > 0)
			{
				DMS_Content_Related_ID = parentContent.getDMS_Content_ID();
				DMS_AssociationType_ID = MDMSAssociationType.PARENT_ID;
			}
		}

		// check if file exists
		if (!isVersion)
		{
			dms_content_id = Utils.checkExistsFileDir(parentURL, fileName, true);
		}

		if (dms_content_id > 0)
		{
			throw new AdempiereException("File already exists, either rename or upload as a version. \n (Either same file name content exist in inActive mode)");
		}
		else
		{
			if (isVersion)
			{
				// Create DMS Version record
				MDMSVersion version = MDMSVersion.create(parentContent.getDMS_Content_ID(), actualFileName, seqNo, file, trxName);

				// File write on Storage provider and create thumbnail
				DMSOprUtils.writeFileOnStorageAndThumnail(dms, file, version);
			}
			else
			{
				// Create Content
				int contentID = MDMSContent.create(fileName, MDMSContent.CONTENTBASETYPE_Content, parentURL, desc, file, contentTypeID, asiID, false, trxName);
				// Create Association
				dms.createAssociation(contentID, DMS_Content_Related_ID, Record_ID, AD_Table_ID, DMS_AssociationType_ID, trxName);
				// Create DMS Version record
				MDMSVersion version = MDMSVersion.create(contentID, actualFileName, seqNo, file, trxName);

				// File write on Storage provider and create thumbnail
				DMSOprUtils.writeFileOnStorageAndThumnail(dms, file, version);

				parentContent = (MDMSContent) version.getDMS_Content();
			}

			return parentContent.get_ID();
		}
	} // createContentAssociationFileStoreAndThumnail

	public static void updateContentTypeAndAttribute(DMS dms, int contentID, String contentType, Map<String, String> attributeMap)
	{
		String trxName = Trx.createTrxName("UpdateAttrs");
		Trx trx = Trx.get(trxName, true);

		try
		{
			int contentTypeID = 0;
			int asiID = 0;

			MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(contentID, trx.getTrxName());
			if (!Util.isEmpty(contentType, true))
			{
				contentTypeID = MDMSContentType.getContentTypeIDFromName(contentType, dms.AD_Client_ID);
				MDMSContentType cType = (MDMSContentType) MTable.get(Env.getCtx(), MDMSContentType.Table_ID).getPO(contentTypeID, trx.getTrxName());
				if (attributeMap != null && !attributeMap.isEmpty())
					asiID = Utils.createOrUpdateASI(attributeMap, content.getM_AttributeSetInstance_ID(), cType.getM_AttributeSet_ID(), trx.getTrxName());
			}

			content.setDMS_ContentType_ID(contentTypeID);
			content.setM_AttributeSetInstance_ID(asiID);
			content.saveEx();
		}
		finally
		{
			if (trx != null)
			{
				trx.close();
			}
		}
	}

	/**
	 * File write on Storage provider and create thumbnail
	 * 
	 * @param dms
	 * @param file
	 * @param version
	 */
	public static void writeFileOnStorageAndThumnail(DMS dms, File file, MDMSVersion version)
	{
		byte[] data = null;
		try
		{
			data = Files.readAllBytes(file.toPath());
		}
		catch (IOException e)
		{
			throw new AdempiereException("Error while reading file:" + version.getValue(), e);
		}

		IFileStorageProvider fsProvider = dms.getFileStorageProvider();
		fsProvider.writeBLOB(dms.getBaseDirPath(version), data);

		IThumbnailGenerator thumbnailGenerator = DMSFactoryUtils.getThumbnailGenerator(dms, version.getDMS_Content().getDMS_MimeType().getMimeType());
		if (thumbnailGenerator != null)
			thumbnailGenerator.addThumbnail(version, file, null);
	} // writeFileOnStorageAndThumnail

	/**
	 * Create Directory Hierarchy or Get leaf DMSContent from dirPath
	 * 
	 * @param  dms
	 * @param  dirPath
	 * @param  dirContent
	 * @param  AD_Table_ID
	 * @param  Record_ID
	 * @param  trxName
	 * @return             {@link MDMSContent} - Leaf node
	 */
	public static MDMSContent createDirHierarchy(DMS dms, String dirPath, MDMSContent dirContent, int AD_Table_ID, int Record_ID, String trxName)
	{
		// For Ref '\\' = '\', "\\" = "\\\\"
		String[] dirs;
		if (Adempiere.getOSInfo().startsWith("Windows"))
		{
			dirs = dirPath.split("\\\\");
		}
		else
		{
			dirs = dirPath.split(DMSConstant.FILE_SEPARATOR);
		}
		for (int i = 0; i < dirs.length; i++)
		{
			String dir = dirs[i].trim();
			if (!Util.isEmpty(dir, true))
			{
				dirContent = createDirectory(dms, dir, dirContent, AD_Table_ID, Record_ID, false, true, trxName);
			}
		}
		return dirContent;
	} // createDirHierarchy

	/**
	 * Create Directory
	 * 
	 * @param  dirName             - Directory Name
	 * @param  dmsContent          - Root or Parent Directory or Null
	 * @param  AD_Table_ID         - Table ID
	 * @param  Record_ID           - Record ID
	 * @param  fileStorageProvider - File Storage Provider
	 * @param  contentMngr         - Content Manager
	 * @param  errorIfDirExists    - Throw error if directory Exists
	 * @param  isCreateAssociation
	 * @param  trxName             - Transaction Name
	 * @return                     DMS Content
	 */
	public static MDMSContent createDirectory(	DMS dms, String dirContentName, MDMSContent parentContent, int AD_Table_ID, int Record_ID,
												boolean errorIfDirExists, boolean isCreateAssociation, String trxName)
	{
		int contentID = 0;

		dirContentName = dirContentName.trim();

		String error = Utils.isValidFileName(dirContentName, true);
		if (!Util.isEmpty(error))
			throw new AdempiereException(error);

		try
		{
			MDMSVersion parentVersion = (MDMSVersion) MDMSVersion.getLatestVersion(parentContent);
			String baseURL = dms.getPathFromContentManager(parentVersion);
			File rootFolder = new File(dms.getBaseDirPath(parentVersion));
			if (!rootFolder.exists())
				rootFolder.mkdirs();
			String dirName = dirContentName;
			int DMS_Content_ID = Utils.checkExistsFileDir(baseURL, dirContentName);
			String dirPath = rootFolder.getPath() + DMSConstant.FILE_SEPARATOR + dirContentName;
			if (DMS_Content_ID > 0)
			{
				MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_Name).getPO(DMS_Content_ID, null);
				String existingDir = dms.getContentManager().getPathByName(content);
				if (existingDir.equalsIgnoreCase(dirPath) && errorIfDirExists)
				{
					File newDir = new File(dirPath);
					if (!newDir.exists())
					{
						throw new AdempiereException(Msg.getMsg(Env.getCtx(),
																"Directory already exists. \n (Either same file name content exist in inActive mode)"));
					}
				}
				else if (errorIfDirExists)
				{
					throw new AdempiereException(Msg.getMsg(Env.getCtx(),
															"Directory already exists. \n (Either same file name content exist in inActive mode)"));
				}
			}
			else
			{
				dirName = dms.getContentManager().getContentName(	dms.getFileStorageProvider(), DMSConstant.CONTENT_DIR, parentContent, dirContentName, "", "",
																	DMSConstant.OPERATION_CREATE);
				File newFile = new File(rootFolder.getPath() + DMSConstant.FILE_SEPARATOR + dirName);
				// File newFile = new File(dirName);
				if (!newFile.exists())
				{
					if (!newFile.mkdir())
						throw new AdempiereException(	"Something went wrong!\nDirectory is not created:\n'"
														+ dms.getPathFromContentManager(parentVersion)
														+ DMSConstant.FILE_SEPARATOR + newFile.getName() + "'");
				}
			}

			// Get directory content ID
			List<Object> params = new ArrayList<Object>();
			params.add(Env.getAD_Client_ID(Env.getCtx()));
			params.add(AD_Table_ID);
			params.add(Record_ID);
			params.add(dirName);

			String sql = "SELECT dc.DMS_Content_ID FROM DMS_Content dc "
							+ "INNER JOIN DMS_Association da ON (dc.DMS_Content_ID = da.DMS_Content_ID) "
							+ "WHERE dc.IsActive = 'Y' AND dc.ContentBaseType = 'DIR' AND da.AD_Client_ID = ? AND NVL(da.AD_Table_ID, 0) = ? AND da.Record_ID = ? AND dc.Name = ? ";

			if (parentContent == null)
				sql += "AND dc.ParentURL IS NULL";
			else
			{
				sql += "AND dc.ParentURL = ? ";
				params.add(dms.getPathFromContentManager(parentVersion));
			}

			contentID = DB.getSQLValueEx(trxName, sql, params);

			if (contentID <= 0)
			{
				contentID = MDMSContent.create(	dirContentName, MDMSContent.CONTENTBASETYPE_Directory, dms.getPathFromContentManager(parentVersion), false,
												trxName);
				if (isCreateAssociation)
					MDMSAssociation.create(contentID, (parentContent != null) ? parentContent.getDMS_Content_ID() : 0, Record_ID, AD_Table_ID, 0, trxName);

				// Create DMS Version
				MDMSVersion.create(contentID, dirContentName, 0, null, trxName);
			}

			parentContent = new MDMSContent(Env.getCtx(), contentID, trxName);

		}
		catch (Exception e)
		{
			throw new AdempiereException(e.getLocalizedMessage(), e);
		}

		return parentContent;
	} // createDirectory

	/**
	 * Paste copied folder into physical storage
	 * 
	 * @param  dms
	 * @param  copiedContent    - Copied Content
	 * @param  destPasteContent - Paste Content Destination
	 * @return                  FileName
	 */
	public static String pastePhysicalCopiedFolder(DMS dms, MDMSContent copiedContent, MDMSContent destPasteContent, String actualName)
	{
		String newFileName = dms.getBaseDirPath(destPasteContent);

		File newFile = new File(newFileName + DMSConstant.FILE_SEPARATOR + actualName);

		if (newFile.exists())
			return actualName;
		if (!newFile.mkdir())
			throw new AdempiereException(	"Something went wrong!\nDirectory is not created:\n'"	+ dms.getPathFromContentManager(destPasteContent)
											+ DMSConstant.FILE_SEPARATOR + actualName + "'");

		return actualName;
	} // pastePhysicalCopiedFolder

	/**
	 * Paste Physical Copied content
	 * 
	 * @param  dms
	 * @param  oldVersion       - Copied Content
	 * @param  destPasteContent - Destination Content
	 * @param  fileName         - FileName
	 * @return                  FileName
	 */
	public static String pastePhysicalCopiedContent(DMS dms, MDMSVersion oldVersion, MDMSContent destPasteContent, String fileName)
	{
		File oldFile = new File(dms.getBaseDirPath(oldVersion));
		String actualFileName = "";
		String newFileName = dms.getBaseDirPath(destPasteContent);

		if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
			actualFileName += oldVersion.getValue();
		else
			actualFileName += DMSConstant.FILE_SEPARATOR + oldVersion.getValue();

		actualFileName = dms.getActualFileOrDirName(DMSConstant.CONTENT_FILE, destPasteContent, oldVersion.getValue(), "", "", DMSConstant.OPERATION_COPY);

		File newFile = new File(newFileName + DMSConstant.FILE_SEPARATOR + actualFileName);

		try
		{
			FileUtils.copyFile(oldFile, newFile);
		}
		catch (IOException e)
		{
			DMS.log.log(Level.SEVERE, "Copy Content Failure.", e);
			throw new AdempiereException("Copy Content Failure." + e.getLocalizedMessage());
		}

		return newFile.getName();
	} // pastePhysicalCopiedContent

	/**
	 * Paste copy file content
	 * 
	 * @param dms
	 * @param copiedContent - Copied File Content
	 * @param destContent   - Destination Content
	 * @param tableID       - AD_Table_ID
	 * @param recordID      - Record_ID
	 */
	public static void pasteCopyFileContent(DMS dms, MDMSContent copiedContent, MDMSContent destContent, int tableID, int recordID)
	{
		String fileName = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		int DMS_Content_ID = (new MDMSContent(Env.getCtx(), copiedContent.getDMS_Content_ID(), null)).getDMS_Content_Related_ID();

		String trxName = Trx.createTrxName("copy-paste");
		Trx trx = Trx.get(trxName, true);

		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_ASSOCIATION_FOR_COPY_PASTE, null);
			pstmt.setInt(1, DMS_Content_ID);
			pstmt.setInt(2, MDMSAssociationType.PARENT_ID);
			pstmt.setInt(3, DMS_Content_ID);
			pstmt.setInt(4, MDMSAssociationType.LINK_ID);
			rs = pstmt.executeQuery();

			while (rs.next())
			{
				copiedContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), trx.getTrxName());

				// Copy Content
				MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, trx.getTrxName());
				PO.copyValues(copiedContent, newDMSContent);
				if (copiedContent.getM_AttributeSetInstance_ID() > 0)
				{
					MAttributeSetInstance newASI = Utils.copyASI(copiedContent.getM_AttributeSetInstance_ID(), trx.getTrxName());
					newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
				}
				newDMSContent.saveEx();

				// Copy Association
				MDMSAssociation oldDMSAssociation = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
				MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, trx.getTrxName());
				PO.copyValues(oldDMSAssociation, newDMSAssociation);
				newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
				if (oldDMSAssociation.getDMS_AssociationType_ID() == MDMSAssociationType.PARENT_ID)
				{
					if (destContent != null && destContent.getDMS_Content_ID() > 0)
						newDMSAssociation.setDMS_Content_Related_ID(destContent.getDMS_Content_ID());
					else
						newDMSAssociation.setDMS_Content_Related_ID(0);
				}
				else
				{
					newDMSAssociation.setDMS_Content_Related_ID(newDMSContent.getDMS_Content_ID());
				}
				newDMSAssociation.updateTableRecordRef(tableID, recordID);

				// Copy Version
				boolean isContentSaved = false;
				for (MDMSVersion oldVersion : MDMSVersion.getVersionHistory(copiedContent))
				{
					String baseURL = null;
					if (!Util.isEmpty(copiedContent.getParentURL()))
						baseURL = dms.getPathFromContentManager(oldVersion);
					else
						baseURL = DMSConstant.FILE_SEPARATOR + oldVersion.getValue();

					baseURL = baseURL.substring(0, baseURL.lastIndexOf(DMSConstant.FILE_SEPARATOR));

					fileName = pastePhysicalCopiedContent(dms, oldVersion, destContent, fileName);

					MDMSVersion newVersion = new MDMSVersion(Env.getCtx(), 0, trx.getTrxName());
					PO.copyValues(oldVersion, newVersion);
					newVersion.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
					newVersion.setValue(fileName);
					newVersion.saveEx();

					if (!isContentSaved)
					{
						String renamedURL = dms.getPathFromContentManager(destContent);
						String associationType = MDMSContent.getContentAssociationType(copiedContent.getDMS_Content_ID());

						String name = "";
						if (associationType.equals(MDMSAssociationType.TYPE_PARENT))
							name = Utils.getCopyFileContentName(dms.getPathFromContentManager(newVersion), copiedContent.getName(), copiedContent.getName());
						else
							name = newDMSContent.getName();

						newDMSContent.setParentURL(renamedURL);
						newDMSContent.setName(name);
						newDMSContent.saveEx();

						// Once content is saved then no need to update again for another versioning
						// iterating
						isContentSaved = true;
					}

					//
					IThumbnailGenerator thumbnailGenerator = DMSFactoryUtils.getThumbnailGenerator(dms, newDMSContent.getDMS_MimeType().getMimeType());

					if (thumbnailGenerator != null)
						thumbnailGenerator.addThumbnail(newVersion, dms.getFileFromStorage(oldVersion), null);
				}

				trx.commit();
			}
		}
		catch (Exception e)
		{
			trx.rollback();
			DMS.log.log(Level.SEVERE, "Error while paste Copy File Content", e);
			throw new AdempiereException(e);
		}
		finally
		{
			trx.close();
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	} // pasteCopyFileContent

	/**
	 * Paste the content [ Copy Operation ]
	 * 
	 * @param dms           - DMS
	 * @param copiedContent - Content From
	 * @param destContent   - Content To
	 * @param tableID       - AD_Table_ID
	 * @param recordID      - Record_ID
	 */
	public static void pasteCopyContent(DMS dms, MDMSContent copiedContent, MDMSContent destContent, int tableID, int recordID)
	{
		if (destContent != null && destContent.getDMS_Content_ID() > 0)
		{
			MDMSAssociation destAssociation = dms.getParentAssociationFromContent(destContent.getDMS_Content_ID());
			tableID = destAssociation.getAD_Table_ID();
			recordID = destAssociation.getRecord_ID();
		}

		if (copiedContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			// check name exists
			String newName = Utils.getCopyDirContentName(destContent, copiedContent.getName(), dms.getContentManager().getPathByName(destContent));
			String newActualName = dms.getActualFileOrDirName(	DMSConstant.CONTENT_DIR, destContent, copiedContent.getName(), "", "",
																DMSConstant.OPERATION_COPY);

			// Copy Content
			MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, null);
			PO.copyValues(copiedContent, newDMSContent);
			if (copiedContent.getM_AttributeSetInstance_ID() > 0)
			{
				MAttributeSetInstance newASI = Utils.copyASI(copiedContent.getM_AttributeSetInstance_ID(), null);
				newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
			}
			newDMSContent.setParentURL(dms.getPathFromContentManager(destContent));
			newDMSContent.saveEx();

			// Copy Association
			MDMSAssociation oldDMSAssociation = dms.getParentAssociationFromContent(copiedContent.getDMS_Content_ID());
			MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, null);
			PO.copyValues(oldDMSAssociation, newDMSAssociation);
			newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
			if (destContent != null && destContent.getDMS_Content_ID() > 0)
				newDMSAssociation.setDMS_Content_Related_ID(destContent.getDMS_Content_ID());
			else
				newDMSAssociation.setDMS_Content_Related_ID(0);
			newDMSAssociation.updateTableRecordRef(tableID, recordID);

			// Note: Must save association first other wise creating issue of wrong info in solr
			// indexing entry
			newDMSContent.setName(newName);
			newDMSContent.saveEx();

			// Create Folder
			String contentname = pastePhysicalCopiedFolder(dms, copiedContent, destContent, newActualName);

			// Copy Version
			MDMSVersion oldVersion = (MDMSVersion) MDMSVersion.getLatestVersion(copiedContent);
			MDMSVersion newVersion = new MDMSVersion(Env.getCtx(), 0, null);
			PO.copyValues(oldVersion, newVersion);
			newVersion.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
			newVersion.setValue(contentname);
			newVersion.saveEx();

			//
			String baseURL = dms.getPathFromContentManager(copiedContent);
			String renamedURL = dms.getPathFromContentManager(destContent) + DMSConstant.FILE_SEPARATOR + oldVersion.getValue();

			pasteCopyDirContent(dms, copiedContent, newDMSContent, baseURL, renamedURL, tableID, recordID);

			//
			dms.grantChildPermissionFromParentContent(newDMSContent, destContent);
		}
		else
		{
			pasteCopyFileContent(dms, copiedContent, destContent, tableID, recordID);
		}

	} // pasteCopyContent

	/**
	 * Paste the Copy Directory Content
	 * 
	 * @param dms              - DMS
	 * @param copiedContent    - Content From
	 * @param destPasteContent - Content To
	 * @param baseURL          - Base URL
	 * @param renamedURL       - Renamed URL
	 * @param tableID          - AD_Table_ID
	 * @param recordID         - Record_ID
	 */
	public static void pasteCopyDirContent(	DMS dms, MDMSContent copiedContent, MDMSContent destPasteContent, String baseURL, String renamedURL, int tableID,
											int recordID)
	{
		HashMap<I_DMS_Version, I_DMS_Association> map = dms.getDMSContentsWithAssociation(copiedContent, dms.AD_Client_ID, true);
		for (Entry<I_DMS_Version, I_DMS_Association> mapEntry : map.entrySet())
		{
			MDMSContent oldDMSContent = (MDMSContent) ((MDMSVersion) mapEntry.getKey()).getDMS_Content();
			MDMSAssociation oldDMSAssociation = (MDMSAssociation) mapEntry.getValue();
			if (oldDMSContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
			{
				String trxName = Trx.createTrxName("pasteCopyDir");
				Trx trx = Trx.get(trxName, true);

				//
				MDMSContent newDMSContent = dms.createDirectory(oldDMSContent.getName(), destPasteContent, dms.getSubstituteTableInfo().getOriginTable_ID(),
																dms.getSubstituteTableInfo().getOriginRecord_ID(), true, true, trx.getTrxName());
				if (copiedContent.getM_AttributeSetInstance_ID() > 0)
				{
					MAttributeSetInstance newASI = Utils.copyASI(copiedContent.getM_AttributeSetInstance_ID(), trx.getTrxName());
					newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
				}

				// Note: Must save association first other wise creating issue of wrong info in solr
				// indexing entry
				newDMSContent.saveEx();

				//
				trx.commit();
				trx.close();

				if (!Util.isEmpty(oldDMSContent.getParentURL()))
				{
					if (oldDMSContent.getParentURL().startsWith(baseURL))
					{
						newDMSContent.setParentURL(dms.getPathFromContentManager(destPasteContent));
						newDMSContent.saveEx();
					}
					DMSOprUtils.pasteCopyDirContent(dms, oldDMSContent, newDMSContent, baseURL, renamedURL, tableID, recordID);
				}
			}
			else if (MDMSAssociationType.isLink(oldDMSAssociation))
			{
				dms.createAssociation(	oldDMSAssociation.getDMS_Content_ID(), destPasteContent.getDMS_Content_ID(),
										dms.getSubstituteTableInfo().getOriginRecord_ID(), dms.getSubstituteTableInfo().getOriginTable_ID(),
										MDMSAssociationType.LINK_ID, null);
			}
			else
			{
				pasteCopyFileContent(dms, oldDMSContent, destPasteContent, tableID, recordID);
			}
		}
	} // pasteCopyDirContent

	/**
	 * Paste the content [ Cut operation ]
	 * 
	 * @param dms         - DMS
	 * @param cutContent  - Content From
	 * @param destContent - Content To
	 * @param tableID     - AD_Table_ID
	 * @param recordID    - Record_ID
	 */
	public static void pasteCutContent(DMS dms, MDMSContent cutContent, MDMSContent destContent, int tableID, int recordID, boolean isDocExplorerWindow)
	{
		// check if exists..
		int DMS_Content_ID = Utils.checkExistsFileDir(dms.getContentManager().getPathByName(destContent), cutContent.getName());
		if (DMS_Content_ID > 0)
		{
			throw new AdempiereException("Content already exists.");
		}

		if (destContent != null && destContent.getDMS_Content_ID() > 0)
		{
			MDMSAssociation destAssociation = dms.getParentAssociationFromContent(destContent.getDMS_Content_ID());
			tableID = destAssociation.getAD_Table_ID();
			recordID = destAssociation.getRecord_ID();
		}

		if (cutContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			String baseURL = null;
			String renamedURL = null;

			if (!Util.isEmpty(cutContent.getParentURL()))
				baseURL = dms.getPathFromContentManager(cutContent);
			else
				baseURL = DMSConstant.FILE_SEPARATOR + cutContent.getName();

			File dirPath = new File(dms.getBaseDirPath(cutContent));
			String newFileName = dms.getBaseDirPath(destContent);

			if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
				newFileName = newFileName + cutContent.getName();
			else
				newFileName = newFileName + DMSConstant.FILE_SEPARATOR + cutContent.getName();

			File newFile = new File(newFileName);

			renamedURL = dms.getPathFromContentManager(destContent) + DMSConstant.FILE_SEPARATOR + cutContent.getName();

			DMSOprUtils.renameFolder(cutContent, baseURL, renamedURL, tableID, recordID, isDocExplorerWindow);
			dirPath.renameTo(newFile);

			MDMSAssociation association = dms.getParentAssociationFromContent(cutContent.getDMS_Content_ID());
			if (destContent != null && destContent.getDMS_Content_ID() > 0)
			{
				association.setDMS_Content_Related_ID(destContent.getDMS_Content_ID());
				cutContent.setParentURL(dms.getPathFromContentManager(destContent));
			}
			else
			{
				association.setDMS_Content_Related_ID(0);
				cutContent.setParentURL(null);
			}

			association.updateTableRecordRef(tableID, recordID);

			// Note: Must save association first other wise creating issue of wrong info in solr
			// indexing entry
			cutContent.saveEx();
		}
		else
		{
			MDMSContent dmsContent = new MDMSContent(Env.getCtx(), cutContent.getDMS_Content_Related_ID(), null);

			// Moving Versioning Content and its association
			int contentExists = Utils.checkExistsFileDir(dms.getPathFromContentManager(destContent), dmsContent.getName());
			if (contentExists > 0)
				throw new AdempiereException("File is already exist.");
			else
			{
				List<MDMSVersion> versionList = MDMSVersion.getVersionHistory(dmsContent);
				for (MDMSVersion version : versionList)
				{
					moveFile(dms, version, destContent);
				}
			}

			MDMSAssociation association = MDMSAssociation.getParentAssociationFromContent(dmsContent.getDMS_Content_ID(), true, null);
			if (association.getDMS_AssociationType_ID() == MDMSAssociationType.PARENT_ID
				|| (association.getDMS_AssociationType_ID() == 0 && MDMSContent.CONTENTBASETYPE_Content.equals(dmsContent.getContentBaseType())))
			{
				if (destContent != null && destContent.getDMS_Content_ID() == 0)
					destContent = null;

				if (destContent == null)
					association.setDMS_Content_Related_ID(0);
				else
					association.setDMS_Content_Related_ID(destContent.getDMS_Content_ID());
			}
			association.updateTableRecordRef(tableID, recordID);

			// Note: Must save association first other wise creating issue of wrong info in
			// solr indexing entry
			dmsContent.setParentURL(dms.getPathFromContentManager(destContent));
			dmsContent.saveEx();
		}

		dms.grantChildPermissionFromParentContent(cutContent, destContent);
	} // pasteCutContent

	/**
	 * Move File
	 * 
	 * @param dms
	 * @param version
	 * @param destContent
	 */
	public static void moveFile(DMS dms, MDMSVersion version, MDMSContent destContent)
	{
		String newPath = dms.getBaseDirPath(destContent);
		newPath = newPath + DMSConstant.FILE_SEPARATOR + version.getValue();

		File oldFile = new File(dms.getFileFromStorage(version).getAbsolutePath());
		File newFile = new File(newPath);

		if (!newFile.exists())
			oldFile.renameTo(newFile);
		else
			throw new AdempiereException("File is already exist.");
	} // moveFile

	/**
	 * Rename Content
	 * 
	 * @param dms      - DMS
	 * @param fileName - New FileName
	 * @param content  - Content
	 */
	public static void renameContent(DMS dms, String fileName, MDMSContent content)
	{
		int DMS_Content_ID = Utils.checkExistsFileDir(content.getParentURL(), fileName);
		if (DMS_Content_ID > 0)
		{
			throw new AdempiereException("File with same name Exists");
		}
		else
		{
			// check if parent or not
			String associationType = MDMSContent.getContentAssociationType(content.getDMS_Content_ID());
			if (associationType.equalsIgnoreCase(MDMSAssociationType.TYPE_RECORD))
			{
				List<MDMSContent> dmsContentList = content.getVersionRelatedContentList();
				for (MDMSContent dc : dmsContentList)
				{
					associationType = MDMSContent.getContentAssociationType(dc.getDMS_Content_ID());
					String actualFileName = "";
					MDMSVersion dmsVersion = (MDMSVersion) MDMSVersion.getLatestVersion(dc);
					if (associationType.equalsIgnoreCase(MDMSAssociationType.TYPE_RECORD))
					{
						String count = dmsVersion.getValue().substring(dmsVersion.getValue().lastIndexOf("(") + 1, dmsVersion.getValue().lastIndexOf(")"));
						actualFileName = dms.getActualFileOrDirName(DMSConstant.CONTENT_FILE, content, FilenameUtils.getBaseName(fileName) + "(" + count + ")",
																	Utils.getFileExtension(fileName), MDMSAssociationType.TYPE_RECORD,
																	DMSConstant.OPERATION_RENAME);
					}
					else
					{
						actualFileName = dms.getActualFileOrDirName(DMSConstant.CONTENT_FILE, content, FilenameUtils.getBaseName(fileName),
																	Utils.getFileExtension(fileName), MDMSAssociationType.TYPE_VERSIONPARENT,
																	DMSConstant.OPERATION_RENAME);
					}

					String newFileName = dms.getBaseDirPath(dc);
					newFileName = newFileName.substring(0, newFileName.lastIndexOf(DMSConstant.FILE_SEPARATOR));
					if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
						newFileName = newFileName + actualFileName;
					else
						newFileName = newFileName + DMSConstant.FILE_SEPARATOR + actualFileName;

					if (!new File(dms.getBaseDirPath(dc)).renameTo(new File(newFileName)))
						throw new AdempiereException("Invalid File Name.");

					dc.setName(fileName);
					dc.save();

					dmsVersion.setValue(actualFileName);
					dmsVersion.save();
				}
			}
			else
			{
				String actualName = dms.getActualFileOrDirName(	DMSConstant.CONTENT_FILE, content, FilenameUtils.getBaseName(fileName),
																Utils.getFileExtension(fileName), MDMSAssociationType.TYPE_PARENT,
																DMSConstant.OPERATION_RENAME);

				String newFileName = dms.getBaseDirPath(content);
				newFileName = newFileName.substring(0, newFileName.lastIndexOf(DMSConstant.FILE_SEPARATOR));
				if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
					newFileName = newFileName + actualName;
				else
					newFileName = newFileName + DMSConstant.FILE_SEPARATOR + actualName;

				if (!new File(dms.getBaseDirPath(content)).renameTo(new File(newFileName)))
					throw new AdempiereException("Invalid File Name.");

				content.setName(fileName);
				content.save();

				MDMSVersion dmsVersion = (MDMSVersion) MDMSVersion.getLatestVersion(content);
				dmsVersion.setValue(actualName);
				dmsVersion.saveEx();
			}
		}
	} // renameContent

	/**
	 * @param      dms                 - DMS
	 * @param      fileName            - New Filename
	 * @param      content             - Content
	 * @param      parent_Content      - Parent Content
	 * @param      tableID             - AD_Table_ID
	 * @param      recordID            - Record_ID
	 * @param      isDocExplorerWindow - is Document Explorer Window
	 * @deprecated                     After version table implementation no required to utilize
	 *                                 this method, Just need to re-index its all versioning content
	 *                                 if it is a content document.
	 */
	public static void renameContent(	DMS dms, String fileName, MDMSContent content, MDMSContent parent_Content, int tableID, int recordID,
										boolean isDocExplorerWindow)
	{
		fileName = fileName.trim();

		if (content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			if (!fileName.equalsIgnoreCase(parent_Content.getName()))
			{
				String baseURL = dms.getPathFromContentManager(content);

				String newFileName = dms.getBaseDirPath(content);
				newFileName = newFileName.substring(0, newFileName.lastIndexOf(DMSConstant.FILE_SEPARATOR));

				int DMS_Content_ID = Utils.checkExistsFileDir(content.getParentURL(), fileName);
				if (DMS_Content_ID > 0)
				{
					throw new AdempiereException("Directory already exists.");
				}
				else
				{
					String renamedURL = null;
					if (!Util.isEmpty(content.getParentURL()))
						renamedURL = content.getParentURL() + DMSConstant.FILE_SEPARATOR + fileName;
					else
						renamedURL = DMSConstant.FILE_SEPARATOR + fileName;

					String actualFileName = dms.getActualFileOrDirName(DMSConstant.CONTENT_DIR, content, fileName, "", "", DMSConstant.OPERATION_RENAME);

					if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
						newFileName = newFileName + actualFileName;
					else
						newFileName = newFileName + DMSConstant.FILE_SEPARATOR + actualFileName;

					if (!new File(dms.getBaseDirPath(content)).renameTo(new File(newFileName)))
						throw new AdempiereException("Invalid File Name.");

					content.setName(fileName);
					content.saveEx();

					MDMSVersion dmsVersion = (MDMSVersion) MDMSVersion.getLatestVersion(content);
					dmsVersion.setValue(actualFileName);
					dmsVersion.saveEx();

					DMSOprUtils.renameFolder(content, baseURL, renamedURL, tableID, recordID, isDocExplorerWindow);
				}
			}
		}
		else
		{
			if (!fileName.equalsIgnoreCase(parent_Content.getName().substring(0, parent_Content.getName().lastIndexOf("."))))
			{
				fileName = fileName + "." + FilenameUtils.getExtension(content.getName());
				renameContent(dms, fileName, content);
			}
		}
	} // renameContent

	public static void renameContentOnly(DMS dms, MDMSContent content, String fileName, String description, boolean isDocExplorerWindow)
	{
		fileName = fileName.trim();

		int DMS_Content_ID = Utils.checkExistsFileDir(content.getParentURL(), fileName);
		if (DMS_Content_ID > 0)
		{
			throw new AdempiereException("Same directory already exists.");
		}

		if (content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Content))
		{
			fileName = fileName + "." + FilenameUtils.getExtension(content.getName());
		}

		content.setName(fileName);
		if (!Util.isEmpty(description, true))
			content.setDescription(description);
		content.saveEx();
	} // renameContentOnly

	/**
	 * Rename Folder
	 * 
	 * @param content
	 * @param baseURL
	 * @param renamedURL
	 * @param tableID
	 * @param recordID
	 * @param isDocExplorerWindow
	 */
	public static void renameFolder(MDMSContent content, String baseURL, String renamedURL, int tableID, int recordID, boolean isDocExplorerWindow)
	{
		HashMap<I_DMS_Version, I_DMS_Association> map = DMSSearchUtils.getDMSContentsWithAssociation(	content, content.getAD_Client_ID(),
																										DMSConstant.DOCUMENT_VIEW_ALL_VALUE);

		for (Entry<I_DMS_Version, I_DMS_Association> mapEntry : map.entrySet())
		{
			MDMSContent dmsContent = (MDMSContent) ((MDMSVersion) mapEntry.getKey()).getDMS_Content();
			MDMSAssociation associationWithContent = (MDMSAssociation) mapEntry.getValue();

			// Do not change association type is link for the content
			if (MDMSAssociationType.isLink(associationWithContent))
				continue;

			if (dmsContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
			{
				String parentURL = dmsContent.getParentURL() == null ? "" : dmsContent.getParentURL();
				if (parentURL.startsWith(baseURL))
					dmsContent.setParentURL(Utils.replacePath(baseURL, renamedURL, parentURL));
				DMSOprUtils.renameFolder(dmsContent, baseURL, renamedURL, tableID, recordID, isDocExplorerWindow);

				MDMSAssociation associationDir = MDMSAssociation.getParentAssociationFromContent(dmsContent.getDMS_Content_ID(), false, null);
				associationDir.updateTableRecordRef(tableID, recordID);

				// Note: Must save association first other wise creating issue of wrong info in solr
				// indexing entry
				dmsContent.saveEx();
			}
			else
			{
				Query query = new Query(Env.getCtx(), MDMSAssociation.Table_Name, " DMS_Content_ID = ? AND DMS_AssociationType_ID <> 1000003 ", null);
				query.setClient_ID();
				query.setParameters(dmsContent.getDMS_Content_ID());
				query.setOrderBy(MDMSAssociation.COLUMNNAME_DMS_Association_ID);
				List<MDMSAssociation> associationList = query.list();

				boolean isUpdateContent = true;
				for (MDMSAssociation association : associationList)
				{
					if (isUpdateContent && dmsContent.getParentURL().startsWith(baseURL))
						dmsContent.setParentURL(Utils.replacePath(baseURL, renamedURL, dmsContent.getParentURL()));

					association.updateTableRecordRef(tableID, recordID);

					// Note: Must save association first other wise creating issue of wrong info in
					// solr indexing entry
					if (isUpdateContent)
					{
						dmsContent.saveEx();
						isUpdateContent = false;
					}
				}
			}
		}
	} // renameFolder

	/**
	 * Rename File
	 * 
	 * @param dms
	 * @param content
	 * @param fileName
	 * @param isAddFileExtention
	 */
	public static void renameFile(DMS dms, MDMSContent content, String fileName, boolean isAddFileExtention)
	{
		HashMap<I_DMS_Version, I_DMS_Association> map = dms.getDMSContentsWithAssociation(content, dms.AD_Client_ID, false);
		for (Map.Entry<I_DMS_Version, I_DMS_Association> entry : map.entrySet())
		{
			if (entry.getKey().getDMS_Content().getName().equals(fileName))
				throw new AdempiereException(fileName + " filename already exists.");
		}

		// Renaming to correct if any linkable docs exists for re-indexing
		content.setName(fileName);
		content.saveEx();
	} // renameFile

	/**
	 * Create Link of the content
	 * 
	 * @param  dms
	 * @param  contentParent
	 * @param  clipboardContent
	 * @param  isDir
	 * @param  tableID
	 * @param  recordID
	 * @return                  Error message if any
	 */
	public static String createLink(DMS dms, MDMSContent contentParent, MDMSContent clipboardContent, boolean isDir, int tableID, int recordID)
	{
		if (clipboardContent == null)
			return "";

		int linkableContentID = clipboardContent.getDMS_Content_ID();

		if (contentParent != null && dms.isHierarchyContentExists(contentParent.getDMS_Content_ID(), linkableContentID))
		{
			return "You can't create link of parent content into itself or its children content";
		}

		boolean isDocPresent = DMSOprUtils.isDocumentPresent(contentParent, clipboardContent, isDir);
		if (isDocPresent)
		{
			return "Document already exists at same position.";
		}

		int contentID = 0;
		int associationID = 0;
		int contentRelatedID = 0;

		if (contentParent != null && contentParent.getDMS_Content_ID() > 0)
			contentRelatedID = contentParent.getDMS_Content_ID();

		// For Tab viewer
		if (tableID > 0 && recordID > 0)
		{
			associationID = dms.createAssociation(	linkableContentID, contentRelatedID, dms.getSubstituteTableInfo().getOriginRecord_ID(),
													dms.getSubstituteTableInfo().getOriginTable_ID(), MDMSAssociationType.LINK_ID, null);
			MDMSAssociation association = new MDMSAssociation(Env.getCtx(), associationID, null);
			contentID = association.getDMS_Content_Related_ID();
			if (contentID <= 0)
				contentID = association.getDMS_Content_ID();
			else
			{
				MDMSContent dmsContent = new MDMSContent(Env.getCtx(), contentID, null);

				if (dmsContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
					contentID = association.getDMS_Content_ID();
				else
					contentID = association.getDMS_Content_Related_ID();
			}
		}
		else
		{
			contentID = linkableContentID;
			associationID = dms.createAssociation(linkableContentID, contentRelatedID, 0, 0, MDMSAssociationType.LINK_ID, null);
		}

		return null;
	} // createLink

	/**
	 * Check Clipboard/Copied Document already exists in same position for Copy to CreateLink
	 * operation
	 * 
	 * @param  currContent   - Current DMS Content
	 * @param  copiedContent - Copied Content
	 * @param  isDir         - is Directory
	 * @return               True if Document exists in same level
	 */
	public static boolean isDocumentPresent(MDMSContent currContent, MDMSContent copiedContent, boolean isDir)
	{
		String sql = "	SELECT COUNT(DMS_Content_ID) FROM DMS_Association WHERE DMS_Content_ID=? AND DMS_Content_Related_ID "
						+ (((currContent == null || currContent.getDMS_Content_ID() <= 0) && !isDir) ? "IS NULL" : " = " + currContent.getDMS_Content_ID());

		return DB.getSQLValue(null, sql.toString(), copiedContent.getDMS_Content_ID()) > 0 ? true : false;
	} // isDocumentPresent

	/**
	 * Delete Content [ Do In-Active ]
	 * 
	 * @param dms
	 * @param dmsContent
	 * @param dmsAssociation
	 * @param isDeleteLinkableRefs
	 * @param trxName
	 */
	public static void deleteContent(DMS dms, MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs, String trxName)
	{
		if (MDMSAssociationType.isLink(dmsAssociation))
		{
			setContentAndAssociation(null, dmsAssociation, false, trxName);
			return;
		}
		else if (dmsContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Content))
		{
			HashMap<I_DMS_Content, I_DMS_Association> relatedContentList = null;
			if (dmsAssociation.getDMS_AssociationType_ID() == MDMSAssociationType.RECORD_ID)
			{
				MDMSContent parentContent = new MDMSContent(Env.getCtx(), dmsAssociation.getDMS_Content_Related_ID(), null);
				MDMSAssociation parentAssociation = dms.getParentAssociationFromContent(parentContent.getDMS_Content_ID());
				setContentAndAssociation(parentContent, parentAssociation, false, trxName);
				relatedContentList = DMSOprUtils.getRelatedContents(parentContent, true, trxName);
			}
			else
			{
				setContentAndAssociation(dmsContent, dmsAssociation, false, trxName);
				relatedContentList = DMSOprUtils.getRelatedContents(dmsContent, true, trxName);
			}

			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : relatedContentList.entrySet())
			{
				MDMSContent content = (MDMSContent) entry.getKey();
				MDMSAssociation association = (MDMSAssociation) entry.getValue();
				setContentAndAssociation(content, association, false, trxName);
			}
		}
		else if (dmsContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Directory))
		{
			// Inactive Directory content and its child recursively
			setContentAndAssociation(dmsContent, dmsAssociation, false, trxName);
			HashMap<I_DMS_Content, I_DMS_Association> relatedContentList = DMSOprUtils.getRelatedContents(dmsContent, true, trxName);
			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : relatedContentList.entrySet())
			{
				MDMSContent content = (MDMSContent) entry.getKey();
				MDMSAssociation association = (MDMSAssociation) entry.getValue();
				// recursive call
				DMSOprUtils.deleteContent(dms, content, association, isDeleteLinkableRefs, trxName);
			}
		}

		// Linkable association references to set as InActive
		if (isDeleteLinkableRefs)
		{
			HashMap<I_DMS_Association, I_DMS_Content> linkRefs = dms.getLinkableAssociationWithContentRelated(dmsContent);
			for (Entry<I_DMS_Association, I_DMS_Content> linkRef : linkRefs.entrySet())
			{
				MDMSAssociation linkAssociation = (MDMSAssociation) linkRef.getKey();
				linkAssociation.setIsActive(false);
				linkAssociation.save();
			}
		}
	} // deleteContent

	/**
	 * Undo Delete Content [ Do Active ]
	 * 
	 * @param dms
	 * @param dmsContent
	 * @param dmsAssociation
	 * @param isUnDeleteLinkableRefs
	 * @param trxName
	 */
	public static void undoDeleteContent(DMS dms, MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isUnDeleteLinkableRefs, String trxName)
	{
		if (MDMSAssociationType.isLink(dmsAssociation))
		{
			setContentAndAssociation(null, dmsAssociation, true, trxName);
			return;
		}
		else if (dmsContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Content))
		{
			HashMap<I_DMS_Content, I_DMS_Association> relatedContentList = null;
			if (dmsAssociation.getDMS_AssociationType_ID() == MDMSAssociationType.RECORD_ID)
			{
				MDMSContent parentContent = new MDMSContent(Env.getCtx(), dmsAssociation.getDMS_Content_Related_ID(), null);
				MDMSAssociation parentAssociation = dms.getParentAssociationFromContent(parentContent.getDMS_Content_ID());
				setContentAndAssociation(parentContent, parentAssociation, true, trxName);
				relatedContentList = DMSOprUtils.getRelatedContents(parentContent, false, trxName);
			}
			else
			{
				setContentAndAssociation(dmsContent, dmsAssociation, true, trxName);
				relatedContentList = DMSOprUtils.getRelatedContents(dmsContent, false, trxName);
			}

			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : relatedContentList.entrySet())
			{
				MDMSContent content = (MDMSContent) entry.getKey();
				MDMSAssociation association = (MDMSAssociation) entry.getValue();
				setContentAndAssociation(content, association, true, trxName);
			}
		}
		else if (dmsContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Directory))
		{
			// Active Directory content and its child recursively
			setContentAndAssociation(dmsContent, dmsAssociation, true, trxName);
			HashMap<I_DMS_Content, I_DMS_Association> relatedContentList = DMSOprUtils.getRelatedContents(dmsContent, false, trxName);
			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : relatedContentList.entrySet())
			{
				MDMSContent content = (MDMSContent) entry.getKey();
				MDMSAssociation association = (MDMSAssociation) entry.getValue();
				// recursive call
				DMSOprUtils.undoDeleteContent(dms, content, association, isUnDeleteLinkableRefs, trxName);
			}
		}

		// Linkable association references to set as Active
		if (isUnDeleteLinkableRefs)
		{
			HashMap<I_DMS_Association, I_DMS_Content> linkRefs = dms.getLinkableAssociationWithContentRelated(dmsContent, false);
			for (Entry<I_DMS_Association, I_DMS_Content> linkRef : linkRefs.entrySet())
			{
				MDMSAssociation linkAssociation = (MDMSAssociation) linkRef.getKey();
				linkAssociation.setIsActive(true);
				linkAssociation.save();
			}
		}

		MDMSAssociation parentAssociation = dmsAssociation;
		while (parentAssociation != null && parentAssociation.getDMS_Content_Related_ID() > 0)
		{
			MDMSContent parentContent = new MDMSContent(Env.getCtx(), parentAssociation.getDMS_Content_Related_ID(), null);
			if (parentContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Directory) && !parentContent.isActive())
			{
				List<MDMSAssociation> associationlist = dms.getAssociationFromContent(parentContent.getDMS_Content_ID(), 0, false, trxName);
				if (!associationlist.isEmpty())
				{
					parentAssociation = (MDMSAssociation) associationlist.get(0);
					setContentAndAssociation(parentContent, parentAssociation, true, trxName);
				}
			}
			else
			{
				break;
			}
		}
	} // undoDeleteContent

	public static void setContentAndAssociation(MDMSContent content, MDMSAssociation association, boolean isActive, String trxName)
	{
		if (association != null)
		{
			association.setIsActive(isActive);
			association.saveEx(trxName);
		}

		if (content != null)
		{
			content.setIsActive(isActive);
			content.saveEx(trxName);
		}
	} // setContentAndAssociationInActive

	/**
	 * Delete Content With Physical Document
	 * 
	 * @param  dms
	 * @param  content
	 * @throws IOException
	 */
	public static void deleteContentWithPhysicalDocument(DMS dms, MDMSContent content) throws IOException
	{
		for (MDMSVersion version : content.getAllVersions())
		{
			File document = dms.getFileFromStorage(version);
			if (document.exists())
				document.delete();

			File thumbnails = new File(dms.getThumbnailURL(version, null));

			if (thumbnails.exists())
				FileUtils.deleteDirectory(thumbnails);
		}

		int no = DB.executeUpdate("DELETE FROM DMS_Association 	WHERE DMS_Content_ID = ?", content.getDMS_Content_ID(), null);
		DMS.log.log(Level.INFO, no + " association deleted.");

		no = DB.executeUpdate("DELETE FROM DMS_Version			WHERE DMS_Content_ID = ?", content.getDMS_Content_ID(), null);
		DMS.log.log(Level.INFO, no + " version deleted.");

		no = DB.executeUpdate("DELETE FROM DMS_Content 			WHERE DMS_Content_ID = ?", content.getDMS_Content_ID(), null);
		DMS.log.log(Level.INFO, no + " content deleted.");

		// TODO Need code for remove linkable docs
	} // deleteContentWithPhysicalDocument

	/**
	 * Get Linkable Docs any for given content and association
	 * 
	 * @param  dms
	 * @param  content
	 * @param  association
	 * @return             Linkable Docs info if exists
	 */
	public static String hasLinkableDocs(DMS dms, I_DMS_Content content, I_DMS_Association association)
	{
		String name = "";
		if (MDMSAssociationType.isLink(association))
			;
		else
		{
			int count = 0;
			HashMap<I_DMS_Association, I_DMS_Content> linkRefs = dms.getLinkableAssociationWithContentRelated(content, content.isActive());
			for (Entry<I_DMS_Association, I_DMS_Content> linkRef : linkRefs.entrySet())
			{
				String contentName = content.getName();
				String contentBaseType = content.getContentBaseType();
				String currentParentURL = content.getParentURL();
				String linkableContentURL = null;
				if (linkRef.getValue() != null)
					linkableContentURL = linkRef.getValue().getParentURL() + DMSConstant.FILE_SEPARATOR + linkRef.getValue().getName();

				if (count == 0)
				{
					name += "\n\n <b>" + contentBaseType + " : </b>" + contentName;
					name += "\n <b>Current Path : </b>" + currentParentURL;
				}
				name += "\n <b>Link ref : </b>" + (linkableContentURL == null ? "Root at Document Explorer" : linkableContentURL);

				count++;
			}

			if (content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
			{
				HashMap<I_DMS_Version, I_DMS_Association> childContents = dms.getDMSContentsWithAssociation((MDMSContent) content, dms.AD_Client_ID, true);
				for (Entry<I_DMS_Version, I_DMS_Association> children : childContents.entrySet())
				{
					name += hasLinkableDocs(dms, (MDMSContent) ((MDMSVersion) children.getKey()).getDMS_Content(), children.getValue());
				}
			}
		}
		return name;
	} // hasLinkableDocs

	/**
	 * Get the related contents of give content like versions, Linkable docs etc
	 * 
	 * @param  dmsContent
	 * @param  trxName
	 * @param  isActive
	 * @return            Map of related contents
	 */
	public static HashMap<I_DMS_Content, I_DMS_Association> getRelatedContents(MDMSContent dmsContent, boolean isActive, String trxName)
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_RELATED_CONTENT, trxName);
			pstmt.setInt(1, dmsContent.getDMS_Content_ID());
			pstmt.setString(2, isActive ? "Y" : "N");
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				map.put(new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), trxName),
						new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), trxName));
			}
		}
		catch (SQLException e)
		{
			DMS.log.log(Level.SEVERE, "getRelatedContents fetching failure: ", e);
			throw new AdempiereException("getRelatedContents fetching failure: " + e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return map;
	} // getRelatedContents

	/**
	 * Get Files from storage for all version of content
	 * 
	 * @param  dms     DMS
	 * @param  content - Content
	 * @return         Files
	 */
	public static File[] getFileFromStorageAllVersion(DMS dms, I_DMS_Content content)
	{
		ArrayList<File> files = new ArrayList<File>();
		for (MDMSVersion version : MDMSVersion.getVersionHistory(content))
		{
			File file = dms.getFileFromStorage(version);
			if (file != null)
				files.add(file);
		}
		return (File[]) files.toArray();
	} // getFileFromStorageAllVersion

}
