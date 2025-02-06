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

import java.util.Date;

import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;

import com.logilite.dms.DMS_ZK_Util;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.ContentDetail;
import com.logilite.dms.model.I_DMS_Version;

/**
 * Default DMS List Viewer Component
 * 
 * @author Sachin
 */
public class DefaultComponentIconViewerList extends AbstractComponentIconViewer
{
	private Row row;

	@Override
	public void createHeaderPart()
	{
		Columns columns = new Columns();
		columns.setSizable(true);
		columns.appendChild(createColumnCheckBoxAllSelect("3%"));
		columns.appendChild(createColumn(DMSConstant.MSG_CONTENT_NAME, DMSConstant.ATTRIB_NAME, "30%", "left"));
		columns.appendChild(createColumn(DMSConstant.MSG_CONTENT_TYPE, DMSConstant.ATTRIB_CONTENT_TYPE, "10%", "left"));
		columns.appendChild(createColumn(DMSConstant.MSG_SIZE, DMSConstant.ATTRIB_SIZE, "7%", "Left"));
		columns.appendChild(createColumn(DMSConstant.MSG_CREATED, DMSConstant.ATTRIB_CREATED, "13%", "center"));
		columns.appendChild(createColumn(DMSConstant.MSG_UPDATED, DMSConstant.ATTRIB_UPDATED, "13%", "center"));
		columns.appendChild(createColumn(DMSConstant.MSG_FILE_TYPE, DMSConstant.ATTRIB_FILETYPE, "10%", "left"));
		columns.appendChild(createColumn(DMSConstant.MSG_UPDATEDBY, DMSConstant.ATTRIB_MODIFIEDBY, "10%", "center"));
		columns.appendChild(createColumn(DMSConstant.MSG_LINK, DMSConstant.ATTRIB_LINK, "5%", "center"));

		grid.appendChild(columns);
	} // createHeaderPart

	@Override
	public void setNoComponentExistsMsg(Rows rows)
	{
	} // setNoComponentExistsMsg

	@Override
	public void createComponent(Rows rows, ContentDetail contentDetail, int compWidth, int compHeight, boolean isFirstPage)
	{
		I_DMS_Version version = contentDetail.getVersion();

		row = rows.newRow();
		row.setSclass("SB-ROW");

		Checkbox checkBox = new Checkbox();
		checkBox.addActionListener(this);
		checkBox.setAttribute(DMSConstant.COMP_ATTRIBUTE_DMS_VERSION_REF, version);

		// Content Thumbnail
		Image thumbImg = new Image();
		thumbImg.setContent(DMS_ZK_Util.getThumbImageForVersion(dms, version, "150"));
		thumbImg.setStyle("width: 100%; max-width: 30px; max-height: 30px;");
		thumbImg.setSclass("SB-THUMBIMAGE");

		Label lblName = new Label(contentDetail.getName());
		Label lblCType = new Label(contentDetail.getContentTypeName());
		Label lblSize = new Label(contentDetail.getSize());
		Label lblCreated = new Label(DMSConstant.SDF.format(new Date(contentDetail.getCreated().getTime())));
		Label lblUpdated = new Label(DMSConstant.SDF.format(new Date(contentDetail.getUpdated().getTime())));
		Label lblFileType = new Label(contentDetail.getFileType());
		Label lblModifiedBy = new Label(contentDetail.getModifiedByName());
		Component linkIcon = getLinkIconComponent(contentDetail);

		Hbox hbox = new Hbox();
		hbox.appendChild(thumbImg);
		hbox.appendChild(lblName);

		row.appendCellChild(checkBox);
		row.appendCellChild(hbox);
		row.appendCellChild(lblCType);
		row.appendCellChild(lblSize);
		row.appendCellChild(lblCreated);
		row.appendCellChild(lblUpdated);
		row.appendCellChild(lblFileType);
		row.appendCellChild(lblModifiedBy);
		row.appendCellChild(linkIcon);
		row.setClass(isContentActive ? "SB-Active-Content" : "SB-InActive-Content");

		//
		setAttributesInRow(row, contentDetail);

		// Listener for component selection
		row.addEventListener(Events.ON_CLICK, this);
		row.addEventListener(Events.ON_RIGHT_CLICK, this);

		for (int i = 0; i < eventsList.length; i++)
			row.addEventListener(eventsList[i], listener);

		// set tooltip text
		row.setTooltiptext(contentDetail.getTooltipText());
	} // createComponent

	@Override
	public void removeSelection(Component component)
	{
		if (prevComponent != null)
			((Row) component).setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LIST_NORMAL);
	} // removeSelection

	@Override
	public void setSelection(Component component)
	{
		Row selectedRow = (Row) component;
		Object isActive = selectedRow.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISACTIVE);
		if (isActive != null && isActive instanceof Boolean)
			selectedRow.setStyle((Boolean) isActive ? DMSConstant.CSS_CONTENT_COMP_VIEWER_LIST_SELECTED : DMSConstant.CSS_CONTENT_VIEWER_LIST_SEL_INACTIVE);
		else
			selectedRow.setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LIST_SELECTED);
	} // setSelection

	private Column createColumn(String labelName, String attributeName, String size, String align)
	{
		Column column = new Column();
		column.setAttribute(DMSConstant.ATTRIB_NAME, attributeName);
		if (isSorted && attributeName.equalsIgnoreCase(sortedColumn))
		{
			if (isSortedAsc)
				labelName = labelName + "  ↓"; // ALT + 25
			else
				labelName = labelName + "  ↑"; // ALT + 24
		}
		column.setLabel(labelName);
		column.setWidth(size);
		column.setAlign(align);
		column.addEventListener(Events.ON_CLICK, this);
		return column;
	} // createColumn

	private Column createColumnCheckBoxAllSelect(String size)
	{
		Checkbox checkBoxAll = new Checkbox();
		checkBoxAll.setWidth(size);
		checkBoxAll.setChecked(false);
		checkBoxAll.setId(DMSConstant.All_SELECT);
		checkBoxAll.addActionListener(this);

		//
		Column column = new Column();
		column.appendChild(checkBoxAll);
		return column;
	} // createColumnCheckBoxAllSelect
}
