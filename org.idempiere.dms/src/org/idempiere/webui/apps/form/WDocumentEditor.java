package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Window;
import org.compiere.util.CLogger;
import org.idempiere.model.MDMSContent;
import org.zkoss.util.media.AMedia;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Iframe;

public class WDocumentEditor extends Window
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7234966943628502177L;
	public static CLogger		log					= CLogger.getCLogger(WDocumentEditor.class);

	private WDocumentViewer		viewer				= null;
	private Tabpanel			tabDataPanel		= null;
	private MDMSContent			mDMSContent			= null;

	public WDocumentEditor(WDocumentViewer viewer, File document_preview, Tabpanel tabDataPanel,
			MDMSContent mdms_content)
	{
		this.viewer = viewer;
		this.tabDataPanel = tabDataPanel;
		this.mDMSContent = mdms_content;

		intiform(document_preview);
	}

	private void intiform(File document_preview)
	{
		Cell cellAttribute = new Cell();
		cellAttribute.setWidth("30%");

		Hbox boxViewSeparator = new Hbox();
		boxViewSeparator.setWidth("100%");
		boxViewSeparator.setHeight("100%");

		Iframe iframeContentPriview = new Iframe();

		AMedia media = null;
		try
		{
			media = new AMedia(document_preview, null, null);
		}
		catch (FileNotFoundException e)
		{
			log.log(Level.SEVERE, "Document cannot be displayed:" + e.getLocalizedMessage());
			throw new AdempiereException("Document cannot be displayed:" + e.getLocalizedMessage());
		}

		iframeContentPriview.setContent(null);
		iframeContentPriview.setContent(media);

		iframeContentPriview.setWidth("100%");
		iframeContentPriview.setHeight("100%");
		iframeContentPriview.setStyle("overflow: auto;");

		Cell cellPreview = new Cell();
		cellPreview.setWidth("70%");
		cellPreview.appendChild(iframeContentPriview);

		Cell cellCPreview = new Cell();
		cellCPreview.setWidth("30%");
		cellCPreview.appendChild(new WDAttributePanel(mDMSContent, viewer));

		boxViewSeparator.appendChild(cellPreview);
		boxViewSeparator.appendChild(cellCPreview);

		viewer.setStyle("width: 100%; height:100%; overflow: auto;");

		tabDataPanel.appendChild(boxViewSeparator);
		viewer.tabPanels.appendChild(tabDataPanel);
		this.setHeight("100%");
		this.setWidth("100%");
		viewer.appendChild(viewer.tabBox);
	}
}
