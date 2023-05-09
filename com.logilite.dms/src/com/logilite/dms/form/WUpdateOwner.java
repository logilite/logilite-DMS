package com.logilite.dms.form;

import java.util.HashMap;

import org.adempiere.util.Callback;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Space;

import com.logilite.dms.DMS;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSPermission;

/**
 * Update the Owner of the content dialog
 */
public class WUpdateOwner extends Window implements EventListener<Event>
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6806826981614309201L;

	private DMS					dms;
	private MDMSContent			content				= null;

	private boolean				isDMSAdmin			= false;

	private Grid				gridView			= GridFactory.newGridLayout();
	private Button				btnChangeOwner		= new Button(DMSConstant.MSG_CHANGE_OWNER);
	private Label				lblCurrentOwner		= new Label(DMSConstant.MSG_CURRENT_OWNER);
	private Label				lblNewOwner			= new Label(DMSConstant.MSG_NEW_OWNER);
	private WSearchEditor		currentOwner;
	private WSearchEditor		newOwner;

	/**
	 * @param  dms
	 * @param  DMSContent
	 * @throws Exception  Update owner of the content when the user is DMSAdmin or content Creator
	 */
	public WUpdateOwner(DMS dms, MDMSContent DMSContent) throws Exception
	{
		this.dms = dms;
		this.content = DMSContent;

		isDMSAdmin = MRole.getDefault().get_ValueAsBoolean(DMSConstant.COLUMNNAME_IS_DMS_ADMIN);

		init();
	}

	private void init() throws Exception
	{
		if (!ClientInfo.isMobile())
		{
			this.setWidth("25%");
			this.setHeight("25%");
		}

		this.setTitle(DMSConstant.MSG_OWNER);
		this.setSizable(true);
		this.setClosable(true);
		this.setMaximizable(true);
		this.addEventListener(Events.ON_OK, this);
		this.setStyle("min-widht:150px; min-height:150px;");
		this.appendChild(gridView);

		gridView.setStyle("position:relative; overflow:auto;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");
		gridView.setStyle("min-widht:230px; min-height:230px;");

		int columnID = MColumn.getColumn_ID(MUser.Table_Name, MUser.COLUMNNAME_AD_User_ID);
		MLookup lookup = MLookupFactory.get(Env.getCtx(), 0, columnID, DisplayType.Search, Env.getLanguage(Env.getCtx()), MUser.COLUMNNAME_AD_User_ID, 0, true,
											"");
		// Current Owner
		currentOwner = new WSearchEditor(MUser.COLUMNNAME_AD_User_ID, true, true, true, lookup);
		currentOwner.setValue(content.getDMS_Owner_ID());

		// New Owner
		newOwner = new WSearchEditor(MUser.COLUMNNAME_AD_User_ID, true, false, true, lookup);

		//
		btnChangeOwner.addEventListener(Events.ON_CLICK, this);
		btnChangeOwner.setVisible(isDMSAdmin || content.getDMS_Owner_ID() == Env.getAD_User_ID(Env.getCtx()));
		btnChangeOwner.setWidth("98%");
		btnChangeOwner.setHeight("40px");

		//
		Rows rows = gridView.newRows();

		Row row = rows.newRow();
		row.appendCellChild(lblCurrentOwner.rightAlign(), 2);
		row.appendCellChild(new Space(), 1);
		row.appendCellChild(currentOwner.getComponent(), 5);
		row.setHeight("40px");

		row = rows.newRow();
		rows.newRow();
		row.appendCellChild(lblNewOwner.rightAlign(), 2);
		row.appendCellChild(new Space(), 1);
		row.appendCellChild(newOwner.getComponent(), 5);
		row.setVisible(isDMSAdmin || content.getDMS_Owner_ID() == Env.getAD_User_ID(Env.getCtx()));
		row.setHeight("40px");

		row = rows.newRow();
		row.setAlign("center");
		row.appendCellChild(btnChangeOwner, 8);
		row.setHeight("40px");

		AEnv.showCenterScreen(this);
	} // init

	@Override
	public void onEvent(Event event) throws Exception
	{
		HashMap<String, Object> mapColumnValue = new HashMap<String, Object>();

		mapColumnValue.put(MDMSPermission.COLUMNNAME_AD_Role_ID, 0);
		mapColumnValue.put(MDMSPermission.COLUMNNAME_AD_User_ID, newOwner.getValue());
		mapColumnValue.put(MDMSPermission.COLUMNNAME_IsRead, true);
		mapColumnValue.put(MDMSPermission.COLUMNNAME_IsWrite, true);
		mapColumnValue.put(MDMSPermission.COLUMNNAME_IsDelete, true);

		if (btnChangeOwner.equals(event.getTarget()))
		{
			if (newOwner.getValue() != null)
			{
				content.setDMS_Owner_ID((int) newOwner.getValue());
				content.saveEx();

				if (MDMSContent.CONTENTBASETYPE_Directory.equals(content.getContentBaseType()))
				{
					Callback<Boolean> callbackConfirmation = new Callback<Boolean>() {

						@Override
						public void onCallback(Boolean result)
						{
							dms.getPermissionManager().createPermission(content, mapColumnValue, result);
						}
					};

					FDialog.ask("Grant permission for the child content?", 0, this, "GrantPermissionToChildContent?",
								callbackConfirmation);
				}
				else
				{
					dms.getPermissionManager().createPermission(content, mapColumnValue, false);
				}
				this.detach();
			}
			else
			{
				FDialog.error(0, newOwner.getComponent(), "Please update the new owner...!!!");
			}
		}
	}
}
