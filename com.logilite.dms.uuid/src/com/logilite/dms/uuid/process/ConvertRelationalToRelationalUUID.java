package com.logilite.dms.uuid.process;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;

import com.logilite.dms.uuid.classes.UUIDContentManager;
import com.logilite.dms.uuid.contant.DMSContantUUID;

/**
 * Process for converting relational DMS data structure to relational UUID structure
 * 
 * <pre>
 * 	This process will help to migrate existing Relational based 
 * 	DMS content hierarchy naming structure convert into UUID based 
 * 	naming into a physical storage
 * </pre>
 * 
 * Steps to follow:
 * 
 * <pre>
 *  - Make application run, and active only org.idempiere.DMS plug-in only, other DMS plug-in should be down will be better
 *  - Export command file, do not configure Client Info Content manager type
 *  - Verify exported script and apply manually script in terminal where DMS storage used
 *  - Once script applied then it's converted from normal directory & content to UUID based name
 *  - Again execute this process and 'Client Info configure Content Type as Relational UUID' mark this flag as TRUE
 *  - Do cache reset and check to upload new content and its version value is UUID based created
 * </pre>
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class ConvertRelationalToRelationalUUID extends SvrProcess
{

	private boolean	p_ClientInfoSetContentTypeToUUID	= false;
	private boolean	p_ExportCmdFileForChangeToUUID		= false;
	private boolean	p_ExportWithBaseDirPath				= false;

	private int		p_NoOfRecordsExportPerFile			= 100000;

	@Override
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if ("ClientInfoSetContentTypeToUUID".equals(name))
				p_ClientInfoSetContentTypeToUUID = para[i].getParameterAsBoolean();
			else if ("ExportCmdFileForChangeToUUID".equals(name))
				p_ExportCmdFileForChangeToUUID = para[i].getParameterAsBoolean();
			else if ("ExportWithBaseDirPath".equals(name))
				p_ExportWithBaseDirPath = para[i].getParameterAsBoolean();
			else if ("NoOfRecordsExportPerFile".equals(name))
				p_NoOfRecordsExportPerFile = para[i].getParameterAsInt();
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		/**
		 * Export command file to change the name to UUID based structure
		 */
		if (p_ExportCmdFileForChangeToUUID)
		{
			DMS dms = new DMS(getAD_Client_ID());
			String baseDir = (p_ExportWithBaseDirPath ? dms.getFileStorageProvider().getBaseDirectory(null) : "");

			HashMap<String, ArrayList<String>> mapCMDList = new HashMap<String, ArrayList<String>>(100000);
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				String key = "";
				int count = 0;
				int noOfRecords = DB.getSQLValue(get_TrxName(), DMSContantUUID.SQL_COUNT_VERSION, getAD_Client_ID());

				pstmt = DB.prepareStatement(DMSContantUUID.SQL_OLD_NEW_PATH, get_TrxName());
				pstmt.setInt(1, getAD_Client_ID());
				pstmt.setInt(2, getAD_Client_ID());
				rs = pstmt.executeQuery();
				while (rs.next())
				{
					int tableID = rs.getInt("AD_Table_ID");
					int recordID = rs.getInt("Record_ID");

					String cmd = rs.getString("Command")
									+ " \"" + baseDir + rs.getString("OldURL") + "\""
									+ " \"" + baseDir + rs.getString("NewURL") + "\" ";
					key = tableID + "_" + recordID;
					if (mapCMDList.containsKey(key))
					{
						mapCMDList.get(key).add(cmd);
					}
					else
					{
						ArrayList<String> value = new ArrayList<String>();
						value.add(cmd);
						mapCMDList.put(key, value);
					}

					count++;

					if (count % 1000 == 0)
						statusUpdate("UUID based path created " + count + " out of " + noOfRecords);
				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, "Fail to fetching tree hierarchy for UUID based structure : " + e.getLocalizedMessage(), e);
				throw new AdempiereException("Fail to fetching tree hierarchy for UUID based structure : " + e.getLocalizedMessage(), e);
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}

			//
			writeToFile(mapCMDList);
		}

		/**
		 * Change Content manager type in Client info window
		 */
		if (p_ClientInfoSetContentTypeToUUID)
		{
			int no = DB.executeUpdateEx("UPDATE AD_ClientInfo SET DMS_ContentManagerType = ? WHERE AD_Client_ID = ? AND IsActive = 'Y' ",
										new Object[] { UUIDContentManager.KEY, getAD_Client_ID() }, get_TrxName());

			addLog(	0, null, null, "Updated to client info window for configure UUID based Relation type for the DMS, " + no,
					MClient.Table_ID, getAD_Client_ID());
		}

		return "";
	} // doIt

	void writeToFile(HashMap<String, ArrayList<String>> mapCMDList)
	{
		int seqNo = 10;
		int count = 0;
		int resetTableID = -1;
		String filePrefix = "DMSUUID_" + DMSConstant.SDF_NO_SPACE.format(new Date());

		ArrayList<String> masterListPerFile = new ArrayList<String>();
		boolean requireToSplitFile = false;
		Object[] keys = mapCMDList.keySet().toArray();
		Arrays.sort(keys);
		for (Object objKey : keys)
		{
			String key = (String) objKey;
			int tableID = Integer.parseInt(key.split("_")[0]);
			// int recordID = Integer.parseInt(key.split("_")[1]);
			if (resetTableID != -1 && resetTableID != tableID || requireToSplitFile)
			{
				createFile(resetTableID, masterListPerFile, seqNo, count, filePrefix);
				//
				masterListPerFile = new ArrayList<String>();
				resetTableID = tableID;
				count = 0;
				requireToSplitFile = false;
				seqNo += 10;
			}
			else if (resetTableID == -1)
			{
				resetTableID = tableID;
			}

			ArrayList<String> cmdList = mapCMDList.get(key);
			masterListPerFile.addAll(cmdList);
			count += cmdList.size();
			System.out.println(key + "  " + cmdList.size());

			if (count / p_NoOfRecordsExportPerFile >= 1)
			{
				requireToSplitFile = true;
			}
		}

		if (count > 0)
		{
			createFile(resetTableID, masterListPerFile, seqNo, count, filePrefix);
		}
	} // writeToFile

	private void createFile(int tableID, ArrayList<String> masterListPerFile, int seqNo, int count, String filePrefix)
	{
		String tablename = "Root";
		if (tableID > 0)
			tablename = MTable.get(getCtx(), tableID).getTableName();

		String fileName = filePrefix + "_" + seqNo + "_" + tablename + (Env.isWindows() ? ".txt" : ".sh");
		File fileDownload = null;
		try
		{
			fileDownload = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
			if (!fileDownload.createNewFile())
			{
				addLog("Failed to create temporary file with name:" + fileName);
			}

			// append a list of lines, add new lines automatically
			Files.write(fileDownload.toPath(), masterListPerFile, StandardOpenOption.APPEND);
		}
		catch (IOException e)
		{
			addLog("Failed to write command into file for seqNo:" + seqNo + "; " + e.getLocalizedMessage());
			log.log(Level.SEVERE, "Error while generating temporary file for exporting commands for renaming normal structure to UUID based structure, "
									+ e.getLocalizedMessage());
		}

		addLog("File created: " + fileName + ", with " + masterListPerFile.size() + " command lines");

		//
		processUI.download(fileDownload);

	} // createFile

}
