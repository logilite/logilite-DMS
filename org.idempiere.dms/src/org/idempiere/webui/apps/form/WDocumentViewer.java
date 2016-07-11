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

package org.idempiere.webui.apps.form;

import java.io.File;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Window;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IContentEditor;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSMimeType;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Splitter;

public class WDocumentViewer extends Window
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7234966943628502177L;
	public static CLogger		log					= CLogger.getCLogger(WDocumentViewer.class);

	private Tabbox				tabBox				= null;
	private Tabpanel			tabDataPanel		= new Tabpanel();
	private MDMSContent			mDMSContent			= null;
	private MDMSMimeType		mimeType			= null;
	private File				document_preview	= null;

	private WDAttributePanel	attributePanel		= null;

	private int					tableID				= 0;
	private int					recordID			= 0;

	public WDocumentViewer(Tabbox tabBox, File document_preview, MDMSContent mdms_content, int tableID, int recordID)
	{
		mimeType = new MDMSMimeType(Env.getCtx(), mdms_content.getDMS_MimeType_ID(), null);
		this.tabBox = tabBox;
		this.mDMSContent = mdms_content;
		this.document_preview = document_preview;
		this.tableID = tableID;
		this.recordID = recordID;
	}

	public WDAttributePanel getAttributePanel()
	{
		return attributePanel;
	}

	public Tabpanel initForm()
	{
		this.setHeight("100%");
		this.setWidth("100%");

		Hbox boxViewSeparator = new Hbox();
		boxViewSeparator.setWidth("100%");
		boxViewSeparator.setHeight("100%");

		Cell cellPreview = new Cell();
		cellPreview.setWidth("70%");

		IContentEditor contentEditor = Utils.getContentEditor(mimeType.getMimeType());

		if (contentEditor != null)
		{
			contentEditor.setFile(document_preview);
			contentEditor.setContent(mDMSContent);
		}
		else
			throw new AdempiereException("No Content Editor found.");

		cellPreview.appendChild(contentEditor.initPanel());
		Splitter splitter = new Splitter();
		splitter.setCollapse("after");
		
		Cell cellCPreview = new Cell();
		cellCPreview.setWidth("30%");
		attributePanel = new WDAttributePanel(mDMSContent, tabBox, tableID, recordID);
		cellCPreview.appendChild(attributePanel);

		boxViewSeparator.setStyle("position:relative; overflow: auto;");
		boxViewSeparator.appendChild(cellPreview);
		boxViewSeparator.appendChild(splitter);
		boxViewSeparator.appendChild(cellCPreview);

		tabDataPanel.setStyle("position:relative; overflow: auto;");
		tabDataPanel.setZclass("none");
		tabDataPanel.appendChild(boxViewSeparator);
		return tabDataPanel;
	}
}
