package org.idempiere.dms.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;

public class ThumbnailProvider implements IThumbnailProvider
{

	private static CLogger		log						= CLogger.getCLogger(ThumbnailProvider.class);

	private String				thumbnailBasePath		= null;
	private String				thumbnailSizes			= null;

	public static final String	DMS_THUMBNAIL_BASEPATH	= "DMS_THUMBNAIL_BASEPATH";
	public static final String	DMS_THUMBNAILS_SIZES	= "DMS_THUMBNAILS_SIZES";

	private ArrayList<File>		thumbnailsFiles			= null;

	private String				fileSeparator			= null;

	private ArrayList			thumbSizesList			= null;

	@Override
	public void init()
	{
		thumbnailBasePath = MSysConfig.getValue(DMS_THUMBNAIL_BASEPATH, "/opt/DMS_Thumbnails");

		thumbnailSizes = MSysConfig.getValue(DMS_THUMBNAILS_SIZES, "150,300,500");

		fileSeparator = Utils.getStorageProviderFileSeparator();

		thumbSizesList = new ArrayList(Arrays.asList(thumbnailSizes.split(",")));
	}

	@Override
	public String getURL(I_DMS_Content content, String size)
	{
		File documentfile = null;

		if (size != null)
		{
			documentfile = new File(thumbnailBasePath + fileSeparator + Env.getAD_Client_ID(Env.getCtx())
					+ fileSeparator + content.getDMS_Content_ID() + fileSeparator + content.getDMS_Content_ID() + "-"
					+ size + ".jpg");
		}
		else
		{
			documentfile = new File(thumbnailBasePath + fileSeparator + Env.getAD_Client_ID(Env.getCtx())
					+ fileSeparator + content.getDMS_Content_ID());
		}

		if (documentfile.exists())
			return documentfile.getAbsolutePath();
		else
			return null;
	}

	@Override
	public File getFile(I_DMS_Content content, String size)
	{
		File imgpxfile = new File(thumbnailBasePath + fileSeparator + Env.getAD_Client_ID(Env.getCtx()) + fileSeparator
				+ content.getDMS_Content_ID() + fileSeparator + content.getDMS_Content_ID() + "-" + size + ".jpg");

		if (imgpxfile.exists())
			return imgpxfile;
		else
			return null;
	}

	@Override
	public ArrayList<File> getThumbnails(File document, I_DMS_Content content)
	{
		File imgpxfile = null;
		thumbnailsFiles = new ArrayList<File>();

		for (int i = 0; i < thumbSizesList.size(); i++)
		{
			imgpxfile = new File(thumbnailBasePath + fileSeparator + Env.getAD_Client_ID(Env.getCtx()) + fileSeparator
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
		return thumbnailsFiles;
	}
}