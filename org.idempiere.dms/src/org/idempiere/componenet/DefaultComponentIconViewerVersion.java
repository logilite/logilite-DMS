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

import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.compiere.model.MUser;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Vbox;

/**
 * Default DMS Version Viewer Component
 * 
 * @author Sachin
 */
public class DefaultComponentIconViewerVersion extends AbstractComponentIconViewer
{
	// private Row row;

	@Override
	public void createComponent(Rows rows, I_DMS_Content content, I_DMS_Association association, int compWidth, int compHeight)
	{
		// if (row == null)
		// {
		// grid.setAttribute("cellspacing", "10");
		// grid.setWidgetAttribute("cellspacing", "10");
		// rows.getParent().setWidgetAttribute("cellspacing", "10");
		Row row = rows.newRow();
		row.setSclass("SB-ROW");
		// row.setStyle("display: flex; flex-direction: row; flex-wrap: wrap; width: 100%; height: 100%; overflow: hidden; padding: 2px; cellspacing=10;");
		// }

		// Content Label
		// Label lblName = new Label(getContentName(content, ((MDMSContent)
		// content).getSeqNo()));
		// lblName.setSclass("SB-LABEL");
		// lblName.setStyle("text-overflow: ellipsis; white-space: nowrap; overflow: hidden; text-align: center; height: 15px; width: "
		// + compWidth + "px;");

		Vbox vbox = new Vbox();
		vbox.appendChild(new Label(DMSConstant.MSG_CREATED + ": " + content.getCreated()));
		vbox.appendChild(new Label(DMSConstant.MSG_CREATEDBY + ": " + MUser.getNameOfUser(content.getCreatedBy())));
		vbox.appendChild(new Label(DMSConstant.MSG_FILESIZE + ": " + content.getDMS_FileSize()));

		// Content Thumbnail
		Image thumbImg = new Image();
		thumbImg.setContent(DMS_ZK_Util.getThumbImageForContent(dms, content, "150"));
		thumbImg.setStyle("width: 100%; max-width: " + compWidth + "px; max-height: " + (compHeight - 30) + "px;");
		thumbImg.setSclass("SB-THUMBIMAGE");

		Hbox hbox = new Hbox();
		hbox.setSclass("SB-VBOX");
		hbox.appendChild(thumbImg);
		hbox.appendChild(vbox);
		hbox.setAlign("center");
		hbox.setStyle("width: 100%; height: 100%; max-width:" + compWidth + "px;");

		Cell cell = new Cell();
		row.appendChild(cell);
		cell.appendChild(hbox);
		cell.setSclass("SB-CELL");
		cell.setWidth(compWidth + "px");
		cell.setHeight(compHeight + "px");
		cell.setStyle("background: #f3f3f3;");

		//
		cell.setAttribute(DMSConstant.CELL_ATTRIBUTE_CONTENT, content);
		cell.setAttribute(DMSConstant.CELL_ATTRIBUTE_ASSOCIATION, association);

		cell.addEventListener(Events.ON_CLICK, this); // Listener for Selection

		for (int i = 0; i < eventsList.length; i++)
			cell.addEventListener(eventsList[i], listener);

		// viewerComponent.addEventListener(Events.ON_CLICK, this);
		// viewerComponent.addEventListener(Events.ON_RIGHT_CLICK, this);
		// viewerComponent.addEventListener(Events.ON_DOUBLE_CLICK, this);

		// if (isLink(association) || true)
		// {
		// Div mimeIcon = new Div();
		// mimeIcon.appendChild(LinkImage);
		// ZkCssHelper.appendStyle(mimeIcon,
		// "position: absolute; top: 75px; left: 5px; z-index: 999;");
		// hbox.appendChild(mimeIcon);
		// }
	} // createComponent

	@Override
	public void createHeaderPart()
	{
		Columns columns = new Columns();

		Column column = new Column();
		column.setWidth("30%");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("65%");
		columns.appendChild(column);

		grid.appendChild(columns);
		grid.setHeight("100%");
		grid.setWidth("98%");
		grid.setZclass("none");
	} // createHeaderPart

	@Override
	public void removeSelection(Component component)
	{
		if (prevComponent != null)
			((Cell) prevComponent).setStyle(DMSConstant.CSS_CONTENT_NORMAL_COMP_VIEWER_LARGE);
	}

	@Override
	public void setSelection(Component component)
	{
		((Cell) component).setStyle(DMSConstant.CSS_CONTENT_SELECTED_COMP_VIEWER_LARGE);
	}

	@Override
	public void setNoComponentExistsMsg(Rows rows)
	{
		Cell cell = new Cell();
		cell.appendChild(new Label(DMSConstant.MSG_NO_VERSION_DOC_EXISTS));
		cell.setColspan(2);

		Row row = rows.newRow();
		row.appendChild(cell);
	}

}
