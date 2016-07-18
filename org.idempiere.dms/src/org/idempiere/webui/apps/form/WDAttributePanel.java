
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
import java.io.FileNotFoundException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.window.FDialog;
import org.apache.commons.io.FileUtils;
import org.compiere.model.MImage;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.componenet.DMSViewerComponent;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;
import org.zkoss.image.AImage;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Cell;
import org.zkoss.zul.South;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

public class WDAttributePanel extends Panel implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long		serialVersionUID		= 5200959427619624094L;
	private static CLogger			log						= CLogger.getCLogger(WDAttributePanel.class);

	private Panel					panelAttribute			= new Panel();
	private Panel					panelFooterButtons		= new Panel();
	private Borderlayout			mainLayout				= new Borderlayout();

	private Tabbox					tabBoxAttribute			= new Tabbox();

	private Tabs					tabsAttribute			= new Tabs();

	private Tab						tabAttribute			= new Tab();
	private Tab						tabVersionHistory		= new Tab();

	private Tabpanels				tabpanelsAttribute		= new Tabpanels();

	private Tabpanel				tabpanelAttribute		= new Tabpanel();
	private Tabpanel				tabpanelVersionHitory	= new Tabpanel();

	private Grid					gridAttributeLayout		= new Grid();
	
	private Grid					grid					= new Grid();

	private Label					lblStatus				= null;

	private Button					btnDelete				= null;
	private Button					btnRequery				= null;
	private Button					btnClose				= null;
	private Button					btnDownload				= null;
	private Button					btnEdit					= null;
	private Button					btnSave					= null;
	private Button					btnVersionUpload		= null;

	private AImage					imageVersion			= null;

	private ConfirmPanel			confirmPanel			= null;

	private MDMSContent				DMS_Content				= null;

	private DMSViewerComponent		viewerComponenet		= null;

	private IFileStorageProvider	fileStorageProvider		= null;
	private IContentManager			contentManager			= null;
	private IThumbnailProvider		thumbnailProvider		= null;
	private IIndexSearcher			indexSeracher			= null;

	private Tabbox					tabBox					= null;

	private WDLoadASIPanel			ASIPanel				= null;

	private int						m_M_AttributeSetInstance_ID;
	private int						tableId					= 0;
	private int						recordId				= 0;

	private static final String		SQL_FETCH_VERSION_LIST	= "SELECT DISTINCT DMS_Content_ID FROM DMS_Association a WHERE DMS_Content_Related_ID= ? "
																	+ " AND a.DMS_AssociationType_ID = (SELECT DMS_AssociationType_ID FROM DMS_AssociationType "
																	+ " WHERE NAME='Version') UNION SELECT DMS_Content_ID FROM DMS_Content WHERE DMS_Content_ID = ?"
																	+ " AND ContentBaseType <> 'DIR' order by DMS_Content_ID DESC";

	public WDAttributePanel(I_DMS_Content DMS_Content, Tabbox tabBox, int tableID, int recordID)
	{
		fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);

		if (fileStorageProvider == null)
			throw new AdempiereException("Storage provider is not found");

		contentManager = Utils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found");

		thumbnailProvider = Utils.getThumbnailProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thumbnailProvider == null)
			throw new AdempiereException("thumbnailProvider is not found");

		m_M_AttributeSetInstance_ID = DMS_Content.getM_AttributeSetInstance_ID();

		this.DMS_Content = (MDMSContent) DMS_Content;
		this.tabBox = tabBox;
		this.tableId = tableID;
		this.recordId = recordID;

		try
		{
			init();
			initAttributes();
			initVersionHistory();
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "WDMSAsi: ", ex);
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
		tabBoxAttribute.setHeight("100%");
		tabBoxAttribute.setWidth("100%");

		tabsAttribute.appendChild(tabAttribute);
		tabsAttribute.appendChild(tabVersionHistory);

		tabAttribute.setLabel("Attribute Set");
		tabAttribute.setWidth("100%");
		tabVersionHistory.setLabel("Version History");

		tabpanelsAttribute.appendChild(tabpanelAttribute);
		// tabpanelsAttribute.setStyle("display: flex;");
		tabpanelsAttribute.setHeight("100%");
		tabpanelsAttribute.setWidth("100%");

		tabpanelsAttribute.appendChild(tabpanelVersionHitory);
		tabpanelVersionHitory.setHeight("550px");
		tabpanelAttribute.setHeight("100%");

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
		btnDownload.setTooltiptext("Download");
		btnDownload.setImage(ThemeManager.getThemeResource("images/Export24.png"));

		btnVersionUpload = new Button();
		btnVersionUpload.setTooltiptext("Upload Version");
		btnVersionUpload.setImage(ThemeManager.getThemeResource("images/Assignment24.png"));

		btnRequery = confirmPanel.createButton(ConfirmPanel.A_REFRESH);

		btnClose = confirmPanel.createButton(ConfirmPanel.A_CANCEL);
		btnClose.setStyle("float:right;");

		btnEdit = new Button();
		btnEdit.setTooltiptext("Edit");
		btnEdit.setImageContent(Utils.getImage("Edit24.png"));

		btnSave = new Button();
		btnSave.setVisible(false);
		btnSave.setTooltiptext("Save");
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
		
		btnVersionUpload.setImageContent(Utils.getImage("uploadversion24.png"));
		btnDelete.setImageContent(Utils.getImage("Delete24.png"));
		btnRequery.setImageContent(Utils.getImage("Refresh24.png"));
		btnDownload.setImageContent(Utils.getImage("Downloads24.png"));
		btnClose.setImageContent(Utils.getImage("Close24.png"));

		panelAttribute.appendChild(panelFooterButtons);
		mainLayout.appendChild(south);

	}

	/**
	 * initialize version history components
	 */
	private void initVersionHistory()
	{
		Components.removeAllChildren(tabpanelVersionHitory);
		Grid versionGrid = new Grid();
		versionGrid.setHeight("65%");
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

		columns.appendChild(column);

		Rows rows = new Rows();
		Row row = null;

		versionGrid.appendChild(columns);
		versionGrid.setHeight("100%");
		versionGrid.appendChild(rows);
		versionGrid.setZclass("none");

		try
		{
			MDMSContent versionContent = null;

			int DMS_Association_ID = DB.getSQLValue(null,
					"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ?",
					DMS_Content.getDMS_Content_ID());

			MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);

			PreparedStatement pstmt = DB.prepareStatement(SQL_FETCH_VERSION_LIST, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE, null);

			pstmt.setInt(1, dmsAssociation.getDMS_Content_Related_ID());
			pstmt.setInt(2, dmsAssociation.getDMS_Content_Related_ID());

			ResultSet rs = pstmt.executeQuery();

			if (rs.isBeforeFirst())
			{
				while (rs.next())
				{
					versionContent = new MDMSContent(Env.getCtx(), rs.getInt(1), null);

					if (Util.isEmpty(thumbnailProvider.getURL(versionContent, "150")))
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
						imageVersion = new AImage(thumbnailProvider.getURL(versionContent, "150"));
					}

					viewerComponenet = new DMSViewerComponent(versionContent, imageVersion, false,dmsAssociation);
					viewerComponenet.addEventListener(Events.ON_DOUBLE_CLICK, this);

					viewerComponenet.setDheight(100);
					viewerComponenet.setDwidth(100);

					viewerComponenet.getfLabel().setStyle(
							"text-overflow: ellipsis; white-space: nowrap; overflow: hidden; float: right;");
					
					Cell cell = new Cell();
					cell.setRowspan(1);
					
					Label created = new Label("Created: " + versionContent.getCreated());
					Label createdby = new Label("Created By: " + MUser.getNameOfUser(versionContent.getCreatedBy()));
					Label size = new Label("Size: " + versionContent.getDMS_FileSize());
					
					cell.appendChild(created);
					cell.appendChild(createdby);
					cell.appendChild(size);
					
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
				cell.appendChild(new Label("No version Document available."));
				row = new Row();
				row.appendChild(cell);
				rows.appendChild(row);
			}

		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Version listing failure", e);
		}

	}

	private void initAttributes()
	{
		Components.removeAllChildren(tabpanelAttribute);
		ASIPanel = new WDLoadASIPanel(DMS_Content.getDMS_ContentType_ID(), m_M_AttributeSetInstance_ID);
		ASIPanel.setEditableAttribute(false);
		tabpanelAttribute.appendChild(ASIPanel);
		ASIPanel.appendChild(btnEdit);
		ASIPanel.appendChild(btnSave);
	}

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
			ASIPanel.setEditableAttribute(true);
			btnSave.setVisible(true);
		}
		else if (event.getTarget().equals(btnSave))
		{
			indexSeracher = ServiceUtils.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));

			if (indexSeracher == null)
				throw new AdempiereException("Index server is not found.");

			ASIPanel.saveAttributes();
			btnSave.setVisible(false);
			ASIPanel.setEditableAttribute(false);

			int DMS_Content_ID = DB.getSQLValue(null,
					"SELECT DMS_Content_Related_ID FROM DMS_Association WHERE DMS_Content_ID = ?",
					DMS_Content.getDMS_Content_ID());

			if (DMS_Content_ID <= 0)
				DMS_Content_ID = DMS_Content.getDMS_Content_ID();
			else
			{
				MDMSContent dirContent = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
				if (dirContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
				{
					DMS_Content_ID = DMS_Content.getDMS_Content_ID();
				}
			}

			int DMS_Association_ID = DB.getSQLValue(null,
					"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ?", DMS_Content_ID);

			MDMSContent DMSContent = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
			MDMSAssociation DMSAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);

			Map<String, Object> solrValue = Utils.createIndexMap(DMSContent, DMSAssociation);
			indexSeracher.deleteIndex(DMS_Content_ID);
			indexSeracher.indexContent(solrValue);
		}
		else if (event.getTarget().getId().equals(confirmPanel.A_CANCEL))
		{
			tabBox.getSelectedTab().close();
		}
		else if (event.getTarget().equals(btnDownload))
		{
			File document = fileStorageProvider.getFile(contentManager.getPath(DMS_Content));
			if (document.exists())
			{
				AMedia media = new AMedia(document, "application/octet-stream", null);
				Filedownload.save(media);
			}
			else
				FDialog.warn(0, "Docuement is not available to download.");
		}
		else if (event.getTarget().getId().equals(confirmPanel.A_DELETE))
		{
			File document = fileStorageProvider.getFile(contentManager.getPath(DMS_Content));

			if (document.exists())
			{
				document.delete();
			}

			File thumbnails = new File(thumbnailProvider.getURL(DMS_Content, null));

			if (thumbnails.exists())
				FileUtils.deleteDirectory(thumbnails);

			DB.executeUpdate("DELETE FROM DMS_Association WHERE DMS_Content_ID = ?", DMS_Content.getDMS_Content_ID(),
					null);
			DB.executeUpdate("DELETE FROM DMS_Content WHERE DMS_Content_ID = ?", DMS_Content.getDMS_Content_ID(), null);
			tabBox.getSelectedTab().close();
		}
		else if (event.getTarget().equals(btnVersionUpload))
		{
			final Tab tab = (Tab) tabBox.getSelectedTab();
			final WDAttributePanel panel = this;

			WUploadContent uploadContent = new WUploadContent(DMS_Content, true, tableId, recordId);
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
		else if (event.getTarget().getId().equals(confirmPanel.A_REFRESH))
		{
			initAttributes();
			initVersionHistory();
		}
		else if (event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			downloadSelectedComponent(DMSViewerComp);
		}

	}

	private void downloadSelectedComponent(DMSViewerComponent DMSViewerComp) throws FileNotFoundException
	{
		IFileStorageProvider fileStorgProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);

		if (fileStorgProvider == null)
			throw new AdempiereException("Storage provider is not define on clientInfo.");

		File document = fileStorgProvider.getFile(contentManager.getPath(DMSViewerComp.getDMSContent()));

		if (document.exists())
		{
			AMedia media = new AMedia(document, "application/octet-stream", null);
			Filedownload.save(media);
		}
		else
		{
			FDialog.warn(0, "Docuement is not available to download.");
		}
	}
}
