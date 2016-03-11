package org.idempiere.webui.apps.form;

import java.io.File;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Separator;
import org.zkoss.zul.South;

public class CreateDirectoryForm extends Window implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long		serialVersionUID	= 4397569198011705268L;
	protected static final CLogger	log					= CLogger.getCLogger(CreateDirectoryForm.class);

	private Borderlayout			mainLayout			= new Borderlayout();
	private Panel					parameterPanel		= new Panel();
	private ConfirmPanel			confirmPanel		= new ConfirmPanel(true, false, false, false, false, false);
	private Label					lblDir				= new Label(Msg.translate(Env.getCtx(), "Directory Name"));
	private Textbox					txtboxDirectory		= new Textbox();
	private File					file				= null;
	private MDMSContent				mDMSContent			= null;

	private String					fileSeprator		= null;

	private IFileStorageProvider	fileStorageProvider	= null;
	private IContentManager			contentManager		= null;

	private int						tableID				= 0;
	private int						recordID			= 0;

	/**
	 * Constructor initialize
	 * 
	 * @param DMSContent
	 */
	public CreateDirectoryForm(I_DMS_Content DMSContent, int tableID, int recordID)
	{
		try
		{
			this.mDMSContent = (MDMSContent) DMSContent;
			this.tableID = tableID;
			this.recordID = recordID;

			fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);

			if (fileStorageProvider == null)
				throw new AdempiereException("Storage provider is not found");

			contentManager = Utils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));

			if (contentManager == null)
				throw new AdempiereException("Content manager is not found");

			fileSeprator = Utils.getStorageProviderFileSeparator();
			init();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render Component Problem", e);
			throw new AdempiereException("Render Component Problem : " + e);
		}
	}

	/**
	 * initialize components
	 * 
	 * @throws Exception
	 */
	public void init() throws Exception
	{
		this.setHeight("150px");
		this.setWidth("500px");
		this.setTitle(Msg.getMsg(Env.getCtx(), "Create Directory"));
		mainLayout.setParent(this);
		mainLayout.setHflex("1");
		mainLayout.setVflex("1");

		lblDir.setValue(Msg.getMsg(Env.getCtx(), "Enter Directory Name") + ": ");
		lblDir.setStyle("padding-left: 5px");
		txtboxDirectory.setWidth("300px");
		txtboxDirectory.setFocus(true);

		North north = new North();
		north.setParent(mainLayout);
		mainLayout.appendChild(north);
		north.appendChild(parameterPanel);

		Hbox hbox = new Hbox();
		hbox.setAlign("center");
		hbox.setPack("start");
		hbox.appendChild(lblDir);
		hbox.appendChild(txtboxDirectory);

		parameterPanel.setStyle("padding: 5px");
		parameterPanel.appendChild(hbox);
		Separator separator = new Separator();
		separator.setOrient("horizontal");
		separator.setBar(true);
		separator.setStyle("padding-top: 40px");
		parameterPanel.appendChild(separator);

		South south = new South();
		south.setSclass("dialog-footer");
		south.setParent(mainLayout);
		mainLayout.appendChild(south);
		south.appendChild(confirmPanel);

		confirmPanel.addActionListener(Events.ON_CLICK, this);
		AEnv.showCenterScreen(this);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.zkoss.zk.ui.event.EventListener#onEvent(org.zkoss.zk.ui.event.Event)
	 */
	@Override
	public void onEvent(Event event) throws Exception
	{
		log.info(event.getName());

		if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			this.detach();
		}
		if (event.getTarget().getId().equals(ConfirmPanel.A_OK))
		{
			String fillMandatory = Msg.translate(Env.getCtx(), "FillMandatory");
			String dirName = txtboxDirectory.getValue();

			if (Util.isEmpty(dirName) || dirName.equals(""))
				throw new WrongValueException(txtboxDirectory, fillMandatory);

			try
			{
				File rootFolder = new File(fileStorageProvider.getBaseDirectory(contentManager.getPath(mDMSContent)));

				if (!rootFolder.exists())
					rootFolder.mkdirs();

				file = new File(rootFolder + fileSeprator + dirName);
				if (!file.exists())
					file.mkdir();
				else
					throw new AdempiereException(Msg.getMsg(Env.getCtx(), "Directory already exists."));

				MDMSContent content = new MDMSContent(Env.getCtx(), 0, null);
				content.setDMS_MimeType_ID(Utils.getMimeTypeID(null));
				content.setName(dirName);

				content.setParentURL(contentManager.getPath(mDMSContent));

				content.setValue(dirName);
				content.setContentBaseType(X_DMS_Content.CONTENTBASETYPE_Directory);
				content.saveEx();

				MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), 0, null);
				dmsAssociation.setDMS_Content_ID(content.getDMS_Content_ID());
				if (mDMSContent != null)
					dmsAssociation.setDMS_Content_Related_ID(mDMSContent.getDMS_Content_ID());
				dmsAssociation.setAD_Table_ID(tableID);
				dmsAssociation.setRecord_ID(recordID);
				// dmsAssociation.setDMS_AssociationType_ID(MDMSAssociationType.getVersionType());
				dmsAssociation.saveEx();
				this.detach();

			}
			catch (AdempiereException e)
			{
				log.log(Level.SEVERE, "Directory is allready created", e);
				throw new AdempiereException(Msg.getMsg(Env.getCtx(), "Directory is allready created"));
			}
			catch (Exception e)
			{

				log.log(Level.SEVERE, "Directory is not created", e);
				throw new AdempiereException(Msg.getMsg(Env.getCtx(), "Directory is not created"));

			}
		}
	}
}