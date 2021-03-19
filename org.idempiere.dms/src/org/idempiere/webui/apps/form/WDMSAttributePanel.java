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

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.componenet.AbstractComponentIconViewer;
import org.idempiere.dms.DMS;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.util.DMSFactoryUtils;
import org.idempiere.dms.util.Utils;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.idempiere.model.MDMSVersion;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.South;

public class WDMSAttributePanel extends Panel implements EventListener<Event>, ValueChangeListener
{

	/**
	 * 
	 */
	private static final long	serialVersionUID		= 5200959427619624094L;
	private static CLogger		log						= CLogger.getCLogger(WDMSAttributePanel.class);

	private Panel				panelAttribute			= new Panel();
	private Panel				panelFooterButtons		= new Panel();
	private Borderlayout		mainLayout				= new Borderlayout();

	private Tabbox				tabBoxAttribute			= new Tabbox();
	private Tabs				tabsAttribute			= new Tabs();
	private Tab					tabAttribute			= new Tab();
	private Tab					tabVersionHistory		= new Tab();

	private Tabpanels			tabpanelsAttribute		= new Tabpanels();
	private Tabpanel			tabpanelAttribute		= new Tabpanel();
	private Tabpanel			tabpanelVersionHitory	= new Tabpanel();

	private Grid				gridAttributeLayout		= new Grid();
	private Grid				grid					= new Grid();

	private Row					contentTypeRow			= null;

	private Button				btnDelete				= null;
	private Button				btnRequery				= null;
	private Button				btnClose				= null;
	private Button				btnDownload				= null;
	private Button				btnEdit					= null;
	private Button				btnSave					= null;
	private Button				btnVersionUpload		= null;

	private Label				lblStatus				= null;
	private Label				lblName					= null;
	private Label				lblDesc					= null;
	private Label				lblContentType			= null;

	private Textbox				txtName					= null;
	private Textbox				txtDesc					= null;

	private DMS					dms;
	private MDMSContent			content					= null;
	private MDMSContent			parentContent			= null;
	private MDMSVersion			versionSelected			= null;

	private Tabbox				tabBox					= null;
	private ConfirmPanel		confirmPanel			= null;
	private WDLoadASIPanel		ASIPanel				= null;

	private int					tableId					= 0;
	private int					recordId				= 0;
	private int					contentTypeID			= 0;

	private boolean				isWindowAccess			= true;
	private boolean				isMountingBaseStructure	= false;
	private boolean				isLink					= false;

	private WTableDirEditor		editorContentType		= null;

	private int					windowNo				= 0;
	private int					tabNo					= 0;

	public WDMSAttributePanel(	DMS dms, I_DMS_Content content, Tabbox tabBox, int tableID, int recordID, boolean isWindowAccess, boolean isMountingBaseStructure,
								boolean isLink, int windowNo,
								int tabNo)
	{
		this.dms = dms;
		this.tabBox = tabBox;
		this.tableId = tableID;
		this.recordId = recordID;
		this.content = (MDMSContent) content;
		this.isWindowAccess = isWindowAccess;
		this.isMountingBaseStructure = isMountingBaseStructure;
		this.isLink = isLink;
		this.windowNo = windowNo;
		this.tabNo = tabNo;

		try
		{
			init();
			this.contentTypeID = content.getDMS_ContentType_ID();
			refreshPanel();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Issue while opening content attribute panel. " + e.getLocalizedMessage(), e);
		}
	} // Constructor

	/**
	 * initialize components
	 */
	private void init()
	{
		this.appendChild(grid);
		grid.setHeight("100%");
		grid.setWidth("100%");
		grid.setZclass("none");
		panelAttribute.setZclass("none");

		this.setHeight("100%");
		this.setWidth("100%");

		Columns columns = new Columns();
		Rows rows = new Rows();

		Column column = new Column();
		columns.appendChild(column);

		Row row = new Row();
		row.appendChild(panelAttribute);
		rows.appendChild(row);
		grid.appendChild(columns);
		grid.appendChild(rows);

		lblStatus = new Label();
		ZkCssHelper.appendStyle(lblStatus, "font-weight: bold;");
		ZkCssHelper.appendStyle(lblStatus, "align: center;");
		lblStatus.setValue(MUser.getNameOfUser(content.getUpdatedBy()) + " edited at " + DMSConstant.SDF.format(content.getUpdated()));

		panelAttribute.appendChild(lblStatus);
		tabBoxAttribute.appendChild(tabsAttribute);
		tabBoxAttribute.appendChild(tabpanelsAttribute);
		panelAttribute.appendChild(tabBoxAttribute);
		tabBoxAttribute.setMold("accordion");
		tabBoxAttribute.setHeight("98%");
		tabBoxAttribute.setWidth("100%");

		tabsAttribute.appendChild(tabAttribute);
		tabsAttribute.appendChild(tabVersionHistory);

		tabAttribute.setLabel(DMSConstant.MSG_ATTRIBUTES);
		tabAttribute.setWidth("100%");
		tabVersionHistory.setLabel(DMSConstant.MSG_VERSION_HISTORY);

		tabpanelsAttribute.appendChild(tabpanelAttribute);
		tabpanelAttribute.setStyle("overflow: auto;");
		tabpanelsAttribute.setHeight("98%");
		tabpanelsAttribute.setWidth("100%");

		tabpanelsAttribute.appendChild(tabpanelVersionHitory);
		if (ClientInfo.maxHeight(500))
			ZKUpdateUtil.setWindowHeightX(tabpanelVersionHitory, (ClientInfo.get().desktopHeight - 100));
		else
			tabpanelVersionHitory.setHeight("500px");
		tabpanelVersionHitory.setWidth("100%");

		tabpanelAttribute.appendChild(gridAttributeLayout);
		tabVersionHistory.setWidth("100%");

		columns = new Columns();
		column = new Column();

		rows = new Rows();
		row = new Row();

		gridAttributeLayout.appendChild(columns);
		gridAttributeLayout.appendChild(rows);

		column = new Column();
		column.setWidth("30%");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("70%");
		columns.appendChild(column);

		confirmPanel = new ConfirmPanel();
		btnDelete = confirmPanel.createButton(ConfirmPanel.A_DELETE);
		btnDelete.setEnabled(false);

		btnDownload = confirmPanel.createButton(ConfirmPanel.A_EXPORT);
		btnDownload.setTooltiptext(DMSConstant.TTT_DOWNLOAD);

		btnVersionUpload = new Button();
		btnVersionUpload.setTooltiptext(DMSConstant.TTT_UPLOAD_VERSION);
		DMS_ZK_Util.setFontOrImageAsIcon("UploadVersion", btnVersionUpload);

		btnRequery = confirmPanel.createButton(ConfirmPanel.A_REFRESH);

		btnClose = confirmPanel.createButton(ConfirmPanel.A_CANCEL);
		btnClose.setStyle("float:right;");

		btnEdit = new Button();
		btnEdit.setTooltiptext(DMSConstant.TTT_EDIT);
		DMS_ZK_Util.setFontOrImageAsIcon("Edit", btnEdit);

		btnSave = new Button();
		btnSave.setVisible(false);
		btnSave.setTooltiptext(DMSConstant.TTT_SAVE);
		DMS_ZK_Util.setFontOrImageAsIcon("Save", btnSave);

		btnSave.addEventListener(Events.ON_CLICK, this);
		btnDelete.addEventListener(Events.ON_CLICK, this);
		btnDownload.addEventListener(Events.ON_CLICK, this);
		btnRequery.addEventListener(Events.ON_CLICK, this);
		btnClose.addEventListener(Events.ON_CLICK, this);
		btnEdit.addEventListener(Events.ON_CLICK, this);
		btnVersionUpload.addEventListener(Events.ON_CLICK, this);

		South south = new South();
		rows.appendChild(row);

		panelFooterButtons.appendChild(btnVersionUpload);
		panelFooterButtons.appendChild(btnDelete);
		panelFooterButtons.appendChild(btnRequery);
		panelFooterButtons.appendChild(btnDownload);
		panelFooterButtons.appendChild(btnClose);
		panelFooterButtons.setStyle("display: inline-flex; padding-top: 5px;");

		panelAttribute.appendChild(panelFooterButtons);
		mainLayout.appendChild(south);

		btnVersionUpload.setDisabled(!isWindowAccess || isMountingBaseStructure || content == null || !content.isActive());
		btnEdit.setDisabled(!isWindowAccess || isMountingBaseStructure || isLink);
	} // init

	/**
	 * initialize version history components
	 */
	private void initVersionHistory()
	{
		Components.removeAllChildren(tabpanelVersionHitory);
		Grid versionGrid = new Grid();
		versionGrid.setHeight("100%");
		versionGrid.setWidth("100%");
		versionGrid.setStyle("position:relative; float: right; overflow-y: auto;");

		this.setZclass("none");
		this.setStyle("position:relative; float: right; height: 100%; overflow: auto;");

		tabpanelVersionHitory.appendChild(versionGrid);

		MDMSAssociation dmsAssociation = dms.getParentAssociationFromContent(content.getDMS_Content_ID(), content.isActive());

		HashMap<I_DMS_Version, I_DMS_Association> contentsMap = new HashMap<I_DMS_Version, I_DMS_Association>();
		List<MDMSVersion> contentVersions = MDMSVersion.getVersionHistory(content);
		for (MDMSVersion contentVersion : contentVersions)
		{
			contentsMap.put(contentVersion, dmsAssociation);
		}

		String[] eventsList = new String[] { Events.ON_CLICK, Events.ON_DOUBLE_CLICK };

		AbstractComponentIconViewer viewerComponent = (AbstractComponentIconViewer) DMSFactoryUtils.getDMSComponentViewer(DMSConstant.ICON_VIEW_VERSION);
		viewerComponent.init(	dms, contentsMap, versionGrid, DMSConstant.CONTENT_LARGE_ICON_WIDTH - 30, DMSConstant.CONTENT_LARGE_ICON_HEIGHT - 30, this,
								eventsList);

	} // initVersionHistory

	private void initAttributes(boolean isEdit)
	{
		Components.removeAllChildren(tabpanelAttribute);
		Grid commGrid = GridFactory.newGridLayout();

		Rows rows = new Rows();

		commGrid.appendChild(rows);

		txtName = new Textbox();
		txtDesc = new Textbox();

		lblName = new Label(DMSConstant.MSG_NAME);
		lblDesc = new Label(DMSConstant.MSG_DESCRIPTION);

		txtName.setWidth("100%");
		txtDesc.setWidth("100%");

		parentContent = new MDMSContent(Env.getCtx(), content.getDMS_Content_Related_ID(), null);
		txtName.setValue(parentContent.getName().substring(0, parentContent.getName().lastIndexOf(".")));
		txtDesc.setValue(content.getDescription());
		txtName.setMaxlength(DMSConstant.MAX_FILENAME_LENGTH);

		txtName.setEnabled(isEdit);
		txtDesc.setEnabled(isEdit);

		lblContentType = new Label(DMSConstant.MSG_DMS_CONTENT_TYPE);

		if (editorContentType != null)
			editorContentType.removeValuechangeListener(this);

		int Column_ID = MColumn.getColumn_ID(MDMSContent.Table_Name, MDMSContent.COLUMNNAME_DMS_ContentType_ID);
		MLookup lookup = MLookupFactory.get(Env.getCtx(), windowNo, tabNo, Column_ID, DisplayType.TableDir);
		lookup.refresh();
		editorContentType = new WTableDirEditor(MDMSContentType.COLUMNNAME_DMS_ContentType_ID, false, false, true, lookup);
		editorContentType.setValue(contentTypeID);
		editorContentType.addValueChangeListener(this);

		Row row = new Row();
		row.appendCellChild(lblName);
		row.appendCellChild(txtName, 2);
		rows.appendChild(row);

		row = new Row();
		row.appendCellChild(lblDesc);
		row.appendCellChild(txtDesc, 2);
		rows.appendChild(row);

		contentTypeRow = new Row();
		contentTypeRow.appendCellChild(lblContentType);
		contentTypeRow.appendCellChild(editorContentType.getComponent(), 2);
		contentTypeRow.setVisible(isEdit);
		rows.appendChild(contentTypeRow);

		tabpanelAttribute.appendChild(commGrid);

		ASIPanel = new WDLoadASIPanel(contentTypeID, content.getM_AttributeSetInstance_ID());
		ASIPanel.setEditableAttribute(isEdit);
		ASIPanel.appendChild(btnEdit);
		ASIPanel.appendChild(btnSave);
		btnSave.setVisible(isEdit);
		tabpanelAttribute.appendChild(ASIPanel);
	} // initAttributes

	/*
	 * (non-Javadoc)
	 * @see
	 * org.zkoss.zk.ui.event.EventListener#onEvent(org.zkoss.zk.ui.event.Event)
	 */
	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget().equals(btnEdit))
		{
			txtName.setEnabled(true);
			txtDesc.setEnabled(true);
			ASIPanel.setEditableAttribute(true);
			btnSave.setVisible(true);
			btnEdit.setVisible(false);
			contentTypeRow.setVisible(true);
			if (editorContentType != null)
				editorContentType.addValueChangeListener(this);
		}
		else if (event.getTarget().equals(btnSave))
		{
			boolean isContentSave = false;
			if (content.getDMS_ContentType_ID() != contentTypeID)
			{
				isContentSave = true;

				//
				int oldAttributeSet_ID = content.getDMS_ContentType_ID() > 0 ? content.getDMS_ContentType().getM_AttributeSet_ID() : 0;
				content.setDMS_ContentType_ID(contentTypeID);
				int newAttributeSet_ID = content.getDMS_ContentType_ID() > 0 ? content.getDMS_ContentType().getM_AttributeSet_ID() : 0;

				//
				if (oldAttributeSet_ID != newAttributeSet_ID)
				{
					MAttributeSet mAttributeSet = new MAttributeSet(Env.getCtx(), oldAttributeSet_ID, null);
					MAttribute[] attributes = mAttributeSet.getMAttributes(false);
					for (MAttribute att : attributes)
					{
						MAttributeInstance attInstance = att.getMAttributeInstance(content.getM_AttributeSetInstance_ID());
						if (attInstance != null)
							attInstance.deleteEx(false);
					}
				}
			}

			if (!txtName.getValue().equals(parentContent.getName().substring(0, parentContent.getName().lastIndexOf("."))))
			{
				String error = Utils.isValidFileName(txtName.getValue(), false);
				if (!Util.isEmpty(error, true))
					throw new WrongValueException(txtName, error);

				String fileName = txtName.getValue() + "." + FilenameUtils.getExtension(content.getName());
				dms.renameContent(fileName, content);
			}

			int asiID = ASIPanel.saveAttributes();
			ASIPanel.setEditableAttribute(false);
			btnSave.setVisible(false);
			txtName.setEnabled(false);
			txtDesc.setEnabled(false);
			contentTypeRow.setVisible(false);
			btnEdit.setVisible(true);

			if (content.getDMS_ContentType_ID() == 0)
			{
				asiID = 0;
			}

			if (content.getM_AttributeSetInstance_ID() != asiID)
			{
				content.setM_AttributeSetInstance_ID(asiID);
				isContentSave = true;
			}

			if ((Util.isEmpty(content.getDescription()) && !Util.isEmpty(txtDesc.getValue()))
				|| (!Util.isEmpty(content.getDescription()) && !content.getDescription().equals(txtDesc.getValue())))
			{
				content.setDescription(txtDesc.getValue());
				isContentSave = true;
			}

			if (isContentSave)
			{
				content.save();
			}

			content.load(null);

			Events.sendEvent(new Event(DMSConstant.EVENT_ON_RENAME_COMPLETE, this));
			tabBox.setSelectedTab((Tab) tabBox.getSelectedTab());
			tabBox.getSelectedTab().setLabel(content.getName());

			refreshPanel();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			tabBox.getSelectedTab().close();
		}
		else if (event.getTarget().equals(btnDownload))
		{
			// Resolve NPE after file rename and try to download
			content.load(content.get_TrxName());
			//
			if (versionSelected != null)
				DMS_ZK_Util.downloadDocument(dms, versionSelected);
			else
				DMS_ZK_Util.downloadDocument(dms, MDMSVersion.getLatestVersion(content));
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_DELETE))
		{
			dms.deleteContentWithPhysicalDocument(content);

			tabBox.getSelectedTab().close();
		}
		else if (event.getTarget().equals(btnVersionUpload))
		{
			final Tab tab = (Tab) tabBox.getSelectedTab();
			final WDMSAttributePanel panel = this;

			WUploadContent uploadContent = new WUploadContent(dms, content, true, tableId, recordId, windowNo, tabNo);
			uploadContent.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

				@Override
				public void onEvent(Event arg0) throws Exception
				{
					Events.sendEvent(new Event(DMSConstant.EVENT_ON_UPLOAD_COMPLETE, panel));
					tabBox.setSelectedTab(tab);
				}
			});
			uploadContent.addEventListener(Events.ON_CLOSE, this);
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_REFRESH))
		{
			refreshPanel();
		}
		else if (Events.ON_DOUBLE_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(Row.class))
		{
			versionSelected = (MDMSVersion) event.getTarget().getAttribute(DMSConstant.COMP_ATTRIBUTE_VERSION);
			DMS_ZK_Util.downloadDocument(dms, versionSelected);
		}
		else if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(Row.class))
		{
			versionSelected = (MDMSVersion) event.getTarget().getAttribute(DMSConstant.COMP_ATTRIBUTE_VERSION);
		}
	} // onEvent

	/**
	 * 
	 */
	public void refreshPanel()
	{
		initAttributes(false);
		initVersionHistory();
	}

	@Override
	public void valueChange(ValueChangeEvent event)
	{
		if (event.getSource().equals(editorContentType))
		{
			if (editorContentType.getValue() != null)
				contentTypeID = (int) editorContentType.getValue();
			else
				contentTypeID = 0;
			initAttributes(true);
		}
	}

}
