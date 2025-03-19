package com.logilite.dms.component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.theme.ThemeManager;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.util.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Image;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.event.ZulEvents;

import com.logilite.dms.DMS;
import com.logilite.dms.DMS_ZK_Util;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IDMSViewer;
import com.logilite.dms.factories.IPermissionManager;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.model.MDMSAssociationType;
import com.logilite.dms.model.MDMSContent;
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

	protected Map<I_DMS_Version, I_DMS_Association>	contentItems	= new LinkedHashMap<I_DMS_Version, I_DMS_Association>();

	protected int								compWidth;
	protected int								compHeight;

	protected boolean							isSorted;
	protected boolean							isSortedAsc;
	protected boolean							isContentSelectable;

	protected String							sortedColumn;

	/***
	 * Paging
	 */
	protected Paging							paging;
	protected int								contentSize;
	protected int								contentPerPage	= MSysConfig.getIntValue(DMSConstant.DMS_EXPLORER_CONTENT_PAGE_SIZE, 50);

	// Abstract method definition
	public abstract void createHeaderPart();

	public abstract void setNoComponentExistsMsg(Rows rows);

	public abstract void createComponent(Rows rows, I_DMS_Version version, I_DMS_Association association, int compWidth, int compHeight, boolean isFirstPage);

	@Override
	public void init(	DMS dms, HashMap<I_DMS_Version, I_DMS_Association> contentsMap, Grid gridLayout, int compWidth, int compHeight,
						EventListener<? extends Event> listener, String[] eventsList, String sortFieldName)
	{
		this.dms = dms;
		this.grid = gridLayout;
		this.listener = listener;
		this.eventsList = eventsList;
		this.compWidth = compWidth;
		this.compHeight = compHeight;
		this.contentItems = contentsMap;

		permissionManager = dms.getPermissionManager();

		// Remove paging if binded to grid
		for (Component cmp : grid.getParent().getChildren())
		{
			if (cmp instanceof Paging)
				grid.getParent().removeChild(cmp);
		}

		contentSize = contentItems.size();
		// If content size is less than the default size then don't show paging
		if (contentSize > contentPerPage)
		{
			boolean isPagingMoveToFirstPage = false;
			if (grid.hasAttribute(DMSConstant.ATTRIB_PAGING_MOVE_TO_FIRST_PAGE))
			{
				isPagingMoveToFirstPage = (boolean) grid.getAttribute(DMSConstant.ATTRIB_PAGING_MOVE_TO_FIRST_PAGE);
				grid.removeAttribute(DMSConstant.ATTRIB_PAGING_MOVE_TO_FIRST_PAGE);
			}

			paging = new Paging();
			paging.setVisible(true);
			paging.setDetailed(true);
			paging.setSclass("dms_paging");
			paging.setPageSize(contentPerPage);
			paging.addEventListener(ZulEvents.ON_PAGING, this);
			if (isPagingMoveToFirstPage)
				paging.setActivePage(0);

			grid.getParent().appendChild(paging);
		}

		//
		if (Util.isEmpty(sortFieldName))
		{
			sortFieldName = DMSConstant.ATTRIB_NAME;
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

		/***
		 * Paging Handler + Content Renderer
		 */
		if (contentItems == null || contentItems.isEmpty())
		{
			setNoComponentExistsMsg(rows);
			if (paging != null)
				paging.setVisible(false);
		}
		else
		{
			int currentPage = 1; // Assuming we start from the first page
			int totalPages = (int) Math.ceil((double) contentSize / contentPerPage);

			// If paging is not null then set the size and current page
			if (paging != null)
			{
				paging.setTotalSize(contentSize);
				currentPage = paging.getActivePage() + 1;
			}

			//
			if (currentPage < 1)
			{
				currentPage = 1;
			}
			else if (currentPage > totalPages)
			{
				// Set to last page if it exceeds totalPages
				currentPage = totalPages;
			}

			// Assign the start and end index for the current page
			int startIndex = (currentPage - 1) * contentPerPage;
			int endIndex = Math.min(startIndex + contentPerPage, contentSize);

			// Iteration of component renderer
			int i = 0;
			for (Map.Entry<I_DMS_Version, I_DMS_Association> entry : contentItems.entrySet())
			{
				if (i < endIndex)
				{
					I_DMS_Version version = entry.getKey();
					I_DMS_Association association = entry.getValue();
					if (association != null)
					{
						if (version != null)
							isContentActive = (version.getDMS_Content().isActive() && association.isActive());
						if (MDMSAssociationType.isLink(association.getDMS_Association_ID()))
							isContentActive = association.isActive();
					}

					createComponent(rows, version, association, compWidth, compHeight, startIndex == i);
				}
				i++;
			}

		}
	} // renderZK

	@Override
	public void setAttributesInRow(Component component, I_DMS_Version version, I_DMS_Association association)
	{
		component.setAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT, version.getDMS_Content());
		component.setAttribute(DMSConstant.COMP_ATTRIBUTE_VERSION, version);
		component.setAttribute(DMSConstant.COMP_ATTRIBUTE_ASSOCIATION, association);
		component.setAttribute(DMSConstant.COMP_ATTRIBUTE_ISACTIVE, Boolean.valueOf(isContentActive));

		if (DMSPermissionUtils.isPermissionAllowed())
		{
			permissionManager.initContentPermission(version.getDMS_Content());

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
			if (isContentSelectable)
			{
				if (prevComponent.getFirstChild() instanceof Vbox
					&& prevComponent.getFirstChild().getFirstChild() instanceof Checkbox)
				{
					Checkbox chk = (Checkbox) event.getTarget().getFirstChild().getFirstChild();
					chk.setChecked(!chk.isChecked());
					//
					Events.sendEvent(new Event(DMSConstant.EVENT_ON_SELECTION_CHANGE, (Component) listener, chk));
				}
			}
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
		else if (event.getName().equals("onPaging"))
		{
			renderZK();
		}
	} // onEvent

	/**
	 * @param  isLink
	 * @return        {@link Component} Icon component
	 */
	public Component getLinkIconComponent(boolean isLink)
	{
		if (isLink)
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

		//
		doSorting(sortedColumn, isSortedAsc);

		//
		renderZK();
	} // doSortGridColumn

	/**
	 * Do Sorting operation on the Content list
	 * 
	 * @param sortingColumn - Sorting Column or Attribute name
	 * @param isSortingAsc  - Order of sorting ASC/DESC
	 */
	public void doSorting(String sortingColumn, boolean isSortingAsc)
	{
		// Define comparator based on sorting column
		Comparator<Map.Entry<I_DMS_Version, I_DMS_Association>> comparator;
		switch (sortingColumn)
		{
			case DMSConstant.ATTRIB_NAME:
				comparator = Comparator.comparing(	entry -> {
														String name = entry.getKey().getDMS_Content().getName();
														return name != null ? name : "";
													},
													Comparator.naturalOrder());
				break;
			case DMSConstant.ATTRIB_CONTENT_TYPE:
				comparator = Comparator.comparing(	entry -> {
														String contentTypeName = entry.getKey().getDMS_Content().getDMS_ContentType().getName();
														return contentTypeName != null ? contentTypeName : "";
													},
													Comparator.naturalOrder());
				break;
			case DMSConstant.ATTRIB_SIZE:
				comparator = Comparator.comparing(	entry -> Optional.ofNullable(entry.getKey().getDMS_FileSize()).orElse(""),
													Comparator.nullsLast(Comparator.naturalOrder()));
				break;
			case DMSConstant.ATTRIB_CREATED:
				comparator = Comparator.comparing(entry -> (entry.getKey()).getDMS_Content().getCreated());
				break;
			case DMSConstant.ATTRIB_UPDATED:
				comparator = Comparator.comparing(entry -> (entry.getKey()).getUpdated());
				break;
			case DMSConstant.ATTRIB_FILETYPE:
				comparator = Comparator.comparing(entry -> (entry.getKey()).getDMS_Content().getDMS_MimeType().getName());
				break;
			case DMSConstant.ATTRIB_MODIFIEDBY:
				comparator = Comparator.comparing(entry -> MUser.getNameOfUser((entry.getKey()).getUpdatedBy()));
				break;
			case DMSConstant.ATTRIB_LINK:
				comparator = Comparator.comparing(entry -> (MDMSAssociationType.isLink(entry.getValue())));
				break;
			default:
				return; // Exit if sorting column is invalid
		}

		// Reverse comparator for descending order
		if (!isSortingAsc)
		{
			comparator = comparator.reversed();
		}

		contentItems = contentItems	.entrySet()
									.parallelStream()
									.sorted(comparator)
									.collect(Collectors.toMap(
																Map.Entry::getKey,
																Map.Entry::getValue,
																(e1, e2) -> e1,
																LinkedHashMap::new));
	} // doSorting

	public void isContentSelectable(boolean isContentSelectable)
	{
		this.isContentSelectable = isContentSelectable;
	}

	/**
	 * Tooltip text building
	 * 
	 * @param  version
	 * @param  association
	 * @return
	 */
	public String getTooltipText(I_DMS_Version version, I_DMS_Association association)
	{
		StringBuffer renderTooltipText = new StringBuffer(((MDMSContent) version.getDMS_Content()).getToolTipTextMsg());

		if (MDMSContent.CONTENTBASETYPE_Content.equals(version.getDMS_Content().getContentBaseType()) && version.getDMS_FileSize() != null)
			renderTooltipText.append("\nFile Size: " + version.getDMS_FileSize());

		if (association.getDMS_AssociationType_ID() > 0)
			renderTooltipText.append("\nAssociation as " + association.getDMS_AssociationType().getName());

		renderTooltipText.append("\nVersion ID: " + version.getDMS_Version_ID());
		renderTooltipText.append("\nContent ID: " + version.getDMS_Content_ID());

		return renderTooltipText.toString();
	} // getTooltipText

	/**
	 * Get content name with versioning if applicable
	 * 
	 * @param  version
	 * @return
	 */
	public String getRenderContentName(I_DMS_Version version)
	{
		String name = version.getDMS_Content().getName();

		if (MDMSContent.CONTENTBASETYPE_Content.equals(version.getDMS_Content().getContentBaseType()) && version.getSeqNo() > 0)
			name = name + " - V" + version.getSeqNo();

		return name;
	} // getRenderContentName
}
