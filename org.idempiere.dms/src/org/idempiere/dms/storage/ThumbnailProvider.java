package org.idempiere.dms.storage;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class ThumbnailProvider implements IThumbnailProvider
{

	private static CLogger		log						= CLogger.getCLogger(ThumbnailProvider.class);

	private String				thumbnailBasePath		= null;
	private String				thumbnailSizes			= null;

	public static final String	DMS_THUMBNAIL_BASEPATH	= "DMS_THUMBNAIL_BASEPATH";
	public static final String	DMS_THUMBNAILS_SIZES	= "DMS_THUMBNAILS_SIZES";
	private static final String	MIME_EXTENTION_PDF		= "pdf";

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
	public void addThumbnail(I_DMS_Content content, File file, String size)
	{
		if (DmsUtility.accept(file))
		{
			File thumbnailFile = null;

			File thumbnailContentFolder = new File(thumbnailBasePath + fileSeparator
					+ Env.getAD_Client_ID(Env.getCtx()) + fileSeparator + content.getDMS_Content_ID());

			if (!thumbnailContentFolder.exists())
				thumbnailContentFolder.mkdirs();

			if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase(MIME_EXTENTION_PDF))
			{
				try
				{
					@SuppressWarnings("resource")
					RandomAccessFile raf = new RandomAccessFile(file, "r");
					FileChannel fileChannel = raf.getChannel();
					MappedByteBuffer mbBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
					PDFFile pFile = new PDFFile(mbBuffer);
					PDFPage page = pFile.getPage(0);
					Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox()
							.getHeight());

					BufferedImage imagepx = null;

					if (size == null)
					{
						for (int i = 0; i < thumbSizesList.size(); i++)
						{
							thumbnailFile = new File(thumbnailContentFolder.getAbsolutePath() + fileSeparator
									+ content.getDMS_Content_ID() + "-" + thumbSizesList.get(i) + ".jpg");

							imagepx = DmsUtility.toBufferedImage(page.getImage(
									Integer.parseInt(thumbSizesList.get(i).toString()),
									Integer.parseInt(thumbSizesList.get(i).toString()), rect, null, true, true));

							ImageIO.write(imagepx, "jpg", thumbnailFile);
						}
					}
					else
					{
						thumbnailFile = new File(thumbnailContentFolder.getAbsolutePath() + fileSeparator
								+ content.getDMS_Content_ID() + "-" + size + ".jpg");

						imagepx = DmsUtility.toBufferedImage(page.getImage(Integer.parseInt(size),
								Integer.parseInt(size), rect, null, true, true));

						ImageIO.write(imagepx, "jpg", thumbnailFile);
					}

				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, "PDF thumbnail creation failure:" + e.getLocalizedMessage());
					throw new AdempiereException("PDF thumbnail creation failure:" + e.getLocalizedMessage());
				}
			}
			else
			{
				try
				{
					BufferedImage thumbnailImage = null;

					if (size == null)
					{
						for (int i = 0; i < thumbSizesList.size(); i++)
						{
							thumbnailFile = new File(thumbnailContentFolder.getAbsolutePath() + fileSeparator
									+ content.getDMS_Content_ID() + "-" + thumbSizesList.get(i) + ".jpg");

							thumbnailImage = DmsUtility.getImageThumbnail(file, thumbSizesList.get(i).toString());
							ImageIO.write(thumbnailImage, "jpg", thumbnailFile);
						}
					}
					else
					{
						thumbnailImage = DmsUtility.getImageThumbnail(file, size);
						ImageIO.write(thumbnailImage, "jpg", thumbnailFile);
					}
				}
				catch (IOException e)
				{
					log.log(Level.SEVERE, "Image thumbnail creation failure:" + e.getLocalizedMessage());
					throw new AdempiereException("Image thumbnail creation failure:" + e.getLocalizedMessage());
				}
			}
		}
	}
}
