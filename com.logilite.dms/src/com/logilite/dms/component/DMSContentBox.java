package com.logilite.dms.component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.desktop.IDesktop;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.window.FDialog;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment.WorkbookNotFoundException;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;

import com.logilite.dms.DMS;
import com.logilite.dms.DMS_ZK_Util;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IContentEditor;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSMimeType;
import com.logilite.dms.model.MDMSVersion;
import com.logilite.dms.util.DMSConvertToPDFUtils;
import com.logilite.dms.util.DMSFactoryUtils;

/**
 * DMS Content Box as component for individual content viewer
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class DMSContentBox extends Hbox implements EventListener<Event>
{
	private static final long	serialVersionUID	= 1L;
	private static CLogger		log					= CLogger.getCLogger(DMSContentBox.class);

	private int					tableID				= 0;
	private int					recordID			= 0;
	private int					contentID			= 0;
	private int					windowNo			= 0;

	private String				title;
	private DMS					dms;
	private MDMSContent			content;
	private MDMSVersion			version;

	/**
	 * Constructor
	 * 
	 * @param title
	 */
	public DMSContentBox(String title)
	{
		this.title = Util.isEmpty(title, true) ? "Content" : title;
		this.setHeight((DMSConstant.CONTENT_GALLERY_ICON_WIDTH + 15) + "px");
	} // DMSContentBox

	/**
	 * Constructor
	 * 
	 * @param title
	 * @param tableID
	 * @param recordID
	 * @param contentID
	 */
	public DMSContentBox(String title, int tableID, int recordID, int contentID)
	{
		this(title);
		this.tableID = tableID;
		this.recordID = recordID;
		this.contentID = contentID;
	} // DMSContentBox

	/**
	 * Render Content Viewer
	 */
	public void renderViewer()
	{
		if (tableID > 0 && recordID > 0)
		{
			String tableName = MTable.getTableName(Env.getCtx(), tableID);

			dms = new DMS(Env.getAD_Client_ID(Env.getCtx()), tableName);
			dms.initiateMountingContent(tableName, recordID, tableID);

			content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(contentID, null);
			version = (MDMSVersion) MDMSVersion.getLatestVersion(content, false);

			// Content Thumbnail
			Image thumbImg = new Image();
			thumbImg.setContent(DMS_ZK_Util.getThumbImageForVersion(dms, version, "150"));
			thumbImg.setStyle("width: 100%; max-width: 30px; max-height: 30px;");
			thumbImg.setSclass("SB-THUMBIMAGE");

			this.appendChild(thumbImg);
			this.setTooltiptext(content.getToolTipTextMsg());

			if (MDMSContent.CONTENTBASETYPE_Content.equals(content.getContentBaseType()))
			{
				this.addEventListener(Events.ON_CLICK, this);
			}
		}
	} // renderViewer

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (Events.ON_CLICK.equals(event.getName()))
		{
			openContentViewer();
		}
	} // onEvent

	public void openContentViewer() throws FileNotFoundException, IOException
	{
		MDMSMimeType mimeType = (MDMSMimeType) content.getDMS_MimeType();
		File documentToPreview = dms.getFileFromStorage(version);
		if (documentToPreview != null)
		{
			try
			{
				documentToPreview = DMSConvertToPDFUtils.convertDocToPDF(documentToPreview, mimeType);
			}
			catch (Exception e)
			{
				if (e.getCause() instanceof WorkbookNotFoundException)
				{
					// Ignore specific exception
				}
				else
				{
					String errorMsg = "Whoops! There was a problem previewing this document. \n Due to exception: " + e.getLocalizedMessage();
					log.log(Level.SEVERE, errorMsg, e);
					FDialog.warn(windowNo, errorMsg, "Document preview issue...");
					return;
				}
			}

			if (DMSFactoryUtils.getContentEditor(mimeType.getMimeType()) != null)
			{
				IContentEditor contentEditor = DMSFactoryUtils.getContentEditor(mimeType.getMimeType());

				if (contentEditor != null)
				{
					contentEditor.setFile(documentToPreview);
					contentEditor.setContent(content);
				}

				// Create a modal window for content preview
				Window window = new Window();
				window.setTitle(title);
				window.setWidth("100%");
				window.setHeight("100%");
				window.setBorder("normal");
				window.setClosable(true);
				window.setStyle("position: relative; overflow: auto;");

				// Add the content editor to the window
				Component previewPanel = contentEditor.initPanel();
				window.appendChild(previewPanel);
				window.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);

				int windowNo = SessionManager.getAppDesktop().registerWindow(window);
				window.setAttribute(IDesktop.WINDOWNO_ATTRIBUTE, windowNo);
				AEnv.showWindow(window);
			}
			else
			{
				FDialog.warn(	windowNo, "Not able to preview for this content, Please download it...",
								"Document preview issue...");
			}
		}
		else
		{
			FDialog.error(	windowNo, this, "ContentNotFoundInStorage", dms.getPathFromContentManager(version),
							"Content Not Found In the Storage");
		}
	} // openContentViewer

	public void setTableID(int tableID)
	{
		this.tableID = tableID;
	} // setTableID

	public void setRecordID(int recordID)
	{
		this.recordID = recordID;
	} // setRecordID

	public void setContent_ID(Integer contentID)
	{
		this.contentID = contentID;
	} // setContent_ID

	public int getContent_ID()
	{
		return contentID;
	} // getContent_ID

}
