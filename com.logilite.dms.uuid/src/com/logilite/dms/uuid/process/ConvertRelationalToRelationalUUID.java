package com.logilite.dms.uuid.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.Adempiere;
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
import com.logilite.dms.uuid.util.UtilsUUID;

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
 *  - Click on this process:'Configure Archive Store's Method to DMSUU' and mark this flag as TRUE to store Archive documents as UUID based structure.
 *  - Do cache reset and check to upload new content and its version value is UUID based created
 * </pre>
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class ConvertRelationalToRelationalUUID extends SvrProcess
{

	private boolean			p_ClientInfoSetContentTypeToUUID	= false;
	private boolean			p_ExportCmdFileForChangeToUUID		= false;
	private boolean			p_ExportWithBaseDirPath				= true;
	private boolean			p_IsModifyContentParentURL			= false;
	private boolean			p_IsExecuteShellCommandDirectly		= false;
	private boolean			p_ArchiveStoreSetMethodToDMSUU		= false;

	private int				p_NoOfRecordsExportPerFile			= 100000;
	private int				p_NoOfThreadInPool					= 1;

	private ArrayList<File>	fileList							= new ArrayList<File>();
	private ArrayList<File>	fileListOutput						= new ArrayList<File>();
	private CountDownLatch	latch;

	@Override
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if ("ExportCmdFileForChangeToUUID".equals(name))
				p_ExportCmdFileForChangeToUUID = para[i].getParameterAsBoolean();
			else if ("ExportWithBaseDirPath".equals(name))
				p_ExportWithBaseDirPath = para[i].getParameterAsBoolean();
			else if ("NoOfRecordsExportPerFile".equals(name))
				p_NoOfRecordsExportPerFile = para[i].getParameterAsInt();
			else if ("IsExecuteShellCommandDirectly".equals(name))
				p_IsExecuteShellCommandDirectly = para[i].getParameterAsBoolean();
			else if ("NoOfThreadInPool".equals(name))
				p_NoOfThreadInPool = para[i].getParameterAsInt();
			else if ("IsModifyContentParentURL".equals(name))
				p_IsModifyContentParentURL = para[i].getParameterAsBoolean();
			else if ("ClientInfoSetContentTypeToUUID".equals(name))
				p_ClientInfoSetContentTypeToUUID = para[i].getParameterAsBoolean();
			else if ("ArchiveStoreSetMethodToDMSUU".equals(name))
				p_ArchiveStoreSetMethodToDMSUU = para[i].getParameterAsBoolean();
		}
	}

	/**
	 *
	 */
	@Override
	protected String doIt() throws Exception
	{
		/**
		 * Step 1
		 * Export command file to change the name to UUID based structure
		 */
		if (p_ExportCmdFileForChangeToUUID)
		{
			String filePrefix = "DMS_UUID_" + DMSConstant.SDF_NO_SPACE.format(new Date());

			DMS dms = new DMS(getAD_Client_ID());
			String baseDir = (p_ExportWithBaseDirPath ? dms.getFileStorageProvider().getBaseDirectory(null) : "");

			HashMap<String, ArrayList<String>> mapCMDList = new HashMap<String, ArrayList<String>>(100000);
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				statusUpdate("Preparing data for exporting command files each table wise + No of records limit");

				String key = "";
				int count = 0;
				int noOfRecords = DB.getSQLValue(get_TrxName(), DMSContantUUID.SQL_COUNT_VERSION, getAD_Client_ID());

				pstmt = DB.prepareStatement(DMSContantUUID.SQL_OLD_NEW_PATH, get_TrxName());
				pstmt.setInt(1, getAD_Client_ID());
				pstmt.setString(2, DMSConstant.FILE_SEPARATOR);
				pstmt.setInt(3, getAD_Client_ID());
				pstmt.setString(4, DMSConstant.FILE_SEPARATOR);
				pstmt.setString(5, DMSConstant.FILE_SEPARATOR);
				rs = pstmt.executeQuery();

				statusUpdate("Going to prepare command list file ");

				while (rs.next())
				{
					int tableID = rs.getInt("AD_Table_ID");
					int recordID = rs.getInt("Record_ID");

					String cmd = (Adempiere.getOSInfo().startsWith("Windows") ? "move " : "mv -v ")
									+ " \"" + baseDir + rs.getString("OldURL") + "\" "
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
						statusUpdate("UUID based path created " + count + " out of " + noOfRecords + " ( Approx )");
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

			File file = new File(System.getProperty("java.io.tmpdir") + File.separator + filePrefix);
			if (!file.exists())
			{
				file.mkdir();
			}

			// Write data to the file
			writeToFile(filePrefix, mapCMDList);

			// Script file download
			downloadableZip(filePrefix, file);

			//
			if (p_IsExecuteShellCommandDirectly)
			{
				String fileOutPrefix = filePrefix + "_Out";
				File fileOutputDir = new File(System.getProperty("java.io.tmpdir") + File.separator + fileOutPrefix);
				if (!fileOutputDir.exists())
				{
					fileOutputDir.mkdir();
				}

				//
				latch = new CountDownLatch(fileList.size());
				ExecutorService executor = Executors.newFixedThreadPool(p_NoOfThreadInPool);

				for (File file1 : fileList)
				{
					Runnable worker = new CommandExeThread(file1, fileOutPrefix, this);
					executor.execute(worker);
				}

				try
				{
					latch.await();
				}
				catch (InterruptedException e)
				{
					log.saveError("Error- ", e.getMessage());
					throw new AdempiereException(e);
				}
				executor.shutdown();

				//
				downloadableZip(fileOutPrefix, fileOutputDir);
			}
		}

		/**
		 * Step 2
		 * Modify content parent URL
		 */
		if (p_IsModifyContentParentURL)
		{
			statusUpdate("Updating the content parent url");

			int no = DB.executeUpdateEx(DMSContantUUID.SQL_MODIFY_CONTENT_PARENTURL,
										new Object[] {
														getAD_Client_ID(),
															DMSConstant.FILE_SEPARATOR,
															getAD_Client_ID(),
															DMSConstant.FILE_SEPARATOR,
															DMSConstant.FILE_SEPARATOR
										}, get_TrxName());

			addLog("#" + no + " content parent URL updated.");
			log.log(Level.INFO, "#" + no + " content parent URL updated.");
		}

		/**
		 * Step 3
		 * Change Content manager type in Client info window and Archive Store Method to DMSUU
		 */
		if (p_ClientInfoSetContentTypeToUUID)
		{
			int no = DB.executeUpdateEx("UPDATE AD_ClientInfo SET DMS_ContentManagerType = ? WHERE AD_Client_ID = ? AND IsActive = 'Y' ",
										new Object[] { UUIDContentManager.KEY, getAD_Client_ID() }, get_TrxName());

			addLog(	0, null, null, "Updated to client info window for configure UUID based Relation type for the DMS, " + no,
					MClient.Table_ID, getAD_Client_ID());
		}

		if (p_ArchiveStoreSetMethodToDMSUU)
		{
			String methodName = DB.getSQLValueString(null, DMSContantUUID.SQL_GET_ARCHIVE_STORAGE_METHOD, getAD_Client_ID());

			if (DMSContantUUID.ARCHIVE_STORAGE_METHOD_DMS.equals(methodName))
			{
				int no = DB.executeUpdateEx("UPDATE AD_StorageProvider SET Method = ? FROM AD_ClientInfo c					   			 						"
											+ " INNER JOIN AD_StorageProvider sp ON ( sp.AD_StorageProvider_ID = c.StorageArchive_ID AND sp.AD_Client_ID = ? )	"
											+ " WHERE  AD_StorageProvider.AD_StorageProvider_ID = c.StorageArchive_ID  											",
											new Object[] { DMSContantUUID.ARCHIVE_STORAGE_METHOD_DMSUU, getAD_Client_ID() }, get_TrxName());

				addLog(0, null, null, "Updated Archive Storage Provider to configure Method to DMSUU , " + no);
			}
		}
		return "";
	} // doIt

	void writeToFile(String filePrefix, HashMap<String, ArrayList<String>> mapCMDList)
	{
		int seqNo = 10;
		int count = 0;
		int resetTableID = -1;

		ArrayList<String> masterListPerFile = new ArrayList<String>();
		boolean requireToSplitFile = false;
		Object[] keys = mapCMDList.keySet().toArray();
		Arrays.sort(keys);

		for (Object objKey : keys)
		{
			String key = (String) objKey;
			int tableID = Integer.parseInt(key.split("_")[0]);
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

		String fileName = filePrefix + "_" + seqNo + "_" + tablename + (Env.isWindows() ? ".bat" : ".sh");
		File fileDownload = null;
		try
		{
			fileDownload = new File(System.getProperty("java.io.tmpdir") + File.separator + filePrefix + File.separator + fileName);
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

		fileList.add(fileDownload);
	} // createFile

	private void downloadableZip(String filePrefix, File file) throws Exception
	{
		String headDirName = filePrefix + "_";
		File srcFolder = file;
		if (!srcFolder.exists())
		{
			srcFolder.mkdir();
		}

		File destZipFile = null;
		try
		{
			destZipFile = File.createTempFile(headDirName, ".zip");
			destZipFile.createNewFile();
		}
		catch (Throwable e)
		{
			throw new AdempiereException("Unable to create temp file", e);
		}
		destZipFile.delete();

		UtilsUUID.zipFolder(Paths.get(srcFolder.getAbsolutePath()), Paths.get(destZipFile.getAbsolutePath()));

		processUI.download(destZipFile);
		srcFolder.deleteOnExit();
	} // downloadableZip

	/**
	 * to concurrent execution shell command
	 * 
	 * @author Sachin Bhimani
	 */
	class CommandExeThread implements Runnable
	{
		File								file;
		String								filePrefix;
		ConvertRelationalToRelationalUUID	crtruProcess;

		public CommandExeThread(File file, String filePrefix, ConvertRelationalToRelationalUUID process)
		{
			this.file = file;
			this.filePrefix = filePrefix;
			this.crtruProcess = process;
		}

		@Override
		public void run()
		{
			StringBuilder output = new StringBuilder();
			Date startTime = new Date();
			output.append("\nStartTime: ").append(DMSConstant.SDF_WITH_TIME.format(startTime));
			output.append("\nFile: ").append(file.getAbsolutePath()).append("\n\n");

			try
			{
				ProcessBuilder processBuilder = new ProcessBuilder();

				// -- Linux --

				// Run a shell command
				// processBuilder.command("bash", "-c", "ls /home/SB/");

				// Run a shell script
				// processBuilder.command("path/to/hello.sh");

				// -- Windows --
				// Run a command
				// processBuilder.command("cmd.exe", "/c", "dir C:\\Users\\SB");

				// Run a bat file
				// processBuilder.command("C:\\Users\\SB\\hello.bat");

				processBuilder.command(file.getAbsolutePath());
				Process process = processBuilder.start();

				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String line;
				while ((line = reader.readLine()) != null)
				{
					output.append(line + "\n");
				}

				int exitVal = process.waitFor();
				if (exitVal == 0)
				{
					output.append("\nSuccess! \n");
				}
				else
				{
					output.append("\nAbnormal! \n");
				}
			}
			catch (IOException e)
			{
				output.append("\n\n\nIOException:\n");
				output.append(e.getLocalizedMessage() + "\n\n" + e);
			}
			catch (InterruptedException e)
			{
				output.append("\n\n\nInterruptedException:\n");
				output.append(e.getLocalizedMessage() + "\n\n" + e);
			}
			finally
			{
				crtruProcess.latch.countDown();
			}

			Date endTime = new Date();
			output.append("\n\nEndTime: ").append(DMSConstant.SDF_WITH_TIME.format(endTime));
			output.append("\nDifference in Millis: " + (endTime.getTime() - startTime.getTime()));

			// Output file create
			try
			{
				String fileName = file.getName() + "_" + Thread.currentThread().getId() + ".out";
				File outFile = new File(System.getProperty("java.io.tmpdir") + File.separator + filePrefix + File.separator + fileName);
				if (!outFile.createNewFile())
				{
					addLog("Failed to create temporary file with name:" + fileName);
				}

				// append a list of lines, add new lines automatically
				Files.writeString(outFile.toPath(), output, StandardOpenOption.APPEND);

				//
				fileListOutput.add(outFile);
			}
			catch (IOException e)
			{
				log.log(Level.SEVERE, "Failed to write output file of the shell command execution result. error:" + e.getLocalizedMessage());
			}
		} // run
	} // Class CommandExeThread
}
