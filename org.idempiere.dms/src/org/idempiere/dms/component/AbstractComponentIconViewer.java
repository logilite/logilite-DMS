package org.idempiere.dms.component;

import java.util.HashMap;
import java.util.Map;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.theme.ThemeManager;
import org.idempiere.dms.DMS;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IDMSViewer;
import org.idempiere.dms.factories.IPermissionManager;
import org.idempiere.dms.util.DMSPermissionUtils;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
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
	protected IPermissionManager				permissionManager;

	protected Grid								grid;
	protected Component							prevComponent;

	protected String[]							eventsList;
	protected EventListener<? extends Event>	listener;

	protected boolean							isContentActive	= true;

	// Abstract method definition
	public abstract void createHeaderPart();

	public abstract void setNoComponentExistsMsg(Rows rows);

	public abstract void createComponent(Rows rows, I_DMS_Version version, I_DMS_Association association, int compWidth, int compHeight);

	@Override
	public void init(	DMS dms, HashMap<I_DMS_Version, I_DMS_Association> contentsMap, Grid gridLayout, int compWidth, int compHeight,
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
			permissionManager = dms.getPermissionManager();

			for (Map.Entry<I_DMS_Version, I_DMS_Association> entry : contentsMap.entrySet())
			{
				I_DMS_Version version = entry.getKey();
				I_DMS_Association association = entry.getValue();
				isContentActive = (version != null && association != null && version.getDMS_Content().isActive() && association.isActive());
				if (association != null && MDMSAssociationType.isLink(association))
					isContentActive = association.isActive();

				//
				createComponent(rows, version, association, compWidth, compHeight);
			}
		}
	} // init

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
		}
	} // onEvent

	@Override
	public String getContentName(I_DMS_Content content, int version)
	{
		String name = content.getName();

		if (MDMSContent.CONTENTBASETYPE_Content.equals(content.getContentBaseType()) && version > 0)
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

	public String getToolTipTextMsg(I_DMS_Version version, I_DMS_Association association)
	{
		StringBuffer sb = new StringBuffer(((MDMSContent) version.getDMS_Content()).getToolTipTextMsg());

		if (MDMSContent.CONTENTBASETYPE_Content.equals(version.getDMS_Content().getContentBaseType()) && version.getDMS_FileSize() != null)
			sb.append("\nFileSize:" + version.getDMS_FileSize());

		if (association.getDMS_AssociationType_ID() > 0)
			sb.append("\nAssociation as " + association.getDMS_AssociationType().getName());

		sb.append("\nVersion ID:" + version.getDMS_Version_ID());
		sb.append("\nContent ID:" + version.getDMS_Content_ID());
		return sb.toString();
	}
}
