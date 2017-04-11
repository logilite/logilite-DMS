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
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.Utils;
import org.idempiere.webui.apps.form.WDMSPanel;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Window.Mode;

public class CustomToolbarFactory implements IAction
{

	private AbstractADWindowContent	winContent	= null;
	private WDMSPanel				dmsPanel	= null;
	private Window					dmsWindow	= null;
	public static CLogger			log			= CLogger.getCLogger(CustomToolbarFactory.class);

	@Override
	public void execute(Object target)
	{
		ADWindow window = (ADWindow) target;
		winContent = window.getADWindowContent();
		int record_ID = winContent.getADTab().getSelectedGridTab().getRecord_ID();
		int table_ID = winContent.getADTab().getSelectedGridTab().getAD_Table_ID();

		if (record_ID == -1 || table_ID == -1)
			return;

		dmsPanel = new WDMSPanel(table_ID, record_ID, winContent);

		dmsWindow = new Window();
		dmsWindow.setHeight("80%");
		dmsWindow.setWidth("80%");
		dmsWindow.setClosable(true);
		dmsWindow.setMaximizable(true);
		dmsWindow.setMode(Mode.OVERLAPPED);
		dmsWindow.setTitle("Document Explorer");
		dmsWindow.setParent(window.getComponent());
		dmsWindow.appendChild(dmsPanel);
		dmsWindow.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

			@Override
			public void onEvent(Event arg0) throws Exception
			{
				int associateRecords = DB.getSQLValue(null,
						"SELECT COUNT(DMS_Association_ID) FROM DMS_Association WHERE AD_Table_ID = ? AND Record_ID = ? "
								+ " AND DMS_AssociationType_ID NOT IN (1000000,1000001,1000002,1000003) AND DMS_AssociationType_ID IS NOT NULL",
						winContent.getADTab().getSelectedGridTab().getAD_Table_ID(),
						winContent.getADTab().getSelectedGridTab().getRecord_ID());

				winContent.getToolbar().getButton("Document Explorer").setPressed((associateRecords > 0));
			}
		});

		Utils.initiateMountingContent(winContent.getADTab().getSelectedGridTab().getTableName(), record_ID, table_ID);

		try
		{
			IMountingStrategy mountingStrategy = Utils.getMountingStrategy(winContent.getADTab().getSelectedGridTab()
					.getTableName());
			dmsPanel.setCurrDMSContent(mountingStrategy.getMountingParent(winContent.getADTab().getSelectedGridTab()
					.getTableName(), record_ID));
			dmsPanel.renderViewer();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render Component Problem.", e);
			throw new AdempiereException("Render Component Problem: " + e);
		}
		SessionManager.getAppDesktop().showWindow(dmsWindow);
	}
}
