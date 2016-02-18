package org.idempiere.componenet;

import java.io.File;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.window.FDialog;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.Utils;
import org.idempiere.dms.storage.RelationalContentManager;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.webui.apps.form.WDMSVersion;
import org.zkoss.image.AImage;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;

public class DMSVersionViewer extends DMSViewerComponent implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long		serialVersionUID	= 7182341901659307540L;

	public static CLogger			log					= CLogger.getCLogger(DMSVersionViewer.class);

	private IFileStorageProvider	fileStorgProvider	= null;
	private IContentManager			contentManager		= null;
	private I_DMS_Content			DMSContent			= null;
	private int						componentNo			= 0;
	private static int				prevCompNo			= 0;

	public DMSVersionViewer(I_DMS_Content content, AImage image, int compNo)
	{
		super(content, image, compNo);
		this.DMSContent = content;
		this.componentNo = compNo;

		fileStorgProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()));

		if (fileStorgProvider == null)
			throw new AdempiereException("Storage provider is not define on clientInfo.");

		contentManager = Utils.getContentManager(RelationalContentManager.KEY);

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		this.addEventListener(Events.ON_CLICK, this);
		this.addEventListener(Events.ON_DOUBLE_CLICK, this);
		this.addEventListener(Events.ON_CLOSE, this);
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		log.info(event.getName());
		if (Events.ON_CLICK.equals(event.getName()))
		{
			WDMSVersion.isSelected[componentNo] = !WDMSVersion.isSelected[componentNo];

			if (WDMSVersion.isSelected.length == 1)
				prevCompNo = 0;

			ZkCssHelper.appendStyle(WDMSVersion.cstmComponent[prevCompNo].fLabel,
					"background-color:#ffffff; box-shadow: 7px 7px 7px #ffffff");

			ZkCssHelper.appendStyle(WDMSVersion.cstmComponent[componentNo].fLabel,
					"background-color:#99cbff; box-shadow: 7px 7px 7px #888888");

			prevCompNo = componentNo;
		}
		else if (Events.ON_DOUBLE_CLICK.equals(event.getName()))
		{
			File document = fileStorgProvider.getFile(contentManager.getPath(DMSContent));
			if (document.exists())
			{
				AMedia media = new AMedia(document, "application/octet-stream", null);
				Filedownload.save(media);
			}
			else
			{
				FDialog.warn(0, "Docuement is not available to download.");
			}
		}
	}

}
