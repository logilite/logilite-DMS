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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClientInfo;
import org.compiere.model.MImage;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.IPermissionManager;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.util.DMSFactoryUtils;
import org.idempiere.dms.util.DMSOprUtils;
import org.idempiere.dms.util.DMSPermissionUtils;
import org.idempiere.dms.util.DMSSearchUtils;
import org.idempiere.dms.util.Utils;
import org.idempiere.model.DMSSubstituteTableInfo;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSMimeType;
import org.idempiere.model.MDMSVersion;

import com.logilite.search.factory.IIndexSearcher;

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
	private IPermissionManager		permissionManager			= null;

	private boolean					isDocExplorerWindow			= false;

	public int						AD_Client_ID				= 0;

	private DMSSubstituteTableInfo	ssTableInfo					= null;

	// Used for thread safety
	private static Object			o							= new Object();

	/**
	 * Constructor for initialize provider
	 */
	public DMS(int AD_Client_ID)
	{
		synchronized (o)
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

			indexSearcher = DMSSearchUtils.getIndexSearcher(AD_Client_ID);

			if (indexSearcher == null)
				throw new AdempiereException("Index server is not found.");

			// When open Document Explorer
			ssTableInfo = new DMSSubstituteTableInfo(0);

			//
			permissionManager = DMSFactoryUtils.getPermissionFactory();
		}
	} // Constructor

	/**
	 * Constructor for initialize provider
	 */
	public DMS(int AD_Client_ID, String Table_Name)
	{
		this(AD_Client_ID);
		initMountingStrategy(Table_Name);
	} // Constructor

	public String getContentManagerType()
	{
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);
		return clientInfo.get_ValueAsString("DMS_ContentManagerType");
	} // getContentManagerType

	public void initMountingStrategy(String Table_Name)
	{
		// Initiate the Substitute table info
		synchronized (o)
		{
			ssTableInfo = new DMSSubstituteTableInfo(MTable.getTable_ID(Table_Name));
			mountingStrategy = DMSFactoryUtils.getMountingStrategy(getContentManagerType(), validTableName(Table_Name));
		}
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

	// For Archive
	public MDMSContent getMountingParentForArchive()
	{
		return getMountingStrategy().getMountingParentForArchive();
	}

	public MDMSContent getMountingContentForArchive(int AD_Table_ID, int Record_ID, int Process_ID)
	{
		return getMountingStrategy().getMountingParentForArchive(AD_Table_ID, Record_ID, Process_ID);
	}

	// For Explorer
	public MDMSContent getRootMountingContent(int AD_Table_ID, int Record_ID)
	{
		return getDMSMountingParent(AD_Table_ID, Record_ID);
	}

	public MDMSContent getDMSMountingParent(int AD_Table_ID, int Record_ID)
	{
		return getMountingStrategy().getMountingParent(validTableID(AD_Table_ID), validRecordID(Record_ID));
	}

	public MDMSContent getDMSMountingParent(String Table_Name, int Record_ID)
	{
		return getMountingStrategy().getMountingParent(validTableName(Table_Name), validRecordID(Record_ID));
	}

	public MDMSContent getDMSMountingParent(PO po)
	{
		return getDMSMountingParent(po.get_Table_ID(), po.get_ID());
	}

	public MDMSContent createDirectory(String dirName, MDMSContent parentContent, int tableID, int recordID, boolean errorIfDirExists, String trxName)
	{
		return createDirectory(dirName, parentContent, tableID, recordID, errorIfDirExists, true, trxName);
	} // createDirectory

	public MDMSContent createDirectory(	String dirName, MDMSContent parentContent, int tableID, int recordID, boolean errorIfDirExists,
										boolean isCreateAssociation, String trxName)
	{
		return contentManager.createDirectory(	this, dirName, parentContent, validTableID(tableID), validRecordID(recordID), errorIfDirExists,
												isCreateAssociation, trxName);
	} // createDirectory

	/**
	 * Create Directory Hierarchy or Get leaf DMSContent from dirPath
	 * 
	 * @return {@link MDMSContent} - Leaf node
	 */
	public MDMSContent createDirHierarchy(String dirPath, MDMSContent dirContent, int AD_Table_ID, int Record_ID, String trxName)
	{
		dirContent = contentManager.createDirHierarchy(this, dirPath, dirContent, validTableID(AD_Table_ID), validRecordID(Record_ID), trxName);
		return dirContent;
	} // createDirHierarchy

	public String getThumbnailURL(I_DMS_Version version, String size)
	{
		return thumbnailProvider.getURL(version, size);
	} // getThumbnailURL

	public File getThumbnailFile(I_DMS_Version version, String size)
	{
		return thumbnailProvider.getFile(version, size);
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

	public String getPathFromContentManager(I_DMS_Version version)
	{
		return contentManager.getPathByValue(version);
	} // getPathFromContentManager

	public String getActualFileOrDirName(String contentType, I_DMS_Content content, String fileName, String extention, String type, String operationType)
	{
		return contentManager.getContentName(fileStorageProvider, contentType, content, fileName, extention, type, operationType);
	} // getActualFileOrDirName

	public File getFileFromStorageLatestVersionOnly(I_DMS_Content content)
	{
		return getFileFromStorage(MDMSVersion.getLatestVersion(content));
	} // getFileFromStorageLatestVersionOnly

	public File[] getFileFromStorageAllVersion(I_DMS_Content content)
	{
		return DMSOprUtils.getFileFromStorageAllVersion(this, content);
	} // getFileFromStorageAllVersion

	public File getFileFromStorage(I_DMS_Version version)
	{
		return fileStorageProvider.getFile(this.getPathFromContentManager(version));
	} // getFileFromStorage

	public File getFileFromStorage(String path)
	{
		return fileStorageProvider.getFile(path);
	} // getFileFromStorage

	public String getBaseDirPath(I_DMS_Content content)
	{
		return fileStorageProvider.getBaseDirectory(this.getPathFromContentManager(content));
	} // getBaseDirPath

	public String getBaseDirPath(MDMSVersion version)
	{
		return fileStorageProvider.getBaseDirectory(this.getPathFromContentManager(version));
	} // getBaseDirPath

	public int checkContentSoftNameExists(MDMSContent parentContent, String fileName)
	{
		return contentManager.checkContentExists(getPathFromContentManager(parentContent), fileName, false, true);
	} // checkContentSoftNameExists

	public int checkContentPhysicalNameExists(MDMSContent parentContent, String fileName)
	{
		return contentManager.checkContentExists(getPathFromContentManager(parentContent), fileName, false, false);
	} // checkContentPhysicalNameExists

	/*
	 * Index server util methods
	 */

	public HashSet<Integer> searchIndex(String query)
	{
		return DMSSearchUtils.searchIndex(indexSearcher, query);
	} // searchIndex

	public String buildSolrSearchQuery(HashMap<String, List<Object>> params)
	{
		return indexSearcher.buildSolrSearchQuery(params);
	} // buildSolrSearchQuery

	public void createIndexContent(MDMSContent content, MDMSAssociation association)
	{
		this.createIndexContent(content, association, MDMSVersion.getLatestVersion(content, false));
	} // createIndexContent

	public void createIndexContent(I_DMS_Content content, I_DMS_Association association, I_DMS_Version version)
	{
		File file = getFileFromStorage(version);
		indexSearcher.indexContent(DMSSearchUtils.createIndexMap(content, association, version, file));
	} // createIndexContent

	public HashMap<I_DMS_Version, I_DMS_Association> getGenericSearchedContent(	String searchText, int tableID, int recordID, MDMSContent content,
																				String documentView)
	{
		return DMSSearchUtils.getGenericSearchedContent(this, searchText, validTableID(tableID), validRecordID(recordID), content, documentView);
	} // getGenericSearchedContent

	public HashMap<I_DMS_Version, I_DMS_Association> renderSearchedContent(	HashMap<String, List<Object>> queryParamas, MDMSContent content, int tableID,
																			int recordID)
	{
		return DMSSearchUtils.renderSearchedContent(this, queryParamas, content, validTableID(tableID), validRecordID(recordID));
	} // renderSearchedContent

	/*
	 * Initial mounting content creation util methods
	 */
	public void initiateMountingContent(String tableName, int recordID, int tableID)
	{
		this.initiateMountingContent(Utils.getDMSMountingBase(AD_Client_ID), tableName, recordID, tableID);
	} // initiateMountingContent

	public void initiateMountingContent(String mountingBaseName, String tableName, int recordID, int tableID)
	{
		synchronized (o)
		{
			this.ssTableInfo.updateRecord(recordID);
			getMountingStrategy().initiateMountingContent(mountingBaseName, validTableName(tableName), validRecordID(recordID), validTableID(tableID));
		}
	} // initiateMountingContent

	/*
	 * Object creation util methods
	 */
	public int createDMSContent(String name, String contentBaseType, String parentURL, String desc, File file, int contentTypeID, int asiID,
								boolean isMounting, String trxName)
	{
		return contentManager.createContent(name, contentBaseType, parentURL, desc, file, contentTypeID, asiID, isMounting, trxName);
	} // createDMSContent

	public int createAssociation(int dms_Content_ID, int contentRelatedID, int Record_ID, int AD_Table_ID, int associationTypeID, String trxName)
	{
		return contentManager.createAssociation(dms_Content_ID, contentRelatedID, validRecordID(Record_ID), validTableID(AD_Table_ID), associationTypeID,
												trxName);
	} // createAssociation

	public MDMSVersion createVersion(String value, int contentID, int seqNo, File file, String trxName)
	{
		return contentManager.createVersion(contentID, value, seqNo, file, trxName);
	} // createVersion

	public String createLink(MDMSContent contentParent, MDMSContent clipboardContent, boolean isDir, int tableID, int recordID)
	{
		return contentManager.createLink(this, contentParent, clipboardContent, isDir, validTableID(tableID), validRecordID(recordID));
	} // createLink

	/*
	 * Content and Association related util methods
	 */

	public List<MDMSAssociation> getAssociationFromContent(int contentID, int associationTypeID, boolean isActiveOnly, String trxName)
	{
		return MDMSAssociation.getAssociationFromContent(contentID, associationTypeID, isActiveOnly, trxName);
	} // getAssociationFromContent

	public MDMSAssociation getParentAssociationFromContent(int contentID)
	{
		return getParentAssociationFromContent(contentID, true);
	} // getParentAssociationFromContent

	public MDMSAssociation getParentAssociationFromContent(int contentID, boolean isActiveOnly)
	{
		return MDMSAssociation.getParentAssociationFromContent(contentID, isActiveOnly, null);
	} // getParentAssociationFromContent

	public List<MDMSAssociation> getLinkableAssociationFromContent(int contentID)
	{
		return MDMSAssociation.getLinkableAssociationFromContent(contentID, true);
	} // getLinkableAssociationFromContent

	public List<MDMSAssociation> getLinkableAssociationFromContent(int contentID, boolean isActiveOnly)
	{
		return MDMSAssociation.getLinkableAssociationFromContent(contentID, isActiveOnly);
	} // getLinkableAssociationFromContent

	public HashMap<I_DMS_Version, I_DMS_Association> getDMSContentsWithAssociation(I_DMS_Content content, int AD_Client_ID, boolean isActiveOnly)
	{
		return DMSSearchUtils.getDMSContentsWithAssociation(content, AD_Client_ID, (isActiveOnly	? DMSConstant.DOCUMENT_VIEW_NON_DELETED_VALUE
																									: DMSConstant.DOCUMENT_VIEW_ALL_VALUE));
	} // getDMSContentsWithAssociation

	public HashMap<I_DMS_Version, I_DMS_Association> getDMSContentsWithAssociation(I_DMS_Content content, int AD_Client_ID, String documentView)
	{
		return DMSSearchUtils.getDMSContentsWithAssociation(content, AD_Client_ID, documentView);
	} // getDMSContentsWithAssociation

	public HashMap<I_DMS_Association, I_DMS_Content> getLinkableAssociationWithContentRelated(I_DMS_Content content)
	{
		return getLinkableAssociationWithContentRelated(content, true);
	} // getLinkableAssociationWithContentRelated

	public HashMap<I_DMS_Association, I_DMS_Content> getLinkableAssociationWithContentRelated(I_DMS_Content content, boolean isActiveonly)
	{
		return ((MDMSContent) content).getLinkableAssociationWithContentRelated(isActiveonly);
	} // getLinkableAssociationWithContentRelated

	/*
	 * Add file util methods
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
		return addFile(dirPath, file, fileName, null, contentType, attributeMap, AD_Table_ID, Record_ID);
	} // addFile

	public int addFile(	String dirPath, File file, String fileName, String description, String contentType, Map<String, String> attributeMap, int AD_Table_ID,
						int Record_ID)
	{
		return contentManager.addFile(	this, dirPath, file, fileName, description, contentType, attributeMap, validTableID(AD_Table_ID),
										validRecordID(Record_ID), false);
	} // addFile

	public int addFile(MDMSContent parentContent, File file, int AD_Table_ID, int Record_ID)
	{
		return addFile(parentContent, file, file.getName(), null, 0, 0, AD_Table_ID, Record_ID);
	} // addFile

	public int addFile(MDMSContent parentContent, File file, String name, String desc, int contentTypeID, int asiID, int AD_Table_ID, int Record_ID)
	{
		return contentManager.addFile(this, parentContent, file, name, desc, contentTypeID, asiID, validTableID(AD_Table_ID), validRecordID(Record_ID), false);
	} // addFile

	/*
	 * Add Version File util methods
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
		return contentManager.addFile(this, parentContent, file, null, desc, 0, 0, validTableID(AD_Table_ID), validRecordID(Record_ID), true);
	} // addFileVersion

	/*
	 * Update Content/Association util methods
	 */

	public void updateContentTypeAndAttribute(int contentID, String contentType, Map<String, String> attributeMap)
	{
		DMSOprUtils.updateContentTypeAndAttribute(this, contentID, contentType, attributeMap);
	}

	public void updateAssociation(MDMSAssociation association, int AD_Table_ID, int Record_ID)
	{
		association.updateTableRecordRef(validTableID(AD_Table_ID), validRecordID(Record_ID));
	}

	/*
	 * Select Content util methods
	 */

	public I_DMS_Content[] selectContent(int AD_Table_ID, int Record_ID)
	{
		return selectContent(AD_Table_ID, Record_ID, 0);
	}

	public I_DMS_Content[] selectContent(int AD_Table_ID, int Record_ID, int associationTypeID)
	{
		return selectContent(AD_Table_ID, Record_ID, associationTypeID, 0);
	}

	public I_DMS_Content[] selectContent(int AD_Table_ID, int Record_ID, int associationTypeID, int contentTypeID)
	{
		return selectContent(AD_Table_ID, Record_ID, associationTypeID, contentTypeID, null);
	}

	public I_DMS_Content[] selectContent(int AD_Table_ID, int Record_ID, int associationTypeID, int contentTypeID, String filename)
	{
		return selectContent(getRootMountingContent(AD_Table_ID, Record_ID), associationTypeID, contentTypeID, filename);
	}

	public I_DMS_Content[] selectContent(MDMSContent parentContent)
	{
		return selectContent(parentContent, 0);
	}

	public I_DMS_Content[] selectContent(MDMSContent parentContent, int contentTypeID)
	{
		return selectContent(parentContent, 0, contentTypeID);
	}

	public I_DMS_Content[] selectContent(MDMSContent parentContent, int associationTypeID, int contentTypeID)
	{
		return selectContent(parentContent, associationTypeID, contentTypeID, null);
	}

	public I_DMS_Content[] selectContent(MDMSContent parentContent, int associationTypeID, int contentTypeID, String filename)
	{
		return DMSSearchUtils.selectContentActiveOnly(parentContent, associationTypeID, contentTypeID, filename);
	}

	public I_DMS_Content[] selectContent(	MDMSContent parentContent, int contentTypeID,
											boolean isApplyContentTypeAccessFilter, boolean isApplyPermissionFilter)
	{
		return selectContent(parentContent, 0, contentTypeID, isApplyContentTypeAccessFilter, isApplyPermissionFilter);
	}

	public I_DMS_Content[] selectContent(	MDMSContent parentContent, int associationTypeID, int contentTypeID,
											boolean isApplyContentTypeAccessFilter, boolean isApplyPermissionFilter)
	{
		return selectContent(parentContent, associationTypeID, contentTypeID, null, isApplyContentTypeAccessFilter, isApplyPermissionFilter);
	}

	public I_DMS_Content[] selectContent(	MDMSContent parentContent, int associationTypeID, int contentTypeID, String filename,
											boolean isApplyContentTypeAccessFilter, boolean isApplyPermissionFilter)
	{
		I_DMS_Content[] selectedContent = selectContent(parentContent, associationTypeID, contentTypeID, filename);
		return DMSSearchUtils.getFilteredContents(selectedContent, isApplyContentTypeAccessFilter, isApplyPermissionFilter);
	} // selectContent

	/**
	 * Get list of child content based on ParentContent
	 * 
	 * @implSpec               filter applied for access on ContentType and Permission if applicable
	 * @param    parentContent - Directory type content
	 * @return                 List of child content versions
	 */
	public I_DMS_Version[] selectChildContentFiltered(I_DMS_Content parentContent)
	{
		return selectChildContentFiltered(parentContent, true, true);
	} // selectChildContentFiltered

	/**
	 * Get list of child content based on ParentContent
	 * 
	 * @param  parentContent                  - Directory type content
	 * @param  isApplyContentTypeAccessFilter
	 * @param  isApplyPermissionFilter
	 * @return                                List of child content versions
	 */
	public I_DMS_Version[] selectChildContentFiltered(I_DMS_Content parentContent, boolean isApplyContentTypeAccessFilter, boolean isApplyPermissionFilter)
	{
		return DMSSearchUtils.getChildContentFiltered(this, parentContent, isApplyContentTypeAccessFilter, isApplyPermissionFilter);
	} // selectChildContentFiltered

	/*
	 * Other operations on Content util methods
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
		contentManager.pasteCopyContent(this, copiedContent, destContent, validTableID(tableID), validRecordID(recordID));
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
		contentManager.pasteCutContent(this, cutContent, destContent, validTableID(tableID), validRecordID(recordID), isDocExplorerWindow);
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
		contentManager.renameContent(this, fileName, content);
	} // renameContent

	/**
	 * Rename Content File/Directory only in Content not in physical path
	 * 
	 * @param content     - Content
	 * @param fileName    - File name - without extension
	 * @param description - Description for the content
	 */
	public void renameContentOnly(MDMSContent content, String fileName, String description)
	{
		contentManager.renameContentOnly(this, content, fileName, description, isDocExplorerWindow);
	} // renameContentOnly

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
		contentManager.renameFile(this, content, fileName, isAddFileExtention);
	} // renameFile

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
		contentManager.deleteContent(this, dmsContent, dmsAssociation, isDeleteLinkableRefs);

	} // deleteContent

	/**
	 * This will be a undo soft deletion. System will only active the files.
	 * 
	 * @param dmsContent
	 * @param dmsAssociation
	 * @param isDeleteLinkableRefs - Is Delete References of Links to another place
	 */
	public void undoDeleteContent(MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs)
	{
		contentManager.undoDeleteContent(this, dmsContent, dmsAssociation, isDeleteLinkableRefs);
	} // undoDeleteContent

	/**
	 * Get info about linkable docs of the given content
	 * 
	 * @param  content
	 * @param  association
	 * @return             Information about content and its linkable references path
	 */
	public String hasLinkableDocs(I_DMS_Content content, I_DMS_Association association)
	{
		return contentManager.hasLinkableDocs(this, content, association);
	} // hasLinkableDocs

	/**
	 * Check copy/cut content exists in same Hierarchy.
	 * 
	 * @param  destContentID
	 * @param  sourceContentID
	 * @return                 true if copy/cut content exists in same Hierarchy.
	 */
	public boolean isHierarchyContentExists(int destContentID, int sourceContentID)
	{
		return contentManager.isHierarchyContentExists(destContentID, sourceContentID);
	} // isHierarchyContentExists

	/**
	 * Parsing table and recordID based on Substitute Table configuration
	 */

	// Get Valid TableID
	public int validTableID(int tableID)
	{
		if (ssTableInfo != null && ssTableInfo.getSubstitute() != null)
			return ssTableInfo.getSubstituteTable_ID();
		else
			return tableID;
	} // validTableID

	// Get Valid TableName
	public String validTableName(String tableName)
	{
		if (ssTableInfo != null && ssTableInfo.getSubstitute() != null)
			return ssTableInfo.getSubstituteTable_Name();
		else
			return tableName;
	} // validTableName

	// Get Valid RecordID
	public int validRecordID(int record_ID)
	{
		if (ssTableInfo == null)
		{
			return record_ID;
		}
		else if (ssTableInfo.getSubstitute() != null && ssTableInfo.getSubstituteRecord_ID() == record_ID)
		{
			return ssTableInfo.getValidRecord_ID();
		}
		else
		{
			ssTableInfo.updateRecord(record_ID);
			return ssTableInfo.getValidRecord_ID();
		}
	} // validRecordID

	// Get Substitute Table Information
	public DMSSubstituteTableInfo getSubstituteTableInfo()
	{
		return ssTableInfo;
	}

	/*
	 * DMS Content access based on Permission granted
	 */

	public IPermissionManager getPermissionManager()
	{
		return permissionManager;
	} // getPermissionManager

	public void grantChildPermissionFromParentContent(MDMSContent content, MDMSContent parentContent)
	{
		if (DMSPermissionUtils.isPermissionAllowed())
			permissionManager.grantChildPermissionFromParentContent(content, parentContent);
	} // grantChildPermissionFromParentContent

	public boolean isWritePermission(MDMSContent content)
	{
		if (DMSPermissionUtils.isPermissionAllowed())
		{
			permissionManager.initContentPermission(content);
			return permissionManager.isWrite();
		}
		return true;
	} // isWritePermission

	public boolean isDeletePermission(MDMSContent content)
	{
		if (DMSPermissionUtils.isPermissionAllowed())
		{
			permissionManager.initContentPermission(content);
			return permissionManager.isDelete();
		}
		return true;
	}
	// isDeletePermission

	public boolean isReadPermission(MDMSContent content)
	{
		if (DMSPermissionUtils.isPermissionAllowed())
		{
			permissionManager.initContentPermission(content);
			return permissionManager.isRead();
		}
		return true;
	} // isReadPermission

	public boolean isNavigationPermission(MDMSContent content)
	{
		if (DMSPermissionUtils.isPermissionAllowed())
		{
			permissionManager.initContentPermission(content);
			return permissionManager.isNavigation();
		}
		return true;
	} // isNavigationPermission

	public boolean isAllPermissionGranted(MDMSContent content)
	{
		if (DMSPermissionUtils.isPermissionAllowed())
		{
			permissionManager.initContentPermission(content);
			return permissionManager.isAllPermission();
		}
		return true;
	} // isAllPermissionGranted

}
