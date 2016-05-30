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
import java.util.TimeZone;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.adwindow.BreadCrumbLink;
import org.adempiere.webui.component.Button;
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
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.window.FDialog;
import org.apache.commons.io.FileUtils;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MColumn;
import org.compiere.model.MImage;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.PO;
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

public class WDMSPanel extends Panel implements EventListener<Event>, ValueChangeListener
{

	private static final long				serialVersionUID			= -6813481516566180243L;
	public static CLogger					log							= CLogger.getCLogger(WDMSPanel.class);

	private static final String				SQL_FETCH_DMS_CONTENTS		= "WITH ContentAssociation AS "
																				+ " ( "
																				+ " SELECT	c.DMS_Content_ID, a.DMS_Content_Related_ID, c.ContentBasetype, "
																				+ " a.DMS_Association_ID, a.DMS_AssociationType_ID, a.AD_Table_ID, a.Record_ID "
																				+ " FROM DMS_Association a "
																				+ " INNER JOIN DMS_Content c	ON (c.DMS_Content_ID = a.DMS_Content_ID) "
																				+ " ) "
																				+ " SELECT "
																				+ " COALESCE((SELECT a.DMS_Content_ID FROM DMS_Association a WHERE a.DMS_Content_Related_ID = ca.DMS_Content_ID AND a.DMS_AssociationType_ID = 1000000 ORDER BY SeqNo DESC FETCH FIRST ROW ONLY), DMS_Content_ID) AS DMS_Content_ID, "
																				+ " COALESCE((SELECT a.DMS_Content_Related_ID FROM DMS_Association a WHERE a.DMS_Content_Related_ID = ca.DMS_Content_ID AND a.DMS_AssociationType_ID = 1000000 ORDER BY SeqNo DESC FETCH FIRST ROW ONLY), DMS_Content_Related_ID) AS DMS_Content_Related_ID, DMS_Association_ID "
																				+ " FROM ContentAssociation ca "
																				+ " WHERE "
																				+ " (COALESCE(AD_Table_ID,0) = COALESCE(?,0) AND COALESCE(Record_ID,0) = COALESCE(?,0) AND COALESCE(DMS_Content_Related_ID,0) = COALESCE(?,0)) OR "
																				+ " (COALESCE(DMS_Content_Related_ID,0) = COALESCE(?,0) AND ContentBaseType = 'DIR') OR "
																				+ " (CASE WHEN (? = 0 AND ? = 0) "
																				+ " THEN ((COALESCE(DMS_Content_Related_ID,0) = COALESCE(?,0) AND COALESCE(AD_Table_ID,0) != 0 AND COALESCE(Record_ID,0) != 0)) "
																				+ " ELSE ((COALESCE(DMS_Content_Related_ID,0) = COALESCE(?,0) AND AD_Table_ID = ? AND Record_ID = ?)) "
																				+ " END)";

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
	private BreadCrumbLink					breadCrumbEvent				= null;

	private Rows							breadRows					= new Rows();
	private Row								breadRow					= new Row();

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

	// create Directory
	private Button							btnCreateDir				= new Button();
	private Button							btnUploadContent			= new Button();
	private Button							btnBack						= new Button();
	private Button							btnNext						= new Button();

	private Label							lblPositionInfo				= new Label();

	private MDMSContent						currDMSContent				= null;
	private MDMSContent						prevDMSContent				= null;
	private MDMSContent						nextDMSContent				= null;
	private MDMSContent						cutDMSContent				= null;

	private ArrayList<DMSViewerComponent>	viewerComponents			= null;

	public IFileStorageProvider				fileStorageProvider			= null;
	public IThumbnailProvider				thumbnailProvider			= null;
	public IContentManager					contentManager				= null;
	public IFileStorageProvider				thumbnailStorageProvider	= null;
	private IIndexSearcher					indexSeracher				= null;

	private Menupopup						contentContextMenu			= new Menupopup();
	private Menupopup						canvasContextMenu			= new Menupopup();

	private Menuitem						versionList					= null;
	private Menuitem						copy						= null;
	private Menuitem						createLink					= null;
	private Menuitem						delete						= null;
	private Menuitem						associate					= null;
	private Menuitem						uploadVersion				= null;
	private Menuitem						rename						= null;
	private Menuitem						cut							= null;
	private Menuitem						paste						= null;

	private Menuitem						canvasCreateLink			= null;
	private Menuitem						canvasPaste					= null;

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
	private boolean							isCut						= false;
	private boolean							isCopy						= false;

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
		grid.setStyle("width: 100%; height:100%; position:relative; overflow: auto;");
		// View Result Tab

		Grid gridView = GridFactory.newGridLayout();
		gridView.setStyle("height:100%; position:relative; overflow: auto;");
		gridView.setWidth("100%");
		gridView.setHeight("100%");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
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
		gridView.appendChild(rows);

		Row row = new Row();
		rows.appendChild(row);

		btnBack.setImage(ThemeManager.getThemeResource("images/wfBack24.png"));
		btnBack.setTooltiptext("Previous Record");

		lblPositionInfo.setHflex("1");
		lblPositionInfo.setStyle("float: right;");
		ZkCssHelper.appendStyle(lblPositionInfo, "font-weight: bold;");
		ZkCssHelper.appendStyle(lblPositionInfo, "text-align: center;");

		btnNext.setImage(ThemeManager.getThemeResource("images/wfNext24.png"));
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

		btnCreateDir.setImage(ThemeManager.getThemeResource("images/Folder24.png"));
		btnCreateDir.setTooltiptext("Create Directory");
		btnCreateDir.addEventListener(Events.ON_CLICK, this);

		btnUploadContent.setImage(ThemeManager.getThemeResource("images/Parent24.png"));
		btnUploadContent.setTooltiptext("Upload Content");
		btnUploadContent.addEventListener(Events.ON_CLICK, this);

		row = new Row();
		Cell searchCell = new Cell();
		searchCell.setRowspan(1);
		searchCell.setColspan(3);
		searchCell.appendChild(vsearchBox);
		rows.appendChild(row);
		row.appendChild(searchCell);
		vsearchBox.getButton().setImage(ThemeManager.getThemeResource("images/Find16.png"));

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
		dbCreatedFrom.setStyle("width: 100%; display:flex; flex-direction: row; flex-wrap: wrap;");
		dbCreatedTo.setStyle("width: 100%; display:flex; flex-direction: row; flex-wrap: wrap;");
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
		dbUpdatedFrom.setStyle("width: 100%; display:flex; flex-direction: row; flex-wrap: wrap;");
		dbUpdatedTo.setStyle("width: 100%; display:flex; flex-direction: row; flex-wrap: wrap;");
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

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblContentMeta);
		ZkCssHelper.appendStyle(lblContentMeta, "font-weight: bold;");

		row = new Row();
		rows.appendChild(row);
		Cell cell = new Cell();
		cell.setColspan(3);
		cell.appendChild(panelAttribute);
		panelAttribute.setStyle("max-height: 200px; overflow: auto;");
		row.appendChild(cell);

		row = new Row();
		rows.appendChild(row);
		hbox = new Hbox();

		btnClear.addEventListener(Events.ON_CLICK, this);
		btnRefresh.addEventListener(Events.ON_CLICK, this);

		btnSearch.setImage(ThemeManager.getThemeResource("images/Find24.png"));
		btnSearch.setTooltiptext("Search");

		btnSearch.addEventListener(Events.ON_CLICK, this);

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
		cell.setWidth("30%");
		cell.appendChild(gridView);
		boxViewSeparator.appendChild(cell);
		tabViewPanel.appendChild(boxViewSeparator);
		gridView.appendChild(rows);

		tabs.appendChild(tabView);
		tabPanels.appendChild(tabViewPanel);
		this.setHeight("100%");
		this.setWidth("100%");
		this.appendChild(tabBox);

		this.addEventListener(Events.ON_CLICK, this);
		this.addEventListener(Events.ON_DOUBLE_CLICK, this);

		versionList = new Menuitem(MENUITEM_VERSIONlIST);
		cut = new Menuitem(MENUITEM_CUT);
		copy = new Menuitem(MENUITEM_COPY);
		paste = new Menuitem(MENUITEM_PASTE);
		createLink = new Menuitem(MENUITEM_CREATELINK);
		delete = new Menuitem(MENUITEM_DELETE);
		associate = new Menuitem(MENUITEM_ASSOCIATE);
		uploadVersion = new Menuitem(MENUITEM_UPLOADVERSION);
		rename = new Menuitem(MENUITEM_RENAME);

		canvasCreateLink = new Menuitem(MENUITEM_CREATELINK);
		canvasPaste = new Menuitem(MENUITEM_PASTE);

		canvasContextMenu.appendChild(canvasPaste);
		canvasContextMenu.appendChild(canvasCreateLink);
		canvasCreateLink.addEventListener(Events.ON_CLICK, this);
		canvasPaste.addEventListener(Events.ON_CLICK, this);

		canvasCreateLink.setImage(ThemeManager.getThemeResource("images/Attachment24.png"));

		contentContextMenu.appendChild(uploadVersion);
		contentContextMenu.appendChild(versionList);
		contentContextMenu.appendChild(cut);
		contentContextMenu.appendChild(copy);
		contentContextMenu.appendChild(paste);
		contentContextMenu.appendChild(createLink);
		contentContextMenu.appendChild(rename);
		contentContextMenu.appendChild(delete);
		contentContextMenu.appendChild(associate);

		uploadVersion.setImage(ThemeManager.getThemeResource("images/Assignment24.png"));
		rename.setImage(ThemeManager.getThemeResource("images/Editor24.png"));
		cut.setImage(ThemeManager.getThemeResource("images/Cancel24.png"));
		versionList.setImage(ThemeManager.getThemeResource("images/Wizard24.png"));
		copy.setImage(ThemeManager.getThemeResource("images/Copy24.png"));
		delete.setImage(ThemeManager.getThemeResource("images/Delete24.png"));
		associate.setImage(ThemeManager.getThemeResource("images/Attachment24.png"));

		uploadVersion.addEventListener(Events.ON_CLICK, this);
		versionList.addEventListener(Events.ON_CLICK, this);
		cut.addEventListener(Events.ON_CLICK, this);
		paste.addEventListener(Events.ON_CLICK, this);
		rename.addEventListener(Events.ON_CLICK, this);
		createLink.addEventListener(Events.ON_CLICK, this);
		copy.addEventListener(Events.ON_CLICK, this);
		delete.addEventListener(Events.ON_CLICK, this);
		associate.addEventListener(Events.ON_CLICK, this);

		delete.setDisabled(true);

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
			if (isSearch)
			{
				isSearch = false;
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
			isSearch = false;
			clearComponenets();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_REFRESH))
		{
			HashMap<String, List<Object>> params = getQueryParamas();
			String query = indexSeracher.buildSolrSearchQuery(params);

			if (query.equals("*:*"))
			{
				isSearch = false;
				if (currDMSContent != null)
					lblPositionInfo.setValue(currDMSContent.getName());
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

			renderViewer();
		}
		else if (event.getTarget().equals(btnSearch))
		{
			HashMap<String, List<Object>> params = getQueryParamas();
			String query = indexSeracher.buildSolrSearchQuery(params);

			if (query.equals("*:*"))
			{
				isSearch = false;
				if (currDMSContent != null)
					lblPositionInfo.setValue(currDMSContent.getName());
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
		else if (event.getTarget().equals(versionList))
		{
			new WDMSVersion(DMSViewerComp.getDMSContent());
		}
		else if (event.getTarget().equals(uploadVersion))
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
		else if (event.getTarget().equals(copy))
		{
			isCopy = true;
			isCut = false;
			DMSClipboard.put(DMSViewerComp.getDMSContent());
		}
		else if (event.getTarget().equals(createLink))
		{
			linkCopyDocument(dirContent, true);
		}
		else if (event.getTarget().equals(cut))
		{
			cutDMSContent = dirContent;
			isCut = true;
			isCopy = false;
		}
		else if (event.getTarget().equals(paste) || event.getTarget().equals(canvasPaste))
		{
			if (isCut && cutDMSContent != null)
			{
				MDMSContent destPasteContent = dirContent;
				pasteCutContent(cutDMSContent, destPasteContent);
				renderViewer();
				isCut = false;
			}
			else if (isCopy && DMSClipboard.get() != null)
			{
				MDMSContent copiedContent = DMSClipboard.get();
				MDMSContent destPasteContent = dirContent;
				pasteCopyContent(copiedContent, destPasteContent);
				renderViewer();
			}
		}
		else if (event.getTarget().equals(rename))
		{
			final WRenameContent renameContent = new WRenameContent(dirContent, tableID, recordID);
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
		else if (event.getTarget().equals(delete))
		{

		}
		else if (event.getTarget().equals(associate))
		{
			new WDAssociationType(copyDMSContent, DMSViewerComp.getDMSContent());
		}
		else if (event.getTarget().equals(canvasCreateLink))
		{
			linkCopyDocument(currDMSContent, false);
		}
		else if (event.getName().equals("onUploadComplete"))
		{
			renderViewer();
		}
		else if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(BreadCrumbLink.class))
		{
			renderBreadCrumb(event);
		}
	}

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

			if (!Util.isEmpty(copiedContent.getParentURL()))
				baseURL = contentManager.getPath(copiedContent);
			else
				baseURL = spFileSeprator + copiedContent.getName();

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
					throw new AdempiereException("Directory already exists.");
				}
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
			}
			if (newASI != null)
				newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

			newDMSContent.setParentURL(contentManager.getPath(destPasteContent));

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
			newDMSAssociation.saveEx();

			copyContent(copiedContent, baseURL, renamedURL, newDMSContent);
			try
			{
				FileUtils.copyDirectory(dirPath, newFile);
			}
			catch (IOException e)
			{
				log.log(Level.SEVERE, "Copy Content Failure.", e);
				throw new AdempiereException("Copy Content Failure." + e.getLocalizedMessage());
			}
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
			pstmt = DB.prepareStatement(Utils.SQL_GET_RELATED_FOLDER_CONTENT, null);
			pstmt.setInt(1, tableID);
			pstmt.setInt(2, recordID);
			pstmt.setInt(3, copiedContent.getDMS_Content_ID());
			pstmt.setInt(4, copiedContent.getDMS_Content_ID());
			pstmt.setInt(5, tableID);
			pstmt.setInt(6, recordID);
			pstmt.setInt(7, copiedContent.getDMS_Content_ID());
			pstmt.setInt(8, copiedContent.getDMS_Content_ID());
			pstmt.setInt(9, tableID);
			pstmt.setInt(10, recordID);

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
		int DMS_Content_ID = Utils.getDMS_Content_Related_ID(new MDMSContent(Env.getCtx(), oldDMSContent
				.getDMS_Content_ID(), null));
		int crID = 0;
		PreparedStatement ps = DB
				.prepareStatement(
						"SELECT DMS_Association_ID,DMS_Content_ID FROM DMS_Association WHERE DMS_Content_Related_ID = ? AND DMS_AssociationType_ID = 1000000 OR DMS_Content_ID = ? Order By DMS_Association_ID",
						null);
		ps.setInt(1, DMS_Content_ID);
		ps.setInt(2, DMS_Content_ID);
		ResultSet res = ps.executeQuery();

		while (res.next())
		{
			String baseURL = null;
			String renamedURL = null;

			oldDMSContent = new MDMSContent(Env.getCtx(), res.getInt("DMS_Content_ID"), null);

			if (!Util.isEmpty(oldDMSContent.getParentURL()))
				baseURL = contentManager.getPath(oldDMSContent);
			else
				baseURL = spFileSeprator + oldDMSContent.getName();

			baseURL = baseURL.substring(0, baseURL.lastIndexOf("/"));

			File oldFile = new File(fileStorageProvider.getBaseDirectory(contentManager.getPath(oldDMSContent)));
			String newFileName = fileStorageProvider.getBaseDirectory(contentManager.getPath(destPasteContent));

			File files[] = new File(newFileName).listFiles();

			if (newFileName.charAt(newFileName.length() - 1) == spFileSeprator.charAt(0))
				newFileName = newFileName + oldDMSContent.getName();
			else
				newFileName = newFileName + spFileSeprator + oldDMSContent.getName();

			File newFile = new File(newFileName);

			if (newFile.exists())
			{
				String uniqueName = Utils.getUniqueFilename(newFile.getAbsolutePath());
				newFile = new File(uniqueName);
			}

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
			}
			if (newASI != null)
				newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());

			newDMSContent.setName(newFile.getName());
			newDMSContent.saveEx();

			MDMSAssociation oldDMSAssociation = new MDMSAssociation(Env.getCtx(), res.getInt("DMS_Association_ID"),
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

			newDMSAssociation.saveEx();

			if (oldDMSContent.getParentURL().startsWith(baseURL))
			{
				newDMSContent.setParentURL(oldDMSContent.getParentURL().replaceFirst(baseURL, renamedURL));
				newDMSContent.saveEx();
			}
			MDMSMimeType mimeType = new MDMSMimeType(Env.getCtx(), newDMSContent.getDMS_MimeType_ID(), null);

			IThumbnailGenerator thumbnailGenerator = Utils.getThumbnailGenerator(mimeType.getMimeType());

			if (thumbnailGenerator != null)
				thumbnailGenerator.addThumbnail(newDMSContent,
						fileStorageProvider.getFile(contentManager.getPath(oldDMSContent)), null);

			String newPath = fileStorageProvider.getBaseDirectory(contentManager.getPath(destPasteContent));
			newPath = newPath + spFileSeprator + oldDMSContent.getName();

			if (!newFile.exists())
			{
				FileUtils.copyFile(oldFile, newFile);
			}

			try
			{
				Map<String, Object> solrValue = Utils.createIndexMap(newDMSContent, newDMSAssociation);
				indexSeracher.deleteIndex(newDMSContent.getDMS_Content_ID());
				indexSeracher.indexContent(solrValue);
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Indexing of copy Content Failure :", e);
				throw new AdempiereException("Indexing of copy Content Failure :" + e);
			}
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

			Utils.renameFolder(sourceCutContent, baseURL, renamedURL, tableID, recordID);
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

		if (isSearch)
			dmsContent = renderSearchedContent();
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
				viewerComponent = new DMSViewerComponent(entry.getKey(), image, true);
			}
			else
			{
				viewerComponent = new DMSViewerComponent(entry.getKey(), image, false);
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

			pstmt = DB.prepareStatement(SQL_FETCH_DMS_CONTENTS, null);

			pstmt.setInt(1, tableID);
			pstmt.setInt(2, recordID);
			pstmt.setInt(3, contentID);
			pstmt.setInt(4, contentID);
			pstmt.setInt(5, tableID);
			pstmt.setInt(6, recordID);
			pstmt.setInt(7, contentID);
			pstmt.setInt(8, contentID);
			pstmt.setInt(9, tableID);
			pstmt.setInt(10, recordID);

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
	 */
	private void openDirectoryORContent(DMSViewerComponent DMSViewerComp) throws IOException, URISyntaxException
	{
		MDMSContent selectedContent = DMSViewerComp.getDMSContent();

		if (selectedContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
		{
			currDMSContent = selectedContent;
			showBreadcumb(currDMSContent);
			renderViewer();
			lblPositionInfo.setValue(selectedContent.getName());
			btnBack.setEnabled(true);
			btnNext.setEnabled(false);
		}
		else if (selectedContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Content))
		{
			File documentToPreview = null;

			MDMSMimeType mimeType = new MDMSMimeType(Env.getCtx(), selectedContent.getDMS_MimeType_ID(), null);

			documentToPreview = fileStorageProvider.getFile(contentManager.getPath(selectedContent));

			if (documentToPreview != null)
			{
				String name = selectedContent.getName();

				if (name.contains("(") && name.contains(")"))
					name = name.replace(name.substring(name.lastIndexOf("("), name.lastIndexOf(")") + 1), "");

				if (Utils.getContentEditor(mimeType.getMimeType()) != null)
				{
					Tab tabData = new Tab(name);
					tabData.setClosable(true);
					tabs.appendChild(tabData);
					tabBox.setSelectedTab(tabData);

					WDocumentViewer documentViewer = new WDocumentViewer(tabBox, documentToPreview, selectedContent,
							tableID, recordID);
					tabPanels.appendChild(documentViewer.initForm());
					documentViewer.getAttributePanel().addEventListener("onUploadComplete", this);

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
		}
		else
		{
			addRootBreadCrumb();
		}

		nextDMSContent = currDMSContent;

		if (currDMSContent != null)
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

				lblShowBreadCrumb = new Label();
				lblShowBreadCrumb.setValue(" > ");
				breadRow.appendChild(new Space());
				breadRow.appendChild(lblShowBreadCrumb);
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

		renderViewer();

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

		breadRow.appendChild(breadCrumbLink);
		breadRows.appendChild(breadRow);
		gridBreadCrumb.appendChild(breadRows);

		if (nextDMSContent != null)
		{
			currDMSContent = nextDMSContent;
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

		if (copyDMSContent == null)
		{
			paste.setDisabled(true);
			canvasPaste.setDisabled(true);
			createLink.setDisabled(true);
			associate.setDisabled(true);
			versionList.setDisabled(true);
			uploadVersion.setDisabled(true);
		}
		else if (copyDMSContent == DMSViewerCom.getDMSContent())
		{
			paste.setDisabled(true);
			canvasPaste.setDisabled(true);
			associate.setDisabled(true);
			createLink.setDisabled(true);
		}
		else if (X_DMS_Content.CONTENTBASETYPE_Directory.equals(DMSViewerCom.getDMSContent().getContentBaseType()))
		{
			paste.setDisabled(false);
			canvasPaste.setDisabled(false);
			associate.setDisabled(true);
			versionList.setDisabled(true);
			uploadVersion.setDisabled(true);
		}
		else
		{
			createLink.setDisabled(false);
			associate.setDisabled(false);
		}

		if (X_DMS_Content.CONTENTBASETYPE_Content.equals(DMSViewerCom.getContentBaseType()))
		{
			versionList.setDisabled(false);
			paste.setDisabled(true);
			canvasPaste.setDisabled(true);
			uploadVersion.setDisabled(false);
			createLink.setDisabled(true);

			if (copyDMSContent != null && copyDMSContent != DMSViewerCom.getDMSContent())
				associate.setDisabled(false);
			else
				associate.setDisabled(true);
		}

		if (X_DMS_Content.CONTENTBASETYPE_Directory.equals(DMSViewerCom.getContentBaseType()))
		{
			if (copyDMSContent != null)
				createLink.setDisabled(false);

			if (cutDMSContent != null)
			{
				paste.setDisabled(false);
				canvasPaste.setDisabled(false);
			}
		}

		copy.setDisabled(false);
		DMSViewerCom.setContext(contentContextMenu);
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
			canvasCreateLink.setDisabled(true);
		}
		else
		{
			canvasCreateLink.setDisabled(false);
		}

		if (cutDMSContent == null)
		{
			canvasPaste.setDisabled(true);
		}
		else
		{
			canvasPaste.setDisabled(false);
		}

		if (!isCut && !isCopy)
		{
			canvasPaste.setDisabled(true);
		}
		else
		{
			canvasPaste.setDisabled(false);
		}
	}

	private void linkCopyDocument(MDMSContent DMSContent, boolean isDir)
	{
		boolean isDocPresent = false;

		if (DMSClipboard.get() == null)
		{
			return;
		}

		// IF DMS Tab
		if (recordID > 0 && tableID > 0)
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
		HashMap<String, List<Object>> params = new HashMap<String, List<Object>>();
		List<Object> value = new ArrayList<Object>();

		if (!Util.isEmpty(txtDocumentName.getValue()))
		{
			value.add(txtDocumentName.getValue());
			params.put(Utils.NAME, value);

		}

		if (!Util.isEmpty(txtDescription.getValue()))
		{
			value = new ArrayList<Object>();
			value.add("*" + txtDescription.getValue() + "*");
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

		if (lstboxContentType.getValue() != null)
		{

			for (WEditor editor : m_editors)
			{
				if (editor.getValue() != null && editor.getValue() != "")
				{
					int displayType = editor.getGridField().getDisplayType();
					String compName = "ASI_" + editor.getLabel().getValue().replaceAll("(?i)[^a-z0-9-_/]", "_");
					compName = compName.replaceAll("/", "");

					if (displayType == DisplayType.Number)
					{
						NumberBox fromNumBox = (NumberBox) ASI_Value.get(compName);
						NumberBox toNumBox = (NumberBox) ASI_Value.get(compName + "to");

						if (fromNumBox.getValue() != null && toNumBox.getValue() != null)
						{
							value = new ArrayList<Object>();
							value.add(fromNumBox.getValue());
							value.add(toNumBox.getValue());
							params.put(compName, value);
						}
						else if (fromNumBox.getValue() != null && toNumBox.getValue() == null)
						{
							value = new ArrayList<Object>();
							value.add(fromNumBox.getValue());
							params.put(compName, value);
						}
						else if (fromNumBox.getValue() == null && toNumBox.getValue() != null)
						{
							value = new ArrayList<Object>();
							value.add("*");
							value.add(toNumBox.getValue());
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
					else
					{
						value = new ArrayList<Object>();
						value.add(editor.getDisplay());
						params.put(compName, value);
					}
				}
			}
		}
		return params;
	}

	private HashMap<I_DMS_Content, I_DMS_Association> renderSearchedContent()
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();
		List<Integer> documentList = null;

		HashMap<String, List<Object>> params = getQueryParamas();
		String query = indexSeracher.buildSolrSearchQuery(params);
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
					String compName = "ASI_" + editor.getLabel().getValue().replaceAll("(?i)[^a-z0-9-_/]", "_");
					compName = compName.replaceAll("/", "");

					if (displayType == DisplayType.Number)
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

		if (breadcumbContent.getParentURL() != null)
		{
			lblShowBreadCrumb = new Label();
			lblShowBreadCrumb.setValue(" > ");
			breadRow.appendChild(new Space());
			breadRow.appendChild(lblShowBreadCrumb);
		}

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

	private void addRootBreadCrumb()
	{
		BreadCrumbLink rootBreadCrumbLink = new BreadCrumbLink();

		rootBreadCrumbLink.setLabel("/");
		rootBreadCrumbLink.setPathId(String.valueOf(0));
		rootBreadCrumbLink.addEventListener(Events.ON_CLICK, this);
		rootBreadCrumbLink.setStyle("font-weight: bold; font-size: small; padding-left: 15px; color: dimgray;");

		breadRow.appendChild(rootBreadCrumbLink);
		lblShowBreadCrumb = new Label();
		lblShowBreadCrumb.setValue(" > ");
		breadRow.appendChild(new Space());
		breadRow.appendChild(lblShowBreadCrumb);

		breadRows.appendChild(breadRow);
		gridBreadCrumb.appendChild(breadRows);
	}
}