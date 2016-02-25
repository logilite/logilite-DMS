package org.idempiere.dms.storage;

import org.adempiere.webui.adwindow.ADTreePanel;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.adwindow.DetailPane;
import org.adempiere.webui.adwindow.GridView;
import org.adempiere.webui.adwindow.IADTabpanel;
import org.adempiere.webui.component.Panel;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.idempiere.webui.apps.form.WDMSPanel;

public class DMSContentTab extends Panel implements IADTabpanel
{
	/**
	 * 
	 */
	private static final long		serialVersionUID	= -2839735607533233505L;

	private AbstractADWindowContent	adWindowContent		= null;
	private GridTab					gridTab				= null;
	private int						windowNumber		= 0;
	private GridWindow				gridWindow			= null;
	private WDMSPanel				documentViewerPanel	= null;
	private boolean					activated			= false;
	private GridView				listPanel			= new GridView();
	private boolean					detailPaneMode		= false;
	private DetailPane				detailPane			= null;

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
		documentViewerPanel = new WDMSPanel();
		// listPanel.appendChild(comp);
		this.appendChild(documentViewerPanel);
	}

	@Override
	public GridTab getGridTab()
	{
		return this.gridTab;
	}

	@Override
	public void activate(boolean b)
	{
		/*
		 * activated = true; Event event = new Event(ON_ACTIVATE_EVENT, this,
		 * activated); Events.postEvent(event);
		 */
	}

	@Override
	public void query()
	{
		/*
		 * boolean open = gridTab.isOpen(); gridTab.query(false); if (!open) {
		 * gridTab.getTableModel().fireTableDataChanged(); }
		 */
	}

	@Override
	public void refresh()
	{
		// gridTab.dataRefresh();
		createUI();
	}

	@Override
	public void query(boolean currentRows, int currentDays, int maxRows)
	{
		/*
		 * boolean open = gridTab.isOpen(); gridTab.query(currentRows,
		 * currentDays, maxRows); if (!open) {
		 * gridTab.getTableModel().fireTableDataChanged(); }
		 */
	}

	@Override
	public void switchRowPresentation()
	{
	}

	@Override
	public void dynamicDisplay(int i)
	{
		createUI();
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
		return listPanel.isVisible();
	}

	@Override
	public boolean isActivated()
	{
		return activated;
	}

	@Override
	public void setDetailPaneMode(boolean detailMode)
	{
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
}
