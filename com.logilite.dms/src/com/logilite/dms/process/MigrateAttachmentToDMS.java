package com.logilite.dms.process;

import java.util.List;

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
import com.logilite.dms.model.MDMSContent;

public class MigrateAttachmentToDMS extends SvrProcess
{

	private DMS		dms;
	private Boolean	m_isDelete	= false;

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
				m_isDelete = para[i].getParameterAsBoolean();
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
		List<MAttachment> attachments = new Query(	getCtx(), MAttachment.Table_Name,
													" AD_Table_ID NOT IN(284, 285, 376, 454, 489, 523, 50008) AND BinaryData IS NOT NULL AND isMigratedToDMS <> 'Y' AND AD_Client_ID = ? ",
													get_TrxName()).setParameters(Env.getAD_Client_ID(Env.getCtx())).list();

		for (MAttachment attachment : attachments)
		{
			MTable table = MTable.get(getCtx(), attachment.getAD_Table_ID());

			dms.initMountingStrategy(table.getTableName());
			dms.initiateMountingContent(table.getTableName(), attachment.getRecord_ID(), attachment.getAD_Table_ID());
			MDMSContent mountingParent = dms.getRootMountingContent(table.getAD_Table_ID(), attachment.getRecord_ID());

			MAttachmentEntry[] attachmentEntries = attachment.getEntries();

			for (MAttachmentEntry entry : attachmentEntries)
			{
				statusUpdate(" Backup attachment of " + table.getTableName() + " for Record ID #" + attachment.getRecord_ID() + ": " + entry.getName());

				int contentID = dms.addFile(mountingParent, entry.getFile(), attachment.getAD_Table_ID(), attachment.getRecord_ID());
				if (contentID > 0)
					cntMigrated++;
			}

			if (m_isDelete)
			{
				cntDeleted++;
				attachment.deleteEx(true);
			}
			else
			{
				attachment.set_CustomColumn("isMigratedToDMS", true);
				attachment.saveEx();
			}
		}

		addLog(cntMigrated + " Attachment(s) Backuped to DMS.");
		addLog(cntDeleted + " Attachment(s) Deleted from DB.");

		if (m_isDelete || attachments.size() == 0)
		{
			processUI.ask("Do you want to delete previously migrated attachments?", new Callback<Boolean>() {

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
		return "Process Completed.";
	}

}
