package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.AdempiereWebUI;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WTableDirEditor;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.idempiere.dms.storage.DmsUtility;
import org.idempiere.dms.storage.ThumbnailGenerator;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;
import org.idempiere.model.X_DMS_ContentType;
import org.zkoss.util.media.AMedia;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Vbox;

public class WUploadContent extends Window implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6554158380274124479L;
	private static CLogger		log					= CLogger.getCLogger(WUploadContent.class);

	private Borderlayout		mainLayout			= new Borderlayout();
	private WTableDirEditor		contentType;

	private Label				lblFile				= new Label();
	private Label				lblContentType		= new Label();
	private Label				lblDesc				= new Label();

	private Textbox				txtDesc				= new Textbox();

	private Button				fileUploadButton	= new Button();

	private Panel				parameterPanel		= new Panel();
	private Panel				dmsContentType		= new Panel();

	private ConfirmPanel		confirmPanel		= new ConfirmPanel(true, false, false, false, false, false);

	private AMedia				media				= null;

	private MDMSContent		mdmsContent			= null;

	public WUploadContent(MDMSContent mdms_Content)
	{
		this.mdmsContent = mdms_Content;
		init();
	}

	public void init()
	{
		this.setHeight("230px");
		this.setWidth("500px");
		this.setTitle("Upload Content");
		this.setClosable(true);
		mainLayout.setParent(this);
		mainLayout.setHflex("1");
		mainLayout.setVflex("1");

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
		fileUploadButton.setLabel("-");
		lblDesc.setValue("Description");
		txtDesc.setMultiline(true);
		txtDesc.setRows(2);
		txtDesc.setWidth("300px");
		LayoutUtils.addSclass("txt-btn", fileUploadButton);

		North north = new North();
		north.setParent(mainLayout);
		mainLayout.appendChild(north);
		north.appendChild(dmsContentType);

		Center center = new Center();
		center.setParent(mainLayout); 
		mainLayout.appendChild(center);
		center.appendChild(parameterPanel);

		Hbox hboxx = new Hbox();
		hboxx.setAlign("center");
		hboxx.setPack("start");
		hboxx.appendChild(lblContentType);
		hboxx.appendChild(contentType.getComponent());

		Vbox vbox = new Vbox();
		vbox.appendChild(new Space());

		Hbox descBox = new Hbox();
		descBox.setAlign("center");
		descBox.setPack("start");

		descBox.appendChild(lblDesc);
		descBox.appendChild(txtDesc);

		dmsContentType.setStyle("padding: 10px");
		dmsContentType.appendChild(hboxx);

		Hbox hbox = new Hbox();
		hbox.setAlign("center");
		hbox.setPack("start");
		hbox.appendChild(lblFile);
		hbox.appendChild(fileUploadButton);

		parameterPanel.setStyle("padding: 10px");
		parameterPanel.appendChild(hbox);
		parameterPanel.appendChild(vbox);
		parameterPanel.appendChild(descBox);

		South south = new South();
		south.setSclass("dialog-footer");
		south.setParent(mainLayout);
		mainLayout.appendChild(south);
		south.appendChild(confirmPanel);

		fileUploadButton.setUpload(AdempiereWebUI.getUploadSetting());
		fileUploadButton.addEventListener(Events.ON_UPLOAD, this);
		confirmPanel.addActionListener(Events.ON_CLICK, this);
		fileUploadButton.setLabel("-");
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
			String fillMandatory = Msg.translate(Env.getCtx(), "FillMandatory");
			MDMSContent dmsContent = null;

			if (contentType.getValue() == null || (Integer) contentType.getValue() == 0)
				throw new WrongValueException(contentType.getComponent(), fillMandatory);

			if (fileUploadButton.getLabel().equalsIgnoreCase("-"))
				throw new WrongValueException(fileUploadButton, fillMandatory);

			File uploadedDocument = null;
			if (mdmsContent.getParentURL() == null)
				uploadedDocument = new File(System.getProperty("user.dir") + File.separator + mdmsContent.getName()
						+ File.separator + media.getName());
			else
				uploadedDocument = new File(System.getProperty("user.dir") + mdmsContent.getParentURL()
						+ File.separator + mdmsContent.getName() + File.separator + media.getName());

			if (uploadedDocument.exists())
				uploadedDocument = new File(getUniqueFilename(uploadedDocument.getAbsolutePath()));

			FileOutputStream fos = new FileOutputStream(uploadedDocument);
			fos.write(media.getByteData());
			fos.close();

			try
			{
				dmsContent = new MDMSContent(Env.getCtx(), 0, null);
				dmsContent.setName(uploadedDocument.getName());
				dmsContent.setDescription(txtDesc.getValue());
				dmsContent.setDMS_MimeType_ID(DmsUtility.getMimeTypeID(media));
				dmsContent.setDMS_Status_ID(DmsUtility.getStatusID());
				dmsContent.setDMS_ContentType_ID((Integer) contentType.getValue());
				dmsContent.setContentBaseType(X_DMS_Content.CONTENTBASETYPE_Content);
				if (mdmsContent.getParentURL() == null)
					dmsContent.setParentURL(File.separator + mdmsContent.getName());
				else
					dmsContent.setParentURL(mdmsContent.getParentURL() + File.separator + mdmsContent.getName());
				dmsContent.saveEx();

				ThumbnailGenerator thumbnailGenerator = new ThumbnailGenerator();
				thumbnailGenerator.getThumbnails(uploadedDocument, dmsContent);

				MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), 0, null);
				dmsAssociation.setDMS_Content_ID(dmsContent.getDMS_Content_ID());
				dmsAssociation.setDMS_Content_Related_ID(mdmsContent.getDMS_Content_ID());
				dmsAssociation.setDMS_AssociationType_ID(MDMSAssociationType.getVersionType());
				dmsAssociation.saveEx();
			}
			catch (Exception ex)
			{
				log.log(Level.SEVERE, "Upload Content Failure :" + ex.getLocalizedMessage());
				throw new AdempiereException("Upload Content Failure :" + ex.getLocalizedMessage());
			}

			this.detach();
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
			this.detach();
	}

	private void processUploadMedia(Media document)
	{
		if (document == null)
			return;
		try
		{
			media = new AMedia(document.getName(), null, null, document.getByteData());
			fileUploadButton.setLabel(media.getName());
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Upload Content Failure: " + e.getLocalizedMessage());
			throw new AdempiereException("Upload Content Failure: " + e.getLocalizedMessage());
		}
	}

	private String getUniqueFilename(String fullPath)
	{
		File document = new File(fullPath);

		if (document.exists())
		{
			String filename = document.getName();
			String path = fullPath.substring(0, fullPath.length() - filename.length());
			String filenameWOExt = fullPath.substring(fullPath.lastIndexOf("/") + 1, fullPath.lastIndexOf("."));
			String ext = FilenameUtils.getExtension(document.getName());
			int n = 1;
			do
			{
				fullPath = path + filenameWOExt + "(" + n++ + ")." + ext;
				document = new File(fullPath);
			}
			while (document.exists());
		}
		return fullPath;
	}

}
