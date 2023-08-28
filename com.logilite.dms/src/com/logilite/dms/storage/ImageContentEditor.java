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

package com.logilite.dms.storage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;

import org.adempiere.base.Core;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Panel;
import org.apache.commons.io.FileUtils;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.util.CLogger;
import org.zkoss.io.Files;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Div;
import org.zkoss.zul.Script;

import com.logilite.dms.factories.IContentEditor;
import com.logilite.dms.model.I_DMS_Content;

public class ImageContentEditor extends Panel implements IContentEditor, EventListener<Event>
{

	/**
	 * 
	 */
	private static final long		serialVersionUID			= 7241323248511502223L;
	private static final String		EVENT_ON_LOAD_DMS_CONTENT	= "onLoadDMSContent";

	public static CLogger			log							= CLogger.getCLogger(ImageContentEditor.class);

	private Div						divzoomcontent				= new Div();
	private File					file						= null;
	private String					src;
	@SuppressWarnings("unused")
	private I_DMS_Content			content						= null;

	@Override
	public void setFile(File file)
	{
		this.file = file;
	}

	@Override
	public void setContent(I_DMS_Content content)
	{
		this.content = content;
	}

	@Override
	public void init(I_AD_StorageProvider storageProvider)
	{
		
	}

	@Override
	public Panel initPanel()
	{
		loadPanAndZoomJS();

		getImageSrc();

		this.appendChild(divzoomcontent);

		divzoomcontent.addEventListener(EVENT_ON_LOAD_DMS_CONTENT, this);
		
		Events.echoEvent(EVENT_ON_LOAD_DMS_CONTENT, divzoomcontent, null);

		return this;
	} // initPanel

	/**
	 * Get JS of PanAndZoom And Append it
	 */
	public void loadPanAndZoomJS()
	{
		// Load JS of PanAndZoom On the DMS Content
		Script script = new Script();
		URL urlJS = Core.getResourceFinder().getResource("js/dmscontentpreview_min.js");
		if (urlJS == null)
			throw new AdempiereException("Fail to load scanner JS");

		try
		{
			byte[] byteCode = Files.readAll(urlJS.openStream());
			script.setContent(new String(byteCode));
			divzoomcontent.appendChild(script);
		}
		catch (IOException e)
		{
			throw new AdempiereException("Fail to load script : " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Get Base64 encoder src of the Image File
	 */
	public void getImageSrc()
	{
		try
		{
			byte[] bytes = FileUtils.readFileToByteArray(file);
			src = Base64.getEncoder().encodeToString(bytes);
		}
		catch (IOException e)
		{
			throw new AdempiereException("Fail to encode src of the image file: " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (EVENT_ON_LOAD_DMS_CONTENT.equalsIgnoreCase(event.getName()))
		{
			// Create canvas as per encoder src of image in the Image Container
			Clients.evalJavaScript("onPenAndZoom('" + this.getUuid() + "', '" + src + "')");
		}
	}
}
