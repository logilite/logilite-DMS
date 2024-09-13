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

package com.logilite.dms.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FilenameUtils;
import org.compiere.Adempiere;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import com.logilite.dms.DMS;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.exception.DMSContentExistException;
import com.logilite.dms.exception.DMSException;
import com.logilite.dms.factories.IContentManager;
import com.logilite.dms.factories.IThumbnailGenerator;
import com.logilite.dms.model.IFileStorageProvider;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSAssociationType;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSContentType;
import com.logilite.dms.model.MDMSPermission;
import com.logilite.dms.model.MDMSVersion;
import com.logilite.dms.util.DMSFactoryUtils;
import com.logilite.dms.util.DMSPermissionUtils;
import com.logilite.dms.util.RelationUtils;
import com.logilite.dms.util.Utils;
import com.logilite.search.exception.IndexException;

/**
 * @author Deepak Pansheria
 * @author Sachin Bhimani
 */
public class RelationalContentManager implements IContentManager
{
	// Relational - Content Manager Type
	public static final String	KEY	= "REL";

	public static CLogger		log	= CLogger.getCLogger(RelationalContentManager.class);

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getPathByValue(I_DMS_Content content)
	{
		return getPathByValue(MDMSVersion.getLatestVersion(content, false));
	} // getPathByValue

	@Override
	public String getPathByValue(I_DMS_Version version)
	{
		String path = "";

		if (version != null && version.getDMS_Content_ID() > 0)
		{
			if (!Util.isEmpty(version.getDMS_Content().getParentURL(), true))
				path = version.getDMS_Content().getParentURL() + DMSConstant.FILE_SEPARATOR + version.getValue();
			else if (!Util.isEmpty(version.getDMS_Content().getName(), true))
				path = DMSConstant.FILE_SEPARATOR + version.getValue();
		}
		return path;
	} // getPathByValue

	@Override
	public String getPathByName(I_DMS_Content content)
	{
		String path = "";

		if (content != null && content.getDMS_Content_ID() > 0)
		{
			if (!Util.isEmpty(content.getParentURL(), true))
				path = content.getParentURL() + DMSConstant.FILE_SEPARATOR + content.getName();
			else if (!Util.isEmpty(content.getName(), true))
				path = DMSConstant.FILE_SEPARATOR + content.getName();
		}
		return path;
	} // getPathByName

	@Override
	public String getContentName(	IFileStorageProvider storageProvider, String contentType, I_DMS_Content content, String fileName, String extention,
									String type, String operationType)
	{
		String contentName = RelationUtils.getActualContentName(this, storageProvider, contentType, content, fileName, extention, type, operationType);
		return contentName;
	} // getContentName

	@Override
	public int createContent(	String name, String contentBaseType, String parentURL, String desc, File file, int contentTypeID, int asiID, boolean isMounting,
								String trxName)
	{
		return MDMSContent.create(name, contentBaseType, parentURL, desc, file, contentTypeID, asiID, isMounting, trxName);
	}

	@Override
	public int createAssociation(int dms_Content_ID, int contentRelatedID, int Record_ID, int AD_Table_ID, int associationTypeID, String trxName)
	{
		return MDMSAssociation.create(dms_Content_ID, contentRelatedID, Record_ID, AD_Table_ID, associationTypeID, trxName);
	}

	@Override
	public MDMSVersion createVersion(int contentID, String value, int seqNo, File file, String trxName)
	{
		return MDMSVersion.create(contentID, value, seqNo, file, trxName);
	} // createVersion

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
	@Override
	public MDMSContent createDirectory(	DMS dms, String dirContentName, MDMSContent parentContent, int AD_Table_ID, int Record_ID,
										boolean errorIfDirExists, boolean isCreateAssociation, String trxName)
	{
		return RelationUtils.createDirectory(dms, dirContentName, parentContent, AD_Table_ID, Record_ID, errorIfDirExists, isCreateAssociation, trxName);
	} // createDirectory

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
	@Override
	public MDMSContent createDirHierarchy(DMS dms, String dirPath, MDMSContent dirContent, int AD_Table_ID, int Record_ID, String trxName)
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
	@Override
	public int addFile(	DMS dms, String dirPath, File file, String fileName, String desc, String contentType, Map<String, String> attributeMap,
						int AD_Table_ID, int Record_ID, boolean isVersion, String trxName)
	{
		int asiID = 0;
		int contentTypeID = 0;
		MDMSContent dirContent = dms.getMountingStrategy().getMountingParent(AD_Table_ID, Record_ID, trxName);

		fileName = Utils.validateFileName(dirContent, file, fileName, isVersion);

		boolean isNewTrx = Util.isEmpty(trxName, true);
		if (isNewTrx)
			trxName = Trx.createTrxName("DMSAdd_");
		Trx trx = Trx.get(trxName, isNewTrx);

		try
		{
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

			if (isNewTrx)
			trx.commit(true);
		}
		catch (SQLException e)
		{
			trx.rollback();
			throw new AdempiereException("Error while committing transaction:" + e.getLocalizedMessage(), e);
		}
		finally
		{
			if (isNewTrx && trx != null)
				trx.close();
		}
		//
		return addFile(dms, dirContent, file, fileName, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, isVersion, (isNewTrx ? null : trxName));

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
	@Override
	public int addFile(	DMS dms, MDMSContent parentContent, File file, String fileName, String desc, int contentTypeID, int asiID, int AD_Table_ID,
						int Record_ID, boolean isVersion, String trxName)
	{
		fileName = Utils.validateFileName(parentContent, file, fileName, isVersion);

		boolean isNewTrx = Util.isEmpty(trxName, true);
		if (isNewTrx)
			trxName = Trx.createTrxName("DMSUpload_");
		Trx trx = Trx.get(trxName, isNewTrx);

		// Create Content, Association, Store File & Thumbnail generate
		int contentID = 0;
		try
		{
			contentID = createContentAssociationFileStoreAndThumnail(	dms, parentContent, file, fileName, desc, contentTypeID, asiID, AD_Table_ID, Record_ID,
																		isVersion, trx.getTrxName());
			if (isNewTrx)
			trx.commit();
		}
		catch (IndexException | DMSContentExistException e)
		{
			DMSException dmsExc = new DMSException();
			dmsExc.setException(e);
			if (e instanceof IndexException)
				dmsExc.setContentID(contentID);
			throw dmsExc;
		}
		catch (Exception e)
		{
			if (isNewTrx)
			trx.rollback();
			throw new AdempiereException("Upload Content Failure:\n" + e.getLocalizedMessage(), e);
		}
		finally
		{
			if (isNewTrx && trx != null)
				trx.close();
		}
		return contentID;
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
	@Override
	public int createContentAssociationFileStoreAndThumnail(DMS dms, MDMSContent parentContent, File file, String fileName, String desc,
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
			String format = Utils.getFileExtension(file.getName());
			if (format == null)
				format = Utils.getFileExtension(fileName);
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
			dms_content_id = checkExistsFileDir(parentURL, fileName, true);
		}

		if (dms_content_id > 0)
		{
			throw new DMSContentExistException(	"File already exists, either rename or upload as a version."
												+ "\n (Either same file name content exist in inActive mode)"
												+ "\n Content Path = " + parentURL
												+ "\n Content Name = " + fileName);
		}
		else
		{
			if (isVersion)
			{
				// Create DMS Version record
				MDMSVersion version = MDMSVersion.create(parentContent.getDMS_Content_ID(), actualFileName, seqNo, file, trxName);

				// File write on Storage provider and create thumbnail
				writeFileOnStorageAndThumnail(dms, file, version);
			}
			else
			{
				// Create Content
				int contentID = MDMSContent.create(	fileName, MDMSContent.CONTENTBASETYPE_Content, parentURL, desc, file, contentTypeID, asiID, false,
													trxName);
				// Create Association
				dms.createAssociation(contentID, DMS_Content_Related_ID, Record_ID, AD_Table_ID, DMS_AssociationType_ID, trxName);
				// Create DMS Version record
				MDMSVersion version = MDMSVersion.create(contentID, actualFileName, seqNo, file, trxName);

				// File write on Storage provider and create thumbnail
				writeFileOnStorageAndThumnail(dms, file, version);

				parentContent = (MDMSContent) version.getDMS_Content();
			}

			return parentContent.get_ID();
		}
	} // createContentAssociationFileStoreAndThumnail

	/**
	 * File write on Storage provider and create thumbnail
	 * 
	 * @param dms
	 * @param file
	 * @param version
	 */
	@Override
	public void writeFileOnStorageAndThumnail(DMS dms, File file, MDMSVersion version)
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

		// This flag is false then thumbnail will not be created otherwise it will be created
		if (dms.isAllowThumbnailContentCreation())
		{
			IThumbnailGenerator thumbnailGenerator = DMSFactoryUtils.getThumbnailGenerator(dms, version.getDMS_Content().getDMS_MimeType().getMimeType());
			if (thumbnailGenerator != null)
				thumbnailGenerator.addThumbnail(version, file, null);
		}
	} // writeFileOnStorageAndThumnail

	/*
	 * 
	 */

	/**
	 * Paste the content [ Copy Operation ]
	 * 
	 * @param dms                               - DMS
	 * @param copiedContent                     - Content From
	 * @param destContent                       - Content To
	 * @param tableID                           - AD_Table_ID
	 * @param recordID                          - Record_ID
	 * @param isCreatePermissionForPasteContent - create permission for paste content from parent if
	 *                                          true
	 */
	@Override
	public void pasteCopyContent(	DMS dms, MDMSContent copiedContent, MDMSContent destContent, int tableID, int recordID,
									boolean isCreatePermissionForPasteContent)
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
			String newName = RelationUtils.getCopyDirContentName(destContent, copiedContent.getName(), dms.getContentManager().getPathByName(destContent));
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
			// set owner from the created in copy paste
			newDMSContent.setDMS_Owner_ID(newDMSContent.getCreatedBy());
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
			String contentname = RelationUtils.pastePhysicalCopiedFolder(dms, copiedContent, destContent, newActualName);

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

			RelationUtils.pasteCopyDirContent(dms, copiedContent, newDMSContent, baseURL, renamedURL, tableID, recordID, isCreatePermissionForPasteContent);

			if (isCreatePermissionForPasteContent)
			{
				dms.grantChildPermissionFromParentContent(newDMSContent, destContent, isCreatePermissionForPasteContent);
			}

		}
		else
		{
			RelationUtils.pasteCopyFileContent(dms, copiedContent, destContent, tableID, recordID, isCreatePermissionForPasteContent);
		}

	} // pasteCopyContent

	/**
	 * Paste the content [ Cut operation ]
	 * 
	 * @param dms         - DMS
	 * @param cutContent  - Content From
	 * @param destContent - Content To
	 * @param tableID     - AD_Table_ID
	 * @param recordID    - Record_ID
	 */
	@Override
	public void pasteCutContent(DMS dms, MDMSContent cutContent, MDMSContent destContent, int tableID, int recordID, boolean isDocExplorerWindow)
	{
		// check if exists..
		int DMS_Content_ID = checkExistsFileDir(dms.getContentManager().getPathByName(destContent), cutContent.getName());
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

			RelationUtils.renameFolder(cutContent, baseURL, renamedURL, tableID, recordID, isDocExplorerWindow);
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
			int contentExists = checkExistsFileDir(dms.getPathFromContentManager(destContent), dmsContent.getName());
			if (contentExists > 0)
				throw new AdempiereException("File is already exist.");
			else
			{
				List<MDMSVersion> versionList = MDMSVersion.getVersionHistory(dmsContent);
				for (MDMSVersion version : versionList)
				{
					RelationUtils.moveFile(dms, version, destContent);
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

		/***
		 * Create navigation permission for owner of cut content
		 */
		MDMSPermission[] arrayCutContentPermission = (MDMSPermission[]) MDMSPermission.getAllPermissionForContent((MDMSContent) cutContent);
		for (MDMSPermission cutContentPermission : arrayCutContentPermission)
		{

			int permissionID = DMSPermissionUtils.getPermissionIDByUserRole(destContent.getDMS_Content_ID(), cutContentPermission.getAD_Role_ID(),
																			cutContentPermission.getAD_User_ID(), "");
			MDMSPermission newPermission = (MDMSPermission) MTable	.get(((PO) destContent).getCtx(), MDMSPermission.Table_ID)
																	.getPO(permissionID, ((PO) destContent).get_TrxName());
			if (permissionID <= 0 && !destContent.isMounting())
			{
				newPermission.setIsNavigation(MDMSContent.CONTENTBASETYPE_Directory.equals(destContent.getContentBaseType()));
				newPermission.setDMS_Content_ID(destContent.getDMS_Content_ID());
				newPermission.setAD_User_ID(cutContentPermission.getAD_User_ID());
				newPermission.setAD_Role_ID(cutContentPermission.getAD_Role_ID());
				newPermission.setIsAllPermission(false);
				newPermission.saveEx();
			}
			else
			{
				// Set navigation of destcontent true if permission is already created for the cut
				// content owner
				// but navigation flag is not true in the destcontent
				newPermission.setIsNavigation(MDMSContent.CONTENTBASETYPE_Directory.equals(destContent.getContentBaseType()) && !(newPermission.isRead()
																																	&& newPermission.isWrite()));
				newPermission.saveEx();
			}
		}

		dms.grantChildPermissionFromParentContent(cutContent, destContent);
	} // pasteCutContent

	/**
	 * Rename Content
	 * 
	 * @param dms      - DMS
	 * @param fileName - New FileName
	 * @param content  - Content
	 */
	@Override
	public void renameContent(DMS dms, String fileName, MDMSContent content)
	{
		int DMS_Content_ID = checkExistsFileDir(content.getParentURL(), fileName);
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

	@Override
	public void renameContentOnly(DMS dms, MDMSContent content, String fileName, String description, boolean isDocExplorerWindow)
	{
		fileName = fileName.trim();

		int DMS_Content_ID = checkExistsFileDir(content.getParentURL(), fileName);
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
	 * Rename File
	 * 
	 * @param dms
	 * @param content
	 * @param fileName
	 * @param isAddFileExtention
	 */
	@Override
	public void renameFile(DMS dms, MDMSContent content, String fileName, boolean isAddFileExtention)
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
	 * Delete Content [ Do In-Active ]
	 * 
	 * @param dms
	 * @param dmsContent
	 * @param dmsAssociation
	 * @param isDeleteLinkableRefs
	 * @param trxName
	 */
	@Override
	public void deleteContent(DMS dms, MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs)
	{
		String trxName = Trx.createTrxName("DMSDelete_");
		Trx trx = Trx.get(trxName, true);

		RelationUtils.deleteContent(dms, dmsContent, dmsAssociation, isDeleteLinkableRefs, trx.getTrxName());

		try
		{
			trx.commit(true);
		}
		catch (SQLException e)
		{
			trx.rollback();
			throw new AdempiereException("Error while committing transaction for delete content:" + e.getLocalizedMessage(), e);
		}
		finally
		{
			if (trx != null)
				trx.close();
		}
	} // deleteContent

	/**
	 * This will be a undo soft deletion. System will only active the files.
	 * 
	 * @param dmsContent
	 * @param dmsAssociation
	 * @param isDeleteLinkableRefs - Is Delete References of Links to another place
	 */
	@Override
	public void undoDeleteContent(DMS dms, MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs)
	{
		String trxName = Trx.createTrxName("DMSUndoDel_");
		Trx trx = Trx.get(trxName, true);

		RelationUtils.undoDeleteContent(dms, dmsContent, dmsAssociation, isDeleteLinkableRefs, trx.getTrxName());

		try
		{
			trx.commit(true);
		}
		catch (SQLException e)
		{
			trx.rollback();
			throw new AdempiereException("Error while committing transaction for undo delete content:" + e.getLocalizedMessage(), e);
		}
		finally
		{
			if (trx != null)
				trx.close();
		}
	} // undoDeleteContent

	/**
	 * Check copy/cut content exists in same Hierarchy.
	 * 
	 * @param  destContentID
	 * @param  sourceContentID
	 * @return                 true if copy/cut content exists in same Hierarchy.
	 */
	@Override
	public boolean isHierarchyContentExists(int destContentID, int sourceContentID, String trxName)
	{
		int contentID = DB.getSQLValue(	trxName, DMSConstant.SQL_CHECK_HIERARCHY_CONTENT_RECURSIVELY, Env.getAD_Client_ID(Env.getCtx()), destContentID,
										sourceContentID);
		return contentID > 0;
	} // isHierarchyContentExists

	public static int checkExistsDir(String ParentURL, String dirName)
	{
		return RelationUtils.checkDMSContentExists(ParentURL, dirName, false, false);
	}

	public static int checkExistsFileDir(String parentURL, String contentName)
	{
		return checkExistsFileDir(parentURL, contentName, false);
	}

	public static int checkExistsFileDir(String parentURL, String contentName, boolean isActiveOnly)
	{
		return RelationUtils.checkDMSContentExists(parentURL, contentName, isActiveOnly, true);
	}

	@Override
	public int checkContentExists(String parentURL, String contentName, boolean isActiveOnly, boolean isCheckByContentName)
	{
		return RelationUtils.checkDMSContentExists(parentURL, contentName, isActiveOnly, isCheckByContentName);
	}

}
