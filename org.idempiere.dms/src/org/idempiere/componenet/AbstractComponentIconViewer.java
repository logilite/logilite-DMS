package org.idempiere.componenet;

import java.util.HashMap;
import java.util.Map;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.theme.ThemeManager;
import org.idempiere.dms.DMS;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.factories.IDMSViewer;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociationType;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Image;

/**
 * Abstract Component Icon Viewer
 * 
 * @author Sachin
 */
public abstract class AbstractComponentIconViewer implements IDMSViewer, EventListener<Event>
{
	protected static Image LinkImage = new Image();

	static
	{
		LinkImage.setContent(DMS_ZK_Util.getImage("Link16.png"));
	}

	protected DMS								dms;

	protected Grid								grid;
	protected Component							prevComponent;

	protected String[]							eventsList;
	protected EventListener<? extends Event>	listener;

	// Abstract method definition
	public abstract void createHeaderPart();

	public abstract void setNoComponentExistsMsg(Rows rows);

	public abstract void createComponent(Rows rows, I_DMS_Content content, I_DMS_Association association, int compWidth, int compHeight);

	@Override
	public void init(DMS dms, HashMap<I_DMS_Content, I_DMS_Association> contentsMap, Grid gridLayout, int compWidth, int compHeight,
		EventListener<? extends Event> listener, String[] eventsList)
	{
		this.dms = dms;
		this.grid = gridLayout;
		this.listener = listener;
		this.eventsList = eventsList;

		// Clearing Grid layout children's
		if (grid.getChildren() != null && grid.getChildren().size() > 0)
			Components.removeAllChildren(grid);

		Rows rows = grid.newRows();
		rows.setSclass("SB-ROWS");

		// Grid Header Part creation
		createHeaderPart();

		//
		if (contentsMap == null || contentsMap.isEmpty())
		{
			setNoComponentExistsMsg(rows);
		}
		else
		{
			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : contentsMap.entrySet())
				createComponent(rows, entry.getKey(), entry.getValue(), compWidth, compHeight);
		}
	} // init

	@Override
	public void onEvent(Event event) throws Exception
	{
		if ((event.getName().equals(Events.ON_CLICK) || event.getName().equals(Events.ON_RIGHT_CLICK))
			&& (event.getTarget() instanceof Cell || event.getTarget() instanceof Row))
		{
			removeSelection(prevComponent);
			setSelection(event.getTarget());
			prevComponent = event.getTarget();
		}
	} // onEvent

	@Override
	public String getContentName(I_DMS_Content content, String version)
	{
		String name = content.getName();

		if (version != null)
			name = name + " - V" + version;

		return name;
	} // getContentName

	/**
	 * @param  association
	 * @return             {@link Component} Icon component
	 */
	public Component getLinkIconComponent(I_DMS_Association association)
	{
		if (MDMSAssociationType.isLink(association))
		{
			if (ThemeManager.isUseFontIconForImage())
			{
				A icon = new A();
				icon.setDisabled(true);
				icon.setStyle("Color: black; background: #ffffff80; font-size: large;");
				icon.setIconSclass("z-icon-Link");
				icon.setImageContent((org.zkoss.image.Image) null);
				LayoutUtils.addSclass("font-icon-toolbar-button", icon);
				return icon;
			}
			else
			{
				Image linkImg = new Image();
				linkImg = (Image) LinkImage.clone();
				linkImg.setStyle("background: #ffffff80;");

				return linkImg;
			}
		}
		else
		{
			return new Image();
		}
	} // getLinkIconComponent
}
