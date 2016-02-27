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
	private IContentEditor		contentEditor		= null;

	public WDocumentViewer(Tabbox tabBox, File document_preview, MDMSContent mdms_content)
	{
		MDMSMimeType mimeType = new MDMSMimeType(Env.getCtx(), mdms_content.getDMS_MimeType_ID(), null);
		contentEditor = Utils.getContentEditor(mimeType.getMimeType());
		
		if(contentEditor!=null)
		{
			contentEditor.setFile(document_preview);
			contentEditor.setContent(mdms_content);
		}
		else
			throw new AdempiereException("No Content Editor found.");
		
		this.tabBox = tabBox;
		this.mDMSContent = mdms_content;
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
		cellPreview.appendChild(contentEditor.initPanel());

		Cell cellCPreview = new Cell();
		cellCPreview.setWidth("30%");
		cellCPreview.appendChild(new WDAttributePanel(mDMSContent, tabBox));

		boxViewSeparator.appendChild(cellPreview);
		boxViewSeparator.appendChild(cellCPreview);

		tabDataPanel.appendChild(boxViewSeparator);
		return tabDataPanel;
	}
}
