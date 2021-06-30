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

import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.util.Utils;
import org.idempiere.model.MDMSContent;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;

public class WRenameContent extends Window implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -4440351217070536198L;

	private DMS					dms;
	private MDMSContent			content				= null;
	private MDMSContent			parent_Content		= null;

	private Grid				gridView			= GridFactory.newGridLayout();
	private ConfirmPanel		confirmpanel		= new ConfirmPanel();

	private Textbox				txtName				= new Textbox();
	private Textbox				txtDesc				= new Textbox();

	private Button				btnOk				= null;
	private Button				btnCancel			= null;

	private boolean				isCancel			= false;

	public WRenameContent(DMS dms, MDMSContent DMSContent, int tableID, int recordID)
	{
		this.dms = dms;
		this.content = DMSContent;

		init();
	}

	private void init()
	{
		if (!ClientInfo.isMobile())
		{
			this.setWidth("30%");
			this.setHeight("32%");
		}

		this.setClosable(true);
		this.setTitle(DMSConstant.MSG_RENAME);
		this.addEventListener(Events.ON_OK, this);
		this.setStyle("max-widht:230px; max-height:230px;");
		this.setStyle("min-widht:230px; min-height:230px;");
		this.appendChild(gridView);

		gridView.setStyle("position:relative; overflow:auto;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");
		gridView.setStyle("max-widht:230px; max-height:230px;");
		gridView.setStyle("min-widht:230px; min-height:230px;");

		parent_Content = new MDMSContent(Env.getCtx(), content.getDMS_Content_Related_ID(), null);
		if (content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Content))
		{
			txtName.setValue(parent_Content.getName().substring(0, parent_Content.getName().lastIndexOf(".")));
			txtName.setMaxlength(DMSConstant.MAX_FILENAME_LENGTH);
		}
		else
		{
			txtName.setText(content.getName());
			txtName.setMaxlength(DMSConstant.MAX_DIRECTORY_LENGTH);
		}

		txtName.setFocus(true);
		txtName.setSelectionRange(0, txtName.getValue().length() - 1);
		txtName.setWidth("98%");

		txtDesc.setRows(2);
		txtDesc.setWidth("98%");
		txtDesc.setMultiline(true);
		txtDesc.setValue(content.getDescription());

		btnOk = confirmpanel.createButton(ConfirmPanel.A_OK);
		btnOk.addEventListener(Events.ON_CLICK, this);

		btnCancel = confirmpanel.createButton(ConfirmPanel.A_CANCEL);
		btnCancel.addEventListener(Events.ON_CLICK, this);

		Rows rows = gridView.newRows();

		Row row = rows.newRow();
		row.appendChild(new Label(DMSConstant.MSG_ENTER_NEW_NAME_FOR_ITEM));

		row = rows.newRow();
		row.appendChild(txtName);

		row = rows.newRow();
		row.appendChild(new Label(DMSConstant.MSG_DESCRIPTION));

		row = rows.newRow();
		row.appendChild(txtDesc);

		row = rows.newRow();
		Cell cell = new Cell();
		cell.setAlign("right");
		cell.appendChild(btnOk);
		cell.appendChild(btnCancel);
		row.appendChild(cell);

		AEnv.showCenterScreen(this);
	} // init

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			isCancel = true;
			this.detach();
		}
		if (event.getTarget().getId().equals(ConfirmPanel.A_OK) || Events.ON_OK.equals(event.getName()))
		{
			renameContent();
		}
	}

	private void ValidateName()
	{
		boolean isDir = content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory);
		String error = Utils.isValidFileName(txtName.getValue(), isDir);
		if (!Util.isEmpty(error, true))
			throw new WrongValueException(txtName, error);
	} // ValidateName

	private void renameContent()
	{
		String description = "";
		if (!txtDesc.getValue().equals(content.getDescription()))
		{
			description = txtDesc.getValue();
		}

		ValidateName();

		dms.renameContentOnly(content, txtName.getValue(), description);

		this.detach();
	} // renameContent

	/**
	 * @return true if dialog cancel by user
	 */
	public boolean isCancel()
	{
		return isCancel;
	}
}
