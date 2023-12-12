package com.logilite.dms.component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.theme.ThemeManager;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Image;

import com.logilite.dms.DMS;
import com.logilite.dms.DMS_ZK_Util;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IDMSViewer;
import com.logilite.dms.factories.IPermissionManager;
import com.logilite.dms.model.ContentDetail;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.util.DMSPermissionUtils;

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
	protected IPermissionManager				permissionManager;

	protected Grid								grid;
	protected Component							prevComponent;

	protected String[]							eventsList;
	protected EventListener<? extends Event>	listener;

	protected boolean							isContentActive	= true;

	protected List<ContentDetail>				contentItems	= new ArrayList<ContentDetail>();

	protected int								compWidth;
	protected int								compHeight;

	protected boolean							isSorted;
	protected boolean							isSortedAsc;
	protected String							sortedColumn;

	// Abstract method definition
	public abstract void createHeaderPart();

	public abstract void setNoComponentExistsMsg(Rows rows);

	public abstract void createComponent(Rows rows, ContentDetail contentDetail, int compWidth, int compHeight);

	@Override
	public void init(	DMS dms, HashMap<I_DMS_Version, I_DMS_Association> contentsMap, Grid gridLayout, int compWidth, int compHeight,
						EventListener<? extends Event> listener, String[] eventsList)
	{
		this.dms = dms;
		this.grid = gridLayout;
		this.listener = listener;
		this.eventsList = eventsList;
		this.compWidth = compWidth;
		this.compHeight = compHeight;

		permissionManager = dms.getPermissionManager();

		for (Map.Entry<I_DMS_Version, I_DMS_Association> entry : contentsMap.entrySet())
		{
			contentItems.add(new ContentDetail(entry.getKey(), entry.getValue()));
		}

		//
		renderZK();
	} // init

	private void renderZK()
	{
		Rows rows;

		// Clearing Grid layout children's
		if (grid.getChildren() != null && grid.getChildren().size() > 0)
		{
			for (Component comp : grid.getChildren())
			{
				ArrayList<Component> list = new ArrayList<>();
				list.addAll(comp.getChildren());

				for (Component c : list)
				{
					comp.removeChild(c);
				}

				if (!(comp instanceof Rows))
				{
					grid.removeChild(comp);
					grid.invalidate();
				}
			}

			// Get existing rows object for Drag & Drop event already registered and no need to do
			// register each navigation event.
			rows = (Rows) grid.getFirstChild();
		}
		else
		{
			rows = grid.newRows();
			rows.setSclass("SB-ROWS");
		}

		// Grid Header Part creation
		createHeaderPart();

		//
		if (contentItems == null || contentItems.isEmpty())
		{
			setNoComponentExistsMsg(rows);
		}
		else
		{
			for (ContentDetail contentDetail : contentItems)
			{
				I_DMS_Version version = contentDetail.getVersion();
				I_DMS_Association association = contentDetail.getAssociation();
				isContentActive = (version != null && association != null && contentDetail.getContent().isActive() && association.isActive());
				if (association != null && contentDetail.isLink())
					isContentActive = association.isActive();

				//
				createComponent(rows, contentDetail, compWidth, compHeight);
			}
		}
	} // renderZK

	@Override
	public void setAttributesInRow(Component component, ContentDetail contentDetail)
	{
		component.setAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT, contentDetail.getContent());
		component.setAttribute(DMSConstant.COMP_ATTRIBUTE_VERSION, contentDetail.getVersion());
		component.setAttribute(DMSConstant.COMP_ATTRIBUTE_ASSOCIATION, contentDetail.getAssociation());
		component.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISACTIVE, Boolean.valueOf(isContentActive));

		if (DMSPermissionUtils.isPermissionAllowed())
		{
			permissionManager.initContentPermission(contentDetail.getContent());

			component.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISREAD, permissionManager.isRead());
			component.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISWRITE, permissionManager.isWrite());
			component.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISDELETE, permissionManager.isDelete());
			component.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISNAVIGATION, permissionManager.isNavigation());
		}
	} // setAttributesInRow

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
		else if (event.getName().equals(Events.ON_CHECK) && event.getTarget() instanceof Checkbox)
		{
			// Checkbox select all - event from icon viewer list
			Events.sendEvent(new Event(DMSConstant.EVENT_ON_SELECTION_CHANGE, (Component) listener, event.getTarget()));
		}
		else if (event.getName().equals(Events.ON_CLICK) && event.getTarget() instanceof Column)
		{
			doSortGridColumn((Column) event.getTarget());

			// Remove downloaded set in sorting event
			Events.sendEvent(new Event(DMSConstant.EVENT_ON_SELECTION_CHANGE, (Component) listener, Boolean.FALSE));
		}
	} // onEvent

	/**
	 * @param  association
	 * @return             {@link Component} Icon component
	 */
	public Component getLinkIconComponent(ContentDetail contentDetail)
	{
		if (contentDetail.isLink())
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

	public void doSortGridColumn(Column column)
	{
		boolean sortType = true;
		String sortingColumn = String.valueOf(column.getAttribute(DMSConstant.ATTRIB_NAME));
		if (isSorted)
		{
			if (sortedColumn.equals(sortingColumn))
				sortType = !isSortedAsc;
		}

		sortedColumn = sortingColumn;
		isSortedAsc = sortType;
		isSorted = true;

		// Implement comparator for sorting based on column and ascending/descending
		Comparator<ContentDetail> comparator = null;
		switch (sortedColumn)
		{
			case DMSConstant.ATTRIB_NAME:
				comparator = Comparator.comparing(ContentDetail::getName);
				break;
			case DMSConstant.ATTRIB_CONTENT_TYPE:
				comparator = Comparator.comparing(ContentDetail::getContentTypeName);
				break;
			case DMSConstant.ATTRIB_SIZE:
				comparator = Comparator.comparing(ContentDetail::getSize);
				break;
			case DMSConstant.ATTRIB_UPDATED:
				comparator = Comparator.comparing(ContentDetail::getUpdated);
				break;
			case DMSConstant.ATTRIB_FIELDTYPE:
				comparator = Comparator.comparing(ContentDetail::getFileType);
				break;
			case DMSConstant.ATTRIB_MODIFIEDBY:
				comparator = Comparator.comparing(ContentDetail::getModifiedByName);
				break;
			case DMSConstant.ATTRIB_LINK:
				comparator = Comparator.comparing(ContentDetail::isLink);
				break;
			default:
				break;
		}

		if (comparator != null)
		{
			if (isSortedAsc)
				contentItems.sort(comparator);
			else
				contentItems.sort(comparator.reversed());
		}

		//
		renderZK();
	} // doSortGridColumn

}
