package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.idempiere.componenet.ImgTextComponent;
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

public class WDocumentViewer extends Panel implements EventListener<Event>
{

	private static final long	serialVersionUID	= -6813481516566180243L;
	public static CLogger		log					= CLogger.getCLogger(WDocumentViewer.class);

	// private CustomForm form = new CustomForm();
	public Tabbox				tabBox				= new Tabbox();
	private Tabs				tabs				= new Tabs();
	public Tab					tabView				= new Tab(Msg.getMsg(Env.getCtx(), "ViewerResult"));
	public Tabpanels			tabPanels			= new Tabpanels();
	public Tabpanel				tabViewPanel		= new Tabpanel();
	private Grid				grid				= GridFactory.newGridLayout();

	// View Result Tab
	private Searchbox			vsearchBox			= new Searchbox();
	private Label				lblAdvanceSearch	= new Label(Msg.translate(Env.getCtx(), "Advance Search"));
	private Label				lblDocumentName		= new Label(Msg.translate(Env.getCtx(), "Name"));
	private Label				lblCategory			= new Label(Msg.translate(Env.getCtx(), "Category"));
	private Label				lblCreated			= new Label(Msg.translate(Env.getCtx(), "Created"));
	private Label				lblUpdated			= new Label(Msg.translate(Env.getCtx(), "Updated"));
	private Label				lblContentMeta		= new Label(Msg.translate(Env.getCtx(), "Content Meta"));
	private Label				lblReportDate		= new Label(Msg.translate(Env.getCtx(), "Report Date"));
	private Label				lblBPartner			= new Label(Msg.translate(Env.getCtx(), "C_BPartner_ID"));

	private Datebox				dbCreatedTo			= new Datebox();
	private Datebox				dbCreatedFrom		= new Datebox();
	private Datebox				dbUpdated			= new Datebox();
	private Datebox				dbUpdatedFrom		= new Datebox();
	private Datebox				dbReportTo			= new Datebox();
	private Datebox				dbReportFrom		= new Datebox();

	private ConfirmPanel		confirmPanel		= new ConfirmPanel();

	private Button				clearButton			= confirmPanel.createButton(ConfirmPanel.A_RESET);
	private Button				searchButton		= confirmPanel.createButton(ConfirmPanel.A_REFRESH);
	private Button				closetabButton		= confirmPanel.createButton(ConfirmPanel.A_CANCEL);

	private Textbox				txtDocumentName		= new Textbox();
	private Listbox				lstboxCategory		= new Listbox();

	private WSearchEditor		seBPartnerField		= null;

	// create Directory
	private Button				createDirButton		= new Button();
	private Button				uploadContentButton	= new Button();
	private Button				backButton			= new Button();
	private Button				nextButton			= new Button();

	private Label				positionInfo		= new Label();

	public static MDMSContent	currentDMSContent;
	public static MDMSContent	previousDmsContent;
	public static MDMSContent	nextDmsContent;

	public static final String	SQL_GET_IMAGE_ID	= "SELECT AD_Image_ID FROM AD_Image Where name ilike ? ";
	public static final String	IMAGE_DOWNLOAD		= "Download";
	public static final String	IMAGE_DIRECTORY		= "Directory";

	private ImgTextComponent	cstmComponenet		= null;

	public IFileStorageProvider	fileStorageProvider	= null;
	public IThumbnailProvider	thubnailProvider	= null;
	public IContentManager		contentManager		= null;

	private WUploadContent		uploadContent		= null;
	private CreateDirectoryForm	createDirectoryForm	= null;

	private I_DMS_Content[]		dmsContent			= null;

	public WDocumentViewer()
	{
		currentDMSContent = null;
		previousDmsContent = null;
		nextDmsContent = null;

		fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()));

		if (fileStorageProvider == null)
			throw new AdempiereException("Storage provider is not found.");

		thubnailProvider = Utils.getThumbnailProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thubnailProvider == null)
			throw new AdempiereException("Thumbnail provider is not found.");

		contentManager = Utils.getContentManager(RelationalContentManager.KEY);

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		initForm();
	}

	protected void initForm()
	{
		try
		{
			jbInit();
			dynInit();
			renderViewer(currentDMSContent);

			backButton.setEnabled(false);
			nextButton.setEnabled(false);
		}
		catch (IllegalArgumentException e)
		{
			log.log(Level.SEVERE, "Thumbnai not found for directory or document.");
			throw new AdempiereException("Thumbnai not found for directory or document: " + e.getLocalizedMessage());
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render Componenet Problem");
			throw new AdempiereException("Render Componenet Problem " + e.getLocalizedMessage());
		}
		SessionManager.getAppDesktop();
		// setMode(Mode.EMBEDDED);
	}

	private void dynInit()
	{

	}

	private void jbInit() throws Exception
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

		backButton.setImage(ThemeManager.getThemeResource("images/wfBack24.png"));
		backButton.setTooltiptext("Previous Record");

		positionInfo.setHflex("1");
		ZkCssHelper.appendStyle(positionInfo, "font-weight: bold;");
		ZkCssHelper.appendStyle(positionInfo, "align: center;");

		nextButton.setImage(ThemeManager.getThemeResource("images/wfNext24.png"));
		nextButton.setTooltiptext("Next Record");
		backButton.addEventListener(Events.ON_CLICK, this);
		nextButton.addEventListener(Events.ON_CLICK, this);
		nextButton.setStyle("float:right;");

		row.appendChild(backButton);
		row.appendChild(positionInfo);
		row.appendChild(nextButton);

		row = new Row();
		rows.appendChild(row);

		row.appendChild(createDirButton);
		row.appendChild(uploadContentButton);

		createDirButton.setImage(ThemeManager.getThemeResource("images/Folder24.png"));
		createDirButton.setTooltiptext("Create Directory");
		createDirButton.addEventListener(Events.ON_CLICK, this);

		uploadContentButton.setImage(ThemeManager.getThemeResource("images/Parent24.png"));
		uploadContentButton.setTooltiptext("Upload Content");
		uploadContentButton.addEventListener(Events.ON_CLICK, this);

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

		clearButton.addEventListener(Events.ON_CLICK, this);
		hbox.appendChild(clearButton);
		hbox.appendChild(searchButton);
		hbox.appendChild(closetabButton);

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
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		log.info(event.getName());

		if (Events.ON_DOUBLE_CLICK.equals(event.getName()) && event.getTarget().getClass() == ImgTextComponent.class)
		{
			if (currentDMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
			{
				renderViewer(currentDMSContent);
			}
			else if (currentDMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Content))
			{
				File documentToPreview = null;

				documentToPreview = fileStorageProvider.getFile(contentManager.getPath(currentDMSContent));

				if (documentToPreview != null)
				{
					if (Utils.accept(documentToPreview))
					{
						Tab tabData = new Tab(currentDMSContent.getName());
						tabData.setClosable(true);
						tabs.appendChild(tabData);
						tabBox.setSelectedTab(tabData);
						Tabpanel tabDataPanel = new Tabpanel();
						new WDocumentEditor(this, documentToPreview, tabDataPanel, currentDMSContent);
						currentDMSContent = previousDmsContent;
					}
					else
					{
						AMedia media = new AMedia(documentToPreview, "application/octet-stream", null);
						Filedownload.save(media);
						currentDMSContent = previousDmsContent;
					}
				}
				else
				{
					FDialog.error(0, currentDMSContent.getName() + " Document not found");
					log.log(Level.SEVERE, currentDMSContent.getName() + " Document not found");
				}
			}
		}
		else if (event.getTarget().equals(createDirButton))
		{
			createDirectoryForm = new CreateDirectoryForm();

			createDirectoryForm.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

				@Override
				public void onEvent(Event event) throws Exception
				{
					renderViewer(currentDMSContent);
				}
			});
		}
		else if (event.getTarget().equals(uploadContentButton))
		{
			uploadContent = new WUploadContent();

			uploadContent.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

				@Override
				public void onEvent(Event event) throws Exception
				{
					renderViewer(currentDMSContent);
				}
			});
			uploadContent.addEventListener(Events.ON_CLOSE, this);
		}
		else if (event.getTarget().equals(backButton))
		{
			nextDmsContent = currentDMSContent;
			int DMS_Content_ID = DB.getSQLValue(null,
					"SELECT DMS_Content_Related_ID FROM DMS_Association WHERE DMS_Content_ID = ?",
					currentDMSContent.getDMS_Content_ID());

			if (DMS_Content_ID == -1)
				backButton.setEnabled(false);
			else
			{
				currentDMSContent = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
				renderViewer(currentDMSContent);
				nextButton.setEnabled(true);
				if (currentDMSContent.getParentURL() == null)
					backButton.setEnabled(false);
			}
		}
		else if (event.getTarget().equals(nextButton))
		{
			if (nextDmsContent != null)
			{
				currentDMSContent = nextDmsContent;
				renderViewer(currentDMSContent);
			}
			nextButton.setEnabled(false);
		}
		else if (event.getTarget().getId().equals(confirmPanel.A_RESET))
		{
			clearComponenets();
		}
	}

	public void renderViewer(I_DMS_Content DMS_Content) throws IOException, URISyntaxException
	{
		byte[] imgByteData = null;
		File thumbFile = null;

		Components.removeAllChildren(grid);
		// tabs.appendChild(tabView);
		Rows rows = new Rows();
		Row row = new Row();
		Cell cell = null;
		MImage mImage = null;
		AImage image = null;

		int i = 0;
		int size = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sqlFetchRootContent = null;

		if (DMS_Content == null)
			sqlFetchRootContent = "SELECT * FROM DMS_Content WHERE parentUrl IS NULL";
		else
			sqlFetchRootContent = "SELECT * FROM DMS_Association WHERE DMS_Content_Related_ID = "
					+ DMS_Content.getDMS_Content_ID();

		try
		{
			pstmt = DB.prepareStatement(sqlFetchRootContent, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE, null);
			rs = pstmt.executeQuery();

			if (rs != null)
			{
				rs.beforeFirst();
				rs.last();
				size = rs.getRow();
				rs.beforeFirst();
			}

			dmsContent = new I_DMS_Content[size];

			while (rs.next())
			{
				dmsContent[i++] = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Root content fetching failure: ", e.getLocalizedMessage());
			throw new AdempiereException("Root content fetching failure: " + e.getLocalizedMessage());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		for (i = 0; i < dmsContent.length; i++)
		{
			thumbFile = thubnailProvider.getFile(dmsContent[i], "150");
			if (thumbFile == null)
			{
				if (dmsContent[i].getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
				{
					mImage = Utils.getDirThumbnail();
				}
				else
				{
					mImage = Utils.getMimetypeThumbnail(dmsContent[i].getDMS_MimeType_ID());
				}
				imgByteData = mImage.getData();
				if (imgByteData != null)
					image = new AImage(dmsContent[i].getName(), imgByteData);
			}
			else
			{
				image = new AImage(thumbFile);
			}

			cstmComponenet = new ImgTextComponent(dmsContent[i], image);
			cstmComponenet.addEventListener(Events.ON_DOUBLE_CLICK, this);
			cstmComponenet.addEventListener(Events.ON_CLICK, this);
			cstmComponenet.addEventListener(Events.ON_RIGHT_CLICK, this);

			grid.setSizedByContent(true);
			grid.setZclass("none");
			cstmComponenet.setDheight(130);
			cstmComponenet.setDwidth(130);

			cell = new Cell();
			cell.setWidth(row.getWidth());
			cell.appendChild(cstmComponenet);
			// flex: 1 0 150px;
			// justify-content:space-around;
			row.setStyle("display:flex; flex-direction: row; flex-wrap: wrap;");
			row.setZclass("none");
			row.appendCellChild(cell);
			rows.appendChild(row);
			row.appendChild(cstmComponenet);

		}

		grid.appendChild(rows);
		tabBox.setSelectedIndex(0);
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

}
