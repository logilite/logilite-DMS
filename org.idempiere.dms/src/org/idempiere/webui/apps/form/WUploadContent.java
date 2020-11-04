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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.AdempiereWebUI;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
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
import org.compiere.model.MRole;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
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

import com.adaxa.signature.webui.component.SignatureImgBox;

public class WUploadContent extends Window implements EventListener <Event>, ValueChangeListener
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6554158380274124479L;

	private static CLogger		log					= CLogger.getCLogger(WUploadContent.class);

	private DMS					dms;
	private MDMSContent			DMSContent			= null;

	private Label				lblFile				= new Label();
	private Label				lblContentType		= new Label();
	private Label				lblDesc				= new Label();
	private Label				lblName				= new Label();
	private Label				lblSignature		= new Label();

	private Textbox				txtDesc				= new Textbox();
	private Textbox				txtName				= new Textbox();
	
	private Grid				gridView			= GridFactory.newGridLayout();
	private Row					contentTypeRow		= new Row();
	private Row					nameRow				= new Row();

	private Button				btnFileUpload		= new Button();
	private Button				btnClose			= null;
	private Button				btnOk				= null;
	private ConfirmPanel		confirmPanel		= null;

	private Tabbox				tabBoxAttribute		= new Tabbox();
	private Tabs				tabsAttribute		= new Tabs();
	private Tab					tabAttribute		= new Tab();
	private Tabpanels			tabPanelsAttribute	= new Tabpanels();
	private Tabpanel			tabPanelAttribute	= new Tabpanel();
	
	private SignatureImgBox		signatureBox		= new SignatureImgBox(false);

	private int					tableID				= 0;
	private int					recordID			= 0;

	private boolean				isVersion			= false;
	private boolean				isCancel			= false;

	private AMedia				uploadedMedia		= null;
	private WTableDirEditor		editorContentType;
	private WDLoadASIPanel		asiPanel			= null;

	private int					windowNo			= 0;
	private int					tabNo				= 0;

	/**
	 * Constructor initialize
	 * 
	 * @param dms
	 * @param mDMSContent
	 * @param isVersion
	 * @param tableID
	 * @param recordID
	 * @param tabNo
	 * @param windowNo
	 * @param winContent
	 */
	public WUploadContent(DMS dms, MDMSContent mDMSContent, boolean isVersion, int tableID, int recordID, int windowNo, int tabNo)
	{
		this.dms = dms;
		this.DMSContent = (MDMSContent) mDMSContent;
		this.isVersion = isVersion;
		this.tableID = tableID;
		this.recordID = recordID;
		this.windowNo = windowNo;
		this.tabNo = tabNo;

		init();
	}

	/**
	 * initialize components
	 */
	public void init()
	{
		if (!isVersion)
		{
			this.setStyle("min-height:40%; max-height:100%; overflow-y:auto;");
			this.setWidth("50%");
		}
		else
		{
			contentTypeRow.setVisible(false);
			nameRow.setVisible(false);
			tabBoxAttribute.setVisible(false);
			this.setHeight("26%");
			this.setWidth("40%");
		}

		if (ClientInfo.isMobile())
		{
			this.setHeight("100%");
			this.setWidth("100%");
		}

		this.setTitle(DMSConstant.MSG_UPLOAD_CONTENT);
		this.setClosable(true);
		this.appendChild(gridView);
		this.addEventListener(Events.ON_OK, this);

		gridView.setStyle("overflow: auto; position:relative;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");

		int Column_ID = MColumn.getColumn_ID(MDMSContent.Table_Name, MDMSContent.COLUMNNAME_DMS_ContentType_ID);
		MLookup lookup = MLookupFactory.get(Env.getCtx(), windowNo, tabNo, Column_ID, DisplayType.TableDir);
		lookup.refresh();
		editorContentType = new WTableDirEditor(MDMSContentType.COLUMNNAME_DMS_ContentType_ID, false, false, true, lookup);

		lblFile.setValue(DMSConstant.MSG_SELECT_FILE + "* ");
		lblContentType.setValue(DMSConstant.MSG_DMS_CONTENT_TYPE);
		btnFileUpload.setLabel("-");
		btnFileUpload.setWidth("100%");
		lblDesc.setValue(DMSConstant.MSG_DESCRIPTION);
		txtDesc.setMultiline(true);
		txtDesc.setRows(2);
		txtDesc.setWidth("100%");
		txtName.setWidth("100%");
		txtName.addEventListener(Events.ON_CHANGE, this);
		LayoutUtils.addSclass("txt-btn", btnFileUpload);

		Rows rows = gridView.newRows();

		Row row = rows.newRow();
		row.appendCellChild(lblFile);
		row.appendCellChild(btnFileUpload, 2);

		lblName.setValue(DMSConstant.MSG_NAME);
		nameRow.appendCellChild(lblName);
		nameRow.appendCellChild(txtName, 2);
		rows.appendChild(nameRow);

		contentTypeRow.appendCellChild(lblContentType);
		contentTypeRow.appendCellChild(editorContentType.getComponent(), 2);
		editorContentType.addValueChangeListener(this);
		rows.appendChild(contentTypeRow);

		row = rows.newRow();
		row.appendCellChild(lblDesc);
		row.appendCellChild(txtDesc, 2);
		
		boolean isDMSSignSupport = MRole.get(Env.getCtx(), Env.getAD_Role_ID(Env.getCtx())).get_ValueAsBoolean("IsDMSSignSupport");
		if (isDMSSignSupport)
		{
			row = rows.newRow();
			lblSignature.setValue(DMSConstant.MSG_SIGNATURE);
			row.appendCellChild(lblSignature);
			row.appendCellChild(signatureBox, 2);
			signatureBox.addEventListener(Events.ON_CHANGE, this);
		}

		row = rows.newRow();
		Cell cell = new Cell();
		cell.setColspan(3);

		tabBoxAttribute.appendChild(tabsAttribute);
		tabBoxAttribute.appendChild(tabPanelsAttribute);

		tabAttribute.setLabel(DMSConstant.MSG_ATTRIBUTE_SET);
		tabsAttribute.appendChild(tabAttribute);
		tabPanelsAttribute.appendChild(tabPanelAttribute);
		tabPanelAttribute.setStyle("min-height :20px; max-height: 200px; overflow: auto;");
		tabBoxAttribute.setMold("accordion");

		cell.appendChild(tabBoxAttribute);
		row.appendChild(cell);

		row = rows.newRow();
		confirmPanel = new ConfirmPanel();
		btnOk = confirmPanel.createButton(ConfirmPanel.A_OK);
		btnClose = confirmPanel.createButton(ConfirmPanel.A_CANCEL);

		cell = new Cell();
		cell.setAlign("right");
		cell.setColspan(3);
		cell.appendChild(btnOk);
		cell.appendChild(new Space());
		cell.appendChild(btnClose);
		cell.setStyle("position: relative;");
		row.appendChild(cell);

		btnFileUpload.setUpload(AdempiereWebUI.getUploadSetting());
		btnFileUpload.addEventListener(Events.ON_UPLOAD, this);
		btnClose.addEventListener(Events.ON_CLICK, this);
		btnOk.addEventListener(Events.ON_CLICK, this);
		addEventListener(Events.ON_UPLOAD, this);

		AEnv.showCenterScreen(this);
	} // init

	/*
	 * (non-Javadoc)
	 * @see
	 * org.zkoss.zk.ui.event.EventListener#onEvent(org.zkoss.zk.ui.event.Event)
	 */
	@Override
	public void onEvent(Event e) throws Exception
	{
		if (e instanceof UploadEvent)
		{
			UploadEvent ue = (UploadEvent) e;
			processUploadMedia(ue.getMedia());
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_OK) || Events.ON_OK.equals(e.getName()))
		{
			saveUploadedDocument();
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			isCancel = true;
			this.detach();
		}
		else if (e.getTarget().equals(signatureBox.getImage()))
		{
			signatureBox.setContent(signatureBox.getAImage());
			if (signatureBox.getAImage() != null)
			{
				btnFileUpload.setEnabled(false);
				if (isVersion)
				{
					MDMSContent parentContent = new MDMSContent(Env.getCtx(), DMSContent.getDMS_Content_Related_ID(), null);
					txtName.setValue(parentContent.getName().substring(0, parentContent.getName().lastIndexOf(".")));
				}
				else
				{
					txtName.setValue("Signature_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
				}
			}
			else
			{
				btnFileUpload.setEnabled(true);
				txtName.setValue(null);
			}
		}
	}

	/**
	 * save uploaded document in current directory
	 */
	private void saveUploadedDocument()
	{
		if (btnFileUpload.getLabel().equalsIgnoreCase("-") && signatureBox.getAImage() == null)
			throw new WrongValueException(btnFileUpload, DMSConstant.MSG_FILL_MANDATORY);

		if (Util.isEmpty(txtName.getValue(), true))
			throw new WrongValueException(txtName, "File name is mandatory");

		File tmpFile = null;
		byte[] fileData = null;
		try
		{
			if (signatureBox.getAImage() != null)
			{
				tmpFile = File.createTempFile(txtName.getValue(), ".png");
				fileData = signatureBox.getAImage().getByteData();
			}
			else
			{
				tmpFile = File.createTempFile(uploadedMedia.getName(), "." + uploadedMedia.getFormat());
				fileData = uploadedMedia.getByteData();
			}

			FileOutputStream os = new FileOutputStream(tmpFile);
			os.write(fileData);
			os.flush();
			os.close();

			if (DMSContent != null && DMSContent.getDMS_Content_ID() > 0)
			{
				MDMSAssociation destAssociation = dms.getAssociationFromContent(DMSContent.getDMS_Content_ID());
				tableID = destAssociation.getAD_Table_ID();
				recordID = destAssociation.getRecord_ID();
			}

			// Adding File
			if (isVersion)
			{
				//TODO - TableID and RecordID getting fro Substitute Record need to check conversion
				dms.addFileVersion(DMSContent, tmpFile, txtDesc.getValue(), tableID, recordID);
			}
			else
			{
				int ASI_ID = 0;
				int cTypeID = 0;

				if (editorContentType.getValue() != null)
				{
					cTypeID = (int) editorContentType.getValue();
					ASI_ID = asiPanel.saveAttributes();
				}

				//TODO - TableID and RecordID getting fro Substitute Record need to check conversion
				dms.addFile(DMSContent, tmpFile, txtName.getValue(), txtDesc.getValue(), cTypeID, ASI_ID, tableID, recordID);
			}
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to convert Media to File.", e);
			throw new AdempiereException("Fail to convert Media to File.", e);
		}
		finally
		{
			if (tmpFile != null)
				tmpFile.delete();
		}

		this.detach();
	} // saveUploadedDocument

	/**
	 * check media is uploaded
	 * 
	 * @param media
	 */
	private void processUploadMedia(Media media)
	{
		if (media == null)
			return;
		try
		{
			uploadedMedia = new AMedia(media.getName(), null, null, media.getByteData());
			btnFileUpload.setLabel(media.getName());
			txtName.setValue(FilenameUtils.getBaseName(media.getName()));
			signatureBox.setReadWrite(false);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Upload Content Failure: ", e);
			throw new AdempiereException("Upload Content Failure: " + e);
		}
	} // processUploadMedia

	@Override
	public void valueChange(ValueChangeEvent event)
	{
		if (event.getSource().equals(editorContentType))
		{
			Components.removeAllChildren(tabPanelAttribute);

			if (editorContentType.getValue() != null)
			{
				asiPanel = new WDLoadASIPanel((int) editorContentType.getValue(), 0);
				tabPanelAttribute.appendChild(asiPanel);
			}
		}
	}

	/**
	 * @return true if dialog cancel by user
	 */
	public boolean isCancel()
	{
		return isCancel;
	}

}
