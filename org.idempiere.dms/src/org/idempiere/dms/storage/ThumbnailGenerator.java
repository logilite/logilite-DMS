package org.idempiere.dms.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.model.I_DMS_Content;

public class ThumbnailGenerator implements IThumbnailGenerator
{

	private static CLogger	log					= CLogger.getCLogger(ThumbnailGenerator.class);

	private String			thumbnailBasePath	= null;
	private String			thumbnailSizes		= null;

	private int				AD_Client_ID		= 0;

	private Properties		ctx					= null;

	private ArrayList<File>	thumbnailsFiles		= null;
	private List<String>	thumbSizesList		= null;

	@Override
	public void init()
	{
		ctx = Env.getCtx();
		AD_Client_ID = Env.getAD_Client_ID(ctx);

		thumbnailBasePath = MSysConfig.getValue(ThumbnailProvider.DMS_THUMBNAIL_BASEPATH, "/opt/DMS_Thumbnails");
		thumbnailSizes = MSysConfig.getValue(ThumbnailProvider.DMS_THUMBNAILS_SIZES, "150,300,500");

		thumbSizesList = Arrays.asList(thumbnailSizes.split(","));
	}

	@Override
	public ArrayList<File> getThumbnails(File document, I_DMS_Content content)
	{
		if (DmsUtility.accept(document))
		{
			File imgpxfile = null;
			thumbnailsFiles = new ArrayList<File>();

			for (int i = 0; i < thumbSizesList.size(); i++)
			{
				imgpxfile = new File(thumbnailBasePath + File.separator + AD_Client_ID + File.separator
						+ content.getDMS_Content_ID() + "-" + thumbSizesList.get(i) + ".jpg");

				if (imgpxfile.exists())
				{
					thumbnailsFiles.add(imgpxfile);
				}
				else
				{
					log.log(Level.SEVERE, thumbSizesList.get(i) + "px Thumbnail was not found.");
				}
			}
		}
		return thumbnailsFiles;
	}
}
