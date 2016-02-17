package org.idempiere.componenet;

import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Menupopup;
import org.adempiere.webui.component.ZkCssHelper;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;
import org.idempiere.webui.apps.form.WDocumentViewer;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Vbox;

public class ImgTextComponent extends Div implements EventListener<Event>
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -5792458835475106196L;
	public static CLogger		log					= CLogger.getCLogger(ImgTextComponent.class);

	private String				fName				= null;

	private Image				prevImg;
	private Div					fLabel				= new Div();
	private Div					dImage				= new Div();

	private int					dHeight;
	private int					dWidth;
	private int					contentID			= 0;

	private Menuitem			menuItem			= null;
	private int					componentNo			= 0;
	private String				contentBaseType		= null;

	private static int			prevCompNo			= 0;

	public int getDheight()
	{
		return dHeight;
	}

	public void setDheight(int dheight)
	{
		this.dHeight = dheight;
		this.setHeight(dheight + "px");
		prevImg.setHeight(dheight - 40 + "px");
	}

	public int getDwidth()
	{
		return dWidth;
	}

	public void setDwidth(int dwidth)
	{
		this.dWidth = dwidth;
		this.setWidth(dwidth + "px");
		fLabel.setWidth(dwidth - 10 + "px");
		prevImg.setWidth(dwidth + "px");
	}

	private Vbox	vbox	= new Vbox();

	public ImgTextComponent(I_DMS_Content content, AImage image, int compNo)
	{
		this.contentBaseType = content.getContentBaseType();
		this.fName = content.getName();
		this.contentID = content.getDMS_Content_ID();
		this.componentNo = compNo;

		fLabel.appendChild(new Label(fName));
		fLabel.setTooltiptext(content.getName());
		prevImg = new Image();
		prevImg.setContent(image);
		dImage.appendChild(prevImg);
		vbox.appendChild(dImage);
		vbox.appendChild(fLabel);
		fLabel.setStyle("text-overflow: ellipsis; white-space: nowrap; overflow: hidden;");
		ZkCssHelper.appendStyle(fLabel, "text-align: left");
		ZkCssHelper.appendStyle(dImage, "text-align: center");
		if (content.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
		{
			ZkCssHelper.appendStyle(fLabel, "text-align: center");
		}

		this.appendChild(vbox);
		this.setStyle("background-color: #ffffff");
		// this.setStyle("border:1px solid black;");
		this.addEventListener(Events.ON_CLICK, this);
		this.addEventListener(Events.ON_DOUBLE_CLICK, this);
		this.addEventListener(Events.ON_RIGHT_CLICK, this);

		ZkCssHelper.appendStyle(this, "text-align: center");
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
				prevCompNo = 0;
			WDocumentViewer.prevDMSContent = WDocumentViewer.currDMSContent;
			WDocumentViewer.currDMSContent = new MDMSContent(Env.getCtx(), contentID, null);
		}
		else if (Events.ON_RIGHT_CLICK.equals(event.getName())
				&& event.getTarget().getClass() == ImgTextComponent.class)
		{
			Menupopup popup = new Menupopup();
			popup.setPage(this.getPage());

			menuItem = new Menuitem("Link Content");
			menuItem.addEventListener(Events.ON_CLICK, this);
			popup.appendChild(menuItem);
			this.setContext(popup);
		}
	}
}
