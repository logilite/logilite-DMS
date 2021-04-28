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

package org.idempiere.componenet;

import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSAssociationType;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Vbox;

/**
 * Default DMS Large Icon Viewer Component
 * 
 * @author Sachin
 */
public class DefaultComponentIconViewerLarge extends AbstractComponentIconViewer
{
	private Row row;

	@Override
	public void createHeaderPart()
	{
	} // createHeaderPart

	@Override
	public void setNoComponentExistsMsg(Rows rows)
	{
	} // setNoComponentExistsMsg

	@Override
	public void createComponent(Rows rows, I_DMS_Version version, I_DMS_Association association, int compWidth, int compHeight)
	{
		if (row == null)
		{
			row = rows.newRow();
			row.setSclass("SB-ROW");
			row.setStyle(DMSConstant.CSS_FLEX_ROW_DIRECTION + " width: 100%; overflow: hidden; padding: 2px;");
		}

		// Content Label
		Label lblName = new Label(getContentName(version.getDMS_Content(), version.getSeqNo()));
		lblName.setStyle(	"text-overflow: ellipsis; white-space: nowrap; overflow: hidden; text-align: center; height: 20px; width: "
							+ (compWidth - 10) + "px; display: inline-block;" + (isContentActive ? " " : "color: red;"));

		// Content Thumbnail
		Image thumbImg = new Image();
		thumbImg.setContent(DMS_ZK_Util.getThumbImageForVersion(dms, version, "150"));
		thumbImg.setStyle("width: 100%; max-width: " + compWidth + "px; max-height: " + (compHeight - 35) + "px;");
		thumbImg.setSclass("SB-THUMBIMAGE");

		Vbox vbox = new Vbox();
		vbox.appendChild(thumbImg);
		vbox.appendChild(lblName);
		vbox.setAlign("center");
		vbox.setStyle("position: relative; width: 100%; height: 100%; max-width:" + compWidth + "px;");

		Cell cell = new Cell();
		cell.appendChild(vbox);
		cell.setWidth(compWidth + "px");
		cell.setHeight(compHeight + "px");
		cell.setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LARGE_NORMAL);
		row.appendChild(cell);

		//
		setAttributesInRow(cell, version, association);

		// Listener for component selection
		cell.addEventListener(Events.ON_CLICK, this);
		cell.addEventListener(Events.ON_RIGHT_CLICK, this);

		for (int i = 0; i < eventsList.length; i++)
			cell.addEventListener(eventsList[i], listener);

		if (MDMSAssociationType.isLink(association))
		{
			Component icon = getLinkIconComponent(association);

			Div mimeIcon = new Div();
			mimeIcon.appendChild(icon);
			mimeIcon.setStyle("position: absolute; bottom: 24%;");
			vbox.appendChild(mimeIcon);
		}

		// set tooltip text
		cell.setTooltiptext(getToolTipTextMsg(version, association));
	} // createComponent

	@Override
	public void removeSelection(Component component)
	{
		if (prevComponent != null)
			((Cell) prevComponent).setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LARGE_NORMAL);
	}

	@Override
	public void setSelection(Component component)
	{
		Cell cell = (Cell) component;
		Object isActive = cell.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISACTIVE);
		if (isActive != null && isActive instanceof Boolean)
		{
			cell.setStyle((Boolean) isActive ? DMSConstant.CSS_CONTENT_COMP_VIEWER_LARGE_SELECTED : DMSConstant.CSS_CONTENT_VIEWER_LARGE_SEL_INACTIVE);
		}
		else
		{
			cell.setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LARGE_SELECTED);
		}
	}
}
