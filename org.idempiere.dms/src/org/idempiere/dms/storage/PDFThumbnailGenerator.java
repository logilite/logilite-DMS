package org.idempiere.dms.storage;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class PDFThumbnailGenerator implements IThumbnailGenerator
{

	private static CLogger			log							= CLogger.getCLogger(PDFThumbnailGenerator.class);

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
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
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
					path = thumbnailProvider.getThumbDirPath(content) + "-" + thumbSizesList.get(i) + ".jpg";

					imagepx = Utils.toBufferedImage(page.getImage(Integer.parseInt(thumbSizesList.get(i).toString()),
							Integer.parseInt(thumbSizesList.get(i).toString()), rect, null, true, true));

					ImageIO.write(imagepx, "jpg", baos);

					thumbnailStorageProvider.writeBLOB(path, baos.toByteArray(), content);
				}
			}
			else
			{
				path = thumbnailProvider.getThumbDirPath(content) + "-" + size + ".jpg";

				imagepx = Utils.toBufferedImage(page.getImage(Integer.parseInt(size), Integer.parseInt(size), rect,
						null, true, true));

				ImageIO.write(imagepx, "jpg", baos);

				thumbnailStorageProvider.writeBLOB(path, baos.toByteArray(), content);
			}

		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "PDF thumbnail creation failure:", e);
			throw new AdempiereException("PDF thumbnail creation failure:" + e.getLocalizedMessage());
		}
	}
}
