package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
import org.idempiere.componenet.DMSViewerComponent;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.dms.storage.RelationalContentManager;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;
import org.zkoss.image.AImage;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Menuitem;

public class WDMSPanel extends Panel implements EventListener<Event>
{

	private static final long				serialVersionUID		= -6813481516566180243L;
	public static CLogger					log						= CLogger.getCLogger(WDMSPanel.class);

	private static final String				SQL_FETCH_DMS_CONTENTS	= "SELECT * FROM DMS_Document_Explorer_V "
																			+ "WHERE COALESCE(DMS_Content_Related_ID,0) = COALESCE(?,0) "
																			+ "ORDER BY DMS_Content_ID";
	// private CustomForm form = new CustomForm();
	public Tabbox							tabBox					= new Tabbox();
	private Tabs							tabs					= new Tabs();
	public Tab								tabView					= new Tab(Msg.getMsg(Env.getCtx(), "Explorer"));
	public Tabpanels						tabPanels				= new Tabpanels();
	public Tabpanel							tabViewPanel			= new Tabpanel();
	private Grid							grid					= GridFactory.newGridLayout();

	// View Result Tab
	private Searchbox						vsearchBox				= new Searchbox();
	private Label							lblAdvanceSearch		= new Label(Msg.translate(Env.getCtx(),
																			"Advance Search"));
	private Label							lblDocumentName			= new Label(Msg.translate(Env.getCtx(), "Name"));
	private Label							lblCategory				= new Label(Msg.translate(Env.getCtx(), "Category"));
	private Label							lblCreated				= new Label(Msg.translate(Env.getCtx(), "Created"));
	private Label							lblUpdated				= new Label(Msg.translate(Env.getCtx(), "Updated"));
	private Label							lblContentMeta			= new Label(Msg.translate(Env.getCtx(),
																			"Content Meta"));
	private Label							lblReportDate			= new Label(Msg.translate(Env.getCtx(),
																			"Report Date"));
	private Label							lblBPartner				= new Label(Msg.translate(Env.getCtx(),
																			"C_BPartner_ID"));

	private Datebox							dbCreatedTo				= new Datebox();
	private Datebox							dbCreatedFrom			= new Datebox();
	private Datebox							dbUpdated				= new Datebox();
	private Datebox							dbUpdatedFrom			= new Datebox();
	private Datebox							dbReportTo				= new Datebox();
	private Datebox							dbReportFrom			= new Datebox();

	private ConfirmPanel					confirmPanel			= new ConfirmPanel();

	private Button							btnClear				= confirmPanel.createButton(ConfirmPanel.A_RESET);
	private Button							btnSearch				= confirmPanel.createButton(ConfirmPanel.A_REFRESH);
	private Button							btnCloseTab				= confirmPanel.createButton(ConfirmPanel.A_CANCEL);

	private Textbox							txtDocumentName			= new Textbox();
	private Listbox							lstboxCategory			= new Listbox();

	private WSearchEditor					seBPartnerField			= null;

	// create Directory
	private Button							btnCreateDir			= new Button();
	private Button							btnUploadContent		= new Button();
	private Button							btnBack					= new Button();
	private Button							btnNext					= new Button();

	private Label							lblPositionInfo			= new Label();

	private MDMSContent						currDMSContent			= null;
	private MDMSContent						prevDMSContent			= null;
	private MDMSContent						nextDMSContent			= null;

	private ArrayList<DMSViewerComponent>	viewerComponents		= null;

	public IFileStorageProvider				fileStorageProvider		= null;
	public IThumbnailProvider				thumbnailProvider		= null;
	public IContentManager					contentManager			= null;

	private WUploadContent					uploadContent			= null;
	private CreateDirectoryForm				createDirectoryForm		= null;

	public static final int					COMPONENT_HEIGHT		= 150;
	public static final int					COMPONENT_WIDTH			= 150;

	public WDMSPanel()
	{
		fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()));

		if (fileStorageProvider == null)
			throw new AdempiereException("Storage provider is not found.");

		thumbnailProvider = Utils.getThumbnailProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thumbnailProvider == null)
			throw new AdempiereException("Thumbnail provider is not found.");

		contentManager = Utils.getContentManager(RelationalContentManager.KEY);

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		try
		{
			initForm();
			renderViewer();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render Component Problem.");
			throw new AdempiereException("Render Component Problem: " + e);
		}
	}

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
		gridView.setStyle("position:relative; float: right; overflow: auto;");
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
		dbUpdated.setStyle("width: 100%; display:flex; flex-direction: row; flex-wrap: wrap;");
		hbox.appendChild(dbUpdatedFrom);
		hbox.appendChild(dbUpdated);

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
		btnSearch.addEventListener(Events.ON_CLICK, this);

		hbox.appendChild(btnClear);
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
			backNavigation();
		}
		else if (event.getTarget().equals(btnNext))
		{
			directoryNavigation();
		}
		else if (event.getTarget().getId().equals(confirmPanel.A_RESET))
		{
			clearComponenets();
		}
		else if (event.getTarget().getId().equals(confirmPanel.A_REFRESH))
		{
			renderViewer();
		}
		else if (Events.ON_CLICK.equals(event.getName())
				&& event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			currentCompSelection(DMSViewerComp);
		}
		else if (Events.ON_RIGHT_CLICK.equals(event.getName())
				&& event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerCom = (DMSViewerComponent) event.getTarget();
			openMenuPopup(DMSViewerCom);
		}
	}

	public void renderViewer() throws IOException, URISyntaxException
	{
		byte[] imgByteData = null;
		File thumbFile = null;

		Components.removeAllChildren(grid);
		Rows rows = new Rows();
		Row row = new Row();
		Cell cell = null;
		MImage mImage = null;
		AImage image = null;

		int i = 0;

		List<I_DMS_Content> dmsContent = getDMSContents();

		viewerComponents = new ArrayList<DMSViewerComponent>();

		for (i = 0; i < dmsContent.size(); i++)
		{
			thumbFile = thumbnailProvider.getFile(dmsContent.get(i), "150");
			if (thumbFile == null)
			{
				if (dmsContent.get(i).getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
				{
					mImage = Utils.getDirThumbnail();
				}
				else
				{
					mImage = Utils.getMimetypeThumbnail(dmsContent.get(i).getDMS_MimeType_ID());
				}
				imgByteData = mImage.getData();
				if (imgByteData != null)
					image = new AImage(dmsContent.get(i).getName(), imgByteData);
			}
			else
			{
				image = new AImage(thumbFile);
			}

			DMSViewerComponent viewerComponent = new DMSViewerComponent(dmsContent.get(i), image);

			viewerComponent.addEventListener(Events.ON_DOUBLE_CLICK, this);
			viewerComponent.addEventListener(Events.ON_CLICK, this);
			viewerComponent.addEventListener(Events.ON_RIGHT_CLICK, this);

			viewerComponents.add(viewerComponent);

			grid.setSizedByContent(true);
			grid.setZclass("none");
			viewerComponent.setDheight(COMPONENT_HEIGHT);
			viewerComponent.setDwidth(COMPONENT_WIDTH);

			cell = new Cell();
			cell.setWidth(row.getWidth());
			cell.appendChild(viewerComponent);
			// flex: 1 0 150px;
			// justify-content:space-around;
			row.setStyle("display:flex; flex-direction: row; flex-wrap: wrap;");
			row.setZclass("none");
			row.appendCellChild(cell);
			rows.appendChild(row);
			row.appendChild(viewerComponent);
		}

		grid.appendChild(rows);
		tabBox.setSelectedIndex(0);
	}

	private List<I_DMS_Content> getDMSContents()
	{
		List<I_DMS_Content> dmsContent = new ArrayList<I_DMS_Content>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(SQL_FETCH_DMS_CONTENTS, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE, null);

			if (currDMSContent == null)
			{
				pstmt.setInt(1, 0);
			}
			else
			{
				pstmt.setInt(1, currDMSContent.getDMS_Content_ID());
			}

			rs = pstmt.executeQuery();

			if (rs != null)
			{
				while (rs.next())
				{
					dmsContent.add(new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null));
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Root content fetching failure: ", e);
			throw new AdempiereException("Root content fetching failure: " + e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return dmsContent;
	}

	private void clearComponenets()
	{
		vsearchBox.setText(null);
		txtDocumentName.setValue(null);
		lstboxCategory.setValue(null);
		dbCreatedFrom.setValue(null);
		dbCreatedTo.setValue(null);
		dbUpdatedFrom.setValue(null);
		dbUpdated.setValue(null);
		dbReportFrom.setValue(null);
		dbReportTo.setValue(null);
		seBPartnerField.setValue(null);
	}

	private void openDirectoryORContent(DMSViewerComponent DMSViewerComp) throws IOException, URISyntaxException
	{
		currDMSContent = DMSViewerComp.getDMSContent();
		if (currDMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
		{
			renderViewer();
			lblPositionInfo.setValue(currDMSContent.getName());
			btnBack.setEnabled(true);
			btnNext.setEnabled(false);
			prevDMSContent = currDMSContent;
		}
		else if (currDMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Content))
		{
			File documentToPreview = null;

			documentToPreview = fileStorageProvider.getFile(contentManager.getPath(currDMSContent));

			if (documentToPreview != null)
			{
				if (Utils.accept(documentToPreview))
				{
					Tab tabData = new Tab(currDMSContent.getName());
					tabData.setClosable(true);
					tabs.appendChild(tabData);
					tabBox.setSelectedTab(tabData);
					Tabpanel tabDataPanel = new Tabpanel();
					new WDocumentViewer(this, documentToPreview, tabDataPanel, currDMSContent);
					currDMSContent = prevDMSContent;
				}
				else
				{
					AMedia media = new AMedia(documentToPreview, "application/octet-stream", null);
					Filedownload.save(media);
					currDMSContent = prevDMSContent;
				}
			}
			else
			{
				FDialog.error(0, currDMSContent.getName() + " Document not found");
				log.log(Level.SEVERE, currDMSContent.getName() + " Document not found");
			}
		}
	}

	private void backNavigation() throws IOException, URISyntaxException
	{
		nextDMSContent = currDMSContent;
		int DMS_Content_ID = DB.getSQLValue(null,
				"SELECT DMS_Content_Related_ID FROM DMS_Association WHERE DMS_Content_ID = ?",
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
		renderViewer();
		btnNext.setEnabled(true);
	}

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

	private void createDirectory()
	{
		createDirectoryForm = new CreateDirectoryForm(currDMSContent);

		createDirectoryForm.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception
			{
				renderViewer();
			}
		});
	}

	private void uploadContent()
	{
		uploadContent = new WUploadContent(prevDMSContent, false);

		uploadContent.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception
			{
				renderViewer();
			}
		});
		uploadContent.addEventListener(Events.ON_CLOSE, this);
	}

	private void openMenuPopup(final DMSViewerComponent DMSViewerCom)
	{
		currDMSContent = DMSViewerCom.getDMSContent();

		Menupopup popup = new Menupopup();
		popup.setPage(DMSViewerCom.getPage());

		Menuitem versionList = new Menuitem("Version List");
		Menuitem copy = new Menuitem("Copy");
		Menuitem paste = new Menuitem("Paste");
		Menuitem createLink = new Menuitem("Create Link");

		if (X_DMS_Content.CONTENTBASETYPE_Content.equals(DMSViewerCom.getContentBaseType()))
		{
			versionList.addEventListener(Events.ON_CLICK, new EventListener<Event>() {

				@Override
				public void onEvent(Event e) throws Exception
				{
					WDMSVersion DMSVersion = new WDMSVersion(DMSViewerCom.getDMSContent());
				}
			});
			popup.appendChild(versionList);

		}
		else if (X_DMS_Content.CONTENTBASETYPE_Directory.equals(DMSViewerCom.getContentBaseType()))
		{

		}
		popup.appendChild(copy);
		popup.appendChild(paste);
		popup.appendChild(createLink);
		DMSViewerCom.setContext(popup);
	}

	private void currentCompSelection(DMSViewerComponent DMSViewerComp)
	{
		currDMSContent = DMSViewerComp.getDMSContent();
		for (DMSViewerComponent thumbComponent : viewerComponents)
		{
			if (thumbComponent.getDMSContent().getDMS_Content_ID() == DMSViewerComp.getDMSContent().getDMS_Content_ID())
			{
				ZkCssHelper.appendStyle(DMSViewerComp.getfLabel(),
						"background-color:#99cbff; box-shadow: 7px 7px 7px #888888");
			}
			else
			{
				ZkCssHelper.appendStyle(DMSViewerComp.getfLabel(),
						"background-color:#ffffff; box-shadow: 7px 7px 7px #ffffff");
			}
		}
	}
}
