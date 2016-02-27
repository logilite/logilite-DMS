package org.idempiere.webui.apps.form;

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.AdempiereWebUI;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSMimeType;
import org.idempiere.model.X_DMS_Content;
import org.idempiere.model.X_DMS_ContentType;
import org.zkoss.util.media.AMedia;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Space;

public class WUploadContent extends Window implements EventListener<Event>, ValueChangeListener
{

	/**
	 * 
	 */
	private static final long		serialVersionUID		= -6554158380274124479L;
	private static CLogger			log						= CLogger.getCLogger(WUploadContent.class);

	private WTableDirEditor			contentType;

	private Label					lblFile					= new Label();
	private Label					lblContentType			= new Label();
	private Label					lblDesc					= new Label();
	private Label					lblName					= new Label();

	private Textbox					txtDesc					= new Textbox();
	private Textbox					txtName					= new Textbox();

	private Grid					gridView				= GridFactory.newGridLayout();
	private Row						contentTypeRow			= new Row();
	private Row						nameRow					= new Row();

	private Button					btnFileUpload			= new Button();
	private Button					btnClose				= null;
	public Button					btnOk					= null;
	private ConfirmPanel			confirmPanel			= null;

	private AMedia					uploadedMedia			= null;

	private IFileStorageProvider	fileStorgProvider		= null;
	private IThumbnailGenerator		thumbnailGenerator		= null;
	private IContentManager			contentManager			= null;

	private Tabbox					tabBoxAttribute			= new Tabbox();
	private Tabs					tabsAttribute			= new Tabs();
	private Tab						tabAttribute			= new Tab();
	private Tabpanels				tabPanelsAttribute		= new Tabpanels();
	private Tabpanel				tabPanelAttribute		= new Tabpanel();

	private int						DMS_Content_Related_ID	= 0;

	private MDMSContent				DMSContent				= null;

	private boolean					isVersion				= false;
	private WDLoadASIPanel			asiPanel				= null;

	private CharSequence[]			specialCh				= { "!", "@", "#", "$", "%", "^", "&", "*", "`", "~", "+" };

	public WUploadContent(MDMSContent mDMSContent, boolean isVersion)
	{
		this.DMSContent = (MDMSContent) mDMSContent;

		fileStorgProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()));

		if (fileStorgProvider == null)
			throw new AdempiereException("Storage provider is not define on clientInfo.");

		contentManager = Utils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		init();

		if (isVersion)
		{
			contentTypeRow.setVisible(false);
			nameRow.setVisible(false);
			DMS_Content_Related_ID = Utils.getDMS_Content_Related_ID(mDMSContent);
			this.isVersion = isVersion;
			this.setHeight("35%");
			this.setWidth("40%");
		}
	}

	public void init()
	{
		this.setHeight("70%");
		this.setWidth("44%");
		this.setTitle("Upload Content");
		this.setClosable(true);
		this.appendChild(gridView);
		gridView.setStyle("overflow: auto; position:relative;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");

		int Column_ID = MColumn.getColumn_ID(X_DMS_ContentType.Table_Name,
				X_DMS_ContentType.COLUMNNAME_DMS_ContentType_ID);
		MLookup lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, Column_ID, DisplayType.TableDir,
					Env.getLanguage(Env.getCtx()), X_DMS_ContentType.COLUMNNAME_DMS_ContentType_ID, 0, true, "");
			contentType = new WTableDirEditor(X_DMS_ContentType.COLUMNNAME_DMS_ContentType_ID, true, false, true,
					lookup);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Contenttype fetching failure :" + e.getLocalizedMessage());
			throw new AdempiereException("Contenttype fetching failure :" + e.getLocalizedMessage());
		}

		lblFile.setValue(Msg.getMsg(Env.getCtx(), "SelectFile") + "* ");
		lblContentType.setValue("DMS Content Type*");
		btnFileUpload.setLabel("-");
		btnFileUpload.setWidth("100%");
		lblDesc.setValue("Description");
		txtDesc.setMultiline(true);
		txtDesc.setRows(2);
		txtDesc.setWidth("100%");
		txtName.setWidth("100%");
		txtName.addEventListener(Events.ON_CHANGE, this);
		LayoutUtils.addSclass("txt-btn", btnFileUpload);

		Columns columns = new Columns();
		gridView.appendChild(columns);

		Column column = new Column();
		column.setWidth("15%");
		column.setAlign("left");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("40%");
		column.setAlign("left");
		columns.appendChild(column);

		Rows rows = new Rows();
		gridView.appendChild(rows);

		Row row = new Row();
		row.appendChild(lblFile);
		row.appendChild(btnFileUpload);
		rows.appendChild(row);

		lblName.setValue("Name");
		nameRow.appendChild(lblName);
		nameRow.appendChild(txtName);
		rows.appendChild(nameRow);

		contentTypeRow.appendChild(lblContentType);
		contentTypeRow.appendChild(contentType.getComponent());
		contentType.addValueChangeListener(this);
		rows.appendChild(contentTypeRow);

		row = new Row();
		row.appendChild(lblDesc);
		row.appendChild(txtDesc);
		rows.appendChild(row);

		confirmPanel = new ConfirmPanel();
		btnClose = confirmPanel.createButton(ConfirmPanel.A_CANCEL);
		btnOk = confirmPanel.createButton(ConfirmPanel.A_OK);

		row = new Row();
		rows.appendChild(row);
		Cell cell = new Cell();
		cell.setColspan(2);

		tabBoxAttribute.appendChild(tabsAttribute);
		tabBoxAttribute.appendChild(tabPanelsAttribute);

		tabAttribute.setLabel("Attribute Set");
		tabsAttribute.appendChild(tabAttribute);
		tabPanelsAttribute.appendChild(tabPanelAttribute);

		tabBoxAttribute.setMold("accordion");

		cell.appendChild(tabBoxAttribute);
		row.appendChild(cell);

		row = new Row();
		rows.appendChild(row);
		cell = new Cell();
		cell.setAlign("right");
		cell.setColspan(2);
		cell.appendChild(btnOk);
		cell.appendChild(new Space());
		cell.appendChild(btnClose);
		row.appendChild(cell);

		btnFileUpload.setUpload(AdempiereWebUI.getUploadSetting());
		btnFileUpload.addEventListener(Events.ON_UPLOAD, this);
		btnClose.addEventListener(Events.ON_CLICK, this);
		btnOk.addEventListener(Events.ON_CLICK, this);
		addEventListener(Events.ON_UPLOAD, this);
		AEnv.showCenterScreen(this);
	}

	@Override
	public void onEvent(Event e) throws Exception
	{
		if (e instanceof UploadEvent)
		{
			UploadEvent ue = (UploadEvent) e;
			processUploadMedia(ue.getMedia());
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_OK))
		{
			saveUploadedDcoument();
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
			this.detach();
	}

	private void saveUploadedDcoument()
	{
		String fillMandatory = Msg.translate(Env.getCtx(), "FillMandatory");
		MDMSContent uploadedDMSContent = null;

		if (btnFileUpload.getLabel().equalsIgnoreCase("-"))
			throw new WrongValueException(btnFileUpload, fillMandatory);

		if (nameRow.isVisible())
		{
			if (txtName.getValue().equals("") || txtName.getValue().equals(null))
				throw new WrongValueException(txtName, fillMandatory);

			String newFilename = txtName.getValue();

			for (int i = 0; i < specialCh.length; i++)
			{
				if (newFilename.contains(specialCh[i]))
					throw new WrongValueException(txtName, "Invalid File Name.");

			}

			if (newFilename.contains("."))
				if (!newFilename.substring(newFilename.lastIndexOf('.') + 1, newFilename.length()).equals(
						uploadedMedia.getFormat()))
					throw new WrongValueException(txtName, "Invalid File Extension.");
		}

		if (contentTypeRow.isVisible())
			if (contentType.getValue() == null || (Integer) contentType.getValue() == 0)
				throw new WrongValueException(contentType.getComponent(), fillMandatory);

		if (isVersion)
		{
			if (Utils.getMimeTypeID(uploadedMedia) != DMSContent.getDMS_MimeType_ID())
				throw new WrongValueException(btnFileUpload,
						"Mime type not matched, please upload same mime type version document.");
		}

		try
		{
			uploadedDMSContent = new MDMSContent(Env.getCtx(), 0, null);

			if (!isVersion)
			{
				if (!txtName.getValue().contains(uploadedMedia.getFormat()))
				{
					uploadedDMSContent.setName(txtName.getValue() + "." + uploadedMedia.getFormat());
				}
				else
				{
					uploadedDMSContent.setName(txtName.getValue());
				}

				uploadedDMSContent.setDMS_ContentType_ID((Integer) contentType.getValue());
				uploadedDMSContent.setM_AttributeSetInstance_ID(asiPanel.saveAttributes());
				uploadedDMSContent.setParentURL(contentManager.getPath(DMSContent));
			}
			else
			{
				uploadedDMSContent.setName(DMSContent.getName());
				uploadedDMSContent.setDMS_ContentType_ID(DMSContent.getDMS_ContentType_ID());
				uploadedDMSContent.setM_AttributeSetInstance_ID(DMSContent.getM_AttributeSetInstance_ID());
				uploadedDMSContent.setParentURL(DMSContent.getParentURL());
			}

			uploadedDMSContent.setDescription(txtDesc.getValue());
			uploadedDMSContent.setDMS_MimeType_ID(Utils.getMimeTypeID(uploadedMedia));
			uploadedDMSContent.setDMS_Status_ID(Utils.getStatusID());
			uploadedDMSContent.setContentBaseType(X_DMS_Content.CONTENTBASETYPE_Content);

			uploadedDMSContent.saveEx();

			MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), 0, null);

			dmsAssociation.setDMS_Content_ID(uploadedDMSContent.getDMS_Content_ID());

			if (isVersion)
			{
				dmsAssociation.setDMS_Content_Related_ID(DMS_Content_Related_ID);
				dmsAssociation.setDMS_AssociationType_ID(MDMSAssociationType.getVersionType(false));

				int seqNo = DB.getSQLValue(null,
						"SELECT MAX(seqNo) FROM DMS_Association WHERE DMS_Content_Related_ID = ?",
						DMS_Content_Related_ID);
				dmsAssociation.setSeqNo(seqNo + 1);
			}
			else
			{
				if (DMSContent != null)
					dmsAssociation.setDMS_Content_Related_ID(DMSContent.getDMS_Content_ID());

				dmsAssociation.setDMS_AssociationType_ID(MDMSAssociationType.getVersionType(true));
			}

			dmsAssociation.saveEx();

			fileStorgProvider.writeBLOB(contentManager.getPath(uploadedDMSContent), uploadedMedia.getByteData(),
					uploadedDMSContent);

			MDMSMimeType mimeType = new MDMSMimeType(Env.getCtx(), uploadedDMSContent.getDMS_MimeType_ID(), null);

			thumbnailGenerator = Utils.getThumbnailGenerator(mimeType.getMimeType());

			if (thumbnailGenerator != null)
				thumbnailGenerator.addThumbnail(uploadedDMSContent,
						fileStorgProvider.getFile(contentManager.getPath(uploadedDMSContent)), null);

		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "Upload Content Failure :" + ex.getLocalizedMessage());
			throw new AdempiereException("Upload Content Failure :" + ex.getLocalizedMessage());
		}

		this.detach();
	}

	private void processUploadMedia(Media media)
	{
		if (media == null)
			return;
		try
		{
			uploadedMedia = new AMedia(media.getName(), null, null, media.getByteData());
			btnFileUpload.setLabel(media.getName());
			txtName.setValue(FilenameUtils.getBaseName(media.getName()));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Upload Content Failure: " + e.getLocalizedMessage());
			throw new AdempiereException("Upload Content Failure: " + e.getLocalizedMessage());
		}
	}

	@Override
	public void valueChange(ValueChangeEvent event)
	{
		if (event.getSource().equals(contentType))
		{
			Components.removeAllChildren(tabPanelAttribute);
			asiPanel = new WDLoadASIPanel((int) contentType.getValue(), 0);
			tabPanelAttribute.appendChild(asiPanel);
		}
	}

}
