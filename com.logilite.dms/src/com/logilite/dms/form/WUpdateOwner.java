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

public class WUpdateOwner extends Window implements EventListener<Event>
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6806826981614309201L;

	private DMS					dms;

	private Grid				gridView			= GridFactory.newGridLayout();
	private MDMSContent			content				= null;

	private Label				lblCurrentOwner		= new Label(DMSConstant.MSG_CURRENT_OWNER);
	private Label				lblNewOwner			= new Label(DMSConstant.MSG_NEW_OWNER);

	private Button				btnChangeOwner		= new Button(DMSConstant.MSG_CHANGE_OWNER);

	private boolean				isDMSAdmin			= false;

	private WSearchEditor		currentOwner;
	private WSearchEditor		newOwner;

	/**
	 * @param  dms
	 * @param  DMSContent
	 * @throws Exception
	 * Update owner of the content when the user is DMSAdmin or content Creator
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
			this.setWidth("27%");
			this.setHeight("27%");
		}

		this.setClosable(true);
		this.setTitle(DMSConstant.MSG_OWNER);
		this.addEventListener(Events.ON_OK, this);
		this.setStyle("max-widht:230px; max-height:230px;");
		this.setStyle("min-widht:230px; min-height:230px;");
		this.setSizable(true);
		this.setMaximizable(true);
		this.appendChild(gridView);

		gridView.setStyle("position:relative; overflow:auto;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");
		gridView.setStyle("max-widht:230px; max-height:230px;");
		gridView.setStyle("min-widht:230px; min-height:230px;");

		int columnID = MColumn.getColumn_ID(MUser.Table_Name, MUser.COLUMNNAME_AD_User_ID);
		MLookup lookup = MLookupFactory.get(Env.getCtx(), 0, columnID, DisplayType.Search, Env.getLanguage(Env.getCtx()), MUser.COLUMNNAME_AD_User_ID, 0, true,
											"");

		currentOwner = new WSearchEditor(MUser.COLUMNNAME_AD_User_ID, true, true, true, lookup);
		currentOwner.setValue(content.getDMS_Owner_ID());

		newOwner = new WSearchEditor(MUser.COLUMNNAME_AD_User_ID, true, false, true, lookup);

		btnChangeOwner.addEventListener(Events.ON_CLICK, this);
		btnChangeOwner.setVisible(isDMSAdmin || content.getDMS_Owner_ID() == Env.getAD_User_ID(Env.getCtx()));
		btnChangeOwner.setWidth("70%");
		btnChangeOwner.setStyle("margin-left:100px !important");
		btnChangeOwner.setLeft("100px");

		Rows rows = gridView.newRows();

		Row row = rows.newRow();
		row.appendCellChild(lblCurrentOwner.rightAlign(), 3);
		row.appendCellChild(currentOwner.getComponent(), 7);
		row.appendCellChild(new Space(), 1);

		row = rows.newRow();
		rows.newRow();
		row.appendCellChild(lblNewOwner.rightAlign(), 3);
		row.appendCellChild(newOwner.getComponent(), 7);
		row.setVisible(isDMSAdmin || content.getDMS_Owner_ID() == Env.getAD_User_ID(Env.getCtx()));

		row = rows.newRow();
		row.setAlign("center");
		row.appendCellChild(btnChangeOwner, 10);

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

				FDialog.ask("Grant permission for the child content?", 0, this, "Will you grant same permission for the child content documents?",
							callbackConfirmation);
			}
			else
			{
				dms.getPermissionManager().createPermission(content, mapColumnValue, false);
			}

			this.detach();
		}
	}
}
