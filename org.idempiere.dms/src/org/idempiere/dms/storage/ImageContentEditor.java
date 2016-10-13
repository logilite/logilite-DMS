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

package org.idempiere.dms.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Panel;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.util.CLogger;
import org.idempiere.dms.factories.IContentEditor;
import org.idempiere.model.I_DMS_Content;
import org.zkoss.util.media.AMedia;
import org.zkoss.zul.Iframe;

public class ImageContentEditor extends Panel implements IContentEditor
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7241323248511502223L;

	public static CLogger		log					= CLogger.getCLogger(ImageContentEditor.class);

	File						file				= null;
	I_DMS_Content				content				= null;
	I_AD_StorageProvider		provider;
	String						baseDir;

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
		provider = storageProvider;
		baseDir = storageProvider.getFolder();
	}

	@Override
	public Panel initPanel()
	{
		this.setHeight("99%");
		this.setWidth("99%");

		Iframe iframeContentPriview = new Iframe();

		AMedia media = null;

		try
		{
			media = new AMedia(file, null, null);
		}
		catch (FileNotFoundException e)
		{
			log.log(Level.SEVERE, "Document cannot be displayed:" + e.getLocalizedMessage());
			throw new AdempiereException("Document cannot be displayed:" + e.getLocalizedMessage());
		}

		iframeContentPriview.setContent(media);
		iframeContentPriview.setWidth("100%");
		iframeContentPriview.setHeight("100%");
		iframeContentPriview.setStyle("overflow: auto;");

		this.appendChild(iframeContentPriview);

		return this;
	}

}
