package org.idempiere.componenet;

import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Menupopup;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.window.FDialog;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;
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

	private int					dheight;
	private int					dwidth;
	private int					dms_content_id		= 0;

	private Menuitem			menuitem			= null;

	public int getDheight()
	{
		return dheight;
	}

	public void setDheight(int dheight)
	{
		this.dheight = dheight;
		this.setHeight(dheight + "px");
		prevImg.setHeight(dheight - 40 + "px");
	}

	public int getDwidth()
	{
		return dwidth;
	}

	public void setDwidth(int dwidth)
	{
		this.dwidth = dwidth;
		this.setWidth(dwidth + "px");
		fLabel.setWidth(dwidth - 10 + "px");
		prevImg.setWidth(dwidth + "px");
	}

	private Boolean	isSelected	= false;
	private Vbox	vbox		= new Vbox();

	public ImgTextComponent(I_DMS_Content content, AImage image)
	{
		this.fName = content.getName();
		this.dms_content_id = content.getDMS_Content_ID();
		fLabel.appendChild(new Label(fName));

		prevImg = new Image();
		prevImg.setContent(image);
		dImage.appendChild(prevImg);
		vbox.appendChild(dImage);
		vbox.appendChild(fLabel);
		ZkCssHelper.appendStyle(fLabel, "word-wrap: break-word");
		ZkCssHelper.appendStyle(fLabel, "text-align: center");
		ZkCssHelper.appendStyle(dImage, "text-align: center");

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
			isSelected = !isSelected;
			if (isSelected)
			{
				ZkCssHelper.appendStyle(this, "background-color:#99cbff");
				ZkCssHelper.appendStyle(this, "box-shadow: 7px 7px 7px #888888");
			}
			else
			{
				ZkCssHelper.appendStyle(this, "background-color:#ffffff");
				ZkCssHelper.appendStyle(this, "box-shadow: 7px 7px 7px #ffffff");
			}
		}
		else if (Events.ON_DOUBLE_CLICK.equals(event.getName()))
		{
			WDocumentViewer.previousDmsContent = WDocumentViewer.currentDMSContent;
			WDocumentViewer.currentDMSContent = new MDMSContent(Env.getCtx(), dms_content_id, null);
		}
		else if (Events.ON_RIGHT_CLICK.equals(event.getName())
				&& event.getTarget().getClass() == ImgTextComponent.class)
		{
			Menupopup popup = new Menupopup();
			popup.setPage(this.getPage());

			menuitem = new Menuitem("Link Content");
			menuitem.addEventListener(Events.ON_CLICK, this);
			popup.appendChild(menuitem);
			this.setContext(popup);
		}
		else if (event.getTarget().equals(menuitem))
		{
			FDialog.warn(0, "hello");
		}

	}
}
