package org.idempiere.componenet;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Menupopup;
import org.adempiere.webui.component.ZkCssHelper;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;
import org.idempiere.webui.apps.form.WDMSVersion;
import org.idempiere.webui.apps.form.WDocumentViewer;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Menuitem;

public class DMSContentViewer extends DMSViewerComponent implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 8592326342697328662L;

	public static CLogger		log					= CLogger.getCLogger(DMSContentViewer.class);
	private static int			prevCompNo			= 0;

	private int					componentNo			= 0;

	private MDMSContent			content				= null;

	public DMSContentViewer(I_DMS_Content content, AImage image, int compNo)
	{
		super(content, image, compNo);

		this.content = (MDMSContent) content;
		this.componentNo = compNo;

		this.addEventListener(Events.ON_CLICK, this);
		this.addEventListener(Events.ON_DOUBLE_CLICK, this);
		this.addEventListener(Events.ON_RIGHT_CLICK, this);
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		log.info(event.getName());
		if (Events.ON_CLICK.equals(event.getName()))
		{
			WDocumentViewer.isSelected[componentNo] = !WDocumentViewer.isSelected[componentNo];

			if (WDocumentViewer.isSelected.length == 1)
				prevCompNo = 0;

			ZkCssHelper.appendStyle(WDocumentViewer.cstmComponent[prevCompNo].fLabel,
					"background-color:#ffffff; box-shadow: 7px 7px 7px #ffffff");

			ZkCssHelper.appendStyle(WDocumentViewer.cstmComponent[componentNo].fLabel,
					"background-color:#99cbff; box-shadow: 7px 7px 7px #888888");

			prevCompNo = componentNo;
		}
		else if (Events.ON_DOUBLE_CLICK.equals(event.getName()))
		{
			if (X_DMS_Content.CONTENTBASETYPE_Directory.equals(contentBaseType))
			{
				prevCompNo = 0;
			}

			WDocumentViewer.prevDMSContent = WDocumentViewer.currDMSContent;
			WDocumentViewer.currDMSContent = new MDMSContent(Env.getCtx(), contentID, null);
		}
		else if (Events.ON_RIGHT_CLICK.equals(event.getName()))
		{
			Menupopup popup = new Menupopup();
			popup.setPage(this.getPage());

			menuItem = new Menuitem("Version List");

			menuItem.addEventListener(Events.ON_CLICK, new EventListener<Event>() {

				@Override
				public void onEvent(Event e) throws Exception
				{
					if (X_DMS_Content.CONTENTBASETYPE_Directory.equals(contentBaseType))
					{
						throw new AdempiereException("No Version available for Directory.");
					}
					else
					{
						WDMSVersion DMSVersion = new WDMSVersion(content);
					}
				}
			});
			popup.appendChild(menuItem);
			this.setContext(popup);
		}
	}
}
