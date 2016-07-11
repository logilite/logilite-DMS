package org.idempiere.dms.toolbar;

import java.io.IOException;
import java.net.URISyntaxException;

import org.adempiere.webui.action.IAction;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.session.SessionManager;
import org.idempiere.webui.apps.form.WDMSPanel;
import org.zkoss.zul.Window.Mode;

public class CustomToolbarFactory implements IAction
{

	private AbstractADWindowContent	winContent	= null;
	private WDMSPanel				dmsPanel	= null;
	private Window					dmsWindow	= null;

	@Override
	public void execute(Object target)
	{
		ADWindow window = (ADWindow) target;
		winContent = window.getADWindowContent();
		int record_ID = winContent.getADTab().getSelectedGridTab().getRecord_ID();
		int table_ID = winContent.getADTab().getSelectedGridTab().getAD_Table_ID();

		if (record_ID == -1 || table_ID == -1)
			return;

		dmsPanel = new WDMSPanel();

		dmsPanel.setTable_ID(table_ID);
		dmsPanel.setRecord_ID(record_ID);

		dmsWindow = new Window();
		dmsWindow.setHeight("80%");
		dmsWindow.setWidth("80%");
		dmsWindow.setClosable(true);
		dmsWindow.setMaximizable(true);
		dmsWindow.setMode(Mode.OVERLAPPED);
		dmsWindow.setTitle("Document Explorer");
		dmsWindow.setParent(window.getComponent());
		dmsWindow.appendChild(dmsPanel);
		try
		{
			dmsPanel.renderViewer();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (URISyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SessionManager.getAppDesktop().showWindow(dmsWindow);

	}
}
