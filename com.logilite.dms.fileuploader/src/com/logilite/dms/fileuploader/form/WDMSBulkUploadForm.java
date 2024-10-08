package com.logilite.dms.fileuploader.form;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.factory.ButtonFactory;
import org.adempiere.webui.window.Dialog;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.util.media.AMedia;
import org.zkoss.util.media.Media;
import org.zkoss.zhtml.Div;
import org.zkoss.zhtml.Text;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Center;
import org.zkoss.zul.South;

import com.logilite.dms.DMS;
import com.logilite.dms.DMS_Context_Util;
import com.logilite.dms.DMS_ZK_Util;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.fileuploader.util.FileUploaderUtils;
import com.logilite.dms.form.AbstractUploadContent;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSContentType;
import com.logilite.dms.util.Utils;

/**
 * DMS content Upload form use as utility of bulk upload files
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 * @author Parth Ambani @ Logilite Technologies
 */
public class WDMSBulkUploadForm extends AbstractUploadContent
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 8146814718717680556L;

	private Label				lblTable;
	private Label				lblRecord;
	private Label				lblDirName;

	private WEditor				editorTable;
	private WEditor				editorRecord;
	private Textbox				txtDirName;

	private Row					rowTable;
	private Row					rowRecord;

	private String				dirName;

	public WDMSBulkUploadForm()
	{
		this(null, null, false, -1, -1, 0, 0);
	}

	/**
	 * Constructor
	 * 
	 * @param dms
	 * @param content
	 * @param isVersion
	 * @param tableID
	 * @param recordID
	 * @param windowNo
	 * @param tabNo
	 */
	public WDMSBulkUploadForm(DMS dms, I_DMS_Content content, boolean isVersion, int tableID, int recordID, int windowNo, int tabNo)
	{
		super(dms, content, isVersion, tableID, recordID, windowNo, tabNo);
		this.dms = new DMS(Env.getAD_Client_ID(Env.getCtx()));
	}

	/**
	 * initialize components
	 */
	public void init()
	{
		mainLayout.setParent(this);

		if (!isVersion)
			this.setStyle("min-height:30%; max-height:100%; overflow-y:auto;");
		else
			contentTypeRow.setVisible(false);
		tabBoxAttribute.setVisible(false);

		this.setHeight("100%");
		this.setWidth("100%");

		this.addEventListener(Events.ON_OK, this);

		//
		Center center = new Center();
		center.setParent(mainLayout);
		center.setSclass("dialog-content");
		center.appendChild(gridView);

		gridView.setStyle("overflow: auto; position:relative;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");

		int columnIDCT = MColumn.getColumn_ID(MDMSContent.Table_Name, MDMSContent.COLUMNNAME_DMS_ContentType_ID);
		MLookup lookupCT = MLookupFactory.get(Env.getCtx(), windowNo, tabNo, columnIDCT, DisplayType.TableDir);
		lookupCT.refresh();
		editorContentType = new WTableDirEditor(MDMSContentType.COLUMNNAME_DMS_ContentType_ID, false, false, true, lookupCT);

		// Load value from context if available for Content Type field
		DMS_Context_Util.setEditorDefaultValueFromCtx(Env.getCtx(), windowNo, tabNo, lookupCT.getDisplayType(), editorContentType);

		//
		lblContentType.setValue(DMSConstant.MSG_DMS_CONTENT_TYPE);
		btnFileUpload.setLabel(DMSConstant.MSG_UPLOAD_CONTENT);
		btnFileUpload.setWidth("100%");
		btnFileUpload.setHeight("60px");
		LayoutUtils.addSclass("txt-btn", btnFileUpload);

		// Rows
		rows = gridView.newRows();
		rows.setClass("bulk-form");

		// Row 1
		Text text = new Text(Msg.getMsg(Env.getCtx(), DMSConstant.MSG_DMS_BULK_UPLOAD));
		Div div = new Div();
		div.setStyle(" font-style: italic; height: 30px; color:red ");
		div.appendChild(text);

		Row row = rows.newRow();
		row.appendCellChild(div, 5);

		// Row 2
		lblTable = new Label(Msg.translate(Env.getCtx(), DMSConstant.AD_TABLE_ID));

		int columnIDTable = MColumn.getColumn_ID(MTab.Table_Name, MTab.COLUMNNAME_AD_Table_ID);
		MLookup lookupTable = MLookupFactory.get(Env.getCtx(), windowNo, tabNo, columnIDTable, DisplayType.TableDir);
		lookupTable.refresh();

		editorTable = new WTableDirEditor(MTab.COLUMNNAME_AD_Table_ID, false, false, true, lookupTable);
		editorTable.addValueChangeListener(this);
		editorTable.fillHorizontal();

		rowTable = new Row();
		rowTable.appendCellChild(lblTable, 2);
		rowTable.appendCellChild(editorTable.getComponent(), 3);
		rows.appendChild(rowTable);

		// Row 3
		rowRecord = new Row();
		rowRecord.setVisible(false);
		rows.appendChild(rowRecord);

		// Row 4
		lblDirName = new Label(Msg.translate(Env.getCtx(), DMSConstant.DIRECTORY));
		txtDirName = new Textbox();
		txtDirName.setWidth("100%");
		txtDirName.addEventListener(Events.ON_CHANGE, this);
		row = new Row();
		row.appendCellChild(lblDirName, 2);
		row.appendCellChild(txtDirName, 3);
		rows.appendChild(row);

		// Row 5
		contentTypeRow.appendCellChild(lblContentType, 2);
		contentTypeRow.appendCellChild(editorContentType.getComponent(), 3);
		editorContentType.fillHorizontal();
		editorContentType.addValueChangeListener(this);
		rows.appendChild(contentTypeRow);

		// Row 6
		tabsAttribute.appendChild(tabAttribute);
		tabAttribute.setLabel(DMSConstant.MSG_ATTRIBUTE_SET);
		tabPanelsAttribute.appendChild(tabPanelAttribute);
		tabPanelAttribute.setStyle("min-height :20px; overflow: auto;");

		tabBoxAttribute.appendChild(tabsAttribute);
		tabBoxAttribute.appendChild(tabPanelsAttribute);
		tabBoxAttribute.setMold("accordion");
		tabBoxAttribute.setVisible(false);

		Cell cell = new Cell();
		cell.setColspan(5);
		cell.appendChild(tabBoxAttribute);
		row = rows.newRow();
		row.appendChild(cell);

		// Row 7
		row = rows.newRow();
		row.appendCellChild(btnFileUpload, 5);

		//
		South south = new South();
		south.setParent(mainLayout);
		south.setSclass("dialog-footer");
		south.setStyle("background-color: transparent; border: none;");
		south.appendChild(confirmPanel);

		btnOk = confirmPanel.getOKButton();
		btnClose = confirmPanel.getButton(ConfirmPanel.A_CANCEL);
		btnClose.setVisible(false);
		// Load ASI Panel if ContentType value pre-filled from the context
		loadASIPanel();

		btnFileUpload.setUpload("multiple=true," + DMS_ZK_Util.getUploadSetting());
		btnFileUpload.addEventListener(Events.ON_UPLOAD, this);
		btnClose.addEventListener(Events.ON_CLICK, this);
		btnOk.addEventListener(Events.ON_CLICK, this);
		addEventListener(Events.ON_UPLOAD, this);

	} // init

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
				btnFileUpload.setEnabled(true);
				setBtnLabelFileUpload();
			}
			else
			{
				setBtnLabelFileUpload();
			}
		}
		else if (e.getTarget().equals(txtDirName))
		{
			dirName = txtDirName.getValue();
			if (!Util.isEmpty(dirName))
			{
				Pattern pattern = Pattern.compile("^[a-zA-Z0-9]+$");
				if (!pattern.matcher(dirName).matches())
					throw new WrongValueException(txtDirName, Msg.getCleanMsg(Env.getCtx(), "InvalidDirectoryName"));
			}
		}
	} // onEvent

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

				name = FileUploaderUtils.removeSpecialChars(name);
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

				rows.appendChild(row);
				row.getLastCell().setWidth("10px");

				Object[] value = new Object[] { row, new AMedia(media.getName(), null, null, media.getByteData()), txtName, txtDesc };
				mapUploadInfo.put(rowCount, value);
				rowCount++;
			}

			setBtnLabelFileUpload();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Upload Content Failure: ", e);
			throw new AdempiereException("Upload Content Failure: " + e);
		}
	} // processUploadMedia

	public void validateUploadedDocInfo()
	{
		if (mapUploadInfo.size() <= 0)
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

		if ((!Util.isEmpty(dirName) && tableID <= 0 && recordID <= 0) || (tableID > 0 && recordID > 0))
			return;

		// Format name
		if (Util.isEmpty(dirName))
			throw new WrongValueException(txtDirName, DMSConstant.MSG_FILL_MANDATORY);
		if (tableID <= 0)
			throw new WrongValueException(editorTable.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
		if (recordID <= 0)
			throw new WrongValueException(editorRecord.getComponent(), DMSConstant.MSG_FILL_MANDATORY);

	} // validateUploadedDocInfo

	/**
	 * save uploaded document in current directory
	 */
	protected void saveUploadedDocument()
	{
		validateUploadedDocInfo();

		String tableName = null;
		if (tableID > 0)
		{
			tableName = MTable.getTableName(Env.getCtx(), tableID);
			dms.initMountingStrategy(tableName);
		}

		if (!Util.isEmpty(dirName) && recordID <= 0 && tableID <= 0)
		{
			dms.initMountingStrategy(tableName);
			content = dms.getDMSMountingParent(0, 0);
			content = dms.createDirectory(dirName, content, tableID, recordID, false, null);
		}
		else
		{
			dms.initiateMountingContent(tableName, recordID, tableID);
			content = dms.getRootMountingContent(tableID, recordID);
			if (!Util.isEmpty(dirName))
				content = dms.createDirectory(dirName, content, tableID, recordID, false, null);
		}

		Map<Integer, Timestamp> contentCreateMap = new HashMap<Integer, Timestamp>();
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

				if (content != null && content.getDMS_Content_ID() > 0)
				{
					MDMSAssociation destAssociation = dms.getParentAssociationFromContent(content.getDMS_Content_ID());
					if (destAssociation != null)
					{
						tableID = destAssociation.getAD_Table_ID();
						recordID = destAssociation.getRecord_ID();
					}
				}

				int contentID = saveFileToDMS(tmpFile, txtName.getValue(), txtDesc.getValue());
				Timestamp date = FileUploaderUtils.getCreatedDate(tmpFile);
				if (date != null)
					contentCreateMap.put(contentID, date);

				if (!isVersion)
				{
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

		//
		Dialog.info(this.windowNo, "", "Total: " + contentIDs.size() + " Documents Uploaded", "Bulk Content Upload");

		//
		for (Entry<Integer, Timestamp> map : contentCreateMap.entrySet())
		{
			DB.executeUpdate("UPDATE DMS_Content SET Created=? WHERE DMS_Content_ID = ? ", new Object[] { map.getValue(), map.getKey() }, false, null);
			DB.executeUpdate(	"UPDATE DMS_Version SET IsIndexed='N', Created=? WHERE DMS_Content_ID = ? ", new Object[] { map.getValue(), map.getKey() }, false,
								null);
			DB.executeUpdate("UPDATE DMS_Association SET Created=? WHERE DMS_Content_ID = ? ", new Object[] { map.getValue(), map.getKey() }, false, null);
		}

	} // saveUploadedDocument

	public int saveFileToDMS(File uploadFile, String contentName, String description)
	{
		// Adding File to DMS
		int contentID = 0;
		if (isVersion)
		{
			contentID = dms.addFileVersion(content, uploadFile, description, tableID, recordID);
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

			contentID = dms.addFile(content, uploadFile, contentName, description, cTypeID, ASI_ID, tableID, recordID);
			if (content != null && !content.isMounting() && content.getDMS_Content_ID() != contentID)
			{
				MDMSContent uploadedContent = (MDMSContent) MTable.get(content.getCtx(), MDMSContent.Table_ID).getPO(contentID, content.get_TrxName());
				dms.grantChildPermissionFromParentContent(uploadedContent, content);
			}
			// Add id in list for returning
			contentIDs.add(contentID);
		}

		return contentID;
	} // saveToDMS

	@Override
	public void valueChange(ValueChangeEvent event)
	{
		if (event.getSource().equals(editorTable))
		{
			if (event.getNewValue() != null)
			{
				tableID = (int) event.getNewValue();
				Components.removeAllChildren(rowRecord);
				rowRecord.setVisible(true);

				MTable table = MTable.get(Env.getCtx(), tableID);
				String columnName = table.getTableName() + "_ID";

				int rColumn_ID = MColumn.getColumn_ID(table.getTableName(), columnName);
				MLookup rlookup = MLookupFactory.get(Env.getCtx(), windowNo, tabNo, rColumn_ID, DisplayType.Search);
				rlookup.refresh();

				lblRecord = new Label(Msg.translate(Env.getCtx(), DMSConstant.RECORD_ID));
				editorRecord = new WSearchEditor(rlookup, columnName, null, false, false, true);
				editorRecord.fillHorizontal();
				editorRecord.addValueChangeListener(this);

				rowRecord.appendCellChild(lblRecord, 2);
				rowRecord.appendCellChild(editorRecord.getComponent(), 3);
			}
			else
			{
				tableID = 0;
				Components.removeAllChildren(rowRecord);
				rowRecord.setVisible(false);
			}

		}
		else if (event.getSource().equals(editorRecord))
		{
			recordID = event.getNewValue() == null ? 0 : (int) event.getNewValue();
		}
		else
		{
			super.valueChange(event);
		}
	}
}
