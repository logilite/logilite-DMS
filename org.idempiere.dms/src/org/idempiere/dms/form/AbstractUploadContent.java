package org.idempiere.dms.form;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IDMSUploadContent;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

/**
 * Abstract for upload content
 * 
 * @author Sachin Bhimani
 */
public abstract class AbstractUploadContent extends Window implements EventListener<Event>, ValueChangeListener, IDMSUploadContent
{
	/**
	 * 
	 */
	private static final long						serialVersionUID			= 4978616583376061583L;

	protected static CLogger						log							= CLogger.getCLogger(AbstractUploadContent.class);

	public static final String						BTN_ID_UPLOAD_CANCEL_PREFIX	= "UploadCancel_";
	public static final String						ATTRIB_ROW_REF				= "ATTRIB_ROW_REF";
	public static final String						ATTRIB_ROW_NO				= "ATTRIB_ROW_NO";

	protected DMS									dms;
	protected MDMSContent							content						= null;

	protected boolean								isVersion					= false;
	protected boolean								isCancel					= false;

	protected int									tableID						= 0;
	protected int									recordID					= 0;
	protected int									windowNo					= 0;
	protected int									rowCount					= 0;
	protected int									tabNo						= 0;
	protected ArrayList<Integer>					contentIDs					= new ArrayList<Integer>();

	protected ConfirmPanel							confirmPanel				= new ConfirmPanel(true);
	protected Button								btnFileUpload				= new Button();
	protected Button								btnClose					= null;
	protected Button								btnOk						= null;

	protected Label									lblContentType				= new Label();
	protected Borderlayout							mainLayout					= new Borderlayout();
	protected Grid									gridView					= GridFactory.newGridLayout();
	protected Row									contentTypeRow				= new Row();

	protected Tabbox								tabBoxAttribute				= new Tabbox();
	protected Tabs									tabsAttribute				= new Tabs();
	protected Tab									tabAttribute				= new Tab();
	protected Tabpanels								tabPanelsAttribute			= new Tabpanels();
	protected Tabpanel								tabPanelAttribute			= new Tabpanel();

	protected WDLoadASIPanel						asiPanel					= null;
	protected WTableDirEditor						editorContentType;

	protected Rows									rows;

	/*
	 * Map used for adding/uploading multiple content as same time to track the required data.
	 * Concurrent map - for one by one content is added to DMS then remove that content from map. If
	 * in case its failed from DMS side in between then no need to do repeat already uploaded
	 * content again.
	 * Map data like: < RowNo, [ row, media, txtName, txtDesc, others if any ] >
	 */
	protected ConcurrentHashMap<Integer, Object[]>	mapUploadInfo				= new ConcurrentHashMap<Integer, Object[]>();

	/***
	 * Constructor
	 * 
	 * @param dms
	 * @param content
	 * @param isVersion
	 * @param tableID
	 * @param recordID
	 * @param windowNo
	 * @param tabNo
	 */
	public AbstractUploadContent(DMS dms, I_DMS_Content content, boolean isVersion, int tableID, int recordID, int windowNo, int tabNo)
	{
		this.dms = dms;
		this.content = (MDMSContent) content;
		this.isVersion = isVersion;
		this.tableID = tableID;
		this.recordID = recordID;
		this.windowNo = windowNo;
		this.tabNo = tabNo;

		//
		init();
	}

	protected abstract void init();

	protected Textbox createTextbox(String name, String placeholder, boolean isForDesc)
	{
		Textbox txtbox = new Textbox();
		txtbox.setPlaceholder(placeholder);
		txtbox.setWidth("100%");
		txtbox.setValue(name);
		if (isForDesc)
		{
			txtbox.setMultiline(true);
			txtbox.setRows(2);
		}
		return txtbox;
	} // createTextbox

	@Override
	public void valueChange(ValueChangeEvent event)
	{
		if (event.getSource().equals(editorContentType))
		{
			Components.removeAllChildren(tabPanelAttribute);
			tabBoxAttribute.setVisible(false);
			//
			loadASIPanel();
		}
	}

	protected void loadASIPanel()
	{
		if (editorContentType.getValue() != null)
		{
			asiPanel = new WDLoadASIPanel((int) editorContentType.getValue(), 0, windowNo, tabNo);
			tabPanelAttribute.appendChild(asiPanel);
			tabBoxAttribute.setVisible(true);
		}
	} // loadASIPanel

	protected void setBtnLabelFileUpload()
	{
		if (mapUploadInfo.size() <= 0)
			btnFileUpload.setLabel(DMSConstant.MSG_UPLOAD_CONTENT);
		else
			btnFileUpload.setLabel(DMSConstant.MSG_UPLOAD_CONTENT + " [ " + mapUploadInfo.size() + " content(s) ] ");
	} // setBtnLabelFileUpload

	public void saveToDMS(File uploadFile, String contentName, String description)
	{
		// Adding File to DMS
		if (isVersion)
		{
			dms.addFileVersion(content, uploadFile, description, tableID, recordID);
		}
		else
		{
			int ASI_ID = 0;
			int cTypeID = 0;

			if (editorContentType.getValue() != null)
			{
				cTypeID = (int) editorContentType.getValue();
				ASI_ID = asiPanel.saveAttributes();
				asiPanel.setASIID(0);
			}

			int contentID = dms.addFile(content, uploadFile, contentName, description, cTypeID, ASI_ID, tableID, recordID);
			if (content != null && !content.isMounting() && content.getDMS_Content_ID() != contentID)
			{
				MDMSContent uploadedContent = (MDMSContent) MTable.get(content.getCtx(), MDMSContent.Table_ID).getPO(contentID, content.get_TrxName());
				dms.grantChildPermissionFromParentContent(uploadedContent, content);
			}
			// Add id in list for returning
			contentIDs.add(contentID);
		}
	} // saveToDMS

	/**
	 * @return true if dialog cancel by user
	 */
	public boolean isCancel()
	{
		return isCancel;
	}

	/**
	 * @return content ID of the uploaded document
	 */
	public Integer[] getUploadedDocContentIDs()
	{
		return contentIDs.toArray(new Integer[contentIDs.size()]);
	}
}
