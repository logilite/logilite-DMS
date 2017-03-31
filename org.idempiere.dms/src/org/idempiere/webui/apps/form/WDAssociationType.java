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

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
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
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_AssociationType;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Space;

public class WDAssociationType extends Window implements EventListener<Event>
{

	private static final long		serialVersionUID	= 4397569198011705268L;
	protected static final CLogger	log					= CLogger.getCLogger(WDAssociationType.class);

	private WTableDirEditor			associationType;
	private Grid					gridView			= GridFactory.newGridLayout();
	private Label					lblAssociationType	= new Label();

	private Button					btnClose			= null;
	private Button					btnOk				= null;
	private ConfirmPanel			confirmPanel		= null;

	private Row						associationTypeRow	= new Row();
	private MDMSContent				copyDMSContent		= null;
	private MDMSContent				associateContent	= null;
	
	private int						AD_Table_ID			= 0;
	private int						Record_ID			= 0;

	/**
	 * @param copyDMSContent
	 * @param associateContent
	 */
	public WDAssociationType(MDMSContent copyDMSContent, MDMSContent associateContent, int AD_Table_ID, int Record_ID)
	{
		this.copyDMSContent = copyDMSContent;
		this.associateContent = associateContent;
		this.AD_Table_ID = AD_Table_ID;
		this.Record_ID = Record_ID;

		try
		{
			init();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Problem in Rendering Association type Form.", e);
			throw new AdempiereException("Problem in Rendering Association type Form." + e);
		}
	}

	/**
	 * initialize components
	 * 
	 * @throws Exception
	 */
	public void init() throws Exception
	{
		this.setTitle("Association Type");
		this.setClosable(true);
		this.appendChild(gridView);
		this.setWidth("35%");
		gridView.setStyle("position:relative;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");

		int Column_ID = MColumn.getColumn_ID(X_DMS_AssociationType.Table_Name,
				X_DMS_AssociationType.COLUMNNAME_DMS_AssociationType_ID);
		MLookup lookup = null;
		try
		{
			lookup = MLookupFactory
					.get(Env.getCtx(), 0, Column_ID, DisplayType.TableDir, Env.getLanguage(Env.getCtx()),
							X_DMS_AssociationType.COLUMNNAME_DMS_AssociationType_ID, 0, true,
							"DMS_AssociationType.DMS_AssociationType_ID NOT IN (SELECT DMS_AssociationType_ID WHERE EntityType = 'D')");

			associationType = new WTableDirEditor(X_DMS_AssociationType.COLUMNNAME_DMS_AssociationType_ID, true, false,
					true, lookup);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Association type fetching failure :", e);
			throw new AdempiereException("Association type fetching failure :" + e);
		}

		Columns columns = new Columns();
		gridView.appendChild(columns);

		Column column = new Column();
		column.setWidth("10%");
		column.setAlign("left");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("15%");
		column.setAlign("left");
		columns.appendChild(column);

		Rows rows = new Rows();
		Row row = null;
		Cell cell = null;
		gridView.appendChild(rows);

		lblAssociationType.setValue("Association Type*");

		associationTypeRow.appendChild(lblAssociationType);
		associationTypeRow.appendChild(associationType.getComponent());
		// associationType.addValueChangeListener(this);
		rows.appendChild(associationTypeRow);

		confirmPanel = new ConfirmPanel();
		btnClose = confirmPanel.createButton(ConfirmPanel.A_CANCEL);
		btnOk = confirmPanel.createButton(ConfirmPanel.A_OK);

		row = new Row();
		rows.appendChild(row);
		cell = new Cell();
		cell.setAlign("right");
		cell.setColspan(2);
		cell.appendChild(btnOk);
		cell.appendChild(new Space());
		cell.appendChild(btnClose);
		row.appendChild(cell);
		cell.setStyle("position: relative;");
		btnClose.addEventListener(Events.ON_CLICK, this);
		btnOk.addEventListener(Events.ON_CLICK, this);
		btnOk.setImageContent(Utils.getImage("Ok24.png"));
		btnClose.setImageContent(Utils.getImage("Cancel24.png"));
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
		if (event.getTarget().getId().equals(ConfirmPanel.A_OK))
		{
			String fillMandatory = Msg.translate(Env.getCtx(), "FillMandatory");

			if (associationType.getValue() == null || (Integer) associationType.getValue() == 0)
				throw new WrongValueException(associationType.getComponent(), fillMandatory);

			int DMS_Association_ID = DB.getSQLValue(null,
					"SELECT count(DMS_Association_ID) FROM DMS_Association where DMS_Content_ID = ?"
							+ "  AND DMS_Content_Related_ID = ? AND DMS_AssociationType_ID = ?",
					associateContent.getDMS_Content_ID(), copyDMSContent.getDMS_Content_ID(),
					(Integer) associationType.getValue());

			if (DMS_Association_ID == 0)
			{
				MDMSAssociation DMSassociation = new MDMSAssociation(Env.getCtx(), 0, null);
				DMSassociation.setDMS_Content_ID(associateContent.getDMS_Content_ID());
				DMSassociation.setDMS_Content_Related_ID(copyDMSContent.getDMS_Content_ID());
				DMSassociation.setDMS_AssociationType_ID((Integer) associationType.getValue());
				DMSassociation.setAD_Table_ID(AD_Table_ID);
				DMSassociation.setRecord_ID(Record_ID);
				DMSassociation.saveEx();
			}
			else
			{
				FDialog.warn(0, "This Document is already associate.");
			}

			this.detach();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			this.detach();
		}
	}
}
