package com.logilite.dms.uuid.process;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
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

			ArrayList<String> cmdList = new ArrayList<String>();
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				int count = 0;
				int noOfRecords = DB.getSQLValue(get_TrxName(), DMSContantUUID.SQL_COUNT_VERSION, getAD_Client_ID());

				pstmt = DB.prepareStatement(DMSContantUUID.SQL_OLD_NEW_PATH, get_TrxName());
				pstmt.setInt(1, getAD_Client_ID());
				pstmt.setInt(2, getAD_Client_ID());
				rs = pstmt.executeQuery();
				while (rs.next())
				{
					String cmd = rs.getString("Command")
									+ " \"" + baseDir + rs.getString("OldURL") + "\""
									+ " \"" + baseDir + rs.getString("NewURL") + "\" ";
					cmdList.add(cmd);

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
			writeToFile(cmdList);
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

	void writeToFile(List<String> cmdList)
	{
		int seqNo = 10;
		int count = 0;
		ArrayList<String> list = new ArrayList<String>();

		for (String item : cmdList)
		{
			count++;
			list.add(item);
			if (count % p_NoOfRecordsExportPerFile == 0)
			{
				createFile(seqNo, list);
				list = new ArrayList<String>();
				seqNo += 10;
			}
		}

		if (count % p_NoOfRecordsExportPerFile != 0)
		{
			createFile(seqNo, list);
		}
	} // writeToFile

	private void createFile(int seqNo, List<String> content)
	{
		String fileName = "CNV_DMS_UUID_STR_" + DMSConstant.SDF_NO_SPACE.format(new Date()) + "_" + seqNo + ".txt";
		File fileDownload = null;
		try
		{
			fileDownload = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);

			if (!fileDownload.createNewFile())
			{
				addLog("Failed to create temporary file with name:" + fileName);
			}

			// append a list of lines, add new lines automatically
			Files.write(fileDownload.toPath(), content, StandardOpenOption.APPEND);
		}
		catch (IOException e)
		{
			addLog("Failed to write command into file for seqNo:" + seqNo + "; " + e.getLocalizedMessage());
			log.log(Level.SEVERE, "Error while generating temporary file for exporting commands for renaming normal structure to UUID based structure, "
									+ e.getLocalizedMessage());
		}

		addLog("File created: " + fileName + ", with " + content.size() + " command lines");

		//
		processUI.download(fileDownload);

	} // createFile

}
