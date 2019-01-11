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
import java.util.ArrayList;
import java.util.Arrays;

import org.compiere.model.MStorageProvider;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;

public class ThumbnailProvider implements IThumbnailProvider
{

	public static final String	DMS_THUMBNAILS_SIZES		= "DMS_THUMBNAILS_SIZES";

	private String				thumbnailBasePath			= null;
	private MStorageProvider	thumbnailStorageProvider	= null;

	@Override
	public void init()
	{
		thumbnailStorageProvider = (MStorageProvider) Utils.getThumbnailStorageProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thumbnailStorageProvider != null && !Util.isEmpty(thumbnailStorageProvider.getFolder()))
			thumbnailBasePath = thumbnailStorageProvider.getFolder();
		else
			thumbnailBasePath = "/opt/DMS_Thumbnails";
	}

	@Override
	public String getURL(I_DMS_Content content, String size)
	{
		File docFile = getFile(content, size);

		if (docFile != null)
			return docFile.getAbsolutePath();
		else
			return null;
	}

	@Override
	public File getFile(I_DMS_Content content, String size)
	{
		File imgpxfile = new File(getThumbPath(content, size));

		if (imgpxfile.exists())
			return imgpxfile;
		else
			return null;
	}

	@Override
	public ArrayList<File> getThumbnails(I_DMS_Content content)
	{
		File thumbnailFolder = new File(getThumbDirPath(content));
		return new ArrayList<File>(Arrays.asList(thumbnailFolder.listFiles()));
	}

	@Override
	public String getThumbDirPath(I_DMS_Content content)
	{
		return thumbnailBasePath + DMSConstant.FILE_SEPARATOR + Env.getAD_Client_ID(Env.getCtx()) + DMSConstant.FILE_SEPARATOR + content.getDMS_Content_ID();
	}

	@Override
	public String getThumbPath(I_DMS_Content content, String size)
	{
		String path = getThumbDirPath(content) + DMSConstant.FILE_SEPARATOR + content.getDMS_Content_ID();

		if (!Util.isEmpty(size, true))
			path += "-" + size;

		return path + ".jpg";
	}
}