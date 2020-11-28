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
import org.idempiere.model.MDMSContent;
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
	public void createHeaderPart()
	{
		Columns columns = new Columns();
		columns.appendChild(new Column());
		columns.appendChild(new Column());

		grid.appendChild(columns);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setZclass("none");
	} // createHeaderPart

	@Override
	public void setNoComponentExistsMsg(Rows rows)
	{
		Cell cell = new Cell();
		cell.appendChild(new Label(DMSConstant.MSG_NO_VERSION_DOC_EXISTS));
		cell.setColspan(2);

		Row row = rows.newRow();
		row.appendChild(cell);
	} // setNoComponentExistsMsg

	@Override
	public void createComponent(Rows rows, I_DMS_Content content, I_DMS_Association association, int compWidth, int compHeight)
	{
		Row row = rows.newRow();
		row.setSclass("SB-ROW");
		row.setWidgetAttribute("cellspacing", "15");

		// Content Thumbnail
		Image thumbImg = new Image();
		thumbImg.setContent(DMS_ZK_Util.getThumbImageForContent(dms, content, "150"));
		thumbImg.setStyle("width: 100%; max-width: " + compWidth + "px; max-height: " + compHeight + "px;");
		thumbImg.setSclass("SB-THUMBIMAGE");

		Vbox vbox = new Vbox();
		vbox.appendChild(new Label(DMSConstant.MSG_CREATED + ": " + content.getCreated()));
		vbox.appendChild(new Label(DMSConstant.MSG_CREATEDBY + ": " + MUser.getNameOfUser(content.getCreatedBy())));
		vbox.appendChild(new Label(DMSConstant.MSG_FILESIZE + ": " + content.getDMS_FileSize()));

		Hbox hbox = new Hbox();
		hbox.appendChild(thumbImg);
		hbox.appendChild(vbox);
		hbox.setAlign("center");
		hbox.setStyle("width: 90%; height: 100%; max-height:" + compHeight + "px;");

		Cell cell = new Cell();
		cell.setWidth("100%");
		cell.appendChild(hbox);
		cell.setHeight(compHeight + "px");
		// cell.setStyle("background: #f3f3f3;");
		row.appendChild(cell);

		//
		row.setAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT, content);
		row.setAttribute(DMSConstant.COMP_ATTRIBUTE_ASSOCIATION, association);

		// Listener for component selection
		row.addEventListener(Events.ON_CLICK, this);

		for (int i = 0; i < eventsList.length; i++)
			row.addEventListener(eventsList[i], listener);

		// set tooltip text
		row.setTooltiptext(((MDMSContent) content).getToolTipTextMsg());
	} // createComponent

	@Override
	public void removeSelection(Component component)
	{
		if (prevComponent != null)
			((Row) prevComponent).setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LARGE_NORMAL);
	}

	@Override
	public void setSelection(Component component)
	{
		((Row) component).setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LARGE_SELECTED);
	}
}
