package org.idempiere.dms.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.compiere.model.MStorageProvider;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;

public class ThumbnailProvider implements IThumbnailProvider
{

	private String				thumbnailBasePath			= null;

	public static final String	DMS_THUMBNAILS_SIZES		= "DMS_THUMBNAILS_SIZES";

	private MStorageProvider	thumbnailStorageProvider	= null;

	private ArrayList<File>		thumbnailsFiles				= null;

	private String				fileSeparator				= null;

	@Override
	public void init()
	{
		thumbnailStorageProvider = (MStorageProvider) Utils.getThumbnailStorageProvider(Env.getAD_Client_ID(Env
				.getCtx()));

		if (thumbnailStorageProvider != null && !Util.isEmpty(thumbnailStorageProvider.getFolder()))
		{
			thumbnailBasePath = thumbnailStorageProvider.getFolder();
		}
		else
		{
			thumbnailBasePath = "/opt/DMS_Thumbnails";
		}

		fileSeparator = Utils.getStorageProviderFileSeparator();
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
	public ArrayList<File> getThumbnails(I_DMS_Content content)
	{
		File thumbnailFolder = new File(thumbnailBasePath + fileSeparator + Env.getAD_Client_ID(Env.getCtx())
				+ fileSeparator + content.getDMS_Content_ID());

		thumbnailsFiles = new ArrayList<File>(Arrays.asList(thumbnailFolder.listFiles()));
		return thumbnailsFiles;
	}

	@Override
	public String getThumbDirPath(I_DMS_Content content)
	{
		return thumbnailBasePath + fileSeparator + Env.getAD_Client_ID(Env.getCtx()) + fileSeparator
				+ content.getDMS_Content_ID() + fileSeparator + content.getDMS_Content_ID();
	}
}