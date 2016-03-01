package org.idempiere.componenet;

import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ZkCssHelper;
import org.compiere.util.CLogger;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;
import org.zkoss.image.AImage;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Vbox;

public class DMSViewerComponent extends Div
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -5792458835475106196L;

	public static CLogger		log					= CLogger.getCLogger(DMSViewerComponent.class);

	protected String			fName				= null;
	private String				contentBaseType		= null;

	protected Image				prevImg;

	private Div					fLabel				= new Div();
	protected Div				dImage				= new Div();

	protected int				dHeight;
	protected int				dWidth;

	private MDMSContent			DMSContent			= null;

	protected Menuitem			menuItem			= null;

	protected Vbox				vbox				= new Vbox();

	public MDMSContent getDMSContent()
	{
		return DMSContent;
	}

	public void setDMSContent(MDMSContent dMSContent)
	{
		DMSContent = dMSContent;
	}

	public Div getfLabel()
	{
		return fLabel;
	}

	public void setfLabel(Div fLabel)
	{
		this.fLabel = fLabel;
	}

	public String getContentBaseType()
	{
		return contentBaseType;
	}

	public void setContentBaseType(String contentBaseType)
	{
		this.contentBaseType = contentBaseType;
	}

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

	public String getfName()
	{
		return fName;
	}

	public void setfName(String fName)
	{
		this.fName = fName;
	}

	public DMSViewerComponent(I_DMS_Content content, AImage image)
	{
		this.contentBaseType = content.getContentBaseType();
		this.fName = content.getName();
		this.DMSContent = (MDMSContent) content;

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

		ZkCssHelper.appendStyle(this, "text-align: center");
	}
}
