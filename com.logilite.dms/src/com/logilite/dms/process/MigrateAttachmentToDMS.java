package com.logilite.dms.process;

import java.util.List;

import org.adempiere.util.Callback;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import com.logilite.dms.DMS;
import com.logilite.dms.model.MDMSContent;

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

		for (MAttachment attachment : attachments)
		{
			MTable table = MTable.get(getCtx(), attachment.getAD_Table_ID());

			dms.initMountingStrategy(table.getTableName());
			dms.initiateMountingContent(table.getTableName(), attachment.getRecord_ID(), attachment.getAD_Table_ID());
			MDMSContent mountingParent = dms.getRootMountingContent(table.getAD_Table_ID(), attachment.getRecord_ID());

			MAttachmentEntry[] attachmentEntries = attachment.getEntries();
			for (MAttachmentEntry entry : attachmentEntries)
			{
				statusUpdate("Move attachment of " + table.getTableName() + " for Record ID #" + attachment.getRecord_ID() + " : " + entry.getName());

				//
				int contentID = dms.addFile(mountingParent, entry.getFile(), attachment.getAD_Table_ID(), attachment.getRecord_ID());
				if (contentID > 0)
					cntMigrated++;
			}

			if (p_isDeleteOrigAttch)
			{
				cntDeleted++;
				attachment.deleteEx(true);
			}
			else
			{
				attachment.set_CustomColumn("isMigratedToDMS", true);
				attachment.saveEx();
			}
			// CLDE Commit after each record migrated successfully changes
			// so we do not revert changes when process fails in middle of migration.
			commitEx();
		}

		addLog(cntMigrated + " Attachment(s) Backuped to DMS.");
		addLog(cntDeleted + " Attachment(s) Deleted from DB.");

		if (p_isDeleteOrigAttch || attachments.size() == 0)
		{
			processUI.ask("Do you want to delete previously migrated attachments?", new Callback<Boolean>() {

				@Override
				public void onCallback(Boolean result)
				{
					if (result)
					{
						int cntMigAtt = DB.executeUpdate("DELETE FROM AD_Attachment WHERE isMigratedToDMS = 'Y'", get_TrxName());
						FDialog.info(0, null, cntMigAtt + " record(s) attachments were deleted as previously migrated from DB.");
					}
				}
			});
		}
		return "Process Completed.";
	}

}
