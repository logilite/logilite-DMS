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

package org.idempiere.dms.component;

import java.util.Date;

import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.compiere.model.MUser;
import org.compiere.util.Env;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSContent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;

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
		columns.appendChild(createColumnAllSelectCheckBox("3%"));
		columns.appendChild(createColumn(DMSConstant.MSG_CONTENT_NAME, "35%", "left"));
		columns.appendChild(createColumn(DMSConstant.MSG_CONTENT_TYPE, "12%", "left"));
		columns.appendChild(createColumn(DMSConstant.MSG_SIZE, "10%", "Left"));
		columns.appendChild(createColumn(DMSConstant.MSG_UPDATED, "15%", "center"));
		columns.appendChild(createColumn(DMSConstant.MSG_FILE_TYPE, "10%", "left"));
		columns.appendChild(createColumn(DMSConstant.MSG_UPDATEDBY, "10%", "center"));
		columns.appendChild(createColumn(DMSConstant.MSG_LINK, "5%", "center"));

		grid.appendChild(columns);
	} // createHeaderPart

	@Override
	public void setNoComponentExistsMsg(Rows rows)
	{
	} // setNoComponentExistsMsg

	@Override
	public void createComponent(Rows rows, I_DMS_Version version, I_DMS_Association association, int compWidth, int compHeight)
	{
		I_DMS_Content content = version.getDMS_Content();

		row = rows.newRow();
		row.setSclass("SB-ROW");
		
		Checkbox checkBox = new Checkbox();
		checkBox.addActionListener(this);
		checkBox.setId(content.getDMS_Content_UU());
		checkBox.setAttribute("DMS_VERSION_REF", version);

		// Content Thumbnail
		Image thumbImg = new Image();
		thumbImg.setContent(DMS_ZK_Util.getThumbImageForVersion(dms, version, "150"));
		thumbImg.setStyle("width: 100%; max-width: 30px; max-height: 30px;");
		thumbImg.setSclass("SB-THUMBIMAGE");

		Label lblName = new Label(getContentName(content, version.getSeqNo()));
		Label lblCType = new Label("");
		if (content.getDMS_ContentType_ID() > 0)
			lblCType.setText(content.getDMS_ContentType().getName());
		Label lblSize = new Label(version.getDMS_FileSize());
		Label lblUpdated = new Label(DMSConstant.SDF.format(new Date(version.getUpdated().getTime())));
		Label lblFileType = new Label(content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory)	? DMSConstant.MSG_FILE_FOLDER
																													: content.getDMS_MimeType().getName());
		Label lblModifiedBy = new Label(MUser.get(Env.getCtx(), version.getUpdatedBy()).getName());
		Component linkIcon = getLinkIconComponent(association);

		Hbox hbox = new Hbox();
		hbox.appendChild(thumbImg);
		hbox.appendChild(lblName);

		row.appendCellChild(checkBox);
		row.appendCellChild(hbox);
		row.appendCellChild(lblCType);
		row.appendCellChild(lblSize);
		row.appendCellChild(lblUpdated);
		row.appendCellChild(lblFileType);
		row.appendCellChild(lblModifiedBy);
		row.appendCellChild(linkIcon);
		row.setClass(isContentActive ? "SB-Active-Content" : "SB-InActive-Content");

		//
		setAttributesInRow(row, version, association);

		// Listener for component selection
		row.addEventListener(Events.ON_CLICK, this);
		row.addEventListener(Events.ON_RIGHT_CLICK, this);

		for (int i = 0; i < eventsList.length; i++)
			row.addEventListener(eventsList[i], listener);

		// set tooltip text
		row.setTooltiptext(getToolTipTextMsg(version, association));

	} // createComponent

	@Override
	public void removeSelection(Component component)
	{
		if (prevComponent != null)
			((Row) component).setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LIST_NORMAL);
	}

	@Override
	public void setSelection(Component component)
	{
		Row selectedRow = (Row) component;
		Object isActive = selectedRow.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISACTIVE);
		if (isActive != null && isActive instanceof Boolean)
			selectedRow.setStyle((Boolean) isActive ? DMSConstant.CSS_CONTENT_COMP_VIEWER_LIST_SELECTED : DMSConstant.CSS_CONTENT_VIEWER_LIST_SEL_INACTIVE);
		else
			selectedRow.setStyle(DMSConstant.CSS_CONTENT_COMP_VIEWER_LIST_SELECTED);
	}

	private Column createColumn(String labelName, String size, String align)
	{
		Column column = new Column();
		column.setLabel(labelName);
		column.setWidth(size);
		column.setAlign(align);
		return column;
	} // createColumn

	private Column createColumnAllSelectCheckBox(String size)
	{
		Column column = new Column();
		Checkbox allCheckBox = new Checkbox();
		allCheckBox.setWidth(size);
		allCheckBox.setChecked(false);
		allCheckBox.setId(DMSConstant.All_SELECT);
		allCheckBox.addActionListener(this);
		column.appendChild(allCheckBox);
		return column;
	}
}
