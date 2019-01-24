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

package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.component.ZkCssHelper;
import org.compiere.model.MImage;
import org.compiere.util.CLogger;
import org.idempiere.componenet.DMSViewerComponent;
import org.idempiere.dms.DMS;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;

public class WDMSVersion extends Window implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long				serialVersionUID	= -3613076228042516782L;

	private static CLogger					log					= CLogger.getCLogger(WDMSVersion.class);

	private ArrayList<DMSViewerComponent>	viewerComponents	= null;
	private DMSViewerComponent				prevComponent		= null;
	private Grid							gridView			= GridFactory.newGridLayout();
	private DMS								dms;

	/**
	 * Constructor
	 * 
	 * @param dms
	 * @param content
	 */
	public WDMSVersion(DMS dms, MDMSContent content)
	{
		this.dms = dms;

		try
		{
			renderDMSVersion(content);
			init();

			this.addEventListener(Events.ON_CLICK, this);
			this.addEventListener(Events.ON_DOUBLE_CLICK, this);
			this.addEventListener(Events.ON_CLOSE, this);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "DMS Version fetching failure :", e);
			throw new AdempiereException("DMS Version fetching failure :" + e);
		}
	} // Constructor

	private void init()
	{
		this.setHeight("38%");
		this.setWidth("44%");
		this.setTitle(DMSConstant.MSG_DMS_VERSION_LIST);
		this.setClosable(true);
		this.appendChild(gridView);

		gridView.setStyle("overflow: auto; position:relative;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");

		AEnv.showCenterScreen(this);
	} // init

	public void renderDMSVersion(MDMSContent DMS_Content) throws IOException
	{

		Components.removeAllChildren(gridView);
		gridView.setSizedByContent(true);
		gridView.setZclass("none");
		Rows rows = gridView.newRows();
		Row row = rows.newRow();
		row.setZclass("none");
		row.setStyle("display:flex; flex-direction: row; flex-wrap: wrap;");

		List<I_DMS_Content> contentList = MDMSContent.getVersionHistory(DMS_Content);

		if (contentList.size() == 0)
		{
			throw new AdempiereException("No versions are available.");
		}

		viewerComponents = new ArrayList<DMSViewerComponent>();

		for (int i = 0; i < contentList.size(); i++)
		{
			AImage image = null;

			File thumbFile = dms.getThumbnail(contentList.get(i), "150");
			if (thumbFile == null)
			{
				MImage mImage = Utils.getMimetypeThumbnail(contentList.get(i).getDMS_MimeType_ID());
				if (mImage.getData() != null)
					image = new AImage(contentList.get(i).getName(), mImage.getData());
			}
			else
			{
				image = new AImage(thumbFile);
			}

			// Getting version number of version
			String seqNo = ((MDMSContent) contentList.get(i)).getSeqNo();
			DMSViewerComponent viewerComponent = new DMSViewerComponent(dms, contentList.get(i), image, false, null, seqNo);

			viewerComponent.setDwidth(DMSConstant.CONTENT_COMPONENT_WIDTH);
			viewerComponent.setDheight(DMSConstant.CONTENT_COMPONENT_HEIGHT);

			viewerComponent.addEventListener(Events.ON_CLICK, this);
			viewerComponent.addEventListener(Events.ON_RIGHT_CLICK, this);
			viewerComponent.addEventListener(Events.ON_DOUBLE_CLICK, this);
			viewerComponents.add(viewerComponent);

			Cell cell = new Cell();
			cell.setWidth(row.getWidth());
			cell.appendChild(viewerComponent);
			row.appendCellChild(cell);
		}

	} // renderDMSVersion

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (Events.ON_DOUBLE_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			DMS_ZK_Util.downloadDocument(dms, DMSViewerComp.getDMSContent());
		}
		else if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			currentCompSelection(DMSViewerComp);
		}
		else if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(WDMSVersion.class))
		{
			removeStyle();
		}
	} // onEvent

	private void currentCompSelection(DMSViewerComponent DMSViewerComp)
	{
		removeStyle();

		for (DMSViewerComponent viewerComponent : viewerComponents)
		{
			if (viewerComponent.getDMSContent().getDMS_Content_ID() == DMSViewerComp.getDMSContent().getDMS_Content_ID())
			{
				ZkCssHelper.appendStyle(DMSViewerComp.getfLabel(), DMSConstant.STYLE_CONTENT_COMP_VIEWER_SELECTED);

				prevComponent = viewerComponent;
				break;
			}
		}
	} // currentCompSelection

	public void removeStyle()
	{
		if (prevComponent != null)
			ZkCssHelper.appendStyle(prevComponent.getfLabel(), DMSConstant.STYLE_CONTENT_COMP_VIEWER_NORMAL);
	}
}
