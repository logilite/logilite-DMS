package com.logilite.dms.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.logilite.dms.DMS;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.MDMSContentType;

/**
 * DMS Utility Class for Easy Customization
 * 
 * @author Sachin Bhimani
 */
public final class DMSUtils
{

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
									String fileName, File file)
	{
		return addFileToDMS(clientID, tableID, recordID, contentType, attributeMap, null, fileName, null, file);
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
									String dirPath, String fileName, String description, File file)
	{
		String tableName = MTable.getTableName(Env.getCtx(), tableID);
		//
		DMS dms = new DMS(clientID, tableName);
		dms.initiateMountingContent(tableName, recordID, tableID);
		return dms.addFile(dirPath, file, fileName, description, contentType, attributeMap, tableID, recordID);
	} // addFileToDMS

	/**
	 * Fetch content files from the DMS with latest version file, if content has multiple versions
	 * 
	 * @param  clientID
	 * @param  tableID
	 * @param  recordID
	 * @param  contentType
	 * @param  isFetchFirstFileOnly
	 * @return                      List of Files
	 */
	public static ArrayList<File> getContentFilesFromDMS(int clientID, int tableID, int recordID, String contentType, boolean isFetchFirstFileOnly)
	{
		int ctID = 0;
		if (!Util.isEmpty(contentType, true))
			ctID = MDMSContentType.getContentTypeIDFromName(contentType, clientID);

		//
		String tableName = MTable.getTableName(Env.getCtx(), tableID);
		//
		DMS dms = new DMS(clientID, tableName);
		dms.initiateMountingContent(tableName, recordID, tableID);
		I_DMS_Content[] contents = dms.selectContent(tableID, recordID, 0, ctID);

		ArrayList<File> list = new ArrayList<File>();
		if (contents.length <= 0)
		{
			return list;
		}

		//
		if (isFetchFirstFileOnly)
		{
			list.add(dms.getFileFromStorageLatestVersionOnly(contents[0]));
		}
		else
		{
			for (I_DMS_Content content : contents)
			{
				list.add(dms.getFileFromStorageLatestVersionOnly(content));
			}
		}
		return list;
	} // getContentFilesFromDMS
}
