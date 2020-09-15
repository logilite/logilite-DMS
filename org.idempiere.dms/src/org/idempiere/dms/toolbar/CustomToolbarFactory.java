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

package org.idempiere.dms.toolbar;

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.action.IAction;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.session.SessionManager;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.DMSSubstituteTableInfo;
import org.idempiere.model.MDMSSubstitute;
import org.idempiere.webui.apps.form.WDMSPanel;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Window.Mode;

public class CustomToolbarFactory implements IAction
{

	private static CLogger			log			= CLogger.getCLogger(CustomToolbarFactory.class);

	private AbstractADWindowContent	winContent	= null;
	private WDMSPanel				dmsPanel	= null;
	private Window					dmsWindow	= null;

	private MDMSSubstitute			substitute	= null;
	private DMSSubstituteTableInfo	ssTableInfo	= null;

	@Override
	public void execute(Object target)
	{
		// Load DMS CSS file content and attach as style tag in Head tab
		DMS_ZK_Util.loadDMSThemeCSSFile();

		//
		ADWindow window = (ADWindow) target;
		winContent = window.getADWindowContent();

		// Check if any sbustitute configuration exists for DMS for the table.
		substitute = MDMSSubstitute.get(winContent.getADTab().getSelectedGridTab().getAD_Table_ID());

		// Based on Substitute or normal table get Table and RecordID info
		ssTableInfo = new DMSSubstituteTableInfo(winContent, winContent.getADTab().getSelectedGridTab(), substitute);
		ssTableInfo.updateRecord();

		int tableID = ssTableInfo.getTable_ID();
		int recordID = ssTableInfo.getRecord_ID();
		String tableName = ssTableInfo.getTable_Name();

		if (recordID == -1 || tableID == -1 || winContent.getADTab().getSelectedGridTab().getRecord_ID() == -1)
			return;

		dmsPanel = new WDMSPanel(tableID, recordID, winContent);
		dmsPanel.setCurrDMSContent(dmsPanel.getDMS().getMountingStrategy().getMountingParent(tableName, recordID));

		dmsWindow = new Window();
		dmsWindow.setHeight("80%");
		dmsWindow.setWidth("80%");
		dmsWindow.setClosable(true);
		dmsWindow.setMaximizable(true);
		dmsWindow.setMode(Mode.OVERLAPPED);
		dmsWindow.setTitle(DMSConstant.TOOLBAR_BUTTON_DOCUMENT_EXPLORER);
		dmsWindow.setParent(window.getComponent());
		dmsWindow.appendChild(dmsPanel);
		dmsWindow.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

			@Override
			public void onEvent(Event arg0) throws Exception
			{
				int associateRecords = DB
								.getSQLValue(null, "SELECT COUNT(DMS_Association_ID) FROM DMS_Association WHERE AD_Table_ID = ? AND Record_ID = ? "
												+ " AND DMS_AssociationType_ID NOT IN (1000000,1000001,1000002,1000003) AND DMS_AssociationType_ID IS NOT NULL",
												winContent.getADTab().getSelectedGridTab().getAD_Table_ID(),
												winContent.getADTab().getSelectedGridTab().getRecord_ID());

				winContent.getToolbar().getButton(DMSConstant.TOOLBAR_BUTTON_DOCUMENT_EXPLORER).setPressed((associateRecords > 0));
			}
		});

		try
		{

			dmsPanel.renderViewer();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render Component Problem.", e);
			throw new AdempiereException("Render Component Problem: " + e);
		}

		SessionManager.getAppDesktop().showWindow(dmsWindow);
	} // execute
}
