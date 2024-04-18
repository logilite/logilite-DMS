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

package com.logilite.dms.component;

import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.compiere.model.MUser;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Vbox;

import com.logilite.dms.DMS_ZK_Util;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.ContentDetail;
import com.logilite.dms.model.I_DMS_Version;

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
	public void createComponent(Rows rows, ContentDetail contentDetail, int compWidth, int compHeight, boolean isFirstPage)
	{
		I_DMS_Version version = contentDetail.getVersion();

		Row row = rows.newRow();
		row.setSclass("SB-ROW");
		row.setWidgetAttribute("cellspacing", "15");

		// Content Thumbnail
		Image thumbImg = new Image();
		thumbImg.setContent(DMS_ZK_Util.getThumbImageForVersion(dms, contentDetail.getVersion(), "150"));
		thumbImg.setStyle("width: 100%; max-width: " + compWidth + "px; max-height: " + compHeight + "px;");
		thumbImg.setSclass("SB-THUMBIMAGE");

		Vbox vbox = new Vbox();
		vbox.appendChild(new Label("Version: " + version.getSeqNo()));
		vbox.appendChild(new Label(DMSConstant.MSG_CREATED + ": " + version.getCreated()));
		vbox.appendChild(new Label(DMSConstant.MSG_CREATEDBY + ": " + MUser.getNameOfUser(version.getCreatedBy())));
		vbox.appendChild(new Label(DMSConstant.MSG_FILESIZE + ": " + contentDetail.getSize()));

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
		row.setClass(isContentActive ? "SB-Active-Content" : "SB-InActive-Content");
		row.setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LARGE_NORMAL);

		//
		setAttributesInRow(row, contentDetail);

		// Listener for component selection
		row.addEventListener(Events.ON_CLICK, this);

		for (int i = 0; i < eventsList.length; i++)
			row.addEventListener(eventsList[i], listener);

		// set tooltip text
		row.setTooltiptext(contentDetail.getTooltipText());
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
		Row row = (Row) component;
		Object isActive = row.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISACTIVE);
		if (isActive != null && isActive instanceof Boolean)
			row.setStyle((Boolean) isActive ? DMSConstant.CSS_CONTENT_COMP_VIEWER_LARGE_SELECTED : DMSConstant.CSS_CONTENT_VIEWER_LARGE_SEL_INACTIVE);
		else
			row.setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LARGE_SELECTED);
	}
}
