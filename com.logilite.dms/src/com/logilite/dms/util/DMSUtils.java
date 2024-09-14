package com.logilite.dms.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.logilite.dms.DMS;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSContentType;

/**
 * DMS Utility Class for Easy Customization
 * 
 * @author Sachin Bhimani
 */
public final class DMSUtils
{

	private static CLogger log = CLogger.getCLogger(DMSUtils.class);

	/**
	 * Add content to DMS
	 * 
	 * @param  clientID
	 * @param  tableID
	 * @param  recordID
	 * @param  contentType
	 * @param  attributeMap
	 * @param  fileName
	 * @param  file
	 * @return              Content ID
	 */
	public static int addFileToDMS(	int clientID, int tableID, int recordID, String contentType, HashMap<String, String> attributeMap,
									String fileName, File file, String trxName)
	{
		return addFileToDMS(clientID, tableID, recordID, contentType, attributeMap, null, fileName, null, file, trxName);
	} // addFileToDMS

	/**
	 * Add content to DMS
	 * 
	 * @param  clientID
	 * @param  tableID
	 * @param  recordID
	 * @param  contentType
	 * @param  attributeMap
	 * @param  dirPath
	 * @param  fileName
	 * @param  description
	 * @param  file
	 * @return              Content ID
	 */
	public static int addFileToDMS(	int clientID, int tableID, int recordID, String contentType, HashMap<String, String> attributeMap,
									String dirPath, String fileName, String description, File file, String trxName)
	{
		String tableName = MTable.getTableName(Env.getCtx(), tableID);
		//
		DMS dms = new DMS(clientID, tableName);
		dms.initiateMountingContent(tableName, recordID, tableID, trxName);
		return dms.addFile(dirPath, file, fileName, description, contentType, attributeMap, tableID, recordID, trxName);
	} // addFileToDMS

	/**
	 * Add file to DMS as Content, if same name/value content found then create Version
	 *
	 * @param  clientID
	 * @param  tableID
	 * @param  recordID
	 * @param  contentType
	 * @param  attributeMap
	 * @param  dirPath
	 * @param  fileName
	 * @param  description
	 * @param  file
	 * @param  trxName
	 * @return              Content ID
	 */
	public static int addFileToDMSOrVersionIfContentExists(	int clientID, int tableID, int recordID, String contentType, HashMap<String, String> attributeMap,
															String dirPath, String fileName, String description, File file, String trxName)
	{
		String tableName = MTable.getTableName(Env.getCtx(), tableID);
		//
		DMS dms = new DMS(clientID, tableName);
		dms.initiateMountingContent(tableName, recordID, tableID, trxName);

		// get the Root Content to check for the existing file
		MDMSContent parentContent = dms.getRootMountingContent(tableID, recordID, trxName);

		// Create Directory folder hierarchy OR get leaf DMS-Content
		if (!Util.isEmpty(dirPath, true) && !dirPath.equals(DMSConstant.FILE_SEPARATOR))
		{
			parentContent = dms.createDirHierarchy(	dirPath, parentContent, dms.getSubstituteTableInfo().getOriginTable_ID(),
													dms.getSubstituteTableInfo().getOriginRecord_ID(), trxName);
		}
		//
		String URL = dms.getPathFromContentManager(parentContent);

		log.log(Level.WARNING, "Fetch Parent Content : " + parentContent + ", URL=" + URL + " Trx=" + trxName);

		// check for file with Content name
		int contentID = RelationUtils.checkDMSContentExists(URL, fileName, true, true);
		// check for file with version name
		if (contentID <= 0)
			contentID = RelationUtils.checkDMSContentExists(URL, fileName, true, false);

		// if content with match name not found then add the file else update file version
		if (contentID > 0)
		{
			log.log(Level.WARNING, "Add Version, Content Exists contentID: " + contentID + ", Table=" + tableID + ", Record=" + recordID + ", Trx=" + trxName);
			MDMSContent prntContent = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(contentID, trxName);
			return dms.addFileVersion(prntContent, file, description, tableID, recordID, trxName);
		}
		else
		{
			log.log(Level.WARNING, "Add File, dirPath: "	+ dirPath + ", FileName=" + fileName + ", Table=" + tableID + ", Record=" + recordID + ", Trx="
									+ trxName);
			return dms.addFile(dirPath, file, fileName, description, contentType, attributeMap, tableID, recordID, trxName);
		}
	} // addFileToDMSOrVersionIfContentExists

	/**
	 * Create Link
	 * 
	 * @param  clientID
	 * @param  tableID
	 * @param  recordID
	 * @param  content
	 * @return
	 */
	public static String createLink(int clientID, int tableID, int recordID, MDMSContent content)
	{
		return createLink(clientID, tableID, recordID, content, null);
	} // createLink

	/**
	 * Create Link
	 * 
	 * @param  clientID
	 * @param  tableID
	 * @param  recordID
	 * @param  content
	 * @param  parentContent
	 * @return
	 */
	public static String createLink(int clientID, int tableID, int recordID, MDMSContent content, MDMSContent parentContent)
	{
		if (tableID > 0 && recordID > 0)
		{
			String tableName = MTable.getTableName(Env.getCtx(), tableID);
			//
			DMS dms = new DMS(clientID, tableName);
			dms.initiateMountingContent(tableName, recordID, tableID, content.get_TrxName());
			//
			if (parentContent == null)
				parentContent = dms.getRootMountingContent(tableID, recordID, content.get_TrxName());
			//
			boolean isLinkPresent = DMSOprUtils.isDocumentPresent(parentContent, content, false, content.get_TrxName());
			if (!isLinkPresent)
				return dms.createLink(parentContent, content, false, tableID, recordID, content.get_TrxName());
		}
		return null;
	} // createLink

	/**
	 * Remove Link
	 *
	 * @param clientID
	 * @param tableID
	 * @param recordID
	 * @param content
	 * @param trxName
	 */
	public static void removeLink(int clientID, int tableID, int recordID, int content_ID, String trxName)
	{
		if (tableID > 0 && recordID > 0)
		{
			String tableName = MTable.getTableName(Env.getCtx(), tableID);
			//
			DMS dms = new DMS(clientID, tableName);
			dms.initiateMountingContent(tableName, recordID, tableID, trxName);
			dms.removeLink("DMS_Content_ID = " + content_ID, recordID, tableID, trxName);
		}
	} // removeLink

	/**
	 * @param  clientID
	 * @param  tableID
	 * @param  recordID
	 * @param  contentType
	 * @param  trxName
	 * @return
	 */
	public static I_DMS_Content[] getContentFromDMS(int clientID, int tableID, int recordID, String contentType, String trxName)
	{
		int ctID = 0;
		if (!Util.isEmpty(contentType, true))
			ctID = MDMSContentType.getContentTypeIDFromName(contentType, clientID);

		//
		String tableName = MTable.getTableName(Env.getCtx(), tableID);
		//
		DMS dms = new DMS(clientID, tableName);
		dms.initiateMountingContent(tableName, recordID, tableID, trxName);
		I_DMS_Content[] contents = dms.selectContent(dms.getRootMountingContent(tableID, recordID, trxName), 0, ctID);

		return contents;
	}

	/**
	 * Fetch content files from the DMS with latest version file, if content has multiple versions
	 * 
	 * @param  clientID
	 * @param  tableID
	 * @param  recordID
	 * @param  contentType
	 * @param  isFetchFirstFileOnly - If True then fetch latest 1st file
	 * @param  trxName
	 * @return                      List of File(s)
	 */
	public static ArrayList<File> getContentFilesFromDMS(	int clientID, int tableID, int recordID, String contentType, boolean isFetchFirstFileOnly,
															String trxName)
	{
		HashMap<I_DMS_Content, File> map = getContentWithFilesFromDMS(clientID, tableID, recordID, contentType, isFetchFirstFileOnly, trxName);
		return new ArrayList<>(map.values());
	} // getContentFilesFromDMS

	/**
	 * Fetch map of content with files from the DMS with latest version against content
	 * 
	 * @param  clientID
	 * @param  tableID
	 * @param  recordID
	 * @param  contentType
	 * @param  isFetchFirstFileOnly - If True then fetch latest 1st file
	 * @param  trxName
	 * @return                      List of File(s)
	 */
	public static HashMap<I_DMS_Content, File> getContentWithFilesFromDMS(	int clientID, int tableID, int recordID, String contentType,
																			boolean isFetchFirstFileOnly, String trxName)
	{
		int ctID = 0;
		if (!Util.isEmpty(contentType, true))
			ctID = MDMSContentType.getContentTypeIDFromName(contentType, clientID);

		//
		String tableName = MTable.getTableName(Env.getCtx(), tableID);
		//
		DMS dms = new DMS(clientID, tableName);
		dms.initiateMountingContent(tableName, recordID, tableID, trxName);
		// TODO Need to check Trx based select the content
		I_DMS_Content[] contents = dms.selectContent(dms.getRootMountingContent(tableID, recordID, trxName), 0, ctID);

		HashMap<I_DMS_Content, File> map = new HashMap<I_DMS_Content, File>();
		if (contents.length <= 0)
		{
			return map;
		}

		//
		if (isFetchFirstFileOnly)
		{
			map.put(contents[0], dms.getFileFromStorageLatestVersionOnly(contents[0]));
		}
		else
		{
			for (I_DMS_Content content : contents)
			{
				map.put(content, dms.getFileFromStorageLatestVersionOnly(content));
			}
		}
		return map;
	} // getContentWithFilesFromDMS
}
