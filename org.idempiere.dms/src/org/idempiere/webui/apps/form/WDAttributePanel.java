package org.idempiere.webui.apps.form;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.NumberBox;
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
import org.adempiere.webui.window.FDialog;
import org.apache.commons.io.FileUtils;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MImage;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.dms.storage.RelationalContentManager;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.zkoss.image.AImage;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Image;
import org.zkoss.zul.North;
import org.zkoss.zul.South;

public class WDAttributePanel extends Panel implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long		serialVersionUID		= 5200959427619624094L;
	private static CLogger			log						= CLogger.getCLogger(WDAttributePanel.class);

	private Panel					panelAttribute			= new Panel();
	private Panel					panelButtons			= new Panel();
	private Borderlayout			mainLayout				= new Borderlayout();

	private Tabbox					tabBoxAttribute			= new Tabbox();

	private Tabs					tabsAttribute			= new Tabs();

	private Tab						tabAttribute			= new Tab();
	private Tab						tabVersionHistory		= new Tab();

	private Tabpanels				tabpanelsAttribute		= new Tabpanels();

	private Tabpanel				tabpanelAttribute		= new Tabpanel();
	private Tabpanel				tabpanelVersionHitory	= new Tabpanel();

	private Grid					gridAttributeLayout		= new Grid();

	private Label					lblStatus				= null;

	private Button					btnDelete				= null;
	private Button					btnRequery				= null;
	private Button					btnClose				= null;
	private Button					btnDownload				= null;
	private Button					btnEdit					= null;
	private Button					btnSave					= null;
	private Button					btnVersionUpload		= null;

	private ConfirmPanel			confirmPanel			= null;

	private MAttributeSetInstance	m_masi;
	private MDMSContent				DMS_Content				= null;

	private IFileStorageProvider	fileStorageProvider		= null;
	private IContentManager			contentManager			= null;
	private IThumbnailProvider		thumbnailProvider		= null;

	private WDocumentViewer			viewer					= null;

	private int						m_M_AttributeSetInstance_ID;

	private static final String		SQL_FETCH_VERSION_LIST	= "SELECT DISTINCT DMS_Content_ID FROM DMS_Association a WHERE DMS_Content_Related_ID= ? "
																	+ " AND a.DMS_AssociationType_ID = (SELECT DMS_AssociationType_ID FROM DMS_AssociationType "
																	+ " WHERE NAME='Version') UNION SELECT DMS_Content_ID FROM DMS_Content WHERE DMS_Content_ID = ?"
																	+ " AND ContentBaseType <> 'DIR' order by DMS_Content_ID";

	public WDAttributePanel(I_DMS_Content DMS_Content, WDocumentViewer viewer)
	{
		fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()));

		if (fileStorageProvider == null)
			throw new AdempiereException("Storage provider is not found");

		contentManager = Utils.getContentManager(RelationalContentManager.KEY);

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found");

		thumbnailProvider = Utils.getThumbnailProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thumbnailProvider == null)
			throw new AdempiereException("thumbnailProvider is not found");

		m_M_AttributeSetInstance_ID = DMS_Content.getM_AttributeSetInstance_ID();

		this.DMS_Content = (MDMSContent) DMS_Content;
		this.viewer = viewer;

		try
		{
			init();
			// initAttributes();
			initVersionHistory();
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "WDMSAsi: " + ex);
		}

	}

	private void init()
	{
		this.appendChild(mainLayout);
		mainLayout.setHeight("100%");
		mainLayout.setWidth("100%");
		this.setHeight("100%");
		this.setWidth("100%");

		North north = new North();
		mainLayout.appendChild(north);
		north.appendChild(panelAttribute);
		north.setHeight("100%");
		mainLayout.appendChild(north);

		lblStatus = new Label();
		ZkCssHelper.appendStyle(lblStatus, "font-weight: bold;");
		ZkCssHelper.appendStyle(lblStatus, "align: center;");
		lblStatus.setValue(MUser.getNameOfUser(DMS_Content.getUpdatedBy()) + " edited at " + DMS_Content.getUpdated());

		panelAttribute.appendChild(lblStatus);
		tabBoxAttribute.appendChild(tabsAttribute);
		tabBoxAttribute.appendChild(tabpanelsAttribute);
		panelAttribute.appendChild(tabBoxAttribute);
		tabBoxAttribute.setMold("accordion");

		tabsAttribute.appendChild(tabAttribute);
		tabsAttribute.appendChild(tabVersionHistory);

		tabAttribute.setLabel("Attribute Set");
		tabVersionHistory.setLabel("Version History");

		tabpanelsAttribute.appendChild(tabpanelAttribute);
		tabpanelsAttribute.setHeight("600px");

		tabpanelsAttribute.appendChild(tabpanelVersionHitory);
		tabpanelAttribute.setHeight("550px");

		tabpanelAttribute.appendChild(gridAttributeLayout);
		tabVersionHistory.setWidth("100%");

		Columns columns = new Columns();
		Column column = new Column();
		
		Rows rows = new Rows();
		Row row = new Row();
		
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

		btnDownload = new Button();
		btnDownload.setTooltiptext("Download");
		btnDownload.setImage(ThemeManager.getThemeResource("images/Export24.png"));

		btnVersionUpload = new Button();
		btnVersionUpload.setTooltiptext("Upload Version");
		btnVersionUpload.setImage(ThemeManager.getThemeResource("images/Assignment24.png"));

		btnRequery = confirmPanel.createButton(ConfirmPanel.A_REFRESH);

		btnClose = confirmPanel.createButton(ConfirmPanel.A_CANCEL);

		btnEdit = new Button();
		btnEdit.setTooltiptext("Edit");
		btnEdit.setImage(ThemeManager.getThemeResource("images/Editor24.png"));

		btnSave = new Button();
		btnSave.setVisible(false);
		btnSave.setTooltiptext("Save");
		btnSave.setImage(ThemeManager.getThemeResource("images/Save24.png"));

		btnSave.addEventListener(Events.ON_CLICK, this);
		btnDelete.addEventListener(Events.ON_CLICK, this);
		btnDownload.addEventListener(Events.ON_CLICK, this);
		btnRequery.addEventListener(Events.ON_CLICK, this);
		btnClose.addEventListener(Events.ON_CLICK, this);
		btnEdit.addEventListener(Events.ON_CLICK, this);
		btnVersionUpload.addEventListener(Events.ON_CLICK, this);

		South south = new South();
		panelButtons.appendChild(btnEdit);
		panelButtons.appendChild(btnSave);
		panelButtons.appendChild(btnDelete);
		panelButtons.appendChild(btnRequery);
		panelButtons.appendChild(btnDownload);
		panelButtons.appendChild(btnVersionUpload);
		panelButtons.appendChild(btnClose);
		
		panelAttribute.appendChild(panelButtons);
		mainLayout.appendChild(south);
		
	}

	private void initVersionHistory()
	{
		Components.removeAllChildren(tabpanelVersionHitory);
		Grid versionGrid = new Grid();
		versionGrid.setHeight("100%");
		versionGrid.setWidth("100%");
		versionGrid.setStyle("position:relative; float: right;overflow: auto;");
		this.setStyle("position:relative; float: right;overflow: auto;");

		tabpanelVersionHitory.appendChild(versionGrid);

		Columns columns = new Columns();

		Column column = new Column();
		column.setWidth("50%");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("50%");

		columns.appendChild(column);

		Rows rows = new Rows();
		Row row = null;

		versionGrid.appendChild(columns);
		versionGrid.appendChild(rows);

		try
		{
			MDMSContent versionContent = null;
			Label labelVersion = null;

			int DMS_Association_ID = DB.getSQLValue(null,
					"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ?",
					DMS_Content.getDMS_Content_ID());

			MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);

			PreparedStatement pstmt = DB.prepareStatement(SQL_FETCH_VERSION_LIST, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE, null);

			pstmt.setInt(1, dmsAssociation.getDMS_Content_Related_ID());
			pstmt.setInt(2, dmsAssociation.getDMS_Content_Related_ID());

			ResultSet rs = pstmt.executeQuery();

			if (rs.next())
			{
				rs.beforeFirst();

				while (rs.next())
				{
					versionContent = new MDMSContent(Env.getCtx(), rs.getInt(1), null);
					Image imageVersion = new Image();

					if (Util.isEmpty(thumbnailProvider.getURL(versionContent, "150")))
					{
						MImage mImage = Utils.getMimetypeThumbnail(versionContent.getDMS_MimeType_ID());
						byte[] imgByteData = mImage.getData();

						if (imgByteData != null)
						{
							imageVersion.setContent(new AImage(versionContent.getName(), imgByteData));
						}
					}
					else
					{
						imageVersion.setContent(new AImage(thumbnailProvider.getURL(versionContent, "150")));
					}

					labelVersion = new Label(versionContent.getName() + "     " + versionContent.getCreated());

					row = new Row();
					row.appendChild(imageVersion);
					row.appendChild(labelVersion);
					rows.appendChild(row);
				}
			}
			else
			{
				Cell cell = new Cell();
				cell.setColspan(2);
				cell.appendChild(new Label("No version Documet available."));
				row = new Row();
				row.appendChild(cell);
				rows.appendChild(row);
			}

		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Version listing failure");
		}

	}

	private boolean initAttributes()
	{
		Components.removeAllChildren(tabpanelsAttribute);

		Grid attributeGrid = new Grid();
		attributeGrid.setHeight("100%");
		attributeGrid.setWidth("100%");
		attributeGrid.setStyle("position:relative; float: right;overflow: auto;");
		
		this.setStyle("position:relative; float: right;overflow: auto;");

		tabpanelsAttribute.appendChild(attributeGrid);

		Columns columns = new Columns();

		Column column = new Column();
		column.setWidth("30%");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("70%");

		columns.appendChild(column);

		Rows rows = new Rows();
		Row row = null;

		MAttributeSet as = null;

		m_masi = new MAttributeSetInstance(Env.getCtx(), DMS_Content.getM_AttributeSetInstance_ID(), null);
		as = m_masi.getMAttributeSet();

		if (as == null)
		{
			log.log(Level.SEVERE, "No AttributeSet Found");
			return false;
		}

		MAttribute[] attributes = as.getMAttributes(true);

		for (int i = 0; i < attributes.length; i++)
			addAttributeLine(rows, attributes[i]);

		return true;
	}

	private void addAttributeLine(Rows rows, MAttribute attribute)
	{
		Label label = new Label(attribute.getName());

		if (attribute.getDescription() != null)
			label.setTooltiptext(attribute.getDescription());

		Row row = rows.newRow();
		row.appendChild(label.rightAlign());

		if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType()))
		{
			MAttributeValue[] values = attribute.getMAttributeValues(); // optional
																		// =
																		// null
			Listbox editor = new Listbox();
			editor.setMold("select");
			for (MAttributeValue value : values)
			{
				ListItem item = new ListItem(value != null ? value.getName() : "", value);
				editor.appendChild(item);
			}
			row.appendChild(editor);
			editor.setHflex("1");
			setListAttribute(attribute, editor);
		}
		else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attribute.getAttributeValueType()))
		{
			NumberBox editor = new NumberBox(false);
			setNumberAttribute(attribute, editor);
			row.appendChild(editor);
			editor.setHflex("1");
		}
		else if(MAttribute.ATTRIBUTEVALUETYPE_Reference.equals(attribute.getAD_Reference()))
		{
			
		}
		else
		// Text Field
		{
			Textbox editor = new Textbox();
			setStringAttribute(attribute, editor);
			row.appendChild(editor);
			editor.setHflex("1");
		}

	}

	private void setStringAttribute(MAttribute attribute, Textbox editor)
	{
		MAttributeInstance instance = attribute.getMAttributeInstance(m_M_AttributeSetInstance_ID);
		if (instance != null)
			editor.setText(instance.getValue());
	}

	private void setNumberAttribute(MAttribute attribute, NumberBox editor)
	{
		MAttributeInstance instance = attribute.getMAttributeInstance(m_M_AttributeSetInstance_ID);
		if (instance != null)
			editor.setValue(instance.getValueNumber());
		else
			editor.setValue(Env.ZERO);
	}

	private void setListAttribute(MAttribute attribute, Listbox editor)
	{
		boolean found = false;
		MAttributeInstance instance = attribute.getMAttributeInstance(m_M_AttributeSetInstance_ID);
		MAttributeValue[] values = attribute.getMAttributeValues(); // optional
																	// = null
		if (instance != null)
		{
			for (int i = 0; i < values.length; i++)
			{
				if (values[i] != null && values[i].getM_AttributeValue_ID() == instance.getM_AttributeValue_ID())
				{
					editor.setSelectedIndex(i);
					found = true;
					break;
				}
			}
			if (found)
			{
				if (log.isLoggable(Level.FINE))
					log.fine("Attribute=" + attribute.getName() + " #" + values.length + " - found: " + instance);
			}
			else
			{
				log.warning("Attribute=" + attribute.getName() + " #" + values.length + " - NOT found: " + instance);
			}
		} // setComboBox
		else if (log.isLoggable(Level.FINE))
			log.fine("Attribute=" + attribute.getName() + " #" + values.length + " no instance");
	}

	@Override
	public void onEvent(Event event) throws Exception
	{

		if (event.getTarget().equals(btnEdit))
		{

			btnSave.setVisible(true);
		}
		else if (event.getTarget().equals(btnSave))
		{
		}
		else if (event.getTarget().getId().equals(confirmPanel.A_CANCEL))
		{
			viewer.tabBox.getSelectedTab().close();
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
			viewer.tabBox.getSelectedTab().close();
			viewer.renderViewer(viewer.currDMSContent);
		}
		else if (event.getTarget().equals(btnVersionUpload))
		{
			final Tab tab = (Tab) viewer.tabBox.getSelectedTab();

			WUploadContent uploadContent = new WUploadContent(DMS_Content);
			uploadContent.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

				@Override
				public void onEvent(Event arg0) throws Exception
				{
					viewer.tabBox.setSelectedTab(tab);
				}
			});
		}
		else if (event.getTarget().getId().equals(confirmPanel.A_REFRESH))
		{
			initVersionHistory();
		}

	}

}
