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

package com.logilite.dms.form;

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Space;

import com.logilite.dms.DMS;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.MDMSAssociationType;
import com.logilite.dms.model.MDMSContent;

public class WDAssociationType extends Window implements EventListener<Event>
{

	private static final long		serialVersionUID	= 4397569198011705268L;
	private static final CLogger	log					= CLogger.getCLogger(WDAssociationType.class);

	private WTableDirEditor			associationType;
	private Row						associationTypeRow	= new Row();
	private Grid					gridView			= GridFactory.newGridLayout();
	private Label					lblAssociationType	= new Label();

	private Button					btnClose			= null;
	private Button					btnOk				= null;
	private ConfirmPanel			confirmPanel		= null;

	private int						AD_Table_ID			= 0;
	private int						Record_ID			= 0;

	private DMS						dms;
	private MDMSContent				copyDMSContent		= null;
	private MDMSContent				associateContent	= null;
	private AbstractADWindowContent	winContent			= null;

	/**
	 * @param dms
	 * @param copyDMSContent
	 * @param associateContent
	 * @param AD_Table_ID
	 * @param Record_ID
	 * @param winContent
	 */
	public WDAssociationType(	DMS dms, MDMSContent copyDMSContent, MDMSContent associateContent, int AD_Table_ID, int Record_ID,
								AbstractADWindowContent winContent)
	{
		this.dms = dms;
		this.copyDMSContent = copyDMSContent;
		this.associateContent = associateContent;
		this.AD_Table_ID = AD_Table_ID;
		this.Record_ID = Record_ID;
		this.winContent = winContent;

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
		if (ClientInfo.isMobile())
			ZKUpdateUtil.setWindowWidthX(this, 320);
		else
			this.setWidth("35%");

		gridView.setStyle("position:relative;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");

		int Column_ID = MColumn.getColumn_ID(MDMSAssociationType.Table_Name, MDMSAssociationType.COLUMNNAME_DMS_AssociationType_ID);
		MLookup lookup = null;
		try
		{
			lookup = MLookupFactory.get(Env.getCtx(), 0, Column_ID, DisplayType.TableDir, Env.getLanguage(Env.getCtx()),
										MDMSAssociationType.COLUMNNAME_DMS_AssociationType_ID, 0, true,
										"DMS_AssociationType.DMS_AssociationType_ID NOT IN (SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE EntityType = 'D')");

			associationType = new WTableDirEditor(MDMSAssociationType.COLUMNNAME_DMS_AssociationType_ID, true, false, true, lookup);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Association type fetching failure :", e);
			throw new AdempiereException("Association type fetching failure :" + e);
		}

		Rows rows = new Rows();
		gridView.appendChild(rows);

		lblAssociationType.setValue("Association Type*");
		associationTypeRow.appendCellChild(lblAssociationType);
		associationTypeRow.appendCellChild(associationType.getComponent(), 2);
		rows.appendChild(associationTypeRow);

		confirmPanel = new ConfirmPanel();
		btnClose = confirmPanel.createButton(ConfirmPanel.A_CANCEL);
		btnOk = confirmPanel.createButton(ConfirmPanel.A_OK);

		Row row = new Row();
		rows.appendChild(row);
		Cell cell = new Cell();
		cell.setAlign("right");
		cell.setColspan(3);
		cell.appendChild(btnOk);
		cell.appendChild(new Space());
		cell.appendChild(btnClose);
		row.appendChild(cell);
		cell.setStyle("position: relative;");

		btnClose.addEventListener(Events.ON_CLICK, this);
		btnOk.addEventListener(Events.ON_CLICK, this);

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
			if (associationType.getValue() == null || (Integer) associationType.getValue() == 0)
				throw new WrongValueException(associationType.getComponent(), DMSConstant.MSG_FILL_MANDATORY);

			int countAssociations = DB.getSQLValue(	null, "SELECT COUNT(DMS_Association_ID) FROM DMS_Association WHERE DMS_Content_ID = ?"
															+ "  AND DMS_Content_Related_ID = ? AND DMS_AssociationType_ID = ?",
													associateContent.getDMS_Content_ID(),
													copyDMSContent.getDMS_Content_ID(),
													(Integer) associationType.getValue());

			if (countAssociations == 0)
			{
				dms.createAssociation(	associateContent.getDMS_Content_ID(), copyDMSContent.getDMS_Content_ID(), Record_ID, AD_Table_ID,
										(int) associationType.getValue(), null);

				if (AD_Table_ID > 0 && Record_ID > 0)
					winContent.getToolbar().getButton(DMSConstant.TOOLBAR_BUTTON_DOCUMENT_EXPLORER).setPressed(true);
			}
			else
			{
				Dialog.warn(0, "This Document is already associate.");
			}

			this.detach();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			this.detach();
		}
	}
}
