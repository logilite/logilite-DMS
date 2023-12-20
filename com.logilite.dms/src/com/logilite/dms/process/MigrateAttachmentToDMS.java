package com.logilite.dms.process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.util.Callback;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.logilite.dms.DMS;
import com.logilite.dms.exception.DMSContentExistException;
import com.logilite.dms.exception.DMSException;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSVersion;
import com.logilite.dms.util.DMSSearchUtils;
import com.logilite.search.exception.IndexException;

public class MigrateAttachmentToDMS extends SvrProcess
{

	private DMS		dms;
	private Boolean	p_isDeleteOrigAttch	= false;
	private int		p_tableID			= 0;

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
		}

		dms = new DMS(getAD_Client_ID());
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
		String whereClause = " AD_Table_ID NOT IN(284, 285, 376, 454, 489, 523, 50008) AND BinaryData IS NOT NULL AND isMigratedToDMS <> 'Y'";
		if (p_tableID > 0)
			whereClause += " AND AD_Table_ID = " + p_tableID;

		List<MAttachment> attachments = new Query(getCtx(), MAttachment.Table_Name, whereClause, get_TrxName())
																												.setClient_ID()
																												.setOrderBy("AD_Table_ID")
																												.list();
		ArrayList<Integer> unIndexedContentList = new ArrayList<Integer>();
		for (MAttachment attachment : attachments)
		{
			MTable table = MTable.get(getCtx(), attachment.getAD_Table_ID());

			dms.initMountingStrategy(table.getTableName());
			dms.initiateMountingContent(table.getTableName(), attachment.getRecord_ID(), attachment.getAD_Table_ID());
			MDMSContent mountingParent = dms.getRootMountingContent(table.getAD_Table_ID(), attachment.getRecord_ID());

			MAttachmentEntry[] attachmentEntries = attachment.getEntries();
			int cntFailed = 0;
			for (MAttachmentEntry entry : attachmentEntries)
			{
				statusUpdate("Move attachment of " + table.getTableName() + " for Record ID #" + attachment.getRecord_ID() + " : " + entry.getName());

				try
				{
					int contentID = dms.addFile(mountingParent, entry.getFile(), attachment.getAD_Table_ID(), attachment.getRecord_ID());
					if (contentID > 0)
						cntMigrated++;
				}
				catch (DMSException e)
				{
					if (e.getException() instanceof DMSContentExistException)
					{
						cntFailed++;
						addLog(e.getException().getLocalizedMessage());
						log.log(Level.SEVERE, e.getException().getLocalizedMessage());
					}
					else if (e.getException() instanceof IndexException)
					{
						cntMigrated++;
						unIndexedContentList.add(e.getContentID());
						log.log(Level.WARNING, e.getException().getLocalizedMessage());
					}
				}
			}

			// if entry failed to sync with DMS then no require to mark as migrated, Give a try for
			// next time if user do.
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
		}

		addLog(cntMigrated + " Attachment(s) Moved to DMS");
		addLog(cntDeleted + " Attachment(s) Deleted from DB");
		addLog(unIndexedContentList.size() + " Pending index content creation");

		//
		if (p_isDeleteOrigAttch || attachments.size() == 0)
		{
			processUI.ask("Do you want to delete previously migrated attachments if exist?", new Callback<Boolean>() {

				@Override
				public void onCallback(Boolean result)
				{
					if (result)
					{
						int cntMigAtt = DB.executeUpdate("DELETE FROM AD_Attachment WHERE isMigratedToDMS = 'Y'", get_TrxName());
						Dialog.info(0, cntMigAtt + " record(s) attachments were deleted as previously migrated from DB.");
					}
				}
			});
		}

		//
		if (unIndexedContentList.size() > 0)
		{
			processUI.ask("Do you want to try again for pending content index creation?", new Callback<Boolean>() {

				@Override
				public void onCallback(Boolean result)
				{
					if (result)
					{
						// fetch non-indexed content and its association reference for re-indexing
						String sql = "SELECT v.DMS_Content_ID, a.DMS_Association_ID "
										+ " FROM DMS_Version v "
										+ " INNER JOIN DMS_Association a ON ((a.DMS_Content_Related_ID = v.DMS_Content_ID OR a.DMS_Content_ID = v.DMS_Content_ID) "
										+ " 								AND COALESCE(DMS_AssociationType_ID, 0) IN (0, 1000001)) "
										+ " WHERE v.DMS_Content_ID IN (" + unIndexedContentList.toString().replaceAll("\\[|\\]", "") + ") AND IsIndexed = 'N'";

						List<List<Object>> latestVersion = DB.getSQLArrayObjectsEx(null, sql);
						if (latestVersion != null && latestVersion.size() > 0)
						{
							int cnt = 0;
							for (int i = 0; i < latestVersion.size(); i++)
							{
								List<Object> contentData = (List<Object>) latestVersion.get(i);
								I_DMS_Content content = new MDMSContent(Env.getCtx(), ((BigDecimal) contentData.get(0)).intValue(), null);
								I_DMS_Association associationLink = new MDMSAssociation(getCtx(), ((BigDecimal) contentData.get(1)).intValue(), null);

								try
								{
									DMSSearchUtils.doIndexingInServer(content, associationLink, MDMSVersion.getLatestVersion(content, false), null, null);
									cnt++;
								}
								catch (Exception e)
								{}
							}
							Dialog.info(0, cnt + " Index created out of " + latestVersion.size());
						}
					}
				}
			});
		}

		return "Process Completed.";
	}

}
