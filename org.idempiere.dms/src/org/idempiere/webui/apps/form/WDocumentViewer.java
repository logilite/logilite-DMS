package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;
import java.util.logging.Level;

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
import org.adempiere.webui.window.FDialog;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MImage;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MStorageProvider;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.idempiere.componenet.ImgTextComponent;
import org.idempiere.dms.storage.DmsUtility;
import org.idempiere.dms.storage.FileSystemStorageProvider;
import org.idempiere.form.DocumentViewer;
import org.idempiere.model.MDMS_Association;
import org.idempiere.model.MDMS_Content;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Space;

public class WDocumentViewer extends DocumentViewer implements EventListener<Event>
{

	private static final long	serialVersionUID	= -6813481516566180243L;
	private CustomForm			form				= new CustomForm();
	private Tabbox				tabbox				= new Tabbox();
	private Tabs				tabs				= new Tabs();
	private Tabpanels			tabpanels			= new Tabpanels();
	private Grid				grid				= GridFactory.newGridLayout();
	private Grid				mainGridData		= GridFactory.newGridLayout();
	Tab							tabView				= new Tab(Msg.getMsg(Env.getCtx(), "ViewerResult"));
	Tabpanel					tabDataPanel		= new Tabpanel();
	Tabpanel					tabViewPanel		= new Tabpanel();

	private Cell				tempcell			= new Cell();
	private Cell				tempcell1			= new Cell();

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
	private ConfirmPanel		cp					= new ConfirmPanel();
	private Button				clearButton			= cp.createButton(ConfirmPanel.A_RESET);
	private Button				searchButton		= cp.createButton(ConfirmPanel.A_REFRESH);
	private Button				closetabButton		= cp.createButton(ConfirmPanel.A_CANCEL);
	private Button				gridViewButton		= cp.createButton(ConfirmPanel.A_CUSTOMIZE);
	private boolean				isGridButton		= true;

	// tabData
	private Label				nameTLabel			= new Label(Msg.translate(Env.getCtx(), "Name:"));
	private Label				nameTContent		= new Label(Msg.translate(Env.getCtx(), "so-123"));
	private Label				categoryTLabel		= new Label(Msg.translate(Env.getCtx(), "Category:"));
	private Label				categoryTContent	= new Label(Msg.translate(Env.getCtx(), "Sales Report"));
	private Label				statusTLabel		= new Label(Msg.translate(Env.getCtx(), "Status:"));
	private Label				statusTContent		= new Label(Msg.translate(Env.getCtx(), "Emailed"));
	private Label				metaTLabel			= new Label(Msg.translate(Env.getCtx(), "Meta"));
	private Label				reportDateTLabel	= new Label(Msg.translate(Env.getCtx(), "Report Date:"));
	private Label				reportDateTContent	= new Label(Msg.translate(Env.getCtx(), "10 Oct 2015 "));
	private Label				partnerTLabel		= new Label(Msg.translate(Env.getCtx(), "Partner:"));
	private Label				partnerTContent		= new Label(Msg.translate(Env.getCtx(), "C&W Construction"));
	private WListbox			xMiniTable			= ListboxFactory.newDataTable();

	// create Directory
	private Button				createDirButton		= new Button();
	public MDMS_Content			dms_content;

	public WDocumentViewer()
	{
		m_WindowNo = form.getWindowNo();
	}

	private void dynInit()
	{
		MLookup lookup = MLookupFactory.get(Env.getCtx(), m_WindowNo, 0, 2762, DisplayType.Search);
		bPartnerField = new WSearchEditor(lookup, Msg.translate(Env.getCtx(), "C_BPartner_ID"), "", true, false, true);
	}

	private void jbInit() throws Exception
	{
		tabbox.setWidth("100%");
		tabbox.setHeight("90%");
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
		row.appendChild(gridViewButton);
		createDirButton.setImage("/home/dhaval/Desktop/1.png");
		createDirButton.setTooltiptext("Create Directory");
		row.appendCellChild(createDirButton);
		gridViewButton.addActionListener(this);
		createDirButton.addEventListener(Events.ON_CLICK, this);

		row = new Row();
		vsearcBox.setWidth("100%");
		rows.appendChild(row);
		row.appendChild(vsearcBox);
		vsearcBox.setButtonImage(ThemeManager.getThemeResource("images/find16.png"));

		row = new Row();
		rows.appendChild(row);
		row.appendCellChild(advanceSearchLabel);
		advanceSearchLabel.setHflex("1");
		ZkCssHelper.appendStyle(advanceSearchLabel, "font-weight: bold;");

		row = new Row();
		rows.appendChild(row);
		row.appendCellChild(nameLabel);
		ZkCssHelper.appendStyle(nameLabel, "font-weight: bold;");
		row.appendCellChild(nametextbox);
		nametextbox.setWidth("100%");
		row.appendChild(new Space());

		row = new Row();
		rows.appendChild(row);
		row.setAlign("right");
		row.appendCellChild(categoryLabel);
		row.appendCellChild(categoryListbox);
		categoryListbox.setWidth("100%");
		row.appendChild(new Space());

		row = new Row();
		rows.appendChild(row);
		row.appendCellChild(createdVLabel);
		Hbox hbox = new Hbox();
		hbox.appendChild(createdVTo);
		hbox.appendChild(createdVFrom);
		row.appendCellChild(hbox);
		row.appendChild(new Space());

		row = new Row();
		rows.appendChild(row);
		row.appendCellChild(updatedvLabel);
		hbox = new Hbox();
		hbox.appendChild(updatedVTo);
		hbox.appendChild(updatedVFrom);
		row.appendCellChild(hbox);
		row.appendChild(new Space());

		row = new Row();
		rows.appendChild(row);
		row.appendCellChild(contentmetaLabel);
		ZkCssHelper.appendStyle(contentmetaLabel, "font-weight: bold;");

		row = new Row();
		rows.appendChild(row);
		row.appendCellChild(reportdateLabel);
		hbox = new Hbox();
		hbox.appendChild(reportVTo);
		hbox.appendChild(reportVFrom);
		row.appendCellChild(hbox);
		row.appendChild(new Space());

		row = new Row();
		rows.appendChild(row);
		row.appendCellChild(bPartnerLabel);
		row.appendCellChild(bPartnerField.getComponent());
		row.appendChild(new Space());

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
		tempcell.setWidth("70%");
		tempcell.appendChild(grid);
		boxViewSeparator.appendChild(tempcell);
		cell = new Cell();
		cell.setWidth("30%");
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
			Tab tabData = new Tab(Msg.getMsg(Env.getCtx(), "tabData"));
			tabData.setClosable(true);
			tabs.appendChild(tabData);
			tabbox.setSelectedTab(tabData);
			showDataTab();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_CUSTOMIZE))
		{
			RenderViewer(isGridButton);
		}
		else if (event.getTarget().equals(createDirButton))
		{
			CreateDirectoryForm form = new CreateDirectoryForm(new MDMS_Content(Env.getCtx(), 0, null));
		}
	}

	@Override
	protected void initForm()
	{
		try
		{
			dynInit();
			jbInit();
			RenderViewer(isGridButton);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render Componenet Problem");
		}
		SessionManager.getAppDesktop();
		setMode(Mode.EMBEDDED);
		m_WindowNo = form.getWindowNo();
	}

	public void RenderViewer(boolean isGridformat) throws IOException, URISyntaxException
	{
		Components.removeAllChildren(grid);
		tabs.appendChild(tabView);
		Rows rows = new Rows();

		if (isGridformat)
		{
			isGridButton = false;
			AImage image = null;

			String src = null;

			Row row = null;
			Cell cell = null;

			row = new Row();
			src = "/home/dhaval/Desktop/1.png";

			image = new AImage(new File(src));

			ImgTextComponent cstmComponenet = new ImgTextComponent("1.png", "/home/dhaval/Desktop", image);

			cstmComponenet.addEventListener(Events.ON_DOUBLE_CLICK, this);
			grid.setSizedByContent(true);
			cstmComponenet.setDheight(150);
			cstmComponenet.setDwidth(150);

			Hbox hbox = new Hbox();
			hbox.appendChild(cstmComponenet);
			hbox.appendChild(new Space());

			cell = new Cell();
			cell.appendChild(hbox);

			row = new Row();
			row.setHeight("150px");
			row.appendChild(cell);
			rows.appendChild(row);

			cell.setWidth(row.getWidth());

			row.appendChild(cell);
		}
		else
		{
			isGridButton = true;
			Row row = new Row();
			Cell cell = new Cell();
			cell.setParent(row);
			row.setParent(rows);

			Vector<Vector<Object>> imageData = new Vector<Vector<Object>>();
			Vector<Object> rowList = null;
			String src = "file:///home/dhaval/Desktop/1.png";
			for (int i = 0; i < 8; i++)
			{
				rowList = new Vector<Object>();
				rowList.add(src);
				rowList.add("2.png");
				rowList.add("PNG");
				imageData.add(rowList);

			}

			Vector<String> columnNames = null;
			xMiniTable.clear();
			columnNames = new Vector<String>();
			columnNames.add("Image");
			columnNames.add("Image Name");
			columnNames.add("Image Type");
			ListModelTable model = new ListModelTable(imageData);
			xMiniTable.setData(model, columnNames);
			xMiniTable.setColumnClass(0, MImage.class, true);
			xMiniTable.setColumnClass(1, String.class, true);
			xMiniTable.setColumnClass(2, String.class, true);
			xMiniTable.setMultiSelection(false);
			xMiniTable.setSpan(true);
			xMiniTable.autoSize();
			cell.appendChild(xMiniTable);
		}

		grid.appendChild(rows);
		tabbox.setSelectedIndex(0);
	}

	public void showDataTab()
	{
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
		tempcell1.setWidth("70%");
		tempcell1.appendChild(mainGridData);
		boxViewSeparator.appendChild(tempcell1);
		Cell cell = new Cell();
		cell.setWidth("30%");
		cell.appendChild(gridData);
		boxViewSeparator.appendChild(cell);
		tabDataPanel.appendChild(boxViewSeparator);

		mainGridData.setHeight("100%");
		mainGridData.setWidth("100%");
		mainGridData.setAutopaging(true);
		tabpanels.appendChild(tabDataPanel);
		this.setHeight("100%");
		this.setWidth("100%");
		this.appendChild(tabbox);
	}
}