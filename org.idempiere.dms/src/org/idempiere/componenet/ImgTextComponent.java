package org.idempiere.componenet;

import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ZkCssHelper;
import org.compiere.util.CLogger;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Vbox;

public class ImgTextComponent extends Div implements EventListener<Event>
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -5792458835475106196L;
	public static CLogger		log					= CLogger.getCLogger(ImgTextComponent.class);

	private String				fName				= null;
	private String				fPath				= null;
	private Image				prevImg;
	private Div					fLabel				= new Div();
	private Div					dImage				= new Div();
	private int					dheight;
	private int					dwidth;

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

	public ImgTextComponent(String fName, String fpath, AImage image)
	{
		this.fName = fName;
		this.fPath = fpath;

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
		this.setStyle("border:1px solid black;");
		this.addEventListener(Events.ON_CLICK, this);
		this.addEventListener(Events.ON_DOUBLE_CLICK, this);

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
				ZkCssHelper.appendStyle(this, "background-color:#E68A00");
				ZkCssHelper.appendStyle(this, "box-shadow: 7px 7px 7px #888888");
			}
			else
			{
				ZkCssHelper.appendStyle(this, "background-color:#ffffff");
				ZkCssHelper.appendStyle(this, "box-shadow: 7px 7px 7px #ffffff");
			}
		}

	}
}
