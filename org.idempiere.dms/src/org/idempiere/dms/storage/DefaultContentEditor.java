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
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Panel;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.model.MImage;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IContentEditor;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSMimeType;
import org.zkoss.util.media.AMedia;
import org.zkoss.zul.Iframe;

/**
 * Default Content Editor
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class DefaultContentEditor extends Panel implements IContentEditor
{

	/**
	 * 
	 */
	private static final long			serialVersionUID	= -4779531876093362001L;

	public static CLogger				log					= CLogger.getCLogger(DefaultContentEditor.class);

	private static ArrayList<String>	listMimeType		= new ArrayList<String>();
	private static ArrayList<String>	listFileExtension	= new ArrayList<String>();

	File								file				= null;
	I_DMS_Content						content				= null;
	I_AD_StorageProvider				provider;

	static
	{
		// Supported mime type for Text viewer
		listMimeType.add("text/css");
		listMimeType.add("text/plain");
		listMimeType.add("text/html");
		listMimeType.add("text/richtext");

		// Supported extension for Text viewer
		listFileExtension.add("sh");
		listFileExtension.add("js");
		listFileExtension.add("mf");
		listFileExtension.add("bat");
		listFileExtension.add("inf");
		listFileExtension.add("ini");
		listFileExtension.add("sql");
		listFileExtension.add("txt");
		listFileExtension.add("rtx");
		listFileExtension.add("java");
		listFileExtension.add("conf");
		listFileExtension.add("patch");
		listFileExtension.add("properties");
	}

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
	}

	@Override
	public Panel initPanel()
	{
		this.setHeight("100%");
		this.setWidth("100%");
		this.setStyle("overflow: auto; -webkit-overflow-scrolling: touch;");

		Iframe iframeContentPriview = new Iframe();
		iframeContentPriview.setWidth("100%");
		iframeContentPriview.setHeight("98%");
		iframeContentPriview.setContent(getMedia());
		this.appendChild(iframeContentPriview);

		return this;
	} // initPanel

	/**
	 * Get media of file or default image if mimetype or extension is not
	 * supported
	 * 
	 * @return {@link AMedia}
	 */
	public AMedia getMedia()
	{
		AMedia media = null;
		String mimeType = null;
		try
		{
			mimeType = Files.probeContentType(file.toPath());
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "ERROR: Unable to determine file type for " + file.getName() + " due to exception " + e);
			throw new AdempiereException("ERROR: Unable to determine file type for " + file.getName() + " due to exception " + e, e);
		}

		String extension = FilenameUtils.getExtension(file.getName());
		extension = extension != null ? extension.toLowerCase() : null;
		if ((mimeType != null && listMimeType.contains(mimeType)) || listFileExtension.contains(extension))
		{
			try
			{
				media = new AMedia(file, null, null);
			}
			catch (FileNotFoundException e)
			{
				log.log(Level.SEVERE, "ERROR: file not found for " + file.getName() + " due to exception " + e);
				throw new AdempiereException("ERROR: file not found for " + file.getName() + " due to exception " + e, e);
			}
		}
		else
		{
			// Default logo
			int logoID = content.getDMS_MimeType().getIcon500_ID();
			if (logoID <= 0)
				logoID = content.getDMS_MimeType().getIcon300_ID();
			if (logoID <= 0)
				logoID = content.getDMS_MimeType().getIcon_ID();

			MImage image;
			if (logoID > 0)
				image = new MImage(Env.getCtx(), logoID, null);
			else
				image = MDMSMimeType.getThumbnail(content.getDMS_MimeType_ID());

			if (image.getData() != null)
				media = new AMedia(null, null, null, image.getData());
		}
		return media;
	} // getMedia
}
