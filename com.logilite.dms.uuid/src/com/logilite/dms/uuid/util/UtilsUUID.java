package com.logilite.dms.uuid.util;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.idempiere.dms.util.Utils;
import org.idempiere.model.MDMSVersion;

/**
 * Miscellaneous Utils
 * 
 * @author Sachin Bhimani
 */
public class UtilsUUID
{

	/**
	 * Create DMS VersionUU
	 * 
	 * @param  contentID
	 * @param  value
	 * @param  seqNo
	 * @param  file
	 * @return           DMS_VERSION_ID
	 */

	public static MDMSVersion createUU(int contentID, String value, int seqNo, File file, String trxName)
	{
		String uu = UUID.randomUUID().toString();
		MDMSVersion version = (MDMSVersion) MTable.get(Env.getCtx(), MDMSVersion.Table_ID).getPO(0, trxName);
		version.setDMS_Version_UU(uu);
		version.setDMS_Content_ID(contentID);
		version.setValue(uu);
		version.setSeqNo(seqNo);
		if (file != null)
			version.setDMS_FileSize(Utils.readableFileSize(FileUtils.sizeOf(file)));
		version.saveEx();
		System.out.println("old uu " + uu + " Vuu " + version.getDMS_Version_UU());
		// log.log(Level.INFO, "created new DMS_Version with UU :" + version.getDMS_Version_ID() + "
		// for " + value);
		return version;
	}
}
