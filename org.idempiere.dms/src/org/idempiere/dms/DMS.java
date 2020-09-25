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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MImage;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.util.DMSSearchUtils;
import org.idempiere.dms.util.Utils;
import org.idempiere.dms.util.DMSFactoryUtils;
import org.idempiere.dms.util.DMSOprUtils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSMimeType;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

/**
 * DMS API SUPPORT
 * 
 * @author Sachin Bhimani
 */
public class DMS
{

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

		thumbnailProvider = DMSFactoryUtils.getThumbnailProvider(AD_Client_ID);

		if (thumbnailProvider == null)
			throw new AdempiereException("Thumbnail provider is not found.");

		contentManager = DMSFactoryUtils.getContentManager(AD_Client_ID);

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
		mountingStrategy = DMSFactoryUtils.getMountingStrategy(Table_Name);
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

	/**
	 * Utils Methods
	 */
	public MDMSContent getRootContent(int AD_Table_ID, int Record_ID)
	{
		return getMountingStrategy().getMountingParent(MTable.getTableName(Env.getCtx(), AD_Table_ID), Record_ID);
	} // getRootContent

	public MDMSContent createDirectory(String dirName, MDMSContent parentContent, int tableID, int recordID, boolean errorIfDirExists, String trxName)
	{
		return createDirectory(dirName, parentContent, tableID, recordID, errorIfDirExists, true, trxName);
	} // createDirectory

	public MDMSContent createDirectory(	String dirName, MDMSContent parentContent, int tableID, int recordID, boolean errorIfDirExists,
										boolean isCreateAssociation, String trxName)
	{
		return DMSOprUtils.createDirectory(this, dirName, parentContent, tableID, recordID, errorIfDirExists, isCreateAssociation, trxName);
	} // createDirectory

	/**
	 * Create Directory Hierarchy or Get leaf DMSContent from dirPath
	 * 
	 * @return {@link MDMSContent} - Leaf node
	 */
	public MDMSContent createDirHierarchy(String dirPath, MDMSContent dirContent, int AD_Table_ID, int Record_ID, String trxName)
	{
		dirContent = DMSOprUtils.createDirHierarchy(this, dirPath, dirContent, AD_Table_ID, Record_ID, trxName);
		return dirContent;
	} // createDirHierarchy

	public String getThumbnailURL(I_DMS_Content content, String size)
	{
		return thumbnailProvider.getURL(content, size);
	} // getThumbnailURL

	public File getThumbnailFile(I_DMS_Content content, String size)
	{
		return thumbnailProvider.getFile(content, size);
	} // getThumbnailFile

	public MImage getDirThumbnail()
	{
		return Utils.getDirThumbnail();
	} // getDirThumbnail

	public MImage getMimetypeThumbnail(int dms_MimeType_ID)
	{
		return MDMSMimeType.getThumbnail(dms_MimeType_ID);
	} // getMimetypeThumbnail

	public String getPathFromContentManager(I_DMS_Content content)
	{
		return contentManager.getPathByValue(content);
	} // getPathFromContentManager

	public String getActualFileOrDirName(String contentType, I_DMS_Content content, String fileName, String extention, String type, String operationType)
	{
		return contentManager.getContentName(fileStorageProvider, contentType, content, fileName, extention, type, operationType);
	} // getActualFileOrDirName

	public File getFileFromStorage(I_DMS_Content content)
	{
		return fileStorageProvider.getFile(this.getPathFromContentManager(content));
	} // getFileFromStorage

	public File getFileFromStorage(String path)
	{
		return fileStorageProvider.getFile(path);
	} // getFileFromStorage

	public String getBaseDirPath(MDMSContent content)
	{
		return fileStorageProvider.getBaseDirectory(this.getPathFromContentManager(content));
	} // getBaseDirPath

	public List<Integer> searchIndex(String query)
	{
		return indexSearcher.searchIndex(query);
	} // searchIndex

	public String buildSolrSearchQuery(HashMap<String, List<Object>> params)
	{
		return indexSearcher.buildSolrSearchQuery(params);
	} // buildSolrSearchQuery

	public void createIndexContent(MDMSContent content, MDMSAssociation association)
	{
		indexSearcher.indexContent(DMSSearchUtils.createIndexMap(content, association));
	} // createIndexContent

	public void initiateMountingContent(String tableName, int recordID, int tableID)
	{
		this.initiateMountingContent(Utils.getDMSMountingBase(AD_Client_ID), tableName, recordID, tableID);
	} // initiateMountingContent

	public void initiateMountingContent(String mountingBaseName, String tableName, int recordID, int tableID)
	{
		Utils.initiateMountingContent(mountingBaseName, tableName, recordID, tableID);
	} // initiateMountingContent

	public int createDMSContent(String name, String value, String contentBaseType, String parentURL, String desc, File file, int contentTypeID, int asiID,
								boolean isMounting, String trxName)
	{
		return MDMSContent.create(name, value, contentBaseType, parentURL, desc, file, contentTypeID, asiID, isMounting, trxName);
	} // createDMSContent

	public int createAssociation(int dms_Content_ID, int contentRelatedID, int Record_ID, int AD_Table_ID, int associationTypeID, int seqNo, String trxName)
	{
		return MDMSAssociation.create(dms_Content_ID, contentRelatedID, Record_ID, AD_Table_ID, associationTypeID, seqNo, trxName);
	} // createAssociation

	public MDMSAssociation getAssociationFromContent(int contentID)
	{
		return MDMSAssociation.getAssociationFromContent(contentID, null);
	} // getAssociationFromContent

	public MDMSAssociation getAssociationFromContent(int contentID, boolean isLinkAssociationOnly)
	{
		return MDMSAssociation.getAssociationFromContent(contentID, isLinkAssociationOnly, null);
	} // getAssociationFromContent

	public HashMap<I_DMS_Content, I_DMS_Association> getDMSContentsWithAssociation(MDMSContent content, int AD_Client_ID, boolean isActiveOnly)
	{
		return DMSSearchUtils.getDMSContentsWithAssociation(content, AD_Client_ID, isActiveOnly);
	} // getDMSContentsWithAssociation

	public HashMap<I_DMS_Association, I_DMS_Content> getLinkableAssociationWithContentRelated(I_DMS_Content content)
	{
		return ((MDMSContent) content).getLinkableAssociationWithContentRelated();
	} // getLinkableAssociationWithContentRelated

	/**
	 * Adding files and File Version
	 */
	public int addFile(File file)
	{
		return addFile(null, file);
	} // addFile

	public int addFile(String dirPath, File file)
	{
		return addFile(dirPath, file, file.getName());
	} // addFile

	public int addFile(String dirPath, File file, String fileName)
	{
		return addFile(dirPath, file, fileName, 0, 0);
	} // addFile

	public int addFile(String dirPath, File file, String fileName, int AD_Table_ID, int Record_ID)
	{
		return addFile(dirPath, file, fileName, null, null, AD_Table_ID, Record_ID);
	} // addFile

	public int addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap)
	{
		return addFile(dirPath, file, fileName, contentType, attributeMap, 0, 0);
	} // addFile

	public int addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap, int AD_Table_ID, int Record_ID)
	{
		return DMSOprUtils.addFile(this, dirPath, file, fileName, null, contentType, attributeMap, AD_Table_ID, Record_ID, false);
	} // addFile

	public int addFile(MDMSContent parentContent, File file, int AD_Table_ID, int Record_ID)
	{
		return addFile(parentContent, file, file.getName(), null, 0, 0, AD_Table_ID, Record_ID);
	} // addFile

	public int addFile(MDMSContent parentContent, File file, String name, String desc, int contentTypeID, int asiID, int AD_Table_ID, int Record_ID)
	{
		return DMSOprUtils.addFile(this, parentContent, file, name, desc, contentTypeID, asiID, AD_Table_ID, Record_ID, false);
	} // addFile

	/**
	 * Add Version File
	 */
	public int addFileVersion(int DMS_Content_ID, File file)
	{
		MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(DMS_Content_ID, null);
		return addFileVersion(content, file, null);
	} // addFileVersion

	public int addFileVersion(MDMSContent parentContent, File file)
	{
		return addFileVersion(parentContent, file, null);
	} // addFileVersion

	public int addFileVersion(MDMSContent parentContent, File file, String desc)
	{
		return addFileVersion(parentContent, file, desc, 0, 0);
	} // addFileVersion

	public int addFileVersion(MDMSContent parentContent, File file, String desc, int AD_Table_ID, int Record_ID)
	{
		return DMSOprUtils.addFile(this, parentContent, file, null, desc, 0, 0, AD_Table_ID, Record_ID, true);
	} // addFileVersion

	/**
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
	 * Other operations on Content
	 */

	/**
	 * Paste the content [ Copy Operation ]
	 * 
	 * @param copiedContent - Content From
	 * @param destContent   - Content To
	 * @param tableID       - AD_Table_ID
	 * @param recordID      - Record_ID
	 */
	public void pasteCopyContent(MDMSContent copiedContent, MDMSContent destContent, int tableID, int recordID)
	{
		DMSOprUtils.pasteCopyContent(this, copiedContent, destContent, tableID, recordID);
	} // pasteCopyContent

	/**
	 * Paste the content [ Cut operation ]
	 * 
	 * @param cutContent  - Content From
	 * @param destContent - Content To
	 * @param tableID     - AD_Table_ID
	 * @param recordID    - Record_ID
	 */
	public void pasteCutContent(MDMSContent cutContent, MDMSContent destContent, int tableID, int recordID)
	{
		DMSOprUtils.pasteCutContent(this, cutContent, destContent, tableID, recordID, isDocExplorerWindow);
	} // pasteCutContent

	/**
	 * Rename Content
	 *
	 * @param  fileName           - New Filename
	 * @param  content            - Content
	 * @throws AdempiereException
	 */
	public void renameContent(String fileName, MDMSContent content) throws AdempiereException
	{
		DMSOprUtils.renameContent(this, fileName, content);
	} // renameContent

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
		DMSOprUtils.renameContent(this, fileName, content, parent_Content, tableID, recordID, isDocExplorerWindow);
	} // renameContent

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
		DMSOprUtils.renameFile(this, content, fileName, isAddFileExtention);
	} // renameFile

	public String createLink(MDMSContent contentParent, MDMSContent clipboardContent, boolean isDir, int tableID, int recordID)
	{
		return DMSOprUtils.createLink(this, contentParent, clipboardContent, isDir, tableID, recordID);
	} // createLink

	/**
	 * Delete Content from Storage and DB entry
	 * 
	 * @param  content
	 * @throws IOException
	 */
	public void deleteContentWithPhysicalDocument(MDMSContent content) throws IOException
	{
		DMSOprUtils.deleteContentWithPhysicalDocument(this, content);
	} // deleteContentWithPhysicalDocument

	/**
	 * This will be a soft deletion. System will only inactive the files.
	 * 
	 * @param dmsContent
	 * @param dmsAssociation
	 * @param isDeleteLinkableRefs - Is Delete References of Links to another place
	 */
	public void deleteContent(MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs)
	{
		DMSOprUtils.deleteContent(this, dmsContent, dmsAssociation, isDeleteLinkableRefs);
	} // deleteContent

	/**
	 * Get info about linkable docs of the given content
	 * 
	 * @param  content
	 * @param  association
	 * @return             Information about content and its linkable references path
	 */
	public String hasLinkableDocs(I_DMS_Content content, I_DMS_Association association)
	{
		return DMSOprUtils.hasLinkableDocs(this, content, association);
	} // hasLinkableDocs

	public HashMap<I_DMS_Content, I_DMS_Association> getGenericSearchedContent(String searchText, int tableID, int recordID, MDMSContent content)
	{
		return DMSSearchUtils.getGenericSearchedContent(this, searchText, tableID, recordID, content);
	} // getGenericSearchedContent

	public HashMap<I_DMS_Content, I_DMS_Association> renderSearchedContent(	HashMap<String, List<Object>> queryParamas, MDMSContent content, int tableID,
																			int recordID)
	{
		return DMSSearchUtils.renderSearchedContent(this, queryParamas, content, tableID, recordID);
	} // renderSearchedContent

	/**
	 * Check copy/cut content exists in same Hierarchy.
	 * 
	 * @param  destContentID
	 * @param  sourceContentID
	 * @return                 true if copy/cut content exists in same Hierarchy.
	 */
	public boolean isHierarchyContentExists(int destContentID, int sourceContentID)
	{
		int contentID = DB.getSQLValue(	null, DMSConstant.SQL_CHECK_HIERARCHY_CONTENT_RECURSIVELY, Env.getAD_Client_ID(Env.getCtx()), destContentID,
										sourceContentID);
		return contentID > 0;
	} // isHierarchyContentExists

}
