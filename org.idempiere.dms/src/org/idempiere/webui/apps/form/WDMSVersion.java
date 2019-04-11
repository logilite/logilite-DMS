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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Window;
import org.compiere.util.CLogger;
import org.idempiere.componenet.AbstractComponentIconViewer;
import org.idempiere.dms.DMS;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;

public class WDMSVersion extends Window implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -3613076228042516782L;

	private static CLogger		log					= CLogger.getCLogger(WDMSVersion.class);

	private Grid				gridView			= GridFactory.newGridLayout();
	private DMS					dms;

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
			init();
			renderDMSVersion(content);

			AEnv.showCenterScreen(this);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "DMS Version fetching failure :", e);
			throw new AdempiereException("DMS Version fetching failure :" + e);
		}
	} // Constructor

	private void init()
	{
		this.setWidth("44%");
		this.setHeight("50%");
		this.setClosable(true);
		this.appendChild(gridView);
		this.setTitle(DMSConstant.MSG_DMS_VERSION_LIST);

		gridView.setStyle("width: 100%; height: 95%; position: relative; overflow: auto;");
	} // init

	public void renderDMSVersion(MDMSContent DMS_Content) throws IOException
	{
		MDMSAssociation dmsAssociation = dms.getAssociationFromContent(DMS_Content.getDMS_Content_ID());

		List<I_DMS_Content> contentList = MDMSContent.getVersionHistory(DMS_Content);
		if (contentList.size() == 0)
		{
			throw new AdempiereException("No versions are available.");
		}

		HashMap<I_DMS_Content, I_DMS_Association> contentMap = new HashMap<I_DMS_Content, I_DMS_Association>();
		for (int i = 0; i < contentList.size(); i++)
			contentMap.put(contentList.get(i), dmsAssociation);

		String[] eventsList = new String[] { Events.ON_DOUBLE_CLICK };

		AbstractComponentIconViewer viewerComponent = (AbstractComponentIconViewer) DMS_ZK_Util.getDMSCompViewer(DMSConstant.ICON_VIEW_LARGE);
		viewerComponent.init(dms, contentMap, gridView, DMSConstant.CONTENT_LARGE_ICON_WIDTH, DMSConstant.CONTENT_LARGE_ICON_HEIGHT, this, eventsList);

	} // renderDMSVersion

	@Override
	public void onEvent(Event event) throws Exception
	{
		Component component = event.getTarget();
		if (Events.ON_DOUBLE_CLICK.equals(event.getName()) && (component instanceof Cell || component instanceof Row))
		{
			DMS_ZK_Util.downloadDocument(dms, (MDMSContent) component.getAttribute(DMSConstant.COMP_ATTRIBUTE_CONTENT));
		}
	} // onEvent
}
