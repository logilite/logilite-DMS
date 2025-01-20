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

package com.logilite.dms.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.adwindow.ADTreePanel;
import org.adempiere.webui.adwindow.ADWindowToolbar;
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
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Toolbar;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.form.WDMSPanel;

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

	public DMSContentTab()
	{
	}

	@Override
	public String get_ValueAsString(String variableName)
	{
		return gridTab.get_ValueAsString(variableName);
	}

	@Override
	public void init(AbstractADWindowContent winPanel, GridTab gridTab)
	{
		this.adWindowContent = winPanel;
		this.windowNumber = winPanel.getWindowNo();
		this.gridTab = gridTab;
		this.gridWindow = gridTab.getGridWindow();

		if (gridTab.getParentTab() == null)
			throw new AdempiereException("Parent Tab not found");

		docDMSPanel = new WDMSPanel(gridTab.getParentTab().getAD_Table_ID(), gridTab.getParentTab().getRecord_ID(), adWindowContent);
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
		int tableID = gridTab.getParentTab().getAD_Table_ID();
		int recordID = gridTab.getParentTab().getRecord_ID();
		String tableName = gridTab.getParentTab().getTableName();

		docDMSPanel.clearComponents();

		// if detail tab as DMS and parent tab to create new record then prevent to do any
		// operations.
		if (tableID > 0 && recordID > 0)
		{
			docDMSPanel.setVisible(true);
			docDMSPanel.setButtonsContentCreationEnabled(true);
			docDMSPanel.allowUserToCreateDir();
			docDMSPanel.setNavigationButtonEnabled(false);
			docDMSPanel.getDMS().initiateMountingContent(tableName, recordID, tableID);
			//
			reload();
			// Fire event for drag and drop functionality in DMS tab as Detail tab
			Events.postEvent(DMSConstant.EVENT_ON_LOAD_DRAGNDROP, docDMSPanel, null);
		}
		else
		{
			docDMSPanel.setButtonsContentCreationEnabled(false);
			docDMSPanel.setNavigationButtonEnabled(false);
			docDMSPanel.setVisible(false);
		}
	}

	public void reload()
	{
		int tableID = gridTab.getParentTab().getAD_Table_ID();
		int recordID = gridTab.getParentTab().getRecord_ID();
		docDMSPanel.setTable_ID(tableID);
		docDMSPanel.setRecord_ID(recordID);
		docDMSPanel.getBreadRow().getChildren().clear();
		docDMSPanel.addRootBreadCrumb();
		docDMSPanel.setCurrDMSContent(docDMSPanel.getDMS().getRootMountingContent(tableID, recordID));
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
		docDMSPanel.setTable_ID((gridTab.getParentTab().getAD_Table_ID()));
		docDMSPanel.setRecord_ID(gridTab.getParentTab().getRecord_ID());
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

	@Override
	public void updateToolbar(ADWindowToolbar toolbar)
	{

	}

	@Override
	public void updateDetailToolbar(Toolbar toolbar)
	{

	}
}
