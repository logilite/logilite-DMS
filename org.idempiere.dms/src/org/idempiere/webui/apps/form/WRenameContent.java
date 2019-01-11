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

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
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
import org.idempiere.dms.factories.Utils;
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
	private MDMSContent			DMSContent			= null;
	private MDMSContent			parent_Content		= null;

	private Grid				gridView			= null;
	private ConfirmPanel		confirmpanel		= null;

	private Textbox				txtName				= null;
	private Textbox				txtDesc				= null;

	private Label				lblName				= null;
	private Label				lblDesc				= null;

	private Button				btnOk				= null;
	private Button				btnCancel			= null;

	private boolean				cancel				= false;

	public WRenameContent(DMS dms, MDMSContent DMSContent)
	{
		this.dms = dms;
		this.DMSContent = DMSContent;

		init();
	}

	private void init()
	{
		gridView = GridFactory.newGridLayout();

		this.setHeight("32%");
		this.setWidth("30%");
		this.setTitle("Rename");
		this.appendChild(gridView);
		this.setClosable(true);
		this.addEventListener(Events.ON_OK, this);
		this.setStyle("max-widht:230px; max-height:230px;");
		this.setStyle("min-widht:230px; min-height:230px;");

		gridView.setStyle("position:relative; overflow:auto;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");
		gridView.setStyle("max-widht:230px; max-height:230px;");
		gridView.setStyle("min-widht:230px; min-height:230px;");

		Columns columns = new Columns();
		Rows rows = new Rows();

		lblName = new Label("Please enter a new name for the item:");
		txtName = new Textbox();

		lblDesc = new Label("Description");
		txtDesc = new Textbox();
		txtDesc.setMultiline(true);
		txtDesc.setRows(2);

		parent_Content = new MDMSContent(Env.getCtx(), Utils.getDMS_Content_Related_ID(DMSContent), null);
		if (DMSContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Content))
			txtName.setValue(parent_Content.getName().substring(0, parent_Content.getName().lastIndexOf(".")));
		else
			txtName.setText(DMSContent.getName());

		txtDesc.setValue(DMSContent.getDescription());

		txtName.setFocus(true);
		txtName.setSelectionRange(0, txtName.getValue().length() - 1);
		txtName.setWidth("98%");
		txtDesc.setWidth("98%");

		confirmpanel = new ConfirmPanel();

		btnOk = confirmpanel.createButton(ConfirmPanel.A_OK);
		btnCancel = confirmpanel.createButton(ConfirmPanel.A_CANCEL);

		gridView.appendChild(columns);
		gridView.appendChild(rows);

		Column column = new Column();
		columns.appendChild(column);

		Row row = new Row();
		row.appendChild(lblName);
		rows.appendChild(row);

		row = new Row();
		row.appendChild(txtName);
		rows.appendChild(row);

		row = new Row();
		row.appendChild(lblDesc);
		rows.appendChild(row);

		row = new Row();
		row.appendChild(txtDesc);
		rows.appendChild(row);

		row = new Row();
		Cell cell = new Cell();
		cell.setAlign("right");
		cell.appendChild(btnOk);
		cell.appendChild(btnCancel);
		row.appendChild(cell);
		rows.appendChild(row);

		btnOk.addEventListener(Events.ON_CLICK, this);
		btnCancel.addEventListener(Events.ON_CLICK, this);
		btnOk.setImageContent(Utils.getImage("Ok24.png"));
		btnCancel.setImageContent(Utils.getImage("Cancel24.png"));

		AEnv.showCenterScreen(this);
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			cancel = true;
			this.detach();
		}
		if (event.getTarget().getId().equals(ConfirmPanel.A_OK) || Events.ON_OK.equals(event.getName()))
		{
			renameContent();
		}
	}

	private void ValidateName()
	{
		if (Util.isEmpty(txtName.getValue()))
		{
			throw new WrongValueException(txtName, DMSConstant.MSG_FILL_MANDATORY);
		}
		else if (DMSContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			if (txtName.getValue().length() > DMSConstant.MAX_FILENAME_LENGTH)
				throw new WrongValueException(txtName, "Invalid Directory Name. Directory name less than 250 character");

			if (txtName.getValue().contains(DMSConstant.FILE_SEPARATOR))
				throw new WrongValueException(txtName, "Invalid Directory Name.");
		}
		else if (DMSContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Content))
		{
			try
			{
				Utils.isValidFileName(txtName.getValue(), true);
			}
			catch (WrongValueException e)
			{
				throw new WrongValueException(txtName, e.getMessage());
			}
		}
	}

	private void renameContent()
	{
		if (!txtDesc.getValue().equals(DMSContent.getDescription()))
		{
			DMSContent.setDescription(txtDesc.getValue());
			DMSContent.save();
		}

		ValidateName();

		dms.renameContent(txtName.getValue(), DMSContent, parent_Content);

		this.detach();
	} // renameContent

	/**
	 * @return true if dialog cancel by user
	 */
	public boolean isCancel()
	{
		return cancel;
	}
}
