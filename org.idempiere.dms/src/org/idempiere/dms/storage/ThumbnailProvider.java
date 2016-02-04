package org.idempiere.dms.storage;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FilenameUtils;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.model.I_DMS_Content;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class ThumbnailProvider implements IThumbnailProvider
{

	private static CLogger	log	= CLogger.getCLogger(ThumbnailProvider.class);

	@Override
	public String getURL(I_DMS_Content content, String size)
	{
		File documentfile = new File(System.getProperty("user.dir") + File.separator + "DMS_Thumbnails"
				+ File.separator + Env.getAD_Client_ID(Env.getCtx()) + File.separator + content.getDMS_Content_ID());

		if (documentfile.exists() && documentfile.isDirectory())
		{
			File[] fileList = documentfile.listFiles();

			for (int i = 0; i < fileList.length; i++)
			{
				if (fileList[i].getName().equalsIgnoreCase(content.getName()) && fileList[i].getName().contains(size))
				{
					return documentfile.getAbsolutePath() + File.separator + fileList[i].getName();
				}
			}
		}
		return null;
	}

	@Override
	public File getFile(I_DMS_Content content, String size)
	{
		File documentfile = new File(System.getProperty("user.dir") + File.separator + "DMS_Thumbnails"
				+ File.separator + Env.getAD_Client_ID(Env.getCtx()) + File.separator + content.getDMS_Content_ID());

		if (documentfile.exists() && documentfile.isDirectory())
		{
			File[] fileList = documentfile.listFiles();

			for (int i = 0; i < fileList.length; i++)
			{
				if (fileList[i].getName().contains(size))
				{
					return fileList[i];
				}
			}
		}
		return null;
	}

	@Override
	public void addThumbnail(I_DMS_Content content, File file, String size)
	{
		if (DmsUtility.accept(file))
		{
			String rootfolder = System.getProperty("user.dir");
			File rootFolder = new File(rootfolder + File.separator + "DMS_Thumbnails");
			File clientFolder = new File(rootFolder.getAbsolutePath() + File.separator
					+ Env.getAD_Client_ID(Env.getCtx()));
			File contentFolder = new File(clientFolder.getAbsolutePath() + File.separator + content.getDMS_Content_ID());

			if (!rootFolder.exists())
				rootFolder.mkdir();
			if (!clientFolder.exists())
				clientFolder.mkdirs();
			if (!contentFolder.exists())
				contentFolder.mkdirs();

			File imgpxfile = new File(contentFolder.getAbsolutePath() + File.separator + content.getDMS_Content_ID()
					+ "-" + size + ".jpg");

			if (FilenameUtils.getExtension(file.getName()).equals("pdf"))
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

					BufferedImage imagepx = DmsUtility.toBufferedImage(page.getImage(Integer.parseInt(size),
							Integer.parseInt(size), rect, null, true, true));

					ImageIO.write(imagepx, "jpg", imgpxfile);
				}
				catch (Exception ex)
				{
					log.log(Level.SEVERE, "Thumbnail Creation Failure:" + ex.getLocalizedMessage());
					throw new AdempiereException("Thumbnail Creation Failure:" + ex.getLocalizedMessage());
				}
			}
			else
			{
				try
				{
					BufferedImage thumbnailImage = DmsUtility.convThumbtoBufferedImage(file, size);
					ImageIO.write(thumbnailImage, "jpg", imgpxfile);
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
