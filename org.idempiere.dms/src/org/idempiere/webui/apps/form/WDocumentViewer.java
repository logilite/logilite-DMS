package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
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
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.model.MImage;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.idempiere.componenet.ImgTextComponent;
import org.idempiere.dms.storage.DmsUtility;
import org.idempiere.model.MDMS_Content;
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

public class WDocumentViewer extends ADForm implements EventListener<Event>
{

	private static final long		serialVersionUID	= -6813481516566180243L;
	public static CLogger			log					= CLogger.getCLogger(WDocumentViewer.class);

	private CustomForm				form				= new CustomForm();
	public Tabbox					tabBox				= new Tabbox();
	private Tabs					tabs				= new Tabs();
	public Tab						tabView				= new Tab(Msg.getMsg(Env.getCtx(), "ViewerResult"));
	public Tabpanels				tabPanels			= new Tabpanels();
	public Tabpanel					tabViewPanel		= new Tabpanel();
	private Grid					grid				= GridFactory.newGridLayout();

	// View Result Tab
	private Searchbox				vsearchBox			= new Searchbox();
	private Label					lblAdvanceSearch	= new Label(Msg.translate(Env.getCtx(), "Advance Search"));
	private Label					lblDocumentName		= new Label(Msg.translate(Env.getCtx(), "Name"));
	private Label					lblCategory			= new Label(Msg.translate(Env.getCtx(), "Category"));
	private Label					lblCreated			= new Label(Msg.translate(Env.getCtx(), "Created"));
	private Label					lblUpdated			= new Label(Msg.translate(Env.getCtx(), "Updated"));
	private Label					lblContentMeta		= new Label(Msg.translate(Env.getCtx(), "Content Meta"));
	private Label					lblReportDate		= new Label(Msg.translate(Env.getCtx(), "Report Date"));
	private Label					lblBPartner			= new Label(Msg.translate(Env.getCtx(), "C_BPartner_ID"));

	private Datebox					dbCreatedTo			= new Datebox();
	private Datebox					dbCreatedFrom		= new Datebox();
	private Datebox					dbUpdated			= new Datebox();
	private Datebox					dbUpdatedFrom		= new Datebox();
	private Datebox					dbReportTo			= new Datebox();
	private Datebox					dbReportFrom		= new Datebox();

	private ConfirmPanel			confirmPanel		= new ConfirmPanel();

	private Button					clearButton			= confirmPanel.createButton(ConfirmPanel.A_RESET);
	private Button					searchButton		= confirmPanel.createButton(ConfirmPanel.A_REFRESH);
	private Button					closetabButton		= confirmPanel.createButton(ConfirmPanel.A_CANCEL);

	private Textbox					txtDocumentName		= new Textbox();
	private Listbox					lstboxCategory		= new Listbox();

	private WSearchEditor			seBPartnerField		= null;


	// create Directory
	private Button					createDirButton		= new Button();
	private Button					uploadContentButton	= new Button();
	private Button					backButton			= new Button();
	private Button					nextButton			= new Button();

	private Label					positionInfo		= new Label();

	public static MDMS_Content		currentDMSContent;
	public static MDMS_Content		previousDmsContent;
	public static MDMS_Content		nextDmsContent;

	public static final String		SQL_GET_IMAGE_ID	= "SELECT AD_Image_ID FROM AD_Image Where name ilike ? ";
	public static final String		IMAGE_DOWNLOAD		= "Download";
	public static final String		IMAGE_DIRECTORY		= "Directory";

	private I_AD_StorageProvider	storageProvider;
	private ImgTextComponent		cstmComponenet		= null;

	public WDocumentViewer()
	{
		m_WindowNo = form.getWindowNo();
	}

	private void dynInit()
	{
		int dms_Content_ID;
		previousDmsContent = null;
		nextDmsContent = null;

		// storageProvider =

		// IFileStorageProvider provider= FileStorageUtil.get("FileSystem");

		dms_Content_ID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE parentUrl IS NULL");

		if (dms_Content_ID == -1)
		{
			try
			{
				storageProvider = DmsUtility.getStorageProvider(Env.getAD_Client_ID(Env.getCtx()));

				if (storageProvider == null)
				{
					FDialog.warn(0, "DMS Storage Provider is not define on client Info");
					throw new AdempiereException("Storage provider is not define");
				}
				File rootDir = new File(storageProvider.getFolder());

				if (!rootDir.exists())
					rootDir.mkdirs();

				currentDMSContent = new MDMS_Content(Env.getCtx(), 0, null);
				currentDMSContent.setName(storageProvider.getFolder());
				currentDMSContent.setValue(rootDir.getName());
				currentDMSContent.setDMS_ContentType_ID(DmsUtility.getContentTypeID());
				currentDMSContent.setDMS_MimeType_ID(DmsUtility.getMimeTypeID(null));
				currentDMSContent.setDMS_Status_ID(DmsUtility.getStatusID());
				// mdms_content.setM_AttributeSetInstance_ID(DmsUtility.getAttributeSet_ID());
				currentDMSContent.setContentBaseType(X_DMS_Content.CONTENTBASETYPE_Directory);
				currentDMSContent.saveEx();
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Root Creation failure:" + e.getLocalizedMessage());
				throw new AdempiereException("Root Creation failure:" + e.getLocalizedMessage());
			}
		}
		else
		{
			currentDMSContent = new MDMS_Content(Env.getCtx(), dms_Content_ID, null);
		}

	}

	private void jbInit() throws Exception
	{
		tabBox.setWidth("100%");
		tabBox.setHeight("100%");
		tabBox.appendChild(tabs);
		tabBox.appendChild(tabPanels);
		tabBox.addEventListener(Events.ON_SELECT, this);

		// View Result Tab

		Grid gridView = GridFactory.newGridLayout();
		gridView.setStyle("margin:0; padding:0; ");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		Columns columns = new Columns();
		gridView.appendChild(columns);

		Column column = new Column();
		column.setWidth("100px");
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
		hbox.appendChild(dbReportFrom);
		hbox.appendChild(dbReportTo);

		Cell reportingCell = new Cell();
		reportingCell.setColspan(2);
		reportingCell.appendChild(hbox);
		row.appendChild(reportingCell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblBPartner);

		MLookup lookup = MLookupFactory.get(Env.getCtx(), m_WindowNo, 0, 2762, DisplayType.Search);
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
		cell.setWidth("60%");
		cell.appendChild(grid);
		boxViewSeparator.appendChild(cell);

		cell = new Cell();
		cell.setWidth("35%");
		gridView.setWidth("100%");
		cell.appendChild(gridView);
		boxViewSeparator.appendChild(cell);
		tabViewPanel.appendChild(boxViewSeparator);

		grid.setHeight("680px");
		grid.setWidth("950px");
		grid.setAutopaging(true);
		grid.setZclass("none");
		gridView.setZclass("none");

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
				renderViewer();
			}
			else if (currentDMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Content))
			{
				File documentToPreview = null;

				if (currentDMSContent.getParentURL() != null)
				{
					documentToPreview = new File(System.getProperty("user.dir") + File.separator
							+ currentDMSContent.getParentURL() + File.separator + currentDMSContent.getName());

					if (DmsUtility.accept(documentToPreview))
					{
						if (documentToPreview.exists())
						{
							Tab tabData = new Tab(currentDMSContent.getName());
							tabData.setClosable(true);
							tabs.appendChild(tabData);
							tabBox.setSelectedTab(tabData);
							Tabpanel tabDataPanel = new Tabpanel();
							new WDocumentEditor(this, documentToPreview, tabDataPanel, currentDMSContent);
							currentDMSContent = previousDmsContent;
						}
					}
					else
					{
						AMedia media = new AMedia(documentToPreview, "application/octet-stream", null);
						Filedownload.save(media);
						currentDMSContent = previousDmsContent;
					}
				}
			}
		}
		else if (event.getTarget().equals(createDirButton))
		{
			new CreateDirectoryForm(currentDMSContent);
		}
		else if (event.getTarget().equals(uploadContentButton))
		{
			new WUploadContent(currentDMSContent);
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
				currentDMSContent = new MDMS_Content(Env.getCtx(), DMS_Content_ID, null);
				renderViewer();
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
				renderViewer();
			}
			nextButton.setEnabled(false);
		}
		else if (event.getTarget().getId().equals(confirmPanel.A_RESET))
		{
			clearComponenets();
		}
	}

	@Override
	protected void initForm()
	{
		try
		{
			jbInit();
			dynInit();
			renderViewer();

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
		setMode(Mode.EMBEDDED);
		m_WindowNo = form.getWindowNo();
	}

	public void renderViewer() throws IOException, URISyntaxException
	{
		File documents[] = null;
		if (currentDMSContent.getParentURL() == null)
		{
			I_AD_StorageProvider storageProvider = DmsUtility.getStorageProvider(Env.getAD_Client_ID(Env.getCtx()));
			documents = new File(System.getProperty("user.dir") + File.separator + storageProvider.getFolder())
					.listFiles();
		}
		else
			documents = new File(System.getProperty("user.dir") + currentDMSContent.getParentURL() + File.separator
					+ currentDMSContent.getName()).listFiles();

		Components.removeAllChildren(grid);
		// tabs.appendChild(tabView);
		Rows rows = new Rows();

		if (documents.length > 0)
		{
			BigDecimal Content_ID;
			int i = 0;
			MDMS_Content dmsContent = null;
			File document_thumbFile = null;

			List<List<Object>> DMS_Association_IDS = DB.getSQLArrayObjectsEx(null,
					"SELECT DMS_Content_ID FROM DMS_Documents_V WHERE DMS_Content_Related_ID = ?",
					currentDMSContent.getDMS_Content_ID());

			if (DMS_Association_IDS.size() > 0)
			{
				AImage image = null;
				String src = null;
				Row row = null;
				Cell cell = null;

				for (List<Object> documentRow : DMS_Association_IDS)
				{
					Content_ID = (BigDecimal) documentRow.get(0);
					dmsContent = new MDMS_Content(Env.getCtx(), Content_ID.intValue(), null);

					src = System.getProperty("user.dir") + File.separator + "DMS_Thumbnails" + File.separator
							+ Env.getAD_Client_ID(Env.getCtx()) + File.separator + dmsContent.getDMS_Content_ID()
							+ File.separator + dmsContent.getDMS_Content_ID() + "-150.jpg";
					if (documents[i].isDirectory())
					{
						nextButton.setEnabled(true);
						backButton.setEnabled(true);
					}
					else
					{
						nextButton.setEnabled(false);
						backButton.setEnabled(true);
					}
					document_thumbFile = new File(src);

					if (!document_thumbFile.exists())
					{
						int AD_Image_ID = 0;
						MImage mImage = null;
						byte[] b = null;

						AD_Image_ID = DB.getSQLValue(null, SQL_GET_IMAGE_ID, ((dmsContent.getContentBaseType()
								.equals(X_DMS_Content.CONTENTBASETYPE_Directory)) ? IMAGE_DIRECTORY : IMAGE_DOWNLOAD));

						mImage = new MImage(Env.getCtx(), AD_Image_ID, null);
						b = mImage.getData();
						image = new AImage("", b);
					}
					else
						image = new AImage(document_thumbFile);

					if (i % 6 == 0)
					{
						row = new Row();
						row.setHeight("150px");
						rows.appendChild(row);
					}
					cstmComponenet = new ImgTextComponent(dmsContent.getName(), documents[i].getAbsolutePath(), image,
							dmsContent.getDMS_Content_ID());

					cstmComponenet.addEventListener(Events.ON_DOUBLE_CLICK, this);
					cstmComponenet.addEventListener(Events.ON_CLICK, this);
					cstmComponenet.addEventListener(Events.ON_RIGHT_CLICK, this);

					grid.setSizedByContent(true);
					cstmComponenet.setDheight(150);
					cstmComponenet.setDwidth(150);
					cell = new Cell();

					cell.setWidth(row.getWidth());
					cell.appendChild(cstmComponenet);
					row.appendChild(cell);
					i++;
				}
			}
		}
		else
		{
			nextButton.setEnabled(false);
			backButton.setEnabled(true);
		}
		positionInfo.setValue(currentDMSContent.getName());
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
