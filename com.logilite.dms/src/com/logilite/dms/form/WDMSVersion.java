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

package com.logilite.dms.form;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.window.Dialog;
import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;

import com.logilite.dms.DMS;
import com.logilite.dms.DMS_ZK_Util;
import com.logilite.dms.component.AbstractComponentIconViewer;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSVersion;
import com.logilite.dms.util.DMSFactoryUtils;

public class WDMSVersion extends Window implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -3613076228042516782L;

	private static CLogger		log					= CLogger.getCLogger(WDMSVersion.class);

	private Grid				gridView			= GridFactory.newGridLayout();
	private boolean				isWrite				= true;
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
		isWrite = dms.isWritePermission(content);
		try
		{
			init();

			String msg = renderDMSVersion(content);
			if (!Util.isEmpty(msg))
				Dialog.info(0, msg);
			else
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
		if (ClientInfo.isMobile())
			this.setHeight("100%");
		else
		{
			this.setWidth("44%");
			this.setHeight("50%");
		}

		this.setClosable(true);
		this.appendChild(gridView);
		this.setTitle(DMSConstant.MSG_DMS_VERSION_LIST);

		gridView.setStyle("width: 100%; height: 95%; max-height: 100%; position: relative; overflow: auto;");
	} // init

	public String renderDMSVersion(MDMSContent DMS_Content) throws IOException
	{
		MDMSAssociation association = dms.getParentAssociationFromContent(DMS_Content.getDMS_Content_ID());

		List<MDMSVersion> versionList = MDMSVersion.getVersionHistory(DMS_Content);
		if (versionList.size() == 0)
		{
			return DMSConstant.MSG_NO_VERSION_DOC_EXISTS;
		}

		HashMap<I_DMS_Version, I_DMS_Association> contentMap = new HashMap<I_DMS_Version, I_DMS_Association>();
		for (int i = 0; i < versionList.size(); i++)
			contentMap.put(versionList.get(i), association);

		String[] eventsList = new String[] { Events.ON_DOUBLE_CLICK };

		AbstractComponentIconViewer viewerComponent = (AbstractComponentIconViewer) DMSFactoryUtils.getDMSComponentViewer(DMSConstant.ICON_VIEW_LARGE);
		viewerComponent.init(dms, contentMap, gridView, DMSConstant.CONTENT_LARGE_ICON_WIDTH, DMSConstant.CONTENT_LARGE_ICON_HEIGHT, this, eventsList);
		return null;

	} // renderDMSVersion

	@Override
	public void onEvent(Event event) throws Exception
	{
		Component component = event.getTarget();
		if (Events.ON_DOUBLE_CLICK.equals(event.getName()) && (component instanceof Cell || component instanceof Row) && isWrite)
		{
			DMS_ZK_Util.downloadDocument(dms, (MDMSVersion) component.getAttribute(DMSConstant.COMP_ATTRIBUTE_VERSION));
		}
	} // onEvent
}
