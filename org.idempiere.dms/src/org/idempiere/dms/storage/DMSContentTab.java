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

package org.idempiere.dms.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.adwindow.ADTreePanel;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.adwindow.DetailPane;
import org.adempiere.webui.adwindow.GridView;
import org.adempiere.webui.adwindow.IADTabpanel;
import org.adempiere.webui.component.Panel;
import org.compiere.model.DataStatusEvent;
import org.compiere.model.DataStatusListener;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.compiere.util.CLogger;
import org.idempiere.model.DMSSubstituteTableInfo;
import org.idempiere.model.MDMSSubstitute;
import org.idempiere.webui.apps.form.WDMSPanel;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;

public class DMSContentTab extends Panel implements IADTabpanel, DataStatusListener
{
	/**
	 * 
	 */
	private static final long		serialVersionUID	= -2839735607533233505L;

	private static CLogger			log					= CLogger.getCLogger(DMSContentTab.class);

	private GridTab					gridTab				= null;
	private GridView				listPanel			= new GridView();
	private WDMSPanel				docDMSPanel			= null;
	private GridWindow				gridWindow			= null;
	private DetailPane				detailPane			= null;
	private AbstractADWindowContent	adWindowContent		= null;

	private boolean					activated			= false;
	private boolean					detailPaneMode		= false;

	private int						windowNumber		= 0;

	private MDMSSubstitute			substitute			= null;
	private DMSSubstituteTableInfo	ssTableInfo			= null;

	public DMSContentTab()
	{
	}

	@Override
	public String get_ValueAsString(String variableName)
	{
		return gridTab.get_ValueAsString(variableName);
	}

	@Override
	public void init(AbstractADWindowContent winPanel, int windowNo, GridTab gridTab, GridWindow gridWindow)
	{
		this.adWindowContent = winPanel;
		this.windowNumber = windowNo;
		this.gridTab = gridTab;
		this.gridWindow = gridWindow;

		if (gridTab.getParentTab() == null)
			throw new AdempiereException("Parent Tab not found");

		// Check if any sbustitute configuration exists for DMS for the table.
		substitute = MDMSSubstitute.get(gridTab.getParentTab().getAD_Table_ID());

		// Based on Substitute or normal table get Table and RecordID info
		ssTableInfo = new DMSSubstituteTableInfo(winPanel, gridTab.getParentTab(), substitute);
		ssTableInfo.updateRecord();

		// Initiate the DMS Panel
		docDMSPanel = new WDMSPanel(ssTableInfo.getTable_ID(), ssTableInfo.getRecord_ID(), adWindowContent);

		this.appendChild(docDMSPanel);

	}

	@Override
	public String getDisplayLogic()
	{
		return gridTab.getDisplayLogic();
	}

	@Override
	public int getTabLevel()
	{
		return gridTab.getTabLevel();
	}

	@Override
	public String getTableName()
	{
		return gridTab.getTableName();
	}

	@Override
	public int getRecord_ID()
	{
		return gridTab.getRecord_ID();
	}

	@Override
	public boolean isCurrent()
	{
		return gridTab.isCurrent();
	}

	@Override
	public String getTitle()
	{
		return gridTab.getName();
	}

	@Override
	public void createUI()
	{
		// set Current Record ID
		ssTableInfo.updateRecord();

		int tableID = ssTableInfo.getTable_ID();
		int recordID = ssTableInfo.getRecord_ID();
		String tableName = ssTableInfo.getTable_Name();

		docDMSPanel.clearComponents();
		docDMSPanel.setButtonsContentCreationEnabled(true);
		docDMSPanel.allowUserToCreateDir();
		docDMSPanel.setNavigationButtonEnabled(false);
		docDMSPanel.setTable_ID(tableID);
		docDMSPanel.setRecord_ID(recordID);
		docDMSPanel.getDMS().initiateMountingContent(tableName, recordID, tableID);

		reload();
	}

	public void reload()
	{
		// set Current Record ID
		ssTableInfo.updateRecord();

		docDMSPanel.getBreadRow().getChildren().clear();
		docDMSPanel.addRootBreadCrumb();
		docDMSPanel.setCurrDMSContent(docDMSPanel.getDMS().getMountingStrategy().getMountingParent(ssTableInfo.getTable_Name(), ssTableInfo.getRecord_ID()));

		renderViewer();
	}

	@Override
	public GridTab getGridTab()
	{
		return this.gridTab;
	}

	@Override
	public void activate(boolean b)
	{
		activated = true;
		Event event = new Event(ON_ACTIVATE_EVENT, this, activated);
		Events.postEvent(event);
	}

	@Override
	public void query()
	{
		boolean open = gridTab.isOpen();
		gridTab.query(false);
		if (!open)
		{
			gridTab.getTableModel().fireTableDataChanged();
		}
	}

	@Override
	public void refresh()
	{
		// set Current Record ID
		ssTableInfo.updateRecord();

		docDMSPanel.setTable_ID(ssTableInfo.getTable_ID());
		docDMSPanel.setRecord_ID(ssTableInfo.getRecord_ID());
		//
		renderViewer();
	}

	@Override
	public void query(boolean currentRows, int currentDays, int maxRows)
	{
		boolean open = gridTab.isOpen();
		gridTab.query(currentRows, currentDays, maxRows);
		if (!open)
		{
			gridTab.getTableModel().fireTableDataChanged();
		}
	}

	@Override
	public void switchRowPresentation()
	{
	}

	@Override
	public void dynamicDisplay(int i)
	{
	}

	@Override
	public void afterSave(boolean onSaveEvent)
	{
	}

	@Override
	public boolean onEnterKey()
	{
		if (listPanel.isVisible())
		{
			return listPanel.onEnterKey();
		}
		return false;
	}

	@Override
	public boolean isGridView()
	{
		return false;
	}

	@Override
	public boolean isActivated()
	{
		return activated;
	}

	@Override
	public void setDetailPaneMode(boolean detailMode)
	{
		this.detailPaneMode = detailMode;
		this.setVflex("true");
	}

	@Override
	public boolean isDetailPaneMode()
	{
		return this.detailPaneMode;
	}

	@Override
	public GridView getGridView()
	{
		return this.listPanel;
	}

	@Override
	public boolean needSave(boolean rowChange, boolean onlyRealChange)
	{
		return false;
	}

	@Override
	public boolean dataSave(boolean onSaveEvent)
	{
		return false;
	}

	@Override
	public void setTabNo(int tabNo)
	{
	}

	@Override
	public int getTabNo()
	{
		return gridTab.getTabNo();
	}

	@Override
	public void setDetailPane(DetailPane detailPane)
	{
		this.detailPane = detailPane;
	}

	@Override
	public DetailPane getDetailPane()
	{
		return detailPane;
	}

	@Override
	public void resetDetailForNewParentRecord()
	{
	}

	@Override
	public ADTreePanel getTreePanel()
	{
		return null;
	}

	public AbstractADWindowContent getAdWindowContent()
	{
		return adWindowContent;
	}

	public int getWindowNumber()
	{
		return windowNumber;
	}

	public GridWindow getGridWindow()
	{
		return gridWindow;
	}

	@Override
	public void dataStatusChanged(DataStatusEvent e)
	{
		if (e.getAD_Message().equals(GridTab.DEFAULT_STATUS_MESSAGE))
		{
			reload();
		}
	}

	private void renderViewer()
	{
		try
		{
			docDMSPanel.renderViewer();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render component problem", e);
			throw new AdempiereException("Render component problem: " + e.getLocalizedMessage());
		}
	}

	@Override
	public boolean isEnableCustomizeButton()
	{
		return false;
	}

	@Override
	public boolean isEnableProcessButton()
	{
		return false;
	}

	@Override
	public List<Button> getToolbarButtons()
	{
		return new ArrayList<Button>();
	}

	@Override
	public boolean isEnableQuickFormButton()
	{
		return false;
	}
}
