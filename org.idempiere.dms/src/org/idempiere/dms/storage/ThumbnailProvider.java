package org.idempiere.dms.storage;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MStorageProvider;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.model.I_DMS_Content;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class ThumbnailProvider implements IThumbnailProvider
{

	private static CLogger		log				= CLogger.getCLogger(ThumbnailProvider.class);
	private MStorageProvider	storageProvider	= null;

	@Override
	public String getURL(I_DMS_Content content, String size)
	{
		storageProvider = DmsUtility.getStorageProvider(Env.getAD_Client_ID(Env.getCtx()));

		File documentfile = new File(storageProvider.getURL() + File.separator + Env.getAD_Client_ID(Env.getCtx())
				+ File.separator + content.getDMS_Content_ID());

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
		storageProvider = DmsUtility.getStorageProvider(Env.getAD_Client_ID(Env.getCtx()));

		File documentfile = new File(storageProvider.getURL() + File.separator + Env.getAD_Client_ID(Env.getCtx())
				+ File.separator + content.getDMS_Content_ID());

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
		storageProvider = DmsUtility.getStorageProvider(Env.getAD_Client_ID(Env.getCtx()));

		File rootFolder = new File(storageProvider.getURL() + File.separator + storageProvider.getFolder());
		File clientFolder = new File(rootFolder.getAbsolutePath() + File.separator + Env.getAD_Client_ID(Env.getCtx()));
		File contentFolder = new File(clientFolder.getAbsolutePath() + File.separator + content.getDMS_Content_ID());
		File documentFile = new File(contentFolder.getAbsolutePath() + File.separator + content.getDMS_Content_ID()
				+ "." + FilenameUtils.getExtension(file.getName()));

		FileOutputStream fos = null;

		if (!rootFolder.exists())
			rootFolder.mkdir();
		if (!clientFolder.exists())
			clientFolder.mkdirs();
		if (!contentFolder.exists())
			contentFolder.mkdirs();

		if (!documentFile.exists())
		{
			try
			{
				fos = new FileOutputStream(documentFile);
				Path path = Paths.get(file.getAbsolutePath());
				fos.write(Files.readAllBytes(path));
				fos.close();
			}
			catch (IOException e)
			{
				log.log(Level.SEVERE, "Thumbnail Blob Writing Failure: " + e.getLocalizedMessage());
				throw new AdempiereException("Thumbnail Blob Writing Failure: " + e.getLocalizedMessage());
			}
		}

		if (DmsUtility.accept(documentFile))
		{
			File imgpxfile = new File(contentFolder.getAbsoluteFile() + File.separator + content.getDMS_Content_ID()
					+ "-" + size + ".jpg");

			if (FilenameUtils.getExtension(documentFile.getName()).equals("pdf"))
			{
				try
				{
					@SuppressWarnings("resource")
					RandomAccessFile raf = new RandomAccessFile(documentFile, "r");
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
					BufferedImage img = ImageIO.read(documentFile);
					BufferedImage thumbImg = Scalr.resize(img, Method.QUALITY, Mode.AUTOMATIC, Integer.parseInt(size),
							Integer.parseInt(size), Scalr.OP_ANTIALIAS);
					ImageIO.write(thumbImg, "jpg", imgpxfile);
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
