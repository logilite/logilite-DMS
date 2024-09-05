package com.logilite.dms.model;

import java.io.File;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import com.logilite.dms.util.Utils;

public class MDMSVersion extends X_DMS_Version
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -8745235975268643483L;

	private static CLogger		log					= CLogger.getCLogger(MDMSVersion.class);

	public MDMSVersion(Properties ctx, int DMS_Version_ID, String trxName)
	{
		super(ctx, DMS_Version_ID, trxName);
	}

	public MDMSVersion(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer("DMS Version[#").append(get_ID()).append(" - ").append(getSeqNo()).append(" - ").append(getValue()).append("]");
		return sb.toString();
	}

	/**
	 * Create DMS Content
	 * 
	 * @param  contentID
	 * @param  value
	 * @param  seqNo
	 * @param  file
	 * @return           DMS_VERSION_ID
	 */
	public static MDMSVersion create(int contentID, String value, int seqNo, File file, String trxName)
	{
		MDMSVersion version = (MDMSVersion) MTable.get(Env.getCtx(), MDMSVersion.Table_ID).getPO(0, trxName);
		version.setDMS_Content_ID(contentID);
		version.setValue(value);
		version.setSeqNo(seqNo);
		if (file != null)
			version.setDMS_FileSize(Utils.readableFileSize(FileUtils.sizeOf(file)));
		version.saveEx();

		log.log(Level.INFO, "created new DMS_Version:" + version.getDMS_Version_ID() + " for " + value);
		return version;
	} // create

	/**
	 * Get Version History of content
	 * 
	 * @param  content
	 * @return
	 */
	public static List<MDMSVersion> getVersionHistory(I_DMS_Content content)
	{
		if (content == null)
			return null;

		Query query = new Query(Env.getCtx(), MDMSVersion.Table_Name, "DMS_Content_ID=?", null);
		query.setParameters(content.getDMS_Content_ID());
		query.setClient_ID();
		query.setOnlyActiveRecords(content.isActive());
		query.setOrderBy(COLUMNNAME_SeqNo);

		List<MDMSVersion> versionList = query.list();
		return versionList;
	} // getVersionHistory

	/**
	 * Get latest version
	 * 
	 * @param  content
	 * @return
	 */
	public static I_DMS_Version getLatestVersion(I_DMS_Content content)
	{
		return getLatestVersion(content, true, -1);
	} // getLatestVersion

	public static I_DMS_Version getLatestVersion(I_DMS_Content content, boolean isActiveOnly)
	{
		return getLatestVersion(content, isActiveOnly, -1);
	} // getLatestVersion

	/**
	 * Get latest version or specified seqno's version
	 * 
	 * @param  content
	 * @param  isActiveOnly
	 * @param  seqNo
	 * @return
	 */
	public static I_DMS_Version getLatestVersion(I_DMS_Content content, boolean isActiveOnly, int seqNo)
	{
		if (content == null)
			return null;

		Query query = new Query(Env.getCtx(), Table_Name, " DMS_Content_ID=? " + (seqNo >= 0 ? " AND SeqNo=? " : ""), ((PO) content).get_TrxName());
		if (seqNo >= 0)
			query.setParameters(content.getDMS_Content_ID(), seqNo);
		else
			query.setParameters(content.getDMS_Content_ID());
		query.setOnlyActiveRecords(isActiveOnly);
		query.setClient_ID();
		query.setOrderBy("SeqNo DESC");
		return (MDMSVersion) query.first();
	} // getLatestVersion
}
