package org.idempiere.dms.storage;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class PDFThumbnailGenerator implements IThumbnailGenerator
{

	private static CLogger	log					= CLogger.getCLogger(PDFThumbnailGenerator.class);

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
			@SuppressWarnings("resource")
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			FileChannel fileChannel = raf.getChannel();
			MappedByteBuffer mbBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
			PDFFile pFile = new PDFFile(mbBuffer);
			PDFPage page = pFile.getPage(0);
			Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());

			BufferedImage imagepx = null;

			if (size == null)
			{
				for (int i = 0; i < thumbSizesList.size(); i++)
				{
					thumbnailFile = new File(thumbnailContentFolder.getAbsolutePath() + fileSeparator
							+ content.getDMS_Content_ID() + "-" + thumbSizesList.get(i) + ".jpg");

					imagepx = Utils.toBufferedImage(page.getImage(Integer.parseInt(thumbSizesList.get(i).toString()),
							Integer.parseInt(thumbSizesList.get(i).toString()), rect, null, true, true));

					ImageIO.write(imagepx, "jpg", thumbnailFile);
				}
			}
			else
			{
				thumbnailFile = new File(thumbnailContentFolder.getAbsolutePath() + fileSeparator
						+ content.getDMS_Content_ID() + "-" + size + ".jpg");

				imagepx = Utils.toBufferedImage(page.getImage(Integer.parseInt(size), Integer.parseInt(size), rect,
						null, true, true));

				ImageIO.write(imagepx, "jpg", thumbnailFile);
			}

		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "PDF thumbnail creation failure:", e);
			throw new AdempiereException("PDF thumbnail creation failure:" + e.getLocalizedMessage());
		}
	}
}
