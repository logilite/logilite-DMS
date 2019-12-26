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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MImage;
import org.compiere.model.MTable;
import org.compiere.model.PO;
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
import org.idempiere.dms.pdfpreview.ConvertXlsToPdf;
import org.idempiere.dms.pdfpreview.ConvertXlsxToPdf;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.idempiere.model.MDMSMimeType;
import org.w3c.tidy.Tidy;

import com.itextpdf.text.Rectangle;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;

/**
 * DMS API SUPPORT
 * 
 * @author Sachin
 */
public class DMS
{

	private static final String		SQL_GET_ASSOCIATION_SEQ_NO	= "SELECT COALESCE(MAX(seqNo), 0) + 1  FROM DMS_Association WHERE DMS_Content_Related_ID = ? AND AD_Client_ID = ?";

	public static CLogger			log							= CLogger.getCLogger(DMS.class);

	private IFileStorageProvider	thumbnailStorageProvider	= null;
	private IFileStorageProvider	fileStorageProvider			= null;
	private IThumbnailProvider		thumbnailProvider			= null;
	private IMountingStrategy		mountingStrategy			= null;
	private IContentManager			contentManager				= null;
	private IIndexSearcher			indexSearcher				= null;

	private boolean					isDocExplorerWindow			= false;

	public int						AD_Client_ID				= 0;

	/**
	 * Constructor for initialize provider
	 */
	public DMS(int AD_Client_ID)
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
	} // Constructor

	/**
	 * Constructor for initialize provider
	 */
	public DMS(int AD_Client_ID, String Table_Name)
	{
		this(AD_Client_ID);
		initMountingStrategy(Table_Name);
	} // Constructor

	public void initMountingStrategy(String Table_Name)
	{
		mountingStrategy = Utils.getMountingStrategy(Table_Name);
	}

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

	public boolean isDocExplorerWindow()
	{
		return isDocExplorerWindow;
	}

	public void setDocExplorerWindow(boolean isDocExplorerWindow)
	{
		this.isDocExplorerWindow = isDocExplorerWindow;
	}

	/*
	 * Util Methods
	 */
	public MDMSContent getRootContent(int AD_Table_ID, int Record_ID)
	{
		return getMountingStrategy().getMountingParent(MTable.getTableName(Env.getCtx(), AD_Table_ID), Record_ID);
	}

	public MDMSContent createDirectory(String dirName, MDMSContent parentContent, int tableID, int recordID, boolean errorIfDirExists, String trxName)
	{
		return createDirectory(dirName, parentContent, tableID, recordID, errorIfDirExists, true, trxName);
	}

	public MDMSContent createDirectory(String dirName, MDMSContent parentContent, int tableID, int recordID, boolean errorIfDirExists,
			boolean isCreateAssociation, String trxName)
	{
		return Utils.createDirectory(dirName, parentContent, tableID, recordID, fileStorageProvider, contentManager, errorIfDirExists, isCreateAssociation,
				trxName);
	}

	public String getThumbnailURL(I_DMS_Content content, String size)
	{
		return thumbnailProvider.getURL(content, size);
	}

	public File getThumbnailFile(I_DMS_Content content, String size)
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

	public String getPathFromContentManager(I_DMS_Content content)
	{
		return contentManager.getPath(content);
	}

	public File getFileFromStorage(I_DMS_Content content)
	{
		return fileStorageProvider.getFile(this.getPathFromContentManager(content));
	}

	public File getFileFromStorage(String path)
	{
		return fileStorageProvider.getFile(path);
	}

	public String getBaseDirPath(MDMSContent content)
	{
		return fileStorageProvider.getBaseDirectory(contentManager.getPath(content));
	}

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

	public void initiateMountingContent(String tableName, int recordID, int tableID)
	{
		this.initiateMountingContent(Utils.getDMSMountingBase(AD_Client_ID), tableName, recordID, tableID);
	}

	public void initiateMountingContent(String mountingBaseName, String tableName, int recordID, int tableID)
	{
		Utils.initiateMountingContent(mountingBaseName, tableName, recordID, tableID);
	}

	public int createDMSContent(String name, String value, String contentBaseType, String parentURL, String desc, File file, int contentTypeID, int asiID,
			boolean isMounting, String trxName)
	{
		return Utils.createDMSContent(name, value, contentBaseType, parentURL, desc, file, contentTypeID, asiID, isMounting, trxName);
	}

	public int createAssociation(int dms_Content_ID, int contentRelatedID, int Record_ID, int AD_Table_ID, int associationTypeID, int seqNo, String trxName)
	{
		return Utils.createAssociation(dms_Content_ID, contentRelatedID, Record_ID, AD_Table_ID, associationTypeID, seqNo, trxName);
	}

	public MDMSAssociation getAssociationFromContent(int contentID)
	{
		return Utils.getAssociationFromContent(contentID, null);
	}

	public MDMSAssociation getAssociationFromContent(int contentID, boolean isLinkAssociationOnly)
	{
		return Utils.getAssociationFromContent(contentID, isLinkAssociationOnly, null);
	}

	public HashMap<I_DMS_Content, I_DMS_Association> getDMSContentsWithAssociation(MDMSContent content, int AD_Client_ID, boolean isActiveOnly)
	{
		return Utils.getDMSContentsWithAssociation(content, AD_Client_ID, isActiveOnly);
	}

	public HashMap<I_DMS_Association, I_DMS_Content> getLinkableAssociationWithContentRelated(I_DMS_Content content)
	{
		return Utils.getLinkableAssociationWithContentRelated(content);
	}

	/*
	 * Adding files and File Version
	 */

	public int addFile(File file)
	{
		return addFile(null, file);
	}

	public int addFile(String dirPath, File file)
	{
		return addFile(dirPath, file, file.getName());
	}

	public int addFile(String dirPath, File file, String fileName)
	{
		return addFile(dirPath, file, fileName, 0, 0);
	}

	public int addFile(String dirPath, File file, String fileName, int AD_Table_ID, int Record_ID)
	{
		return addFile(dirPath, file, fileName, null, null, AD_Table_ID, Record_ID);
	}

	public int addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap)
	{
		return addFile(dirPath, file, fileName, contentType, attributeMap, 0, 0);
	}

	public int addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap, int AD_Table_ID, int Record_ID)
	{
		return addFile(dirPath, file, fileName, null, contentType, attributeMap, AD_Table_ID, Record_ID, false);
	}

	public int addFile(MDMSContent parentContent, File file, int AD_Table_ID, int Record_ID)
	{
		return addFile(parentContent, file, file.getName(), null, 0, 0, AD_Table_ID, Record_ID);
	}

	public int addFile(MDMSContent parentContent, File file, String name, String desc, int contentTypeID, int asiID, int AD_Table_ID, int Record_ID)
	{
		return addFile(parentContent, file, name, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, false);
	}

	/*
	 * Add Version File
	 */
	public int addFileVersion(int DMS_Content_ID, File file)
	{
		MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(DMS_Content_ID, null);
		return addFileVersion(content, file, null);
	}

	public int addFileVersion(MDMSContent parentContent, File file)
	{
		return addFileVersion(parentContent, file, null);
	}

	public int addFileVersion(MDMSContent parentContent, File file, String desc)
	{
		return addFileVersion(parentContent, file, desc, 0, 0);
	}

	public int addFileVersion(MDMSContent parentContent, File file, String desc, int AD_Table_ID, int Record_ID)
	{
		return addFile(parentContent, file, null, desc, 0, 0, AD_Table_ID, Record_ID, true);
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
	 * @return New contentID
	 */
	private int addFile(String dirPath, File file, String fileName, String desc, String contentType, Map<String, String> attributeMap, int AD_Table_ID,
			int Record_ID, boolean isVersion)
	{
		int asiID = 0;
		int contentTypeID = 0;
		MDMSContent dirContent = getMountingStrategy().getMountingParent(AD_Table_ID, Record_ID);

		fileName = Utils.validateFileName(dirContent, file, fileName, isVersion);

		String trxName = Trx.createTrxName("AddFiles");
		Trx trx = Trx.get(trxName, true);

		// Create Directory folder hierarchy OR get leaf DMS-Content
		if (!Util.isEmpty(dirPath, true) && !dirPath.equals(DMSConstant.FILE_SEPARATOR))
		{
			dirContent = this.createDirHierarchy(dirPath, dirContent, AD_Table_ID, Record_ID, trx.getTrxName());
		}

		if (!isVersion && contentType != null)
		{
			contentTypeID = Utils.getContentTypeID(contentType, AD_Client_ID);

			MDMSContentType cType = new MDMSContentType(Env.getCtx(), contentTypeID, trx.getTrxName());
			if (attributeMap != null && !attributeMap.isEmpty())
				asiID = Utils.createASI(attributeMap, cType.getM_AttributeSet_ID(), trx.getTrxName());
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
		return addFile(dirContent, file, fileName, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, isVersion);

	}// addFile

	/**
	 * @param parentContent
	 * @param file
	 * @param fileName
	 * @param desc
	 * @param contentTypeID
	 * @param asiID
	 * @param AD_Table_ID
	 * @param Record_ID
	 * @param isVersion
	 * @return New ContentID
	 */
	private int addFile(MDMSContent parentContent, File file, String fileName, String desc, int contentTypeID, int asiID, int AD_Table_ID, int Record_ID,
			boolean isVersion)
	{
		boolean isError = false;
		fileName = Utils.validateFileName(parentContent, file, fileName, isVersion);

		String trxName = Trx.createTrxName("UploadFile");
		Trx trx = Trx.get(trxName, true);

		// Create Content, Association, Store File & Thumbnail generate
		try
		{
			return createContentAssociationFileStoreAndThumnail(parentContent, file, fileName, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, isVersion,
					trx.getTrxName());
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
	 * Create Directory Hierarchy or Get leaf DMSContent from dirPath
	 * 
	 * @param dirPath
	 * @param dirContent
	 * @param AD_Table_ID
	 * @param Record_ID
	 * @param trxName
	 * @return {@link MDMSContent} - Leaf node
	 */
	public MDMSContent createDirHierarchy(String dirPath, MDMSContent dirContent, int AD_Table_ID, int Record_ID, String trxName)
	{
		// For Ref '\\' = '\', "\\" = "\\\\"
		String[] dirs = dirPath.split(DMSConstant.FILE_SEPARATOR);

		for (int i = 0; i < dirs.length; i++)
		{
			String dir = dirs[i].trim();
			if (!Util.isEmpty(dir, true))
			{
				dirContent = this.createDirectory(dir, dirContent, AD_Table_ID, Record_ID, false, trxName);
			}
		}
		return dirContent;
	} // createDirHierarchy

	/**
	 * Create DMS Content, Association, File store in StorageProvider and
	 * Thumbnail generate
	 * 
	 * @param parentContent
	 * @param file
	 * @param fileName
	 * @param desc
	 * @param contentTypeID
	 * @param asiID
	 * @param AD_Table_ID
	 * @param Record_ID
	 * @param isVersion
	 * @param trxName
	 * @return New ContentID
	 */
	public int createContentAssociationFileStoreAndThumnail(MDMSContent parentContent, File file, String fileName, String desc, int contentTypeID, int asiID,
			int AD_Table_ID, int Record_ID, boolean isVersion, String trxName)
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

			seqNo = DB.getSQLValue(trxName, SQL_GET_ASSOCIATION_SEQ_NO, Utils.getDMS_Content_Related_ID(parentContent), AD_Client_ID);
		}
		else
		{
			String format = Utils.getFileExtension(fileName);
			if (format == null)
				format = Utils.getFileExtension(file.getName());
			if (format == null)
				throw new AdempiereException("Did not found file extension: " + fileName + " " + file.getName());

			if (!fileName.endsWith(format))
				fileName = fileName + format;

			parentURL = contentManager.getPath(parentContent);

			if (parentContent != null)
				DMS_Content_Related_ID = parentContent.getDMS_Content_ID();
			DMS_AssociationType_ID = MDMSAssociationType.getVersionType(true);
		}

		// Create Content
		int contentID = Utils.createDMSContent(fileName, fileName, MDMSContent.CONTENTBASETYPE_Content, parentURL, desc, file, contentTypeID, asiID, false,
				trxName);

		MDMSContent addedContent = new MDMSContent(Env.getCtx(), contentID, trxName);

		// For Association
		if (!isVersion)
		{
			// Display an error when trying to upload same name file
			String path = fileStorageProvider.getBaseDirectory(contentManager.getPath(addedContent));
			File fileCheck = new File(path);
			if (fileCheck.exists())
			{
				throw new AdempiereException(
						"File already exists, either rename or upload as a version. \n (Either same file name content exist in inActive mode)");
			}
		}

		// Create Association
		int associationID = createAssociation(contentID, DMS_Content_Related_ID, Record_ID, AD_Table_ID, DMS_AssociationType_ID, seqNo, trxName);

		// File write on Storage provider and create thumbnail
		Utils.writeFileOnStorageAndThumnail(this, file, addedContent);

		//
		if (isVersion)
		{
			updateLinkRefOfVersionContent(addedContent, associationID, trxName);
		}

		return contentID;
	} // createContentAssociationFileStoreAndThumnail

	private void updateLinkRefOfVersionContent(MDMSContent addedContent, int associationID, String trxName)
	{
		MDMSAssociation association = new MDMSAssociation(Env.getCtx(), associationID, trxName);
		int[] linkAssociationIDs = DB.getIDsEx(trxName, DMSConstant.SQL_LINK_ASSOCIATIONS_FROM_RELATED_TO_CONTENT, MDMSAssociationType.VERSION_ID,
				association.getDMS_Content_Related_ID(), association.getDMS_Content_Related_ID(), MDMSAssociationType.VERSION_ID);
		for (int linkAssociationID : linkAssociationIDs)
		{
			MDMSAssociation associationLink = new MDMSAssociation(Env.getCtx(), linkAssociationID, trxName);
			associationLink.setDMS_Content_ID(association.getDMS_Content_ID());
			associationLink.save();

			// For re-sync index of linkable docs
			addedContent.setSyncIndexForLinkableDocs(true);
		}
	} // updateLinkRefOfVersionContent

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
	} // selectContent

	/**
	 * Paste copied folder into physical storage
	 * 
	 * @param copiedContent - Copied Content
	 * @param destPasteContent - Paste Content Destination
	 * @return FileName
	 */
	public String pastePhysicalCopiedFolder(MDMSContent copiedContent, MDMSContent destPasteContent)
	{
		// File dirPath = new File(this.getBaseDirPath(copiedContent));
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
					if (!newFileName.contains(" - copy")) // TODO
						newFileName = newFileName + " - copy";

					newFile = new File(newFileName);

					if (newFile.exists())
					{
						newFileName = Utils.getUniqueFolderName(newFile.getAbsolutePath());
						newFile = new File(newFileName);
						break;
					}
				}
			}
		}

		// TODO Stopped to Copy whole folder directory
		if (!newFile.mkdir())
			throw new AdempiereException("Something went wrong!\nDirectory is not created:\n'" + newFileName + "'");

		return newFile.getName();
	} // pastePhysicalCopiedFolder

	/**
	 * Paste Physical Copied content
	 *
	 * @param copiedContent - Copied Content
	 * @param destPasteContent - Destination Content
	 * @param fileName - FileName
	 * @return FileName
	 */
	public String pastePhysicalCopiedContent(MDMSContent copiedContent, MDMSContent destPasteContent, String fileName)
	{// TODO ERROR
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

				if (!newFile.getName().contains(" - copy")) // TODO
				{
					uniqueName = FilenameUtils.getBaseName(newFile.getName()) + " - copy";
					String ext = Utils.getFileExtension(newFile.getName());
					newFile = new File(parent.getAbsolutePath() + DMSConstant.FILE_SEPARATOR + uniqueName + ext);
				}

				if (newFile.exists())
				{
					uniqueName = Utils.getCopiedUniqueFileName(newFile.getAbsolutePath());
					newFile = new File(uniqueName);
					break;
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

	/**
	 * Paste copy file content
	 * 
	 * @param copiedContent - Copied File Content
	 * @param destContent - Destination Content
	 * @param tableID - AD_Table_ID
	 * @param recordID - Record_ID
	 */
	public void pasteCopyFileContent(MDMSContent copiedContent, MDMSContent destContent, int tableID, int recordID)
	{
		int crID = 0;
		String fileName = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		int DMS_Content_ID = Utils.getDMS_Content_Related_ID(new MDMSContent(Env.getCtx(), copiedContent.getDMS_Content_ID(), null));

		String sqlGetAssociation = "SELECT DMS_Association_ID, DMS_Content_ID FROM DMS_Association "
				+ " WHERE DMS_Content_Related_ID = ? AND DMS_AssociationType_ID = ? OR DMS_Content_ID = ? AND DMS_AssociationType_ID != ?"
				+ " ORDER BY DMS_Association_ID";
		try
		{
			pstmt = DB.prepareStatement(sqlGetAssociation, null);
			pstmt.setInt(1, DMS_Content_ID);
			pstmt.setInt(2, MDMSAssociationType.VERSION_ID);
			pstmt.setInt(3, DMS_Content_ID);
			pstmt.setInt(4, MDMSAssociationType.LINK_ID);
			rs = pstmt.executeQuery();

			String trxName = Trx.createTrxName("copy-paste");
			Trx trx = Trx.get(trxName, true);

			while (rs.next())
			{
				String baseURL = null;
				String renamedURL = null;
				copiedContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), trx.getTrxName());

				if (!Util.isEmpty(copiedContent.getParentURL()))
					baseURL = contentManager.getPath(copiedContent);
				else
					baseURL = DMSConstant.FILE_SEPARATOR + copiedContent.getName();

				baseURL = baseURL.substring(0, baseURL.lastIndexOf(DMSConstant.FILE_SEPARATOR));

				fileName = this.pastePhysicalCopiedContent(copiedContent, destContent, fileName);
				renamedURL = contentManager.getPath(destContent);

				MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, trx.getTrxName());
				PO.copyValues(copiedContent, newDMSContent);

				MAttributeSetInstance newASI = Utils.copyASI(copiedContent.getM_AttributeSetInstance_ID(), trx.getTrxName());
				if (newASI != null)
					newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

				newDMSContent.setName(fileName);
				newDMSContent.saveEx();

				// Copy Association
				MDMSAssociation oldDMSAssociation = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
				MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, trx.getTrxName());
				PO.copyValues(oldDMSAssociation, newDMSAssociation);
				newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());

				if (oldDMSAssociation.getDMS_AssociationType_ID() == MDMSAssociationType.PARENT_ID)
				{
					crID = newDMSContent.getDMS_Content_ID();

					if (destContent != null && destContent.getDMS_Content_ID() > 0)
						newDMSAssociation.setDMS_Content_Related_ID(destContent.getDMS_Content_ID());
					else
						newDMSAssociation.setDMS_Content_Related_ID(0);
				}
				else
					newDMSAssociation.setDMS_Content_Related_ID(crID);

				Utils.updateTableRecordRef(newDMSAssociation, tableID, recordID);

				if (!Util.isEmpty(copiedContent.getParentURL()))
				{
					if (copiedContent.getParentURL().startsWith(baseURL))
						newDMSContent.setParentURL(renamedURL);
				}
				else
				{
					newDMSContent.setParentURL(renamedURL);
				}

				newDMSContent.saveEx();
				trx.commit();

				IThumbnailGenerator thumbnailGenerator = Utils.getThumbnailGenerator(this, newDMSContent.getDMS_MimeType().getMimeType());

				if (thumbnailGenerator != null)
					thumbnailGenerator.addThumbnail(newDMSContent, fileStorageProvider.getFile(contentManager.getPath(copiedContent)), null);
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

	/**
	 * Move File
	 * 
	 * @param contentFrom
	 * @param destContent
	 */
	public void moveFile(MDMSContent contentFrom, MDMSContent destContent)
	{
		String newPath = this.getBaseDirPath(destContent);
		newPath = newPath + DMSConstant.FILE_SEPARATOR + contentFrom.getName();

		File oldFile = new File(this.getFileFromStorage(contentFrom).getAbsolutePath());
		File newFile = new File(newPath);

		if (!newFile.exists())
			oldFile.renameTo(newFile);
		else
			throw new AdempiereException("File is already exist.");
	} // moveFile

	/**
	 * Paste the Copy Directory Content
	 * 
	 * @param copiedContent - Content From
	 * @param destPasteContent - Content To
	 * @param baseURL - Base URL
	 * @param renamedURL - Renamed URL
	 * @param tableID - AD_Table_ID
	 * @param recordID - Record_ID
	 */
	public void pasteCopyDirContent(MDMSContent copiedContent, MDMSContent destPasteContent, String baseURL, String renamedURL, int tableID, int recordID)
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = this.getDMSContentsWithAssociation(copiedContent, AD_Client_ID, true);
		for (Entry<I_DMS_Content, I_DMS_Association> mapEntry : map.entrySet())
		{
			MDMSContent oldDMSContent = (MDMSContent) mapEntry.getKey();
			MDMSAssociation oldDMSAssociation = (MDMSAssociation) mapEntry.getValue();
			if (oldDMSContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
			{
				MDMSContent newDMSContent = createDirectory(oldDMSContent.getName(), destPasteContent, tableID, recordID, true, false, null);

				MAttributeSetInstance newASI = Utils.copyASI(copiedContent.getM_AttributeSetInstance_ID(), null);
				if (newASI != null)
					newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

				// Copy Association
				MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, null);
				PO.copyValues(oldDMSAssociation, newDMSAssociation);
				newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
				newDMSAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());

				Utils.updateTableRecordRef(newDMSAssociation, tableID, recordID);

				// Note: Must save association first other wise creating
				// issue of wrong info in solr indexing entry
				newDMSContent.saveEx();

				if (!Util.isEmpty(oldDMSContent.getParentURL()))
				{
					if (oldDMSContent.getParentURL().startsWith(baseURL))
					{
						newDMSContent.setParentURL(this.getPathFromContentManager(destPasteContent));
						newDMSContent.saveEx();
					}
					this.pasteCopyDirContent(oldDMSContent, newDMSContent, baseURL, renamedURL, tableID, recordID);
				}
			}
			else if (Utils.isLink(oldDMSAssociation))
			{
				int associationID = createAssociation(oldDMSAssociation.getDMS_Content_ID(), destPasteContent.getDMS_Content_ID(), recordID, tableID,
						MDMSAssociationType.LINK_ID, 0, null);

				createIndexforLinkableContent(oldDMSAssociation.getDMS_Content().getContentBaseType(), oldDMSAssociation.getDMS_Content_ID(), associationID);
			}
			else
			{
				this.pasteCopyFileContent(oldDMSContent, destPasteContent, tableID, recordID);
			}
		}
	} // pasteCopyDirContent

	/**
	 * Paste the content [ Copy Operation ]
	 * 
	 * @param copiedContent - Content From
	 * @param destContent - Content To
	 * @param tableID - AD_Table_ID
	 * @param recordID - Record_ID
	 * @throws IOException
	 * @throws SQLException
	 */
	public void pasteCopyContent(MDMSContent copiedContent, MDMSContent destContent, int tableID, int recordID) throws IOException, SQLException
	{
		if (destContent != null && destContent.getDMS_Content_ID() > 0)
		{
			MDMSAssociation destAssociation = this.getAssociationFromContent(destContent.getDMS_Content_ID());
			tableID = destAssociation.getAD_Table_ID();
			recordID = destAssociation.getRecord_ID();
		}

		if (copiedContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			String baseURL = null;
			String renamedURL = null;
			String contentname = null;

			if (!Util.isEmpty(copiedContent.getParentURL()))
				baseURL = getPathFromContentManager(copiedContent);
			else
				baseURL = DMSConstant.FILE_SEPARATOR + copiedContent.getName();

			contentname = pastePhysicalCopiedFolder(copiedContent, destContent);
			renamedURL = getPathFromContentManager(destContent) + DMSConstant.FILE_SEPARATOR + copiedContent.getName();

			MDMSContent oldDMSContent = new MDMSContent(Env.getCtx(), copiedContent.getDMS_Content_ID(), null);
			MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, null);

			PO.copyValues(oldDMSContent, newDMSContent);

			MAttributeSetInstance newASI = Utils.copyASI(oldDMSContent.getM_AttributeSetInstance_ID(), null);
			if (newASI != null)
				newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

			newDMSContent.setParentURL(getPathFromContentManager(destContent));
			newDMSContent.saveEx();

			MDMSAssociation oldDMSAssociation = this.getAssociationFromContent(copiedContent.getDMS_Content_ID());
			MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, null);

			PO.copyValues(oldDMSAssociation, newDMSAssociation);

			newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
			if (destContent != null && destContent.getDMS_Content_ID() > 0)
				newDMSAssociation.setDMS_Content_Related_ID(destContent.getDMS_Content_ID());
			else
				newDMSAssociation.setDMS_Content_Related_ID(0);

			Utils.updateTableRecordRef(newDMSAssociation, tableID, recordID);

			// Note: Must save association first other wise creating
			// issue of wrong info in solr indexing entry
			newDMSContent.setName(contentname);
			newDMSContent.saveEx();

			pasteCopyDirContent(copiedContent, newDMSContent, baseURL, renamedURL, tableID, recordID);
		}
		else
		{
			pasteCopyFileContent(copiedContent, destContent, tableID, recordID);
		}
	} // pasteCopyContent

	/**
	 * Paste the content [ Cut operation ]
	 * 
	 * @param cutContent - Content From
	 * @param destContent - Content To
	 * @param tableID - AD_Table_ID
	 * @param recordID - Record_ID
	 */
	public void pasteCutContent(MDMSContent cutContent, MDMSContent destContent, int tableID, int recordID)
	{
		if (destContent != null && destContent.getDMS_Content_ID() > 0)
		{
			MDMSAssociation destAssociation = this.getAssociationFromContent(destContent.getDMS_Content_ID());
			tableID = destAssociation.getAD_Table_ID();
			recordID = destAssociation.getRecord_ID();
		}

		if (cutContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			String baseURL = null;
			String renamedURL = null;

			if (!Util.isEmpty(cutContent.getParentURL()))
				baseURL = this.getPathFromContentManager(cutContent);
			else
				baseURL = DMSConstant.FILE_SEPARATOR + cutContent.getName();

			File dirPath = new File(this.getBaseDirPath(cutContent));
			String newFileName = this.getBaseDirPath(destContent);

			File files[] = new File(newFileName).listFiles();

			if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
				newFileName = newFileName + cutContent.getName();
			else
				newFileName = newFileName + DMSConstant.FILE_SEPARATOR + cutContent.getName();

			File newFile = new File(newFileName);

			for (int i = 0; i < files.length; i++)
			{
				if (newFile.getName().equalsIgnoreCase(files[i].getName()))
					throw new AdempiereException("Directory already exists.");
			}

			renamedURL = this.getPathFromContentManager(destContent) + DMSConstant.FILE_SEPARATOR + cutContent.getName();

			Utils.renameFolder(cutContent, baseURL, renamedURL, tableID, recordID, isDocExplorerWindow);
			dirPath.renameTo(newFile);

			MDMSAssociation association = this.getAssociationFromContent(cutContent.getDMS_Content_ID());
			if (destContent != null && destContent.getDMS_Content_ID() > 0)
			{
				association.setDMS_Content_Related_ID(destContent.getDMS_Content_ID());
				cutContent.setParentURL(this.getPathFromContentManager(destContent));
			}
			else
			{
				association.setDMS_Content_Related_ID(0);
				cutContent.setParentURL(null);
			}

			Utils.updateTableRecordRef(association, tableID, recordID);

			// Note: Must save association first other wise creating
			// issue of wrong info in solr indexing entry
			cutContent.saveEx();
		}
		else
		{
			int DMS_Content_ID = Utils.getDMS_Content_Related_ID(cutContent);

			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				// TODO Need to Test Cut paste option for Association Type is
				// newly created

				// Moving Versioning Content and its association
				pstmt = DB.prepareStatement("SELECT DMS_Association_ID, DMS_Content_ID FROM DMS_Association "
						+ "WHERE DMS_Content_Related_ID = ? AND DMS_AssociationType_ID = 1000000 OR DMS_Content_ID = ? ORDER BY DMS_Association_ID", null);

				pstmt.setInt(1, DMS_Content_ID);
				pstmt.setInt(2, DMS_Content_ID);
				rs = pstmt.executeQuery();
				while (rs.next())
				{
					MDMSContent dmsContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
					MDMSAssociation association = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);

					this.moveFile(dmsContent, destContent);

					if (association.getDMS_AssociationType_ID() == MDMSAssociationType.PARENT_ID)
					{
						if (destContent != null && destContent.getDMS_Content_ID() == 0)
							destContent = null;

						if (destContent == null)
							association.setDMS_Content_Related_ID(0);
						else
							association.setDMS_Content_Related_ID(destContent.getDMS_Content_ID());
					}
					Utils.updateTableRecordRef(association, tableID, recordID);

					// Note: Must save association first other wise creating
					// issue of wrong info in solr indexing entry
					dmsContent.setParentURL(this.getPathFromContentManager(destContent));
					dmsContent.saveEx();
				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, "Content move failure.", e);
				throw new AdempiereException("Content move failure." + e.getLocalizedMessage());
			}
		}
	} // pasteCutContent

	/**
	 * Rename Content File/Directory
	 * 
	 * @param fileName
	 * @param content
	 * @param parent_Content
	 * @param tableID
	 * @param recordID
	 */
	public void renameContent(String fileName, MDMSContent content, MDMSContent parent_Content, int tableID, int recordID)
	{
		fileName = fileName.trim();

		if (content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			if (!fileName.equalsIgnoreCase(parent_Content.getName()))
			{
				String baseURL = this.getPathFromContentManager(content);

				String newFileName = this.getBaseDirPath(content);
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
				if (!Util.isEmpty(content.getParentURL()))
					renamedURL = content.getParentURL() + DMSConstant.FILE_SEPARATOR + fileName;
				else
					renamedURL = DMSConstant.FILE_SEPARATOR + fileName;

				if (!dirPath.renameTo(newFile))
					throw new AdempiereException("Invalid File Name.");

				Utils.renameFolder(content, baseURL, renamedURL, tableID, recordID, isDocExplorerWindow);
				content.setName(fileName);
				content.saveEx();
			}
		}
		else
		{
			if (!fileName.equalsIgnoreCase(parent_Content.getName().substring(0, parent_Content.getName().lastIndexOf("."))))
			{
				updateContent(fileName, content);
			}
		}
	} // renameContent

	/**
	 * Update File Content
	 * 
	 * @param fileName
	 * @param DMSContent
	 */
	public void updateContent(String fileName, MDMSContent DMSContent)
	{
		int DMS_Content_ID = Utils.getDMS_Content_Related_ID(DMSContent);
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
				MDMSContent content = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
				MDMSAssociation association = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
				this.renameFile(content, association, fileName, true);
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
	} // updateContent

	/**
	 * Rename File
	 * 
	 * @param content
	 * @param association
	 * @param fileName
	 * @param isAddFileExtention
	 */
	public void renameFile(MDMSContent content, MDMSAssociation association, String fileName, boolean isAddFileExtention)
	{
		String fileExt = "";
		String newPath = this.getFileFromStorage(content).getAbsolutePath();
		if (isAddFileExtention)
			fileExt = newPath.substring(newPath.lastIndexOf("."), newPath.length());
		newPath = newPath.substring(0, newPath.lastIndexOf(DMSConstant.FILE_SEPARATOR));
		newPath = newPath + DMSConstant.FILE_SEPARATOR + fileName + fileExt;
		newPath = Utils.getUniqueFilename(newPath);

		File oldFile = new File(this.getFileFromStorage(content).getAbsolutePath());
		File newFile = new File(newPath);
		oldFile.renameTo(newFile);

		content.setName(newFile.getAbsolutePath().substring(newFile.getAbsolutePath().lastIndexOf(DMSConstant.FILE_SEPARATOR) + 1,
				newFile.getAbsolutePath().length()));

		// Renaming to correct if any linkable docs exists for re-indexing
		content.setSyncIndexForLinkableDocs(true);

		content.saveEx();
	} // renameFile

	public String createLink(MDMSContent contentParent, MDMSContent clipboardContent, boolean isDir, int tableID, int recordID)
	{
		if (clipboardContent == null)
			return "";

		if (contentParent != null && isHierarchyContentExists(contentParent.getDMS_Content_ID(), clipboardContent.getDMS_Content_ID()))
		{
			return "You can't create link of parent content into itself or its children content";
		}

		boolean isDocPresent = this.isDocumentPresent(contentParent, clipboardContent, isDir);
		if (isDocPresent)
		{
			return "Document already exists at same position.";
		}

		int latestVerContentID = clipboardContent.getDMS_Content_ID();
		if (clipboardContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Content))
		{
			// Get latest versioning contentID for Link referencing
			List<Object> latestVersion = DB.getSQLValueObjectsEx(null, DMSConstant.SQL_GET_CONTENT_LATEST_VERSION_NONLINK,
					clipboardContent.getDMS_Content_ID(), clipboardContent.getDMS_Content_ID());
			if (latestVersion != null)
				latestVerContentID = ((BigDecimal) latestVersion.get(0)).intValue();
		}

		int contentID = 0;
		int associationID = 0;
		int contentRelatedID = 0;

		if (contentParent != null && contentParent.getDMS_Content_ID() > 0)
			contentRelatedID = contentParent.getDMS_Content_ID();

		// For Tab viewer
		if (tableID > 0 && recordID > 0)
		{
			associationID = this.createAssociation(latestVerContentID, contentRelatedID, recordID, tableID, MDMSAssociationType.LINK_ID, 0, null);
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
			contentID = latestVerContentID;
			associationID = this.createAssociation(latestVerContentID, contentRelatedID, 0, 0, MDMSAssociationType.LINK_ID, 0, null);
		}

		createIndexforLinkableContent(clipboardContent.getContentBaseType(), contentID, associationID);

		return null;
	} // createLink

	/**
	 * @param clipboardContent
	 * @param contentID
	 * @param associationID
	 */
	public void createIndexforLinkableContent(String contentBaseType, int contentID, int associationID)
	{
		// Here currently we can not able to move index creation logic in model
		// validator
		// TODO : In future, will find approaches for move index creation logic
		if (MDMSContent.CONTENTBASETYPE_Content.equals(contentBaseType))
		{
			MDMSContent contentLink = new MDMSContent(Env.getCtx(), contentID, null);
			MDMSAssociation associationLink = new MDMSAssociation(Env.getCtx(), associationID, null);
			this.createIndexContent(contentLink, associationLink);
		}
	}

	/**
	 * Check Clipboard/Copied Document already exists in same position for Copy
	 * to CreateLink operation
	 * 
	 * @param currContent - Current DMS Content
	 * @param copiedContent - Copied Content
	 * @param isDir - is Directory
	 * @return true if Document exists in same level
	 */
	public boolean isDocumentPresent(MDMSContent currContent, MDMSContent copiedContent, boolean isDir)
	{
		String sql = "	SELECT COUNT(DMS_Content_ID)	FROM DMS_Association 																			"
				+ "		WHERE 	(DMS_Content_ID = ? OR DMS_Content_ID IN ( 																				"
				+ "					SELECT DMS_Content_Related_ID FROM DMS_Association WHERE DMS_Content_ID = ? AND DMS_AssociationType_ID = 1000000 ) 	"
				+ "				) AND DMS_Content_Related_ID " + ((currContent == null && !isDir) ? "IS NULL" : " = " + currContent.getDMS_Content_ID());

		return DB.getSQLValue(null, sql.toString(), copiedContent.getDMS_Content_ID(), copiedContent.getDMS_Content_ID()) > 0 ? true : false;
	} // isDocumentPresent

	/**
	 * Delete Content from Storage and DB entry
	 * 
	 * @param content
	 * @throws IOException
	 */
	public void deleteContentWithPhysicalDocument(MDMSContent content) throws IOException
	{
		File document = this.getFileFromStorage(content);
		if (document.exists())
			document.delete();

		File thumbnails = new File(this.getThumbnailURL(content, null));

		if (thumbnails.exists())
			FileUtils.deleteDirectory(thumbnails);

		DB.executeUpdate("DELETE FROM DMS_Association 	WHERE DMS_Content_ID = ?", content.getDMS_Content_ID(), null);
		DB.executeUpdate("DELETE FROM DMS_Content 		WHERE DMS_Content_ID = ?", content.getDMS_Content_ID(), null);
	} // deleteContentWithPhysicalDocument

	/**
	 * This will be a soft deletion. System will only inactive the files.
	 * 
	 * @param dmsContent
	 * @param dmsAssociation
	 * @param isDeleteLinkableRefs - Is Delete References of Links to another
	 *            place
	 */
	public void deleteContent(MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs)
	{
		if (Utils.isLink(dmsAssociation))
		{
			setContentAndAssociationInActive(null, dmsAssociation);
			return;
		}
		else if (dmsContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Content))
		{
			HashMap<I_DMS_Content, I_DMS_Association> relatedContentList = null;
			if (dmsAssociation.getDMS_AssociationType_ID() == MDMSAssociationType.VERSION_ID)
			{
				MDMSContent parentContent = new MDMSContent(Env.getCtx(), dmsAssociation.getDMS_Content_Related_ID(), null);
				MDMSAssociation parentAssociation = this.getAssociationFromContent(parentContent.getDMS_Content_ID());
				setContentAndAssociationInActive(parentContent, parentAssociation);
				relatedContentList = getRelatedContents(parentContent);
			}
			else
			{
				setContentAndAssociationInActive(dmsContent, dmsAssociation);
				relatedContentList = getRelatedContents(dmsContent);
			}

			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : relatedContentList.entrySet())
			{
				MDMSContent content = (MDMSContent) entry.getKey();
				MDMSAssociation association = (MDMSAssociation) entry.getValue();
				setContentAndAssociationInActive(content, association);
			}
		}
		else if (dmsContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Directory))
		{
			// Inactive Directory content and its child recursively
			setContentAndAssociationInActive(dmsContent, dmsAssociation);
			HashMap<I_DMS_Content, I_DMS_Association> relatedContentList = getRelatedContents(dmsContent);
			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : relatedContentList.entrySet())
			{
				MDMSContent content = (MDMSContent) entry.getKey();
				MDMSAssociation association = (MDMSAssociation) entry.getValue();
				// recursive call
				deleteContent(content, association, isDeleteLinkableRefs);
			}
		}

		// Linkable association references to set as InActive
		if (isDeleteLinkableRefs)
		{
			HashMap<I_DMS_Association, I_DMS_Content> linkRefs = this.getLinkableAssociationWithContentRelated(dmsContent);
			for (Entry<I_DMS_Association, I_DMS_Content> linkRef : linkRefs.entrySet())
			{
				MDMSAssociation linkAssociation = (MDMSAssociation) linkRef.getKey();
				linkAssociation.setIsActive(false);
				linkAssociation.save();
			}
		}
	} // deleteContent

	private void setContentAndAssociationInActive(MDMSContent dmsContent, MDMSAssociation dmsAssociation)
	{
		if (dmsAssociation != null)
		{
			dmsAssociation.setIsActive(false);
			dmsAssociation.saveEx();
		}

		if (dmsContent != null)
		{
			dmsContent.setIsActive(false);
			dmsContent.saveEx();
		}
	} // setContentAndAssociationInActive

	/**
	 * Get info about linkable docs of the given content
	 * 
	 * @param content
	 * @param association
	 * @return Information about content and its linkable references path
	 */
	public String hasLinkableDocs(I_DMS_Content content, I_DMS_Association association)
	{
		String name = "";
		if (Utils.isLink(association))
			;
		else
		{
			int count = 0;
			HashMap<I_DMS_Association, I_DMS_Content> linkRefs = this.getLinkableAssociationWithContentRelated(content);
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
				HashMap<I_DMS_Content, I_DMS_Association> childContents = this.getDMSContentsWithAssociation((MDMSContent) content, AD_Client_ID, true);
				for (Entry<I_DMS_Content, I_DMS_Association> children : childContents.entrySet())
				{
					name += hasLinkableDocs(children.getKey(), children.getValue());
				}
			}
		}
		return name;
	} // hasLinkableDocs

	private HashMap<I_DMS_Content, I_DMS_Association> getRelatedContents(MDMSContent dmsContent)
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();
		String sql = "SELECT c.DMS_Content_ID, a.DMS_Association_ID FROM DMS_Content c"
				+ " INNER JOIN DMS_Association a ON c.DMS_Content_ID = a.DMS_Content_ID "
				+ " WHERE a.DMS_Content_Related_ID = ? AND c.IsActive='Y' AND a.IsActive='Y' ";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, dmsContent.getDMS_Content_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				map.put((new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null)), (new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"),
						null)));
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "getRelatedContents fetching failure: ", e);
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

	public HashMap<I_DMS_Content, I_DMS_Association> getGenericSearchedContent(String searchText, int tableID, int recordID, MDMSContent content)
	{
		StringBuffer query = new StringBuffer();
		if (!Util.isEmpty(searchText, true))
		{
			String inputParam = searchText.toLowerCase().trim().replaceAll(" +", " ");
			query.append("(").append(DMSConstant.NAME).append(":*").append(inputParam).append("*");
			query.append(" OR ").append(DMSConstant.DESCRIPTION).append(":*").append(inputParam).append("*");

			// Lookup from file content
			if (ServiceUtils.isAllowDocumentContentSearch())					
			{
				query.append(" OR ").append(ServiceUtils.FILE_CONTENT).append(":*").append(inputParam).append("*");
			}

			query.append(")");
		}
		else
		{
			query.append("*:*");
		}

		StringBuffer hirachicalContent = new StringBuffer(" AND DMS_Content_ID:(");

		this.getHierarchicalContent(hirachicalContent, content != null ? content.getDMS_Content_ID() : 0, tableID, recordID);

		if (content != null)
		{
			hirachicalContent.append(content.getDMS_Content_ID()).append(")");
			query.append(hirachicalContent.toString());
		}
		else
		{
			if (hirachicalContent.substring(hirachicalContent.length() - 4, hirachicalContent.length()).equals(" OR "))
			{
				hirachicalContent.replace(hirachicalContent.length() - 4, hirachicalContent.length(), ")");
				query.append(hirachicalContent.toString());
			}
		}

		// AD_Client_id append for search client wise
		if (!Util.isEmpty(query.toString()))
			query.append(" AND ");

		query.append(" AD_Client_ID:(").append(Env.getAD_Client_ID(Env.getCtx())).append(")").append(" AND Show_InActive : 'false'");

		if (recordID > 0)
			query.append(" AND Record_ID:" + recordID);

		if (tableID > 0)
			query.append(" AND AD_Table_ID:" + tableID);

		return fillSearchedContentMap(this.searchIndex(query.toString()));
	} // getGenericSearchedContent

	/**
	 * Fill searched content on map with latest version if exists
	 * 
	 * @param searchedList
	 * @return Map of searched Content with Association
	 */
	public HashMap<I_DMS_Content, I_DMS_Association> fillSearchedContentMap(List<Integer> searchedList)
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		for (Integer entry : searchedList)
		{
			List<Object> latestVersion = DB.getSQLValueObjectsEx(null, DMSConstant.SQL_GET_CONTENT_LATEST_VERSION_NONLINK, entry, entry);

			if (latestVersion != null)
			{
				map.put(new MDMSContent(Env.getCtx(), ((BigDecimal) latestVersion.get(0)).intValue(), null), new MDMSAssociation(Env.getCtx(),
						((BigDecimal) latestVersion.get(1)).intValue(), null));
			}
		}
		return map;
	} // fillSearchedContentMap

	private void getHierarchicalContent(StringBuffer hierarchicalContent, int DMS_Content_ID, int tableID, int recordID)
	{
		MDMSContent content = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
		HashMap<I_DMS_Content, I_DMS_Association> map = this.getDMSContentsWithAssociation(content, AD_Client_ID, false);
		for (Entry<I_DMS_Content, I_DMS_Association> mapEntry : map.entrySet())
		{
			MDMSContent dmsContent = (MDMSContent) mapEntry.getKey();

			if (dmsContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
				this.getHierarchicalContent(hierarchicalContent, dmsContent.getDMS_Content_ID(), tableID, recordID);
			else
			{
				MDMSAssociation association = (MDMSAssociation) mapEntry.getValue();
				hierarchicalContent.append(association.getDMS_Content_ID()).append(" OR ");

				if (association.getDMS_Content_ID() != dmsContent.getDMS_Content_ID())
					hierarchicalContent.append(dmsContent.getDMS_Content_ID()).append(" OR ");
			}
		}
	} // getHierarchicalContent

	public HashMap<I_DMS_Content, I_DMS_Association> renderSearchedContent(HashMap<String, List<Object>> queryParamas, MDMSContent content, int tableID,
			int recordID)
	{
		String query = this.buildSolrSearchQuery(queryParamas);

		// AD_Client_id append for search client wise
		if (!Util.isEmpty(query))
			query += " AND ";

		query += "AD_Client_ID :(" + (Env.getAD_Client_ID(Env.getCtx()) + ")");

		StringBuffer hirachicalContent = new StringBuffer(" AND DMS_Content_ID:(");

		this.getHierarchicalContent(hirachicalContent, content != null ? content.getDMS_Content_ID() : 0, tableID, recordID);

		if (content != null)
		{
			hirachicalContent.append(content.getDMS_Content_ID()).append(")");
			query += " " + hirachicalContent.toString();
		}
		else
		{
			if (hirachicalContent.substring(hirachicalContent.length() - 4, hirachicalContent.length()).equals(" OR "))
			{
				hirachicalContent.replace(hirachicalContent.length() - 4, hirachicalContent.length(), ")");
				query += " " + hirachicalContent.toString();
			}
		}

		return fillSearchedContentMap(this.searchIndex(query));
	}

	/**
	 * Convert .docx to .pdf convert .doc to .pdf
	 * 
	 * @param documentToPreview
	 * @param mimeType
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws DocumentException
	 * @throws com.itextpdf.text.DocumentException
	 */
	public File convertToPDF(File documentToPreview, MDMSMimeType mimeType) throws IOException, FileNotFoundException, DocumentException,
			com.itextpdf.text.DocumentException
	{
		if (mimeType.getMimeType().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
		{
			XWPFDocument document = new XWPFDocument(new FileInputStream(documentToPreview));
			File newDocPDF = File.createTempFile("DMSExport", "DocxToPDF");
			OutputStream pdfFile = new FileOutputStream(newDocPDF);
			PdfOptions options = PdfOptions.create();
			PdfConverter.getInstance().convert(document, pdfFile, options);
			return newDocPDF;
		}
		else if (mimeType.getMimeType().equals("application/msword"))
		{
			HWPFDocument doc = new HWPFDocument(new FileInputStream(documentToPreview));
			WordExtractor we = new WordExtractor(doc);
			File newDocPDF = File.createTempFile("DMSExport", "DocToPDF");
			OutputStream pdfFile = new FileOutputStream(newDocPDF);
			String k = we.getText();
			Document document = new Document();
			PdfWriter.getInstance(document, pdfFile);
			document.open();
			document.add(new Paragraph(k));
			document.close();
			return newDocPDF;
		}
		else if (mimeType.getMimeType().equals("application/vnd.ms-excel")
				|| mimeType.getMimeType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
		{
			String fileName = documentToPreview.getName();
			if (fileName != null && fileName.length() > 0)
			{
				String extension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
				if ("xls".equalsIgnoreCase(extension))
				{
					return convertXlsToPDF(documentToPreview);
				}
				else if ("xlsx".equalsIgnoreCase(extension))
				{
					return convertXlsxToPdf(documentToPreview);
				}
			}
		}

		return documentToPreview;
	} // convertToPDF

	/**
	 * Convert xls file to pdf
	 * 
	 * @param xlsDocument
	 * @return pdf file
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DocumentException
	 */
	private File convertXlsToPDF(File xlsDocument) throws FileNotFoundException, IOException, DocumentException
	{
		InputStream in = new FileInputStream(xlsDocument);
		ConvertXlsToPdf test = new ConvertXlsToPdf(in);
		String html = test.getHTML();

		File newXlsToPdf = File.createTempFile("DMSExport", "XlsToPDF");

		Document document = new Document(PageSize.A4, 20, 20, 20, 20);
		PdfWriter.getInstance(document, new FileOutputStream(newXlsToPdf));
		document.open();
		document.addCreationDate();

		HTMLWorker htmlWorker = new HTMLWorker(document);
		htmlWorker.parse(new StringReader(html));
		document.close();

		return newXlsToPdf;
	} // convertXlsToPDF

	/**
	 * Convert xlsx file to pdf file
	 * 
	 * @param xlsxDocument
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws com.itextpdf.text.DocumentException
	 * @throws com.itextpdf.text.DocumentException
	 */
	private File convertXlsxToPdf(File xlsxDocument) throws IOException, FileNotFoundException, com.itextpdf.text.DocumentException
	{
		File newXlsxToHTML = File.createTempFile("DMSExport", "XlsxToHTML");
		try
		{
			float pdfWidth = 1050;
			float pdfheight = 900;
			String sourcePath = xlsxDocument.getAbsolutePath();
			String destinationPath = newXlsxToHTML.getAbsolutePath();

			// Convert .xlsx file to html
			ConvertXlsxToPdf.convert(sourcePath, destinationPath);
			InputStream in = new FileInputStream(new File(destinationPath));

			// Convert html to xhtml
			Tidy tidy = new Tidy();
			File newHtmlToXhtml = File.createTempFile("DMSExport", "HtmlToXHtml");
			tidy.setShowWarnings(false);
			// tidy.setXmlTags(true);
			tidy.setXHTML(true);
			tidy.setMakeClean(true);
			org.w3c.dom.Document d = tidy.parseDOM(in, null);
			tidy.pprint(d, new FileOutputStream(newHtmlToXhtml));

			// Convert xhtml to pdf
			File newXhtmlToPdf = File.createTempFile("DMSExport", "XHtmlToPdf");
			com.itextpdf.text.Document document = new com.itextpdf.text.Document();
			Rectangle size = new Rectangle(pdfWidth, pdfheight);
			document.setPageSize(size);
			com.itextpdf.text.pdf.PdfWriter writer = com.itextpdf.text.pdf.PdfWriter.getInstance(document, new FileOutputStream(newXhtmlToPdf));
			document.open();

			XMLWorkerHelper.getInstance().parseXHtml(writer, document, new FileInputStream(newHtmlToXhtml));
			document.close();

			return newXhtmlToPdf;
		}
		catch (InvalidFormatException | ParserConfigurationException | TransformerException e)
		{
			throw new AdempiereException(e);
		}
	} // convertXlsxToPdf

	/**
	 * Check copy/cut content exists in same Hierarchy.
	 * 
	 * @param destContentID
	 * @param sourceContentID
	 * @return true if copy/cut content exists in same Hierarchy.
	 */
	public boolean isHierarchyContentExists(int destContentID, int sourceContentID)
	{
		int contentID = DB.getSQLValue(null, Utils.SQL_CHECK_HIERARCHY_CONTENT_RECURSIVELY, Env.getAD_Client_ID(Env.getCtx()), destContentID, sourceContentID);
		return contentID > 0;

	} // isHierarchyContentExists

}
