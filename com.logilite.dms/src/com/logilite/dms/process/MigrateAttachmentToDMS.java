package com.logilite.dms.process;

import java.io.File;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import com.logilite.dms.DMS;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.exception.DMSContentExistException;
import com.logilite.dms.exception.DMSException;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.util.Utils;
import com.logilite.search.exception.IndexException;

/**
 * Migrate Attachment documents to DMS
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class MigrateAttachmentToDMS extends SvrProcess
{

	private Boolean		p_isDeleteOrigAttch						= false;
	private Boolean		p_isDeletePreviouslyMigratedAttachments	= false;

	private int			p_tableID								= 0;
	private int			p_TrxTimeOut							= 60 * 120;	// 120 minutes;

	private Timestamp	p_createdFrom;
	private Timestamp	p_createdTo;

	@Override
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (("DeleteOriginalAttachment").equals(name))
			{
				p_isDeleteOrigAttch = para[i].getParameterAsBoolean();
			}
			else if ("AD_Table_ID".equals(name))
			{
				p_tableID = para[i].getParameterAsInt();
			}
			else if (MDMSContent.COLUMNNAME_Created.equals(name))
			{
				p_createdFrom = para[i].getParameterAsTimestamp();
				p_createdTo = para[i].getParameter_ToAsTimestamp();
			}
			else if (("IsDeletePreviouslyMigratedAttachments").equals(name))
			{
				p_isDeletePreviouslyMigratedAttachments = para[i].getParameterAsBoolean();
			}
			else if (name.equals("TrxTimeOut"))
			{
				p_TrxTimeOut = para[i].getParameterAsInt();
			}
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		int cntMigrated = 0;
		int cntDeleted = 0;

		/*
		 * 284 = AD_Process
		 * 285 = AD_Process_Para
		 * 376 = AD_Form
		 * 454 = AD_PrintForm
		 * 489 = AD_PrintFormatItem
		 * 523 = AD_PrintTableFormat
		 * 50008=AD_Package_Imp_Proc
		 */
		ArrayList<Integer> unIndexedContentList = new ArrayList<Integer>();

		//
		String whereClause = " AD_Table_ID NOT IN(284, 285, 376, 454, 489, 523, 50008) AND BinaryData IS NOT NULL AND isMigratedToDMS <> 'Y' AND AD_Client_ID = ?";
		List<Object> paramList = new ArrayList<>();
		paramList.add(Env.getAD_Client_ID(getCtx()));

		// Table Specific Filter
		if (p_tableID > 0)
		{
			whereClause += " AND AD_Table_ID = ?";
			paramList.add(p_tableID);
		}

		// Created Date Filter
		if (p_createdFrom != null && p_createdTo != null)
		{
			whereClause += " AND TRUNC(Created) BETWEEN ? AND ? ";
			paramList.add(p_createdFrom);
			paramList.add(p_createdTo);
		}
		else if (p_createdFrom != null && p_createdTo == null)
		{
			whereClause += " AND TRUNC(Created) >= ? ";
			paramList.add(p_createdFrom);
		}
		else if (p_createdFrom == null && p_createdTo != null)
		{
			whereClause += " AND TRUNC(Created) <= ? ";
			paramList.add(p_createdTo);
		}

		String sql = "SELECT AD_Attachment_ID FROM AD_Attachment "
						+ " WHERE " + whereClause
						+ " ORDER BY AD_Table_ID, AD_Attachment_ID ";

		int countTotal = DB.getSQLValue(get_TrxName(), "SELECT COUNT(1) FROM (" + sql + " ) AS Data ", paramList.toArray());

		//
		int cntOrphanAtt = 0;
		int cntProcessing = 0;
		int cntEmptyAttEntries = 0;

		//
		Trx trx = Trx.get(get_TrxName(), false);
		trx.setTimeout(p_TrxTimeOut);

		try
		{
			DMS dms = new DMS(getAD_Client_ID());

			//
			int[] attachmentIDs = DB.getIDsEx(get_TrxName(), sql, paramList.toArray());

			Instant progressStart = Instant.now();

			for (int attachmentID : attachmentIDs)
			{
				Instant startTime = Instant.now();
				MAttachment attachment = (MAttachment) MTable.get(getCtx(), MAttachment.Table_Name).getPO(attachmentID, get_TrxName());

				int tableID = attachment.getAD_Table_ID();
				int recordID = attachment.getRecord_ID();

				MTable table = MTable.get(getCtx(), tableID);
				String tableName = table.getTableName();

				// Check physical record present, if not then delete the orphan attachment
				int count = DB.getSQLValue(null, "SELECT COUNT(1) FROM " + tableName + " WHERE " + tableName + "_ID = " + recordID);
				if (count <= 0)
				{
					addLog("Orphan attachment found: Attachment_ID = " + attachmentID + ", Table = " + tableName + ", Record_ID = " + recordID);
					count = DB.executeUpdate("DELETE FROM AD_Attachment WHERE AD_Attachment_ID = ?", attachmentID, get_TrxName());
					if (count > 0)
					{
						log.log(Level.WARNING, "Orphan attachment deleted...");
					}
					cntOrphanAtt++;
					continue;
				}

				// Skip attachment if entries not found
				MAttachmentEntry[] attachmentEntries;
				try
				{
					attachmentEntries = attachment.getEntries();
				}
				catch (Exception e)
				{
					String msg = "Empty attachment record deleted because no attachment entries are found"
									+ " [Attachment_ID=" + attachment.get_ID()
									+ ", Table=" + tableName
									+ ", Record_ID=" + recordID + "]";
					count = DB.executeUpdate("DELETE FROM AD_Attachment WHERE AD_Attachment_ID = ?", attachmentID, get_TrxName());
					if (count > 0)
					{
						addLog(msg);
						cntEmptyAttEntries++;
					}
					continue;
				}

				//
				dms.initMountingStrategy(tableName);
				dms.initiateMountingContent(tableName, recordID, tableID);
				MDMSContent mountingParent = dms.getRootMountingContent(tableID, recordID);

				// Status Progress Message
				String progressMsg = "[ " + cntProcessing + " out of " + countTotal + " ]";
				if (Duration.between(progressStart, Instant.now()).toMinutes() % 2 == 0)
				{
					saveProgress(recordID, Timestamp.valueOf(LocalDate.now().atStartOfDay()), null, progressMsg);
				}
				statusUpdate(progressMsg + " Move attachment of " + tableName + " for Record ID #" + recordID);

				int cntFailed = 0;
				for (MAttachmentEntry entry : attachmentEntries)
				{
					File renamedFile = null;
					String fileName = entry.getName();
					try
					{
						// Remove special characters in filename as
						// ! $ % ^ * ( ) = [ ] { } ; : ' " , < > ? \ | ` ~
						fileName = fileName.replaceAll(DMSConstant.REG_EXP_SPECIAL_CHARACTERS, "");

						if (Util.isEmpty(fileName))
						{
							fileName = "_.def";
						}

						if (fileName.length() > DMSConstant.MAX_FILENAME_LENGTH)
						{
							int lastDotIndex = fileName.lastIndexOf('.');

							if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1)
							{
								String nameWithoutExtension = fileName.substring(0, lastDotIndex);
								String extension = fileName.substring(lastDotIndex);
								int allowedNameLength = DMSConstant.MAX_FILENAME_LENGTH - extension.length();
								fileName = nameWithoutExtension.substring(0, Math.min(nameWithoutExtension.length(), allowedNameLength)) + extension;
							}
						}
						renamedFile = entry.getFile(fileName);

						int contentID = dms.addFile(mountingParent, renamedFile, tableID, recordID);
						if (contentID > 0)
							cntMigrated++;
					}
					catch (DMSException e)
					{
						if (e.getException() instanceof DMSContentExistException)
						{
							if (e.getException().getLocalizedMessage().startsWith("File already exists"))
							{
								String newFilename = FilenameUtils.getBaseName(fileName)	+ "_" + ((int) (Math.random() * 90) + 10)
														+ Utils.getFileExtension(fileName);
								int contentID = dms.addFile(mountingParent, renamedFile, newFilename, null, 0, 0, tableID, recordID);
								if (contentID > 0)
								{
									cntMigrated++;
									String msg = tableName	+ ", Record ID = " + recordID
													+ ", DMS same name document already exists, So renamed from '" + fileName + "' to '" + newFilename + "'";
									addLog(msg);
									log.log(Level.WARNING, msg);
								}
							}
							else
							{
								cntFailed++;
								addLog(e.getException().getLocalizedMessage());
								log.log(Level.SEVERE, e.getException().getLocalizedMessage());
							}
						}
						else if (e.getException() instanceof IndexException)
						{
							cntMigrated++;
							unIndexedContentList.add(e.getContentID());
							log.log(Level.WARNING, e.getException().getLocalizedMessage());
						}
					}
					finally
					{
						if (renamedFile != null)
							renamedFile.delete();
					}
				}

				// if entry failed to sync with DMS then no require to mark as migrated, Give a try
				// for next time if user do.
				if (cntFailed <= 0)
				{
					attachment.set_CustomColumn("isMigratedToDMS", true);
					attachment.saveEx();

					if (p_isDeleteOrigAttch)
					{
						cntDeleted++;
						attachment.deleteEx(true);
					}
				}

				// CLDE Commit after each record migrated successfully changes
				// so we do not revert changes when process fails in middle of migration.
				commitEx();

				long durationSeconds = Duration.between(startTime, Instant.now()).toSeconds();

				log.log(Level.INFO, "Completed processing attachment  " + attachmentID + " for table '" + tableName +
									"', record ID " + recordID + " in " + durationSeconds + " seconds.");
				cntProcessing++;
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Migration failed. Rolling back.", e);
			trx.rollback();
		}

		addLog(cntMigrated + " Attachment(s) Moved to DMS");
		addLog(cntDeleted + " Attachment(s) Deleted from DB");
		addLog(unIndexedContentList.size() + " Pending index content creation");
		addLog(cntOrphanAtt + " orphan or invalid attachment(s) deleted.");
		addLog(cntEmptyAttEntries + " Pending index content creation");

		//
		if (p_isDeletePreviouslyMigratedAttachments)
		{
			int cntMigAtt = DB.executeUpdate("DELETE FROM AD_Attachment WHERE isMigratedToDMS = 'Y'", null);
			addLog(cntMigAtt + " record(s) attachments were deleted as previously migrated from DB.");
		}
		//

		return "Process Completed.";
	}
}
