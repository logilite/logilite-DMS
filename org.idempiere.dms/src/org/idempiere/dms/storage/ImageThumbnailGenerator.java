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

import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.idempiere.dms.DMS;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;

public class ImageThumbnailGenerator implements IThumbnailGenerator
{
	private static CLogger		log				= CLogger.getCLogger(ImageThumbnailGenerator.class);

	private DMS					dms;
	private ArrayList<String>	thumbSizesList	= null;

	/**
	 * Constructor
	 * 
	 * @param dms
	 */
	public ImageThumbnailGenerator(DMS dms)
	{
		this.dms = dms;
	}

	@Override
	public void init()
	{
		String thumbnailSizes = MSysConfig.getValue(ThumbnailProvider.DMS_THUMBNAILS_SIZES, "150,300,500");
		thumbSizesList = new ArrayList<String>(Arrays.asList(thumbnailSizes.split(",")));
	}

	@Override
	public void addThumbnail(I_DMS_Content content, File file, String size)
	{
		try
		{
			if (size == null)
			{
				for (int i = 0; i < thumbSizesList.size(); i++)
					createThumbnail(content, file, thumbSizesList.get(i));
			}
			else
			{
				createThumbnail(content, file, size);
			}
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Image thumbnail creation failure:", e);
		}
	}

	public void createThumbnail(I_DMS_Content content, File file, String size) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		String path = dms.getThumbnailProvider().getThumbPath(content, size);

		BufferedImage thumbnailImage = Utils.getImageThumbnail(file, size);

		ImageIO.write(thumbnailImage, "jpg", baos);

		dms.getThumbnailStorageProvider().writeBLOB(path, baos.toByteArray(), content);
	} // createThumbnail
}