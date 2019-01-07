/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package org.idempiere.componenet;

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ZkCssHelper;
import org.compiere.util.CLogger;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
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
	private MDMSAssociation 	DMSAssociation 		= null;

	protected Menuitem			menuItem			= null;

	protected Vbox				vbox				= new Vbox();

	private boolean				isLink				= false;
	private Image				linkImage			= null;
	private Div					mimeIcon			= new Div();
	private Div					footerDiv			= new Div();

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
		if (isLink)
		{
			footerDiv.setHeight(dHeight - 120 + "px");
		}

		prevImg.setHeight(dheight - 30 + "px");
	}

	public int getDwidth()
	{
		return dWidth;
	}

	public void setDwidth(int dwidth)
	{
		this.dWidth = dwidth;
		this.setWidth(dwidth + "px");
		if (isLink)
		{
			footerDiv.setWidth(dwidth + "px");
			mimeIcon.setWidth(dwidth - 120 + "px");
		}

		prevImg.setWidth(dwidth + "px");

		fLabel.setWidth(dwidth - 30 + "px");
	}

	public String getfName()
	{
		return fName;
	}

	public void setfName(String fName)
	{
		this.fName = fName;
	}
	
	/**
	 * Constructor for dmsContent is not a version
	 * 
	 * @param content
	 * @param image
	 * @param isLink
	 * @param DMSAssociation
	 */
	public DMSViewerComponent(I_DMS_Content content, AImage image, boolean isLink, I_DMS_Association DMSAssociation)
	{
		this(content, image, isLink, DMSAssociation, null);
	}
	

	/**
	 * Constructor for dmsContent is a version 
	 * 
	 * @param content
	 * @param image
	 * @param isLink
	 * @param DMSAssociation
	 * @param version
	 */
	public DMSViewerComponent(I_DMS_Content content, AImage image, boolean isLink, I_DMS_Association DMSAssociation, String version)
	{
		String name = content.getName();
		if (content.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
			name = name.replace("\\(.*\\d\\)", "");

		if (name.contains("(") && name.contains(")"))
		{
			name = name.replace(name.substring(name.lastIndexOf("("), name.lastIndexOf(")") + 1), "");
		}
		
		/*
		 * Append version number if exist
		 */
		
		if (version != null)
		{
			name = name + " (V" + version + ")";
		}

		this.contentBaseType = content.getContentBaseType();
		this.fName = name;
		this.DMSContent = (MDMSContent) content;
		this.isLink = isLink;
		this.DMSAssociation = (MDMSAssociation) DMSAssociation;

		if (isLink)
		{
			try
			{
				linkImage = new Image();
				linkImage.setContent(Utils.getImage("Link16.png"));

				mimeIcon.appendChild(linkImage);
				mimeIcon.setStyle("float :left;");
				ZkCssHelper.appendStyle(mimeIcon, "align: left");

				footerDiv.appendChild(mimeIcon);
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Link image fetching failure: ", e);
				throw new AdempiereException("Link image fetching failure: " + e);
			}

		}

		prevImg = new Image();
		prevImg.setContent(image);

		dImage.appendChild(prevImg);
		dImage.setTooltiptext(Utils.getToolTipTextMsg(content));
		vbox.appendChild(dImage);

		fLabel.appendChild(new Label(fName));
		fLabel.setTooltiptext(content.getName());

		footerDiv.appendChild(fLabel);

		vbox.appendChild(footerDiv);

		fLabel.setStyle("text-overflow: ellipsis; white-space: nowrap; overflow: hidden; float: right;");

		ZkCssHelper.appendStyle(dImage, "text-align: center");

		if (content.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
		{
			ZkCssHelper.appendStyle(fLabel, "text-align: center");
		}

		this.appendChild(vbox);
		this.setStyle("background-color: #ffffff;padding: 8px; ");

		ZkCssHelper.appendStyle(this, "text-align: center");
	}
	
	public MDMSAssociation getDMSAssociation()
	{
		return DMSAssociation;
	}

	public void setDMSAssociation(MDMSAssociation dMSAssociation)
	{
		DMSAssociation = dMSAssociation;
	}
}
