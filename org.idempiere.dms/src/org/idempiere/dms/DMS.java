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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MImage;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.idempiere.model.MDMSMimeType;
import org.idempiere.model.X_DMS_Content;
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

	private static final String		SQL_GET_ASSOCIATION_SEQ_NO	= "SELECT COALESCE(MAX(seqNo), 0) + 1  FROM DMS_Association WHERE DMS_Content_Related_ID = ? AND AD_Client_ID = ?";

	public static CLogger			log							= CLogger.getCLogger(DMS.class);

	public IFileStorageProvider		thumbnailStorageProvider	= null;
	private IFileStorageProvider	fileStorageProvider			= null;
	private IThumbnailProvider		thumbnailProvider			= null;
	private IMountingStrategy		mountingStrategy			= null;
	private IContentManager			contentManager				= null;
	private IIndexSearcher			indexSearcher				= null;

	public int						AD_Client_ID				= 0;

	/**
	 * Constructor for initialize provider
	 */
	public DMS(int AD_Client_ID)
	{
		this(AD_Client_ID, null);
	}

	/**
	 * Constructor for initialize provider
	 */
	public DMS(int AD_Client_ID, String Table_Name)
	{
		this.AD_Client_ID = AD_Client_ID;

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

		createMountingStrategy(Table_Name);
	}

	public void createMountingStrategy(String Table_Name)
	{
		mountingStrategy = Utils.getMountingStrategy(Table_Name);
	}

	// Get Provider
	public IFileStorageProvider getThumbnailStorageProvider()
	{
		return thumbnailStorageProvider;
	}

	public IThumbnailProvider getThumbnailProvider()
	{
		return thumbnailProvider;
	}

	public IFileStorageProvider getFileStorageProvider()
	{
		return fileStorageProvider;
	}

	public IMountingStrategy getMountingStrategy()
	{
		return mountingStrategy;
	}

	public IContentManager getContentManager()
	{
		return contentManager;
	}

	public IIndexSearcher getIndexSearcher()
	{
		return indexSearcher;
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

	public boolean addFile(MDMSContent content, File file, String name, String desc, int contentTypeID, int asiID, int AD_Table_ID, int Record_ID)
	{
		return addFile(content, file, name, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, false);
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
		MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(DMS_Content_ID, null);
		return addFileVersion(content, file, null);
	}

	public boolean addFileVersion(MDMSContent content, File file)
	{
		return addFileVersion(content, file, null);
	}

	public boolean addFileVersion(MDMSContent content, File file, String desc)
	{
		return addFileVersion(content, file, desc, 0, 0);
	}

	private boolean addFileVersion(MDMSContent content, File file, String desc, int AD_Table_ID, int Record_ID)
	{
		return addFile(content, file, null, desc, 0, 0, AD_Table_ID, Record_ID, true);
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
	private boolean addFile(String dirPath, File file, String fileName, String desc, String contentType, Map<String, String> attributeMap, int AD_Table_ID,
			int Record_ID, boolean isVersion)
	{
		int asiID = 0;
		int contentTypeID = 0;

		Trx trx = null;

		if (file == null)
			throw new AdempiereException("File not found.");

		MDMSContent dirContent = mountingStrategy.getMountingParent(AD_Table_ID, Record_ID);

		if (!isVersion)
		{
			if (Util.isEmpty(fileName, true))
				fileName = file.getName();

			Utils.isValidFileName(fileName, true);
		}
		else
		{
			if (Utils.getMimeTypeID(file) != dirContent.getDMS_MimeType_ID())
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
					AD_Client_ID);
			MDMSContentType cType = new MDMSContentType(Env.getCtx(), contentTypeID, trx.getTrxName());
			if (attributeMap != null && !attributeMap.isEmpty())
				asiID = Utils.createASI(attributeMap, cType.getM_AttributeSet_ID(), trx.getTrxName());
		}

		//
		addFile(dirContent, file, fileName, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, isVersion);

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
	private boolean addFile(MDMSContent parentContent, File file, String fileName, String desc, int contentTypeID, int asiID, int AD_Table_ID, int Record_ID,
			boolean isVersion)
	{
		if (file == null)
			throw new AdempiereException("File not found.");

		if (!isVersion)
		{
			if (Util.isEmpty(fileName, true))
				fileName = file.getName();
			Utils.isValidFileName(fileName, true);
		}
		else
		{
			if (Utils.getMimeTypeID(file) != parentContent.getDMS_MimeType_ID())
				throw new WrongValueException("Mime type not matched, please upload same mime type version document.");
		}

		String trxName = Trx.createTrxName("UploadFile");
		Trx trx = Trx.get(trxName, true);

		// Create Content, Association, Store File & Thumbnail generate
		try
		{
			createContentAssociationFileStoreAndThumnail(parentContent, file, fileName, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, isVersion, trx);

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

	public boolean createContentAssociationFileStoreAndThumnail(MDMSContent parentContent, File file, String fileName, String desc, int contentTypeID,
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
			DMS_Content_Related_ID = Utils.getDMS_Content_Related_ID(parentContent);
			DMS_AssociationType_ID = MDMSAssociationType.getVersionType(false);

			seqNo = DB.getSQLValue(trx.getTrxName(), SQL_GET_ASSOCIATION_SEQ_NO, Utils.getDMS_Content_Related_ID(parentContent), AD_Client_ID);
		}
		else
		{
			String format = FilenameUtils.getExtension(file.getName());
			if (format == null)
				throw new AdempiereException("Invalid File format");

			if (!fileName.endsWith(format))
				fileName = fileName + "." + format;

			parentURL = contentManager.getPath(parentContent);

			if (parentContent != null)
				DMS_Content_Related_ID = parentContent.getDMS_Content_ID();
			DMS_AssociationType_ID = MDMSAssociationType.getVersionType(true);
		}

		// Create Content
		int contentID = Utils.createDMSContent(fileName, fileName, MDMSContent.CONTENTBASETYPE_Content, parentURL, desc, file, contentTypeID, asiID, false,
				trx.getTrxName());

		MDMSContent addedContent = new MDMSContent(Env.getCtx(), contentID, trx.getTrxName());

		// For Association
		if (!isVersion)
		{
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
		Utils.writeFileOnStorageAndThumnail(this, file, addedContent);

		return true;
	} // createContentAssociationFileStoreAndThumnail

	/*
	 * Select Content
	 */
	public MDMSContent[] selectContent(String dirPath)
	{
		// TODO
		return null;
	} // selectContent

	public MDMSContent[] selectContent(String dirPath, int AD_Table_ID, int Record_ID)
	{
		// TODO
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

	public String getThumbnailURL(I_DMS_Content content, String size)
	{
		return thumbnailProvider.getURL(content, size);
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

	public String getPathFromContentManager(MDMSContent content)
	{
		return contentManager.getPath(content);
	}

	public File getFileFromStorage(MDMSContent content)
	{
		return fileStorageProvider.getFile(this.getPathFromContentManager(content));
	}

	/**
	 * Get File from Path
	 * 
	 * @param path
	 * @return {@link File}
	 */
	public File getFileFromStorage(String path)
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

	public List<Integer> searchIndex(String query)
	{
		return indexSearcher.searchIndex(query);
	}

	public String buildSolrSearchQuery(HashMap<String, List<Object>> params)
	{
		return indexSearcher.buildSolrSearchQuery(params);
	}

	public void createIndexContent(MDMSContent content, MDMSAssociation association)
	{
		indexSearcher.indexContent(Utils.createIndexMap(content, association));
	}

	public int createAssociation(int dms_Content_ID, int contentRelatedID, int Record_ID, int AD_Table_ID, int associationTypeID, int seqNo, String trxName)
	{
		return Utils.createAssociation(dms_Content_ID, contentRelatedID, Record_ID, AD_Table_ID, associationTypeID, seqNo, trxName);
	}

	/**
	 * Paste copied folder into physical storage
	 * 
	 * @param copiedContent - Copied Content
	 * @param destPasteContent - Paste Content Destination
	 * @return FileName
	 */
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
					if (!newFileName.contains(" - copy ")) // TODO
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

				if (!newFile.getName().contains(" - copy ")) // TODO
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

	public void pasteCopyFileContent(MDMSContent oldDMSContent, MDMSContent destPasteContent, int tableID, int recordID, boolean isTabViewer)
			throws SQLException, IOException
	{
		int crID = 0;
		String fileName = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		int DMS_Content_ID = Utils.getDMS_Content_Related_ID(new MDMSContent(Env.getCtx(), oldDMSContent.getDMS_Content_ID(), null));

		String sqlGetAssociation = "SELECT DMS_Association_ID,DMS_Content_ID FROM DMS_Association "
				+ " WHERE DMS_Content_Related_ID=? AND DMS_AssociationType_ID=1000000 OR DMS_Content_ID=? AND DMS_AssociationType_ID !=1000003"
				+ " Order By DMS_Association_ID";
		try
		{
			pstmt = DB.prepareStatement(sqlGetAssociation.toString(), null);
			pstmt.setInt(1, DMS_Content_ID);
			pstmt.setInt(2, DMS_Content_ID);
			rs = pstmt.executeQuery();

			String trxName = Trx.createTrxName("copy-paste");
			Trx trx = Trx.get(trxName, true);

			while (rs.next())
			{
				String baseURL = null;
				String renamedURL = null;
				oldDMSContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), trx.getTrxName());

				if (!Util.isEmpty(oldDMSContent.getParentURL()))
					baseURL = contentManager.getPath(oldDMSContent);
				else
					baseURL = DMSConstant.FILE_SEPARATOR + oldDMSContent.getName();

				baseURL = baseURL.substring(0, baseURL.lastIndexOf(DMSConstant.FILE_SEPARATOR));

				fileName = this.pastePhysicalCopiedContent(oldDMSContent, destPasteContent, fileName);
				renamedURL = contentManager.getPath(destPasteContent);

				MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, trx.getTrxName());

				PO.copyValues(oldDMSContent, newDMSContent);

				MAttributeSetInstance oldASI = null;
				MAttributeSetInstance newASI = null;
				if (oldDMSContent.getM_AttributeSetInstance_ID() > 0)
				{
					// Copy ASI
					oldASI = new MAttributeSetInstance(Env.getCtx(), oldDMSContent.getM_AttributeSetInstance_ID(), trx.getTrxName());
					newASI = new MAttributeSetInstance(Env.getCtx(), 0, trx.getTrxName());
					PO.copyValues(oldASI, newASI);
					newASI.saveEx();

					List<MAttributeInstance> oldAI = new Query(Env.getCtx(), MAttributeInstance.Table_Name, "M_AttributeSetInstance_ID = ?", null)
							.setParameters(oldASI.getM_AttributeSetInstance_ID()).list();

					for (MAttributeInstance AI : oldAI)
					{
						MAttributeInstance newAI = new MAttributeInstance(Env.getCtx(), 0, trx.getTrxName());
						PO.copyValues(AI, newAI);
						newAI.setM_Attribute_ID(AI.getM_Attribute_ID());
						newAI.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
						newAI.saveEx();
					}
				}
				if (newASI != null)
					newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

				newDMSContent.setName(fileName);
				newDMSContent.saveEx();

				// Copy Association
				MDMSAssociation oldDMSAssociation = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
				MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, trx.getTrxName());
				PO.copyValues(oldDMSAssociation, newDMSAssociation);
				newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());

				if (oldDMSAssociation.getDMS_AssociationType_ID() == 1000001)
				{
					crID = newDMSContent.getDMS_Content_ID();

					if (destPasteContent != null && destPasteContent.getDMS_Content_ID() > 0)
						newDMSAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());
					else
						newDMSAssociation.setDMS_Content_Related_ID(0);
				}
				else
					newDMSAssociation.setDMS_Content_Related_ID(crID);

				if (isTabViewer)
				{
					newDMSAssociation.setAD_Table_ID(tableID);
					newDMSAssociation.setRecord_ID(recordID);
				}

				newDMSAssociation.saveEx();

				if (!Util.isEmpty(oldDMSContent.getParentURL()))
				{
					if (oldDMSContent.getParentURL().startsWith(baseURL))
					{
						newDMSContent.setParentURL(renamedURL);
					}
				}
				else
				{
					newDMSContent.setParentURL(renamedURL);
				}

				newDMSContent.saveEx();
				trx.commit();

				MDMSMimeType mimeType = new MDMSMimeType(Env.getCtx(), newDMSContent.getDMS_MimeType_ID(), null);

				IThumbnailGenerator thumbnailGenerator = Utils.getThumbnailGenerator(this, mimeType.getMimeType());

				if (thumbnailGenerator != null)
					thumbnailGenerator.addThumbnail(newDMSContent, fileStorageProvider.getFile(contentManager.getPath(oldDMSContent)), null);

				String newPath = fileStorageProvider.getBaseDirectory(contentManager.getPath(destPasteContent));
				newPath = newPath + DMSConstant.FILE_SEPARATOR + oldDMSContent.getName();
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error while paste Copy File Content", e);
			throw new AdempiereException(e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	} // pasteCopyFileContent

	public void moveFile(MDMSContent dmsContent, MDMSContent destContent)
	{
		String newPath = this.getBaseDirPath(destContent);
		newPath = newPath + DMSConstant.FILE_SEPARATOR + dmsContent.getName();

		File oldFile = new File(this.getFileFromStorage(dmsContent).getAbsolutePath());
		File newFile = new File(newPath);

		if (!newFile.exists())
			oldFile.renameTo(newFile);
		else
			throw new AdempiereException("File is already exist.");
	} // moveFile

	public void copyContent(MDMSContent copiedContent, String baseURL, String renamedURL, MDMSContent destPasteContent, int tableID, int recordID,
			boolean isTabViewer) throws IOException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try
		{
			pstmt = DB.prepareStatement(Utils.SQL_GET_RELATED_FOLDER_CONTENT_ACTIVE, null);
			pstmt.setInt(1, AD_Client_ID);
			pstmt.setInt(2, copiedContent.getDMS_Content_ID());
			pstmt.setInt(3, copiedContent.getDMS_Content_ID());

			rs = pstmt.executeQuery();

			while (rs.next())
			{
				MDMSContent oldDMSContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
				if (oldDMSContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
				{
					MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, null);
					PO.copyValues(oldDMSContent, newDMSContent);

					MAttributeSetInstance oldASI = null;
					MAttributeSetInstance newASI = null;
					if (oldDMSContent.getM_AttributeSetInstance_ID() > 0)
					{
						oldASI = new MAttributeSetInstance(Env.getCtx(), oldDMSContent.getM_AttributeSetInstance_ID(), null);
						newASI = new MAttributeSetInstance(Env.getCtx(), 0, null);
						PO.copyValues(oldASI, newASI);
						newASI.saveEx();

						List<MAttributeInstance> oldAI = new Query(Env.getCtx(), MAttributeInstance.Table_Name, "M_AttributeSetInstance_ID = ?", null)
								.setParameters(oldASI.getM_AttributeSetInstance_ID()).list();

						for (MAttributeInstance AI : oldAI)
						{
							MAttributeInstance newAI = new MAttributeInstance(Env.getCtx(), 0, null);
							PO.copyValues(AI, newAI);
							newAI.setM_Attribute_ID(AI.getM_Attribute_ID());
							newAI.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
							newAI.saveEx();
						}
					}
					if (newASI != null)
						newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

					newDMSContent.saveEx();

					// Copy Association
					MDMSAssociation oldDMSAssociation = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
					MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, null);
					PO.copyValues(oldDMSAssociation, newDMSAssociation);
					newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
					newDMSAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());

					if (isTabViewer)
					{
						newDMSAssociation.setAD_Table_ID(tableID);
						newDMSAssociation.setRecord_ID(recordID);
					}
					newDMSAssociation.saveEx();

					if (!Util.isEmpty(oldDMSContent.getParentURL()))
					{
						if (oldDMSContent.getParentURL().startsWith(baseURL))
						{
							newDMSContent.setParentURL(this.getPathFromContentManager(destPasteContent));
							newDMSContent.saveEx();
						}
						copyContent(oldDMSContent, baseURL, renamedURL, newDMSContent, tableID, recordID, isTabViewer);
					}
				}
				else
				{
					this.pasteCopyFileContent(oldDMSContent, destPasteContent, tableID, recordID, isTabViewer);
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Content renaming failure: ", e);
			throw new AdempiereException("Content renaming failure: " + e.getLocalizedMessage());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	} // copyContent

	public void pasteCopyContent(MDMSContent copiedContent, MDMSContent destPasteContent, int tableID, int recordID, boolean isTabViewer) throws IOException,
			SQLException
	{
		if (copiedContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			int DMS_Association_ID = DB.getSQLValue(null, "SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ? ORDER BY CREATED",
					copiedContent.getDMS_Content_ID());

			String baseURL = null;
			String renamedURL = null;
			String contentname = null;

			if (!Util.isEmpty(copiedContent.getParentURL()))
				baseURL = getPathFromContentManager(copiedContent);
			else
				baseURL = DMSConstant.FILE_SEPARATOR + copiedContent.getName();

			if (copiedContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
			{
				contentname = pastePhysicalCopiedFolder(copiedContent, destPasteContent);
			}
			renamedURL = getPathFromContentManager(destPasteContent) + DMSConstant.FILE_SEPARATOR + copiedContent.getName();

			MDMSContent oldDMSContent = new MDMSContent(Env.getCtx(), copiedContent.getDMS_Content_ID(), null);

			MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, null);

			PO.copyValues(oldDMSContent, newDMSContent);

			MAttributeSetInstance oldASI = null;
			MAttributeSetInstance newASI = null;
			if (oldDMSContent.getM_AttributeSetInstance_ID() > 0)
			{
				oldASI = new MAttributeSetInstance(Env.getCtx(), oldDMSContent.getM_AttributeSetInstance_ID(), null);
				newASI = new MAttributeSetInstance(Env.getCtx(), 0, null);
				PO.copyValues(oldASI, newASI);
				newASI.saveEx();

				List<MAttributeInstance> oldAI = new Query(Env.getCtx(), MAttributeInstance.Table_Name, "M_AttributeSetInstance_ID = ?", null).setParameters(
						oldASI.getM_AttributeSetInstance_ID()).list();

				for (MAttributeInstance AI : oldAI)
				{
					MAttributeInstance newAI = new MAttributeInstance(Env.getCtx(), 0, null);
					PO.copyValues(AI, newAI);
					newAI.setM_Attribute_ID(AI.getM_Attribute_ID());
					newAI.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
					newAI.saveEx();
				}
			}
			if (newASI != null)
				newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

			newDMSContent.setParentURL(getPathFromContentManager(destPasteContent));
			newDMSContent.setName(contentname);
			newDMSContent.saveEx();

			MDMSAssociation oldDMSAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);
			MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, null);

			PO.copyValues(oldDMSAssociation, newDMSAssociation);

			newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
			if (destPasteContent != null && destPasteContent.getDMS_Content_ID() > 0)
			{
				newDMSAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());
			}
			else
			{
				newDMSAssociation.setDMS_Content_Related_ID(0);
			}

			if (isTabViewer)
			{
				newDMSAssociation.setAD_Table_ID(tableID);
				newDMSAssociation.setRecord_ID(recordID);
			}
			newDMSAssociation.saveEx();

			copyContent(copiedContent, baseURL, renamedURL, newDMSContent, tableID, recordID, isTabViewer);
		}
		else
		{
			pasteCopyFileContent(copiedContent, destPasteContent, tableID, recordID, isTabViewer);
		}
	} // pasteCopyContent

	public void pasteCutContent(MDMSContent sourceCutContent, MDMSContent destPasteContent)
	{
		if (sourceCutContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			int DMS_Association_ID = DB.getSQLValue(null, "SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ? Order by created ",
					sourceCutContent.getDMS_Content_ID());

			String baseURL = null;
			String renamedURL = null;

			if (!Util.isEmpty(sourceCutContent.getParentURL()))
				baseURL = this.getPathFromContentManager(sourceCutContent);
			else
				baseURL = DMSConstant.FILE_SEPARATOR + sourceCutContent.getName();

			File dirPath = new File(this.getBaseDirPath(sourceCutContent));
			String newFileName = this.getBaseDirPath(destPasteContent);

			File files[] = new File(newFileName).listFiles();

			if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
				newFileName = newFileName + sourceCutContent.getName();
			else
				newFileName = newFileName + DMSConstant.FILE_SEPARATOR + sourceCutContent.getName();

			File newFile = new File(newFileName);

			for (int i = 0; i < files.length; i++)
			{
				if (newFile.getName().equalsIgnoreCase(files[i].getName()))
				{
					throw new AdempiereException("Directory already exists.");
				}
			}

			renamedURL = this.getPathFromContentManager(destPasteContent) + DMSConstant.FILE_SEPARATOR + sourceCutContent.getName();

			Utils.renameFolder(sourceCutContent, baseURL, renamedURL);
			dirPath.renameTo(newFile);

			MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);
			if (destPasteContent != null)
			{
				dmsAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());
				sourceCutContent.setParentURL(this.getPathFromContentManager(destPasteContent));
			}
			else
			{
				dmsAssociation.setDMS_Content_Related_ID(0);
				sourceCutContent.setParentURL(null);
			}

			sourceCutContent.saveEx();
			dmsAssociation.saveEx();
		}
		else
		{
			int DMS_Content_ID = Utils.getDMS_Content_Related_ID(sourceCutContent);

			PreparedStatement pstmt = null;
			ResultSet rs = null;

			try
			{
				pstmt = DB
						.prepareStatement(
								"SELECT DMS_Association_ID,DMS_Content_ID FROM DMS_Association WHERE DMS_Content_Related_ID = ? AND DMS_AssociationType_ID = 1000000 OR DMS_Content_ID = ? Order By DMS_Association_ID",
								null);

				pstmt.setInt(1, DMS_Content_ID);
				pstmt.setInt(2, DMS_Content_ID);

				rs = pstmt.executeQuery();

				while (rs.next())
				{
					MDMSContent dmsContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
					MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);

					this.moveFile(dmsContent, destPasteContent);
					if (dmsAssociation.getDMS_AssociationType_ID() == 1000001)
					{
						if (destPasteContent != null && destPasteContent.getDMS_Content_ID() == 0)
						{
							destPasteContent = null;
						}

						if (destPasteContent == null || destPasteContent.getDMS_Content_ID() == 0)
						{
							dmsAssociation.setDMS_Content_Related_ID(0);
							dmsAssociation.saveEx();
						}
						else
						{
							dmsAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());
							dmsAssociation.saveEx();
						}
					}

					dmsContent.setParentURL(this.getPathFromContentManager(destPasteContent));
					dmsContent.saveEx();

				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, "Content move failure.", e);
				throw new AdempiereException("Content move failure." + e.getLocalizedMessage());
			}
		}
	}

	public void renameContent(String fileName, MDMSContent DMSContent, MDMSContent parent_Content)
	{
		if (DMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
		{
			if (!fileName.equalsIgnoreCase(parent_Content.getName()))
			{
				String baseURL = this.getPathFromContentManager(DMSContent);

				String newFileName = this.getBaseDirPath(DMSContent);
				File dirPath = new File(newFileName);
				newFileName = newFileName.substring(0, newFileName.lastIndexOf(DMSConstant.FILE_SEPARATOR));

				File files[] = new File(newFileName).listFiles();

				if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
					newFileName = newFileName + fileName;
				else
					newFileName = newFileName + DMSConstant.FILE_SEPARATOR + fileName;

				File newFile = new File(newFileName);

				for (int i = 0; i < files.length; i++)
				{
					if (newFile.getName().equalsIgnoreCase(files[i].getName()))
						throw new AdempiereException("Directory already exists.");
				}

				String renamedURL = null;
				if (!Util.isEmpty(DMSContent.getParentURL()))
					renamedURL = DMSContent.getParentURL() + DMSConstant.FILE_SEPARATOR + fileName;
				else
					renamedURL = DMSConstant.FILE_SEPARATOR + fileName;

				if (!dirPath.renameTo(newFile))
					throw new AdempiereException("Invalid File Name.");

				Utils.renameFolder(DMSContent, baseURL, renamedURL);
				DMSContent.setName(fileName);
				DMSContent.saveEx();
			}
		}
		else
		{
			if (!fileName.equalsIgnoreCase(parent_Content.getName().substring(0, parent_Content.getName().lastIndexOf("."))))
			{
				updateContent(fileName, DMSContent);
			}
		}
	}

	public void updateContent(String fileName, MDMSContent DMSContent)
	{
		int DMS_Content_ID = Utils.getDMS_Content_Related_ID(DMSContent);
		MDMSContent content = null;
		MDMSAssociation association = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(Utils.SQL_GET_RELATED_CONTENT, null);
			pstmt.setInt(1, DMS_Content_ID);
			pstmt.setInt(2, DMS_Content_ID);

			rs = pstmt.executeQuery();

			while (rs.next())
			{
				content = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
				association = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
				this.renameFile(content, association, fileName);
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Rename content failure.", e);
			throw new AdempiereException("Rename content failure: " + e.getLocalizedMessage());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}

	public void renameFile(MDMSContent content, MDMSAssociation association, String fileName)
	{
		String newPath = this.getFileFromStorage(content).getAbsolutePath();
		String fileExt = newPath.substring(newPath.lastIndexOf("."), newPath.length());
		newPath = newPath.substring(0, newPath.lastIndexOf(DMSConstant.FILE_SEPARATOR));
		newPath = newPath + DMSConstant.FILE_SEPARATOR + fileName + fileExt;
		newPath = Utils.getUniqueFilename(newPath);

		File oldFile = new File(this.getFileFromStorage(content).getAbsolutePath());
		File newFile = new File(newPath);
		oldFile.renameTo(newFile);

		content.setName(newFile.getAbsolutePath().substring(newFile.getAbsolutePath().lastIndexOf(DMSConstant.FILE_SEPARATOR) + 1,
				newFile.getAbsolutePath().length()));
		content.saveEx();
	}

	public void deleteContentWithDocument(MDMSContent content) throws IOException
	{
		File document = this.getFileFromStorage(content);
		if (document.exists())
			document.delete();

		File thumbnails = new File(this.getThumbnailURL(content, null));

		if (thumbnails.exists())
			FileUtils.deleteDirectory(thumbnails);

		DB.executeUpdate("DELETE FROM DMS_Association WHERE DMS_Content_ID = ?", content.getDMS_Content_ID(), null);
		DB.executeUpdate("DELETE FROM DMS_Content WHERE DMS_Content_ID = ?", content.getDMS_Content_ID(), null);
	}
}
