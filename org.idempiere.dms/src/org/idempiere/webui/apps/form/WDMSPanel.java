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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MClientInfo;
import org.compiere.model.MColumn;
import org.compiere.model.MImage;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_AD_User;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.componenet.DMSViewerComponent;
import org.idempiere.dms.factories.DMSClipboard;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSMimeType;
import org.idempiere.model.X_DMS_Content;
import org.idempiere.model.X_DMS_ContentType;
import org.zkoss.image.AImage;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
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

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

public class WDMSPanel extends Panel implements EventListener<Event>, ValueChangeListener
{

	private static final long				serialVersionUID			= -6813481516566180243L;
	public static CLogger					log							= CLogger.getCLogger(WDMSPanel.class);

	private static String					SQL_LATEST_VERSION			= ""
																				+ "SELECT DMS_Content_ID, DMS_Association_ID "
																				+ "FROM DMS_Association "
																				+ "WHERE DMS_Content_Related_ID = ? OR DMS_Content_ID= ? "
																				+ "GROUP BY DMS_Content_ID,DMS_Association_ID "
																				+ "ORDER BY Max(seqNo) DESC FETCH FIRST ROW ONLY";

	private static final String				spFileSeprator				= Utils.getStorageProviderFileSeparator();

	public Tabbox							tabBox						= new Tabbox();
	private Tabs							tabs						= new Tabs();
	public Tab								tabView						= new Tab(Msg.getMsg(Env.getCtx(), "Explorer"));
	public Tabpanels						tabPanels					= new Tabpanels();
	public Tabpanel							tabViewPanel				= new Tabpanel();

	private Grid							grid						= GridFactory.newGridLayout();
	private Grid							gridBreadCrumb				= GridFactory.newGridLayout();
	private Grid							searchgridView				= GridFactory.newGridLayout();

	private BreadCrumbLink					breadCrumbEvent				= null;

	private Rows							breadRows					= new Rows();
	public Row								breadRow					= new Row();

	// View Result Tab
	private Searchbox						vsearchBox					= new Searchbox();
	private Label							lblAdvanceSearch			= new Label(Msg.translate(Env.getCtx(),
																				"Advance Search"));
	private Label							lblDocumentName				= new Label(Msg.translate(Env.getCtx(), "Name"));
	private Label							lblContentType				= new Label(Msg.translate(Env.getCtx(),
																				"Content Type"));
	private Label							lblCreated					= new Label(Msg.translate(Env.getCtx(),
																				"Created"));
	private Label							lblUpdated					= new Label(Msg.translate(Env.getCtx(),
																				"Updated"));
	private Label							lblContentMeta				= new Label(Msg.translate(Env.getCtx(),
																				"Content Meta"));

	private Label							lblDescription				= new Label(Msg.translate(Env.getCtx(),
																				"Description"));

	private Label							lblCreatedBy				= new Label(Msg.translate(Env.getCtx(),
																				"CreatedBy"));

	private Label							lblUpdatedBy				= new Label(Msg.translate(Env.getCtx(),
																				"UpdatedBy"));

	private Label							lblShowBreadCrumb			= null;

	private Datebox							dbCreatedTo					= new Datebox();
	private Datebox							dbCreatedFrom				= new Datebox();
	private Datebox							dbUpdatedTo					= new Datebox();
	private Datebox							dbUpdatedFrom				= new Datebox();

	private ConfirmPanel					confirmPanel				= new ConfirmPanel();

	private Button							btnClear					= confirmPanel
																				.createButton(ConfirmPanel.A_RESET);
	private Button							btnRefresh					= confirmPanel
																				.createButton(ConfirmPanel.A_REFRESH);
	private Button							btnCloseTab					= confirmPanel
																				.createButton(ConfirmPanel.A_CANCEL);

	private Button							btnSearch					= new Button();

	private Textbox							txtDocumentName				= new Textbox();
	private Textbox							txtDescription				= new Textbox();

	private WTableDirEditor					lstboxContentType			= null;
	private WTableDirEditor					lstboxCreatedBy				= null;
	private WTableDirEditor					lstboxUpdatedBy				= null;
	private Checkbox						chkInActive					= new Checkbox();

	// create Directory
	private Button							btnCreateDir				= new Button();
	private Button							btnUploadContent			= new Button();
	private Button							btnBack						= new Button();
	private Button							btnNext						= new Button();

	private Label							lblPositionInfo				= new Label();

	private MDMSContent						currDMSContent				= null;
	private MDMSContent						nextDMSContent				= null;

	private MDMSAssociation					previousDMSAssociation		= null;

	private Stack<MDMSAssociation>			selectedDMSAssociation		= new Stack<MDMSAssociation>();
	private Stack<MDMSContent>				selectedDMSContent			= new Stack<MDMSContent>();

	private ArrayList<DMSViewerComponent>	viewerComponents			= null;

	public IFileStorageProvider				fileStorageProvider			= null;
	public IThumbnailProvider				thumbnailProvider			= null;
	public IContentManager					contentManager				= null;
	public IFileStorageProvider				thumbnailStorageProvider	= null;
	private IIndexSearcher					indexSeracher				= null;
	private IMountingStrategy				mountingStrategy			= null;

	private Menupopup						contentContextMenu			= new Menupopup();
	private Menupopup						canvasContextMenu			= new Menupopup();

	private Menuitem						mnu_versionList				= null;
	private Menuitem						mnu_copy					= null;
	private Menuitem						mnu_createLink				= null;
	private Menuitem						mnu_delete					= null;
	private Menuitem						mnu_associate				= null;
	private Menuitem						mnu_uploadVersion			= null;
	private Menuitem						mnu_rename					= null;
	private Menuitem						mnu_cut						= null;
	private Menuitem						mnu_paste					= null;

	private Menuitem						mnu_canvasCreateLink		= null;
	private Menuitem						mnu_canvasPaste				= null;

	private DMSViewerComponent				DMSViewerComp				= null;
	private MDMSContent						copyDMSContent				= null;
	private MDMSContent						dirContent					= null;

	private WUploadContent					uploadContent				= null;
	private CreateDirectoryForm				createDirectoryForm			= null;

	private Panel							panelAttribute				= new Panel();
	private WDLoadASIPanel					asiPanel					= null;

	public int								recordID					= 0;
	public int								tableID						= 0;

	private boolean							isSearch					= false;
	private boolean							isGenericSearch				= false;
	private boolean 						isAllowCreateDirectory		= true;

	private static final int				COMPONENT_HEIGHT			= 120;
	private static final int				COMPONENT_WIDTH				= 120;

	private static final String				MENUITEM_UPLOADVERSION		= "Upload Version";
	private static final String				MENUITEM_VERSIONlIST		= "Version List";
	private static final String				MENUITEM_RENAME				= "Rename";
	private static final String				MENUITEM_CUT				= "Cut";
	private static final String				MENUITEM_COPY				= "Copy";
	private static final String				MENUITEM_PASTE				= "Paste";
	private static final String				MENUITEM_CREATELINK			= "Create Link";
	private static final String				MENUITEM_DELETE				= "Delete";
	private static final String				MENUITEM_ASSOCIATE			= "Associate";

	private DMSViewerComponent				prevComponent				= null;

	private ArrayList<WEditor>				m_editors					= new ArrayList<WEditor>();

	private Map<String, Component>			ASI_Value					= new HashMap<String, Component>();
	private DateFormat						dateFormat					= new SimpleDateFormat(
																				"yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	private AbstractADWindowContent			winContent;

	/**
	 * Constructor initialize
	 */
	public WDMSPanel()
	{
		fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);

		if (fileStorageProvider == null)
			throw new AdempiereException("Storage provider is not found.");

		thumbnailStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), true);

		if (thumbnailStorageProvider == null)
			throw new AdempiereException("Thumbnail Storage provider is not found.");

		thumbnailProvider = Utils.getThumbnailProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thumbnailProvider == null)
			throw new AdempiereException("Thumbnail provider is not found.");

		contentManager = Utils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		indexSeracher = ServiceUtils.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));

		if (indexSeracher == null)
			throw new AdempiereException("Index server is not found.");

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
		setTable_ID(Table_ID);
		setRecord_ID(Record_ID);
		mountingStrategy = Utils.getMountingStrategy(null);
		currDMSContent = mountingStrategy.getMountingParent(MTable.getTableName(Env.getCtx(), Table_ID), Record_ID);
		
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
		if (tableID > 0 && recordID > 0)
			return true;
		else
			return false;
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

		int Column_ID = MColumn.getColumn_ID(X_AD_User.Table_Name, X_AD_User.COLUMNNAME_AD_User_ID);
		MLookup lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, Column_ID, DisplayType.TableDir,
					Env.getLanguage(Env.getCtx()), X_AD_User.COLUMNNAME_AD_User_ID, 0, true, "");
			lstboxCreatedBy = new WTableDirEditor(X_AD_User.COLUMNNAME_AD_User_ID, false, false, true, lookup);
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

		Column_ID = MColumn.getColumn_ID(X_AD_User.Table_Name, X_AD_User.COLUMNNAME_AD_User_ID);
		lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, Column_ID, DisplayType.TableDir,
					Env.getLanguage(Env.getCtx()), X_AD_User.COLUMNNAME_AD_User_ID, 0, true, "");
			lstboxUpdatedBy = new WTableDirEditor(X_AD_User.COLUMNNAME_AD_User_ID, false, false, true, lookup);
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

		Column_ID = MColumn.getColumn_ID(X_DMS_ContentType.Table_Name, X_DMS_ContentType.COLUMNNAME_DMS_ContentType_ID);
		lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, Column_ID, DisplayType.TableDir,
					Env.getLanguage(Env.getCtx()), X_DMS_ContentType.COLUMNNAME_DMS_ContentType_ID, 0, true, "");
			lstboxContentType = new WTableDirEditor(X_DMS_ContentType.COLUMNNAME_DMS_ContentType_ID, false, false,
					true, lookup);
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
//		row.setAlign("center");
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
				.setStyle("width: 100%; position:relative; overflow: auto; background-color: rgba(0,0,0,.2); background-clip: padding-box; color: #222; background: transparent;font-family: Roboto,sans-serif; border: solid transparent; border-width: 1px 1px 1px 6px;min-height: 28px; padding: 100px 0 0;box-shadow: inset 1px 1px 0 rgba(0,0,0,.1),inset 0 -1px 0 rgba(0,0,0,.07);");

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

		mnu_versionList = new Menuitem(MENUITEM_VERSIONlIST);
		mnu_cut = new Menuitem(MENUITEM_CUT);
		mnu_copy = new Menuitem(MENUITEM_COPY);
		mnu_paste = new Menuitem(MENUITEM_PASTE);
		mnu_createLink = new Menuitem(MENUITEM_CREATELINK);
		mnu_delete = new Menuitem(MENUITEM_DELETE);
		mnu_associate = new Menuitem(MENUITEM_ASSOCIATE);
		mnu_uploadVersion = new Menuitem(MENUITEM_UPLOADVERSION);
		mnu_rename = new Menuitem(MENUITEM_RENAME);

		mnu_canvasCreateLink = new Menuitem(MENUITEM_CREATELINK);
		mnu_canvasPaste = new Menuitem(MENUITEM_PASTE);

		canvasContextMenu.appendChild(mnu_canvasPaste);
		canvasContextMenu.appendChild(mnu_canvasCreateLink);
		mnu_canvasCreateLink.addEventListener(Events.ON_CLICK, this);
		mnu_canvasPaste.addEventListener(Events.ON_CLICK, this);

		contentContextMenu.appendChild(mnu_uploadVersion);
		contentContextMenu.appendChild(mnu_versionList);
		contentContextMenu.appendChild(mnu_cut);
		contentContextMenu.appendChild(mnu_copy);
		contentContextMenu.appendChild(mnu_paste);
		contentContextMenu.appendChild(mnu_createLink);
		contentContextMenu.appendChild(mnu_rename);
		contentContextMenu.appendChild(mnu_delete);
		contentContextMenu.appendChild(mnu_associate);

		mnu_canvasCreateLink.setImageContent(Utils.getImage("Link24.png"));
		mnu_canvasPaste.setImageContent(Utils.getImage("Paste24.png"));
		mnu_createLink.setImageContent(Utils.getImage("Link24.png"));
		mnu_uploadVersion.setImageContent(Utils.getImage("uploadversion24.png"));
		mnu_paste.setImageContent(Utils.getImage("Paste24.png"));
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
		mnu_rename.addEventListener(Events.ON_CLICK, this);
		mnu_createLink.addEventListener(Events.ON_CLICK, this);
		mnu_copy.addEventListener(Events.ON_CLICK, this);
		mnu_delete.addEventListener(Events.ON_CLICK, this);
		mnu_associate.addEventListener(Events.ON_CLICK, this);

		mnu_delete.setDisabled(true);

		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		addRootBreadCrumb();
		SessionManager.getAppDesktop();
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		log.info(event.getName());

		if (Events.ON_DOUBLE_CLICK.equals(event.getName())
				&& event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			openDirectoryORContent(DMSViewerComp);
		}
		else if (event.getTarget().equals(btnCreateDir))
		{
			createDirectory();
		}
		else if (event.getTarget().equals(btnUploadContent))
		{
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
			isGenericSearch = true; // For solve navigation issue After search and reset button pressed.
			isSearch = false;
			clearComponenets();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			breadRow.getChildren().clear();
			if (isTabViewer())
			{
				isSearch = false;
				addRootBreadCrumb();
				int DMS_Content_ID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE name = ?",
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
			String query = indexSeracher.buildSolrSearchQuery(params);

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
		else if (Events.ON_CLICK.equals(event.getName())
				&& event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			currentCompSelection(DMSViewerComp);
		}
		else if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(WDMSPanel.class))
		{
			if (prevComponent != null)
			{
				ZkCssHelper.appendStyle(prevComponent.getfLabel(),
						"background-color:#ffffff; box-shadow: 7px 7px 7px #ffffff");
			}
		}
		else if (Events.ON_RIGHT_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(Cell.class))
		{
			openCanvasContextMenu(event);
		}

		else if (Events.ON_RIGHT_CLICK.equals(event.getName())
				&& event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComp = (DMSViewerComponent) event.getTarget();
			openContentContextMenu(DMSViewerComp);
		}
		else if (event.getTarget().equals(mnu_versionList))
		{
			new WDMSVersion(DMSViewerComp.getDMSContent());
		}
		else if (event.getTarget().equals(mnu_uploadVersion))
		{
			final WUploadContent uploadContent = new WUploadContent(dirContent, true, this.getTable_ID(),
					this.getRecord_ID());
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
				if (DMSClipboard.getIsCopy())
				{
					MDMSContent copiedContent = DMSClipboard.get();
					MDMSContent destPasteContent = dirContent;
					pasteCopyContent(copiedContent, destPasteContent);
					renderViewer();
				}
				else
				{
					MDMSContent cutDMSContent = DMSClipboard.get();
					MDMSContent destPasteContent = dirContent;
					pasteCutContent(cutDMSContent, destPasteContent);
					renderViewer();
				}
			}
		}
		else if (event.getTarget().equals(mnu_rename))
		{
			final WRenameContent renameContent = new WRenameContent(dirContent);
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

		}
		else if (event.getTarget().equals(mnu_associate))
		{
			new WDAssociationType(copyDMSContent, DMSViewerComp.getDMSContent(), getTable_ID(), getRecord_ID(), winContent);
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
				|| (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass()
						.equals(vsearchBox.getButton().getClass())))
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

	private String pastePhysicalCopiedFolder(MDMSContent copiedContent, MDMSContent destPasteContent)
	{
		File dirPath = new File(fileStorageProvider.getBaseDirectory(contentManager.getPath(copiedContent)));
		String newFileName = fileStorageProvider.getBaseDirectory(contentManager.getPath(destPasteContent));

		File files[] = new File(newFileName).listFiles();

		if (newFileName.charAt(newFileName.length() - 1) == spFileSeprator.charAt(0))
			newFileName = newFileName + copiedContent.getName();
		else
			newFileName = newFileName + spFileSeprator + copiedContent.getName();

		File newFile = new File(newFileName);

		for (int i = 0; i < files.length; i++)
		{
			if (newFile.getName().equalsIgnoreCase(files[i].getName()))
			{
				if (!newFileName.contains(" - copy "))
					newFileName = newFileName + " - copy ";

				newFile = new File(newFileName);

				if (newFile.exists())
				{
					newFileName = Utils.getUniqueFoldername(newFile.getAbsolutePath());
					newFile = new File(newFileName);
				}
			}
		}

		try
		{
			FileUtils.copyDirectory(dirPath, newFile, DirectoryFileFilter.DIRECTORY);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Copy Content Failure.", e);
			throw new AdempiereException("Copy Content Failure." + e.getLocalizedMessage());
		}

		return newFile.getName();
	}

	private String pastePhysicalCopiedContent(MDMSContent copiedContent, MDMSContent destPasteContent, String parentName)
	{
		File oldFile = new File(fileStorageProvider.getBaseDirectory(contentManager.getPath(copiedContent)));
		String newFileName = fileStorageProvider.getBaseDirectory(contentManager.getPath(destPasteContent));

		if (newFileName.charAt(newFileName.length() - 1) == spFileSeprator.charAt(0))
			newFileName = newFileName + copiedContent.getName();
		else
			newFileName = newFileName + spFileSeprator + copiedContent.getName();

		File newFile = new File(newFileName);
		File parent = new File(newFile.getParent());

		File files[] = parent.listFiles();

		for (int i = 0; i < files.length; i++)
		{
			if (newFile.getName().equals(files[i].getName()))
			{
				String uniqueName = newFile.getName();

				if (!newFile.getName().contains(" - copy "))
				{
					uniqueName = FilenameUtils.getBaseName(newFile.getName()) + " - copy ";
					String ext = FilenameUtils.getExtension(newFile.getName());
					newFile = new File(parent.getAbsolutePath() + spFileSeprator + uniqueName + "." + ext);
				}
				else
				{
					newFile = new File(parent.getAbsolutePath() + spFileSeprator + parentName);
				}

				if (newFile.exists())
				{
					uniqueName = Utils.getCopiedUniqueFilename(newFile.getAbsolutePath());
					newFile = new File(uniqueName);
				}
			}
		}

		try
		{
			FileUtils.copyFile(oldFile, newFile);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Copy Content Failure.", e);
			throw new AdempiereException("Copy Content Failure." + e.getLocalizedMessage());
		}

		return newFile.getName();
	} // pastePhysicalCopiedContent

	private void pasteCopyContent(MDMSContent copiedContent, MDMSContent destPasteContent) throws IOException,
			SQLException
	{
		if (copiedContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
		{
			int DMS_Association_ID = DB
					.getSQLValue(
							null,
							"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ? Order by created FETCH FIRST ROW ONLY",
							copiedContent.getDMS_Content_ID());

			String baseURL = null;
			String renamedURL = null;
			String contentname = null;

			if (!Util.isEmpty(copiedContent.getParentURL()))
				baseURL = contentManager.getPath(copiedContent);
			else
				baseURL = spFileSeprator + copiedContent.getName();

			if (copiedContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
			{
				contentname = pastePhysicalCopiedFolder(copiedContent, destPasteContent);
			}
			renamedURL = contentManager.getPath(destPasteContent) + spFileSeprator + copiedContent.getName();

			MDMSContent oldDMSContent = new MDMSContent(Env.getCtx(), copiedContent.getDMS_Content_ID(), null);

			MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, null);

			PO.copyValues(oldDMSContent, newDMSContent);

			MAttributeSetInstance oldASI = null;
			MAttributeSetInstance newASI = null;
			if (oldDMSContent.getM_AttributeSetInstance_ID() > 0)
			{
				oldASI = new MAttributeSetInstance(Env.getCtx(), oldDMSContent.getM_AttributeSetInstance_ID(), null);
				newASI = new MAttributeSetInstance(Env.getCtx(), 0, null);
				PO.copyValues(oldASI, newASI);
				newASI.saveEx();
				
				List<MAttributeInstance> oldAI = new Query(Env.getCtx(), MAttributeInstance.Table_Name,
						"M_AttributeSetInstance_ID = ?", null).setParameters(
						oldASI.getM_AttributeSetInstance_ID()).list();

				for (MAttributeInstance AI : oldAI)
				{
					MAttributeInstance newAI = new MAttributeInstance(Env.getCtx(), 0, null);
					PO.copyValues(AI, newAI);
					newAI.setM_Attribute_ID(AI.getM_Attribute_ID());
					newAI.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
					newAI.saveEx();
				}
			}
			if (newASI != null)
				newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

			newDMSContent.setParentURL(contentManager.getPath(destPasteContent));
			newDMSContent.setName(contentname);
			newDMSContent.saveEx();

			MDMSAssociation oldDMSAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);
			MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, null);

			PO.copyValues(oldDMSAssociation, newDMSAssociation);

			newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
			if (destPasteContent != null && destPasteContent.getDMS_Content_ID() > 0)
			{
				newDMSAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());
			}
			else
			{
				newDMSAssociation.setDMS_Content_Related_ID(0);
			}

			if (isTabViewer())
			{
				newDMSAssociation.setAD_Table_ID(tableID);
				newDMSAssociation.setRecord_ID(recordID);
			}
			newDMSAssociation.saveEx();

			copyContent(copiedContent, baseURL, renamedURL, newDMSContent);
		}
		else
		{
			pasteCopyFileContent(copiedContent, destPasteContent);
		}
	}

	private void copyContent(MDMSContent copiedContent, String baseURL, String renamedURL, MDMSContent destPasteContent)
			throws IOException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try
		{
			pstmt = DB.prepareStatement(Utils.SQL_GET_RELATED_FOLDER_CONTENT_ALL, null);

			pstmt.setInt(1, copiedContent.getDMS_Content_ID());
			pstmt.setInt(2, copiedContent.getDMS_Content_ID());

			rs = pstmt.executeQuery();

			while (rs.next())
			{
				MDMSContent oldDMSContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
				if (oldDMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
				{
					MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, null);

					PO.copyValues(oldDMSContent, newDMSContent);

					MAttributeSetInstance oldASI = null;
					MAttributeSetInstance newASI = null;
					if (oldDMSContent.getM_AttributeSetInstance_ID() > 0)
					{
						oldASI = new MAttributeSetInstance(Env.getCtx(), oldDMSContent.getM_AttributeSetInstance_ID(),
								null);
						newASI = new MAttributeSetInstance(Env.getCtx(), 0, null);
						PO.copyValues(oldASI, newASI);
						newASI.saveEx();

						List<MAttributeInstance> oldAI = new Query(Env.getCtx(), MAttributeInstance.Table_Name,
								"M_AttributeSetInstance_ID = ?", null).setParameters(
								oldASI.getM_AttributeSetInstance_ID()).list();

						for (MAttributeInstance AI : oldAI)
						{
							MAttributeInstance newAI = new MAttributeInstance(Env.getCtx(), 0, null);
							PO.copyValues(AI, newAI);
							newAI.setM_Attribute_ID(AI.getM_Attribute_ID());
							newAI.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
							newAI.saveEx();
						}
					}
					if (newASI != null)
						newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

					newDMSContent.saveEx();

					MDMSAssociation oldDMSAssociation = new MDMSAssociation(Env.getCtx(),
							rs.getInt("DMS_Association_ID"), null);
					MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, null);

					PO.copyValues(oldDMSAssociation, newDMSAssociation);

					newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
					newDMSAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());

					if (isTabViewer())
					{
						newDMSAssociation.setAD_Table_ID(tableID);
						newDMSAssociation.setRecord_ID(recordID);
					}
					newDMSAssociation.saveEx();

					if (oldDMSContent.getParentURL().startsWith(baseURL))
					{
						newDMSContent.setParentURL(oldDMSContent.getParentURL().replaceFirst(baseURL, renamedURL));
						newDMSContent.saveEx();
					}
					copyContent(oldDMSContent, baseURL, renamedURL, newDMSContent);
				}
				else
				{
					pasteCopyFileContent(oldDMSContent, destPasteContent);
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Content renaming failure: ", e);
			throw new AdempiereException("Content renaming failure: " + e.getLocalizedMessage());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

	}

	private void pasteCopyFileContent(MDMSContent oldDMSContent, MDMSContent destPasteContent) throws SQLException,
			IOException
	{
		int crID = 0;
		String fileName = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		int DMS_Content_ID = Utils.getDMS_Content_Related_ID(new MDMSContent(Env.getCtx(), oldDMSContent
				.getDMS_Content_ID(), null));

		String sqlGetAssociation = "SELECT DMS_Association_ID,DMS_Content_ID FROM DMS_Association "
				+ " WHERE DMS_Content_Related_ID=? AND DMS_AssociationType_ID=1000000 OR DMS_Content_ID=? "
				+ " Order By DMS_Association_ID";
		try
		{
			pstmt = DB.prepareStatement(sqlGetAssociation.toString(), null);
			pstmt.setInt(1, DMS_Content_ID);
			pstmt.setInt(2, DMS_Content_ID);
			rs = pstmt.executeQuery();

			while (rs.next())
			{
				String baseURL = null;
				String renamedURL = null;

				oldDMSContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);

				if (!Util.isEmpty(oldDMSContent.getParentURL()))
					baseURL = contentManager.getPath(oldDMSContent);
				else
					baseURL = spFileSeprator + oldDMSContent.getName();

				baseURL = baseURL.substring(0, baseURL.lastIndexOf(spFileSeprator));

				fileName = pastePhysicalCopiedContent(oldDMSContent, destPasteContent, fileName);
				renamedURL = contentManager.getPath(destPasteContent);

				MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, null);

				PO.copyValues(oldDMSContent, newDMSContent);

				MAttributeSetInstance oldASI = null;
				MAttributeSetInstance newASI = null;
				if (oldDMSContent.getM_AttributeSetInstance_ID() > 0)
				{
					oldASI = new MAttributeSetInstance(Env.getCtx(), oldDMSContent.getM_AttributeSetInstance_ID(), null);
					newASI = new MAttributeSetInstance(Env.getCtx(), 0, null);
					PO.copyValues(oldASI, newASI);
					newASI.saveEx();

					List<MAttributeInstance> oldAI = new Query(Env.getCtx(), MAttributeInstance.Table_Name,
							"M_AttributeSetInstance_ID = ?", null).setParameters(oldASI.getM_AttributeSetInstance_ID())
							.list();

					for (MAttributeInstance AI : oldAI)
					{
						MAttributeInstance newAI = new MAttributeInstance(Env.getCtx(), 0, null);
						PO.copyValues(AI, newAI);
						newAI.setM_Attribute_ID(AI.getM_Attribute_ID());
						newAI.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
						newAI.saveEx();
					}
				}
				if (newASI != null)
					newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

				newDMSContent.setName(fileName);
				newDMSContent.saveEx();

				MDMSAssociation oldDMSAssociation = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"),
						null);
				MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, null);

				PO.copyValues(oldDMSAssociation, newDMSAssociation);

				newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());

				if (oldDMSAssociation.getDMS_AssociationType_ID() == 1000001)
				{
					crID = newDMSContent.getDMS_Content_ID();

					if (destPasteContent != null && destPasteContent.getDMS_Content_ID() > 0)
						newDMSAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());
					else
						newDMSAssociation.setDMS_Content_Related_ID(0);
				}
				else
					newDMSAssociation.setDMS_Content_Related_ID(crID);

				if (isTabViewer())
				{
					newDMSAssociation.setAD_Table_ID(tableID);
					newDMSAssociation.setRecord_ID(recordID);
				}

				newDMSAssociation.saveEx();

				if (!Util.isEmpty(oldDMSContent.getParentURL()))
				{
					if (oldDMSContent.getParentURL().startsWith(baseURL))
					{
						newDMSContent.setParentURL(renamedURL);
					}
				}
				else
				{
					newDMSContent.setParentURL(renamedURL);
				}

				newDMSContent.saveEx();
				MDMSMimeType mimeType = new MDMSMimeType(Env.getCtx(), newDMSContent.getDMS_MimeType_ID(), null);

				IThumbnailGenerator thumbnailGenerator = Utils.getThumbnailGenerator(mimeType.getMimeType());

				if (thumbnailGenerator != null)
					thumbnailGenerator.addThumbnail(newDMSContent,
							fileStorageProvider.getFile(contentManager.getPath(oldDMSContent)), null);

				String newPath = fileStorageProvider.getBaseDirectory(contentManager.getPath(destPasteContent));
				newPath = newPath + spFileSeprator + oldDMSContent.getName();

				Map<String, Object> solrValue = Utils.createIndexMap(newDMSContent, newDMSAssociation);
				indexSeracher.deleteIndex(newDMSContent.getDMS_Content_ID());
				indexSeracher.indexContent(solrValue);
			}
		}
		catch (Exception e)
		{
			throw new AdempiereException(e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}

	private void pasteCutContent(MDMSContent sourceCutContent, MDMSContent destPasteContent)
	{
		if (sourceCutContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
		{
			int DMS_Association_ID = DB
					.getSQLValue(
							null,
							"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ? Order by created FETCH FIRST ROW ONLY",
							sourceCutContent.getDMS_Content_ID());

			String baseURL = null;
			String renamedURL = null;

			if (!Util.isEmpty(sourceCutContent.getParentURL()))
				baseURL = contentManager.getPath(sourceCutContent);
			else
				baseURL = spFileSeprator + sourceCutContent.getName();

			File dirPath = new File(fileStorageProvider.getBaseDirectory(contentManager.getPath(sourceCutContent)));
			String newFileName = fileStorageProvider.getBaseDirectory(contentManager.getPath(destPasteContent));

			File files[] = new File(newFileName).listFiles();

			if (newFileName.charAt(newFileName.length() - 1) == spFileSeprator.charAt(0))
				newFileName = newFileName + sourceCutContent.getName();
			else
				newFileName = newFileName + spFileSeprator + sourceCutContent.getName();

			File newFile = new File(newFileName);

			for (int i = 0; i < files.length; i++)
			{
				if (newFile.getName().equalsIgnoreCase(files[i].getName()))
				{
					throw new AdempiereException("Directory already exists.");
				}
			}

			renamedURL = contentManager.getPath(destPasteContent) + spFileSeprator + sourceCutContent.getName();

			Utils.renameFolder(sourceCutContent, baseURL, renamedURL);
			dirPath.renameTo(newFile);

			MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);
			if (destPasteContent != null)
			{
				dmsAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());
				sourceCutContent.setParentURL(contentManager.getPath(destPasteContent));
			}
			else
			{
				dmsAssociation.setDMS_Content_Related_ID(0);
				sourceCutContent.setParentURL(null);
			}

			sourceCutContent.saveEx();
			dmsAssociation.saveEx();
		}
		else
		{
			int DMS_Content_ID = Utils.getDMS_Content_Related_ID(sourceCutContent);

			PreparedStatement pstmt = null;
			ResultSet rs = null;

			try
			{
				pstmt = DB
						.prepareStatement(
								"SELECT DMS_Association_ID,DMS_Content_ID FROM DMS_Association WHERE DMS_Content_Related_ID = ? AND DMS_AssociationType_ID = 1000000 OR DMS_Content_ID = ? Order By DMS_Association_ID",
								null);

				pstmt.setInt(1, DMS_Content_ID);
				pstmt.setInt(2, DMS_Content_ID);

				rs = pstmt.executeQuery();

				while (rs.next())
				{
					MDMSContent dmsContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
					MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"),
							null);

					moveFile(dmsContent, destPasteContent);
					if (dmsAssociation.getDMS_AssociationType_ID() == 1000001)
					{
						if (destPasteContent != null && destPasteContent.getDMS_Content_ID() == 0)
						{
							destPasteContent = null;
						}

						if (destPasteContent == null || destPasteContent.getDMS_Content_ID() == 0)
						{
							dmsAssociation.setDMS_Content_Related_ID(0);
							dmsAssociation.saveEx();
						}
						else
						{
							dmsAssociation.setDMS_Content_Related_ID(destPasteContent.getDMS_Content_ID());
							dmsAssociation.saveEx();
						}
					}

					dmsContent.setParentURL(contentManager.getPath(destPasteContent));
					dmsContent.saveEx();

					try
					{
						Map<String, Object> solrValue = Utils.createIndexMap(dmsContent, dmsAssociation);
						indexSeracher.deleteIndex(dmsContent.getDMS_Content_ID());
						indexSeracher.indexContent(solrValue);
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, "RE-Indexing of Content Failure :", e);
						throw new AdempiereException("RE-Indexing of Content Failure :" + e);
					}

				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, "Content move failure.", e);
				throw new AdempiereException("Content move failure." + e.getLocalizedMessage());
			}
		}
	}

	private void moveFile(MDMSContent dmsContent, MDMSContent destContent)
	{
		String newPath = fileStorageProvider.getBaseDirectory(contentManager.getPath(destContent));
		newPath = newPath + spFileSeprator + dmsContent.getName();

		File oldFile = new File(fileStorageProvider.getFile(contentManager.getPath(dmsContent)).getAbsolutePath());
		File newFile = new File(newPath);

		if (!newFile.exists())
		{
			oldFile.renameTo(newFile);
		}
	}

	private void renderBreadCrumb(Event event) throws IOException, URISyntaxException
	{
		breadCrumbEvent = (BreadCrumbLink) event.getTarget();

		if (isTabViewer())
		{
			if (breadCrumbEvent.getPathId().equals("0"))
			{
				int DMS_Content_ID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE name = ?",
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

		int DMS_Content_ID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE DMS_Content_ID = ? ",
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

		HashMap<I_DMS_Content, I_DMS_Association> dmsContent = null;

		// Setting current dms content value on label
		if (isTabViewer())
		{
			String currContentValue = currDMSContent != null ? String.valueOf(currDMSContent.getName()) : null;  
			lblPositionInfo.setValue(currContentValue);
		}

		if (isSearch)
			dmsContent = renderSearchedContent();
		else if (isGenericSearch)
			dmsContent = getGenericSearchedContent();
		else
			dmsContent = getDMSContents();

		Components.removeAllChildren(grid);
		for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : dmsContent.entrySet())
		{

			thumbFile = thumbnailProvider.getFile(entry.getKey(), "150");

			if (thumbFile == null)
			{
				if (entry.getKey().getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
				{
					mImage = Utils.getDirThumbnail();
				}
				else
				{
					mImage = Utils.getMimetypeThumbnail(entry.getKey().getDMS_MimeType_ID());
				}
				imgByteData = mImage.getData();
				if (imgByteData != null)
					image = new AImage(entry.getKey().getName(), imgByteData);
			}
			else
			{
				image = new AImage(thumbFile);
			}

			DMSViewerComponent viewerComponent = null;
			if (entry.getValue().getDMS_AssociationType_ID() == Utils.getDMS_Association_Link_ID())
			{
				viewerComponent = new DMSViewerComponent(entry.getKey(), image, true, entry.getValue());
			}
			else
			{
				viewerComponent = new DMSViewerComponent(entry.getKey(), image, false, entry.getValue());
			}

			viewerComponent.addEventListener(Events.ON_DOUBLE_CLICK, this);
			viewerComponent.addEventListener(Events.ON_CLICK, this);
			viewerComponent.addEventListener(Events.ON_RIGHT_CLICK, this);

			viewerComponents.add(viewerComponent);

			viewerComponent.setDheight(COMPONENT_HEIGHT);
			viewerComponent.setDwidth(COMPONENT_WIDTH);

			row.appendChild(viewerComponent);
		}
		row.setZclass("none");
		// row.setWidth(row.getWidth());
		row.setStyle("display:flex; flex-direction: row; flex-wrap: wrap; height: 100%; overflow: hidden;");
		rows.appendChild(row);

		grid.appendChild(rows);
		tabBox.setSelectedIndex(0);
	}

	private HashMap<I_DMS_Content, I_DMS_Association> getGenericSearchedContent()
	{
		StringBuffer query = new StringBuffer();
		if (!Util.isEmpty(vsearchBox.getTextbox().getValue(), true))
		{
			String inputParam = vsearchBox.getTextbox().getValue();
			query.append("(").append(Utils.NAME).append(":*").append(inputParam).append("*").append(" OR ")
					.append(Utils.DESCRIPTION).append(":*").append(inputParam).append("*)");
		}
		else
		{
			query.append("*:*");
		}

		StringBuffer hirachicalContent = new StringBuffer(" AND DMS_Content_ID:(");

		getHierarchicalContent(hirachicalContent, currDMSContent != null ? currDMSContent.getDMS_Content_ID() : 0);

		if (currDMSContent != null)
		{
			hirachicalContent.append(currDMSContent.getDMS_Content_ID()).append(")");
			query.append(hirachicalContent.toString());
		}
		else
		{
			if (hirachicalContent.substring(hirachicalContent.length() - 4, hirachicalContent.length()).equals(" OR "))
			{
				hirachicalContent.replace(hirachicalContent.length() - 4, hirachicalContent.length(), ")");
				query.append(hirachicalContent.toString());
			}
		}

		if (recordID > 0)
			query.append(" AND Record_ID:" + recordID);

		if (tableID > 0)
			query.append(" AND AD_Table_ID:" + tableID);

		List<Integer> documentList = indexSeracher.searchIndex(query.toString());
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		for (Integer entry : documentList)
		{
			List<Object> latestversion = DB.getSQLValueObjectsEx(null, SQL_LATEST_VERSION, entry, entry);

			map.put(new MDMSContent(Env.getCtx(), ((BigDecimal) latestversion.get(0)).intValue(), null),
					new MDMSAssociation(Env.getCtx(), ((BigDecimal) latestversion.get(1)).intValue(), null));
		}
		return map;
	}

	/**
	 * get all DMS Contents for rendering
	 */
	private HashMap<I_DMS_Content, I_DMS_Association> getDMSContents()
	{
		int contentID = 0;

		if (currDMSContent != null)
			contentID = currDMSContent.getDMS_Content_ID();

		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			// select only active records
			pstmt = DB.prepareStatement(Utils.SQL_GET_RELATED_FOLDER_CONTENT_ACTIVE, null);

			pstmt.setInt(1, contentID);
			pstmt.setInt(2, contentID);

			rs = pstmt.executeQuery();

			if (rs != null)
			{
				while (rs.next())
				{
					map.put((new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null)), (new MDMSAssociation(
							Env.getCtx(), rs.getInt("DMS_Association_ID"), null)));
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Content fetching failure: ", e);
			throw new AdempiereException("Content fetching failure: " + e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return map;
	}

	/**
	 * clear the gridview components
	 */
	private void clearComponenets()
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
	 */
	private void openDirectoryORContent(DMSViewerComponent DMSViewerComp) throws IOException, URISyntaxException, DocumentException
	{
		selectedDMSContent.push(DMSViewerComp.getDMSContent());

		selectedDMSAssociation.push(DMSViewerComp.getDMSAssociation());

		if (selectedDMSContent.peek().getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
		{
			currDMSContent = selectedDMSContent.pop();
			showBreadcumb(currDMSContent);
			renderViewer();
			lblPositionInfo.setValue(currDMSContent.getName());
			btnBack.setEnabled(true);
			btnNext.setEnabled(false);
		}
		else if (selectedDMSContent.peek().getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Content))
		{
			File documentToPreview = null;

			MDMSMimeType mimeType = new MDMSMimeType(Env.getCtx(), selectedDMSContent.peek().getDMS_MimeType_ID(), null);

			documentToPreview = fileStorageProvider.getFile(contentManager.getPath(selectedDMSContent.peek()));

			if (documentToPreview != null)
			{
				String name = selectedDMSContent.peek().getName();

				if (name.contains("(") && name.contains(")"))
					name = name.replace(name.substring(name.lastIndexOf("("), name.lastIndexOf(")") + 1), "");

				documentToPreview = convertToPDF(documentToPreview, mimeType);
				
				if (Utils.getContentEditor(mimeType.getMimeType()) != null)
				{
					Tab tabData = new Tab(name);
					tabData.setClosable(true);
					tabs.appendChild(tabData);
					tabBox.setSelectedTab(tabData);

					WDocumentViewer documentViewer = new WDocumentViewer(tabBox, documentToPreview,
							selectedDMSContent.peek(), tableID, recordID);
					Tabpanel tabPanel = documentViewer.initForm();
					tabPanels.appendChild(tabPanel);
					documentViewer.getAttributePanel().addEventListener("onUploadComplete", this);
					documentViewer.getAttributePanel().addEventListener("onRenameComplete", this);

					this.appendChild(tabBox);
				}
				else
				{
					AMedia media = new AMedia(documentToPreview, "application/octet-stream", null);
					Filedownload.save(media);
				}
			}
			else
			{
				FDialog.error(0, contentManager.getPath(currDMSContent) + " Content missing in storage,");
			}
		}
	}

	/**
	 * Convert .docx to .pdf
	 * convert .doc to .pdf
	 * 
	 * @param documentToPreview
	 * @param mimeType
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws DocumentException
	 */
	private File convertToPDF(File documentToPreview, MDMSMimeType mimeType)
			throws IOException, FileNotFoundException, DocumentException
	{
		if (mimeType.getMimeType()
				.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
		{
			XWPFDocument document = new XWPFDocument(new FileInputStream(documentToPreview));
			File newDocPDF = File.createTempFile("Zito", "DocxToPDF");
			OutputStream pdfFile = new FileOutputStream(newDocPDF);
			PdfOptions options = PdfOptions.create();
			PdfConverter.getInstance().convert(document, pdfFile, options);
			return newDocPDF;
		}
		else if (mimeType.getMimeType().equals("application/msword"))
		{
			HWPFDocument doc = new HWPFDocument(new FileInputStream(documentToPreview));
			WordExtractor we = new WordExtractor(doc);
			File newDocPDF   = File.createTempFile("Zito", "DocToPDF");
			OutputStream pdfFile = new FileOutputStream(newDocPDF);
			String k = we.getText();
			Document document = new Document();
			PdfWriter.getInstance(document, pdfFile);
			document.open();
			document.add(new Paragraph(k));
			document.close();
			return newDocPDF;
		}
		return documentToPreview;
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
				&& selectedDMSAssociation.peek().getDMS_AssociationType_ID() == Utils.getDMS_Association_Link_ID()
				&& currDMSContent != null
				&& currDMSContent.getDMS_Content_ID() == selectedDMSAssociation.peek().getDMS_Content_ID())
		{
			currDMSContent = new MDMSContent(Env.getCtx(), selectedDMSAssociation.peek().getDMS_Content_Related_ID(),
					null);
			lblPositionInfo.setValue(currDMSContent.getName());
			if (currDMSContent.getParentURL() == null)
				btnBack.setEnabled(true);

			btnNext.setEnabled(true);
		}
		else if (currDMSContent != null)
		{
			int DMS_Content_ID = DB
					.getSQLValue(
							null,
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
			int id = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE name = ? AND IsMounting = 'Y' ORDER BY created desc",
					recordID + "");

			if (currDMSContent == null)
				currDMSContent = selectedDMSContent.peek();

			if (currDMSContent.getDMS_Content_ID() == id)
			{
				btnBack.setDisabled(true);
				renderViewer();
			}
			return;
		}

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
		createDirectoryForm = new CreateDirectoryForm(currDMSContent, tableID, recordID);

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
		uploadContent = new WUploadContent(currDMSContent, false, tableID, recordID);

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

		if (dirContent.isMounting() && dirContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
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
			DMSViewerCom.setContext(contentContextMenu);
			contentContextMenu.open(this, "at_pointer");
			return;
		}
		else
		{
			mnu_associate.setDisabled(false);
			mnu_copy.setDisabled(false);
			mnu_createLink.setDisabled(false);
			mnu_cut.setDisabled(false);
			mnu_paste.setDisabled(false);
			mnu_rename.setDisabled(false);
			mnu_uploadVersion.setDisabled(false);
			mnu_versionList.setDisabled(false);
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
		else if (X_DMS_Content.CONTENTBASETYPE_Directory.equals(DMSViewerCom.getDMSContent().getContentBaseType()))
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

		if (X_DMS_Content.CONTENTBASETYPE_Content.equals(DMSViewerCom.getContentBaseType()))
		{
			mnu_versionList.setDisabled(false);
			mnu_paste.setDisabled(true);
			mnu_canvasPaste.setDisabled(true);
			mnu_uploadVersion.setDisabled(false);
			mnu_createLink.setDisabled(true);

			if (copyDMSContent != null && copyDMSContent != DMSViewerCom.getDMSContent())
				mnu_associate.setDisabled(false);
			else
				mnu_associate.setDisabled(true);
		}

		if (X_DMS_Content.CONTENTBASETYPE_Directory.equals(DMSViewerCom.getContentBaseType()))
		{
			if (copyDMSContent != null)
				mnu_createLink.setDisabled(false);

			if (DMSClipboard.get() != null && !DMSClipboard.getIsCopy())
			{
				mnu_paste.setDisabled(false);
				mnu_canvasPaste.setDisabled(false);
			}
		}

		mnu_copy.setDisabled(false);
		DMSViewerCom.setContext(contentContextMenu);
		contentContextMenu.open(this, "at_pointer");
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
			ZkCssHelper.appendStyle(prevComponent.getfLabel(),
					"background-color:#ffffff; box-shadow: 7px 7px 7px #ffffff");
		}

		for (DMSViewerComponent viewerComponent : viewerComponents)
		{
			if (viewerComponent.getDMSContent().getDMS_Content_ID() == DMSViewerComp.getDMSContent()
					.getDMS_Content_ID())
			{
				ZkCssHelper.appendStyle(DMSViewerComp.getfLabel(),
						"background-color:#99cbff; box-shadow: 7px 7px 7px #888888");

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

	private void linkCopyDocument(MDMSContent DMSContent, boolean isDir)
	{
		boolean isDocPresent = false;

		if (DMSClipboard.get() == null)
		{
			return;
		}

		// IF DMS Tab
		if (isTabViewer())
		{
			int DMS_Association_ID = DB
					.getSQLValue(
							null,
							"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ? AND DMS_AssociationType_ID = ? AND AD_Table_ID = ? AND Record_ID = ?",
							DMSClipboard.get().getDMS_Content_ID(), Utils.getDMS_Association_Record_ID(), tableID,
							recordID);

			if (DMS_Association_ID == -1)
			{
				MDMSAssociation DMSassociation = new MDMSAssociation(Env.getCtx(), 0, null);
				DMSassociation.setDMS_Content_ID(DMSClipboard.get().getDMS_Content_ID());

				int DMS_Content_Related_ID = DB
						.getSQLValue(
								null,
								"SELECT DMS_Content_Related_ID FROM DMS_Association WHERE DMS_Content_ID = ? "
										+ "AND DMS_AssociationType_ID IN (SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE Name ilike 'Parent')",
								DMSClipboard.get().getDMS_Content_ID());

				if (DMS_Content_Related_ID != 0)
					DMSassociation.setDMS_Content_Related_ID(DMS_Content_Related_ID);
				else
					DMSassociation.setDMS_Content_Related_ID(currDMSContent.getDMS_Content_ID());

				DMSassociation.setDMS_AssociationType_ID(Utils.getDMS_Association_Record_ID());
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

						if (dmsContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
							DMS_Content_ID = DMSassociation.getDMS_Content_ID();
						else
							DMS_Content_ID = DMSassociation.getDMS_Content_Related_ID();
					}

					MDMSContent dmsContent = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);

					indexSeracher.indexContent(Utils.createIndexMap(dmsContent, DMSassociation));
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

		isDocPresent = isDoumentPresent(DMSContent, isDir);
		if (!isDocPresent)
		{
			MDMSAssociation DMSassociation = new MDMSAssociation(Env.getCtx(), 0, null);
			DMSassociation.setDMS_Content_ID(DMSClipboard.get().getDMS_Content_ID());
			if (DMSContent != null)
				DMSassociation.setDMS_Content_Related_ID(DMSContent.getDMS_Content_ID());
			DMSassociation.setDMS_AssociationType_ID(Utils.getDMS_Association_Link_ID());

			DMSassociation.saveEx();

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
		else
		{
			FDialog.warn(0, "Document already exists.");
		}
	}

	private boolean isDoumentPresent(MDMSContent DMSContent, boolean isDir)
	{
		StringBuilder query = new StringBuilder();
		query.append("SELECT count(DMS_Content_ID) FROM DMS_Association where DMS_Content_ID = ");
		query.append(DMSClipboard.get().getDMS_Content_ID());

		if (currDMSContent == null && !isDir)
		{
			query.append(" AND DMS_Content_Related_ID IS NULL");
		}
		else
		{
			query.append(" AND DMS_Content_Related_ID = ").append(DMSContent.getDMS_Content_ID());
		}

		return DB.getSQLValue(null, query.toString()) > 0 ? true : false;
	}

	private HashMap<String, List<Object>> getQueryParamas()
	{
		HashMap<String, List<Object>> params = new LinkedHashMap<String, List<Object>>();
		List<Object> value = new ArrayList<Object>();

		if (!Util.isEmpty(txtDocumentName.getValue(), true))
		{
			value.add("*" + txtDocumentName.getValue().toLowerCase() + "*");
			params.put(Utils.NAME, value);

		}

		if (!Util.isEmpty(txtDescription.getValue(), true))
		{
			value = new ArrayList<Object>();
			value.add("*" + txtDescription.getValue().toLowerCase() + "*");
			params.put(Utils.DESCRIPTION, value);
		}

		if (dbCreatedFrom.getValue() != null && dbCreatedTo.getValue() != null)
		{
			if (dbCreatedFrom.getValue().after(dbCreatedTo.getValue()))
				throw new WrongValueException(dbCreatedFrom, "Invalid Date Range");
			else
			{
				value = new ArrayList<Object>();
				value.add(dateFormat.format(dbCreatedFrom.getValue()));
				value.add(dateFormat.format(dbCreatedTo.getValue()));
				params.put(Utils.CREATED, value);
			}
		}
		else if (dbCreatedFrom.getValue() != null && dbCreatedTo.getValue() == null)
		{
			value = new ArrayList<Object>();
			value.add(dateFormat.format(dbCreatedFrom.getValue()));
			value.add("*");
			params.put(Utils.CREATED, value);
		}
		else if (dbCreatedTo.getValue() != null && dbCreatedFrom.getValue() == null)
		{
			value = new ArrayList<Object>();
			value.add("*");
			value.add(dateFormat.format(dbCreatedTo.getValue()));
			params.put(Utils.CREATED, value);
		}

		if (dbUpdatedFrom.getValue() != null && dbUpdatedTo.getValue() != null)
		{
			if (dbUpdatedFrom.getValue().after(dbUpdatedTo.getValue()))
				throw new WrongValueException(dbUpdatedFrom, "Invalid Date Range");
			else
			{
				value = new ArrayList<Object>();
				value.add(dateFormat.format(dbUpdatedFrom.getValue()));
				value.add(dateFormat.format(dbUpdatedTo.getValue()));
				params.put(Utils.UPDATED, value);
			}

		}
		else if (dbUpdatedFrom.getValue() != null && dbUpdatedTo.getValue() == null)
		{
			value = new ArrayList<Object>();
			value.add(dateFormat.format(dbUpdatedFrom.getValue()));
			value.add("*");
			params.put(Utils.UPDATED, value);
		}
		else if (dbUpdatedTo.getValue() != null && dbUpdatedFrom.getValue() == null)
		{
			value = new ArrayList<Object>();
			value.add("*");
			value.add(dateFormat.format(dbUpdatedTo.getValue()));
			params.put(Utils.UPDATED, value);
		}

		if (lstboxCreatedBy.getValue() != null)
		{
			value = new ArrayList<Object>();
			value.add(lstboxCreatedBy.getValue());
			params.put(Utils.CREATEDBY, value);
		}

		if (lstboxUpdatedBy.getValue() != null)
		{
			value = new ArrayList<Object>();
			value.add(lstboxUpdatedBy.getValue());
			params.put(Utils.UPDATEDBY, value);
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
			params.put(Utils.SHOW_INACTIVE, value);
		}
		
		if (lstboxContentType.getValue() != null)
		{

			value = new ArrayList<Object>();
			value.add(lstboxContentType.getValue());
			params.put(Utils.CONTENTTYPE, value);

			for (WEditor editor : m_editors)
			{
				//if (editor.getValue() != null && editor.getValue() != "")
				//{
					int displayType = editor.getGridField().getDisplayType();
					String compName = null;

					if (displayType == DisplayType.Search || displayType == DisplayType.Table)
						compName = "ASI_" + editor.getColumnName().replaceAll("(?i)[^a-z0-9-_/]", "_");
					else
						compName = "ASI_" + editor.getLabel().getValue().replaceAll("(?i)[^a-z0-9-_/]", "_");

					compName = compName.replaceAll("/", "");

					if (displayType == DisplayType.Number || displayType == DisplayType.Integer
							|| displayType == DisplayType.Quantity || displayType == DisplayType.Amount
							|| displayType == DisplayType.CostPrice)
					{
						NumberBox fromNumBox = (NumberBox) ASI_Value.get(compName);
						NumberBox toNumBox = (NumberBox) ASI_Value.get(compName + "to");

						if (fromNumBox.getValue() != null && toNumBox.getValue() != null)
						{
							value = new ArrayList<Object>();
							
							if(displayType == DisplayType.Number)
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
							
							if(displayType == DisplayType.Number)
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
							if(displayType == DisplayType.Number)
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
					else if (displayType == DisplayType.Date || displayType == DisplayType.DateTime
							|| displayType == DisplayType.Time)
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
									value.add(dateFormat.format(fromDate.getValue()));
									value.add(dateFormat.format(toDate.getValue()));
									params.put(compName, value);
								}
							}
							else if (fromDate.getValue() != null && toDate.getValue() == null)
							{
								value = new ArrayList<Object>();
								value.add(dateFormat.format(fromDate.getValue()));
								value.add("*");
								params.put(compName, value);
							}
							else if (toDate.getValue() != null && fromDate.getValue() == null)
							{
								value = new ArrayList<Object>();
								value.add("*");
								value.add(dateFormat.format(toDate.getValue()));
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
									value.add(dateFormat.format(fromDatetime.getValue()));
									value.add(dateFormat.format(toDatetime.getValue()));
									params.put(compName, value);
								}
							}
							else if (fromDatetime.getValue() != null && toDatetime.getValue() == null)
							{
								value = new ArrayList<Object>();
								value.add(dateFormat.format(fromDatetime.getValue()));
								value.add("*");
								params.put(compName, value);
							}
							else if (toDatetime.getValue() != null && fromDatetime.getValue() == null)
							{
								value = new ArrayList<Object>();
								value.add("*");
								value.add(dateFormat.format(toDatetime.getValue()));
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
									value.add(dateFormat.format(timeboxFrom.getValue()));
									value.add(dateFormat.format(timeboxTo.getValue()));
									params.put(compName, value);
								}
							}
							else if (timeboxFrom.getValue() != null && timeboxTo.getValue() == null)
							{
								value = new ArrayList<Object>();
								value.add(dateFormat.format(timeboxFrom.getValue()));
								value.add("*");
								params.put(compName, value);
							}
							else if (timeboxTo.getValue() != null && timeboxFrom.getValue() == null)
							{
								value = new ArrayList<Object>();
								value.add("*");
								value.add(dateFormat.format(timeboxTo.getValue()));
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
						if(!Util.isEmpty(editor.getValue().toString(), true))
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
				//}
			}
		}

		if (tableID > 0)
		{
			value = new ArrayList<Object>();
			value.add(tableID);
			params.put(Utils.AD_Table_ID, value);
		}

		if (recordID > 0)
		{
			value = new ArrayList<Object>();
			value.add(recordID);
			params.put(Utils.RECORD_ID, value);
		}

		return params;
	}

	private HashMap<I_DMS_Content, I_DMS_Association> renderSearchedContent()
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();
		List<Integer> documentList = null;

		HashMap<String, List<Object>> params = getQueryParamas();
		String query = indexSeracher.buildSolrSearchQuery(params);

		StringBuffer hirachicalContent = new StringBuffer(" AND DMS_Content_ID:(");

		getHierarchicalContent(hirachicalContent, currDMSContent != null ? currDMSContent.getDMS_Content_ID() : 0);

		if (currDMSContent != null)
		{
			hirachicalContent.append(currDMSContent.getDMS_Content_ID()).append(")");
			query += " " + hirachicalContent.toString();
		}
		else
		{
			if (hirachicalContent.substring(hirachicalContent.length() - 4, hirachicalContent.length()).equals(" OR "))
			{
				hirachicalContent.replace(hirachicalContent.length() - 4, hirachicalContent.length(), ")");
				query += " " + hirachicalContent.toString();
			}
		}

		documentList = indexSeracher.searchIndex(query);

		for (Integer entry : documentList)
		{
			List<Object> latestversion = DB.getSQLValueObjectsEx(null, SQL_LATEST_VERSION, entry, entry);

			map.put(new MDMSContent(Env.getCtx(), ((BigDecimal) latestversion.get(0)).intValue(), null),
					new MDMSAssociation(Env.getCtx(), ((BigDecimal) latestversion.get(1)).intValue(), null));
		}
		return map;
	}

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

					if (displayType == DisplayType.Number || displayType == DisplayType.Integer
							|| displayType == DisplayType.Quantity || displayType == DisplayType.Amount
							|| displayType == DisplayType.CostPrice)
					{
						NumberBox numBox = new NumberBox(false);

						row.appendChild(editor.getLabel());
						row.appendChild(editor.getComponent());
						row.appendChild(numBox);

						ASI_Value.put(compName, editor.getComponent());
						ASI_Value.put(compName + "to", numBox);

					}

					else if (displayType == DisplayType.Date || displayType == DisplayType.DateTime
							|| displayType == DisplayType.Time)
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
	}

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
	}

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

	private void getHierarchicalContent(StringBuffer hierarchicalContent, int DMS_Content_ID)
	{
		PreparedStatement pstmt = DB.prepareStatement(Utils.SQL_GET_RELATED_FOLDER_CONTENT_ALL, null);
		ResultSet rs = null;
		try
		{
			pstmt.setInt(1, DMS_Content_ID);
			pstmt.setInt(2, DMS_Content_ID);
			rs = pstmt.executeQuery();

			while (rs.next())
			{
				MDMSContent dmsContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);

				if (dmsContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
					getHierarchicalContent(hierarchicalContent, dmsContent.getDMS_Content_ID());
				else
				{
					MDMSAssociation association = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"),
							null);
					hierarchicalContent.append(association.getDMS_Content_ID() + " OR ");

					if (association.getDMS_Content_ID() != dmsContent.getDMS_Content_ID())
						hierarchicalContent.append(dmsContent.getDMS_Content_ID() + " OR ");
				}
			}

		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Fail to get hierarchical Content.", e);
			throw new AdempiereException("Fail to get hierarchical Content: " + e);
		}
		finally
		{
			DB.close(rs, pstmt);
			pstmt = null;
			rs = null;
		}
	}
}