package org.idempiere.webui.apps.form;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Combobox;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Searchbox;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WEditorPopupMenu;
import org.adempiere.webui.editor.WebEditorFactory;
import org.adempiere.webui.event.ContextMenuListener;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.event.WTableModelEvent;
import org.adempiere.webui.event.WTableModelListener;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.FDialog;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSPermission;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Space;
import org.zkoss.zul.impl.XulElement;

public class WDMSPermissionPanel extends Window implements EventListener<Event>, ValueChangeListener, WTableModelListener
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 4436091432239358297L;

	private static CLogger		log					= CLogger.getCLogger(WDMSPermissionPanel.class);

	// DMS Content Window ID reference
	private static int			AD_Window_ID		= 0;
	// DMS content window having Permission tab
	private static int			AD_Tab_ID			= 0;

	private DMS					dms					= null;
	private MDMSContent			content				= null;

	private Grid				gridView			= GridFactory.newGridLayout();
	private WListbox			permissionTbl		= ListboxFactory.newDataTable();
	private ConfirmPanel		confirmPanel		= new ConfirmPanel(true);

	private List<WEditor>		m_wEditors			= new ArrayList<>();
	private List<WEditor>		m_wEditors2			= new ArrayList<>();

	private List<Space>			m_separators		= new ArrayList<>();

	private String				sqlPopulateTableData;

	private int					permissionID		= 0;

	private boolean				isDMSAdmin			= false;

	public WDMSPermissionPanel(DMS dms, MDMSContent content)
	{
		this.dms = dms;
		this.content = content;

		if (AD_Window_ID <= 0)
			AD_Window_ID = DB.getSQLValue(null, "SELECT AD_Window_ID FROM AD_Window WHERE AD_Window_UU = '845fbdde-7efb-4f14-965c-b84d894f2955'");
		if (AD_Tab_ID <= 0)
			AD_Tab_ID = DB.getSQLValue(null, "SELECT AD_Tab_ID FROM AD_Tab WHERE AD_Tab_UU = '259e37ec-4790-427f-a60a-6b848dfc5d1f'");

		isDMSAdmin = MRole.getDefault().get_ValueAsBoolean(DMSConstant.COLUMNNAME_IS_DMS_ADMIN);

		//
		init();

		createRowsOfEditors();

		configurePermissionTable(content);

		renderTableData();

		AEnv.showCenterScreen(this);
	} // Constructor

	private void init()
	{
		if (ClientInfo.isMobile())
		{
			this.setHeight("100%");
			this.setWidth("100%");
		}
		else
		{
			ZKUpdateUtil.setWidth(this, "680px");
			ZKUpdateUtil.setHeight(this, "600px");
		}

		Panel centerPanel = new Panel();
		centerPanel.appendChild(gridView);
		centerPanel.appendChild(permissionTbl);
		ZKUpdateUtil.setVflex(gridView, "1");
		ZKUpdateUtil.setVflex(permissionTbl, "1");
		ZKUpdateUtil.setHeight(centerPanel, "100%");
		gridView.setStyle("width: 100%; height: 100%; max-height: 100%; position: relative; overflow: auto;");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.appendCenter(centerPanel);
		borderlayout.appendSouth(confirmPanel);
		ZKUpdateUtil.setHeight(borderlayout.getSouth(), "60px");

		ZKUpdateUtil.setHeight(confirmPanel, "60px");
		confirmPanel.setStyle("padding-top: 15px; padding-bottom: 15px; padding-left: 10px; padding-right: 10px;");
		confirmPanel.addComponentsLeft(confirmPanel.createButton(ConfirmPanel.A_NEW));
		confirmPanel.addComponentsLeft(confirmPanel.createButton(ConfirmPanel.A_DELETE));
		confirmPanel.addActionListener(this);
		confirmPanel.getButton(ConfirmPanel.A_DELETE).setDisabled(true);

		this.setSizable(true);
		this.setClosable(true);
		this.appendChild(borderlayout);
		this.setTitle(DMSConstant.MSG_DMS_PERMISSION_LIST + ": " + content.getName());
	} // init

	private void createRowsOfEditors()
	{
		Rows rows = gridView.newRows();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_FIELD_OF_TAB, null);
			pstmt.setInt(1, AD_Tab_ID);

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				createField(rs, rows);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Error while retriving fields of Permission tab of DMS Content window. Error - " + e.getLocalizedMessage(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	} // createRowsOfEditors

	/**
	 * Create Field. - creates Fields and adds it to m_mFields list - creates
	 * Editor and adds it to m_vEditors list Handeles Ranges by adding
	 * additional mField/vEditor.
	 * <p>
	 * mFields are used for default value and mandatory checking; vEditors are
	 * used to retrieve the value (no data binding)
	 * 
	 * @param rs- result set
	 */
	private void createField(ResultSet rs, Rows rows)
	{
		// Create Field
		GridFieldVO voF = GridFieldVO.create(content.getCtx(), 0, 0, AD_Window_ID, AD_Tab_ID, false, rs);
		GridField mField = new GridField(voF);

		if (mField.getColumnName().equals(MDMSPermission.COLUMNNAME_AD_Client_ID)
			|| mField.getColumnName().equals(MDMSPermission.COLUMNNAME_AD_Org_ID)
			|| mField.getColumnName().equals(MDMSPermission.COLUMNNAME_DMS_Content_ID)
			|| mField.getColumnName().equals(MDMSPermission.COLUMNNAME_IsActive)
			|| (!isDMSAdmin && mField.getColumnName().equals(MDMSPermission.COLUMNNAME_IsAllPermission))
			|| (!isDMSAdmin && mField.getColumnName().equals(MDMSPermission.COLUMNNAME_IsNavigation)
				&& MDMSContent.CONTENTBASETYPE_Directory.equals(content.getContentBaseType())))
			return;

		Row row = new Row();

		// The Editor
		WEditor editor = WebEditorFactory.getEditor(mField, false);
		editor.setProcessParameter(true);
		editor.getComponent().addEventListener(Events.ON_FOCUS, this);
		editor.addValueChangeListener(this);
		editor.dynamicDisplay();
		// MField => VEditor - New Field value to be updated to editor
		mField.addPropertyChangeListener(editor);
		// Set Default
		Object defaultObject = mField.getDefaultForPanel();
		mField.setValue(defaultObject, true);
		// Stretch component to fill grid cell
		editor.fillHorizontal();
		// setup editor context menu
		WEditorPopupMenu popupMenu = editor.getPopupMenu();
		if (popupMenu != null)
		{
			popupMenu.addMenuListener((ContextMenuListener) editor);
			popupMenu.setId(mField.getColumnName() + "-popup");
			this.appendChild(popupMenu);
			if (!mField.isFieldOnly())
			{
				Label label = editor.getLabel();
				popupMenu.addContextElement(label);
				if (editor.getComponent() instanceof XulElement)
				{
					popupMenu.addContextElement((XulElement) editor.getComponent());
				}
			}
		}
		//
		m_wEditors.add(editor); // add to Editors

		Div div = new Div();
		div.setStyle("text-align: right;");
		org.adempiere.webui.component.Label label = editor.getLabel();
		div.appendChild(label);
		if (label.getDecorator() != null)
			div.appendChild(label.getDecorator());
		row.appendCellChild(div);

		//
		if (voF.isRange)
		{
			Div box = new Div();
			box.setStyle("display: flex; align-items: center;");
			ZKUpdateUtil.setWidth(box, "100%");
			box.appendChild(editor.getComponent());
			ZKUpdateUtil.setWidth((HtmlBasedComponent) editor.getComponent(), "49%");
			//
			GridFieldVO voF2 = GridFieldVO.createParameter(voF);
			GridField mField2 = new GridField(voF2);

			// The Editor
			WEditor editor2 = WebEditorFactory.getEditor(mField2, false);
			editor2.setProcessParameter(true);
			// override attribute
			editor2.getComponent().setClientAttribute("columnName", mField2.getColumnName() + "_To");
			editor2.getComponent().addEventListener(Events.ON_FOCUS, this);
			// New Field value to be updated to editor
			mField2.addPropertyChangeListener(editor2);
			editor2.dynamicDisplay();
			ZKUpdateUtil.setWidth((HtmlBasedComponent) editor2.getComponent(), "49%");
			// setup editor context menu
			popupMenu = editor2.getPopupMenu();
			if (popupMenu != null)
			{
				popupMenu.addMenuListener((ContextMenuListener) editor2);
				this.appendChild(popupMenu);
			}
			// Set Default
			Object defaultObject2 = mField2.getDefaultForPanel();
			mField2.setValue(defaultObject2, true);
			//
			m_wEditors2.add(editor2);
			Space separator = new Space();
			separator.setStyle("margin:0; width: 2%;");
			m_separators.add(separator);
			box.appendChild(separator);
			box.appendChild(editor2.getComponent());
			row.appendCellChild(box, 2);
		}
		else
		{
			row.appendCellChild(editor.getComponent(), (voF.isRange ? 2 : 4));
			m_wEditors2.add(null);
			m_separators.add(null);
		}

		if (mField.getColumnName().equals(MDMSPermission.COLUMNNAME_DMS_Owner_ID))
		{
			if (DisplayType.Search == mField.getDisplayType())
			{
				((Searchbox) editor.getComponent()).setEnabled(isDMSAdmin);
			}
			else if (DisplayType.Table == mField.getDisplayType() || DisplayType.TableDir == mField.getDisplayType())
			{
				((Combobox) editor.getComponent()).setEnabled(isDMSAdmin);
			}
		}

		rows.appendChild(row);
	} // createField

	private void configurePermissionTable(MDMSContent content)
	{
		Properties ctx = Env.getCtx();
		ColumnInfo[] permissionColumns = new ColumnInfo[] {
															new ColumnInfo("", "DMS_Permission_ID", IDColumn.class),
//																new ColumnInfo("DMS_Permission_ID", "P.DMS_Permission_ID", Integer.class),
																new ColumnInfo(Msg.translate(ctx, "DMS_Owner_ID"), "o.Name", String.class),
																new ColumnInfo(Msg.translate(ctx, "CreatedBy"), "c.Name", String.class),
																new ColumnInfo(Msg.translate(ctx, "AD_User_ID"), "u.Name", String.class),
																new ColumnInfo(Msg.translate(ctx, "AD_Role_ID"), "r.Name", String.class),
																new ColumnInfo(Msg.translate(ctx, "IsRead"), "IsRead", Boolean.class),
																new ColumnInfo(Msg.translate(ctx, "IsWrite"), "IsWrite", Boolean.class),
																new ColumnInfo(Msg.translate(ctx, "IsDelete"), "IsDelete", Boolean.class),
																new ColumnInfo(Msg.translate(ctx, "IsNavigation"), "IsNavigation", Boolean.class),
																new ColumnInfo(Msg.translate(ctx, "IsAllPermission"), "IsAllPermission", Boolean.class),
		};

		sqlPopulateTableData = permissionTbl.prepareTable(	permissionColumns,
															// FROM
															" DMS_Permission p "
																				+ " INNER JOIN AD_User o	ON (o.AD_User_ID = p.DMS_Owner_ID)	"
																				+ " LEFT JOIN AD_User c		ON (c.AD_User_ID = p.CreatedBy)	"
																				+ " LEFT JOIN AD_Role r		ON (r.AD_Role_ID = p.AD_Role_ID)	"
																				+ " LEFT JOIN AD_User u		ON (u.AD_User_ID = p.AD_User_ID)	",
															// WHERE
															"p.DMS_Content_ID = " + content.getDMS_Content_ID(),
															// Multi Selection
															false,
															"p");
		permissionTbl.setMultiSelection(false);
		permissionTbl.autoSize();
		permissionTbl.getModel().addTableModelListener(this);
	} // configurePermissionTable

	private void renderTableData()
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sqlPopulateTableData, null);
			rs = pstmt.executeQuery();
			permissionTbl.loadTable(rs);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error while rendering permission data on grid. " + e.getLocalizedMessage(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	} // renderTableData

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget().equals(confirmPanel.getOKButton()))
		{
			HashMap<String, Object> mapColumnValue = new HashMap<String, Object>();

			for (WEditor editor : m_wEditors)
			{
				buildMapOfEditorValue(mapColumnValue, editor);
			}

			if (((Integer) mapColumnValue.get(MDMSPermission.COLUMNNAME_AD_Role_ID) == 0
					&& (Integer) mapColumnValue.get(MDMSPermission.COLUMNNAME_AD_User_ID) == 0))
			{
				if (isDMSAdmin && !(Boolean) mapColumnValue.get(MDMSPermission.COLUMNNAME_IsAllPermission))
					throw new AdempiereException("Please select any one option from Role / User / All Permission");
				else if (!isDMSAdmin)
					throw new AdempiereException("Please select Role / User");
			}

			if (permissionTbl.getSelectedRowKey() == null)
				permissionID = 0;
			else
				permissionID = permissionTbl.getSelectedRowKey();

			if (permissionID > 0)
			{
				MDMSPermission permission = (MDMSPermission) MTable.get(content.getCtx(), MDMSPermission.Table_ID).getPO(permissionID, content.get_TrxName());

				dms.getPermissionManager().updatePermission(permission, content, mapColumnValue);

				clearFields();
				renderTableData();
			}
			else
			{
				if (MDMSContent.CONTENTBASETYPE_Directory.equals(content.getContentBaseType()))
				{
					Callback<Boolean> callbackConfirmation = new Callback<Boolean>() {

						@Override
						public void onCallback(Boolean result)
						{
							dms.getPermissionManager().createPermission(content, mapColumnValue, result);

							clearFields();
							renderTableData();
						}
					};

					FDialog.ask("Grant permission for the child content?", 0, this, "Will you grant same permission for the child content documents?",
								callbackConfirmation);
				}
				else
				{
					dms.getPermissionManager().createPermission(content, mapColumnValue, false);

					clearFields();
					renderTableData();
				}
			}
		}
		else if (event.getTarget().equals(confirmPanel.getButton(ConfirmPanel.A_CANCEL)))
		{
			dispose();
		}
		else if (event.getTarget().equals(confirmPanel.getButton(ConfirmPanel.A_NEW)))
		{
			permissionID = 0;

			clearFields();
			permissionTbl.clearSelection();
			confirmPanel.getButton(ConfirmPanel.A_DELETE).setDisabled(true);
		}
		else if (event.getTarget().equals(confirmPanel.getButton(ConfirmPanel.A_DELETE)))
		{
			permissionID = permissionTbl.getSelectedRowKey();
			if (permissionID > 0)
			{
				MDMSPermission permission = (MDMSPermission) MTable.get(content.getCtx(), MDMSPermission.Table_ID).getPO(permissionID, content.get_TrxName());
				if (permission.delete(false))
					permissionID = 0;
			}
			clearFields();
			renderTableData();
			confirmPanel.getButton(ConfirmPanel.A_DELETE).setDisabled(true);
		}
	} // onEvent

	/**
	 * Build map of editors value - Columnname and its value
	 * 
	 * @param mapColumnValue
	 * @param editor
	 */
	void buildMapOfEditorValue(HashMap<String, Object> mapColumnValue, WEditor editor)
	{
		String columnName = editor.getColumnName();

		int dt = editor.getGridField().getDisplayType();

		if (dt == DisplayType.Integer || (DisplayType.isID(dt) && columnName.endsWith("_ID")))
		{
			Object value = editor.getValue();
			if (editor.getGridField().isMandatory(true) && value == null)
			{
				Clients.scrollIntoView(editor.getComponent());
				throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
			}

			mapColumnValue.put(columnName, value == null ? 0 : (Integer) (((Number) value).intValue()));
		}
		else if (DisplayType.isNumeric(dt))
		{
			Object value = editor.getValue();
			if (editor.getGridField().isMandatory(true) && value == null)
			{
				Clients.scrollIntoView(editor.getComponent());
				throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
			}

			mapColumnValue.put(columnName, (BigDecimal) value);
		}
		else if (DisplayType.isDate(dt))
		{
			Timestamp valueTimeStamp = (Timestamp) editor.getValue();
			if (editor.getGridField().isMandatory(true) && valueTimeStamp == null)
			{
				Clients.scrollIntoView(editor.getComponent());
				throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
			}

			mapColumnValue.put(columnName, valueTimeStamp);
		}
		else if (dt == DisplayType.YesNo)
		{
			mapColumnValue.put(columnName, (Boolean) editor.getValue());
		}
		else if (DisplayType.isText(dt))
		{
			String value = null;
			if (editor.getValue() == null || Util.isEmpty(editor.getValue().toString(), true))
				value = null;
			else
				value = String.valueOf(editor.getValue());

			if (editor.getGridField().isMandatory(true) && Util.isEmpty(value))
			{
				Clients.scrollIntoView(editor.getComponent());
				throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
			}

			mapColumnValue.put(columnName, value);
		}
		else
		{
			log.log(Level.SEVERE, columnName + " not mapped " + DisplayType.getDescription(dt));
		}
	} // buildMapOfEditorValue

	private void clearFields()
	{
		for (WEditor editor : m_wEditors)
		{
			GridField field = editor.getGridField();
			editor.setValue(field.getDefaultForPanel());
			field.validateValueNoDirect();
		}
	} // clearFields

	@Override
	public void valueChange(ValueChangeEvent evt)
	{
	}

	@Override
	public void tableChanged(WTableModelEvent event)
	{
		IDColumn idColumn = (IDColumn) permissionTbl.getValueAt(event.getIndex0(), 0);
		if (idColumn.isSelected())
		{
			MDMSPermission permission = (MDMSPermission) MTable	.get(content.getCtx(), MDMSPermission.Table_ID)
																.getPO(idColumn.getRecord_ID(), content.get_TrxName());
			for (WEditor editor : m_wEditors)
			{
				GridField field = editor.getGridField();
				editor.setValue(permission.get_Value(field.getColumnName()));
				field.validateValueNoDirect();
			}

			permissionID = permission.getDMS_Permission_ID();
			confirmPanel.getButton(ConfirmPanel.A_DELETE).setDisabled(false);
		}
	} // tableChanged

}
