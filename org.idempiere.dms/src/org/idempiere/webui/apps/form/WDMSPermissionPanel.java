package org.idempiere.webui.apps.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
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
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.idempiere.dms.DMSPermission;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.util.DMSPermissionUtils;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSPermission;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Space;
import org.zkoss.zul.impl.XulElement;

public class WDMSPermissionPanel extends Window implements EventListener <Event>, ValueChangeListener, WTableModelListener
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 4436091432239358297L;

	private static final int	AD_Window_ID		= DB.getSQLValue(null, "SELECT AD_Window_ID FROM AD_Window WHERE AD_Window_UU = '845fbdde-7efb-4f14-965c-b84d894f2955'");

	private static final int	AD_Tab_ID			= DB.getSQLValue(null, "SELECT AD_Tab_ID FROM AD_Tab WHERE AD_Tab_UU = '259e37ec-4790-427f-a60a-6b848dfc5d1f'");

	private static CLogger		log					= CLogger.getCLogger(WDMSVersion.class);

	private MDMSContent			content				= null;
	private Properties			ctx;

	private Grid				gridView			= GridFactory.newGridLayout();
	private WListbox			permissionTbl		= ListboxFactory.newDataTable();
	private ConfirmPanel		confirmPanel		= new ConfirmPanel(true);

	private List <GridField>	m_mFields			= new ArrayList <>();
	private List <GridField>	m_mFields2			= new ArrayList <>();

	private List <WEditor>		m_wEditors			= new ArrayList <>();
	private List <WEditor>		m_wEditors2			= new ArrayList <>();

	private List <Space>		m_separators		= new ArrayList <>();

	private String				sqlpermissionColumn;

	public WDMSPermissionPanel(MDMSContent content)
	{
		this.ctx = content.getCtx();
		this.content = content;

		init();

		createPermissionEditor();

		configurePermissionTable(content);
		renderDMSPermission();

		AEnv.showCenterScreen(this);
	}

	private void renderDMSPermission( )
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sqlpermissionColumn, null);
			rs = pstmt.executeQuery();
			permissionTbl.loadTable(rs);
		}
		catch (Exception e)
		{
			log.log(Level.WARNING, sqlpermissionColumn, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}

	private void configurePermissionTable(MDMSContent content)
	{
		ColumnInfo[] permissionColumn = new ColumnInfo[] {
															new ColumnInfo(Msg.translate(Env.getCtx(), "Permission"), "DMS_Permission_ID", IDColumn.class),
																new ColumnInfo(Msg.translate(Env.getCtx(), "OwnerName"), "OwnerName", String.class),
																new ColumnInfo(Msg.translate(Env.getCtx(), "IsRead"), "IsRead", Boolean.class),
																new ColumnInfo(Msg.translate(Env.getCtx(), "IsWrite"), "IsWrite", Boolean.class),
																new ColumnInfo(Msg.translate(Env.getCtx(), "IsDelete"), "IsDelete", Boolean.class),
																new ColumnInfo(Msg.translate(Env.getCtx(), "IsNavigation"), "IsNavigation", Boolean.class),
																new ColumnInfo(Msg.translate(Env.getCtx(), "RoleName"), "RoleName", String.class),
																new ColumnInfo(Msg.translate(Env.getCtx(), "UserName"), "UserName", String.class)
		};

		sqlpermissionColumn = permissionTbl.prepareTable(permissionColumn, "DMS_Permission_V", "DMS_Content_ID = " + content.getDMS_Content_ID(), false, "DMS_Permission_V");
		permissionTbl.setMultiSelection(false);
		permissionTbl.autoSize();
		permissionTbl.getModel().addTableModelListener(this);
	}

	private void init( )
	{
		if (ClientInfo.isMobile())
		{
			this.setHeight("100%");
			this.setWidth("100%");
		}
		else
		{
			ZKUpdateUtil.setWidth(this, "680px");
			ZKUpdateUtil.setHeight(this, "580px");
		}

		Borderlayout borderlayout = new Borderlayout();

		Panel centerPanel = new Panel();
		centerPanel.appendChild(gridView);
		centerPanel.appendChild(permissionTbl);
		ZKUpdateUtil.setVflex(gridView, "1");
		ZKUpdateUtil.setVflex(permissionTbl, "1");
		ZKUpdateUtil.setHeight(centerPanel, "100%");
		gridView.setStyle("width: 98%; height: 95%; max-height: 100%; position: relative; overflow: auto;");

		borderlayout.appendCenter(centerPanel);
		borderlayout.appendSouth(confirmPanel);

		ZKUpdateUtil.setHeight(confirmPanel, "60px");
		ZKUpdateUtil.setHeight(borderlayout.getSouth(), "60px");
		confirmPanel.setStyle("padding-top: 15px; padding-bottom: 15px; padding-left: 10px; padding-right: 10px;");

		confirmPanel.addComponentsLeft(confirmPanel.createButton(ConfirmPanel.A_NEW));
		confirmPanel.addComponentsLeft(confirmPanel.createButton(ConfirmPanel.A_DELETE));
		confirmPanel.addActionListener(this);

		confirmPanel.getButton(ConfirmPanel.A_DELETE).setDisabled(true);
		this.setClosable(true);
		this.appendChild(borderlayout);
		this.setTitle(DMSConstant.MSG_DMS_PERMISSION_LIST);
		this.setSizable(true);
	}

	private void createPermissionEditor( )
	{
		Rows rows = gridView.newRows();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = "SELECT * FROM AD_Field_VT WHERE AD_Tab_ID=? AND IsDisplayed='Y' ORDER BY SeqNo;";

		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, AD_Tab_ID);

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				createField(rs, rows);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
		}
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget().equals(confirmPanel.getOKButton()))
		{
			DMSPermission permission = new DMSPermission();

			for (WEditor editor : m_wEditors)
			{
				permission.setValue(editor);
			}

			if (permission.getAD_Role_ID() == null && permission.getAD_User_ID() == null)
				throw new AdempiereException("Please select Role/User");

			permission.setIsAllPermission(false);
			permission.setAD_Client_ID(Env.getAD_Client_ID(ctx));
			permission.setAD_Org_ID(Env.getAD_Org_ID(ctx));
			permission.setDMS_Content_ID(content.getDMS_Content_ID());

			if (MDMSContent.CONTENTBASETYPE_Directory.equals(content.getContentBaseType()))
			{
				Callback <Boolean> callbackWarning = new Callback <Boolean>() {

					@Override
					public void onCallback(Boolean result)
					{
						if (result)
							DMSPermissionUtils.createPermission(permission, content, true);
						else
							DMSPermissionUtils.createPermission(permission, content, false);
						clearFields();
						renderDMSPermission();
					}
				};

				FDialog.ask("Want create permission for the sub Dir ?", 0, this,
								"<b> Are sure you want to create permission for the sub Dir content documents ? </b>", callbackWarning);
			}
			else
			{
				DMSPermissionUtils.createPermission(permission, content, false);
				clearFields();
				renderDMSPermission();
			}
		}
		else if (event.getTarget().equals(confirmPanel.getButton(ConfirmPanel.A_CANCEL)))
		{
			dispose();
		}
		else if (event.getTarget().equals(confirmPanel.getButton(ConfirmPanel.A_NEW)))
		{
			clearFields();
			permissionTbl.clearSelection();
			confirmPanel.getButton(ConfirmPanel.A_DELETE).setDisabled(true);
		}
		else if (event.getTarget().equals(confirmPanel.getButton(ConfirmPanel.A_DELETE)))
		{
			Integer permissionID = permissionTbl.getSelectedRowKey();
			if (permissionID > 0)
			{
				MDMSPermission permission = (MDMSPermission) MTable.get(content.getCtx(), MDMSPermission.Table_ID).getPO(permissionID, content.get_TrxName());
				permission.deleteEx(false);
			}
			clearFields();
			renderDMSPermission();
			confirmPanel.getButton(ConfirmPanel.A_DELETE).setDisabled(true);
		}
	}

	private void clearFields( )
	{
		for (WEditor editor : m_wEditors)
		{
			GridField field = editor.getGridField();
			editor.setValue(field.getDefaultForPanel());
			field.validateValueNoDirect();
		}
	}

	/**
	 * Create Field. - creates Fields and adds it to m_mFields list - creates
	 * Editor and adds it to m_vEditors list Handeles Ranges by adding
	 * additional mField/vEditor.
	 * <p>
	 * mFields are used for default value and mandatory checking; vEditors are
	 * used to retrieve the value (no data binding)
	 * 
	 * @param rs
	 *            result set
	 */
	private void createField(ResultSet rs, Rows rows)
	{
		// Create Field
		GridFieldVO voF = GridFieldVO.create(ctx, 0, 0, AD_Window_ID, AD_Tab_ID, false, rs);
		GridField mField = new GridField(voF);

		if (mField.getColumnName().equals(MDMSPermission.COLUMNNAME_AD_Client_ID)
				|| mField.getColumnName().equals(MDMSPermission.COLUMNNAME_AD_Org_ID)
				|| mField.getColumnName().equals(MDMSPermission.COLUMNNAME_DMS_Content_ID)
				|| mField.getColumnName().equals(MDMSPermission.COLUMNNAME_IsActive)
				|| mField.getColumnName().equals(MDMSPermission.COLUMNNAME_IsAllPermission)
				|| (MDMSContent.CONTENTBASETYPE_Content.equals(content.getContentBaseType())
					&& mField.getColumnName().equals(MDMSPermission.COLUMNNAME_IsNavigation)))
			return;

		m_mFields.add(mField); // add to Fields

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
		// streach component to fill grid cell
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
			m_mFields2.add(mField2);
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
			m_mFields2.add(null);
			m_wEditors2.add(null);
			m_separators.add(null);
		}
		rows.appendChild(row);
	} // createField

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
			MDMSPermission permission = (MDMSPermission) MTable.get(ctx, MDMSPermission.Table_ID).getPO(idColumn.getRecord_ID(), content.get_TrxName());
			for (WEditor editor : m_wEditors)
			{
				GridField field = editor.getGridField();
				editor.setValue(permission.get_Value(field.getColumnName()));
				field.validateValueNoDirect();
			}
			confirmPanel.getButton(ConfirmPanel.A_DELETE).setDisabled(false);
		}
	}

}
