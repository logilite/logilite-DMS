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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.theme.ThemeManager;
import org.compiere.model.MImage;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.componenet.DMSViewerComponent;
import org.idempiere.dms.DMS;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Cell;
import org.zkoss.zul.South;

public class WDAttributePanel extends Panel implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long	serialVersionUID		= 5200959427619624094L;
	private static CLogger		log						= CLogger.getCLogger(WDAttributePanel.class);

	private Panel				panelAttribute			= new Panel();
	private Panel				panelFooterButtons		= new Panel();
	private Borderlayout		mainLayout				= new Borderlayout();

	private Tabbox				tabBoxAttribute			= new Tabbox();
	private Tabs				tabsAttribute			= new Tabs();
	private Tab					tabAttribute			= new Tab();
	private Tab					tabVersionHistory		= new Tab();

	private Tabpanels			tabpanelsAttribute		= new Tabpanels();
	private Tabpanel			tabpanelAttribute		= new Tabpanel();
	private Tabpanel			tabpanelVersionHitory	= new Tabpanel();

	private Grid				gridAttributeLayout		= new Grid();
	private Grid				grid					= new Grid();

	private Button				btnDelete				= null;
	private Button				btnRequery				= null;
	private Button				btnClose				= null;
	private Button				btnDownload				= null;
	private Button				btnEdit					= null;
	private Button				btnSave					= null;
	private Button				btnVersionUpload		= null;

	private Label				lblStatus				= null;
	private Label				lblName					= null;
	private Label				lblDesc					= null;

	private Textbox				txtName					= null;
	private Textbox				txtDesc					= null;

	private DMS					dms;
	private MDMSContent			DMS_Content				= null;
	private MDMSContent			parent_Content			= null;
	private DMSViewerComponent	viewerComponenet		= null;

	private Tabbox				tabBox					= null;
	private AImage				imageVersion			= null;
	private ConfirmPanel		confirmPanel			= null;
	private WDLoadASIPanel		ASIPanel				= null;

	private int					tableId					= 0;
	private int					recordId				= 0;

	private boolean				isWindowAccess			= true;

	public WDAttributePanel(DMS dms, I_DMS_Content DMS_Content, Tabbox tabBox, int tableID, int recordID, boolean isWindowAccess)
	{
		this.dms = dms;
		this.isWindowAccess = isWindowAccess;
		this.DMS_Content = (MDMSContent) DMS_Content;
		this.tabBox = tabBox;
		this.tableId = tableID;
		this.recordId = recordID;

		try
		{
			init();
			refreshPanel();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Issue while opening content attribute panel. " + e.getLocalizedMessage(), e);
		}

	}

	/**
	 * initialize components
	 */
	private void init()
	{
		this.appendChild(grid);
		grid.setHeight("100%");
		grid.setWidth("100%");
		grid.setZclass("none");
		panelAttribute.setZclass("none");

		this.setHeight("100%");
		this.setWidth("100%");

		Columns columns = new Columns();
		Rows rows = new Rows();

		Column column = new Column();
		columns.appendChild(column);

		Row row = new Row();
		row.appendChild(panelAttribute);
		rows.appendChild(row);
		grid.appendChild(columns);
		grid.appendChild(rows);

		lblStatus = new Label();
		ZkCssHelper.appendStyle(lblStatus, "font-weight: bold;");
		ZkCssHelper.appendStyle(lblStatus, "align: center;");
		lblStatus.setValue(MUser.getNameOfUser(DMS_Content.getUpdatedBy()) + " edited at " + DMS_Content.getUpdated());

		panelAttribute.appendChild(lblStatus);
		tabBoxAttribute.appendChild(tabsAttribute);
		tabBoxAttribute.appendChild(tabpanelsAttribute);
		panelAttribute.appendChild(tabBoxAttribute);
		tabBoxAttribute.setMold("accordion");
		tabBoxAttribute.setHeight("98%");
		tabBoxAttribute.setWidth("100%");

		tabsAttribute.appendChild(tabAttribute);
		tabsAttribute.appendChild(tabVersionHistory);

		tabAttribute.setLabel(DMSConstant.MSG_ATTRIBUTES);
		tabAttribute.setWidth("100%");
		tabVersionHistory.setLabel(DMSConstant.MSG_VERSION_HISTORY);

		tabpanelsAttribute.appendChild(tabpanelAttribute);
		// tabpanelsAttribute.setStyle("display: flex;");
		tabpanelsAttribute.setHeight("98%");
		tabpanelsAttribute.setWidth("100%");

		tabpanelsAttribute.appendChild(tabpanelVersionHitory);
		tabpanelVersionHitory.setHeight("500px");
		tabpanelVersionHitory.setWidth("100%");

		tabpanelAttribute.appendChild(gridAttributeLayout);
		tabVersionHistory.setWidth("100%");

		columns = new Columns();
		column = new Column();

		rows = new Rows();
		row = new Row();

		gridAttributeLayout.appendChild(columns);
		gridAttributeLayout.appendChild(rows);

		column = new Column();
		column.setWidth("30%");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("70%");
		columns.appendChild(column);

		confirmPanel = new ConfirmPanel();
		btnDelete = confirmPanel.createButton(ConfirmPanel.A_DELETE);
		btnDelete.setEnabled(false);

		btnDownload = new Button();
		btnDownload.setTooltiptext(DMSConstant.TTT_DOWNLOAD);
		btnDownload.setImage(ThemeManager.getThemeResource("images/Export24.png"));

		btnVersionUpload = new Button();
		btnVersionUpload.setTooltiptext(DMSConstant.TTT_UPLOAD_VERSION);
		btnVersionUpload.setImage(ThemeManager.getThemeResource("images/Assignment24.png"));

		btnRequery = confirmPanel.createButton(ConfirmPanel.A_REFRESH);

		btnClose = confirmPanel.createButton(ConfirmPanel.A_CANCEL);
		btnClose.setStyle("float:right;");

		btnEdit = new Button();
		btnEdit.setTooltiptext(DMSConstant.TTT_EDIT);
		btnEdit.setImageContent(Utils.getImage("Edit24.png"));

		btnSave = new Button();
		btnSave.setVisible(false);
		btnSave.setTooltiptext(DMSConstant.TTT_SAVE);
		btnSave.setImageContent(Utils.getImage("Save24.png"));

		btnSave.addEventListener(Events.ON_CLICK, this);
		btnDelete.addEventListener(Events.ON_CLICK, this);
		btnDownload.addEventListener(Events.ON_CLICK, this);
		btnRequery.addEventListener(Events.ON_CLICK, this);
		btnClose.addEventListener(Events.ON_CLICK, this);
		btnEdit.addEventListener(Events.ON_CLICK, this);
		btnVersionUpload.addEventListener(Events.ON_CLICK, this);

		South south = new South();
		rows.appendChild(row);

		panelFooterButtons.appendChild(btnVersionUpload);
		panelFooterButtons.appendChild(btnDelete);
		panelFooterButtons.appendChild(btnRequery);
		panelFooterButtons.appendChild(btnDownload);
		panelFooterButtons.appendChild(btnClose);
		panelFooterButtons.setStyle("display: inline-flex; padding-top: 5px;");

		btnVersionUpload.setImageContent(Utils.getImage("uploadversion24.png"));
		btnDelete.setImageContent(Utils.getImage("Delete24.png"));
		btnRequery.setImageContent(Utils.getImage("Refresh24.png"));
		btnDownload.setImageContent(Utils.getImage("Downloads24.png"));
		btnClose.setImageContent(Utils.getImage("Close24.png"));

		panelAttribute.appendChild(panelFooterButtons);
		mainLayout.appendChild(south);

		btnVersionUpload.setDisabled(!isWindowAccess);
		btnEdit.setDisabled(!isWindowAccess);
	} // init

	/**
	 * initialize version history components
	 */
	private void initVersionHistory()
	{
		Components.removeAllChildren(tabpanelVersionHitory);
		Grid versionGrid = new Grid();
		versionGrid.setHeight("100%");
		versionGrid.setWidth("100%");
		this.setZclass("none");
		versionGrid.setStyle("position:relative; float: right; overflow-y: auto;");
		this.setStyle("position:relative; float: right; height: 100%; overflow: auto;");

		tabpanelVersionHitory.appendChild(versionGrid);

		Columns columns = new Columns();

		Column column = new Column();
		column.setWidth("30%");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("65%");
		columns.appendChild(column);

		Rows rows = new Rows();
		Row row = null;

		versionGrid.appendChild(columns);
		versionGrid.setHeight("100%");
		versionGrid.setWidth("98%");
		versionGrid.appendChild(rows);
		versionGrid.setZclass("none");

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			MDMSContent versionContent = null;
			MDMSAssociation dmsAssociation = Utils.getAssociationFromContent(DMS_Content.getDMS_Content_ID(), null);

			pstmt = DB.prepareStatement(DMSConstant.SQL_FETCH_CONTENT_VERSION_LIST, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, null);
			pstmt.setInt(1, dmsAssociation.getDMS_Content_Related_ID());
			pstmt.setInt(2, MDMSAssociationType.VERSION_ID);
			pstmt.setInt(3, dmsAssociation.getDMS_Content_Related_ID());

			rs = pstmt.executeQuery();

			if (rs.isBeforeFirst())
			{
				while (rs.next())
				{
					versionContent = new MDMSContent(Env.getCtx(), rs.getInt(1), null);

					String filename = dms.getThumbnailURL(versionContent, "150");
					if (Util.isEmpty(filename))
					{
						MImage mImage = Utils.getMimetypeThumbnail(versionContent.getDMS_MimeType_ID());
						byte[] imgByteData = mImage.getData();

						if (imgByteData != null)
						{
							imageVersion = new AImage(versionContent.getName(), imgByteData);
						}
					}
					else
					{
						imageVersion = new AImage(filename);
					}

					viewerComponenet = new DMSViewerComponent(dms, versionContent, imageVersion, false, dmsAssociation);
					viewerComponenet.addEventListener(Events.ON_DOUBLE_CLICK, this);

					viewerComponenet.setDheight(100);
					viewerComponenet.setDwidth(100);

					viewerComponenet.getfLabel().setStyle("text-overflow: ellipsis; white-space: nowrap; overflow: hidden; float: right;");

					Cell cell = new Cell();
					cell.setRowspan(1);
					cell.appendChild(new Label(DMSConstant.MSG_CREATED + ": " + versionContent.getCreated()));
					cell.appendChild(new Label(DMSConstant.MSG_CREATEDBY + ": " + MUser.getNameOfUser(versionContent.getCreatedBy())));
					cell.appendChild(new Label(DMSConstant.MSG_FILESIZE + ": " + versionContent.getDMS_FileSize()));

					row = new Row();
					row.appendChild(viewerComponenet);
					row.appendChild(cell);
					rows.appendChild(row);
				}
			}
			else
			{
				Cell cell = new Cell();
				cell.setColspan(2);
				cell.appendChild(new Label(DMSConstant.MSG_NO_VERSION_DOC_EXISTS));
				row = new Row();
				row.appendChild(cell);
				rows.appendChild(row);
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Version listing failure", e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

	} // initVersionHistory

	private void initAttributes()
	{
		Components.removeAllChildren(tabpanelAttribute);
		Grid commGrid = GridFactory.newGridLayout();

		Columns columns = new Columns();

		Column column = new Column();
		column.setWidth("30%");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("70%");
		columns.appendChild(column);

		Rows rows = new Rows();

		commGrid.appendChild(columns);
		commGrid.appendChild(rows);

		txtName = new Textbox();
		txtDesc = new Textbox();

		lblName = new Label(DMSConstant.MSG_NAME);
		lblDesc = new Label(DMSConstant.MSG_DESCRIPTION);

		txtName.setWidth("100%");
		txtDesc.setWidth("100%");

		parent_Content = new MDMSContent(Env.getCtx(), Utils.getDMS_Content_Related_ID(DMS_Content), null);
		txtName.setValue(parent_Content.getName().substring(0, parent_Content.getName().lastIndexOf(".")));
		txtDesc.setValue(DMS_Content.getDescription());
		txtName.setMaxlength(DMSConstant.MAX_FILENAME_LENGTH);

		txtName.setEnabled(false);
		txtDesc.setEnabled(false);

		Row row = new Row();
		row.appendChild(lblName);
		row.appendChild(txtName);
		rows.appendChild(row);

		row = new Row();
		row.appendChild(lblDesc);
		row.appendChild(txtDesc);
		rows.appendChild(row);
		tabpanelAttribute.appendChild(commGrid);

		ASIPanel = new WDLoadASIPanel(DMS_Content.getDMS_ContentType_ID(), DMS_Content.getM_AttributeSetInstance_ID());
		ASIPanel.setEditableAttribute(false);
		ASIPanel.appendChild(btnEdit);
		ASIPanel.appendChild(btnSave);
		btnSave.setVisible(false);
		tabpanelAttribute.appendChild(ASIPanel);
	} // initAttributes

	/*
	 * (non-Javadoc)
	 * @see
	 * org.zkoss.zk.ui.event.EventListener#onEvent(org.zkoss.zk.ui.event.Event)
	 */
	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget().equals(btnEdit))
		{
			txtName.setEnabled(true);
			txtDesc.setEnabled(true);
			ASIPanel.setEditableAttribute(true);
			btnSave.setVisible(true);
		}
		else if (event.getTarget().equals(btnSave))
		{
			if (!txtName.getValue().equals(parent_Content.getName().substring(0, parent_Content.getName().lastIndexOf("."))))
			{
				String error = Utils.isValidFileName(txtName.getValue(), false);
				if (!Util.isEmpty(error, true))
					throw new WrongValueException(txtName, error);

				dms.updateContent(txtName.getValue(), DMS_Content);
			}

			ASIPanel.saveAttributes();
			ASIPanel.setEditableAttribute(false);
			btnSave.setVisible(false);
			txtName.setEnabled(false);
			txtDesc.setEnabled(false);

			if ((Util.isEmpty(DMS_Content.getDescription()) && !Util.isEmpty(txtDesc.getValue()))
					|| (!Util.isEmpty(DMS_Content.getDescription()) && !DMS_Content.getDescription().equals(txtDesc.getValue())))
			{
				DMS_Content.setDescription(txtDesc.getValue());
				DMS_Content.save();
			}

			DMS_Content.load(null);

			Events.sendEvent(new Event("onRenameComplete", this));
			tabBox.setSelectedTab((Tab) tabBox.getSelectedTab());
			tabBox.getSelectedTab().setLabel(DMS_Content.getName());

			refreshPanel();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			tabBox.getSelectedTab().close();
		}
		else if (event.getTarget().equals(btnDownload))
		{
			// Resolve NPE after file rename and try to download
			DMS_Content.load(DMS_Content.get_TrxName());
			//
			DMS_ZK_Util.downloadDocument(dms, DMS_Content);
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_DELETE))
		{
			dms.deleteContentWithDocument(DMS_Content);

			tabBox.getSelectedTab().close();
		}
		else if (event.getTarget().equals(btnVersionUpload))
		{
			final Tab tab = (Tab) tabBox.getSelectedTab();
			final WDAttributePanel panel = this;

			WUploadContent uploadContent = new WUploadContent(dms, DMS_Content, true, tableId, recordId);
			uploadContent.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

				@Override
				public void onEvent(Event arg0) throws Exception
				{
					Events.sendEvent(new Event("onUploadComplete", panel));
					tabBox.setSelectedTab(tab);
				}
			});
			uploadContent.addEventListener(Events.ON_CLOSE, this);
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_REFRESH))
		{
			refreshPanel();
		}
		else if (event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			DMS_ZK_Util.downloadDocument(dms, DMSViewerComp.getDMSContent());
		}
	} // onEvent

	/**
	 * 
	 */
	public void refreshPanel()
	{
		initAttributes();
		initVersionHistory();
	}

}
