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
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.adwindow.BreadCrumbLink;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Datebox;
import org.adempiere.webui.component.DatetimeBox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Menupopup;
import org.adempiere.webui.component.NumberBox;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Searchbox;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MClientInfo;
import org.compiere.model.MColumn;
import org.compiere.model.MImage;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.idempiere.componenet.DMSViewerComponent;
import org.idempiere.dms.DMS;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.DMSClipboard;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.idempiere.model.MDMSMimeType;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Space;
import org.zkoss.zul.Splitter;
import org.zkoss.zul.Timebox;

import com.lowagie.text.DocumentException;

public class WDMSPanel extends Panel implements EventListener<Event>, ValueChangeListener
{

	private static final long				serialVersionUID		= -6813481516566180243L;
	public static CLogger					log						= CLogger.getCLogger(WDMSPanel.class);

	public Tabbox							tabBox					= new Tabbox();
	private Tabs							tabs					= new Tabs();
	public Tab								tabView					= new Tab(DMSConstant.MSG_EXPLORER);
	public Tabpanels						tabPanels				= new Tabpanels();
	public Tabpanel							tabViewPanel			= new Tabpanel();

	private Grid							grid					= GridFactory.newGridLayout();
	private Grid							gridBreadCrumb			= GridFactory.newGridLayout();
	private Grid							searchgridView			= GridFactory.newGridLayout();

	private BreadCrumbLink					breadCrumbEvent			= null;

	private Rows							breadRows				= new Rows();
	public Row								breadRow				= new Row();

	// View Result Tab
	private Searchbox						vsearchBox				= new Searchbox();

	private Label							lblAdvanceSearch		= new Label(DMSConstant.MSG_ADVANCE_SEARCH);
	private Label							lblDocumentName			= new Label(DMSConstant.MSG_NAME);
	private Label							lblContentType			= new Label(DMSConstant.MSG_CONTENT_TYPE);
	private Label							lblCreated				= new Label(DMSConstant.MSG_CREATED);
	private Label							lblUpdated				= new Label(DMSConstant.MSG_UPDATED);
	private Label							lblContentMeta			= new Label(DMSConstant.MSG_CONTENT_META);
	private Label							lblDescription			= new Label(DMSConstant.MSG_DESCRIPTION);
	private Label							lblCreatedBy			= new Label(DMSConstant.MSG_CREATEDBY);
	private Label							lblUpdatedBy			= new Label(DMSConstant.MSG_UPDATEDBY);
	private Label							lblPositionInfo			= new Label();
	private Label							lblShowBreadCrumb		= null;

	private Datebox							dbCreatedTo				= new Datebox();
	private Datebox							dbCreatedFrom			= new Datebox();
	private Datebox							dbUpdatedTo				= new Datebox();
	private Datebox							dbUpdatedFrom			= new Datebox();

	private ConfirmPanel					confirmPanel			= new ConfirmPanel();

	private Button							btnClear				= confirmPanel.createButton(ConfirmPanel.A_RESET);
	private Button							btnRefresh				= confirmPanel.createButton(ConfirmPanel.A_REFRESH);
	private Button							btnCloseTab				= confirmPanel.createButton(ConfirmPanel.A_CANCEL);
	private Button							btnSearch				= new Button();
	private Button							btnCreateDir			= new Button();
	private Button							btnUploadContent		= new Button();
	private Button							btnBack					= new Button();
	private Button							btnNext					= new Button();

	private Textbox							txtDocumentName			= new Textbox();
	private Textbox							txtDescription			= new Textbox();

	private WTableDirEditor					lstboxContentType		= null;
	private WTableDirEditor					lstboxCreatedBy			= null;
	private WTableDirEditor					lstboxUpdatedBy			= null;
	private Checkbox						chkInActive				= new Checkbox();

	public DMS								dms						= null;
	private MDMSContent						currDMSContent			= null;
	private MDMSContent						nextDMSContent			= null;
	private MDMSContent						copyDMSContent			= null;
	private MDMSContent						dirContent				= null;
	private MDMSAssociation					previousDMSAssociation	= null;

	private Stack<MDMSContent>				selectedDMSContent		= new Stack<MDMSContent>();
	private Stack<MDMSAssociation>			selectedDMSAssociation	= new Stack<MDMSAssociation>();

	//
	private DMSViewerComponent				DMSViewerComp			= null;
	private DMSViewerComponent				prevComponent			= null;
	private WUploadContent					uploadContent			= null;
	private WCreateDirectoryForm			createDirectoryForm		= null;
	private WDLoadASIPanel					asiPanel				= null;

	private Panel							panelAttribute			= new Panel();

	private Menupopup						contentContextMenu		= new Menupopup();
	private Menupopup						canvasContextMenu		= new Menupopup();

	private Menuitem						mnu_versionList			= null;
	private Menuitem						mnu_copy				= null;
	private Menuitem						mnu_createLink			= null;
	private Menuitem						mnu_delete				= null;
	private Menuitem						mnu_associate			= null;
	private Menuitem						mnu_uploadVersion		= null;
	private Menuitem						mnu_rename				= null;
	private Menuitem						mnu_cut					= null;
	private Menuitem						mnu_paste				= null;
	private Menuitem						mnu_download			= null;

	private Menuitem						mnu_canvasCreateLink	= null;
	private Menuitem						mnu_canvasPaste			= null;

	public int								recordID				= 0;
	public int								tableID					= 0;
	public int								windowID				= 0;

	private boolean							isSearch				= false;
	private boolean							isGenericSearch			= false;
	private boolean							isAllowCreateDirectory	= true;
	private boolean							isWindowAccess			= true;

	private ArrayList<DMSViewerComponent>	viewerComponents		= null;
	private ArrayList<WEditor>				m_editors				= new ArrayList<WEditor>();

	private Map<String, Component>			ASI_Value				= new HashMap<String, Component>();

	private AbstractADWindowContent			winContent;

	/**
	 * Constructor initialize
	 */
	public WDMSPanel()
	{
		dms = new DMS(Env.getAD_Client_ID(Env.getCtx()));

		try
		{
			initForm();
			renderViewer();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render Component Problem.", e);
			throw new AdempiereException("Render Component Problem: " + e);
		}
	}

	public WDMSPanel(int Table_ID, int Record_ID, AbstractADWindowContent winContent)
	{
		this();
		this.winContent = winContent;
		this.windowID = winContent.getADWindow().getAD_Window_ID();
		isWindowAccess = MRole.getDefault().getWindowAccess(windowID);

		setTable_ID(Table_ID);
		setRecord_ID(Record_ID);

		dms.initMountingStrategy(null);
		currDMSContent = dms.getRootContent(Table_ID, Record_ID);

		/*
		 * Navigation and createDir buttons are disabled based on
		 * "IsAllowCreateDirectory" check on client info.
		 */
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), Env.getAD_Client_ID(Env.getCtx()));
		isAllowCreateDirectory = clientInfo.get_ValueAsBoolean("IsAllowCreateDirectory");
		if (isTabViewer() && !isAllowCreateDirectory)
		{
			btnBack.setEnabled(false);
			btnNext.setEnabled(false);
			btnCreateDir.setEnabled(false);
		}

		btnCreateDir.setEnabled(isWindowAccess);
		btnUploadContent.setEnabled(isWindowAccess);
	}

	public int getRecord_ID()
	{
		return recordID;
	}

	public void setRecord_ID(int record_ID)
	{
		this.recordID = record_ID;
	}

	public int getTable_ID()
	{
		return tableID;
	}

	public void setWindow_ID(int AD_Window_ID)
	{
		this.windowID = AD_Window_ID;
	}

	public int getWindow_ID()
	{
		return windowID;
	}

	public void setTable_ID(int table_ID)
	{
		this.tableID = table_ID;
	}

	public MDMSContent getCurrDMSContent()
	{
		return currDMSContent;
	}

	public void setCurrDMSContent(MDMSContent currDMSContent)
	{
		this.currDMSContent = currDMSContent;
		selectedDMSContent.add(currDMSContent);
	}

	public boolean isTabViewer()
	{
		return (tableID > 0 && recordID > 0);
	}

	/**
	 * initialize components
	 */
	private void initForm()
	{
		tabBox.setWidth("100%");
		tabBox.setHeight("100%");
		tabBox.appendChild(tabs);
		tabBox.appendChild(tabPanels);
		tabBox.addEventListener(Events.ON_SELECT, this);
		grid.setStyle("width: 100%; height:95%; position:relative; overflow: auto;");
		// View Result Tab

		Grid btngrid = GridFactory.newGridLayout();
		Columns columns = new Columns();
		Column column = new Column();
		column.setWidth("90px");
		column.setAlign("left");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("130px");
		column.setAlign("center");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("120px");
		column.setAlign("right");
		columns.appendChild(column);

		Rows rows = new Rows();
		btngrid.appendChild(rows);

		Row row = new Row();
		rows.appendChild(row);
		btnBack.setImageContent(Utils.getImage("Left24.png"));
		btnBack.setTooltiptext("Previous Record");

		lblPositionInfo.setHflex("1");
		lblPositionInfo.setStyle("float: right;");
		ZkCssHelper.appendStyle(lblPositionInfo, "font-weight: bold;");
		ZkCssHelper.appendStyle(lblPositionInfo, "text-align: center;");

		btnNext.setImageContent(Utils.getImage("Right24.png"));
		btnNext.setTooltiptext("Next Record");
		btnBack.addEventListener(Events.ON_CLICK, this);
		btnNext.addEventListener(Events.ON_CLICK, this);
		btnNext.setStyle("float:right;");

		btnBack.setEnabled(false);
		btnNext.setEnabled(false);

		row.appendChild(btnBack);
		row.appendChild(lblPositionInfo);
		row.appendChild(btnNext);

		row = new Row();
		rows.appendChild(row);

		row.appendChild(btnCreateDir);
		row.appendChild(btnUploadContent);

		btnCreateDir.setImageContent(Utils.getImage("Folder24.png"));
		btnCreateDir.setTooltiptext("Create Directory");
		btnCreateDir.addEventListener(Events.ON_CLICK, this);

		btnUploadContent.setImageContent(Utils.getImage("Upload24.png"));
		btnUploadContent.setTooltiptext("Upload Content");
		btnUploadContent.addEventListener(Events.ON_CLICK, this);

		searchgridView.setStyle("max-height: 100%;width: 100%;position:relative; overflow: auto;");
		searchgridView.setVflex(true);
		columns = new Columns();

		column = new Column();
		column.setWidth("90px");
		column.setAlign("left");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("130px");
		column.setAlign("center");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("120px");
		column.setAlign("right");
		columns.appendChild(column);

		rows = new Rows();
		searchgridView.appendChild(rows);

		row = new Row();
		Cell searchCell = new Cell();
		searchCell.setRowspan(1);
		searchCell.setColspan(3);
		searchCell.appendChild(vsearchBox);
		rows.appendChild(row);
		row.appendChild(searchCell);
		vsearchBox.getButton().setImageContent(Utils.getImage("Search16.png"));
		vsearchBox.getButton().addEventListener(Events.ON_CLICK, this);
		vsearchBox.addEventListener(Events.ON_OK, this);

		row = new Row();
		rows.appendChild(row);
		row.appendCellChild(lblAdvanceSearch);
		lblAdvanceSearch.setHflex("1");
		ZkCssHelper.appendStyle(lblAdvanceSearch, "font-weight: bold;");

		row = new Row();
		rows.appendChild(row);
		ZkCssHelper.appendStyle(lblDocumentName, "font-weight: bold;");
		Cell nameCell = new Cell();
		nameCell.setColspan(2);
		row.appendChild(lblDocumentName);
		nameCell.appendChild(txtDocumentName);
		row.appendChild(nameCell);
		txtDocumentName.setWidth("100%");

		row = new Row();
		rows.appendChild(row);
		nameCell = new Cell();
		nameCell.setColspan(2);
		row.appendChild(lblDescription);
		nameCell.appendChild(txtDescription);
		row.appendChild(nameCell);
		txtDescription.setWidth("100%");

		Language lang = Env.getLanguage(Env.getCtx());
		int Column_ID = MColumn.getColumn_ID(MUser.Table_Name, MUser.COLUMNNAME_AD_User_ID);
		MLookup lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, Column_ID, DisplayType.TableDir, lang, MUser.COLUMNNAME_AD_User_ID, 0, true, "");
			lstboxCreatedBy = new WTableDirEditor(MUser.COLUMNNAME_AD_User_ID, false, false, true, lookup);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "User fetching failure :", e);
			throw new AdempiereException("User fetching failure :" + e);
		}

		row = new Row();
		rows.appendChild(row);
		row.setAlign("right");
		row.appendChild(lblCreatedBy);
		lblCreatedBy.setStyle("float: left;");
		Cell createdByCell = new Cell();
		createdByCell.setColspan(2);
		lstboxCreatedBy.getComponent().setWidth("100%");
		createdByCell.appendChild(lstboxCreatedBy.getComponent());
		row.appendChild(createdByCell);

		Column_ID = MColumn.getColumn_ID(MUser.Table_Name, MUser.COLUMNNAME_AD_User_ID);
		lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, Column_ID, DisplayType.TableDir, lang, MUser.COLUMNNAME_AD_User_ID, 0, true, "");
			lstboxUpdatedBy = new WTableDirEditor(MUser.COLUMNNAME_AD_User_ID, false, false, true, lookup);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "User fetching failure :", e);
			throw new AdempiereException("User fetching failure :" + e);
		}

		row = new Row();
		rows.appendChild(row);
		row.setAlign("right");
		row.appendChild(lblUpdatedBy);
		lblUpdatedBy.setStyle("float: left;");
		Cell updatedByCell = new Cell();
		updatedByCell.setColspan(2);
		lstboxUpdatedBy.getComponent().setWidth("100%");
		updatedByCell.appendChild(lstboxUpdatedBy.getComponent());
		row.appendChild(updatedByCell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblCreated);
		Hbox hbox = new Hbox();
		dbCreatedFrom.setStyle("width: 100%; display:flex; flex-direction: row;");
		dbCreatedTo.setStyle("width: 100%; display:flex; flex-direction: row;");
		hbox.appendChild(dbCreatedFrom);
		hbox.appendChild(dbCreatedTo);

		Cell createdCell = new Cell();
		createdCell.setColspan(2);
		createdCell.appendChild(hbox);
		row.appendChild(createdCell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblUpdated);
		hbox = new Hbox();
		dbUpdatedFrom.setStyle("width: 100%; display:flex; flex-direction: row;");
		dbUpdatedTo.setStyle("width: 100%; display:flex; flex-direction: row;");
		hbox.appendChild(dbUpdatedFrom);
		hbox.appendChild(dbUpdatedTo);

		Cell updatedCell = new Cell();
		updatedCell.setColspan(2);
		updatedCell.appendChild(hbox);
		row.appendChild(updatedCell);

		Column_ID = MColumn.getColumn_ID(MDMSContentType.Table_Name, MDMSContentType.COLUMNNAME_DMS_ContentType_ID);
		lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, Column_ID, DisplayType.TableDir, lang, MDMSContentType.COLUMNNAME_DMS_ContentType_ID, 0, true, "");
			lstboxContentType = new WTableDirEditor(MDMSContentType.COLUMNNAME_DMS_ContentType_ID, false, false, true, lookup);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Contenttype fetching failure :", e);
			throw new AdempiereException("Contenttype fetching failure :" + e);
		}

		row = new Row();
		rows.appendChild(row);
		row.setAlign("right");
		row.appendChild(lblContentType);
		lblContentType.setStyle("float: left;");
		Cell contentTypeListCell = new Cell();
		contentTypeListCell.setColspan(2);
		lstboxContentType.getComponent().setWidth("100%");
		contentTypeListCell.appendChild(lstboxContentType.getComponent());
		lstboxContentType.addValueChangeListener(this);
		row.appendChild(contentTypeListCell);

		// Active
		chkInActive.setLabel("Show InActive");
		chkInActive.setChecked(false);
		row = new Row();
		rows.appendChild(row);
		Cell activeCell = new Cell();
		activeCell.setColspan(2);
		// row.setAlign("center");
		row.setStyle("padding-left : 109px");
		activeCell.appendChild(chkInActive);
		chkInActive.addEventListener(Events.ON_CLICK, this);
		row.appendChild(activeCell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblContentMeta);
		ZkCssHelper.appendStyle(lblContentMeta, "font-weight: bold;");

		row = new Row();
		rows.appendChild(row);
		Cell cell = new Cell();
		cell.setColspan(3);
		cell.appendChild(panelAttribute);
		// panelAttribute.setStyle("max-height: 200px; overflow: auto;");
		row.appendChild(cell);

		row = new Row();
		rows.appendChild(row);
		hbox = new Hbox();

		btnClear.addEventListener(Events.ON_CLICK, this);
		btnRefresh.addEventListener(Events.ON_CLICK, this);

		btnSearch.setImageContent(Utils.getImage("Search24.png"));
		btnSearch.setTooltiptext("Search");

		btnSearch.addEventListener(Events.ON_CLICK, this);

		btnClear.setImageContent(Utils.getImage("Reset24.png"));
		btnRefresh.setImageContent(Utils.getImage("Refresh24.png"));
		btnCloseTab.setImageContent(Utils.getImage("Close24.png"));

		btnCloseTab.addEventListener(Events.ON_CLICK, this);

		hbox.appendChild(btnClear);
		hbox.appendChild(btnRefresh);
		hbox.appendChild(btnSearch);
		hbox.appendChild(btnCloseTab);

		cell = new Cell();
		cell.setColspan(3);
		cell.setRowspan(1);
		cell.setAlign("right");
		cell.appendChild(hbox);
		row.appendChild(cell);

		Tabpanel tabViewPanel = new Tabpanel();
		tabViewPanel.setHeight("100%");
		tabViewPanel.setWidth("100%");
		Hbox boxViewSeparator = new Hbox();
		boxViewSeparator.setWidth("100%");
		boxViewSeparator.setHeight("100%");

		cell = new Cell();
		cell.setWidth("100%");
		cell.setStyle("position: absolute;");
		cell.appendChild(gridBreadCrumb);
		cell.appendChild(grid);
		cell.addEventListener(Events.ON_RIGHT_CLICK, this);
		boxViewSeparator.appendChild(cell);

		gridBreadCrumb
				.setStyle("width: 100%; position:relative; overflow: auto; background-color: rgba(0,0,0,.2); background-clip: padding-box; color: #222; background: transparent; "
						+ "font-family: Roboto,sans-serif; border: solid transparent; border-width: 1px 1px 1px 6px;min-height: 28px; padding: 100px 0 0;"
						+ "box-shadow: inset 1px 1px 0 rgba(0,0,0,.1),inset 0 -1px 0 rgba(0,0,0,.07);");

		breadRow.setZclass("none");
		breadRow.setStyle("display:flex; flex-direction: row; flex-wrap: wrap; height: 100%;");

		Splitter splitter = new Splitter();
		splitter.setCollapse("after");
		boxViewSeparator.appendChild(splitter);

		cell = new Cell();
		cell.setWidth("32%");
		cell.setHeight("100%");
		// cell.setStyle("height: 100%; overflow: hidden;");
		cell.appendChild(btngrid);
		cell.appendChild(searchgridView);
		searchgridView.setParent(cell);
		boxViewSeparator.appendChild(cell);
		tabViewPanel.appendChild(boxViewSeparator);
		cell.setParent(boxViewSeparator);
		searchgridView.appendChild(rows);

		tabs.appendChild(tabView);
		tabPanels.appendChild(tabViewPanel);
		this.setHeight("100%");
		this.setWidth("100%");
		this.appendChild(tabBox);

		this.addEventListener(Events.ON_CLICK, this);
		this.addEventListener(Events.ON_DOUBLE_CLICK, this);

		mnu_versionList = new Menuitem(DMSConstant.MENUITEM_VERSIONlIST);
		mnu_cut = new Menuitem(DMSConstant.MENUITEM_CUT);
		mnu_copy = new Menuitem(DMSConstant.MENUITEM_COPY);
		mnu_paste = new Menuitem(DMSConstant.MENUITEM_PASTE);
		mnu_download = new Menuitem(DMSConstant.MENUITEM_DOWNLOAD);
		mnu_createLink = new Menuitem(DMSConstant.MENUITEM_CREATELINK);
		mnu_delete = new Menuitem(DMSConstant.MENUITEM_DELETE);
		mnu_associate = new Menuitem(DMSConstant.MENUITEM_ASSOCIATE);
		mnu_uploadVersion = new Menuitem(DMSConstant.MENUITEM_UPLOADVERSION);
		mnu_rename = new Menuitem(DMSConstant.MENUITEM_RENAME);

		mnu_canvasCreateLink = new Menuitem(DMSConstant.MENUITEM_CREATELINK);
		mnu_canvasPaste = new Menuitem(DMSConstant.MENUITEM_PASTE);

		canvasContextMenu.appendChild(mnu_canvasPaste);
		canvasContextMenu.appendChild(mnu_canvasCreateLink);
		mnu_canvasCreateLink.addEventListener(Events.ON_CLICK, this);
		mnu_canvasPaste.addEventListener(Events.ON_CLICK, this);

		contentContextMenu.appendChild(mnu_uploadVersion);
		contentContextMenu.appendChild(mnu_versionList);
		contentContextMenu.appendChild(mnu_cut);
		contentContextMenu.appendChild(mnu_copy);
		contentContextMenu.appendChild(mnu_paste);
		contentContextMenu.appendChild(mnu_download);
		contentContextMenu.appendChild(mnu_createLink);
		contentContextMenu.appendChild(mnu_rename);
		contentContextMenu.appendChild(mnu_delete);
		contentContextMenu.appendChild(mnu_associate);

		mnu_canvasCreateLink.setImageContent(Utils.getImage("Link24.png"));
		mnu_canvasPaste.setImageContent(Utils.getImage("Paste24.png"));
		mnu_createLink.setImageContent(Utils.getImage("Link24.png"));
		mnu_uploadVersion.setImageContent(Utils.getImage("uploadversion24.png"));
		mnu_paste.setImageContent(Utils.getImage("Paste24.png"));
		mnu_download.setImageContent(Utils.getImage("Downloads24.png"));
		mnu_rename.setImageContent(Utils.getImage("Rename24.png"));
		mnu_cut.setImageContent(Utils.getImage("Cut24.png"));
		mnu_versionList.setImageContent(Utils.getImage("Versions24.png"));
		mnu_copy.setImageContent(Utils.getImage("Copy24.png"));
		mnu_delete.setImageContent(Utils.getImage("Delete24.png"));
		mnu_associate.setImageContent(Utils.getImage("Associate24.png"));

		mnu_uploadVersion.addEventListener(Events.ON_CLICK, this);
		mnu_versionList.addEventListener(Events.ON_CLICK, this);
		mnu_cut.addEventListener(Events.ON_CLICK, this);
		mnu_paste.addEventListener(Events.ON_CLICK, this);
		mnu_download.addEventListener(Events.ON_CLICK, this);
		mnu_rename.addEventListener(Events.ON_CLICK, this);
		mnu_createLink.addEventListener(Events.ON_CLICK, this);
		mnu_copy.addEventListener(Events.ON_CLICK, this);
		mnu_delete.addEventListener(Events.ON_CLICK, this);
		mnu_associate.addEventListener(Events.ON_CLICK, this);

		// mnu_delete.setDisabled(true);

		DMSConstant.dateFormatWithTime.setTimeZone(TimeZone.getTimeZone("UTC"));
		addRootBreadCrumb();
		SessionManager.getAppDesktop();
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		log.info(event.getName());

		if (Events.ON_DOUBLE_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			openDirectoryORContent(DMSViewerComp);
		}
		else if (event.getTarget().equals(btnCreateDir))
		{
			if (isWindowAccess)
				createDirectory();
		}
		else if (event.getTarget().equals(btnUploadContent))
		{
			if (isWindowAccess)
				uploadContent();
		}
		else if (event.getTarget().equals(btnBack))
		{
			if (isSearch || isGenericSearch)
			{
				isSearch = false;
				isGenericSearch = false;
				currDMSContent = null;
				lblPositionInfo.setValue(null);
			}
			backNavigation();
		}
		else if (event.getTarget().equals(btnNext))
		{
			directoryNavigation();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_RESET))
		{
			// For solve navigation issue After search and reset button pressed.
			isGenericSearch = true;

			isSearch = false;
			clearComponents();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			breadRow.getChildren().clear();
			if (isTabViewer())
			{
				isSearch = false;
				addRootBreadCrumb();
				int DMS_Content_ID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE name = ? ORDER BY Created desc",
						String.valueOf(recordID));
				setCurrDMSContent(new MDMSContent(Env.getCtx(), DMS_Content_ID, null));

				if (currDMSContent != null)
					lblPositionInfo.setText(currDMSContent.getName());
				else
					lblPositionInfo.setText(String.valueOf(recordID));
				btnBack.setEnabled(false);
				btnNext.setEnabled(false);
			}
			else
			{
				isSearch = false;
				currDMSContent = null;
				lblPositionInfo.setText(null);
				btnBack.setEnabled(false);
				btnNext.setEnabled(false);
				breadRow.getChildren().clear();
				addRootBreadCrumb();
			}
			renderViewer();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_REFRESH) || event.getTarget().equals(btnSearch))
		{
			HashMap<String, List<Object>> params = getQueryParamas();
			String query = dms.buildSolrSearchQuery(params);

			if (query.equals("*:*") || query.startsWith("AD_Table_ID"))
			{
				isSearch = false;
				if (currDMSContent != null)
				{
					lblPositionInfo.setValue(currDMSContent.getName());
				}
				else
					lblPositionInfo.setValue(null);
			}
			else
			{
				isSearch = true;
				breadRow.getChildren().clear();
				btnBack.setEnabled(true);
				lblPositionInfo.setValue(null);
			}
			btnNext.setEnabled(false);

			renderViewer();
		}
		else if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			currentCompSelection(DMSViewerComp);
		}
		else if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(WDMSPanel.class))
		{
			if (prevComponent != null)
			{
				ZkCssHelper.appendStyle(prevComponent.getfLabel(), DMSConstant.STYLE_CONTENT_COMP_VIEWER_NORMAL);
			}
		}
		else if (Events.ON_RIGHT_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(Cell.class))
		{
			openCanvasContextMenu(event);
		}
		else if (Events.ON_RIGHT_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComp = (DMSViewerComponent) event.getTarget();
			openContentContextMenu(DMSViewerComp);

			// show only download option on menu context if access are
			// read-only.
			if (!isWindowAccess)
			{
				mnu_download.setDisabled(false);
				mnu_copy.setDisabled(false);
			}
		}
		else if (event.getTarget().equals(mnu_versionList))
		{
			new WDMSVersion(dms, DMSViewerComp.getDMSContent());
		}
		else if (event.getTarget().equals(mnu_uploadVersion))
		{
			final WUploadContent uploadContent = new WUploadContent(dms, dirContent, true, this.getTable_ID(), this.getRecord_ID());
			uploadContent.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

				@Override
				public void onEvent(Event e) throws Exception
				{
					if (!uploadContent.isCancel())
					{
						renderViewer();
					}
				}
			});
		}
		else if (event.getTarget().equals(mnu_copy))
		{
			DMSClipboard.put(DMSViewerComp.getDMSContent(), true);
		}
		else if (event.getTarget().equals(mnu_createLink))
		{
			linkCopyDocument(dirContent, true);
		}
		else if (event.getTarget().equals(mnu_cut))
		{
			DMSClipboard.put(DMSViewerComp.getDMSContent(), false);
		}
		else if (event.getTarget().equals(mnu_paste) || event.getTarget().equals(mnu_canvasPaste))
		{
			if (DMSClipboard.get() != null)
			{
				MDMSContent sourceContent = DMSClipboard.get();
				MDMSContent destPasteContent = dirContent;
				if (destPasteContent != null && sourceContent.get_ID() == destPasteContent.get_ID())
				{
					FDialog.warn(0, "You cannot Paste into itself");
				}
				else
				{
					if (DMSClipboard.getIsCopy())
						dms.pasteCopyContent(sourceContent, destPasteContent, tableID, recordID, isTabViewer());
					else
						dms.pasteCutContent(sourceContent, destPasteContent);
					renderViewer();
				}
			}
		}
		else if (event.getTarget().equals(mnu_download))
		{
			DMS_ZK_Util.downloadDocument(dms, dirContent);
		}
		else if (event.getTarget().equals(mnu_rename))
		{
			final WRenameContent renameContent = new WRenameContent(dms, dirContent);
			renameContent.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

				@Override
				public void onEvent(Event e) throws Exception
				{
					if (!renameContent.isCancel())
					{
						renderViewer();
					}
				}
			});
		}
		else if (event.getTarget().equals(mnu_delete))
		{
			// TODO inactive DMS_content and same change in solr index

			Callback<Boolean> callback = new Callback<Boolean>() {
				@Override
				public void onCallback(Boolean result)
				{
					if (result)
					{
						dms.deleteContent(DMSViewerComp.getDMSContent(), DMSViewerComp.getDMSAssociation());
						try
						{
							renderViewer();
						}
						catch (Exception e)
						{
							throw new AdempiereException(e);
						}
					}
					else
					{
						return;
					}
				}
			};

			FDialog.ask(0, this, "Are you sure to delete " + DMSViewerComp.getDMSContent().getName() + "?", callback);

		}
		else if (event.getTarget().equals(mnu_associate))
		{
			new WDAssociationType(dms, copyDMSContent, DMSViewerComp.getDMSContent(), getTable_ID(), getRecord_ID(), winContent);
		}
		else if (event.getTarget().equals(mnu_canvasCreateLink))
		{
			linkCopyDocument(currDMSContent, false);
		}
		else if (event.getName().equals("onUploadComplete"))
		{
			renderViewer();
		}
		else if (event.getName().equals("onRenameComplete"))
		{
			Tab tab = (Tab) tabBox.getSelectedTab();
			renderViewer();
			tabBox.setSelectedTab(tab);
		}
		else if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(BreadCrumbLink.class))
		{
			renderBreadCrumb(event);
		}
		else if (Events.ON_OK.equals(event.getName())
				|| (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(vsearchBox.getButton().getClass())))
		{
			breadRow.getChildren().clear();
			btnBack.setEnabled(true);
			btnNext.setEnabled(false);
			lblPositionInfo.setValue(null);
			isGenericSearch = true;
			isSearch = false;
			renderViewer();
		}

		// Disable navigation button based on "isAllowCreateDirectory" check on
		// client info window
		if (isTabViewer() && !isAllowCreateDirectory)
		{
			btnBack.setEnabled(false);
			btnNext.setEnabled(false);
		}

	}

	private void renderBreadCrumb(Event event) throws IOException, URISyntaxException
	{
		breadCrumbEvent = (BreadCrumbLink) event.getTarget();

		if (isTabViewer())
		{
			if (breadCrumbEvent.getPathId().equals("0"))
			{
				int DMS_Content_ID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE Name = ? ORDER BY Created desc",
						String.valueOf(recordID));
				breadCrumbEvent.setPathId(String.valueOf(DMS_Content_ID));

			}

			if (breadCrumbEvent.getImageContent() != null)
			{
				btnBack.setEnabled(false);
				btnNext.setEnabled(false);
			}
		}

		if (breadCrumbEvent.getPathId().equals("0"))
		{
			selectedDMSContent.removeAllElements();
			selectedDMSAssociation.removeAllElements();
			btnNext.setEnabled(false);
			btnBack.setEnabled(false);
		}

		int DMS_Content_ID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE DMS_Content_ID = ? ORDER BY Created desc ",
				Integer.valueOf(breadCrumbEvent.getPathId()));

		currDMSContent = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);

		lblPositionInfo.setValue(currDMSContent.getName());

		List<BreadCrumbLink> parents = getParentLinks();
		if (!parents.isEmpty())
		{
			breadRow.getChildren().clear();
			Iterator<BreadCrumbLink> iterator = parents.iterator();
			while (iterator.hasNext())
			{
				BreadCrumbLink breadCrumbLink = (BreadCrumbLink) iterator.next();
				breadCrumbLink.setStyle("font-weight: bold; font-size: small; padding-left: 15px; color: dimgray;");

				breadRow.appendChild(breadCrumbLink);

				lblShowBreadCrumb = new Label();
				lblShowBreadCrumb.setValue(" >");
				breadRow.appendChild(new Space());
				breadRow.appendChild(lblShowBreadCrumb);

				if (Integer.valueOf(breadCrumbLink.getPathId()) == currDMSContent.getDMS_Content_ID())
				{
					breadRow.removeChild(lblShowBreadCrumb);
					break;
				}
				breadRows.appendChild(breadRow);
				gridBreadCrumb.appendChild(breadRows);
			}
		}
		renderViewer();
	}

	/**
	 * @throws IOException
	 * @throws URISyntaxException Render the Thumb Component
	 */
	public void renderViewer() throws IOException, URISyntaxException
	{
		byte[] imgByteData = null;
		File thumbFile = null;

		Rows rows = new Rows();
		Row row = new Row();
		MImage mImage = null;
		AImage image = null;

		viewerComponents = new ArrayList<DMSViewerComponent>();

		HashMap<I_DMS_Content, I_DMS_Association> contentsMap = null;

		// Setting current dms content value on label
		if (isTabViewer())
		{
			String currContentValue = currDMSContent != null ? String.valueOf(currDMSContent.getName()) : null;
			lblPositionInfo.setValue(currContentValue);
		}

		if (isSearch)
			contentsMap = dms.renderSearchedContent(getQueryParamas(), currDMSContent);
		else if (isGenericSearch)
			contentsMap = dms.getGenericSearchedContent(vsearchBox.getTextbox().getValue(), tableID, recordID, currDMSContent);
		else
			contentsMap = dms.getDMSContentsWithAssociation(currDMSContent);

		Components.removeAllChildren(grid);
		for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : contentsMap.entrySet())
		{

			thumbFile = dms.getThumbnail(entry.getKey(), "150");

			if (thumbFile == null)
			{
				if (entry.getKey().getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
				{
					mImage = dms.getDirThumbnail();
				}
				else
				{
					mImage = dms.getMimetypeThumbnail(entry.getKey().getDMS_MimeType_ID());
				}
				imgByteData = mImage.getData();
				if (imgByteData != null)
					image = new AImage(entry.getKey().getName(), imgByteData);
			}
			else
			{
				image = new AImage(thumbFile);
			}

			boolean isLink = entry.getValue().getDMS_AssociationType_ID() == Utils.getDMS_Association_Link_ID();
			DMSViewerComponent viewerComp = new DMSViewerComponent(dms, entry.getKey(), image, isLink, entry.getValue());
			viewerComp.setDwidth(DMSConstant.CONTENT_COMPONENT_WIDTH);
			viewerComp.setDheight(DMSConstant.CONTENT_COMPONENT_HEIGHT);
			viewerComp.addEventListener(Events.ON_CLICK, this);
			viewerComp.addEventListener(Events.ON_RIGHT_CLICK, this);
			viewerComp.addEventListener(Events.ON_DOUBLE_CLICK, this);
			viewerComponents.add(viewerComp);
			row.appendChild(viewerComp);
		}
		row.setZclass("none");
		row.setStyle("display:flex; flex-direction: row; flex-wrap: wrap; height: 100%; overflow: hidden;");
		rows.appendChild(row);

		grid.appendChild(rows);
		tabBox.setSelectedIndex(0);
	}

	/**
	 * clear the gridview components
	 */
	private void clearComponents()
	{
		vsearchBox.setText(null);
		txtDocumentName.setValue(null);
		txtDescription.setValue(null);
		lstboxContentType.setValue(null);
		lstboxCreatedBy.setValue(null);
		lstboxUpdatedBy.setValue(null);
		dbCreatedFrom.setValue(null);
		dbCreatedTo.setValue(null);
		dbUpdatedFrom.setValue(null);
		dbUpdatedTo.setValue(null);
		chkInActive.setChecked(false);

		if (m_editors != null)
		{
			for (WEditor editor : m_editors)
				editor.setValue(null);
		}
		Components.removeAllChildren(panelAttribute);
	}

	/**
	 * open the Directory OR Content
	 * 
	 * @param DMSViewerComp
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws DocumentException
	 * @throws com.itextpdf.text.DocumentException
	 */
	private void openDirectoryORContent(DMSViewerComponent DMSViewerComp) throws IOException, URISyntaxException, DocumentException,
			com.itextpdf.text.DocumentException
	{
		selectedDMSContent.push(DMSViewerComp.getDMSContent());

		selectedDMSAssociation.push(DMSViewerComp.getDMSAssociation());

		if (selectedDMSContent.peek().getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			currDMSContent = selectedDMSContent.pop();
			showBreadcumb(currDMSContent);
			renderViewer();
			lblPositionInfo.setValue(currDMSContent.getName());
			btnBack.setEnabled(true);
			btnNext.setEnabled(false);
		}
		else if (selectedDMSContent.peek().getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Content))
		{
			File documentToPreview = null;

			MDMSMimeType mimeType = new MDMSMimeType(Env.getCtx(), selectedDMSContent.peek().getDMS_MimeType_ID(), null);

			documentToPreview = dms.getFileFromStorage(selectedDMSContent.peek());

			if (documentToPreview != null)
			{
				String name = selectedDMSContent.peek().getName();

				if (name.contains("(") && name.contains(")"))
					name = name.replace(name.substring(name.lastIndexOf("("), name.lastIndexOf(")") + 1), "");

				try
				{
					documentToPreview = dms.convertToPDF(documentToPreview, mimeType);
				}
				catch (Exception e)
				{
					selectedDMSContent.pop();
					throw new AdempiereException(e);
				}

				if (Utils.getContentEditor(mimeType.getMimeType()) != null)
				{
					Tab tabData = new Tab(name);
					tabData.setClosable(true);
					tabs.appendChild(tabData);
					tabBox.setSelectedTab(tabData);

					WDocumentViewer documentViewer = new WDocumentViewer(dms, tabBox, documentToPreview, selectedDMSContent.peek(), tableID, recordID);
					Tabpanel tabPanel = documentViewer.initForm(isWindowAccess);
					tabPanels.appendChild(tabPanel);
					documentViewer.getAttributePanel().addEventListener("onUploadComplete", this);
					documentViewer.getAttributePanel().addEventListener("onRenameComplete", this);

					this.appendChild(tabBox);
				}
				else
				{
					DMS_ZK_Util.downloadDocument(documentToPreview);
				}
				// Fix for search --> download content --> back (which was
				// navigate to home/root folder)
				selectedDMSContent.pop();
			}
			else
			{
				FDialog.error(0, dms.getPathFromContentManager(currDMSContent) + " Content missing in storage,");
			}
		}
	}

	/**
	 * Navigate the Previous Directory.
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private void backNavigation() throws IOException, URISyntaxException
	{
		List<BreadCrumbLink> parents = getParentLinks();
		if (!parents.isEmpty())
		{
			breadRow.getChildren().clear();
			int count = 0;
			Iterator<BreadCrumbLink> iterator = parents.iterator();
			while (iterator.hasNext())
			{
				BreadCrumbLink breadCrumbLink = (BreadCrumbLink) iterator.next();
				breadCrumbLink.setStyle("font-weight: bold; font-size: small; padding-left: 15px; color: dimgray;");

				if (currDMSContent != null && parents.size() > 1)
				{
					breadRow.appendChild(breadCrumbLink);
					lblShowBreadCrumb = new Label();
					lblShowBreadCrumb.setValue(" >");
					breadRow.appendChild(new Space());
					breadRow.appendChild(lblShowBreadCrumb);

					count++;

					if (parents.size() - 1 == count)
					{
						breadRow.removeChild(lblShowBreadCrumb);
						break;
					}
				}
				breadRows.appendChild(breadRow);
				gridBreadCrumb.appendChild(breadRows);
			}

			if (currDMSContent == null)
			{
				addRootBreadCrumb();
			}
		}
		else
		{
			addRootBreadCrumb();
		}

		nextDMSContent = currDMSContent;

		if (selectedDMSAssociation != null && !selectedDMSAssociation.isEmpty()
				&& selectedDMSAssociation.peek().getDMS_AssociationType_ID() == Utils.getDMS_Association_Link_ID() && currDMSContent != null
				&& currDMSContent.getDMS_Content_ID() == selectedDMSAssociation.peek().getDMS_Content_ID())
		{
			currDMSContent = new MDMSContent(Env.getCtx(), selectedDMSAssociation.peek().getDMS_Content_Related_ID(), null);
			lblPositionInfo.setValue(currDMSContent.getName());
			if (currDMSContent.getParentURL() == null)
				btnBack.setEnabled(true);

			btnNext.setEnabled(true);
		}
		else if (currDMSContent != null)
		{
			int DMS_Content_ID = DB.getSQLValue(null,
					"SELECT DMS_Content_Related_ID FROM DMS_Association WHERE DMS_Content_ID = ? AND DMS_AssociationType_ID IS NULL",
					currDMSContent.getDMS_Content_ID());

			if (DMS_Content_ID <= 0)
			{
				currDMSContent = null;
				lblPositionInfo.setValue("");
				btnBack.setEnabled(false);
			}
			else
			{
				currDMSContent = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
				lblPositionInfo.setValue(currDMSContent.getName());
				if (currDMSContent.getParentURL() == null)
					btnBack.setEnabled(true);
			}
			btnNext.setEnabled(true);
		}
		else
		{
			btnBack.setEnabled(false);
		}

		if (!selectedDMSAssociation.isEmpty())
			previousDMSAssociation = selectedDMSAssociation.pop();

		renderViewer();

		if (recordID > 0 && tableID > 0)
		{
			// Getting latest record if multiple dms content available
			// TODO Need to Check sql purpose
			int id = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE name = ? AND IsMounting = 'Y' ORDER BY created desc", recordID + "");

			if (currDMSContent == null)
				currDMSContent = selectedDMSContent.peek();

			if (currDMSContent.getDMS_Content_ID() == id)
			{
				btnBack.setDisabled(true);
				renderViewer();
			}
			return;
		}
		btnClear.setEnabled(false);

	}

	/**
	 * Move in the Directory
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private void directoryNavigation() throws IOException, URISyntaxException
	{
		BreadCrumbLink breadCrumbLink = new BreadCrumbLink();
		breadCrumbLink.setPathId(nextDMSContent.getName());
		breadCrumbLink.setLabel(nextDMSContent.getName());
		breadCrumbLink.setStyle("font-weight: bold; font-size: small; padding-left: 15px; color: dimgray;");

		breadRow.appendChild(new Label(" > "));
		breadRow.appendChild(breadCrumbLink);
		breadRows.appendChild(breadRow);
		gridBreadCrumb.appendChild(breadRows);

		if (nextDMSContent != null)
		{
			currDMSContent = nextDMSContent;
			if (previousDMSAssociation != null)
				selectedDMSAssociation.add(selectedDMSAssociation.size(), previousDMSAssociation);
			renderViewer();
			lblPositionInfo.setValue(currDMSContent.getName());
		}
		btnNext.setEnabled(false);
		btnBack.setEnabled(true);
	}

	/**
	 * Make Directory
	 */
	private void createDirectory()
	{
		createDirectoryForm = new WCreateDirectoryForm(dms, currDMSContent, tableID, recordID);

		createDirectoryForm.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception
			{
				renderViewer();
			}
		});
	}

	/**
	 * Upload Content
	 */
	private void uploadContent()
	{
		uploadContent = new WUploadContent(dms, currDMSContent, false, tableID, recordID);

		uploadContent.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception
			{
				renderViewer();
			}
		});
		uploadContent.addEventListener(Events.ON_CLOSE, this);
	}

	/**
	 * Open MenuPopup when Right click on Directory OR Content
	 * 
	 * @param DMSViewerCom
	 */
	private void openContentContextMenu(final DMSViewerComponent DMSViewerCom)
	{
		dirContent = DMSViewerCom.getDMSContent();
		contentContextMenu.setPage(DMSViewerCom.getPage());
		copyDMSContent = DMSClipboard.get();

		if (!isWindowAccess || (dirContent.isMounting() && dirContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory)))
		{
			mnu_associate.setDisabled(true);
			mnu_copy.setDisabled(true);
			mnu_createLink.setDisabled(true);
			mnu_cut.setDisabled(true);
			mnu_delete.setDisabled(true);
			mnu_paste.setDisabled(true);
			mnu_rename.setDisabled(true);
			mnu_uploadVersion.setDisabled(true);
			mnu_versionList.setDisabled(true);
			mnu_download.setDisabled(true);
			DMSViewerCom.setContext(contentContextMenu);
			contentContextMenu.open(this, "at_pointer");
			return;
		}
		else
		{
			mnu_delete.setDisabled(false);
			mnu_associate.setDisabled(false);
			mnu_copy.setDisabled(false);
			mnu_createLink.setDisabled(false);
			mnu_cut.setDisabled(false);
			mnu_paste.setDisabled(false);
			mnu_rename.setDisabled(false);
			mnu_uploadVersion.setDisabled(false);
			mnu_versionList.setDisabled(false);
			mnu_download.setDisabled(false);
		}

		if (copyDMSContent == null)
		{
			mnu_paste.setDisabled(true);
			mnu_canvasPaste.setDisabled(true);
			mnu_createLink.setDisabled(true);
			mnu_associate.setDisabled(true);
			mnu_versionList.setDisabled(true);
			mnu_uploadVersion.setDisabled(true);
		}
		else if (copyDMSContent == DMSViewerCom.getDMSContent())
		{
			mnu_paste.setDisabled(true);
			mnu_canvasPaste.setDisabled(true);
			mnu_associate.setDisabled(true);
			mnu_createLink.setDisabled(true);
		}
		else if (MDMSContent.CONTENTBASETYPE_Directory.equals(DMSViewerCom.getDMSContent().getContentBaseType()))
		{
			mnu_paste.setDisabled(false);
			mnu_canvasPaste.setDisabled(false);
			mnu_associate.setDisabled(true);
			mnu_versionList.setDisabled(true);
			mnu_uploadVersion.setDisabled(true);
		}
		else
		{
			mnu_createLink.setDisabled(false);
			mnu_associate.setDisabled(false);
		}

		if (MDMSContent.CONTENTBASETYPE_Content.equals(DMSViewerCom.getContentBaseType()))
		{
			mnu_versionList.setDisabled(false);
			mnu_paste.setDisabled(true);
			mnu_canvasPaste.setDisabled(true);
			mnu_uploadVersion.setDisabled(false);
			mnu_createLink.setDisabled(true);
			mnu_download.setDisabled(false);

			if (copyDMSContent != null && copyDMSContent != DMSViewerCom.getDMSContent())
				mnu_associate.setDisabled(false);
			else
				mnu_associate.setDisabled(true);
		}

		if (MDMSContent.CONTENTBASETYPE_Directory.equals(DMSViewerCom.getContentBaseType()))
		{
			if (copyDMSContent != null)
				mnu_createLink.setDisabled(false);

			if (DMSClipboard.get() != null && !DMSClipboard.getIsCopy())
			{
				mnu_paste.setDisabled(false);
				mnu_canvasPaste.setDisabled(false);
			}
			mnu_download.setDisabled(true);
		}

		mnu_copy.setDisabled(false);
		DMSViewerCom.setContext(contentContextMenu);
		contentContextMenu.open(this, "at_pointer");

		if (MDMSContent.CONTENTBASETYPE_Content.equals(DMSViewerCom.getContentBaseType())
				&& DMSViewerCom.getDMSAssociation().getDMS_AssociationType_ID() == Utils.getDMS_Association_Link_ID())
		{
			mnu_versionList.setDisabled(false);
			mnu_paste.setDisabled(true);
			mnu_canvasPaste.setDisabled(true);
			mnu_uploadVersion.setDisabled(false);
			mnu_createLink.setDisabled(true);
			mnu_download.setDisabled(false);
			mnu_copy.setDisabled(true);
			mnu_associate.setDisabled(true);
			mnu_rename.setDisabled(true);
			mnu_delete.setDisabled(false);
			mnu_cut.setDisabled(true);
		}

		if (MDMSContent.CONTENTBASETYPE_Directory.equals(DMSViewerCom.getContentBaseType())
				&& DMSViewerCom.getDMSAssociation().getDMS_AssociationType_ID() == Utils.getDMS_Association_Link_ID())
		{
			mnu_versionList.setDisabled(true);
			mnu_paste.setDisabled(true);
			mnu_canvasPaste.setDisabled(true);
			mnu_uploadVersion.setDisabled(true);
			mnu_createLink.setDisabled(true);
			mnu_download.setDisabled(true);
			mnu_copy.setDisabled(true);
			mnu_associate.setDisabled(true);
			mnu_rename.setDisabled(true);
			mnu_delete.setDisabled(true);
			mnu_cut.setDisabled(true);
		}
	}

	/**
	 * select the directory or content
	 * 
	 * @param DMSViewerComp
	 */
	private void currentCompSelection(DMSViewerComponent DMSViewerComp)
	{
		if (prevComponent != null)
		{
			ZkCssHelper.appendStyle(prevComponent.getfLabel(), DMSConstant.STYLE_CONTENT_COMP_VIEWER_NORMAL);
		}

		for (DMSViewerComponent viewerComponent : viewerComponents)
		{
			if (viewerComponent.getDMSContent().getDMS_Content_ID() == DMSViewerComp.getDMSContent().getDMS_Content_ID())
			{
				ZkCssHelper.appendStyle(DMSViewerComp.getfLabel(), DMSConstant.STYLE_CONTENT_COMP_VIEWER_SELECTED);

				prevComponent = viewerComponent;
				break;
			}
		}
	}

	private void openCanvasContextMenu(Event event)
	{
		Cell cell = (Cell) event.getTarget();

		dirContent = currDMSContent;
		canvasContextMenu.setPage(cell.getPage());
		cell.setContext(canvasContextMenu);

		if (DMSClipboard.get() == null)
		{
			mnu_canvasCreateLink.setDisabled(true);
			mnu_canvasPaste.setDisabled(true);
		}
		else
		{
			mnu_canvasCreateLink.setDisabled(false);
			mnu_canvasPaste.setDisabled(false);
		}

		if (tableID <= 0 || recordID <= 0)
		{
			if (DMSClipboard.get() != null && !DMSClipboard.getIsCopy())
			{
				mnu_canvasPaste.setDisabled(true);
			}
			else
			{
				mnu_canvasPaste.setDisabled(false);
			}

			if (DMSClipboard.get() == null)
			{
				mnu_canvasPaste.setDisabled(true);
			}
			else
			{
				mnu_canvasPaste.setDisabled(false);
			}
		}

		if (currDMSContent != null)
		{
			if (currDMSContent.isMounting() && !currDMSContent.getName().equals(String.valueOf(getRecord_ID())))
			{
				mnu_canvasCreateLink.setDisabled(true);
				mnu_canvasPaste.setDisabled(true);
			}
		}

		canvasContextMenu.open(this, "at_pointer");
	}

	// TODO Need check for refactoring
	/**
	 * @param DMSContent
	 * @param isDir
	 */
	private void linkCopyDocument(MDMSContent DMSContent, boolean isDir)
	{
		boolean isDocPresent = dms.isDocumentPresent(currDMSContent, DMSContent, isDir);
		if (DMSClipboard.get() == null)
		{
			return;
		}

		if (DMSContent != null && DMSContent.get_ID() == DMSClipboard.get().get_ID())
		{
			FDialog.warn(0, "You cannot Link into itself");
			return;
		}
		if (isDocPresent)
		{
			FDialog.warn(0, "Document already exists.");
			return;
		}

		// IF DMS Tab
		if (isTabViewer())
		{
			int DMS_Association_ID = DB
					.getSQLValue(
							null,
							"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ? AND DMS_AssociationType_ID = ? AND AD_Table_ID = ? AND Record_ID = ?",
							DMSClipboard.get().getDMS_Content_ID(), Utils.getDMS_Association_Record_ID(), tableID, recordID);

			if (DMS_Association_ID == -1)
			{
				MDMSAssociation DMSassociation = new MDMSAssociation(Env.getCtx(), 0, null);
				DMSassociation.setDMS_Content_ID(DMSClipboard.get().getDMS_Content_ID());

				int DMS_Content_Related_ID = DB.getSQLValue(null, "SELECT DMS_Content_Related_ID FROM DMS_Association WHERE DMS_Content_ID = ? "
						+ "AND DMS_AssociationType_ID IN (SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE UPPER(Name) = UPPER('Parent'))",
						DMSClipboard.get().getDMS_Content_ID());

				if (DMS_Content_Related_ID != 0)
					DMSassociation.setDMS_Content_Related_ID(DMSContent.getDMS_Content_ID());

				DMSassociation.setDMS_AssociationType_ID(Utils.getDMS_Association_Link_ID());
				DMSassociation.setRecord_ID(recordID);
				DMSassociation.setAD_Table_ID(tableID);
				DMSassociation.saveEx();

				try
				{
					renderViewer();

					int DMS_Content_ID = DMSassociation.getDMS_Content_Related_ID();

					if (DMS_Content_ID <= 0)
						DMS_Content_ID = DMSassociation.getDMS_Content_ID();
					else
					{
						MDMSContent dmsContent = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);

						if (dmsContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
							DMS_Content_ID = DMSassociation.getDMS_Content_ID();
						else
							DMS_Content_ID = DMSassociation.getDMS_Content_Related_ID();
					}

					MDMSContent dmsContent = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);

					// Here currently we can not able to move index creation
					// logic in model validator
					// TODO : In future, will find approaches for move index
					// creation logic
					dms.createIndexContent(dmsContent, DMSassociation);
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, "Render content problem.", e);
					throw new AdempiereException("Render content problem: " + e);
				}
			}
			else
			{
				FDialog.warn(0, "Document already associated.");
			}

			return;
		}

		//
		int DMS_Content_Related_ID = 0;
		if (DMSContent != null)
			DMS_Content_Related_ID = DMSContent.getDMS_Content_ID();

		dms.createAssociation(DMSClipboard.get().getDMS_Content_ID(), DMS_Content_Related_ID, 0, 0, Utils.getDMS_Association_Link_ID(), 0, null);

		try
		{
			renderViewer();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render content problem.", e);
			throw new AdempiereException("Render content problem: " + e);
		}
	}

	private HashMap<String, List<Object>> getQueryParamas()
	{
		HashMap<String, List<Object>> params = new LinkedHashMap<String, List<Object>>();
		List<Object> value = new ArrayList<Object>();

		if (!Util.isEmpty(txtDocumentName.getValue(), true))
		{
			value.add("*" + txtDocumentName.getValue().toLowerCase() + "*");
			params.put(DMSConstant.NAME, value);

		}

		if (!Util.isEmpty(txtDescription.getValue(), true))
		{
			value = new ArrayList<Object>();
			value.add("*" + txtDescription.getValue().toLowerCase().trim() + "*");
			params.put(DMSConstant.DESCRIPTION, value);
		}

		if (dbCreatedFrom.getValue() != null && dbCreatedTo.getValue() != null)
		{
			if (dbCreatedFrom.getValue().after(dbCreatedTo.getValue()))
				throw new WrongValueException(dbCreatedFrom, "Invalid Date Range");
			else
			{
				value = new ArrayList<Object>();
				value.add(DMSConstant.dateFormatWithTime.format(dbCreatedFrom.getValue()));
				value.add(DMSConstant.dateFormatWithTime.format(dbCreatedTo.getValue()));
				params.put(DMSConstant.CREATED, value);
			}
		}
		else if (dbCreatedFrom.getValue() != null && dbCreatedTo.getValue() == null)
		{
			value = new ArrayList<Object>();
			value.add(DMSConstant.dateFormatWithTime.format(dbCreatedFrom.getValue()));
			value.add("*");
			params.put(DMSConstant.CREATED, value);
		}
		else if (dbCreatedTo.getValue() != null && dbCreatedFrom.getValue() == null)
		{
			value = new ArrayList<Object>();
			value.add("*");
			value.add(DMSConstant.dateFormatWithTime.format(dbCreatedTo.getValue()));
			params.put(DMSConstant.CREATED, value);
		}

		if (dbUpdatedFrom.getValue() != null && dbUpdatedTo.getValue() != null)
		{
			if (dbUpdatedFrom.getValue().after(dbUpdatedTo.getValue()))
				throw new WrongValueException(dbUpdatedFrom, "Invalid Date Range");
			else
			{
				value = new ArrayList<Object>();
				value.add(DMSConstant.dateFormatWithTime.format(dbUpdatedFrom.getValue()));
				value.add(DMSConstant.dateFormatWithTime.format(dbUpdatedTo.getValue()));
				params.put(DMSConstant.UPDATED, value);
			}

		}
		else if (dbUpdatedFrom.getValue() != null && dbUpdatedTo.getValue() == null)
		{
			value = new ArrayList<Object>();
			value.add(DMSConstant.dateFormatWithTime.format(dbUpdatedFrom.getValue()));
			value.add("*");
			params.put(DMSConstant.UPDATED, value);
		}
		else if (dbUpdatedTo.getValue() != null && dbUpdatedFrom.getValue() == null)
		{
			value = new ArrayList<Object>();
			value.add("*");
			value.add(DMSConstant.dateFormatWithTime.format(dbUpdatedTo.getValue()));
			params.put(DMSConstant.UPDATED, value);
		}

		if (lstboxCreatedBy.getValue() != null)
		{
			value = new ArrayList<Object>();
			value.add(lstboxCreatedBy.getValue());
			params.put(DMSConstant.CREATEDBY, value);
		}

		if (lstboxUpdatedBy.getValue() != null)
		{
			value = new ArrayList<Object>();
			value.add(lstboxUpdatedBy.getValue());
			params.put(DMSConstant.UPDATEDBY, value);
		}

		// if chkInActive = true, display all files
		// if chkInActive = false, display only active files
		if (chkInActive != null)
		{
			value = new ArrayList<Object>();
			value.add(chkInActive.isChecked());
			if (chkInActive.isChecked())
			{
				value.add(!chkInActive.isChecked());
			}
			params.put(DMSConstant.SHOW_INACTIVE, value);
		}

		if (lstboxContentType.getValue() != null)
		{

			value = new ArrayList<Object>();
			value.add(lstboxContentType.getValue());
			params.put(DMSConstant.CONTENTTYPE, value);

			for (WEditor editor : m_editors)
			{
				// if (editor.getValue() != null && editor.getValue() != "")
				// {
				int displayType = editor.getGridField().getDisplayType();
				String compName = null;

				if (displayType == DisplayType.Search || displayType == DisplayType.Table || displayType == DisplayType.List)
					compName = "ASI_" + editor.getColumnName().replaceAll("(?i)[^a-z0-9-_/]", "_");
				else
					compName = "ASI_" + editor.getLabel().getValue().replaceAll("(?i)[^a-z0-9-_/]", "_");

				compName = compName.replaceAll("/", "");

				if (displayType == DisplayType.Number || displayType == DisplayType.Integer || displayType == DisplayType.Quantity
						|| displayType == DisplayType.Amount || displayType == DisplayType.CostPrice)
				{
					NumberBox fromNumBox = (NumberBox) ASI_Value.get(compName);
					NumberBox toNumBox = (NumberBox) ASI_Value.get(compName + "to");

					if (fromNumBox.getValue() != null && toNumBox.getValue() != null)
					{
						value = new ArrayList<Object>();

						if (displayType == DisplayType.Number)
						{
							value.add(fromNumBox.getValue().doubleValue());
							value.add(toNumBox.getValue().doubleValue());
						}
						else
						{
							value.add(fromNumBox.getValue());
							value.add(toNumBox.getValue());
						}
						params.put(compName, value);
					}
					else if (fromNumBox.getValue() != null && toNumBox.getValue() == null)
					{
						value = new ArrayList<Object>();

						if (displayType == DisplayType.Number)
						{
							value.add(fromNumBox.getValue().doubleValue());
						}
						else
						{
							value.add(fromNumBox.getValue());
						}
						value.add("*");
						params.put(compName, value);
					}
					else if (fromNumBox.getValue() == null && toNumBox.getValue() != null)
					{
						value = new ArrayList<Object>();
						value.add("*");
						if (displayType == DisplayType.Number)
						{
							value.add(toNumBox.getValue().doubleValue());
						}
						else
						{
							value.add(toNumBox.getValue());
						}
						params.put(compName, value);
					}
				}
				else if (displayType == DisplayType.Date || displayType == DisplayType.DateTime || displayType == DisplayType.Time)
				{

					if (displayType == DisplayType.Date)
					{
						Datebox fromDate = (Datebox) ASI_Value.get(compName);
						Datebox toDate = (Datebox) ASI_Value.get(compName + "to");

						if (fromDate.getValue() != null && toDate.getValue() != null)
						{
							if (fromDate.getValue().after(toDate.getValue()))
							{
								Clients.scrollIntoView(fromDate);
								throw new WrongValueException(fromDate, "Invalid Date Range");
							}
							else
							{
								value = new ArrayList<Object>();
								value.add(DMSConstant.dateFormatWithTime.format(fromDate.getValue()));
								value.add(DMSConstant.dateFormatWithTime.format(toDate.getValue()));
								params.put(compName, value);
							}
						}
						else if (fromDate.getValue() != null && toDate.getValue() == null)
						{
							value = new ArrayList<Object>();
							value.add(DMSConstant.dateFormatWithTime.format(fromDate.getValue()));
							value.add("*");
							params.put(compName, value);
						}
						else if (toDate.getValue() != null && fromDate.getValue() == null)
						{
							value = new ArrayList<Object>();
							value.add("*");
							value.add(DMSConstant.dateFormatWithTime.format(toDate.getValue()));
							params.put(compName, value);
						}
					}
					else if (displayType == DisplayType.DateTime)
					{
						DatetimeBox fromDatetime = (DatetimeBox) ASI_Value.get(compName);
						DatetimeBox toDatetime = (DatetimeBox) ASI_Value.get(compName + "to");

						if (fromDatetime.getValue() != null && toDatetime.getValue() != null)
						{
							if (fromDatetime.getValue().after(toDatetime.getValue()))
								throw new WrongValueException(fromDatetime, "Invalid Date Range");
							else
							{
								value = new ArrayList<Object>();
								value.add(DMSConstant.dateFormatWithTime.format(fromDatetime.getValue()));
								value.add(DMSConstant.dateFormatWithTime.format(toDatetime.getValue()));
								params.put(compName, value);
							}
						}
						else if (fromDatetime.getValue() != null && toDatetime.getValue() == null)
						{
							value = new ArrayList<Object>();
							value.add(DMSConstant.dateFormatWithTime.format(fromDatetime.getValue()));
							value.add("*");
							params.put(compName, value);
						}
						else if (toDatetime.getValue() != null && fromDatetime.getValue() == null)
						{
							value = new ArrayList<Object>();
							value.add("*");
							value.add(DMSConstant.dateFormatWithTime.format(toDatetime.getValue()));
							params.put(compName, value);
						}

					}
					else if (displayType == DisplayType.Time)
					{
						Timebox timeboxFrom = (Timebox) ASI_Value.get(compName);
						Timebox timeboxTo = (Timebox) ASI_Value.get(compName + "to");

						if (timeboxFrom.getValue() != null && timeboxTo.getValue() != null)
						{
							if (timeboxFrom.getValue().after(timeboxTo.getValue()))
								throw new WrongValueException(timeboxFrom, "Invalid Date Range");
							else
							{
								value = new ArrayList<Object>();
								value.add(DMSConstant.dateFormatWithTime.format(timeboxFrom.getValue()));
								value.add(DMSConstant.dateFormatWithTime.format(timeboxTo.getValue()));
								params.put(compName, value);
							}
						}
						else if (timeboxFrom.getValue() != null && timeboxTo.getValue() == null)
						{
							value = new ArrayList<Object>();
							value.add(DMSConstant.dateFormatWithTime.format(timeboxFrom.getValue()));
							value.add("*");
							params.put(compName, value);
						}
						else if (timeboxTo.getValue() != null && timeboxFrom.getValue() == null)
						{
							value = new ArrayList<Object>();
							value.add("*");
							value.add(DMSConstant.dateFormatWithTime.format(timeboxTo.getValue()));
							params.put(compName, value);
						}

					}
				}
				else if (displayType == DisplayType.YesNo)
				{
					value = new ArrayList<Object>();

					if ((boolean) editor.getValue())
						value.add("Y");
					else
						value.add("N");

					params.put(compName, value);
				}
				else if (displayType == DisplayType.String || displayType == DisplayType.Text)
				{
					if (!Util.isEmpty(editor.getValue().toString(), true))
					{
						value = new ArrayList<Object>();
						value.add(editor.getValue().toString().toLowerCase());
						params.put(compName, value);
					}
				}
				else if (!Util.isEmpty(editor.getDisplay()))
				{
					value = new ArrayList<Object>();
					value.add(editor.getValue());
					params.put(compName, value);
				}
				// }
			}
		}

		if (tableID > 0)
		{
			value = new ArrayList<Object>();
			value.add(tableID);
			params.put(DMSConstant.AD_Table_ID, value);
		}

		if (recordID > 0)
		{
			value = new ArrayList<Object>();
			value.add(recordID);
			params.put(DMSConstant.RECORD_ID, value);
		}

		return params;
	} // getQueryParamas

	@Override
	public void valueChange(ValueChangeEvent event)
	{
		if (event.getSource().equals(lstboxContentType))
		{
			Components.removeAllChildren(panelAttribute);

			Grid gridView = GridFactory.newGridLayout();

			gridView.setHeight("100%");

			Columns columns = new Columns();

			Column column = new Column();
			columns.appendChild(column);

			column = new Column();
			columns.appendChild(column);

			column = new Column();
			columns.appendChild(column);

			Rows rows = new Rows();

			gridView.appendChild(rows);
			gridView.appendChild(columns);

			if (lstboxContentType.getValue() != null)
			{
				ASI_Value.clear();
				asiPanel = new WDLoadASIPanel((int) lstboxContentType.getValue(), 0);
				m_editors = asiPanel.m_editors;

				for (WEditor editor : m_editors)
				{
					Row row = new Row();
					rows.appendChild(row);

					int displayType = editor.getGridField().getDisplayType();
					String compName = null;

					if (displayType == DisplayType.Search || displayType == DisplayType.Table)
						compName = "ASI_" + editor.getColumnName().replaceAll("(?i)[^a-z0-9-_/]", "_");
					else
						compName = "ASI_" + editor.getLabel().getValue().replaceAll("(?i)[^a-z0-9-_/]", "_");

					compName = compName.replaceAll("/", "");

					if (displayType == DisplayType.Number || displayType == DisplayType.Integer || displayType == DisplayType.Quantity
							|| displayType == DisplayType.Amount || displayType == DisplayType.CostPrice)
					{
						NumberBox numBox = new NumberBox(false);

						row.appendChild(editor.getLabel());
						row.appendChild(editor.getComponent());
						row.appendChild(numBox);

						ASI_Value.put(compName, editor.getComponent());
						ASI_Value.put(compName + "to", numBox);

					}

					else if (displayType == DisplayType.Date || displayType == DisplayType.DateTime || displayType == DisplayType.Time)
					{

						if (displayType == DisplayType.Date)
						{
							Datebox dateBox = new Datebox();
							dateBox.setName(compName + "to");
							row.appendChild(editor.getLabel());
							row.appendChild(editor.getComponent());
							row.appendChild(dateBox);

							ASI_Value.put(compName, editor.getComponent());
							ASI_Value.put(dateBox.getName(), dateBox);
						}
						else if (displayType == DisplayType.Time)
						{
							Timebox timebox = new Timebox();
							timebox.setFormat("h:mm:ss a");
							timebox.setWidth("100%");
							timebox.setName(compName + "to");
							row.appendChild(editor.getLabel());
							row.appendChild(editor.getComponent());
							row.appendChild(timebox);

							ASI_Value.put(compName, editor.getComponent());
							ASI_Value.put(timebox.getName(), timebox);
						}
						else if (displayType == DisplayType.DateTime)
						{
							DatetimeBox datetimeBox = new DatetimeBox();

							Cell cell = new Cell();
							cell.setColspan(2);

							row.appendChild(editor.getLabel());
							cell.appendChild(editor.getComponent());
							row.appendChild(cell);

							ASI_Value.put(compName, editor.getComponent());

							row = new Row();
							rows.appendChild(row);

							row.appendChild(new Space());
							cell = new Cell();
							cell.setColspan(2);
							cell.appendChild(datetimeBox);
							row.appendChild(cell);

							ASI_Value.put(compName + "to", datetimeBox);
						}
					}
					else
					{
						Cell cell = new Cell();
						cell.setColspan(2);
						row.appendChild(editor.getLabel());
						cell.appendChild(editor.getComponent());
						row.appendChild(cell);

						ASI_Value.put(editor.getLabel().getValue(), editor.getComponent());
					}
				}
				panelAttribute.appendChild(gridView);
			}
		}
	} // valueChange

	private void showBreadcumb(MDMSContent breadcumbContent)
	{
		Components.removeAllChildren(gridBreadCrumb);

		lblShowBreadCrumb = new Label();
		lblShowBreadCrumb.setValue(" > ");
		breadRow.appendChild(new Space());
		breadRow.appendChild(lblShowBreadCrumb);

		BreadCrumbLink breadCrumbLink = new BreadCrumbLink();
		breadCrumbLink.setPathId(String.valueOf(breadcumbContent.getDMS_Content_ID()));
		breadCrumbLink.addEventListener(Events.ON_CLICK, this);
		// breadCrumbLink.addEventListener(Events.ON_MOUSE_OVER, this);
		breadCrumbLink.setLabel(breadcumbContent.getName());
		breadCrumbLink.setStyle("font-weight: bold; font-size: small; padding-left: 15px; color: dimgray;");

		breadRow.appendChild(breadCrumbLink);
		breadRows.appendChild(breadRow);
		gridBreadCrumb.appendChild(breadRows);
	} // showBreadcumb

	public List<BreadCrumbLink> getParentLinks()
	{
		List<BreadCrumbLink> parents = new ArrayList<BreadCrumbLink>();
		for (Component component : breadRow.getChildren())
		{
			if (component instanceof BreadCrumbLink)
				parents.add((BreadCrumbLink) component);
		}
		return parents;
	}

	public void addRootBreadCrumb()
	{
		BreadCrumbLink rootBreadCrumbLink = new BreadCrumbLink();

		rootBreadCrumbLink.setImageContent(Utils.getImage("Home24.png"));
		rootBreadCrumbLink.setPathId(String.valueOf(0));
		rootBreadCrumbLink.addEventListener(Events.ON_CLICK, this);
		rootBreadCrumbLink.setStyle("font-weight: bold; font-size: small; padding-left: 15px; color: dimgray;");

		breadRow.appendChild(rootBreadCrumbLink);
		lblShowBreadCrumb = new Label();
		// lblShowBreadCrumb.setValue(" > ");
		breadRow.appendChild(new Space());
		breadRow.appendChild(lblShowBreadCrumb);

		breadRows.appendChild(breadRow);
		gridBreadCrumb.appendChild(breadRows);
	}

}
