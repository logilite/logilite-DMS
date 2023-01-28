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

package com.logilite.dms.form;

import java.io.File;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Window;
import org.compiere.util.Env;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.South;
import org.zkoss.zul.Splitter;

import com.logilite.dms.DMS;
import com.logilite.dms.factories.IContentEditor;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSMimeType;
import com.logilite.dms.util.DMSFactoryUtils;

public class WDocumentViewer extends Window
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7234966943628502177L;

	private Tabbox				tabBox				= null;
	private Tabpanel			tabDataPanel		= new Tabpanel();

	private File				document_preview	= null;

	private DMS					dms;
	private MDMSContent			mDMSContent			= null;
	private MDMSMimeType		mimeType			= null;
	private WDMSAttributePanel	attributePanel		= null;

	private int					tableID				= 0;
	private int					recordID			= 0;
	private int					windowNo			= 0;
	private int					tabNo				= 0;

	public WDocumentViewer(DMS dms, Tabbox tabBox, File document_preview, MDMSContent mdms_content, int tableID, int recordID, int windowNo, int tabNo)
	{
		mimeType = new MDMSMimeType(Env.getCtx(), mdms_content.getDMS_MimeType_ID(), null);
		this.dms = dms;
		this.tabBox = tabBox;
		this.mDMSContent = mdms_content;
		this.document_preview = document_preview;
		this.tableID = tableID;
		this.recordID = recordID;
		this.windowNo = windowNo;
		this.tabNo = tabNo;
	}

	public WDMSAttributePanel getAttributePanel()
	{
		return attributePanel;
	}

	public Tabpanel initForm(boolean isWindowAccess, boolean isMountingBaseStructure, boolean isLink)
	{
		this.setHeight("100%");
		this.setWidth("100%");

		IContentEditor contentEditor = DMSFactoryUtils.getContentEditor(mimeType.getMimeType());

		if (contentEditor != null)
		{
			contentEditor.setFile(document_preview);
			contentEditor.setContent(mDMSContent);
		}
		else
			throw new AdempiereException("No Content Editor found.");

		// Content view
		Cell cellPreview = new Cell();
		cellPreview.setWidth("70%");
		cellPreview.appendChild(contentEditor.initPanel());

		Splitter splitter = new Splitter();
		splitter.setCollapse("after");

		// Content attribute view
		Cell cellCPreview = new Cell();
		cellCPreview.setWidth("30%");
		attributePanel = new WDMSAttributePanel(dms, mDMSContent, tabBox, tableID, recordID, isWindowAccess, isMountingBaseStructure, isLink, windowNo, tabNo);
		cellCPreview.appendChild(attributePanel);

		if (ClientInfo.isMobile())
		{
			Borderlayout borderViewSeparator = new Borderlayout();
			borderViewSeparator.setWidth("100%");
			borderViewSeparator.setHeight("100%");
			borderViewSeparator.setStyle("position:relative; overflow: auto;");
			borderViewSeparator.appendCenter(cellPreview);
			borderViewSeparator.appendSouth(cellCPreview);

			South south = borderViewSeparator.getSouth();
			south.setZclass("SB-south " + south.getZclass());
			south.setStyle("max-height: 100%; min-height: 80%;");
			south.setSplittable(true);
			south.setCollapsible(true);
			south.setOpen(false);
			tabDataPanel.appendChild(borderViewSeparator);
		}
		else
		{
			Hbox boxViewSeparator = new Hbox();
			boxViewSeparator.setWidth("100%");
			boxViewSeparator.setHeight("100%");
			boxViewSeparator.setStyle("position:relative; overflow: auto;");
			boxViewSeparator.appendChild(cellPreview);
			boxViewSeparator.appendChild(splitter);
			boxViewSeparator.appendChild(cellCPreview);
			tabDataPanel.appendChild(boxViewSeparator);
		}

		tabDataPanel.setStyle("position:relative; overflow: auto;");
		tabDataPanel.setZclass("none");
		return tabDataPanel;
	} // initForm
}
