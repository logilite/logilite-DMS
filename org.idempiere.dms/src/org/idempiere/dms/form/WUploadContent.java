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

package org.idempiere.dms.form;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
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
import org.adempiere.webui.factory.ButtonFactory;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.DMS;
import org.idempiere.dms.DMS_Context_Util;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.util.Utils;
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

public class WUploadContent extends Window implements EventListener<Event>, ValueChangeListener
{

	/**
	 * 
	 */
	private static final long						serialVersionUID			= -6554158380274124479L;

	private static CLogger							log							= CLogger.getCLogger(WUploadContent.class);

	public static final String						BTN_ID_UPLOAD_CANCEL_PREFIX	= "UploadCancel_";

	public static final String						ATTRIB_ROW_REF				= "ATTRIB_ROW_REF";
	public static final String						ATTRIB_ROW_NO				= "ATTRIB_ROW_NO";

	private DMS										dms;
	private MDMSContent								DMSContent					= null;

	private Label									lblContentType				= new Label();
	private Label									lblSignature				= new Label();

	private Textbox									txtSign;
	private Textbox									txtSignDesc;

	private Grid									gridView					= GridFactory.newGridLayout();
	private Row										contentTypeRow				= new Row();
	private Row										nameRow						= new Row();

	private Button									btnFileUpload				= new Button();
	private Button									btnClose					= null;
	private Button									btnOk						= null;
	private ConfirmPanel							confirmPanel				= null;

	private Tabbox									tabBoxAttribute				= new Tabbox();
	private Tabs									tabsAttribute				= new Tabs();
	private Tab										tabAttribute				= new Tab();
	private Tabpanels								tabPanelsAttribute			= new Tabpanels();
	private Tabpanel								tabPanelAttribute			= new Tabpanel();

	private SignatureImgBox							signatureBox				= new SignatureImgBox(false);

	private int										tableID						= 0;
	private int										recordID					= 0;
	private ArrayList<Integer>						contentIDs					= new ArrayList<Integer>();

	private boolean									isVersion					= false;
	private boolean									isCancel					= false;
	boolean											isDMSSignSupport			= false;

	private WTableDirEditor							editorContentType;
	private WDLoadASIPanel							asiPanel					= null;

	private int										windowNo					= 0;
	private int										tabNo						= 0;
	private int										rowCount					= 0;

	private Rows									rows;

	/*
	 * Map used for adding/uploading multiple content as same time to track the required data.
	 * Concurrent map - for one by one content is added to DMS then remove that content from map. If
	 * in case its failed from DMS side in between then no need to go already uploaded content again
	 * Map data like: < RowNo, [ row, media, txtName, txtDesc, byte[] ] >
	 * Here Byte array used for signature data
	 */
	private ConcurrentHashMap<Integer, Object[]>	mapUploadInfo				= new ConcurrentHashMap<Integer, Object[]>();

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
		isDMSSignSupport = MRole.get(Env.getCtx(), Env.getAD_Role_ID(Env.getCtx())).get_ValueAsBoolean("IsDMSSignSupport");

		init();
	}

	/**
	 * initialize components
	 */
	public void init()
	{
		if (!isVersion)
		{
			this.setStyle("min-height:30%; max-height:100%; overflow-y:auto;");
			if (isDMSSignSupport)
				this.setHeight("60%");
			else
				this.setHeight("40%");
			this.setWidth("60%");
		}
		else
		{
			contentTypeRow.setVisible(false);
			nameRow.setVisible(false);
			tabBoxAttribute.setVisible(false);
			this.setWidth("40%");
		}

		if (ClientInfo.isMobile())
		{
			this.setHeight("100%");
			this.setWidth("100%");
		}

		this.setTitle(DMSConstant.MSG_UPLOAD_CONTENT);
		this.setClosable(true);
		this.setMaximizable(true);
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
		// Load value from context if available for Content Type field
		DMS_Context_Util.setEditorDefaultValueFromCtx(Env.getCtx(), windowNo, tabNo, lookup.getDisplayType(), editorContentType);

		//
		lblContentType.setValue(DMSConstant.MSG_DMS_CONTENT_TYPE);
		btnFileUpload.setLabel(DMSConstant.MSG_UPLOAD_CONTENT);
		btnFileUpload.setWidth("100%");
		btnFileUpload.setHeight("60px");
		LayoutUtils.addSclass("txt-btn", btnFileUpload);

		//
		rows = gridView.newRows();
		Row row = rows.newRow();
		row.appendCellChild(btnFileUpload, 5);

		txtSign = createTextbox("", "Content Name", false);
		txtSignDesc = createTextbox("", DMSConstant.MSG_DESCRIPTION, true);
		txtSign.setVisible(false);
		txtSignDesc.setVisible(false);
		nameRow.appendCellChild(txtSign, 2);
		nameRow.appendCellChild(txtSignDesc, 2);
		rows.appendChild(nameRow);

		if (isDMSSignSupport)
		{
			row = rows.newRow();
			lblSignature.setValue(DMSConstant.MSG_SIGNATURE);
			row.appendCellChild(lblSignature, 2);
			row.appendCellChild(signatureBox, 3);
			signatureBox.addEventListener(Events.ON_CHANGE, this);
		}

		contentTypeRow.appendCellChild(lblContentType, 2);
		contentTypeRow.appendCellChild(editorContentType.getComponent(), 3);
		editorContentType.addValueChangeListener(this);
		rows.appendChild(contentTypeRow);

		//
		row = rows.newRow();
		Cell cell = new Cell();
		cell.setColspan(5);

		tabBoxAttribute.appendChild(tabsAttribute);
		tabBoxAttribute.appendChild(tabPanelsAttribute);

		tabAttribute.setLabel(DMSConstant.MSG_ATTRIBUTE_SET);
		tabsAttribute.appendChild(tabAttribute);
		tabPanelsAttribute.appendChild(tabPanelAttribute);
		tabPanelAttribute.setStyle("min-height :20px; max-height: 200px; overflow: auto;");
		tabBoxAttribute.setMold("accordion");
		tabBoxAttribute.setVisible(false);

		cell.appendChild(tabBoxAttribute);
		row.appendChild(cell);

		//
		row = rows.newRow();
		confirmPanel = new ConfirmPanel();
		btnOk = confirmPanel.createButton(ConfirmPanel.A_OK);
		btnClose = confirmPanel.createButton(ConfirmPanel.A_CANCEL);

		cell = new Cell();
		cell.setAlign("right");
		cell.setColspan(4);
		cell.appendChild(btnOk);
		cell.appendChild(new Space());
		cell.appendChild(btnClose);
		cell.setStyle("position: relative;");
		row.appendChild(cell);

		// Load ASI Panel if ContentType value pre-filled from the context
		loadASIPanel();

		btnFileUpload.setUpload("multiple=true," + DMS_ZK_Util.getUploadSetting());
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
			processUploadMedia(ue.getMedias());
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_OK) || Events.ON_OK.equals(e.getName()))
		{
			if (isVersion && mapUploadInfo.size() > 1)
			{
				throw new WrongValueException(btnFileUpload, "Please upload only single content for versioning");
			}

			saveUploadedDocument();
		}
		else if (e.getTarget().getId().startsWith(BTN_ID_UPLOAD_CANCEL_PREFIX))
		{
			Button btnCancelRow = (Button) e.getTarget();
			int rowNo = (int) btnCancelRow.getAttribute(ATTRIB_ROW_NO);
			Row rowRef = (Row) btnCancelRow.getAttribute(ATTRIB_ROW_REF);

			//
			rows.removeChild(rowRef);
			mapUploadInfo.remove(rowNo);
			//
			if (mapUploadInfo.size() <= 0)
			{
				txtSign.setValue(null);
				txtSign.setVisible(false);

				txtSignDesc.setVisible(false);
				txtSignDesc.setValue(null);

				signatureBox.setReadWrite(true);

				btnFileUpload.setEnabled(true);
				setBtnLabelFileUpload();
			}
			else
			{
				setBtnLabelFileUpload();
			}
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			isCancel = true;
			this.detach();
		}
		else if (e.getTarget().equals(signatureBox) || e.getTarget().equals(signatureBox.getImage()))
		{
			signatureBox.setContent(signatureBox.getAImage());
			if (signatureBox.getAImage() != null)
			{
				btnFileUpload.setEnabled(false);
				if (isVersion)
				{
					MDMSContent parentContent = new MDMSContent(Env.getCtx(), DMSContent.getDMS_Content_Related_ID(), null);
					txtSign.setValue(parentContent.getName().substring(0, parentContent.getName().lastIndexOf(".")));
				}
				else
				{
					txtSign.setValue("Signature_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
				}

				// row, media, txtName, txtDesc, byte[]
				mapUploadInfo.put(0, new Object[] { null, null, txtSign, txtSignDesc, signatureBox.getAImage().getByteData() });

				txtSign.setVisible(true);
				txtSignDesc.setVisible(true);
			}
			else
			{
				btnFileUpload.setEnabled(true);

				txtSign.setValue(null);
				txtSign.setVisible(false);

				txtSignDesc.setVisible(false);
				txtSignDesc.setValue(null);
			}
		}
	} // onEvent

	/**
	 * save uploaded document in current directory
	 */
	private void saveUploadedDocument()
	{
		if (mapUploadInfo.size() <= 0 && signatureBox.getAImage() == null)
			throw new WrongValueException(btnFileUpload, DMSConstant.MSG_UPLOAD_CONTENT);

		for (Entry<Integer, Object[]> map : mapUploadInfo.entrySet())
		{
			Textbox txtBox = (Textbox) map.getValue()[2];
			if (Util.isEmpty(txtBox.getValue(), true))
				throw new WrongValueException(txtBox, DMSConstant.MSG_FILL_MANDATORY);

			String errorMsg = Utils.isValidFileName(txtBox.getValue(), false);
			if (!Util.isEmpty(errorMsg))
				throw new WrongValueException(txtBox, errorMsg);
		}

		// Add content in DMS one by one
		for (Entry<Integer, Object[]> map : mapUploadInfo.entrySet())
		{
			File tmpFile = null;
			byte[] fileData = null;
			Textbox txtName = null;
			Textbox txtDesc = null;
			try
			{
				txtName = (Textbox) map.getValue()[2];
				txtDesc = (Textbox) map.getValue()[3];

				// Prepare temporary file
				if (map.getValue()[0] == null && map.getValue()[4] != null)
				{
					tmpFile = File.createTempFile(txtName.getValue(), ".png");
					fileData = (byte[]) map.getValue()[4];
				}
				else
				{
					AMedia media = (AMedia) map.getValue()[1];
					tmpFile = File.createTempFile(media.getName(), "." + media.getFormat());
					fileData = media.getByteData();
				}

				FileOutputStream os = new FileOutputStream(tmpFile);
				os.write(fileData);
				os.flush();
				os.close();

				if (DMSContent != null && DMSContent.getDMS_Content_ID() > 0)
				{
					MDMSAssociation destAssociation = dms.getParentAssociationFromContent(DMSContent.getDMS_Content_ID());
					tableID = destAssociation.getAD_Table_ID();
					recordID = destAssociation.getRecord_ID();
				}

				// Adding File to DMS
				if (isVersion)
				{
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
						asiPanel.setASIID(0);
					}

					int contentID = dms.addFile(DMSContent, tmpFile, txtName.getValue(), txtDesc.getValue(), cTypeID, ASI_ID, tableID, recordID);
					if (DMSContent != null && !DMSContent.isMounting() && DMSContent.getDMS_Content_ID() != contentID)
					{
						MDMSContent content = (MDMSContent) MTable.get(DMSContent.getCtx(), MDMSContent.Table_ID).getPO(contentID, DMSContent.get_TrxName());
						dms.grantChildPermissionFromParentContent(content, DMSContent);
					}
					// Add id in list for returning
					contentIDs.add(contentID);

					// Remove from map as content added in DMS
					Object[] data = mapUploadInfo.remove(map.getKey());

					if (data[0] != null)
					{
						// Remove name and description field row
						rows.removeChild((Row) data[0]);
					}
					setBtnLabelFileUpload();
				}
			}
			catch (IOException e)
			{
				log.log(Level.SEVERE, "Fail to convert Media to File for " + txtName.getValue(), e);
				throw new WrongValueException(txtName, "Fail to convert Media to File: " + e.getLocalizedMessage(), e);
			}
			catch (Exception e)
			{
				throw new WrongValueException(txtName, e.getLocalizedMessage(), e);
			}
			finally
			{
				if (tmpFile != null)
					tmpFile.delete();
			}
		}

		this.detach();
	} // saveUploadedDocument

	/**
	 * check medias is uploaded
	 * 
	 * @param medias
	 */
	private void processUploadMedia(Media[] medias)
	{
		if (medias == null)
			return;

		try
		{
			for (Media media : medias)
			{
				String name = FilenameUtils.getBaseName(media.getName());

				//
				Textbox txtName = createTextbox(name, "Content Name", false);
				Textbox txtDesc = createTextbox("", DMSConstant.MSG_DESCRIPTION, true);
				Button btnCancel = ButtonFactory.createNamedButton("Cancel", false, true);
				btnCancel.setId(BTN_ID_UPLOAD_CANCEL_PREFIX + rowCount);
				txtName.setWidth("98%");

				Row row = new Row();
				row.appendCellChild(txtName, 2);
				row.appendCellChild(txtDesc, 2);
				row.appendCellChild(btnCancel, 1);
				btnCancel.setAttribute(ATTRIB_ROW_REF, row);
				btnCancel.setAttribute(ATTRIB_ROW_NO, rowCount);
				btnCancel.addEventListener(Events.ON_CLICK, this);

				rows.insertBefore(row, nameRow);
				row.getLastCell().setWidth("10px");

				Object[] value = new Object[] { row, new AMedia(media.getName(), null, null, media.getByteData()), txtName, txtDesc };
				mapUploadInfo.put(rowCount, value);
				rowCount++;
			}

			setBtnLabelFileUpload();
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
			tabBoxAttribute.setVisible(false);
			//
			loadASIPanel();
		}
	}

	public void loadASIPanel()
	{
		if (editorContentType.getValue() != null)
		{
			asiPanel = new WDLoadASIPanel((int) editorContentType.getValue(), 0, windowNo, tabNo);
			tabPanelAttribute.appendChild(asiPanel);
			tabBoxAttribute.setVisible(true);
		}
	} // loadASIPanel

	private Textbox createTextbox(String name, String placeholder, boolean isForDesc)
	{
		Textbox txtbox = new Textbox();
		txtbox.setPlaceholder(placeholder);
		txtbox.setWidth("100%");
		txtbox.setValue(name);
		if (isForDesc)
		{
			txtbox.setMultiline(true);
			txtbox.setRows(2);
		}
		return txtbox;
	} // createTextbox

	public void setBtnLabelFileUpload()
	{
		if (mapUploadInfo.size() <= 0)
			btnFileUpload.setLabel(DMSConstant.MSG_UPLOAD_CONTENT);
		else
			btnFileUpload.setLabel(DMSConstant.MSG_UPLOAD_CONTENT + " [ " + mapUploadInfo.size() + " content(s) ] ");
	} // setBtnLabelFileUpload

	/**
	 * @return true if dialog cancel by user
	 */
	public boolean isCancel()
	{
		return isCancel;
	}

	/**
	 * @return content ID of the uploaded document
	 */
	public Integer[] getUploadedDocContentIDs()
	{
		return contentIDs.toArray(new Integer[contentIDs.size()]);
	}
}
