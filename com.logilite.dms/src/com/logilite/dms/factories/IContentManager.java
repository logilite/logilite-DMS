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

package com.logilite.dms.factories;

import java.io.File;
import java.util.Map;

import com.logilite.dms.DMS;
import com.logilite.dms.model.IFileStorageProvider;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSVersion;

/**
 * @author Deepak Pansheria
 * @author Sachin Bhimani
 */
public interface IContentManager
{
	public String getKey();

	public String getPathByValue(I_DMS_Content content);

	public String getPathByValue(I_DMS_Version version);

	public String getPathByName(I_DMS_Content content);

	public String getContentName(	IFileStorageProvider storageProvider, String contentType, I_DMS_Content content, String fileName, String extention,
									String type, String operationType);

	/*
	 * Model Creation
	 */

	public int createContent(	String name, String contentBaseType, String parentURL, String desc, File file, int contentTypeID, int asiID, boolean isMounting,
								String trxName);

	public int createAssociation(int dms_Content_ID, int contentRelatedID, int validRecordID, int validTableID, int associationTypeID, String trxName);

	public MDMSVersion createVersion(int contentID, String value, int seqNo, File file, String trxName);

	/*
	 * Operations
	 */

	public MDMSContent createDirHierarchy(DMS dms, String dirPath, MDMSContent dirContent, int validTableID, int validRecordID, String trxName);

	public MDMSContent createDirectory(	DMS dms, String dirName, MDMSContent parentContent, int validTableID, int validRecordID, boolean errorIfDirExists,
										boolean isCreateAssociation, String trxName);

	public int addFile(	DMS dms, MDMSContent parentContent, File file, String fileName, String desc, int contentTypeID, int asiID, int AD_Table_ID,
						int Record_ID, boolean isVersion);

	public int addFile(	DMS dms, String dirPath, File file, String fileName, String desc, String contentType, Map<String, String> attributeMap, int AD_Table_ID,
						int Record_ID, boolean isVersion);

	public int createContentAssociationFileStoreAndThumnail(DMS dms, MDMSContent parentContent, File file, String fileName, String desc, int contentTypeID,
															int asiID, int AD_Table_ID, int Record_ID, boolean isVersion, String trxName);

	public void writeFileOnStorageAndThumnail(DMS dms, File file, MDMSVersion version);

	/*
	 * 
	 */

	public void pasteCopyContent(	DMS dms, MDMSContent copiedContent, MDMSContent destContent, int tableID, int recordID,
									boolean isCreatePermissionForPasteContent);

	public void pasteCutContent(DMS dms, MDMSContent cutContent, MDMSContent destContent, int tableID, int recordID, boolean isDocExplorerWindow);

	public void renameContent(DMS dms, String fileName, MDMSContent content);

	public void renameContentOnly(DMS dms, MDMSContent content, String fileName, String description, boolean isDocExplorerWindow);

	public void renameFile(DMS dms, MDMSContent content, String fileName, boolean isAddFileExtention);

	public boolean isHierarchyContentExists(int destContentID, int sourceContentID);

	public void deleteContent(DMS dms, MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs);

	public void undoDeleteContent(DMS dms, MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs);

	public int checkContentExists(String parentURL, String contentName, boolean isActiveOnly, boolean isCheckByContentName);

}
