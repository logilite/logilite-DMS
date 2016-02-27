package org.idempiere.dms.storage;

import java.awt.image.BufferedImage;
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
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;

public class ImageThumbnailGenerator implements IThumbnailGenerator
{
	private static CLogger	log					= CLogger.getCLogger(ImageThumbnailGenerator.class);

	private String			thumbnailBasePath	= null;
	private String			thumbnailSizes		= null;
	private String			fileSeparator		= null;

	private ArrayList<String>		thumbSizesList		= null;

	@Override
	public void init()
	{
		thumbnailBasePath = MSysConfig.getValue(ThumbnailProvider.DMS_THUMBNAIL_BASEPATH, "/opt/DMS_Thumbnails");

		thumbnailSizes = MSysConfig.getValue(ThumbnailProvider.DMS_THUMBNAILS_SIZES, "150,300,500");

		fileSeparator = Utils.getStorageProviderFileSeparator();

		thumbSizesList = new ArrayList<String>(Arrays.asList(thumbnailSizes.split(",")));
	}

	@Override
	public void addThumbnail(I_DMS_Content content, File file, String size)
	{
		File thumbnailFile = null;

		File thumbnailContentFolder = new File(thumbnailBasePath + fileSeparator + Env.getAD_Client_ID(Env.getCtx())
				+ fileSeparator + content.getDMS_Content_ID());

		if (!thumbnailContentFolder.exists())
			thumbnailContentFolder.mkdirs();

		try
		{
			BufferedImage thumbnailImage = null;

			if (size == null)
			{
				for (int i = 0; i < thumbSizesList.size(); i++)
				{
					thumbnailFile = new File(thumbnailContentFolder.getAbsolutePath() + fileSeparator
							+ content.getDMS_Content_ID() + "-" + thumbSizesList.get(i) + ".jpg");

					thumbnailImage = Utils.getImageThumbnail(file, thumbSizesList.get(i).toString());
					ImageIO.write(thumbnailImage, "jpg", thumbnailFile);
				}
			}
			else
			{
				thumbnailImage = Utils.getImageThumbnail(file, size);
				ImageIO.write(thumbnailImage, "jpg", thumbnailFile);
			}
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Image thumbnail creation failure:", e);
			throw new AdempiereException("Image thumbnail creation failure:" + e.getLocalizedMessage());
		}
	}
}
