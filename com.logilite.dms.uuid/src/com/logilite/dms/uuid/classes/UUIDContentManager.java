package com.logilite.dms.uuid.classes;

import java.io.File;
import java.util.Map;

import org.idempiere.dms.DMS;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSVersion;

/**
 * UUID content manager
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class UUIDContentManager implements IContentManager
{
	public static final String KEY = "RUU";

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getPathByValue(I_DMS_Content content)
	{
		return null;
	}

	@Override
	public String getPathByValue(I_DMS_Version version)
	{
		return null;
	}

	@Override
	public String getPathByName(I_DMS_Content content)
	{
		return null;
	}

	@Override
	public String getContentName(	IFileStorageProvider storageProvider, String contentType, I_DMS_Content content, String fileName, String extention,
									String type, String operationType)
	{
		return null;
	}

	@Override
	public int createContent(	String name, String contentBaseType, String parentURL, String desc, File file, int contentTypeID, int asiID, boolean isMounting,
								String trxName)
	{
		return 0;
	}

	@Override
	public int createAssociation(int dms_Content_ID, int contentRelatedID, int validRecordID, int validTableID, int associationTypeID, String trxName)
	{
		return 0;
	}

	@Override
	public MDMSVersion createVersion(int contentID, String value, int seqNo, File file, String trxName)
	{
		return null;
	}

	@Override
	public MDMSContent createDirHierarchy(DMS dms, String dirPath, MDMSContent dirContent, int validTableID, int validRecordID, String trxName)
	{
		return null;
	}

	@Override
	public MDMSContent createDirectory(	DMS dms, String dirName, MDMSContent parentContent, int validTableID, int validRecordID, boolean errorIfDirExists,
										boolean isCreateAssociation, String trxName)
	{
		return null;
	}

	@Override
	public int addFile(	DMS dms, MDMSContent parentContent, File file, String fileName, String desc, int contentTypeID, int asiID, int AD_Table_ID,
						int Record_ID, boolean isVersion)
	{
		return 0;
	}

	@Override
	public int addFile(	DMS dms, String dirPath, File file, String fileName, String desc, String contentType, Map<String, String> attributeMap, int AD_Table_ID,
						int Record_ID, boolean isVersion)
	{
		return 0;
	}

	@Override
	public int createContentAssociationFileStoreAndThumnail(DMS dms, MDMSContent parentContent, File file, String fileName, String desc, int contentTypeID,
															int asiID, int AD_Table_ID, int Record_ID, boolean isVersion, String trxName)
	{
		return 0;
	}

	@Override
	public void writeFileOnStorageAndThumnail(DMS dms, File file, MDMSVersion version)
	{

	}

	@Override
	public void pasteCopyContent(DMS dms, MDMSContent copiedContent, MDMSContent destContent, int tableID, int recordID)
	{

	}

	@Override
	public void pasteCutContent(DMS dms, MDMSContent cutContent, MDMSContent destContent, int tableID, int recordID, boolean isDocExplorerWindow)
	{

	}

	@Override
	public void renameContent(DMS dms, String fileName, MDMSContent content)
	{

	}

	@Override
	public void renameContentOnly(DMS dms, MDMSContent content, String fileName, String description, boolean isDocExplorerWindow)
	{

	}

	@Override
	public void renameFile(DMS dms, MDMSContent content, String fileName, boolean isAddFileExtention)
	{

	}

	@Override
	public String createLink(DMS dms, MDMSContent contentParent, MDMSContent clipboardContent, boolean isDir, int tableID, int recordID)
	{
		return null;
	}

	@Override
	public String hasLinkableDocs(DMS dms, I_DMS_Content content, I_DMS_Association association)
	{
		return null;
	}

	@Override
	public boolean isHierarchyContentExists(int destContentID, int sourceContentID)
	{
		return false;
	}

	@Override
	public void deleteContent(DMS dms, MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs)
	{

	}

	@Override
	public void undoDeleteContent(DMS dms, MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs)
	{

	}

	@Override
	public int checkContentExists(String parentURL, String contentName, boolean isActiveOnly, boolean isCheckByContentName)
	{
		return 0;
	}

}
