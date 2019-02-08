package org.idempiere.componenet;

import java.util.HashMap;
import java.util.Map;

import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.idempiere.dms.DMS;
import org.idempiere.dms.factories.IDMSViewer;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Image;

/**
 * Abstract Component Icon Viewer
 * 
 * @author Sachin
 */
public abstract class AbstractComponentIconViewer implements IDMSViewer, EventListener<Event>
{
	protected static Image						LinkImage	= new Image();

	static
	{
		LinkImage.setContent(Utils.getImage("Link16.png"));
	}

	protected DMS								dms;
	protected MDMSContent						content		= null;
	protected MDMSAssociation					association	= null;

	protected Grid								grid;
	protected Component							prevComponent;

	protected String[]							eventsList;
	protected EventListener<? extends Event>	listener;

	//
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
		Components.removeAllChildren(grid);

		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setSclass("SB-GRID");

		Rows rows = grid.newRows();
		rows.setSclass("SB-ROWS");

		//
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
		if (event.getName().equals(Events.ON_CLICK) && (event.getTarget() instanceof Cell || event.getTarget() instanceof Row))
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
		if (content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
			name = name.replace("\\(.*\\d\\)", "");

		if (name.contains("(") && name.contains(")"))
			name = name.replace(name.substring(name.lastIndexOf("("), name.lastIndexOf(")") + 1), "");

		if (version != null)
			name = name + " (V" + version + ")";

		return name;
	} // getContentName

	/**
	 * Is Link
	 * 
	 * @param association
	 * @return TRUE if Link
	 */
	public boolean isLink(I_DMS_Association association)
	{
		return association.getDMS_AssociationType_ID() == MDMSAssociationType.LINK_ID;
	} // isLink

	public MDMSAssociation getAssociation()
	{
		return association;
	}

	public void setAssociation(MDMSAssociation association)
	{
		this.association = association;
	}

	public MDMSContent getContent()
	{
		return content;
	}

	public void setContent(MDMSContent content)
	{
		this.content = content;
	}
}
