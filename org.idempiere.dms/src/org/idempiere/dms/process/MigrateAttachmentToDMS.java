package org.idempiere.dms.process;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSMimeType;
import org.idempiere.model.X_DMS_Content;
import org.zkoss.util.media.AMedia;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

public class MigrateAttachmentToDMS extends SvrProcess
{

	private IFileStorageProvider	fileStorgProvider	= null;
	private IContentManager			contentManager		= null;
	private IThumbnailGenerator		thumbnailGenerator	= null;
	private IIndexSearcher			indexSeracher		= null;
	private Boolean					m_isDelete			= false;

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

		fileStorgProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);

		if (fileStorgProvider == null)
			throw new AdempiereException("Storage provider is not define on clientInfo.");

		contentManager = Utils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		indexSeracher = ServiceUtils.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));

		if (indexSeracher == null)
			throw new AdempiereException("Solr Index server is not found.");

	}

	@Override
	protected String doIt() throws Exception
	{
		int cntMigrated = 0;
		int cntDeleted = 0;

		List<MAttachment> attachments = new Query(getCtx(), MAttachment.Table_Name,
				" AD_Table_ID NOT IN(50008) AND BinaryData IS NOT NULL AND isMigratedToDMS <> 'Y'", get_TrxName())
				.list();

		for (MAttachment attachment : attachments)
		{
			MTable table = MTable.get(getCtx(), attachment.getAD_Table_ID());

			Utils.initiateMountingContent(table.getTableName(), attachment.getRecord_ID(), attachment.getAD_Table_ID());

			IMountingStrategy mountingStrategy = Utils.getMountingStrategy(table.getTableName());
			MDMSContent mountingParent = mountingStrategy.getMountingParent(table.getTableName(),
					attachment.getRecord_ID());

			MAttachmentEntry[] attachmentEntries = attachment.getEntries();

			for (MAttachmentEntry entry : attachmentEntries)
			{
				statusUpdate("Backuping Attachment :" + entry.getName());

				MDMSContent DMSContent = new MDMSContent(Env.getCtx(), 0, get_TrxName());
				DMSContent.setName(entry.getName());
				DMSContent.setValue(entry.getName());
				DMSContent.setDMS_MimeType_ID(Utils.getMimeTypeID(new AMedia(entry.getFile(), entry.getContentType(),
						null)));
				DMSContent.setParentURL(contentManager.getPath(mountingParent));
				DMSContent.setContentBaseType(X_DMS_Content.CONTENTBASETYPE_Content);
				DMSContent.setIsMounting(true);
				DMSContent.saveEx();

				MDMSAssociation DMSAssociation = new MDMSAssociation(Env.getCtx(), 0, get_TrxName());
				DMSAssociation.setDMS_Content_ID(DMSContent.getDMS_Content_ID());
				DMSAssociation.setAD_Table_ID(attachment.getAD_Table_ID());
				DMSAssociation.setRecord_ID(attachment.getRecord_ID());
				DMSAssociation.setDMS_AssociationType_ID(MDMSAssociationType.getVersionType(true));
				DMSAssociation.setDMS_Content_Related_ID(mountingParent.getDMS_Content_ID());
				DMSAssociation.saveEx();

				fileStorgProvider.writeBLOB(fileStorgProvider.getBaseDirectory(contentManager.getPath(DMSContent)),
						entry.getData(), DMSContent);

				MDMSMimeType mimeType = new MDMSMimeType(Env.getCtx(), DMSContent.getDMS_MimeType_ID(), get_TrxName());

				thumbnailGenerator = Utils.getThumbnailGenerator(mimeType.getMimeType());

				if (thumbnailGenerator != null)
					thumbnailGenerator.addThumbnail(DMSContent,
							fileStorgProvider.getFile(contentManager.getPath(DMSContent)), null);

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
			processUI.ask("Do you want to delete previous Migrated Attachments?", new Callback<Boolean>() {

				@Override
				public void onCallback(Boolean result)
				{
					if (result)
					{
						int cntMigAtt = DB.executeUpdate("DELETE FROM AD_Attachment WHERE isMigratedToDMS = 'Y'",
								get_TrxName());
						FDialog.info(0, null, cntMigAtt + " Attachment(s) Deleted Previously Migrated from DB.");
					}
				}
			});
		}

		return "Process Completed.";
	}
}
