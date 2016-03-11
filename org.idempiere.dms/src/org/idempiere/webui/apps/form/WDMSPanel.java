package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Datebox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.Menupopup;
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
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MImage;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.componenet.DMSViewerComponent;
import org.idempiere.dms.factories.DMSClipboard;
import org.idempiere.dms.factories.IContentManager;
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
import org.zkoss.image.AImage;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Menuitem;

public class WDMSPanel extends Panel implements EventListener<Event>
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

	private static String					SQL_FETCH_SEARCH_CONTENT	= " SELECT Distinct * FROM ( "
																				+ " (WITH ContentRelated AS ( "
																				+ " 	SELECT	c.DMS_Content_ID, c.Name, c.Created, c.Updated, c.Description, c.ContentBaseType, a.DMS_Content_Related_ID, a.AD_Table_ID, a.Record_ID, a.DMS_Association_ID  "
																				+ " 	FROM DMS_Association a  "
																				+ " 	INNER JOIN DMS_Content c ON (c.DMS_Content_ID = a.DMS_Content_ID)  "
																				+ " 	WHERE c.ContentBaseType <> 'DIR' AND a.dms_associationtype_id IN  "
																				+ " 		(SELECT dms_associationtype.dms_associationtype_id FROM dms_associationtype WHERE dms_associationtype.name::text <> 'Link'::text) "
																				+ " ), "
																				+ " LatestCreated AS ( "
																				+ " 	SELECT a.DMS_Content_Related_ID, MAX(a.seqNo) AS maxSeqNo, cr.Created, cr.Updated, cr.Name, cr.Description, cr.AD_Table_ID, cr.Record_ID, cr.DMS_Association_ID "
																				+ " 	FROM ContentRelated cr "
																				+ " 	INNER JOIN DMS_Association a ON (a.DMS_Content_Related_ID = cr.DMS_Content_Related_ID) "
																				+ " 	INNER JOIN DMS_Content c ON (c.DMS_Content_ID = a.DMS_Content_ID) "
																				+ " 	GROUP BY a.DMS_Content_Related_ID, cr.Created, cr.Updated, cr.Name, cr.Description, cr.AD_Table_ID, cr.Record_ID, cr.DMS_Association_ID "
																				+ " ) "
																				+ "  "
																				+ " SELECT c.DMS_Content_ID, a.DMS_Content_Related_ID, lc.Created, lc.Updated, lc.Name, lc.Description, lc.AD_Table_ID, lc.Record_ID, lc.DMS_Association_ID  "
																				+ " FROM LatestCreated lc "
																				+ " INNER JOIN DMS_Association a	ON (a.DMS_Content_Related_ID = lc.DMS_Content_Related_ID AND a.seqNo = lc.maxSeqNo) "
																				+ " INNER JOIN DMS_Content c	ON (c.DMS_Content_ID = a.DMS_Content_ID AND c.ContentBaseType <> 'DIR') "
																				+ " INNER JOIN DMS_Content cc	ON (cc.DMS_Content_ID = a.DMS_Content_Related_ID) "
																				+ " INNER JOIN DMS_Association aa	ON (aa.DMS_Content_ID = cc.DMS_Content_ID) "
																				+ " ) "
																				+ " UNION ALL "
																				+ " ( "
																				+ " 	SELECT c.DMS_Content_ID, a.DMS_Content_Related_ID, c.Created, c.Updated, c.Name, c.Description, a.AD_Table_ID, a.Record_ID, a.DMS_Association_ID  "
																				+ " 	FROM DMS_Content c "
																				+ " 	INNER JOIN DMS_Association a ON (a.DMS_Content_ID = c.DMS_Content_ID AND c.ContentBaseType <> 'DIR' "
																				+ " 	AND (a.DMS_AssociationType_ID IN (SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE NAME <>'Version' AND NAME <> 'Link') OR a.DMS_AssociationType_ID IS NULL)) "
																				+ " EXCEPT "
																				+ " 	SELECT c.DMS_Content_ID, aa.DMS_Content_Related_ID, c.Created, c.Updated, c.Name, c.Description, a.AD_Table_ID, a.Record_ID, a.DMS_Association_ID "
																				+ " 	FROM DMS_Content c "
																				+ " 	INNER JOIN DMS_Association a ON (a.DMS_Content_Related_ID = c.DMS_Content_ID "
																				+ " 		AND a.DMS_AssociationType_ID = (SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE NAME='Version')) "
																				+ " 	INNER JOIN DMS_Content cc	ON (cc.DMS_Content_ID = a.DMS_Content_Related_ID) "
																				+ " 	INNER JOIN DMS_Association aa	ON (aa.DMS_Content_ID = cc.DMS_Content_ID) WHERE c.ContentBaseType <> 'DIR' "
																				+ " 	GROUP BY c.DMS_Content_ID, aa.DMS_Content_Related_ID, a.AD_Table_ID, a.Record_ID, a.DMS_Association_ID "
																				+ " ) " + " ) AS searchRecord ";
	// +
	// " WHERE COALESCE(AD_Table_ID,0) = COALESCE(null,0) AND COALESCE(Record_ID,0) = COALESCE(null,0) ";

	// private CustomForm form = new CustomForm();
	public Tabbox							tabBox						= new Tabbox();
	private Tabs							tabs						= new Tabs();
	public Tab								tabView						= new Tab(Msg.getMsg(Env.getCtx(), "Explorer"));
	public Tabpanels						tabPanels					= new Tabpanels();
	public Tabpanel							tabViewPanel				= new Tabpanel();
	private Grid							grid						= GridFactory.newGridLayout();

	// View Result Tab
	private Searchbox						vsearchBox					= new Searchbox();
	private Label							lblAdvanceSearch			= new Label(Msg.translate(Env.getCtx(),
																				"Advance Search"));
	private Label							lblDocumentName				= new Label(Msg.translate(Env.getCtx(), "Name"));
	private Label							lblCategory					= new Label(Msg.translate(Env.getCtx(),
																				"Category"));
	private Label							lblCreated					= new Label(Msg.translate(Env.getCtx(),
																				"Created"));
	private Label							lblUpdated					= new Label(Msg.translate(Env.getCtx(),
																				"Updated"));
	private Label							lblContentMeta				= new Label(Msg.translate(Env.getCtx(),
																				"Content Meta"));
	private Label							lblReportDate				= new Label(Msg.translate(Env.getCtx(),
																				"Report Date"));
	private Label							lblBPartner					= new Label(Msg.translate(Env.getCtx(),
																				"C_BPartner_ID"));

	private Label							lblDescription				= new Label(Msg.translate(Env.getCtx(),
																				"Description"));

	private Datebox							dbCreatedTo					= new Datebox();
	private Datebox							dbCreatedFrom				= new Datebox();
	private Datebox							dbUpdatedTo					= new Datebox();
	private Datebox							dbUpdatedFrom				= new Datebox();
	private Datebox							dbReportTo					= new Datebox();
	private Datebox							dbReportFrom				= new Datebox();

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

	private Listbox							lstboxCategory				= new Listbox();

	private WSearchEditor					seBPartnerField				= null;

	// create Directory
	private Button							btnCreateDir				= new Button();
	private Button							btnUploadContent			= new Button();
	private Button							btnBack						= new Button();
	private Button							btnNext						= new Button();

	private Label							lblPositionInfo				= new Label();

	private MDMSContent						currDMSContent				= null;
	private MDMSContent						prevDMSContent				= null;
	private MDMSContent						nextDMSContent				= null;

	private ArrayList<DMSViewerComponent>	viewerComponents			= null;

	public IFileStorageProvider				fileStorageProvider			= null;
	public IThumbnailProvider				thumbnailProvider			= null;
	public IContentManager					contentManager				= null;
	public IFileStorageProvider				thumbnailStorageProvider	= null;

	private Menupopup						contentContextMenu			= new Menupopup();
	private Menupopup						canvasContextMenu			= new Menupopup();

	private Menuitem						versionList					= null;
	private Menuitem						copy						= null;
	private Menuitem						paste						= null;
	private Menuitem						delete						= null;
	private Menuitem						associate					= null;

	private Menuitem						canvasPaste					= null;

	private DMSViewerComponent				DMSViewerComp				= null;
	private MDMSContent						copyDMSContent				= null;
	private MDMSContent						dirContent					= null;

	private WUploadContent					uploadContent				= null;
	private CreateDirectoryForm				createDirectoryForm			= null;

	public int								recordID					= 0;
	public int								tableID						= 0;

	private boolean							isSearch					= false;

	private static final int				COMPONENT_HEIGHT			= 150;
	private static final int				COMPONENT_WIDTH				= 150;

	private DMSViewerComponent				prevComponent				= null;

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
		grid.setStyle("width: 69%; height:100%; position:relative; overflow: auto;");
		// View Result Tab

		Grid gridView = GridFactory.newGridLayout();
		gridView.setStyle("height:100%; position:relative; float: right; overflow: auto;");
		gridView.setWidth("30%");
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
		txtDocumentName.setWidth("100%");

		row = new Row();
		rows.appendChild(row);
		row.setAlign("right");
		row.appendChild(lblCategory);
		lblCategory.setStyle("float: left;");
		Cell categoryListCell = new Cell();
		categoryListCell.setColspan(2);
		lstboxCategory.setMold("select");
		categoryListCell.appendChild(lstboxCategory);
		lstboxCategory.setWidth("100%");
		row.appendChild(categoryListCell);

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

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblContentMeta);
		ZkCssHelper.appendStyle(lblContentMeta, "font-weight: bold;");

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblReportDate);
		hbox = new Hbox();
		dbReportFrom.setStyle("width: 100%; display:flex; flex-direction: row; flex-wrap: wrap;");
		dbReportTo.setStyle("width: 100%; display:flex; flex-direction: row; flex-wrap: wrap;");
		hbox.appendChild(dbReportFrom);
		hbox.appendChild(dbReportTo);

		Cell reportingCell = new Cell();
		reportingCell.setColspan(2);
		reportingCell.appendChild(hbox);
		row.appendChild(reportingCell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblBPartner);

		MLookup lookup = MLookupFactory.get(Env.getCtx(), 0, 0, 2762, DisplayType.Search);
		seBPartnerField = new WSearchEditor(lookup, Msg.translate(Env.getCtx(), "C_BPartner_ID"), "", true, false, true);
		Cell bpartnercell = new Cell();
		bpartnercell.setColspan(2);
		bpartnercell.appendChild(seBPartnerField.getComponent());
		row.appendChild(bpartnercell);

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

		Cell cell = new Cell();
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
		cell.appendChild(grid);
		cell.addEventListener(Events.ON_RIGHT_CLICK, this);
		boxViewSeparator.appendChild(cell);

		cell = new Cell();
		cell.setWidth("100%");
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

		versionList = new Menuitem("Version List");
		copy = new Menuitem("Copy");
		paste = new Menuitem("Paste");
		delete = new Menuitem("Delete");
		associate = new Menuitem("Associate");

		canvasPaste = new Menuitem("Paste");
		canvasContextMenu.appendChild(canvasPaste);
		canvasPaste.addEventListener(Events.ON_CLICK, this);

		contentContextMenu.appendChild(versionList);
		contentContextMenu.appendChild(copy);
		contentContextMenu.appendChild(paste);
		contentContextMenu.appendChild(delete);
		contentContextMenu.appendChild(associate);

		versionList.setImage(ThemeManager.getThemeResource("images/Wizard24.png"));
		copy.setImage(ThemeManager.getThemeResource("images/Copy16.png"));
		delete.setImage(ThemeManager.getThemeResource("images/Delete24.png"));
		associate.setImage(ThemeManager.getThemeResource("images/Attachment24.png"));

		versionList.addEventListener(Events.ON_CLICK, this);
		paste.addEventListener(Events.ON_CLICK, this);
		copy.addEventListener(Events.ON_CLICK, this);
		delete.addEventListener(Events.ON_CLICK, this);
		associate.addEventListener(Events.ON_CLICK, this);

		delete.setDisabled(true);

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
			isSearch = false;
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
			renderViewer();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_REFRESH))
		{
			renderViewer();
		}
		else if (event.getTarget().equals(btnSearch))
		{
			isSearch = true;
			renderViewer();
			prevDMSContent = currDMSContent;
			btnBack.setEnabled(true);
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
		else if (event.getTarget().equals(copy))
		{
			DMSClipboard.put(DMSViewerComp.getDMSContent());
		}
		else if (event.getTarget().equals(paste))
		{
			pasteDocument(dirContent, true);
		}
		else if (event.getTarget().equals(delete))
		{

		}
		else if (event.getTarget().equals(associate))
		{
			new WDAssociationType(copyDMSContent, DMSViewerComp.getDMSContent());
		}
		else if (event.getTarget().equals(canvasPaste))
		{
			pasteDocument(currDMSContent, false);
		}
	}

	/**
	 * @throws IOException
	 * @throws URISyntaxException Render the Thumb Component
	 */
	public void renderViewer() throws IOException, URISyntaxException
	{
		byte[] imgByteData = null;
		File thumbFile = null;

		Components.removeAllChildren(grid);
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
		lstboxCategory.setValue(null);
		dbCreatedFrom.setValue(null);
		dbCreatedTo.setValue(null);
		dbUpdatedFrom.setValue(null);
		dbUpdatedTo.setValue(null);
		dbReportFrom.setValue(null);
		dbReportTo.setValue(null);
		seBPartnerField.setValue(null);
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
			associate.setDisabled(true);
			versionList.setDisabled(true);
		}
		else if (copyDMSContent == DMSViewerCom.getDMSContent())
		{
			associate.setDisabled(true);
			paste.setDisabled(true);
		}
		else if (X_DMS_Content.CONTENTBASETYPE_Directory.equals(DMSViewerCom.getDMSContent().getContentBaseType()))
		{
			associate.setDisabled(true);
			versionList.setDisabled(true);
		}
		else
		{
			paste.setDisabled(false);
			associate.setDisabled(false);
		}

		if (X_DMS_Content.CONTENTBASETYPE_Content.equals(DMSViewerCom.getContentBaseType()))
		{
			versionList.setDisabled(false);
			paste.setDisabled(true);
			
			if (copyDMSContent != null && copyDMSContent != DMSViewerCom.getDMSContent())
				associate.setDisabled(false);
			else
				associate.setDisabled(true);
		}

		if (X_DMS_Content.CONTENTBASETYPE_Directory.equals(DMSViewerCom.getContentBaseType()))
		{
			if (copyDMSContent != null)
				paste.setDisabled(false);
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

		canvasContextMenu.setPage(cell.getPage());
		cell.setContext(canvasContextMenu);

		if (DMSClipboard.get() == null)
		{
			canvasPaste.setDisabled(true);
		}
		else
		{
			canvasPaste.setDisabled(false);
		}
	}

	private void pasteDocument(MDMSContent DMSContent, boolean isDir)
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

	private HashMap<I_DMS_Content, I_DMS_Association> renderSearchedContent()
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		StringBuilder query = new StringBuilder();
		query.append(" WHERE ");

		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		if (recordID > 0 && tableID > 0)
		{
			query.append(" AND ").append(" AD_Table_ID= '" + tableID + "' AND Record_ID = '" + recordID + "'");
		}

		if (!Util.isEmpty(txtDocumentName.getValue()))
		{
			query.append(" AND ").append(" name ilike '" + txtDocumentName.getValue() + "' ");
		}

		if (!Util.isEmpty(txtDescription.getValue()))
		{
			query.append(" AND ").append(" description ilike '%" + txtDescription.getName() + "%' ");
		}

		if (dbCreatedFrom.getValue() != null && dbCreatedTo.getValue() != null)
		{
			if (dbCreatedFrom.getValue().after(dbCreatedTo.getValue()))
				throw new WrongValueException(dbCreatedFrom, "Invalid Date Range");
			else
				query.append(" AND ").append(
						" Created between '" + dbCreatedFrom.getValue() + "' AND '" + dbCreatedTo.getValue() + "' ");
		}
		else if (dbCreatedFrom.getValue() != null && dbCreatedTo.getValue() == null)
		{
			query.append(" AND ").append(" Created >= '" + dbCreatedFrom.getValue() + "'");
		}
		else if (dbCreatedTo.getValue() != null && dbCreatedFrom.getValue() == null)
		{
			query.append(" AND ").append(" Created <= '" + dbCreatedTo.getValue() + "'");
		}

		if (dbUpdatedFrom.getValue() != null && dbCreatedTo.getValue() != null)
		{
			if (dbUpdatedFrom.getValue().after(dbUpdatedTo.getValue()))
				throw new WrongValueException(dbUpdatedFrom, "Invalid Date Range");
			else
				query.append(" AND ")
						.append(" AND Updated between '" + dbUpdatedFrom.getValue() + "' AND '"
								+ dbUpdatedTo.getValue() + "' ");
		}
		else if (dbUpdatedFrom.getValue() != null && dbUpdatedTo.getValue() == null)
		{
			query.append(" AND ").append(" Updated >= '" + dbUpdatedFrom.getValue() + "'");
		}
		else if (dbUpdatedTo.getValue() != null && dbUpdatedFrom.getValue() == null)
		{
			query.append(" AND ").append(" Updated <= '" + dbUpdatedTo.getValue() + "'");
		}

		if (query.length() > 7)
			query.delete(8, 13);
		else
			query.delete(0, 6);

		try
		{
			pstmt = DB.prepareStatement(SQL_FETCH_SEARCH_CONTENT + query, null);

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
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Search failure.", e);
			throw new AdempiereException("Search failure: " + e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return map;

	}
}
