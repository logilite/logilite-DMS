package com.logilite.dms.contentdirdeduplication.process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.compiere.util.Util;

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
																				+ " WHERE ContentBaseType = 'DIR' AND IsMounting = 'Y' AND AD_Client_ID = ? AND #PARENT_URL_FILTER#	"
																				+ " GROUP BY ParentURL, Name 																		"
																				+ "	HAVING COUNT(1) > 1 																			"
																				+ "	ORDER BY COUNT(1) DESC, ParentURL, Name															";

	public static final String	SQL_CREATE_DMS_TEMPORARY_DATA_TABLE			= "CREATE TABLE IF NOT EXISTS T_DMS_Duplicate_Clearance "
																				+ " (PInstanceID integer, ParentURL character varying, OriginalContentID numeric, DeletableContentID numeric, DeletableContentChildCount bigint)";

	public static final String	SQL_GET_DATA_FROM_TEMPORARY_TABLE			= "SELECT PInstanceID, ParentURL, OriginalContentID, DeletableContentID, DeletableContentChildCount "
																				+ "	FROM T_DMS_Duplicate_Clearance 								"
																				+ " WHERE PInstanceID = ? 										"
																				+ " ORDER BY OriginalContentID, DeletableContentChildCount DESC	";

	public static final String	SQL_CONTENT_CHILD_ASSOCIATION_RECORDS		= "SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_Related_ID = ?";

	// params
	public String				p_DMS_Level_For								= "";
	public boolean				p_isGenerateReportOnly						= false;
	public boolean				p_isMoveDuplicateChildContentToCorrectOne	= false;
	public boolean				p_isDeleteDuplicateContent					= false;
	public boolean				p_isInActiveContentAndSetDescription		= false;

	// counters
	private int					totalChildAssociaionMoved					= 0;
	private int					toalDMSCotnentUpdated						= 0;

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
		addLog(0, new Timestamp(System.currentTimeMillis()), null, "Process Started : DMS Content Directory DeDuplication", getTable_ID(), getRecord_ID());

		//
		correctContentDirDeDuplication();

		//
		if (p_isDeleteDuplicateContent)
		{
			addLog("Total Duplicate DMS Content Records Deleted:" + toalDMSCotnentUpdated);
		}
		else
		{
			addLog("Total Duplicate DMS Content Records Updated:" + toalDMSCotnentUpdated);
		}

		addLog("Total Duplicate DMS Content Child Records Updated:" + totalChildAssociaionMoved);
		addLog("Total Execution Time = " + getTimeDiff(starttime) + " minutes");
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
					sql = SQL_DUPLICATE_DIR_SQL_BASE.replace(PARENT_URL_FILTER, "ParentURL='" + mountingBasePath + "'");
				}
				else
				{
					// EX= " ParentURL IS NOT NULL AND ParentURL!='/Attachment' "
					sql = SQL_DUPLICATE_DIR_SQL_BASE.replace(PARENT_URL_FILTER, "ParentURL IS NOT NULL AND ParentURL!='" + mountingBasePath + "'");
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
				exportDataIntoCSV(sql, trxName);
			}
			else
			{
				statusUpdate("Starting the duplicate data correction for DMS");
				correctDMSDeDuplciateDir(customTrx);
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
	private void correctDMSDeDuplciateDir(Trx trx)
	{
		List<List<Object>> duplicateDirDataList = DB.getSQLArrayObjectsEx(trx.getTrxName(), SQL_GET_DATA_FROM_TEMPORARY_TABLE, getAD_PInstance_ID());
		int totalSize = duplicateDirDataList.size();

		String msg = "Total " + totalSize + " DMS records  are found for correction.";
		addLog(msg);
		log.log(Level.INFO, msg);

		for (List<Object> duplicateDirData : duplicateDirDataList)
		{
			int OriginalContentID = ((BigDecimal) duplicateDirData.get(2)).intValue();
			int deletableContentID = ((BigDecimal) duplicateDirData.get(3)).intValue();

			if (OriginalContentID <= 0 || deletableContentID <= 0)
			{
				addLog("No valid Record Found for correction");
				continue;
			}

			// if actual and duplicate same continue
			if (OriginalContentID == deletableContentID)
				continue;

			MDMSContent deletableContent = (MDMSContent) MTable.get(getCtx(), MDMSContent.Table_ID).getPO(deletableContentID, trx.getTrxName());
			statusUpdate("Remaining Records Count- " + totalSize + " - processing " + deletableContent);

			if (p_isMoveDuplicateChildContentToCorrectOne)
			{
				List<MDMSAssociation> duplicateContentAssociations = MDMSAssociation.getAssociationFromContent(deletableContentID, -1, true, trx.getTrxName());
				// check if current Association link records exist
				if (duplicateContentAssociations.size() > 0)
				{
					for (MDMSAssociation association : duplicateContentAssociations)
					{
						if (MDMSAssociationType.LINK_ID == association.getDMS_AssociationType_ID())
						{
							association.setDMS_Content_ID(OriginalContentID);
							association.saveEx();
						}
					}
				}

				// get child association and update them content related id
				List<MDMSAssociation> childAssociations = MDMSAssociation.getChildAssociationFromContent(deletableContentID, trx.getTrxName());
				for (MDMSAssociation childAssociation : childAssociations)
				{
					statusUpdate("Remaining Records Count: " + totalSize + ", processing: " + deletableContent + " --> " + childAssociation);

					if (MDMSAssociationType.isLink(childAssociation))
					{
						childAssociation.setDMS_Content_ID(OriginalContentID);
					}
					else
					{
						childAssociation.setDMS_Content_Related_ID(OriginalContentID);
					}
					childAssociation.saveEx();
					totalChildAssociaionMoved++;

					if (totalChildAssociaionMoved % 100 == 0)
					{
						trx.commit();
					}
				}
			}

			//
			if (p_isDeleteDuplicateContent)
			{
				log.log(Level.WARNING, deletableContentID + " content deleted.");

				log.warning("Delete Soft       : " + deletableContent.toString() + " " + deletableContent.getParentURL());
				int no = DB.executeUpdate("DELETE FROM DMS_Association 	WHERE DMS_Content_ID = ?", deletableContent.getDMS_Content_ID(), trx.getTrxName());
				log.log(Level.WARNING, no + " association deleted.");

				no = DB.executeUpdate("DELETE FROM DMS_Version			WHERE DMS_Content_ID = ?", deletableContent.getDMS_Content_ID(), trx.getTrxName());
				log.log(Level.WARNING, no + " version deleted.");

				no = DB.executeUpdate("DELETE FROM DMS_Content 			WHERE DMS_Content_ID = ?", deletableContent.getDMS_Content_ID(), trx.getTrxName());
				log.log(Level.WARNING, no + " content deleted.");
			}

			// if inactive deletable content only and set description
			if (p_isInActiveContentAndSetDescription)
			{
				deletableContent.setDescription("[DIRdeDUPL] " + (deletableContent.getDescription() != null ? deletableContent.getDescription() : ""));
				deletableContent.setIsActive(false);
				deletableContent.saveEx(trx.getTrxName());
			}

			if (toalDMSCotnentUpdated % 100 == 0)
			{
				trx.commit();
			}

			totalSize--;
			toalDMSCotnentUpdated++;
		}
	} // correctDMSDeDuplciateDir

	/**
	 * method to export data into the CSV
	 * 
	 * @param  finalSQL
	 * @param  trxName
	 * @throws IOException
	 */
	private void exportDataIntoCSV(String finalSQL, String trxName)
	{
		List<List<Object>> duplicateDirContentsDataList = DB.getSQLArrayObjectsEx(trxName, SQL_GET_DATA_FROM_TEMPORARY_TABLE, getAD_PInstance_ID());

		// if null the return
		if (duplicateDirContentsDataList == null || duplicateDirContentsDataList.size() <= 0)
		{
			String msg = "No eligible Data found in temporary table for export";
			addLog(0, new Timestamp(System.currentTimeMillis()), null, msg, getTable_ID(), getRecord_ID());
			log.log(Level.SEVERE, msg);
			return;
		}

		String fileName = System.getProperty("java.io.tmpdir")	+ File.separator + "DMS_Dir_Duplication_"
							+ DMSConstant.SDF_NO_SPACE.format(new Timestamp(System.currentTimeMillis())) + ".csv";
		// time stamp
		File dataFile = new File(fileName);
		try (PrintWriter writer = new PrintWriter(new FileWriter(fileName)))
		{
			// Writing CSV Header
			writer.println("PInstanceID,ParentUrl,OriginalContentID,DeletableContentID,DeletableContentChildCount");

			for (List<Object> duplicateDir : duplicateDirContentsDataList)
			{
				int PInstanceID = (Integer.valueOf(duplicateDir.get(0).toString())).intValue();
				String ParentUrl = (duplicateDir.get(1) == null ? "" : duplicateDir.get(1).toString());
				int OriginalContentID = (Integer.valueOf(duplicateDir.get(2).toString())).intValue();
				int DeletableContentID = (Integer.valueOf(duplicateDir.get(3).toString())).intValue();
				int DeletableContentChildCount = (Integer.valueOf(duplicateDir.get(4).toString())).intValue();

				writer.println(PInstanceID + "," + ParentUrl + "," + OriginalContentID + "," + DeletableContentID + "," + DeletableContentChildCount);
			}
			log.log(Level.INFO, "Temporary file Created: " + fileName);

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
			long insertStartTime = System.currentTimeMillis();
			// create temporary table if not exist
			DB.executeUpdateEx(SQL_CREATE_DMS_TEMPORARY_DATA_TABLE, trxName);

			// insert data into table
			String insertSQL = "WITH Duplicate_Dir_Data AS ("	+ sql + ")	"
								+ "	INSERT INTO T_DMS_Duplicate_Clearance 	"
								+ " (PInstanceID, ParentURL, OriginalContentID, DeletableContentID, DeletableContentChildCount) "
								+ " 	SELECT " + getAD_PInstance_ID() + " AS PInstanceID,		"
								+ "			ParentURL, 											"
								+ "			DMS_Content_IDs[1] AS OriginalContentID,			"
								+ "			UNNEST(DMS_Content_IDs) AS DeletableContentID,		"
								+ " 		UNNEST(Child_Counts) as DeletableContentChildCount	"
								+ "		FROM Duplicate_Dir_Data 								";

			int insertRecordCount = DB.executeUpdate(insertSQL, getAD_Client_ID(), trxName);

			String msg = "Total "	+ insertRecordCount
							+ " Records are inserted into the temporary table and the time to insert data in table:"
							+ getTimeDiff(insertStartTime) + " minutes";

			addLog(msg);
			log.log(Level.INFO, msg);

			return insertRecordCount > 0;
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
		 * should be any action select from export report, move content or mark
		 * records in-active and set desc
		 */
		if ((!p_isMoveDuplicateChildContentToCorrectOne)	&& (!p_isGenerateReportOnly)
			&& (!p_isInActiveContentAndSetDescription))
		{
			addLog(0, null, null, "Please select a valid action from the parameters", getTable_ID(), getRecord_ID());
			return false;
		}
		return true;
	} // validateAction

	/**
	 * @param  startTime
	 * @return
	 */
	private int getTimeDiff(long startTime)
	{
		int mseconds = (int) (System.currentTimeMillis() - startTime);
		int minutes = (mseconds / 1000) / 60;
		return minutes;
	} // getTimeDiff

}
