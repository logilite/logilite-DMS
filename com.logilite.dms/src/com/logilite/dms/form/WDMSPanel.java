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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.adwindow.BreadCrumbLink;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Combobox;
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
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WDatetimeEditor;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WNumberEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.editor.WTimeEditor;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.FDialog;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment.WorkbookNotFoundException;
import org.compiere.model.MClientInfo;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MToolBarButtonRestrict;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Cell;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Splitter;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.impl.XulElement;

import com.logilite.dms.DMS;
import com.logilite.dms.DMS_ZK_Util;
import com.logilite.dms.component.AbstractComponentIconViewer;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.DMSClipboard;
import com.logilite.dms.factories.IContentTypeAccess;
import com.logilite.dms.factories.IDMSUploadContent;
import com.logilite.dms.factories.IPermissionManager;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSAssociationType;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSContentType;
import com.logilite.dms.model.MDMSMimeType;
import com.logilite.dms.model.MDMSVersion;
import com.logilite.dms.service.CreateZipArchive;
import com.logilite.dms.util.DMSConvertToPDFUtils;
import com.logilite.dms.util.DMSFactoryUtils;
import com.logilite.dms.util.DMSPermissionUtils;
import com.logilite.dms.util.DMSSearchUtils;

public class WDMSPanel extends Panel implements EventListener<Event>, ValueChangeListener
{
	private static final long		serialVersionUID		= -6813481516566180243L;
	private static CLogger			log						= CLogger.getCLogger(WDMSPanel.class);

	private static final String		ATTRIBUTE_TOGGLE		= "Toggle";

	private static int				TOOLBAR_BTN_ID_DIR		= 0;
	private static int				TOOLBAR_BTN_ID_UPLOAD	= 0;

	private String					currThumbViewerAction	= DMSConstant.ICON_VIEW_LARGE;

	private Tabbox					tabBox					= new Tabbox();
	private Tabs					tabs					= new Tabs();
	public Tabpanels				tabPanels				= new Tabpanels();

	private Grid					grid					= GridFactory.newGridLayout();
	private Grid					gridBreadCrumb			= GridFactory.newGridLayout();

	private BreadCrumbLink			breadCrumbEvent			= null;

	private Rows					breadRows				= new Rows();
	private Row						breadRow				= new Row();

	private Searchbox				vsearchBox				= new Searchbox();

	private Label					lblAdvanceSearch		= new Label(DMSConstant.MSG_ADVANCE_SEARCH);
	private Label					lblDocumentName			= new Label(DMSConstant.MSG_NAME);
	private Label					lblContentType			= new Label(DMSConstant.MSG_CONTENT_TYPE);
	private Label					lblCreated				= new Label(DMSConstant.MSG_CREATED);
	private Label					lblUpdated				= new Label(DMSConstant.MSG_UPDATED);
	private Label					lblContentMeta			= new Label(DMSConstant.MSG_CONTENT_META);
	private Label					lblDescription			= new Label(DMSConstant.MSG_DESCRIPTION);
	private Label					lblCreatedBy			= new Label(DMSConstant.MSG_CREATEDBY);
	private Label					lblUpdatedBy			= new Label(DMSConstant.MSG_UPDATEDBY);
	private Label					lblDocumentView			= new Label(DMSConstant.MSG_DOCUMENT_VIEW);
	private Label					lblPositionInfo			= new Label();
	private Label					lblCountAndSelected		= new Label();
	private Label					lblShowBreadCrumb		= null;

	private Datebox					dbCreatedTo				= new Datebox();
	private Datebox					dbCreatedFrom			= new Datebox();
	private Datebox					dbUpdatedTo				= new Datebox();
	private Datebox					dbUpdatedFrom			= new Datebox();

	private ConfirmPanel			confirmPanel			= new ConfirmPanel();

	private Button					btnClear				= confirmPanel.createButton(ConfirmPanel.A_RESET);
	private Button					btnCloseTab				= confirmPanel.createButton(ConfirmPanel.A_CANCEL);
	private Button					btnSearch				= new Button();
	private Button					btnCreateDir			= new Button();
	private Button					btnUploadContent		= new Button();
	private Button					btnBack					= new Button();
	private Button					btnNext					= new Button();
	private Button					btnToggleView			= new Button();

	private Textbox					txtDocumentName			= new Textbox();
	private Textbox					txtDescription			= new Textbox();

	private Combobox				cobDocumentView			= null;

	private WTableDirEditor			lstboxContentType		= null;
	private WSearchEditor			lstboxCreatedBy			= null;
	private WSearchEditor			lstboxUpdatedBy			= null;

	private DMS						dms						= null;
	private MDMSContent				currDMSContent			= null;
	private MDMSContent				nextDMSContent			= null;
	private MDMSContent				copyDMSContent			= null;
	private MDMSContent				dirContent				= null;

	private Stack<MDMSVersion>		selectedDMSVersionStack	= new Stack<MDMSVersion>();

	//
	private Component				compCellRowViewer		= null;
	private IDMSUploadContent		uploadContent			= null;
	private WCreateDirectoryForm	createDirectoryForm		= null;
	private WDLoadASIPanel			asiPanel				= null;

	private Panel					panelAttribute			= new Panel();

	private Menupopup				contentContextMenu		= new Menupopup();
	private Menupopup				canvasContextMenu		= new Menupopup();

	private Menuitem				mnu_cut					= null;
	private Menuitem				mnu_copy				= null;
	private Menuitem				mnu_paste				= null;
	private Menuitem				mnu_rename				= null;
	private Menuitem				mnu_delete				= null;
	private Menuitem				mnu_download			= null;
	private Menuitem				mnu_associate			= null;
	private Menuitem				mnu_createLink			= null;
	private Menuitem				mnu_permission			= null;
	private Menuitem				mnu_undoDelete			= null;
	private Menuitem				mnu_versionList			= null;
	private Menuitem				mnu_uploadVersion		= null;
	private Menuitem				mnu_zoomContentWin		= null;
	private Menuitem				mnu_owner				= null;

	private Menuitem				mnu_canvasPaste			= null;
	private Menuitem				mnu_canvasCreateLink	= null;

	private int						recordID				= 0;
	private int						tableID					= 0;
	private int						windowID				= 0;
	private int						tabID					= 0;

	private int						windowNo				= 0;
	private int						tabNo					= 0;

	private boolean					isDMSAdmin				= false;
	private boolean					isSearch				= false;
	private boolean					isGenericSearch			= false;
	private boolean					isWindowAccess			= true;
	private boolean					isDocExplorerWindow		= false;
	private boolean					isMountingBaseStructure	= false;

	private ArrayList<WEditor>		m_editors				= new ArrayList<WEditor>();

	private Map<String, WEditor>	ASI_Value				= new HashMap<String, WEditor>();

	private AbstractADWindowContent	winContent;

	protected Set<I_DMS_Version>	downloadSet				= new HashSet<I_DMS_Version>();

	/**
	 * Constructor initialize
	 */
	public WDMSPanel(int windowNo, int tabNo)
	{
		dms = new DMS(Env.getAD_Client_ID(Env.getCtx()));
		this.windowNo = windowNo;
		this.tabNo = tabNo;

		isDMSAdmin = MRole.getDefault().get_ValueAsBoolean(DMSConstant.COLUMNNAME_IS_DMS_ADMIN);

		initForm();
	} // Constructor

	public WDMSPanel(int Table_ID, int Record_ID, int windowNo, int tabNo)
	{
		this(windowNo, tabNo);

		initZK(Table_ID, Record_ID);

		setCurrDMSContent(dms.getRootMountingContent(tableID, recordID));

		renderViewer();
	} // Constructor

	public WDMSPanel(int Table_ID, int Record_ID, AbstractADWindowContent winContent)
	{
		this(winContent.getWindowNo(), winContent.getActiveGridTab().getTabNo());

		this.winContent = winContent;
		this.windowID = winContent.getADWindow().getAD_Window_ID();
		this.tabID = winContent.getActiveGridTab().getAD_Tab_ID();
		this.isWindowAccess = MRole.getDefault().getWindowAccess(windowID);

		initZK(Table_ID, Record_ID);
	} // Constructor

	public void initZK(int Table_ID, int Record_ID)
	{
		// Toolbar button restriction
		if (TOOLBAR_BTN_ID_DIR <= 0)
			TOOLBAR_BTN_ID_DIR = DB.getSQLValue(null, DMSConstant.SQL_GET_TOOLBAR_BUTTON_ID, dms.AD_Client_ID, DMSConstant.TOOLBAR_BTN_NAME_DIR);

		if (TOOLBAR_BTN_ID_UPLOAD <= 0)
			TOOLBAR_BTN_ID_UPLOAD = DB.getSQLValue(null, DMSConstant.SQL_GET_TOOLBAR_BUTTON_ID, dms.AD_Client_ID, DMSConstant.TOOLBAR_BTN_NAME_UPLOAD);

		btnCreateDir.setVisible(!MToolBarButtonRestrict.isToolbarButtonRestricted(windowID, tabID, TOOLBAR_BTN_ID_DIR));
		btnUploadContent.setVisible(!MToolBarButtonRestrict.isToolbarButtonRestricted(windowID, tabID, TOOLBAR_BTN_ID_UPLOAD));

		setTable_ID(Table_ID);
		setRecord_ID(Record_ID);

		String tableName = MTable.getTableName(Env.getCtx(), Table_ID);
		dms.initMountingStrategy(tableName);
		dms.initiateMountingContent(tableName, Record_ID, Table_ID);

		currDMSContent = dms.getRootMountingContent(Table_ID, Record_ID);

		btnUploadContent.setEnabled(isWindowAccess);

		allowUserToCreateDir();
	} // initZK

	/*
	 * Navigation and createDir buttons are disabled based on
	 * "IsAllowCreateDirectory" check on client info.
	 */
	public void allowUserToCreateDir()
	{
		boolean isAllowCreateDirectory = MClientInfo.get(Env.getCtx(), dms.AD_Client_ID).get_ValueAsBoolean(DMSConstant.COLUMNNAME_IS_ALLOW_CREATE_DIRECTORY);

		if (isTabViewer() && !isAllowCreateDirectory)
		{
			setNavigationButtonEnabled(false);
			btnCreateDir.setEnabled(false);
		}

		if (isMountingBaseStructure || (currDMSContent != null && !currDMSContent.isActive()))
		{
			setButtonsContentCreationEnabled(false);
		}
		else if (isDocExplorerWindow || (currDMSContent != null && currDMSContent.isActive()))
		{
			setButtonsContentCreationEnabled(true);
		}

		if (currDMSContent != null && !dms.isWritePermission(currDMSContent) && !dms.isAllPermissionGranted(currDMSContent))
		{
			if (!currDMSContent.isMounting())
				setButtonsContentCreationEnabled(false);
		}

	} // allowUserToCreateDir

	public DMS getDMS()
	{
		return dms;
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

	public void setTable_ID(int table_ID)
	{
		this.tableID = table_ID;
	}

	public void setWindow_ID(int AD_Window_ID)
	{
		this.windowID = AD_Window_ID;
	}

	public int getWindow_ID()
	{
		return windowID;
	}

	public boolean isDocExplorerWindow()
	{
		return isDocExplorerWindow;
	}

	public void setDocExplorerWindow(boolean isDocExplorerWindow)
	{
		this.isDocExplorerWindow = isDocExplorerWindow;
	}

	public MDMSContent getCurrDMSContent()
	{
		return currDMSContent;
	}

	public void setCurrDMSContent(MDMSContent currDMSContent)
	{
		this.currDMSContent = currDMSContent;
		MDMSVersion version = null;
		if (currDMSContent != null)
			version = (MDMSVersion) MDMSVersion.getLatestVersion(currDMSContent);
		selectedDMSVersionStack.add(version);
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
		// Load DMS CSS file content and attach as style tag in Head tab
		DMS_ZK_Util.loadDMSThemeCSSFile();
		DMS_ZK_Util.loadDMSMobileCSSFile();

		ZKUpdateUtil.setHeight(this, "100%");
		ZKUpdateUtil.setWidth(this, "100%");
		this.appendChild(tabBox);
		this.addEventListener(Events.ON_CLICK, this);
		this.addEventListener(DMSConstant.EVENT_ON_SELECTION_CHANGE, this);

		grid.setSclass("SB-Grid");
		grid.addEventListener(Events.ON_DOUBLE_CLICK, this);
		grid.addEventListener(Events.ON_RIGHT_CLICK, this); // For_Canvas_Context_Menu
		grid.setStyle("width: 100%; height: calc( 100% - 45px); position: relative; overflow: auto;");//

		// View Result Tab
		Grid btnGrid = GridFactory.newGridLayout();
		// Rows Header Buttons
		Rows rowsBtn = btnGrid.newRows();

		// Row Navigation Button
		DMS_ZK_Util.setButtonData(btnBack, "PreviousRecord", DMSConstant.TTT_PREVIOUS_RECORD, this);
		btnBack.setEnabled(false);

		lblPositionInfo.setHflex("1");
		ZkCssHelper.appendStyle(lblPositionInfo, "float: right; font-weight: bold; text-align: center; font-size: 12px; padding-right: 2px;");

		lblCountAndSelected.setHflex("1");
		ZkCssHelper.appendStyle(lblCountAndSelected, "font-size: 12px; border-left: 1px solid black; padding-left: 2px;");

		btnNext.setEnabled(false);
		btnNext.setStyle("float:right;");
		DMS_ZK_Util.setButtonData(btnNext, "NextRecord", DMSConstant.TTT_NEXT_RECORD, this);

		Row row = rowsBtn.newRow();
		row.appendChild(btnBack);
		row.appendChild(lblPositionInfo);
		row.appendCellChild(lblCountAndSelected);
		row.appendChild(btnNext);

		// Row Operation - Create Directory, Upload Content
		DMS_ZK_Util.setButtonData(btnCreateDir, "Folder", DMSConstant.MSG_CREATE_DIRECTORY, this);
		DMS_ZK_Util.setButtonData(btnUploadContent, "UploadFile", DMSConstant.MSG_UPLOAD_CONTENT, this);

		row = rowsBtn.newRow();
		Hbox hbox = new Hbox();
		hbox.appendChild(btnCreateDir);
		hbox.appendChild(btnUploadContent);
		DMS_ZK_Util.createCellUnderRow(row, 0, 3, hbox);

		cobDocumentView = new Combobox();
		cobDocumentView.appendItem(DMSConstant.DOCUMENT_VIEW_ALL, DMSConstant.DOCUMENT_VIEW_ALL_VALUE);
		cobDocumentView.appendItem(DMSConstant.DOCUMENT_VIEW_DELETED_ONLY, DMSConstant.DOCUMENT_VIEW_DELETED_ONLY_VALUE);
		cobDocumentView.appendItem(DMSConstant.DOCUMENT_VIEW_NON_DELETED, DMSConstant.DOCUMENT_VIEW_NON_DELETED_VALUE);
		cobDocumentView.setSelectedIndex(2);

		if (isDMSAdmin)
		{
			row = rowsBtn.newRow();
			DMS_ZK_Util.createCellUnderRow(row, 1, 1, lblDocumentView);
			DMS_ZK_Util.createCellUnderRow(row, 1, 2, cobDocumentView);
			ZKUpdateUtil.setWidth(cobDocumentView, "99%");
			cobDocumentView.addEventListener(Events.ON_SELECT, this);
		}

		//
		Grid searchGridView = GridFactory.newGridLayout();
		searchGridView.setVflex(true);
		searchGridView.setStyle("max-height: 100%; width: 100%; height: calc( 100% - 90px); position: relative; overflow: auto;");

		Rows rowsSearch = searchGridView.newRows();

		row = rowsSearch.newRow();
		DMS_ZK_Util.createCellUnderRow(row, 1, 3, vsearchBox);

		ZKUpdateUtil.setWidth(vsearchBox, "100%");
		DMS_ZK_Util.setButtonData(vsearchBox.getButton(), "Search", DMSConstant.TTT_SEARCH, this);
		vsearchBox.addEventListener(Events.ON_OK, this);
		vsearchBox.getButton().setStyle("margin: 0px !important;");

		row = rowsSearch.newRow();
		row.appendCellChild(lblAdvanceSearch, 3);
		ZkCssHelper.appendStyle(lblAdvanceSearch, DMSConstant.CSS_HIGHLIGHT_LABEL);

		row = rowsSearch.newRow();
		row.appendCellChild(lblDocumentName);
		row.appendCellChild(txtDocumentName, 2);
		ZkCssHelper.appendStyle(lblDocumentName, "font-weight: bold;");

		row = rowsSearch.newRow();
		row.appendCellChild(lblDescription);
		row.appendCellChild(txtDescription, 2);

		Language lang = Env.getLanguage(Env.getCtx());
		int Column_ID = MColumn.getColumn_ID(MUser.Table_Name, MUser.COLUMNNAME_AD_User_ID);
		MLookup lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, Column_ID, DisplayType.Search, lang, MUser.COLUMNNAME_AD_User_ID, 0, true, "");
			lstboxCreatedBy = new WSearchEditor(MUser.COLUMNNAME_AD_User_ID, false, false, true, lookup);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "User fetching failure :", e);
			throw new AdempiereException("User fetching failure :" + e);
		}

		row = rowsSearch.newRow();
		row.appendCellChild(lblCreatedBy);
		row.appendCellChild(lstboxCreatedBy.getComponent(), 2);
		lblCreatedBy.setStyle("float: left;");

		Column_ID = MColumn.getColumn_ID(MUser.Table_Name, MUser.COLUMNNAME_AD_User_ID);
		lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, Column_ID, DisplayType.Search, lang, MUser.COLUMNNAME_AD_User_ID, 0, true, "");
			lstboxUpdatedBy = new WSearchEditor(MUser.COLUMNNAME_AD_User_ID, false, false, true, lookup);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "User fetching failure :", e);
			throw new AdempiereException("User fetching failure :" + e);
		}

		row = rowsSearch.newRow();
		row.appendCellChild(lblUpdatedBy);
		row.appendCellChild(lstboxUpdatedBy.getComponent(), 2);
		lblUpdatedBy.setStyle("float: left;");

		dbCreatedFrom.setStyle(DMSConstant.CSS_DATEBOX);
		dbUpdatedFrom.setStyle(DMSConstant.CSS_DATEBOX);
		dbCreatedTo.setStyle(DMSConstant.CSS_DATEBOX);
		dbUpdatedTo.setStyle(DMSConstant.CSS_DATEBOX);

		//
		row = rowsSearch.newRow();
		row.setSclass("SB-Grid-field");
		row.appendCellChild(lblCreated);
		row.appendCellChild(dbCreatedFrom);
		row.appendCellChild(dbCreatedTo);

		//
		row = rowsSearch.newRow();
		row.setSclass("SB-Grid-field");
		row.appendCellChild(lblUpdated);
		row.appendCellChild(dbUpdatedFrom);
		row.appendCellChild(dbUpdatedTo);

		Column_ID = MColumn.getColumn_ID(MDMSContent.Table_Name, MDMSContent.COLUMNNAME_DMS_ContentType_ID);
		lookup = MLookupFactory.get(Env.getCtx(), windowNo, tabNo, Column_ID, DisplayType.TableDir);
		lookup.refresh();
		lstboxContentType = new WTableDirEditor(MDMSContentType.COLUMNNAME_DMS_ContentType_ID, false, false, true, lookup);

		//
		row = rowsSearch.newRow();
		row.setSclass("SB-Grid-field");
		row.appendCellChild(lblContentType);
		row.appendCellChild(lstboxContentType.getComponent(), 2);
		lblContentType.setStyle("float: left;");
		lstboxContentType.addValueChangeListener(this);

		//
		row = rowsSearch.newRow();
		row.appendCellChild(lblContentMeta, 3);
		ZkCssHelper.appendStyle(lblContentMeta, DMSConstant.CSS_HIGHLIGHT_LABEL);
		lblContentMeta.setVisible(false);
		//
		row = rowsSearch.newRow();
		DMS_ZK_Util.createCellUnderRow(row, 0, 3, panelAttribute);

		//
		row = rowsSearch.newRow();

		DMS_ZK_Util.setButtonData(btnClear, "Reset", "Clear", this);
		DMS_ZK_Util.setButtonData(btnSearch, "Search", DMSConstant.TTT_SEARCH, this);
		DMS_ZK_Util.setButtonData(btnCloseTab, "Home", "Clear all & Go to Home Directory", this);
		DMS_ZK_Util.setButtonData(btnToggleView, "List", DMSConstant.TTT_DISPLAYS_ITEMS_LAYOUT, this);

		btnToggleView.setAttribute(ATTRIBUTE_TOGGLE, currThumbViewerAction);
		btnToggleView.setStyle("float: left; padding: 5px 7px; margin: 0px 0px 5px 0px !important; height: 45px; width: 45px;");

		hbox = new Hbox();
		hbox.setStyle(DMSConstant.CSS_FLEX_ROW_DIRECTION);
		hbox.appendChild(btnSearch);
		hbox.appendChild(btnClear);
		hbox.appendChild(btnCloseTab);

		Cell cell = DMS_ZK_Util.createCellUnderRow(row, 1, 3, hbox);
		cell.setAlign("right");

		/*
		 * Main Layout View
		 */
		Cell cell_layout = new Cell();
		cell_layout.appendChild(btnToggleView);
		cell_layout.appendChild(gridBreadCrumb);
		cell_layout.appendChild(grid);
		cell_layout.setHeight("100%");

		gridBreadCrumb.setClass("dms-breadcrumb");
		gridBreadCrumb.setStyle("font-family: Roboto,sans-serif; height: 45px; "
								+ "border: 1px solid #AAA !important; border-radius: 5px; box-shadow: 1px 1px 1px 0px; overflow-x: auto;");

		breadRow.setZclass("none");
		breadRow.setStyle(DMSConstant.CSS_FLEX_ROW_DIRECTION_NOWRAP);

		Splitter splitter = new Splitter();
		splitter.setCollapse("after");

		Cell cell_attribute = new Cell();
		cell_attribute.setHeight("100%");
		cell_attribute.appendChild(btnGrid);
		cell_attribute.appendChild(searchGridView);

		if (ClientInfo.isMobile())
		{
			Borderlayout borderViewSeparator = new Borderlayout();
			borderViewSeparator.setWidth("100%");
			borderViewSeparator.setHeight("100%");
			borderViewSeparator.appendCenter(cell_layout);
			borderViewSeparator.appendSouth(cell_attribute);

			South south = borderViewSeparator.getSouth();
			south.setZclass("SB-south " + south.getZclass());
			south.setStyle("max-height: 100%;");
			south.setSplittable(true);
			south.setCollapsible(true);
			south.setOpen(false);

			Tabpanel tabViewPanel = new Tabpanel();
			tabViewPanel.setHeight("100%");
			tabViewPanel.setWidth("100%");
			tabViewPanel.appendChild(borderViewSeparator);
			tabPanels.appendChild(tabViewPanel);
		}
		else
		{
			cell_layout.setWidth("70%");

			Hbox boxViewSeparator = new Hbox();
			boxViewSeparator.setWidth("100%");
			ZKUpdateUtil.setHeight(boxViewSeparator, "100%");
			boxViewSeparator.appendChild(cell_attribute);

			Borderlayout borderViewSeparator = new Borderlayout();
			borderViewSeparator.setStyle("min-height: 300px;");
			borderViewSeparator.appendCenter(cell_layout);
			borderViewSeparator.appendEast(boxViewSeparator);
			borderViewSeparator.getCenter().setSclass("SB-DMS-CenterView");

			East east = borderViewSeparator.getEast();
			east.setWidth("30%");
			east.setSplittable(true);
			east.setCollapsible(true);
			east.setAutoscroll(true);

			//
			Tabpanel tabViewPanel = new Tabpanel();
			tabViewPanel.setSclass("SB_DMS_Side_TabPanel");
			tabViewPanel.setHeight("100%");
			tabViewPanel.setWidth("100%");
			tabViewPanel.appendChild(borderViewSeparator);
			tabPanels.appendChild(tabViewPanel);
		}

		// tabPanels.setHeight("100%");

		tabBox.setWidth("100%");
		tabBox.setHeight("100%");
		tabBox.appendChild(tabs);
		tabBox.appendChild(tabPanels);
		tabBox.addEventListener(Events.ON_SELECT, this);

		tabs.appendChild(new Tab(DMSConstant.MSG_EXPLORER));

		// Context Menu item for Right click on DMSContent
		mnu_cut = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_CUT, "Cut", this);
		mnu_copy = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_COPY, "Copy", this);
		mnu_paste = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_PASTE, "Paste", this);
		mnu_owner = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_OWNER, "Owner", this);
		mnu_rename = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_RENAME, "Rename", this);
		mnu_delete = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_DELETE, "Delete", this);
		mnu_download = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_DOWNLOAD, "Download", this);
		mnu_associate = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_ASSOCIATE, "Associate", this);
		mnu_createLink = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_CREATELINK, "Link", this);
		mnu_permission = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_PERMISSION, "Permission", this);
		mnu_undoDelete = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_UN_ARCHIVE, "UndoDelete", this);
		mnu_versionList = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_VERSIONlIST, "Version", this);
		mnu_uploadVersion = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_UPLOADVERSION, "UploadVersion", this);
		mnu_zoomContentWin = DMS_ZK_Util.createMenuItem(contentContextMenu, DMSConstant.MENUITEM_ZOOMCONTENTWIN, "Zoom", this);

		// Context Menu item for Right click on Canvas area
		mnu_canvasPaste = DMS_ZK_Util.createMenuItem(canvasContextMenu, DMSConstant.MENUITEM_PASTE, "Paste", this);
		mnu_canvasCreateLink = DMS_ZK_Util.createMenuItem(canvasContextMenu, DMSConstant.MENUITEM_CREATELINK, "Link", this);

		//
		DMSConstant.SDF_DATE_FORMAT_WITH_TIME.setTimeZone(TimeZone.getTimeZone("UTC"));
		addRootBreadCrumb();
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (Events.ON_DOUBLE_CLICK.equals(event.getName()) && (event.getTarget() instanceof Cell || event.getTarget() instanceof Row))
		{
			openDirectoryORContent(event.getTarget());
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
			navigationBack();
		}
		else if (event.getTarget().equals(btnNext))
		{
			navigationNext();
		}
		else if (event.getTarget().equals(btnClear))
		{
			clearComponents();
		}
		else if (event.getTarget().equals(btnCloseTab))
		{
			isSearch = false;
			isGenericSearch = false;
			isMountingBaseStructure = false;
			breadRow.getChildren().clear();
			addRootBreadCrumb();
			setButtonsContentCreationEnabled(true);

			if (isTabViewer())
			{
				MDMSContent mountingContent = dms.getRootMountingContent(tableID, recordID);
				selectedDMSVersionStack.removeAllElements();
				setCurrDMSContent(mountingContent);

				if (currDMSContent != null)
					lblPositionInfo.setText(currDMSContent.getName());
				else
					lblPositionInfo.setText(String.valueOf(recordID));
			}
			else
			{
				currDMSContent = null;
				lblPositionInfo.setText(null);
			}

			setNavigationButtonEnabled(false);

			renderViewer();
		}
		else if (event.getTarget() == btnToggleView)
		{
			if (btnToggleView.getAttribute(ATTRIBUTE_TOGGLE).equals(DMSConstant.ICON_VIEW_LARGE))
				currThumbViewerAction = DMSConstant.ICON_VIEW_LIST;
			else
				currThumbViewerAction = DMSConstant.ICON_VIEW_LARGE;

			btnToggleView.setAttribute(ATTRIBUTE_TOGGLE, currThumbViewerAction);

			renderViewer();
		}
		// Event for any area of panel user doing right click then show context
		// related paste or else...
		else if (Events.ON_RIGHT_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(Grid.class))
		{
			openCanvasContextMenu(event);
		}
		else if (Events.ON_RIGHT_CLICK.equals(event.getName())
					&& (event.getTarget().getClass().equals(Cell.class) || event.getTarget().getClass().equals(Row.class)))
		{
			compCellRowViewer = event.getTarget();
			openContentContextMenu(compCellRowViewer);

			// show only download option on menu context if access are read-only.
			if (!isWindowAccess)
			{
				mnu_download.setDisabled(false);
				mnu_copy.setDisabled(false);
			}
		}
		else if (event.getTarget().equals(mnu_versionList))
		{
			new WDMSVersion(dms, (MDMSContent) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT));
		}
		else if (event.getTarget().equals(mnu_uploadVersion))
		{

			IDMSUploadContent uploadContent = DMSFactoryUtils.getUploadContenFactory(	dms, dirContent, true, this.getTable_ID(), this.getRecord_ID(), windowNo,
																						tabNo);
			((Component) uploadContent).addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

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
		else if (event.getTarget().equals(mnu_zoomContentWin))
		{
			AEnv.zoom(MDMSContent.Table_ID, dirContent.getDMS_Content_ID());
		}
		else if (event.getTarget().equals(mnu_copy))
		{
			DMSClipboard.put((MDMSContent) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT), true);
		}
		else if (event.getTarget().equals(mnu_createLink))
		{
			linkCopyDocument(dirContent, true);
		}
		else if (event.getTarget().equals(mnu_cut))
		{
			DMSClipboard.put((MDMSContent) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT), false);
		}
		else if (event.getTarget().equals(mnu_paste) || event.getTarget().equals(mnu_canvasPaste))
		{
			MDMSContent sourceContent = DMSClipboard.get();
			MDMSContent destPasteContent = dirContent;
			boolean isContentIDExists = false;

			if (sourceContent != null)
			{
				if (destPasteContent != null)
					isContentIDExists = dms.isHierarchyContentExists(destPasteContent.get_ID(), sourceContent.get_ID());

				if (destPasteContent != null && sourceContent.get_ID() == destPasteContent.get_ID() || isContentIDExists)
				{
					FDialog.warn(0, "You cannot copy a folder into itself");
				}
				else
				{
					if (DMSClipboard.getIsCopy())
					{
						// Create permission for paste content from parent if true
						if (DMSPermissionUtils.isPermissionAllowed() && !destPasteContent.isMounting())
						{
							Callback<Boolean> callbackConfirmation = new Callback<Boolean>() {
								@Override
								public void onCallback(Boolean isCreatePermissionForPasteContent)
								{
									dms.pasteCopyContent(sourceContent, destPasteContent, tableID, recordID, isCreatePermissionForPasteContent);
									renderViewer();
								}
							};

							// Grant same permission of 'Parent Content' to 'paste content'?
							FDialog.ask("Grant permission to the paste content ?", windowNo, this,
										"GrantPermissionToPasteContent?", callbackConfirmation);
						}
						else
						{
							dms.pasteCopyContent(sourceContent, destPasteContent, tableID, recordID, false);
							renderViewer();
						}
					}
					else
					{
						// TODO need to ask grant permission Dialog
						dms.pasteCutContent(sourceContent, destPasteContent, tableID, recordID);
						renderViewer();
					}
				}
			}
		}
		else if (event.getTarget().equals(mnu_download))
		{
			if (downloadSet == null || downloadSet.isEmpty())
			{
				I_DMS_Version version = MDMSVersion.getLatestVersion(dirContent);
				if (MDMSContent.CONTENTBASETYPE_Directory.equals(dirContent.getContentBaseType()))
				{
					downloadSet.add(version);
				}
				else
				{
					DMS_ZK_Util.downloadDocument(dms, version);
					return;
				}
			}

			if (downloadSet != null && !downloadSet.isEmpty())
			{
				CreateZipArchive createZip = new CreateZipArchive(dms, currDMSContent, downloadSet, cobDocumentView.getSelectedItem().getValue());
				createZip.downloadZip();
			}
		}
		else if (event.getTarget().equals(mnu_rename))
		{
			final WRenameContent renameContent = new WRenameContent(dms, dirContent, tableID, recordID);
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
			Callback<Boolean> callback = new Callback<Boolean>() {
				@Override
				public void onCallback(Boolean result)
				{
					if (result)
					{
						final MDMSContent deletableContent = (MDMSContent) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT);
						final MDMSAssociation deletableAssociation = (MDMSAssociation) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_ASSOCIATION);

						String warningMsg = dms.hasLinkableDocs(deletableContent, deletableAssociation);
						if (!Util.isEmpty(warningMsg, true))
						{
							Callback<Boolean> callbackWarning = new Callback<Boolean>() {

								@Override
								public void onCallback(Boolean result)
								{
									if (result)
									{
										//
										dms.deleteContent(deletableContent, deletableAssociation, true);
										renderViewer();
									}
								}
							};

							// Want to delete actual docs and associated its linkable documents ?
							FDialog.ask(windowNo, mnu_delete, "Want to Delete linkable references ?",
										"DeleteAssociatedLinkableDocuments?", warningMsg, callbackWarning);
						}
						else
						{
							dms.deleteContent(deletableContent, deletableAssociation, false);
							renderViewer();
						}
					}
					else
					{
						return;
					}
				}
			};

			FDialog.ask("Delete Content", windowNo, this, "DeleteContent?", callback,
						((MDMSContent) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT)).getName());

		}
		else if (event.getTarget().equals(mnu_undoDelete))
		{
			Callback<Boolean> callback = new Callback<Boolean>() {
				@Override
				public void onCallback(Boolean result)
				{
					if (result)
					{
						final MDMSContent deletableContent = (MDMSContent) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT);
						final MDMSAssociation deletableAssociation = (MDMSAssociation) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_ASSOCIATION);

						String warningMsg = dms.hasLinkableDocs(deletableContent, deletableAssociation);
						if (!Util.isEmpty(warningMsg, true))
						{
							Callback<Boolean> callbackWarning = new Callback<Boolean>() {

								@Override
								public void onCallback(Boolean result)
								{
									if (result)
									{
										//
										dms.undoDeleteContent(deletableContent, deletableAssociation, true);
										renderViewer();
									}
								}
							};

							// Want to un-Delete actual docs and associated its linkable documents ?
							FDialog.ask(windowNo, mnu_undoDelete, "Want to un-Delete linkable references ?",
										"Un-DeleteAssociatedLinkableDocuments?", warningMsg, callbackWarning);
						}
						else
						{
							dms.undoDeleteContent(deletableContent, deletableAssociation, false);
							renderViewer();
						}
					}
					else
					{
						return;
					}
				}
			};

			FDialog.ask("Un-Delete Content", windowNo, this, "Un-DeleteContent?", callback,
						((MDMSContent) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT)).getName());
		}
		else if (event.getTarget().equals(mnu_associate))
		{
			new WDAssociationType(	dms, copyDMSContent, (MDMSContent) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT), getTable_ID(),
									getRecord_ID(), winContent);
		}
		else if (event.getTarget().equals(mnu_permission))
		{
			MDMSContent content = (MDMSContent) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT);

			// Show permission dialog
			new WDMSPermissionPanel(dms, content);

			IPermissionManager permissionManager = dms.getPermissionManager();
			permissionManager.initContentPermission(content);

			compCellRowViewer.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISREAD, permissionManager.isRead());
			compCellRowViewer.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISWRITE, permissionManager.isWrite());
			compCellRowViewer.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISDELETE, permissionManager.isDelete());
			compCellRowViewer.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISNAVIGATION, permissionManager.isNavigation());
			compCellRowViewer.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISALLPERMISSION, permissionManager.isAllPermission());
		}

		else if (event.getTarget().equals(mnu_owner))
		{
			new WUpdateOwner(dms, dirContent);
		}

		else if (event.getTarget().equals(mnu_canvasCreateLink))
		{
			linkCopyDocument(currDMSContent, false);
		}
		else if (event.getName().equals(DMSConstant.EVENT_ON_UPLOAD_COMPLETE))
		{
			renderViewer();
		}
		else if (event.getName().equals(DMSConstant.EVENT_ON_RENAME_COMPLETE))
		{
			Tab tab = (Tab) tabBox.getSelectedTab();
			renderViewer();
			tabBox.setSelectedTab(tab);
		}
		else if (event.getName().equals(DMSConstant.EVENT_ON_SELECTION_CHANGE))
		{
			if (event.getData() instanceof Checkbox)
			{
				Checkbox targetChkbox = (Checkbox) event.getData();
				if (DMSConstant.All_SELECT.equals(targetChkbox.getId()))
				{
					allContentSelection(targetChkbox.isChecked());
				}
				else
				{
					I_DMS_Version version = (I_DMS_Version) targetChkbox.getAttribute(DMSConstant.COMP_ATTRIBUTE_DMS_VERSION_REF);
					if (targetChkbox.isChecked())
						downloadSet.add(version);
					else
						downloadSet.remove(version);
				}
			}
			else if (event.getData() instanceof Boolean)
			{
				allContentSelection((Boolean) event.getData());
			}

			// Update counting of the selected content
			updateContentSelectedCount();
		}
		else if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(BreadCrumbLink.class))
		{
			renderBreadCrumb(event);
		}
		else if (cobDocumentView != null && Events.ON_SELECT.equals(event.getName()) && event.getTarget().equals(cobDocumentView))
		{
			renderViewer();
		}

		if (!(Events.ON_CLICK.equals(event.getName()) && event.getTarget() == this))
		{
			allowUserToCreateDir();
		}

		// Event for Searching content Simple or Advance level
		if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().equals(vsearchBox.getButton()))
		{
			searchContents(false);
		}
		else if (event.getTarget().equals(btnSearch))
		{
			searchContents(true);
		}

	} // onEvent

	/**
	 * Render content based on given search criteria
	 * 
	 * @param isAdvSearch
	 */
	public void searchContents(boolean isAdvSearch)
	{
		isSearch = isAdvSearch;
		isGenericSearch = !isAdvSearch;

		breadRow.getChildren().clear();
		lblPositionInfo.setValue(null);

		setButtonsContentCreationEnabled(false);

		setNavigationButtonEnabled(false);

		renderViewer();
	} // searchContents

	private void renderBreadCrumb(Event event)
	{
		breadCrumbEvent = (BreadCrumbLink) event.getTarget();
		boolean isRoot = breadCrumbEvent.getPathId().equals("0");
		if (isTabViewer())
		{
			if (isRoot)
			{
				MDMSContent mountingContent = dms.getRootMountingContent(tableID, recordID);
				breadCrumbEvent.setPathId(String.valueOf(mountingContent.getDMS_Content_ID()));
			}

			if (breadCrumbEvent.getImageContent() != null)
			{
				setNavigationButtonEnabled(false);
			}
		}

		if (isRoot)
		{
			selectedDMSVersionStack.removeAllElements();
			setNavigationButtonEnabled(false);

			isMountingBaseStructure = false;
		}

		int DMS_Content_ID = Integer.valueOf(breadCrumbEvent.getPathId());
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
				breadCrumbLink.setStyle(DMSConstant.CSS_BREAD_CRUMB_LINK);
				breadRow.appendChild(breadCrumbLink);

				if (Integer.valueOf(breadCrumbLink.getPathId()) == currDMSContent.getDMS_Content_ID())
					break;

				breadRow.appendChild(new Space());
				breadRow.appendChild(new Label(">"));
				breadRows.appendChild(breadRow);
				gridBreadCrumb.appendChild(breadRows);
			}
		}
		renderViewer();
	} // renderBreadCrumb

	/**
	 * Render Content Viewer based on ViewerAction and also searching like
	 * simple/advance
	 */
	public void renderViewer()
	{
		// clear download set
		downloadSet.clear();

		if (recordID > 0 || isDocExplorerWindow)
		{
			HashMap<I_DMS_Version, I_DMS_Association> contentsMap = null;

			// Setting current dms content value on label
			if (isTabViewer())
			{
				String currContentValue = currDMSContent != null ? String.valueOf(currDMSContent.getName()) : null;
				lblPositionInfo.setValue(currContentValue);
			}

			String documentView = cobDocumentView.getSelectedItem().getValue();
			if (isSearch)
				contentsMap = dms.renderSearchedContent(getQueryParams(), currDMSContent, tableID, recordID, documentView);
			else if (isGenericSearch)
				contentsMap = dms.getGenericSearchedContent(vsearchBox.getTextbox().getValue(), tableID, recordID, currDMSContent, documentView);
			else
				contentsMap = dms.getDMSContentsWithAssociation(currDMSContent, dms.AD_Client_ID, documentView);

			//
			renderViewerWithContent(contentsMap);
		}

		tabBox.setSelectedIndex(0);
	} // renderViewer

	public void renderViewerWithContent(HashMap<I_DMS_Version, I_DMS_Association> contentsMap)
	{
		// Content Type wise access restriction
		IContentTypeAccess contentTypeAccess = DMSFactoryUtils.getContentTypeAccessFactory();
		HashMap<I_DMS_Version, I_DMS_Association> contentsMapCTFiltered = contentTypeAccess.getFilteredContentList(contentsMap);

		// Permission wise access restriction
		HashMap<I_DMS_Version, I_DMS_Association> mapPerFiltered;
		if (DMSPermissionUtils.isPermissionAllowed())
		{
			mapPerFiltered = dms.getPermissionManager().getFilteredVersionList(contentsMapCTFiltered);
		}
		else
		{
			mapPerFiltered = contentsMapCTFiltered;
		}

		// Component Viewer
		String[] eventsList = new String[] { Events.ON_RIGHT_CLICK, Events.ON_DOUBLE_CLICK };
		AbstractComponentIconViewer viewerComponent = (AbstractComponentIconViewer) DMSFactoryUtils.getDMSComponentViewer(currThumbViewerAction);
		viewerComponent.init(dms, mapPerFiltered, grid, DMSConstant.CONTENT_LARGE_ICON_WIDTH, DMSConstant.CONTENT_LARGE_ICON_HEIGHT, this, eventsList);

		lblCountAndSelected.setText(String.valueOf(mapPerFiltered.size()) + " items");
	}

	/**
	 * Clear the grid view components
	 */
	public void clearComponents()
	{
		isSearch = false;
		isGenericSearch = false;

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

		if (m_editors != null)
		{
			for (WEditor editor : m_editors)
				editor.setValue(null);
		}
		Components.removeAllChildren(panelAttribute);
		Components.removeAllChildren(grid);

		downloadSet.clear();
	} // clearComponents

	/**
	 * open the Directory OR Content
	 * 
	 * @param  component
	 * @throws FileNotFoundException
	 */
	private void openDirectoryORContent(Component component) throws FileNotFoundException, IOException
	{
		MDMSVersion version = (MDMSVersion) component.getAttribute(DMSConstant.COMP_ATTRIBUTE_VERSION);
		MDMSContent selectedContent = (MDMSContent) component.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT);
		MDMSAssociation selectedAssociation = (MDMSAssociation) component.getAttribute(DMSConstant.COMP_ATTRIBUTE_ASSOCIATION);

		if (DMSPermissionUtils.isPermissionAllowed())
		{
			boolean isRead = (boolean) component.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISREAD);
			boolean isNavigation = (boolean) component.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISNAVIGATION);

			if (!isRead && !isNavigation)
				throw new AdempiereException("You do not have Read or Navigation access");
		}

		selectedDMSVersionStack.push(version);
		if (selectedContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			currDMSContent = (MDMSContent) selectedDMSVersionStack.pop().getDMS_Content();
			showBreadcumb(currDMSContent);

			renderViewer();

			lblPositionInfo.setValue(currDMSContent.getName());
			btnBack.setEnabled(true);
			btnNext.setEnabled(false);

			if (currDMSContent.isMounting() && isDocExplorerWindow)
				isMountingBaseStructure = true;
		}
		else if (selectedContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Content))
		{
			MDMSMimeType mimeType = (MDMSMimeType) selectedContent.getDMS_MimeType();
			File documentToPreview = dms.getFileFromStorage(version);

			if (documentToPreview != null)
			{
				String name = selectedContent.getName();

				try
				{
					documentToPreview = DMSConvertToPDFUtils.convertDocToPDF(documentToPreview, mimeType);
				}
				catch (Exception e)
				{
					if (e.getCause() instanceof WorkbookNotFoundException)
					{
						// Do not throw error, some document having complex function used and
						// implemented libs not enough to handle that things.
					}
					else
					{
						String errorMsg = "Whoops! There was a problem previewing this document. \n Due to exception: " + e.getLocalizedMessage();
						log.log(Level.SEVERE, errorMsg, e);
						FDialog.warn(windowID, errorMsg, "Document preview issue...");
					}
				}

				if (DMSFactoryUtils.getContentEditor(mimeType.getMimeType()) != null)
				{
					boolean isContentActive = (boolean) component.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISACTIVE);

					Tab tabData = new Tab(name);
					tabData.setClass(isContentActive ? "SB-Active-Content" : "SB-InActive-Content");
					tabData.setClosable(true);
					tabs.appendChild(tabData);
					tabBox.setSelectedTab(tabData);

					WDocumentViewer documentViewer = new WDocumentViewer(dms, tabBox, documentToPreview, selectedContent, tableID, recordID, windowNo, tabNo);
					Tabpanel tabPanel = documentViewer.initForm(isWindowAccess, isMountingBaseStructure, MDMSAssociationType.isLink(selectedAssociation));
					tabPanels.appendChild(tabPanel);
					documentViewer.getAttributePanel().addEventListener(DMSConstant.EVENT_ON_UPLOAD_COMPLETE, this);
					documentViewer.getAttributePanel().addEventListener(DMSConstant.EVENT_ON_RENAME_COMPLETE, this);

					this.appendChild(tabBox);

					// Fix for search --> download content --> back (which was
					// navigate to home/root folder)
					selectedDMSVersionStack.pop();
				}
				else
				{
					DMS_ZK_Util.downloadDocument(documentToPreview, currDMSContent);
				}
			}
			else
			{
				FDialog.error(windowNo, this, "ContentNotFoundInStorage", dms.getPathFromContentManager(version), "Content Not Found In the Storage");
			}
		}
	} // openDirectoryORContent

	/**
	 * Navigate the Previous Directory.
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private void navigationBack()
	{
		List<BreadCrumbLink> parents = getParentLinks();
		int contentID = 0;
		if (!parents.isEmpty())
		{
			breadRow.getChildren().clear();

			int count = 0;
			Iterator<BreadCrumbLink> iterator = parents.iterator();
			while (iterator.hasNext())
			{
				BreadCrumbLink breadCrumbLink = (BreadCrumbLink) iterator.next();
				breadCrumbLink.setStyle(DMSConstant.CSS_BREAD_CRUMB_LINK);

				if (parents.size() > 1)
				{
					lblShowBreadCrumb = new Label(">");
					breadRow.appendChild(breadCrumbLink);
					breadRow.appendChild(new Space());
					breadRow.appendChild(lblShowBreadCrumb);

					count++;

					if (parents.size() - 1 == count)
					{
						contentID = Integer.parseInt(breadCrumbLink.getPathId());
						breadRow.removeChild(lblShowBreadCrumb);
						break;
					}
				}
				breadRows.appendChild(breadRow);
				gridBreadCrumb.appendChild(breadRows);
			}
		}

		nextDMSContent = currDMSContent;

		if (contentID == 0)
		{
			currDMSContent = null;
			breadRow.getChildren().clear();
			addRootBreadCrumb();
		}
		else
		{
			currDMSContent = new MDMSContent(Env.getCtx(), contentID, null);
		}

		if (currDMSContent == null)
		{
			lblPositionInfo.setValue(null);
			btnBack.setEnabled(false);
			isMountingBaseStructure = false;
		}
		else
		{
			lblPositionInfo.setValue(currDMSContent.getName());
			if (currDMSContent.getParentURL() == null)
				btnBack.setEnabled(true);
		}

		btnNext.setEnabled(true);

		renderViewer();

		if (isTabViewer())
		{
			// Getting initial mounting content for disabling back navigation
			MDMSContent mountingContent = dms.getRootMountingContent(tableID, recordID);
			if (currDMSContent == null)
				currDMSContent = (MDMSContent) selectedDMSVersionStack.peek().getDMS_Content();

			if (currDMSContent.getDMS_Content_ID() == mountingContent.getDMS_Content_ID())
			{
				btnBack.setDisabled(true);
				renderViewer();
			}
			return;
		}
	} // backNavigation

	/**
	 * Move in the Directory
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private void navigationNext()
	{
		BreadCrumbLink breadCrumbLink = new BreadCrumbLink();
		breadCrumbLink.setPathId(String.valueOf(nextDMSContent.getDMS_Content_ID()));
		breadCrumbLink.setLabel(nextDMSContent.getName());
		breadCrumbLink.setStyle(DMSConstant.CSS_BREAD_CRUMB_LINK);

		breadRow.appendChild(new Label(" > "));
		breadRow.appendChild(breadCrumbLink);
		breadRows.appendChild(breadRow);
		gridBreadCrumb.appendChild(breadRows);

		if (nextDMSContent != null)
		{
			currDMSContent = nextDMSContent;
			renderViewer();
			lblPositionInfo.setValue(currDMSContent.getName());
		}

		isMountingBaseStructure = isMountingBaseStructure || (currDMSContent.isMounting() && isDocExplorerWindow);

		btnNext.setEnabled(false);
		btnBack.setEnabled(true);
	} // directoryNavigation

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
	} // createDirectory

	/**
	 * Upload Content
	 */
	private void uploadContent()
	{
		uploadContent = DMSFactoryUtils.getUploadContenFactory(dms, currDMSContent, false, tableID, recordID, windowNo, tabNo);

		((Component) uploadContent).addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception
			{
				renderViewer();
			}
		});
		((Component) uploadContent).addEventListener(Events.ON_CLOSE, this);
	} // uploadContent

	/**
	 * Open MenuPopup when Right click on Directory OR Content
	 * 
	 * @param compCellRowViewer
	 */
	private void openContentContextMenu(final Component compCellRowViewer)
	{
		dirContent = (MDMSContent) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT);
		contentContextMenu.setPage(compCellRowViewer.getPage());
		copyDMSContent = DMSClipboard.get();

		if (!isWindowAccess
			|| (dirContent.isMounting() && dirContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
			|| isMountingBaseStructure)
		{
			ctxMenuItemDisabled(true);
			mnu_undoDelete.setVisible(false);

			((XulElement) compCellRowViewer).setContext(contentContextMenu);
			contentContextMenu.open(this, "at_pointer");
			return;
		}
		else
		{
			ctxMenuItemDisabled(false);
		}

		if (copyDMSContent == null)
		{
			mnu_paste.setDisabled(true);
			mnu_associate.setDisabled(true);
			mnu_createLink.setDisabled(true);
			mnu_versionList.setDisabled(true);
			mnu_canvasPaste.setDisabled(true);
			mnu_uploadVersion.setDisabled(true);
		}
		else if (copyDMSContent == dirContent)
		{
			mnu_paste.setDisabled(true);
			mnu_associate.setDisabled(true);
			mnu_createLink.setDisabled(true);
			mnu_canvasPaste.setDisabled(true);
		}
		else if (MDMSContent.CONTENTBASETYPE_Directory.equals(dirContent.getContentBaseType()))
		{
			mnu_paste.setDisabled(false);
			mnu_associate.setDisabled(true);
			mnu_versionList.setDisabled(true);
			mnu_canvasPaste.setDisabled(false);
			mnu_uploadVersion.setDisabled(true);
		}
		else
		{
			mnu_associate.setDisabled(false);
			mnu_createLink.setDisabled(false);
		}

		if (MDMSContent.CONTENTBASETYPE_Content.equals(dirContent.getContentBaseType()))
		{
			mnu_paste.setDisabled(true);
			mnu_download.setDisabled(false);
			mnu_createLink.setDisabled(true);
			mnu_versionList.setDisabled(false);
			mnu_canvasPaste.setDisabled(true);
			mnu_uploadVersion.setDisabled(false);

			if (copyDMSContent != null && copyDMSContent != dirContent)
				mnu_associate.setDisabled(false);
			else
				mnu_associate.setDisabled(true);
		}

		if (MDMSContent.CONTENTBASETYPE_Directory.equals(dirContent.getContentBaseType()))
		{
			if (copyDMSContent != null)
				mnu_createLink.setDisabled(false);

			if (DMSClipboard.get() != null && !DMSClipboard.getIsCopy())
			{
				mnu_paste.setDisabled(false);
				mnu_canvasPaste.setDisabled(false);
			}
			mnu_download.setDisabled(false);
		}

		mnu_copy.setDisabled(false);

		((XulElement) compCellRowViewer).setContext(contentContextMenu);
		contentContextMenu.open(this, "at_pointer");

		if (MDMSAssociationType.isLink(((MDMSAssociation) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_ASSOCIATION))))
		{
			if (MDMSContent.CONTENTBASETYPE_Content.equals(dirContent.getContentBaseType()))
			{
				mnu_cut.setDisabled(true);
				mnu_copy.setDisabled(true);
				mnu_paste.setDisabled(true);
				mnu_owner.setDisabled(false);
				mnu_rename.setDisabled(true);
				mnu_delete.setDisabled(false);
				mnu_download.setDisabled(false);
				mnu_associate.setDisabled(true);
				mnu_createLink.setDisabled(true);
				mnu_permission.setDisabled(false);
				mnu_versionList.setDisabled(false);
				mnu_canvasPaste.setDisabled(true);
				mnu_uploadVersion.setDisabled(false);
			}
			else if (MDMSContent.CONTENTBASETYPE_Directory.equals(dirContent.getContentBaseType()))
			{
				ctxMenuItemDisabled(true);

				mnu_download.setDisabled(false);
				mnu_canvasPaste.setDisabled(true);
			}
		}

		if (DMSPermissionUtils.isPermissionAllowed())
		{
			boolean isRead = (boolean) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISREAD);
			boolean isWrite = (boolean) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISWRITE);
			boolean isDelete = (boolean) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISDELETE);
			boolean isNavigation = (boolean) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISNAVIGATION);
			boolean isAllPermission = isRead && isWrite && isDelete && isNavigation;

			if (!isWrite || (isNavigation && !isAllPermission))
			{
				// WRITE ACCESS
				mnu_cut.setDisabled(true);
				mnu_copy.setDisabled(true);
				mnu_paste.setDisabled(true);
				mnu_owner.setDisabled(true);
				mnu_rename.setDisabled(true);
				mnu_download.setDisabled(true);
				mnu_associate.setDisabled(true);
				mnu_createLink.setDisabled(true);
				mnu_permission.setDisabled(true);
				mnu_uploadVersion.setDisabled(true);
			}

			if (!isRead)
			{
				mnu_versionList.setDisabled(true);
				mnu_zoomContentWin.setDisabled(true);
			}

			if (!isDelete)
			{
				// DELETE ACCESS
				mnu_delete.setDisabled(true);
				mnu_undoDelete.setDisabled(true);
			}

			if (isAllPermission)
			{
				mnu_versionList.setDisabled(false);
				mnu_zoomContentWin.setDisabled(false);
				mnu_delete.setDisabled(false);
				mnu_undoDelete.setDisabled(false);
			}
		}

		if (isDMSAdmin)
		{
			boolean isActive = (boolean) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISACTIVE);

			mnu_cut.setVisible(isActive);
			mnu_copy.setVisible(isActive);
			mnu_paste.setVisible(isActive);
			mnu_owner.setVisible(isActive);
			mnu_rename.setVisible(isActive);
			mnu_delete.setVisible(isActive);
			mnu_download.setVisible(isActive);
			mnu_associate.setVisible(isActive);
			mnu_createLink.setVisible(isActive);
			mnu_permission.setVisible(isActive);
			mnu_versionList.setVisible(isActive);
			mnu_uploadVersion.setVisible(isActive);
			mnu_zoomContentWin.setVisible(isActive);

			if (DMSPermissionUtils.isPermissionAllowed())
			{
				boolean isDelete = (boolean) compCellRowViewer.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISDELETE);
				mnu_undoDelete.setVisible(!isActive && isDelete);
				mnu_undoDelete.setDisabled(isActive || !isDelete);
			}
			else
			{
				mnu_undoDelete.setVisible(!isActive);
				mnu_undoDelete.setDisabled(isActive);
			}
		}
		else
		{
			mnu_undoDelete.setVisible(false);
		}

		// Permission menu item
		if (DMSPermissionUtils.isPermissionAllowed())
		{
			mnu_permission.setVisible(true);
		}
		else
		{
			mnu_permission.setVisible(false);
		}

	} // openContentContextMenu

	/**
	 * @param isDisabled
	 */
	public void ctxMenuItemDisabled(boolean isDisabled)
	{
		mnu_cut.setDisabled(isDisabled);
		mnu_copy.setDisabled(isDisabled);
		mnu_paste.setDisabled(isDisabled);
		mnu_owner.setDisabled(isDisabled);
		mnu_rename.setDisabled(isDisabled);
		mnu_delete.setDisabled(isDisabled);
		mnu_download.setDisabled(isDisabled);
		mnu_associate.setDisabled(isDisabled);
		mnu_createLink.setDisabled(isDisabled);
		mnu_permission.setDisabled(isDisabled);
		mnu_versionList.setDisabled(isDisabled);
		mnu_uploadVersion.setDisabled(isDisabled);
		mnu_zoomContentWin.setDisabled(isDisabled);
	} // ctxMenuItemDisabled

	private void openCanvasContextMenu(Event event)
	{
		Component compCellRowViewer = event.getTarget();
		dirContent = currDMSContent;
		canvasContextMenu.setPage(compCellRowViewer.getPage());
		((XulElement) compCellRowViewer).setContext(canvasContextMenu);

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
			// DMS Tab Level Configured, Allow Cut content to Paste it on Document Explorer window
			if (DMSClipboard.get() == null || (DMSClipboard.get() != null && !DMSClipboard.getIsCopy()))
				; // mnu_canvasPaste.setDisabled(true);
			else
				mnu_canvasPaste.setDisabled(false);
		}

		if (currDMSContent != null)
		{
			if (isMountingBaseStructure || !currDMSContent.isActive())
			{
				mnu_canvasCreateLink.setDisabled(true);
				mnu_canvasPaste.setDisabled(true);
			}
		}

		// Restrict creating link
		if (DMSClipboard.get() != null && !DMSClipboard.getIsCopy())
			mnu_canvasCreateLink.setDisabled(true);

		if (currDMSContent != null && !dms.isWritePermission(currDMSContent) && !dms.isAllPermissionGranted(currDMSContent))
		{
			mnu_canvasCreateLink.setDisabled(true);
			mnu_canvasPaste.setDisabled(true);
		}

		canvasContextMenu.open(this, "at_pointer");
	} // openCanvasContextMenu

	/**
	 * @param contentReferenceTo
	 * @param isDir
	 */
	private void linkCopyDocument(MDMSContent contentReferenceTo, boolean isDir)
	{
		String warnMsg = dms.createLink(contentReferenceTo, DMSClipboard.get(), isDir, tableID, recordID);
		if (!Util.isEmpty(warnMsg, true))
			FDialog.warn(0, warnMsg);

		try
		{
			renderViewer();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render content problem.", e);
			throw new AdempiereException("Render content problem: " + e);
		}
	} // linkCopyDocument

	private HashMap<String, List<Object>> getQueryParams()
	{
		HashMap<String, List<Object>> params = new LinkedHashMap<String, List<Object>>();

		if (!Util.isEmpty(txtDocumentName.getValue(), true))
			DMSSearchUtils.setSearchParams(DMSConstant.NAME, txtDocumentName.getValue().toLowerCase(), null, params);

		if (!Util.isEmpty(txtDescription.getValue(), true))
			DMSSearchUtils.setSearchParams(DMSConstant.DESCRIPTION, "*" + txtDescription.getValue().toLowerCase().trim() + "*", null, params);

		//
		if (dbCreatedFrom.getValue() != null && dbCreatedTo.getValue() != null)
		{
			if (dbCreatedFrom.getValue().after(dbCreatedTo.getValue()))
				throw new WrongValueException(dbCreatedFrom, "Invalid Date Range");
			else
				DMSSearchUtils.setSearchParams(DMSConstant.CREATED, dbCreatedFrom.getValue(), dbCreatedTo.getValue(), params);
		}
		else if (dbCreatedFrom.getValue() != null && dbCreatedTo.getValue() == null)
			DMSSearchUtils.setSearchParams(DMSConstant.CREATED, dbCreatedFrom.getValue(), "*", params);
		else if (dbCreatedTo.getValue() != null && dbCreatedFrom.getValue() == null)
			DMSSearchUtils.setSearchParams(DMSConstant.CREATED, "*", dbCreatedTo.getValue(), params);

		//
		if (dbUpdatedFrom.getValue() != null && dbUpdatedTo.getValue() != null)
		{
			if (dbUpdatedFrom.getValue().after(dbUpdatedTo.getValue()))
				throw new WrongValueException(dbUpdatedFrom, "Invalid Date Range");
			else
				DMSSearchUtils.setSearchParams(DMSConstant.UPDATED, dbUpdatedFrom.getValue(), dbUpdatedTo.getValue(), params);
		}
		else if (dbUpdatedFrom.getValue() != null && dbUpdatedTo.getValue() == null)
			DMSSearchUtils.setSearchParams(DMSConstant.UPDATED, dbUpdatedFrom.getValue(), "*", params);
		else if (dbUpdatedTo.getValue() != null && dbUpdatedFrom.getValue() == null)
			DMSSearchUtils.setSearchParams(DMSConstant.UPDATED, "*", dbUpdatedTo.getValue(), params);

		if (lstboxCreatedBy.getValue() != null)
			DMSSearchUtils.setSearchParams(DMSConstant.CREATEDBY, lstboxCreatedBy.getValue(), null, params);

		if (lstboxUpdatedBy.getValue() != null)
			DMSSearchUtils.setSearchParams(DMSConstant.UPDATEDBY, lstboxUpdatedBy.getValue(), null, params);

//		String documentView = cobDocumentView.getSelectedItem().getValue();
//		if (DMSConstant.DOCUMENT_VIEW_DELETED_ONLY_VALUE.equalsIgnoreCase(documentView))
//			DMSSearchUtils.setSearchParams(DMSConstant.SHOW_INACTIVE, true, null, params);
//		else if (DMSConstant.DOCUMENT_VIEW_NON_DELETED_VALUE.equalsIgnoreCase(documentView))
//			DMSSearchUtils.setSearchParams(DMSConstant.SHOW_INACTIVE, false, null, params);

		//
		if (lstboxContentType.getValue() != null)
		{
			DMSSearchUtils.setSearchParams(DMSConstant.CONTENTTYPE, lstboxContentType.getValue(), null, params);

			for (WEditor editor : m_editors)
			{
				String compName = null;
				int dt = editor.getGridField().getDisplayType();

				if (dt == DisplayType.Search || dt == DisplayType.Table || dt == DisplayType.List)
					compName = "ASI_" + editor.getColumnName().replaceAll("(?i)[^a-z0-9-_/]", "_");
				else
					compName = "ASI_" + editor.getLabel().getValue().replaceAll("(?i)[^a-z0-9-_/]", "_");

				compName = compName.replaceAll("/", "");

				Object from = null;
				Object to = null;

				if (dt == DisplayType.Number
					|| dt == DisplayType.Integer
					|| dt == DisplayType.Quantity
					|| dt == DisplayType.Amount
					|| dt == DisplayType.CostPrice)
				{
					NumberBox fromNumBox = (NumberBox) ASI_Value.get(compName).getComponent();
					NumberBox toNumBox = (NumberBox) ASI_Value.get(compName + "to").getComponent();

					if (fromNumBox.getValue() != null && toNumBox.getValue() != null)
					{
						if (dt == DisplayType.Number)
						{
							from = fromNumBox.getValue().doubleValue();
							to = toNumBox.getValue().doubleValue();
						}
						else
						{
							from = fromNumBox.getValue();
							to = toNumBox.getValue();
						}
					}
					else if (fromNumBox.getValue() != null && toNumBox.getValue() == null)
					{
						if (dt == DisplayType.Number)
							from = fromNumBox.getValue().doubleValue();
						else
							from = fromNumBox.getValue();
						to = "*";
					}
					else if (fromNumBox.getValue() == null && toNumBox.getValue() != null)
					{
						from = "*";
						if (dt == DisplayType.Number)
							to = toNumBox.getValue().doubleValue();
						else
							to = toNumBox.getValue();
					}
				}
				// Component:Date-Datebox, DateTime-DatetimeBox, Time-Timebox
				else if (dt == DisplayType.Date || dt == DisplayType.DateTime || dt == DisplayType.Time)
				{
					WEditor dataFrom = (WEditor) ASI_Value.get(compName);
					WEditor dataTo = (WEditor) ASI_Value.get(compName + "to");

					if (dataFrom.getValue() != null && dataTo.getValue() != null)
					{
						if (((Date) dataFrom.getValue()).after((Date) dataTo.getValue()))
						{
							Clients.scrollIntoView(dataFrom.getComponent());
							throw new WrongValueException(dataFrom.getComponent(), "Invalid Date Range");
						}
						else
						{
							from = dataFrom.getValue();
							to = dataTo.getValue();
						}
					}
					else if (dataFrom.getValue() != null && dataTo.getValue() == null)
					{
						from = dataFrom.getValue();
						to = "*";
					}
					else if (dataTo.getValue() != null && dataFrom.getValue() == null)
					{
						from = "*";
						to = dataTo.getValue();
					}
				}
				else if (dt == DisplayType.YesNo)
				{
					from = ((boolean) editor.getValue() ? "Y" : "N");
					to = null;
				}
				else if (dt == DisplayType.String || dt == DisplayType.Text)
				{
					if (!Util.isEmpty(editor.getValue().toString(), true))
					{
						from = editor.getValue().toString().toLowerCase();
						to = null;
					}
				}
				else if (dt == DisplayType.TableDir)
				{
					if (editor.getValue() != null)
						from = editor.getDisplay();
					to = null;
				}
				else if (dt == DisplayType.ChosenMultipleSelectionSearch
							|| dt == DisplayType.ChosenMultipleSelectionTable
							|| dt == DisplayType.ChosenMultipleSelectionList)
				{
					if (editor.getValue() != null)
					{
						String orClause = "";
						String editorValue = (String) editor.getValue();
						String[] values = editorValue.split(",");
						for (String value : values)
						{
							if (Util.isEmpty(orClause, true))
								orClause += "(";
							else
								orClause += " OR ";
							orClause += "*" + value + "*";
						}
						if (!Util.isEmpty(orClause, true))
							orClause += ")";
						from = orClause;
						to = Boolean.TRUE; // as Chosen multiple passing true as second argument
					}
				}
				else if (!Util.isEmpty(editor.getDisplay()))
				{
					from = editor.getValue();
					to = null;
				}

				//
				if (from != null || to != null)
					DMSSearchUtils.setSearchParams(compName, from, to, params);
			}
		}

//		if (tableID > 0)
//			DMSSearchUtils.setSearchParams(DMSConstant.AD_TABLE_ID, dms.validTableID(tableID), null, params);
//
//		if (recordID > 0)
//			DMSSearchUtils.setSearchParams(DMSConstant.RECORD_ID, dms.validRecordID(recordID), null, params);

		return params;
	} // getQueryParamas

	@Override
	public void valueChange(ValueChangeEvent event)
	{
		if (event.getSource().equals(lstboxContentType))
		{
			lblContentMeta.setVisible(true);
			Components.removeAllChildren(panelAttribute);

			Rows rows = new Rows();

			Grid gridView = GridFactory.newGridLayout();
			gridView.setHeight("100%");
			gridView.appendChild(rows);

			if (lstboxContentType.getValue() != null)
			{
				ASI_Value.clear();
				asiPanel = new WDLoadASIPanel((int) lstboxContentType.getValue(), 0, windowNo, tabNo);
				m_editors = asiPanel.m_editors;

				for (WEditor editor : m_editors)
				{
					String compName = null;
					int dt = editor.getGridField().getDisplayType();

					if (dt == DisplayType.Search || dt == DisplayType.Table)
						compName = "ASI_" + editor.getColumnName().replaceAll("(?i)[^a-z0-9-_/]", "_");
					else
						compName = "ASI_" + editor.getLabel().getValue().replaceAll("(?i)[^a-z0-9-_/]", "_");
					compName = compName.replaceAll("/", "");

					Row row = rows.newRow();
					row.setSclass("SB-Grid-field");
					row.appendCellChild(editor.getLabel());

					if (dt == DisplayType.Number
						|| dt == DisplayType.Integer
						|| dt == DisplayType.Quantity
						|| dt == DisplayType.Amount
						|| dt == DisplayType.CostPrice)
					{
						WNumberEditor numBox = new WNumberEditor(compName + "to", false, false, true, dt, "SB");
						row.appendCellChild(editor.getComponent());
						row.appendCellChild(numBox.getComponent());

						ASI_Value.put(compName, editor);
						ASI_Value.put(compName + "to", numBox);
					}
					// Component:Date-Datebox, DateTime-DatetimeBox,
					// Time-Timebox
					else if (dt == DisplayType.Date || dt == DisplayType.DateTime || dt == DisplayType.Time)
					{
						WEditor compTo = null;

						if (dt == DisplayType.Date)
						{
							compTo = new WDateEditor(compName + "to", false, false, true, "");
							((Datebox) editor.getComponent()).setStyle(DMSConstant.CSS_DATEBOX);
							((Datebox) compTo.getComponent()).setStyle(DMSConstant.CSS_DATEBOX);
							row.appendCellChild(editor.getComponent());
							row.appendCellChild(compTo.getComponent());
						}
						else if (dt == DisplayType.Time)
						{
							compTo = new WTimeEditor(compName + "to", false, false, true, "");
							row.appendCellChild(editor.getComponent());
							row.appendCellChild(compTo.getComponent());
							((Timebox) compTo.getComponent()).setFormat("h:mm:ss a");
							((Timebox) compTo.getComponent()).setWidth("100%");
						}
						else if (dt == DisplayType.DateTime)
						{
							compTo = new WDatetimeEditor(compName, false, false, true, "");
							row.appendCellChild(editor.getComponent(), 2);
							row = rows.newRow();
							row.setSclass("SB-Grid-field");
							row.appendCellChild(new Space());
							row.appendCellChild(compTo.getComponent(), 2);
							((DatetimeBox) editor.getComponent()).setWidth("100%");
						}
						ASI_Value.put(compName, editor);
						ASI_Value.put(compName + "to", compTo);
					}
					else
					{
						row.appendCellChild(editor.getComponent(), 2);
						ASI_Value.put(editor.getLabel().getValue(), editor);
					}
				}
				panelAttribute.appendChild(gridView);
			}
			else
			{
				lblContentMeta.setVisible(false);
			}
		}
	} // valueChange

	private void showBreadcumb(MDMSContent breadcumbContent)
	{
		Components.removeAllChildren(gridBreadCrumb);

		lblShowBreadCrumb = new Label(">");
		breadRow.appendChild(new Space());
		breadRow.appendChild(lblShowBreadCrumb);

		BreadCrumbLink breadCrumbLink = new BreadCrumbLink();
		breadCrumbLink.setPathId(String.valueOf(breadcumbContent.getDMS_Content_ID()));
		breadCrumbLink.addEventListener(Events.ON_CLICK, this);
		// breadCrumbLink.addEventListener(Events.ON_MOUSE_OVER, this);
		breadCrumbLink.setLabel(breadcumbContent.getName());
		breadCrumbLink.setStyle(DMSConstant.CSS_BREAD_CRUMB_LINK);

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

	// TODO Refactor method by passing table & record id
	public void addRootBreadCrumb()
	{
		BreadCrumbLink rootBreadCrumbLink = new BreadCrumbLink();

		DMS_ZK_Util.setFontOrImageAsIcon("Home", rootBreadCrumbLink);
		rootBreadCrumbLink.setPathId(String.valueOf(0));
		rootBreadCrumbLink.addEventListener(Events.ON_CLICK, this);
		rootBreadCrumbLink.setStyle(DMSConstant.CSS_BREAD_CRUMB_LINK);

		breadRow.appendChild(rootBreadCrumbLink);
		breadRow.appendChild(new Label());
		breadRows.appendChild(breadRow);
		gridBreadCrumb.appendChild(breadRows);
	} // addRootBreadCrumb

	/**
	 * Get Current Toggle Action
	 * 
	 * @return Toggle Action like Panel, List, etc
	 */
	public String getCurrToggleAction()
	{
		return currThumbViewerAction;
	}

	public Row getBreadRow()
	{
		return breadRow;
	}

	/**
	 * Set enabled Back & Next button
	 * 
	 * @param isEnabled
	 */
	public void setNavigationButtonEnabled(boolean isEnabled)
	{
		btnBack.setEnabled(isEnabled);
		btnNext.setEnabled(isEnabled);
	}

	/**
	 * Set enabled Create Dir & Upload content button
	 * 
	 * @param isEnabled
	 */
	public void setButtonsContentCreationEnabled(boolean isEnabled)
	{
		btnCreateDir.setEnabled(isEnabled);
		btnUploadContent.setEnabled(isEnabled);
	}

	private void allContentSelection(boolean isChecked)
	{
		if (grid.getChildren() != null && grid.getChildren().size() > 0)
		{
			Component rows = grid.getChildren().get(0);
			Iterator<Component> rowsIt = rows.getChildren().iterator();
			while (rowsIt.hasNext())
			{
				Component row = rowsIt.next();
				Checkbox checkBox = (Checkbox) row.getChildren().get(0).getChildren().get(0);
				checkBox.setChecked(isChecked);
				//
				I_DMS_Version version = (I_DMS_Version) checkBox.getAttribute(DMSConstant.COMP_ATTRIBUTE_DMS_VERSION_REF);
				if (isChecked)
					downloadSet.add(version);
				else
					downloadSet.remove(version);
			}
		}
	} // allContentSelection

	/**
	 * Update the label for no of content selected
	 */
	private void updateContentSelectedCount()
	{
		String value = lblCountAndSelected.getValue();
		if (!Util.isEmpty(value, true))
		{
			String updatedValue = "";
			if (value.contains("/"))
			{
				String[] splited = value.split("/");
				updatedValue = String.valueOf(downloadSet.size()) + "/" + splited[1];
			}
			else
			{
				updatedValue = String.valueOf(downloadSet.size()) + "/" + value;
			}

			lblCountAndSelected.setText(updatedValue);
		}
	} // updateContentSelectedCount

	public WTableDirEditor getContentTypeComp()
	{
		return lstboxContentType;
	}

	public ArrayList<WEditor> getAttributeEditors()
	{
		return m_editors;
	}

	public void setButtonHomeEnabled(boolean isEnabled)
	{
		btnCloseTab.setEnabled(isEnabled);
	}

	public void setButtonCleanEnabled(boolean isEnabled)
	{
		btnClear.setEnabled(isEnabled);
	}
}
