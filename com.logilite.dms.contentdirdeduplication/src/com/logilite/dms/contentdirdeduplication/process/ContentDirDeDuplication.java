package com.logilite.dms.contentdirdeduplication.process;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Trx;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSAssociationType;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.util.Utils;

/**
 * Process to check and verify the duplicate directory mounting content and merge it into the single
 * one
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 * @author Nikhil
 */
public class ContentDirDeDuplication extends SvrProcess
{

	// List data DMS Level For
	public static final String	REFERENCE_LIST_ROOT_LEVEL_DIRECTORY			= "RLD";
	public static final String	REFERENCE_LIST_TABLE_MOUNTING_DIRECTORY		= "TMD";
	public static final String	REFERENCE_LIST_RECORD_MOUNTING_DIRECTORY	= "RMD";

	public static final String	PARENT_URL_FILTER							= "#PARENT_URL_FILTER#";

	// SQL
	public static final String	SQL_DUPLICATE_DIR_SQL_BASE					= "SELECT 	ParentURL, Name, COUNT(DISTINCT c.DMS_Content_ID) AS content_count, 															"
																				+ " 	ARRAY_AGG(c.DMS_Content_ID || ' - ' || NVL(cd.child_count, 0) || ' - ' || c.IsActive || ' - ' || c.Created						"
																				+ "										ORDER BY c.IsActive DESC, COALESCE(cd.child_count, 0) DESC, c.Created) AS duplicateContentInfo,	"
																				+ " 	ARRAY_AGG(c.DMS_Content_ID 		ORDER BY c.IsActive DESC, COALESCE(cd.child_count, 0) DESC, c.Created) AS DMS_Content_IDs,		"
																				+ " 	ARRAY_AGG(NVL(cd.child_count,0) ORDER BY c.IsActive DESC, COALESCE(cd.child_count, 0) DESC, c.Created) AS child_counts,			"
																				+ " 	ARRAY_AGG(c.IsActive 			ORDER BY c.IsActive DESC, COALESCE(cd.child_count, 0) DESC, c.Created) AS IsActives				"
																				+ " FROM DMS_Content c 																				"
																				+ "	LEFT JOIN ( SELECT DMS_Content_Related_ID, COALESCE(COUNT(DMS_Association_ID), 0) AS child_count"
																				+ " 			FROM DMS_Association 																"
																				+ "				WHERE NVL(DMS_AssociationType_ID, 1000001) = 1000001 								"
																				+ "				GROUP BY DMS_Content_Related_ID 													"
																				+ "			) cd	ON (cd.DMS_Content_Related_ID = c.DMS_Content_ID)								"
																				+ " WHERE ContentBaseType = 'DIR' AND AD_Client_ID = ? AND #PARENT_URL_FILTER#						"
																				+ " GROUP BY ParentURL, Name 																		"
																				+ "	HAVING COUNT(1) > 1 																			"
																				+ "	ORDER BY COUNT(1) DESC, ParentURL, Name															";

	public static final String	SQL_CREATE_DMS_TEMPORARY_DATA_TABLE			= "CREATE TABLE IF NOT EXISTS T_DMS_Duplicate_Clearance "
																				+ " (PInstanceID integer, ParentURL character varying, OriginalContentID numeric, DeletableContentID numeric, DeletableContentChildCount bigint)";

	public static final String	SQL_GET_DATA_FROM_TEMPORARY_TABLE			= "SELECT PInstanceID, ParentURL, OriginalContentID, DeletableContentID, DeletableContentChildCount "
																				+ "	FROM T_DMS_Duplicate_Clearance 																"
																				+ " WHERE PInstanceID = ? AND OriginalContentID <> DeletableContentID 							"
																				+ " ORDER BY OriginalContentID, DeletableContentChildCount DESC									";

	public static final String	SQL_CONTENT_CHILD_ASSOCIATION_RECORDS		= "SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_Related_ID = ?";

	// Process Params
	public String				p_DMS_Level_For								= "";
	public boolean				p_isGenerateReportOnly						= false;
	public boolean				p_isMoveDuplicateChildContentToCorrectOne	= false;
	public boolean				p_isDeleteDuplicateContent					= false;
	public boolean				p_isInActiveContentAndSetDescription		= false;

	// Counters
	private int					netChildAssnMoved							= 0;
	private int					netContentProcessed							= 0;

	@Override
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("DMSLevelFor"))
				p_DMS_Level_For = para[i].getParameterAsString();
			else if (name.equals("IsGenerateReportOnly"))
				p_isGenerateReportOnly = para[i].getParameterAsBoolean();
			else if (name.equals("IsMoveDuplicateChildContentToCorrectOne"))
				p_isMoveDuplicateChildContentToCorrectOne = para[i].getParameterAsBoolean();
			else if (name.equals("IsDeleteDuplicateContent"))
				p_isDeleteDuplicateContent = para[i].getParameterAsBoolean();
			else if (name.equals("IsInActiveContentAndSetDescription"))
				p_isInActiveContentAndSetDescription = para[i].getParameterAsBoolean();
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		long starttime = System.currentTimeMillis();
		addLog(0, new Timestamp(starttime), null, "Process Started: DMS Content Directory DeDuplication", getTable_ID(), getRecord_ID());

		//
		correctContentDirDeDuplication();

		//
		if (!p_isGenerateReportOnly)
		{
			addLog("Duplicate content records " + (p_isDeleteDuplicateContent ? "deleted: " : "updated: ") + netContentProcessed);
			addLog("Duplicate content > child records updated: " + netChildAssnMoved);
		}

		addLog("Total Execution Time = " + getTimeDiff(starttime) + " seconds");
		addLog(0, new Timestamp(System.currentTimeMillis()), null, "Process End : DMS Content Directory DeDuplication", getTable_ID(), getRecord_ID());
		return "@OK@";
	} // doIt

	/**
	 * 
	 */
	private void correctContentDirDeDuplication()
	{
		if (!validateAction())
			return;

		// create custom trx
		String trxName = Trx.createTrxName("CntDirDeDupl_");
		Trx customTrx = Trx.get(trxName, true);
		try
		{
			String sql = "";
			if (REFERENCE_LIST_ROOT_LEVEL_DIRECTORY.equals(p_DMS_Level_For))
			{
				sql = SQL_DUPLICATE_DIR_SQL_BASE.replace(PARENT_URL_FILTER, "ParentURL IS NULL");
			}
			else
			{
				String mountingBasePath = DMSConstant.FILE_SEPARATOR + Utils.getDMSMountingBase(getAD_Client_ID());
				if (REFERENCE_LIST_TABLE_MOUNTING_DIRECTORY.equals(p_DMS_Level_For))
				{
					// EX= "ParentURL='/Attachment'"
					sql = SQL_DUPLICATE_DIR_SQL_BASE.replace(PARENT_URL_FILTER, "IsMounting = 'Y' AND ParentURL = '" + mountingBasePath + "'");
				}
				else
				{
					// EX= " ParentURL IS NOT NULL AND ParentURL!='/Attachment' "
					sql = SQL_DUPLICATE_DIR_SQL_BASE.replace(PARENT_URL_FILTER, "IsMounting = 'Y' AND ParentURL IS NOT NULL AND ParentURL != '"
																				+ mountingBasePath + "'");
				}
			}

			statusUpdate("Inserting data into temporary table...");

			// insert data into table and return if insert count is 0
			boolean isRecordFound = insertDataIntoTemporaryTable(sql, trxName);
			if (!isRecordFound)
				return;

			if (p_isGenerateReportOnly)
			{
				statusUpdate("Exporting data in the CSV");
				exportDataIntoCSV(trxName);
			}
			else
			{
				statusUpdate("Starting the duplicate data correction for DMS");
				correction(customTrx);
			}
		}
		catch (Exception e)
		{
			String msg = "Error while processing: " + e.getLocalizedMessage();
			addLog(0, new Timestamp(System.currentTimeMillis()), null, msg, getTable_ID(), getRecord_ID());
			log.log(Level.SEVERE, msg, e);
			throw e;
		}
		finally
		{
			customTrx.commit();
			customTrx.close();
		}
	} // correctContentDirDeDuplication

	/**
	 * Method to perform duplicate directory content. it will be move the
	 * duplicate content, delete content or mark content as inactive
	 * 
	 * @param trx
	 */
	private void correction(Trx trx)
	{
		List<List<Object>> duplicateDirDataList = DB.getSQLArrayObjectsEx(trx.getTrxName(), SQL_GET_DATA_FROM_TEMPORARY_TABLE, getAD_PInstance_ID());
		int pendingCount = duplicateDirDataList.size();

		String info = pendingCount + " records found for duplicate correction.";
		addLog(info);
		log.log(Level.WARNING, info);

		for (List<Object> duplicateDirData : duplicateDirDataList)
		{
			int OriginalContentID = ((BigDecimal) duplicateDirData.get(2)).intValue();
			int deletableContentID = ((BigDecimal) duplicateDirData.get(3)).intValue();

			if (OriginalContentID <= 0 || deletableContentID <= 0)
			{
				addLog("No valid record found for correction");
				continue;
			}

			// Actual and Duplicate content are same then continue
			if (OriginalContentID == deletableContentID)
				continue;

			//
			MDMSContent deletableContent = (MDMSContent) MTable.get(getCtx(), MDMSContent.Table_ID).getPO(deletableContentID, trx.getTrxName());
			statusUpdate("Remaining Records Count: " + pendingCount + "\n Processing = " + deletableContent);

			log.log(Level.WARNING, "Deletable Content Info: " + deletableContent);

			/**
			 * Move duplicate contents child node to original one
			 */
			if (p_isMoveDuplicateChildContentToCorrectOne)
			{
				int currLinkCount = 0;
				List<MDMSAssociation> duplicateContentAssociations = MDMSAssociation.getAssociationFromContent(deletableContentID, -1, true, trx.getTrxName());
				// check if current Association link records exist
				if (duplicateContentAssociations.size() > 0)
				{
					for (MDMSAssociation association : duplicateContentAssociations)
					{
						if (MDMSAssociationType.isLink(association))
						{
							association.setDMS_Content_ID(OriginalContentID);
							association.saveEx();
							currLinkCount++;
						}
					}
					if (currLinkCount > 0)
						log.log(Level.WARNING, currLinkCount + " Links have been moved to actual content ID: " + OriginalContentID);
				}

				// Child association and update them with original content
				int perCntChildAssnMoved = 0;
				List<MDMSAssociation> childAssociations = MDMSAssociation.getChildAssociationFromContent(deletableContentID, trx.getTrxName());
				for (MDMSAssociation childAssociation : childAssociations)
				{
					childAssociation.setDMS_Content_Related_ID(OriginalContentID);
					childAssociation.saveEx();

					//
					perCntChildAssnMoved++;
					if (perCntChildAssnMoved % 100 == 0)
					{
						trx.commit();
					}
				}

				//
				if (perCntChildAssnMoved % 100 != 0)
				{
					trx.commit();
				}

				//
				log.log(Level.WARNING, perCntChildAssnMoved + " Child content items have been moved under actual content ID: " + OriginalContentID);
				netChildAssnMoved += perCntChildAssnMoved;
			}

			/**
			 * Delete Duplicate Content
			 */
			if (p_isDeleteDuplicateContent)
			{
				StringBuffer msg = new StringBuffer("Removed: ");
				int no = DB.executeUpdate("DELETE FROM DMS_Association 	WHERE DMS_Content_ID = ?", deletableContentID, trx.getTrxName());
				msg.append(no + " associations, ");

				no = DB.executeUpdate("DELETE FROM DMS_Version			WHERE DMS_Content_ID = ?", deletableContentID, trx.getTrxName());
				msg.append(no + " versions, ");

				no = DB.executeUpdate("DELETE FROM DMS_Content 			WHERE DMS_Content_ID = ?", deletableContentID, trx.getTrxName());
				msg.append(no + " content item.");
				//
				log.log(Level.WARNING, msg.toString());
			}

			/**
			 * Mark duplicate content as inactive and set description
			 */
			if (p_isInActiveContentAndSetDescription)
			{
				deletableContent.setDescription("[DIRdeDUPL] " + (deletableContent.getDescription() != null ? deletableContent.getDescription() : ""));
				deletableContent.setIsActive(false);
				deletableContent.saveEx(trx.getTrxName());
			}

			if (netContentProcessed % 100 == 0)
			{
				trx.commit();
			}

			pendingCount--;
			netContentProcessed++;
		}
	} // correction

	/**
	 * Export data into the CSV
	 * 
	 * @param trxName
	 */
	private void exportDataIntoCSV(String trxName)
	{
		// Fetch duplicate data
		List<List<Object>> duplicateDirContentsDataList = DB.getSQLArrayObjectsEx(trxName, SQL_GET_DATA_FROM_TEMPORARY_TABLE, getAD_PInstance_ID());
		if (duplicateDirContentsDataList == null || duplicateDirContentsDataList.size() <= 0)
		{
			String msg = "No duplicate data found for export";
			addLog(0, new Timestamp(System.currentTimeMillis()), null, msg, getTable_ID(), getRecord_ID());
			log.log(Level.SEVERE, msg);
			return;
		}

		// CSV filename
		String fileName = System.getProperty("java.io.tmpdir")	+ File.separator + "DMS_DeDuplication_" + getAD_PInstance_ID() + "_" + p_DMS_Level_For + "_"
							+ DMSConstant.SDF_NO_SPACE.format(new Timestamp(System.currentTimeMillis())) + ".csv";

		// Write data into CSV file
		File dataFile = new File(fileName);
		try (PrintWriter writer = new PrintWriter(new FileWriter(fileName)))
		{
			// Writing CSV Header
			writer.println("PInstanceID,ParentUrl,OriginalContentID,DeletableContentID,DeletableContentChildCount");

			// Writing CSV Data
			for (List<Object> duplicateDir : duplicateDirContentsDataList)
			{
				int PInstanceID = Integer.parseInt(duplicateDir.get(0).toString());
				int OriginalContentID = Integer.parseInt(duplicateDir.get(2).toString());
				int DeletableContentID = Integer.parseInt(duplicateDir.get(3).toString());
				int DeletableContentChildCount = Integer.parseInt(duplicateDir.get(4).toString());
				String ParentUrl = (duplicateDir.get(1) == null ? "" : duplicateDir.get(1).toString());

				//
				writer.println(PInstanceID + "," + ParentUrl + "," + OriginalContentID + "," + DeletableContentID + "," + DeletableContentChildCount);
			}
			//
			log.log(Level.INFO, "Duplicate contents data information saved in file: " + fileName);

			// direct download file
			processUI.download(dataFile);
		}
		catch (Exception e)
		{
			String msg = "Error while preparing CSV Data file. Error: " + e.getLocalizedMessage();
			addLog(0, new Timestamp(System.currentTimeMillis()), null, msg, getTable_ID(), getRecord_ID());
			log.log(Level.SEVERE, msg, e);
		}
	} // exportDataIntoCSV

	/**
	 * Method to insert data into the temporary table
	 * 
	 * @param sql
	 * @param trxName
	 */
	private boolean insertDataIntoTemporaryTable(String sql, String trxName)
	{
		try
		{
			long startTime = System.currentTimeMillis();

			// create temporary table if not exist
			int no = DB.executeUpdateEx(SQL_CREATE_DMS_TEMPORARY_DATA_TABLE, trxName);
			log.log(Level.INFO, no + " created table T_DMS_Duplicate_Clearance");

			// insert data into temporary table
			String insertSQL = "WITH Duplicate_Dir_Data AS ("	+ sql + ")						"
								+ "	INSERT INTO T_DMS_Duplicate_Clearance 						"
								+ " (PInstanceID, ParentURL, OriginalContentID, DeletableContentID, DeletableContentChildCount) "
								+ " 	SELECT ? AS PInstanceID,								"
								+ "			ParentURL, 											"
								+ "			DMS_Content_IDs[1] AS OriginalContentID,			"
								+ "			UNNEST(DMS_Content_IDs) AS DeletableContentID,		"
								+ " 		UNNEST(Child_Counts) as DeletableContentChildCount	"
								+ "		FROM Duplicate_Dir_Data 								";
			int count = DB.executeUpdateEx(insertSQL, new Object[] { getAD_Client_ID(), getAD_PInstance_ID() }, trxName);

			String msg = count + ", Records are inserted. Insertion time: " + getTimeDiff(startTime) + " seconds";
			addLog(msg);
			log.log(Level.INFO, msg);

			return count > 0;
		}
		catch (Exception e)
		{
			String msg = "Error while inserting data into a temporary table. Error: " + e.getLocalizedMessage();
			addLog(0, new Timestamp(System.currentTimeMillis()), null, msg, getTable_ID(), getRecord_ID());
			log.log(Level.SEVERE, msg, e);
			throw e;
		}
	} // insertDataIntoTemporaryTable

	/**
	 * validate process rules
	 * 
	 * @return True if parameter passes is valid
	 */
	private boolean validateAction()
	{
		/*
		 * should be any action select from export report, move content or mark records as in-active
		 * with description
		 */
		if ((!p_isMoveDuplicateChildContentToCorrectOne) && (!p_isGenerateReportOnly) && (!p_isInActiveContentAndSetDescription))
		{
			addLog(0, null, null, "Please select a valid action from the parameters", getTable_ID(), getRecord_ID());
			return false;
		}
		return true;
	} // validateAction

	/**
	 * Time difference in Seconds
	 * 
	 * @param  startTime
	 * @return           seconds difference
	 */
	private int getTimeDiff(long startTime)
	{
		int mseconds = (int) (System.currentTimeMillis() - startTime);
		return mseconds / 1000;
	} // getTimeDiff

}
