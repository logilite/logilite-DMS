package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;
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
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Searchbox;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.compiere.model.MImage;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MStorageProvider;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.idempiere.componenet.ImgTextComponent;
import org.idempiere.dms.storage.DmsUtility;
import org.idempiere.form.DocumentViewer;
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
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Space;

public class WDocumentViewer extends DocumentViewer implements EventListener<Event>
{

	private static final long	serialVersionUID	= -6813481516566180243L;
	private CustomForm			form				= new CustomForm();
	private Tabbox				tabbox				= new Tabbox();
	private Tabs				tabs				= new Tabs();
	private Tabpanels			tabpanels			= new Tabpanels();
	private Grid				grid				= GridFactory.newGridLayout();
	Tab							tabView				= new Tab(Msg.getMsg(Env.getCtx(), "ViewerResult"));
	Tabpanel					tabViewPanel		= new Tabpanel();

	private Cell				griddocumentCell	= new Cell();

	// View Result Tab
	private Searchbox			vsearcBox			= new Searchbox();
	private Label				advanceSearchLabel	= new Label(Msg.translate(Env.getCtx(), "Advance Search"));
	private Label				nameLabel			= new Label(Msg.translate(Env.getCtx(), "Name"));
	private Textbox				nametextbox			= new Textbox();
	private Label				categoryLabel		= new Label(Msg.translate(Env.getCtx(), "Category"));
	private Listbox				categoryListbox		= new Listbox();
	private Label				createdVLabel		= new Label(Msg.translate(Env.getCtx(), "Created:"));
	private Datebox				createdVTo			= new Datebox();
	private Datebox				createdVFrom		= new Datebox();
	private Label				updatedvLabel		= new Label(Msg.translate(Env.getCtx(), "Updated:"));
	private Datebox				updatedVTo			= new Datebox();
	private Datebox				updatedVFrom		= new Datebox();
	private Label				contentmetaLabel	= new Label(Msg.translate(Env.getCtx(), "Content Meta"));
	private Label				reportdateLabel		= new Label(Msg.translate(Env.getCtx(), "Report Date"));
	private Datebox				reportVTo			= new Datebox();
	private Datebox				reportVFrom			= new Datebox();
	private Label				bPartnerLabel		= new Label(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
	private WSearchEditor		bPartnerField		= null;
	private ConfirmPanel		confirmPanel		= new ConfirmPanel();
	private Button				clearButton			= confirmPanel.createButton(ConfirmPanel.A_RESET);
	private Button				searchButton		= confirmPanel.createButton(ConfirmPanel.A_REFRESH);
	private Button				closetabButton		= confirmPanel.createButton(ConfirmPanel.A_CANCEL);
	private Button				gridViewButton		= confirmPanel.createButton(ConfirmPanel.A_CUSTOMIZE);
	private boolean				isGridButton		= true;

	// tabData

	private WListbox			xMiniTable			= ListboxFactory.newDataTable();

	// create Directory
	private Button				createDirButton		= new Button();
	private Button				uploadContentButton	= new Button();
	public static MDMS_Content	mdms_content;
	public static MDMS_Content	previous_dmsContent;
	public static MDMS_Content	next_dmsContent;
	private MStorageProvider	storageProvider;
	private Button				backButton			= new Button();
	private Button				nextButton			= new Button();
	private Label				positionInfo		= new Label();

	public WDocumentViewer()
	{
		m_WindowNo = form.getWindowNo();
	}

	private void dynInit()
	{
		int dms_Content_ID;
		MLookup lookup = MLookupFactory.get(Env.getCtx(), m_WindowNo, 0, 2762, DisplayType.Search);
		bPartnerField = new WSearchEditor(lookup, Msg.translate(Env.getCtx(), "C_BPartner_ID"), "", true, false, true);
		previous_dmsContent = null;
		next_dmsContent = null;
		dms_Content_ID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE parentUrl IS NULL");

		if (dms_Content_ID == -1)
		{
			try
			{
				storageProvider = DmsUtility.getStorageProvider(Env.getAD_Client_ID(Env.getCtx()));
				File rootDir = new File(storageProvider.getFolder());

				if (!rootDir.exists())
					rootDir.mkdirs();
				mdms_content = new MDMS_Content(Env.getCtx(), 0, null);
				mdms_content.setName(storageProvider.getFolder());
				mdms_content.setValue(rootDir.getName());
				mdms_content.setDMS_ContentType_ID(DmsUtility.getContentTypeID());
				mdms_content.setDMS_MimeType_ID(DmsUtility.getMimeTypeId(null));
				mdms_content.setDMS_Status_ID(DmsUtility.getStatusID());
				// mdms_content.setM_AttributeSetInstance_ID(DmsUtility.getAttributeSet_ID());
				mdms_content.setContentBaseType(X_DMS_Content.CONTENTBASETYPE_Directory);
				mdms_content.saveEx();
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Root Creation failure:" + e.getLocalizedMessage());
				throw new AdempiereException("Root Creation failure:" + e.getLocalizedMessage());
			}
		}
		else
		{
			mdms_content = new MDMS_Content(Env.getCtx(), dms_Content_ID, null);
		}

	}

	private void jbInit() throws Exception
	{
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");
		tabbox.appendChild(tabs);
		tabbox.appendChild(tabpanels);
		tabbox.addEventListener(Events.ON_SELECT, this);

		// View Result Tab

		Grid gridView = GridFactory.newGridLayout();
		gridView.setStyle("margin:0; padding:0; ");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");

		Columns columns = new Columns();
		gridView.appendChild(columns);

		Column column = new Column();
		column.setHflex("min");
		column.setAlign("left");
		columns.appendChild(column);

		column = new Column();
		column.setHflex("1");
		column.setAlign("center");
		columns.appendChild(column);

		column = new Column();
		column.setHflex("min");
		column.setAlign("right");
		columns.appendChild(column);

		Rows rows = new Rows();
		gridView.appendChild(rows);

		Row row = new Row();
		rows.appendChild(row);

		// row.appendChild(backButton);
		backButton.setImage(ThemeManager.getThemeResource("images/wfBack24.png"));
		backButton.setTooltiptext("Previous Record");

		// row.appendChild(positionInfo);
		positionInfo.setHflex("1");
		ZkCssHelper.appendStyle(positionInfo, "font-weight: bold;");
		ZkCssHelper.appendStyle(positionInfo, "align: center;");

		// row.appendChild(nextButton);
		nextButton.setImage(ThemeManager.getThemeResource("images/wfNext24.png"));
		nextButton.setTooltiptext("Next Record");
		backButton.addEventListener(Events.ON_CLICK, this);
		nextButton.addEventListener(Events.ON_CLICK, this);

		Hbox navigationBox = new Hbox();
		navigationBox.appendChild(backButton);
		navigationBox.appendChild(new Space());
		navigationBox.appendChild(new Space());
		navigationBox.appendChild(new Space());
		navigationBox.appendChild(new Space());
		navigationBox.appendChild(new Space());
		navigationBox.appendChild(positionInfo);
		navigationBox.appendChild(new Space());
		navigationBox.appendChild(new Space());
		navigationBox.appendChild(new Space());
		navigationBox.appendChild(new Space());
		navigationBox.appendChild(new Space());
		navigationBox.appendChild(nextButton);

		Cell navigationCell = new Cell();
		navigationCell.setColspan(3);
		navigationCell.setAlign("center");
		navigationCell.appendChild(navigationBox);
		row.appendChild(navigationCell);

		Hbox btnhbox = new Hbox();
		btnhbox.appendChild(createDirButton);
		btnhbox.appendChild(uploadContentButton);
		btnhbox.appendChild(gridViewButton);

		Cell btnCell = new Cell();
		btnCell.setColspan(3);
		btnCell.setRowspan(1);
		btnCell.setAlign("center");
		btnCell.appendChild(btnhbox);

		createDirButton.setImage("/home/dhaval/Desktop/1.png");
		createDirButton.setTooltiptext("Create Directory");
		createDirButton.addEventListener(Events.ON_CLICK, this);

		uploadContentButton.setImage("/home/dhaval/Desktop/1.png");
		uploadContentButton.setTooltiptext("Upload Content");
		gridViewButton.addActionListener(this);
		uploadContentButton.addEventListener(Events.ON_CLICK, this);

		row = new Row();
		row.appendChild(btnCell);
		rows.appendChild(row);

		row = new Row();
		Cell searchCell = new Cell();
		searchCell.setRowspan(1);
		searchCell.setColspan(3);
		vsearcBox.setWidth("100%");
		searchCell.appendChild(vsearcBox);
		rows.appendChild(row);
		row.appendChild(searchCell);
		vsearcBox.setButtonImage(ThemeManager.getThemeResource("images/find16.png"));

		row = new Row();
		rows.appendChild(row);
		row.appendCellChild(advanceSearchLabel);
		advanceSearchLabel.setHflex("1");
		ZkCssHelper.appendStyle(advanceSearchLabel, "font-weight: bold;");

		row = new Row();
		rows.appendChild(row);
		ZkCssHelper.appendStyle(nameLabel, "font-weight: bold;");
		Cell nameCell = new Cell();
		nameCell.setColspan(2);
		row.appendChild(nameLabel);
		nameCell.appendChild(nametextbox);
		row.appendChild(nameCell);
		nametextbox.setWidth("100%");

		row = new Row();
		rows.appendChild(row);
		row.setAlign("right");
		row.appendChild(categoryLabel);
		Cell categoryListCell = new Cell();
		categoryListCell.setColspan(2);
		categoryListbox.setMold("select");
		categoryListCell.appendChild(categoryListbox);
		categoryListbox.setWidth("100%");
		row.appendChild(categoryListCell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(createdVLabel);
		Hbox hbox = new Hbox();
		hbox.appendChild(createdVTo);
		hbox.appendChild(createdVFrom);

		Cell createdCell = new Cell();
		createdCell.setColspan(2);
		createdCell.appendChild(hbox);
		row.appendCellChild(createdCell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(updatedvLabel);
		hbox = new Hbox();
		hbox.appendChild(updatedVTo);
		hbox.appendChild(updatedVFrom);

		Cell updatedCell = new Cell();
		updatedCell.setColspan(2);
		updatedCell.appendChild(hbox);
		row.appendCellChild(updatedCell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(contentmetaLabel);
		ZkCssHelper.appendStyle(contentmetaLabel, "font-weight: bold;");

		row = new Row();
		rows.appendChild(row);
		row.appendCellChild(reportdateLabel);
		hbox = new Hbox();
		hbox.appendChild(reportVTo);
		hbox.appendChild(reportVFrom);

		Cell reportingCell = new Cell();
		reportingCell.setColspan(2);
		reportingCell.appendChild(hbox);
		row.appendChild(reportingCell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(bPartnerLabel);

		Cell bpartnercell = new Cell();
		bpartnercell.setColspan(2);
		bpartnercell.appendChild(bPartnerField.getComponent());
		row.appendChild(bpartnercell);

		row = new Row();
		rows.appendChild(row);
		hbox = new Hbox();
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
		Hbox boxViewSeparator = new Hbox();
		boxViewSeparator.setWidth("100%");
		boxViewSeparator.setHeight("100%");
		cell = new Cell();
		griddocumentCell.setWidth("70%");
		griddocumentCell.appendChild(grid);
		boxViewSeparator.appendChild(griddocumentCell);
		cell = new Cell();
		cell.setWidth("5%");
		cell.appendChild(gridView);
		boxViewSeparator.appendChild(cell);
		tabViewPanel.appendChild(boxViewSeparator);

		grid.setHeight("100%");
		grid.setWidth("100%");
		grid.setAutopaging(true);
		tabs.appendChild(tabView);
		tabpanels.appendChild(tabViewPanel);
		this.setHeight("100%");
		this.setWidth("100%");
		this.appendChild(tabbox);
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		log.info(event.getName());

		if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			SessionManager.getAppDesktop().closeActiveWindow();
		}
		else if (Events.ON_DOUBLE_CLICK.equals(event.getName()))
		{
			if (mdms_content.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
			{
				renderViewer(isGridButton, mdms_content);
			}
			else if (mdms_content.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Content))
			{
				File document_preview = null;

				if (mdms_content.getParentURL() != null)
				{
					document_preview = new File(System.getProperty("user.dir") + File.separator
							+ mdms_content.getParentURL() + File.separator + mdms_content.getName());

					if (DmsUtility.accept(document_preview))
					{
						if (document_preview.exists())
						{
							Tab tabData = new Tab(mdms_content.getName());
							tabData.setClosable(true);
							tabs.appendChild(tabData);
							tabbox.setSelectedTab(tabData);

							Tabpanel tabDataPanel = new Tabpanel();
							showDataTab(tabDataPanel, document_preview);
							mdms_content = previous_dmsContent;
						}
					}
					else
					{
						AMedia media = new AMedia(document_preview, "application/octet-stream", null);
						Filedownload.save(media);
					}
				}
			}
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_CUSTOMIZE))
		{
			if (isGridButton)
				isGridButton = false;
			else
				isGridButton = true;

			renderViewer(isGridButton, mdms_content);

		}
		else if (event.getTarget().equals(createDirButton))
		{
			CreateDirectoryForm form = new CreateDirectoryForm(mdms_content, this, isGridButton);
		}
		else if (event.getTarget().equals(uploadContentButton))
		{
			WUploadContent uploadContent = new WUploadContent(mdms_content, this, isGridButton);
		}
		else if (event.getTarget().equals(backButton))
		{
			next_dmsContent = mdms_content;
			int DMS_Content_ID = DB.getSQLValue(null,
					"SELECT DMS_Content_Related_ID FROM DMS_Association WHERE DMS_Content_ID = ?",
					mdms_content.getDMS_Content_ID());

			if (DMS_Content_ID == -1)
				backButton.setEnabled(false);
			else
			{
				mdms_content = new MDMS_Content(Env.getCtx(), DMS_Content_ID, null);
				renderViewer(isGridButton, mdms_content);
				nextButton.setEnabled(true);
				if (mdms_content.getParentURL() == null)
					backButton.setEnabled(false);
			}
		}
		else if (event.getTarget().equals(nextButton))
		{
			if (next_dmsContent == null)
				nextButton.setEnabled(false);
			else
			{
				mdms_content = next_dmsContent;
				renderViewer(isGridButton, mdms_content);
				nextButton.setEnabled(false);
			}
		}
	}

	protected void onOk(boolean isGridButton, MDMS_Content mdms_Content) throws IOException, URISyntaxException
	{
		renderViewer(isGridButton, mdms_Content);
	}

	@Override
	protected void initForm()
	{
		try
		{
			dynInit();
			jbInit();
			renderViewer(isGridButton, mdms_content);
			backButton.setEnabled(false);
			nextButton.setEnabled(false);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render Componenet Problem");
		}
		SessionManager.getAppDesktop();
		setMode(Mode.EMBEDDED);
		m_WindowNo = form.getWindowNo();
	}

	public void renderViewer(boolean isGridformat, MDMS_Content mdms_Content) throws IOException, URISyntaxException
	{
		File documents[] = null;
		if (mdms_Content.getParentURL() == null)
		{
			MStorageProvider storageProvider = DmsUtility.getStorageProvider(Env.getAD_Client_ID(Env.getCtx()));
			documents = new File(System.getProperty("user.dir") + File.separator + storageProvider.getFolder())
					.listFiles();
		}
		else
			documents = new File(System.getProperty("user.dir") + mdms_Content.getParentURL() + File.separator
					+ mdms_Content.getName()).listFiles();

		Components.removeAllChildren(grid);
		// tabs.appendChild(tabView);
		Rows rows = new Rows();

		if (documents.length > 0)
		{
			BigDecimal content_id;
			int i = 0;
			MDMS_Content dmsContent = null;
			File document_thumbFile = null;

			List<List<Object>> dms_association_ids = DB.getSQLArrayObjectsEx(null,
					"SELECT DMS_Content_ID FROM DMS_Documents_V WHERE DMS_Content_Related_ID = ?",
					mdms_Content.getDMS_Content_ID());

			if (dms_association_ids.size() > 0)
			{
				i = 0;

				AImage image = null;
				String src = null;
				Row row = null;
				Cell cell = null;

				Vector<Vector<Object>> imageData = new Vector<Vector<Object>>();
				Vector<Object> rowList = null;

				for (List<Object> documentRow : dms_association_ids)
				{
					content_id = (BigDecimal) documentRow.get(0);
					dmsContent = new MDMS_Content(Env.getCtx(), content_id.intValue(), null);

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
					if (isGridformat)
					{
						isGridButton = true;
						document_thumbFile = new File(src);

						if (!document_thumbFile.exists())
						{
							int AD_Image_ID = 0;
							MImage mImage = null;
							byte[] b = null;
							if (dmsContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
								AD_Image_ID = DB.getSQLValue(null,
										"SELECT AD_Image_ID FROM AD_Image Where name ilike 'Directory'");
							else
								AD_Image_ID = DB.getSQLValue(null,
										"SELECT AD_Image_ID FROM AD_Image Where name ilike 'Download'");
							mImage = new MImage(Env.getCtx(), AD_Image_ID, null);
							b = mImage.getData();
							image = new AImage("", b);
						}
						else
							image = new AImage(document_thumbFile);

						if (i % 5 == 0)
						{
							row = new Row();
							row.setHeight("150px");
							rows.appendChild(row);
						}
						ImgTextComponent cstmComponenet = new ImgTextComponent(dmsContent.getName(),
								documents[i].getAbsolutePath(), image, dmsContent.getDMS_Content_ID());
						cstmComponenet.addEventListener(Events.ON_DOUBLE_CLICK, this);
						grid.setSizedByContent(true);
						cstmComponenet.setDheight(150);
						cstmComponenet.setDwidth(150);
						cell = new Cell();

						cell.setWidth(row.getWidth());
						cell.appendChild(cstmComponenet);
						row.appendChild(cell);
						i++;
					}
					else
					{
						isGridButton = false;
						cell = new Cell();
						row = new Row();
						row.setHeight("100%");
						cell.setParent(row);
						row.setParent(rows);
						rowList = new Vector<Object>();
						rowList.add("file://" + src);
						rowList.add(dmsContent.getName());
						rowList.add(dmsContent.getContentBaseType());
						imageData.add(rowList);
						Vector<String> columnNames = null;
						xMiniTable.clear();

						columnNames = new Vector<String>();
						columnNames.add("Image");
						columnNames.add("Document Name");
						columnNames.add("Document Type");

						ListModelTable model = new ListModelTable(imageData);
						xMiniTable.setData(model, columnNames);
						xMiniTable.setColumnClass(0, MImage.class, true);
						xMiniTable.setColumnClass(1, String.class, true);
						xMiniTable.setColumnClass(2, String.class, true);
						xMiniTable.setMultiSelection(false);
						cell.appendChild(xMiniTable);
					}
				}
			}
		}
		else
		{
			nextButton.setEnabled(false);
			backButton.setEnabled(true);
		}
		positionInfo.setValue(mdms_content.getName());
		grid.appendChild(rows);
		tabbox.setSelectedIndex(0);
	}

	public void showDataTab(Tabpanel tabDataPanel, File document_preview)
	{
		Cell preview_cell = new Cell();

		Label nameTLabel = new Label("Name:");
		Label nameTContent = new Label("so-123");
		Label categoryTLabel = new Label("Category:");
		Label categoryTContent = new Label("Sales Report");
		Label statusTLabel = new Label("Status:");
		Label statusTContent = new Label("Emailed");
		Label metaTLabel = new Label("Meta");
		Label reportDateTLabel = new Label("Report Date:");
		Label reportDateTContent = new Label("10 Oct 2015 ");
		Label partnerTLabel = new Label("Partner:");
		Label partnerTContent = new Label("C&W Construction");

		Grid gridData = GridFactory.newGridLayout();
		gridData.setStyle("margin:0; padding:0;");
		gridData.makeNoStrip();
		gridData.setOddRowSclass("even");

		Columns columns = new Columns();
		gridData.appendChild(columns);

		Column column = new Column();
		column.setHflex("min");
		column.setAlign("left");
		columns.appendChild(column);

		column = new Column();
		column.setHflex("1");
		column.setAlign("center");
		columns.appendChild(column);

		column = new Column();
		column.setHflex("min");
		column.setAlign("right");
		columns.appendChild(column);

		Rows rows = new Rows();
		gridData.appendChild(rows);

		Row row = new Row();
		rows.appendChild(row);
		row.appendChild(nameTLabel);
		row.appendChild(nameTContent);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(categoryTLabel);
		row.appendChild(categoryTContent);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(statusTLabel);
		row.appendChild(statusTContent);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(metaTLabel);
		ZkCssHelper.appendStyle(metaTLabel, "font-weight: bold;");
		row.appendChild(new Space());

		row = new Row();
		rows.appendChild(row);
		row.appendChild(reportDateTLabel);
		row.appendChild(reportDateTContent);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(partnerTLabel);
		row.appendChild(partnerTContent);

		Hbox boxViewSeparator = new Hbox();
		boxViewSeparator.setWidth("100%");
		boxViewSeparator.setHeight("100%");
		preview_cell.setWidth("70%");

		Iframe preview_Content = new Iframe();

		AMedia pdfmedia = null;
		try
		{
			pdfmedia = new AMedia(document_preview, null, null);
		}
		catch (FileNotFoundException e)
		{
			log.log(Level.SEVERE, "Document cannot be displayed:" + e.getLocalizedMessage());
			throw new AdempiereException("Document cannot be displayed:" + e.getLocalizedMessage());
		}
		preview_Content.setContent(null);
		preview_Content.setContent(pdfmedia);
		preview_Content.setWidth("100%");
		preview_Content.setHeight("100%");
		preview_cell.appendChild(preview_Content);
		boxViewSeparator.appendChild(preview_cell);
		Cell cell = new Cell();
		cell.setWidth("30%");
		cell.appendChild(gridData);
		boxViewSeparator.appendChild(cell);
		tabDataPanel.appendChild(boxViewSeparator);

		tabpanels.appendChild(tabDataPanel);
		this.setHeight("100%");
		this.setWidth("100%");
		this.appendChild(tabbox);
	}
}
