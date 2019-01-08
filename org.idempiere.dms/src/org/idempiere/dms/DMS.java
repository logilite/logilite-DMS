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

package org.idempiere.dms;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.compiere.model.MImage;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.WrongValueException;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

/**
 * DMS API SUPPORT
 * 
 * @author Sachin
 */
public class DMS
{

	private static final String	SQL_GET_ASSOCIATION_SEQ_NO	= "SELECT COALESCE(MAX(seqNo), 0) + 1  FROM DMS_Association WHERE DMS_Content_Related_ID = ? AND AD_Client_ID = ?";

	public static CLogger		log							= CLogger.getCLogger(DMS.class);

	public IFileStorageProvider	thumbnailStorageProvider	= null;
	public IFileStorageProvider	fileStorageProvider			= null;
	public IThumbnailProvider	thumbnailProvider			= null;
	public IMountingStrategy	mountingStrategy			= null;
	public IContentManager		contentManager				= null;
	public IIndexSearcher		indexSearcher				= null;

	/**
	 * Constructor for initialize provider
	 */
	public DMS(int AD_Client_ID, String Table_Name)
	{
		fileStorageProvider = FileStorageUtil.get(AD_Client_ID, false);

		if (fileStorageProvider == null)
			throw new AdempiereException("Storage provider is not found.");

		thumbnailStorageProvider = FileStorageUtil.get(AD_Client_ID, true);

		if (thumbnailStorageProvider == null)
			throw new AdempiereException("Thumbnail Storage provider is not found.");

		thumbnailProvider = Utils.getThumbnailProvider(AD_Client_ID);

		if (thumbnailProvider == null)
			throw new AdempiereException("Thumbnail provider is not found.");

		contentManager = Utils.getContentManager(AD_Client_ID);

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		indexSearcher = ServiceUtils.getIndexSearcher(AD_Client_ID);

		if (indexSearcher == null)
			throw new AdempiereException("Index server is not found.");

		mountingStrategy = Utils.getMountingStrategy(Table_Name);
	}

	/*
	 * Adding files and File Version
	 */

	public boolean addFile(File file)
	{
		return addFile(null, file);
	}

	public boolean addFile(String dirPath, File file)
	{
		return addFile(dirPath, file, file.getName());
	}

	public boolean addFile(String dirPath, File file, String fileName)
	{
		return addFile(dirPath, file, fileName, 0, 0);
	}

	public boolean addFile(String dirPath, File file, String fileName, int AD_Table_ID, int Record_ID)
	{
		return addFile(dirPath, file, fileName, null, null, AD_Table_ID, Record_ID);
	}

	public boolean addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap)
	{
		return addFile(dirPath, file, fileName, contentType, attributeMap, 0, 0);
	}

	public boolean addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap, int AD_Table_ID, int Record_ID)
	{
		return addFile(dirPath, file, fileName, null, contentType, attributeMap, AD_Table_ID, Record_ID, false);
	}

	// public boolean addFileVersion(String dirPath, File file)
	// {
	// return addFileVersion(dirPath, file, file.getName());
	// }
	//
	// public boolean addFileVersion(String dirPath, File file, String fileName)
	// {
	// return addFileVersion(dirPath, file, fileName, 0, 0);
	// }
	//
	// public boolean addFileVersion(String dirPath, File file, String fileName,
	// int AD_Table_ID, int Record_ID)
	// {
	// return addFile(dirPath, file, fileName, null, null, AD_Table_ID,
	// Record_ID, true);
	// }

	public boolean addFileVersion(int DMS_Content_ID, File file)
	{
		return false;
	}

	/**
	 * Create File, if directory not exists then create
	 * 
	 * @param dirPath
	 * @param file
	 * @param fileName
	 * @param desc
	 * @param contentType
	 * @param attributeMap
	 * @param AD_Table_ID
	 * @param Record_ID
	 * @param isVersion
	 * @return TRUE if success
	 */
	public boolean addFile(String dirPath, File file, String fileName, String desc, String contentType, Map<String, String> attributeMap, int AD_Table_ID,
			int Record_ID, boolean isVersion)
	{
		int asiID = 0;
		int contentTypeID = 0;

		Trx trx = null;
		AMedia media = null;

		if (file == null)
			throw new AdempiereException("File not found.");

		media = Utils.getMediaFromFile(file);

		MDMSContent dirContent = mountingStrategy.getMountingParent(AD_Table_ID, Record_ID);

		if (!isVersion)
		{
			if (Util.isEmpty(fileName, true))
				fileName = file.getName();

			Utils.isValidFileName(fileName, true);
		}
		else
		{
			if (Utils.getMimeTypeID(media) != dirContent.getDMS_MimeType_ID())
				throw new WrongValueException("Mime type not matched, please upload same mime type version document.");
		}

		String trxName = Trx.createTrxName("AddFiles");
		trx = Trx.get(trxName, true);

		// Create Directory folder hierarchy
		if (!Util.isEmpty(dirPath, true))
		{
			// TODO Need implement dir creation with recursive logic
			dirContent = Utils.createDirectory(dirPath, dirContent, AD_Table_ID, Record_ID, fileStorageProvider, contentManager, false, trx.getTrxName());
		}

		if (!isVersion && contentType != null)
		{
			contentTypeID = DB.getSQLValue(null, "SELECT DMS_ContentType_ID FROM DMS_ContentType WHERE Value = ? AND AD_Client_ID = ?", contentType,
					Env.getAD_Client_ID(Env.getCtx()));
			MDMSContentType cType = new MDMSContentType(Env.getCtx(), contentTypeID, trx.getTrxName());
			asiID = Utils.createASI(attributeMap, cType.getM_AttributeSet_ID(), trx.getTrxName());
		}

		//
		addFile(dirContent, media, fileName, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, isVersion);

		return true;
	}// addFile

	/**
	 * @param parentContent
	 * @param media
	 * @param fileName
	 * @param desc
	 * @param contentTypeID
	 * @param asiID
	 * @param AD_Table_ID
	 * @param Record_ID
	 * @param isVersion
	 * @return TRUE if success
	 */
	public boolean addFile(MDMSContent parentContent, AMedia media, String fileName, String desc, int contentTypeID, int asiID, int AD_Table_ID, int Record_ID,
			boolean isVersion)
	{

		if (media == null)
			throw new AdempiereException("Media not found.");

		if (!isVersion)
		{
			if (Util.isEmpty(fileName, true))
				fileName = media.getName();
			Utils.isValidFileName(fileName, true);
		}
		else
		{
			if (Utils.getMimeTypeID(media) != parentContent.getDMS_MimeType_ID())
				throw new WrongValueException("Mime type not matched, please upload same mime type version document.");
		}

		String trxName = Trx.createTrxName("UploadFiles");
		Trx trx = Trx.get(trxName, true);

		// Create Content, Association, Store File & Thumbnail generate
		try
		{
			createContentAssociationFileStoreAndThumnail(parentContent, media, fileName, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, isVersion, trx);

			trx.commit(); // Transaction commit
		}
		catch (Exception e)
		{
			if (trx != null)
				trx.rollback();
			throw new AdempiereException("Upload Content Failure :" + e);
		}
		finally
		{
			if (trx != null)
				trx.close();
		}
		return true;
	} // addFile

	public boolean createContentAssociationFileStoreAndThumnail(MDMSContent parentContent, AMedia media, String fileName, String desc, int contentTypeID,
			int asiID, int AD_Table_ID, int Record_ID, boolean isVersion, Trx trx)
	{
		int seqNo = 0;
		int DMS_Content_Related_ID = 0;
		int DMS_AssociationType_ID = 0;

		String parentURL;

		if (isVersion)
		{
			fileName = parentContent.getName();
			parentURL = parentContent.getParentURL();
			asiID = parentContent.getM_AttributeSetInstance_ID();
			contentTypeID = parentContent.getDMS_ContentType_ID();
		}
		else
		{
			String format = media.getFormat();
			if (format == null)
				throw new AdempiereException("Invalid File format");

			if (!fileName.endsWith(format))
				fileName = fileName + "." + format;

			parentURL = contentManager.getPath(parentContent);
		}

		// Create Content
		int contentID = Utils.createDMSContent(fileName, fileName, MDMSContent.CONTENTBASETYPE_Content, parentURL, desc, media, contentTypeID, asiID, false,
				trx.getTrxName());

		MDMSContent addedContent = new MDMSContent(Env.getCtx(), contentID, trx.getTrxName());

		// For Association
		if (isVersion)
		{
			// TODO Need to check
			DMS_Content_Related_ID = Utils.getDMS_Content_Related_ID(parentContent);
			DMS_AssociationType_ID = MDMSAssociationType.getVersionType(false);

			seqNo = DB.getSQLValue(null, SQL_GET_ASSOCIATION_SEQ_NO, Utils.getDMS_Content_Related_ID(parentContent), Env.getAD_Client_ID(Env.getCtx()));
		}
		else
		{
			if (parentContent != null)
				DMS_Content_Related_ID = parentContent.getDMS_Content_ID();
			DMS_AssociationType_ID = MDMSAssociationType.getVersionType(true);

			// Display an error when trying to upload same name file
			String path = fileStorageProvider.getBaseDirectory(contentManager.getPath(addedContent));
			File fileCheck = new File(path);
			if (fileCheck.exists())
			{
				throw new WrongValueException(
						"File already exists, either rename or upload as a version. \n (Either same file name content exist in inActive mode)");
			}
		}

		// Create Association
		Utils.createAssociation(contentID, DMS_Content_Related_ID, Record_ID, AD_Table_ID, DMS_AssociationType_ID, seqNo, trx.getTrxName());

		// File write on Storage provider and create thumbnail
		Utils.writeFileOnStorageAndThumnail(fileStorageProvider, contentManager, media, addedContent);

		return true;
	} // createContentAssociationFileStoreAndThumnail

	/*
	 * Select Content
	 */

	public MDMSContent[] selectContent(String dirPath)
	{
		return null;
	} // selectContent

	public MDMSContent[] selectContent(String dirPath, int AD_Table_ID, int Record_ID)
	{
		return null;
	}

	public MDMSContent getRootContent(int AD_Table_ID, int Record_ID)
	{
		return mountingStrategy.getMountingParent(MTable.getTableName(Env.getCtx(), AD_Table_ID), Record_ID);
	}

	public MDMSContent createDirectory(String dirName, MDMSContent mDMSContent, int tableID, int recordID, boolean errorIfDirExists, String trxName)
	{
		return Utils.createDirectory(dirName, mDMSContent, tableID, recordID, fileStorageProvider, contentManager, errorIfDirExists, trxName);
	}

	public File getThumbnail(I_DMS_Content content, String size)
	{
		return thumbnailProvider.getFile(content, size);
	}

	public MImage getDirThumbnail()
	{
		return Utils.getDirThumbnail();
	}

	public MImage getMimetypeThumbnail(int dms_MimeType_ID)
	{
		return Utils.getMimetypeThumbnail(dms_MimeType_ID);
	}

	public List<Integer> searchIndex(String query)
	{
		return indexSearcher.searchIndex(query);
	}

	public File getFileFromStorage(MDMSContent content)
	{
		return fileStorageProvider.getFile(contentManager.getPath(content));
	}

	/**
	 * Get File from Path
	 * 
	 * @param path
	 * @return {@link File}
	 */
	public File getFile(String path)
	{
		return fileStorageProvider.getFile(path);
	} // getFile

	/**
	 * Get base directory Path of given content
	 * 
	 * @param content
	 * @return path
	 */
	public String getBaseDirPath(MDMSContent content)
	{
		return fileStorageProvider.getBaseDirectory(contentManager.getPath(content));
	} // getBaseDirPath

	public String pastePhysicalCopiedFolder(MDMSContent copiedContent, MDMSContent destPasteContent)
	{
		File dirPath = new File(this.getBaseDirPath(copiedContent));
		String newFileName = this.getBaseDirPath(destPasteContent);

		File files[] = new File(newFileName).listFiles();

		if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
			newFileName += copiedContent.getName();
		else
			newFileName += DMSConstant.FILE_SEPARATOR + copiedContent.getName();

		File newFile = new File(newFileName);

		if (files.length > 0)
		{
			for (int i = 0; i < files.length; i++)
			{
				if (newFile.getName().equalsIgnoreCase(files[i].getName()))
				{
					if (!newFileName.contains(" - copy "))
						newFileName = newFileName + " - copy ";

					newFile = new File(newFileName);

					if (newFile.exists())
					{
						newFileName = Utils.getUniqueFoldername(newFile.getAbsolutePath());
						newFile = new File(newFileName);
					}
				}
			}
		}

		try
		{
			FileUtils.copyDirectory(dirPath, newFile, DirectoryFileFilter.DIRECTORY);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Copy Content Failure.", e);
			throw new AdempiereException("Copy Content Failure." + e.getLocalizedMessage());
		}

		return newFile.getName();
	} // pastePhysicalCopiedFolder

	public String pastePhysicalCopiedContent(MDMSContent copiedContent, MDMSContent destPasteContent, String fileName)
	{
		File oldFile = new File(this.getBaseDirPath(copiedContent));
		String newFileName = this.getBaseDirPath(destPasteContent);

		if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
			newFileName += copiedContent.getName();
		else
			newFileName += DMSConstant.FILE_SEPARATOR + copiedContent.getName();

		File newFile = new File(newFileName);
		File parent = new File(newFile.getParent());

		File files[] = parent.listFiles();

		for (int i = 0; i < files.length; i++)
		{
			if (newFile.getName().equals(files[i].getName()))
			{
				String uniqueName = newFile.getName();

				if (!newFile.getName().contains(" - copy "))
				{
					uniqueName = FilenameUtils.getBaseName(newFile.getName()) + " - copy ";
					String ext = FilenameUtils.getExtension(newFile.getName());
					newFile = new File(parent.getAbsolutePath() + DMSConstant.FILE_SEPARATOR + uniqueName + "." + ext);
				}

				if (newFile.exists())
				{
					uniqueName = Utils.getCopiedUniqueFilename(newFile.getAbsolutePath());
					newFile = new File(uniqueName);
				}
			}
		}

		try
		{
			FileUtils.copyFile(oldFile, newFile);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Copy Content Failure.", e);
			throw new AdempiereException("Copy Content Failure." + e.getLocalizedMessage());
		}

		return newFile.getName();
	} // pastePhysicalCopiedContent

}
