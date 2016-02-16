package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Datebox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.window.FDialog;
import org.apache.commons.io.FileUtils;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.dms.storage.RelationalContentManager;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.idempiere.model.MDMSStatus;
import org.idempiere.model.X_DMS_ContentType;
import org.idempiere.model.X_DMS_Status;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;

public class WDocumentEditor extends Window implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long		serialVersionUID		= 7234966943628502177L;
	public static CLogger			log						= CLogger.getCLogger(WDocumentEditor.class);

	private Label					lblStatus				= null;
	private Label					lblDocName				= null;
	private Label					lblContentCategory		= null;
	private Label					lblDocStatus			= null;
	private Label					lblMetaData				= null;
	private Label					lblDocReported			= null;
	private Label					lblBPartner				= null;

	private Textbox					txtDocName				= null;

	private WTableDirEditor			lstboxContentCategory	= null;
	private WTableDirEditor			lstboxDocStatus			= null;

	private Button					btnDelete				= null;
	private Button					btnRequery				= null;
	private Button					btnClose				= null;
	private Button					btnDownload				= null;
	private Button					btnEdit					= null;
	private Button					btnSave					= null;
	private Button					btnVersionUpload		= null;

	private Datebox					dbContentReported		= null;

	private WSearchEditor			seBPartner				= null;
	private ConfirmPanel			confirmPanel			= null;
	private WDocumentViewer			viewer					= null;
	private Tabpanel				tabDataPanel			= null;
	private MDMSContent				mDMSContent				= null;

	private IFileStorageProvider	fileStorageProvider		= null;
	private IContentManager			contentManager			= null;
	private IThumbnailProvider		thumbnailProvider		= null;

	public WDocumentEditor(WDocumentViewer viewer, File document_preview, Tabpanel tabDataPanel,
			MDMSContent mdms_content)
	{
		this.viewer = viewer;
		this.tabDataPanel = tabDataPanel;
		this.mDMSContent = mdms_content;

		fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()));

		if (fileStorageProvider == null)
			throw new AdempiereException("Storage provider is not found");

		contentManager = Utils.getContentManager(RelationalContentManager.KEY);

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found");

		thumbnailProvider = Utils.getThumbnailProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thumbnailProvider == null)
			throw new AdempiereException("thumbnailProvider is not found");

		intiform(document_preview);
	}

	private void intiform(File document_preview)
	{
		Cell preview_cell = new Cell();

		lblStatus = new Label();

		lblDocName = new Label("Name :");
		txtDocName = new Textbox();

		lblContentCategory = new Label("Category:");

		int columnID = MColumn.getColumn_ID(X_DMS_ContentType.Table_Name, X_DMS_Status.COLUMNNAME_DMS_ContentType_ID);
		MLookup lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, columnID, DisplayType.TableDir, Env.getLanguage(Env.getCtx()),
					X_DMS_Status.COLUMNNAME_DMS_ContentType_ID, 0, true, null);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Content Type Fetching Failure: " + e.getLocalizedMessage());
			throw new AdempiereException("Content Type Fetching Failure: " + e.getLocalizedMessage());
		}

		lstboxContentCategory = new WTableDirEditor(X_DMS_Status.COLUMNNAME_DMS_ContentType_ID, true, false, true,
				lookup);

		lblDocStatus = new Label("Status:");

		columnID = MColumn.getColumn_ID(X_DMS_Status.Table_Name, X_DMS_Status.COLUMNNAME_DMS_Status_ID);

		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, columnID, DisplayType.TableDir, Env.getLanguage(Env.getCtx()),
					X_DMS_Status.COLUMNNAME_DMS_Status_ID, 0, true, null);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Content status Fetching Failure: " + e.getLocalizedMessage());
			throw new AdempiereException("Content status Fetching Failure: " + e.getLocalizedMessage());
		}
		lstboxDocStatus = new WTableDirEditor(X_DMS_Status.COLUMNNAME_DMS_Status_ID, true, false, true, lookup);

		lblMetaData = new Label("Meta");

		lblDocReported = new Label("Report Date:");
		dbContentReported = new Datebox();

		lblBPartner = new Label("Partner:");

		lookup = MLookupFactory.get(Env.getCtx(), 0, 0, 2762, DisplayType.Search);
		seBPartner = new WSearchEditor(lookup, Msg.translate(Env.getCtx(), "C_BPartner_ID"), "", true, true, true);
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

		Grid gridData = GridFactory.newGridLayout();
		gridData.makeNoStrip();
		gridData.setOddRowSclass("even");
		gridData.setZclass("none");
		gridData.setStyle("overflow: auto; position:relative; float: right;");
		gridData.setWidth("100%");
		gridData.setHeight("100%");

		Rows rows = new Rows();
		gridData.appendChild(rows);

		Row row = new Row();
		Cell cell = new Cell();
		cell.setColspan(3);
		cell.appendChild(lblStatus);
		ZkCssHelper.appendStyle(lblStatus, "font-weight: bold;");
		ZkCssHelper.appendStyle(lblStatus, "align: center;");

		row.appendChild(cell);
		rows.appendChild(row);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblDocName);
		cell = new Cell();
		cell.appendChild(txtDocName);
		txtDocName.setWidth("100%");
		cell.setColspan(2);
		row.appendChild(cell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblContentCategory);
		cell = new Cell();
		lstboxContentCategory.getComponent().setWidth("100%");
		cell.appendChild(lstboxContentCategory.getComponent());
		cell.setColspan(2);
		row.appendChild(cell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblDocStatus);
		cell = new Cell();
		cell.appendChild(lstboxDocStatus.getComponent());
		lstboxDocStatus.getComponent().setWidth("100%");

		cell.setColspan(2);
		row.appendChild(cell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblMetaData);
		ZkCssHelper.appendStyle(lblMetaData, "font-weight: bold;");
		row.appendChild(new Space());

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblDocReported);
		cell = new Cell();
		cell.appendChild(dbContentReported);
		dbContentReported.setWidth("100%");
		cell.setColspan(2);
		row.appendChild(cell);

		row = new Row();
		rows.appendChild(row);
		row.appendChild(lblBPartner);
		cell = new Cell();
		cell.setColspan(2);
		cell.appendChild(seBPartner.getComponent());
		row.appendChild(cell);

		row = new Row();
		cell = new Cell();
		row.setStyle("text-align:right");
		cell.setColspan(2);
		cell.appendChild(btnEdit);
		cell.appendChild(btnSave);
		cell.setAlign("right");
		rows.appendChild(row);
		row.appendCellChild(new Space());
		row.appendChild(cell);

		row.setZclass("none");

		Hbox box = new Hbox();
		row = new Row();
		rows.appendChild(row);

		cell = new Cell();

		cell.setColspan(2);
		box.appendChild(btnDelete);
		box.appendChild(btnRequery);
		box.appendChild(btnDownload);
		box.appendChild(btnVersionUpload);
		cell.appendChild(box);
		row.appendChild(cell);

		cell = new Cell();
		cell.setAlign("right");
		cell.appendChild(btnClose);
		row.appendChild(cell);

		Hbox boxViewSeparator = new Hbox();
		boxViewSeparator.setWidth("100%");
		boxViewSeparator.setHeight("100%");
		preview_cell.setWidth("70%");

		Iframe iframeContentPriview = new Iframe();

		AMedia media = null;
		try
		{
			media = new AMedia(document_preview, null, null);
		}
		catch (FileNotFoundException e)
		{
			log.log(Level.SEVERE, "Document cannot be displayed:" + e.getLocalizedMessage());
			throw new AdempiereException("Document cannot be displayed:" + e.getLocalizedMessage());
		}

		iframeContentPriview.setContent(null);
		iframeContentPriview.setContent(media);
		iframeContentPriview.setWidth("100%");
		iframeContentPriview.setHeight("100%");
		iframeContentPriview.setStyle("overflow: auto;");

		preview_cell.appendChild(iframeContentPriview);
		boxViewSeparator.appendChild(preview_cell);

		cell = new Cell();
		cell.setWidth("100%");
		cell.appendChild(gridData);
		cell.setStyle("trackMouseOver: false");
		boxViewSeparator.appendChild(cell);

		viewer.setStyle("width: 100%; height:100%; overflow: auto;");

		tabDataPanel.appendChild(boxViewSeparator);
		viewer.tabPanels.appendChild(tabDataPanel);
		this.setHeight("100%");
		this.setWidth("100%");
		viewer.appendChild(viewer.tabBox);
		showMetaData(mDMSContent);
		disableShowData(false);
	}

	public void showMetaData(MDMSContent mdms_content)
	{
		File document = fileStorageProvider.getFile(contentManager.getPath(mdms_content));

		if (document.exists())
		{
			lblStatus.setValue(MUser.getNameOfUser(mdms_content.getUpdatedBy()) + " edited at "
					+ mdms_content.getUpdated());
			txtDocName.setValue(mdms_content.getName());

			MDMSContentType content_type = new MDMSContentType(Env.getCtx(), mdms_content.getDMS_ContentType_ID(), null);
			lstboxContentCategory.getComponent().setValue(content_type.getName());

			MDMSStatus dms_status = new MDMSStatus(Env.getCtx(), mdms_content.getDMS_Status_ID(), null);
			lstboxDocStatus.getComponent().setValue(dms_status.getValue());
			dbContentReported.setValue(mdms_content.getCreated());
		}
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget().equals(btnEdit))
		{

			disableShowData(true);
			btnSave.setVisible(true);
		}
		else if (event.getTarget().equals(btnSave))
		{
			mDMSContent.setName(txtDocName.getValue());
			mDMSContent.setDMS_ContentType_ID((Integer) lstboxContentCategory.getValue());
			mDMSContent.setDMS_Status_ID((Integer) lstboxDocStatus.getValue());

			mDMSContent.saveEx();
			btnSave.setVisible(false);
			disableShowData(false);
			showMetaData(mDMSContent);
		}
		else if (event.getTarget().getId().equals(confirmPanel.A_CANCEL))
		{
			viewer.tabBox.getSelectedTab().close();
		}
		else if (event.getTarget().equals(btnDownload))
		{
			File document = fileStorageProvider.getFile(contentManager.getPath(mDMSContent));
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
			File document = fileStorageProvider.getFile(contentManager.getPath(mDMSContent));

			if (document.exists())
			{
				document.delete();
			}

			File thumbnails = new File(thumbnailProvider.getURL(mDMSContent, null));

			if (thumbnails.exists())
				FileUtils.deleteDirectory(thumbnails);

			DB.executeUpdate("DELETE FROM DMS_Association WHERE DMS_Content_ID = ?", mDMSContent.getDMS_Content_ID(),
					null);
			DB.executeUpdate("DELETE FROM DMS_Content WHERE DMS_Content_ID = ?", mDMSContent.getDMS_Content_ID(), null);
			viewer.tabBox.getSelectedTab().close();
			viewer.renderViewer(viewer.currDMSContent);
		}
		else if (event.getTarget().equals(btnVersionUpload))
		{
			final Tab tab = viewer.tabBox.getSelectedTab();

			WUploadContent uploadContent = new WUploadContent(mDMSContent);
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
			showMetaData(mDMSContent);
		}
	}

	public void disableShowData(boolean status)
	{
		txtDocName.setEnabled(status);
		lstboxContentCategory.getComponent().setEnabled(status);
		lstboxDocStatus.getComponent().setEnabled(status);
		dbContentReported.setEnabled(status);
		seBPartner.getComponent().setEnabled(status);
	}

}
