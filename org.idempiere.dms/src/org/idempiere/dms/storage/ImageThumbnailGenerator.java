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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;

public class ImageThumbnailGenerator implements IThumbnailGenerator
{
	private static CLogger			log							= CLogger.getCLogger(ImageThumbnailGenerator.class);

	private String					thumbnailSizes				= null;

	private IFileStorageProvider	fileStorageProvider			= null;
	private IFileStorageProvider	thumbnailStorageProvider	= null;
	private IThumbnailProvider		thumbnailProvider			= null;

	private ArrayList<String>		thumbSizesList				= null;

	@Override
	public void init()
	{
		fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);

		if (fileStorageProvider == null)
			throw new AdempiereException("No Storage Provider Found.");

		thumbnailStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), true);

		if (thumbnailStorageProvider == null)
			throw new AdempiereException("No Thumbnail Storage Provide Found.");

		thumbnailProvider = Utils.getThumbnailProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thumbnailProvider == null)
			throw new AdempiereException("Thumbnail Storage Provide Found.");

		thumbnailSizes = MSysConfig.getValue(ThumbnailProvider.DMS_THUMBNAILS_SIZES, "150,300,500");

		thumbSizesList = new ArrayList<String>(Arrays.asList(thumbnailSizes.split(",")));
	}

	@Override
	public void addThumbnail(I_DMS_Content content, File file, String size)
	{
		String path = null;

		try
		{
			BufferedImage thumbnailImage = null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			if (size == null)
			{
				for (int i = 0; i < thumbSizesList.size(); i++)
				{
					path = thumbnailProvider.getThumbDirPath(content) + "-" + thumbSizesList.get(i) + ".jpg";

					thumbnailImage = Utils.getImageThumbnail(file, thumbSizesList.get(i).toString());
					ImageIO.write(thumbnailImage, "jpg", baos);

					thumbnailStorageProvider.writeBLOB(path, baos.toByteArray(), content);
				}
			}
			else
			{
				path = thumbnailProvider.getThumbDirPath(content) + "-" + size + ".jpg";

				thumbnailImage = Utils.getImageThumbnail(file, size);
				ImageIO.write(thumbnailImage, "jpg", baos);
				thumbnailStorageProvider.writeBLOB(path, baos.toByteArray(), content);
			}
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Image thumbnail creation failure:", e);
			//throw new AdempiereException("Image thumbnail creation failure:" + e.getLocalizedMessage());
		}
	}
}
