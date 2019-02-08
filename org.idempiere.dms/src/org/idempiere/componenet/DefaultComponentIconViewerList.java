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

import java.util.Date;

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
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Image;

/**
 * Default DMS List Viewer Component
 * 
 * @author Sachin
 */
public class DefaultComponentIconViewerList extends AbstractComponentIconViewer
{
	private Row	row;

	@Override
	public void createHeaderPart()
	{
		Columns columns = new Columns();
		columns.setSizable(true);
		columns.appendChild(createColumn("", "5%", "center"));
		columns.appendChild(createColumn("Name", "30%", "left"));
		columns.appendChild(createColumn("Size", "10%", "Left"));
		columns.appendChild(createColumn("Updated", "15%", "center"));
		columns.appendChild(createColumn("File Type", "15%", "left"));
		columns.appendChild(createColumn("Modified By", "20%", "center"));
		columns.appendChild(createColumn("Link", "5%", "center"));

		grid.appendChild(columns);
	} // createHeaderPart

	private Column createColumn(String labelName, String size, String align)
	{
		Column column = new Column();
		column.setLabel(labelName);
		column.setWidth(size);
		column.setAlign(align);
		return column;
	} // createColumn

	@Override
	public void createComponent(Rows rows, I_DMS_Content content, I_DMS_Association association, int compWidth, int compHeight)
	{
		row = rows.newRow();
		row.setSclass("SB-ROW");

		// Content Thumbnail
		Image thumbImg = new Image();
		thumbImg.setContent(DMS_ZK_Util.getThumbImageForContent(dms, content, "150"));
		thumbImg.setStyle("width: 100%; max-width: 30px; max-height: 30px;");
		thumbImg.setSclass("SB-THUMBIMAGE");

		Label lblName = new Label(getContentName(content, ((MDMSContent) content).getSeqNo()));
		Label lblSize = new Label(content.getDMS_FileSize());
		Label lblUpdated = new Label(DMSConstant.SDF.format(new Date(content.getUpdated().getTime())));
		Label lblFileType = new Label(content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory) ? DMSConstant.MSG_FILE_FOLDER : content
				.getDMS_MimeType().getName());
		Label lblModifiedBy = new Label(MUser.get(Env.getCtx(), content.getUpdatedBy()).getName());

		// Icon
		row.appendCellChild(thumbImg);
		// Name
		row.appendCellChild(lblName);
		// Size
		row.appendCellChild(lblSize);
		// Date Updated
		row.appendCellChild(lblUpdated);
		// File Type
		row.appendCellChild(lblFileType);
		// Modified By (user)
		row.appendCellChild(lblModifiedBy);
		// Link
		Image linkImg = new Image();
		if (association.getDMS_AssociationType_ID() == MDMSAssociationType.LINK_ID)
			linkImg = (Image) LinkImage.clone();
		row.appendCellChild(linkImg);

		//
		row.setAttribute(DMSConstant.CELL_ATTRIBUTE_CONTENT, content);
		row.setAttribute(DMSConstant.CELL_ATTRIBUTE_ASSOCIATION, association);

		row.addEventListener(Events.ON_CLICK, this); // Listener for Selection

		for (int i = 0; i < eventsList.length; i++)
			row.addEventListener(eventsList[i], listener);

	} // createComponent

	@Override
	public void removeSelection(Component component)
	{
		if (prevComponent != null)
			((Row) prevComponent).setStyle(DMSConstant.CSS_CONTENT_NORMAL_COMP_VIEWER_LIST);
	}

	@Override
	public void setSelection(Component component)
	{
		((Row) component).setStyle(DMSConstant.CSS_CONTENT_SELECTED_COMP_VIEWER_LIST);
	}

	@Override
	public void setNoComponentExistsMsg(Rows rows)
	{

	}
}
