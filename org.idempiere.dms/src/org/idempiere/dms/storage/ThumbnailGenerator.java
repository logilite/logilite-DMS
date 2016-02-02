package org.idempiere.dms.storage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.model.MDMS_Content;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class ThumbnailGenerator implements IThumbnailGenerator
{

	private static CLogger	log				= CLogger.getCLogger(ThumbnailGenerator.class);

	I_AD_StorageProvider	provider;
	String					baseDir;
	ArrayList<File>			thumbnailsFiles	= null;

	@Override
	public void init(I_AD_StorageProvider storageProvider)
	{
		provider = storageProvider;
		baseDir = storageProvider.getFolder();
	}

	@Override
	public ArrayList<File> getThumbnails(File document, MDMS_Content content)
	{
		if (DmsUtility.accept(document))
		{
			thumbnailsFiles = new ArrayList<File>();

			String rootfolder = System.getProperty("user.dir");
			File rootFolder = new File(rootfolder + "/DMS_Thumbnails");
			File clientFolder = new File(rootFolder.getAbsolutePath() + File.separator
					+ Env.getAD_Client_ID(Env.getCtx()));
			File contentFolder = new File(clientFolder.getAbsolutePath() + File.separator + content.getDMS_Content_ID());

			if (!rootFolder.exists())
				rootFolder.mkdir();
			if (!clientFolder.exists())
				clientFolder.mkdirs();
			if (!contentFolder.exists())
				contentFolder.mkdirs();

			File img150pxfile = new File(contentFolder.getAbsolutePath() + File.separator + content.getDMS_Content_ID()
					+ "-150.jpg");
			File img300pxfile = new File(contentFolder.getAbsolutePath() + File.separator + content.getDMS_Content_ID()
					+ "-300.jpg");
			File img500pxfile = new File(contentFolder.getAbsolutePath() + File.separator + content.getDMS_Content_ID()
					+ "-500.jpg");

			if (FilenameUtils.getExtension(document.getName()).equalsIgnoreCase("pdf"))
			{
				try
				{
					@SuppressWarnings("resource")
					RandomAccessFile raf = new RandomAccessFile(document, "r");
					FileChannel fileChannel = raf.getChannel();
					MappedByteBuffer mbBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
					PDFFile pFile = new PDFFile(mbBuffer);
					PDFPage page = pFile.getPage(0);
					Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox()
							.getHeight());

					BufferedImage image150px = DmsUtility.toBufferedImage(page.getImage(150, 150, rect, null, true,
							true));
					BufferedImage image300px = DmsUtility.toBufferedImage(page.getImage(300, 300, rect, null, true,
							true));
					BufferedImage image500px = DmsUtility.toBufferedImage(page.getImage(500, 500, rect, null, true,
							true));

					ImageIO.write(image150px, "jpg", img150pxfile);
					ImageIO.write(image300px, "jpg", img300pxfile);
					ImageIO.write(image500px, "jpg", img500pxfile);

					thumbnailsFiles.add(img150pxfile);
					thumbnailsFiles.add(img300pxfile);
					thumbnailsFiles.add(img500pxfile);
				}
				catch (Exception ex)
				{
					log.log(Level.SEVERE, "Illegal image format conversion: " + ex.getLocalizedMessage());
					throw new AdempiereException("Illegal image format conversion: " + ex.getLocalizedMessage());
				}
			}
			else
			{
				try
				{
					Image image = ImageIO.read(document);

					BufferedImage thumbnailImage150px = DmsUtility.convThumbtoBufferedImage(document, "150");
					BufferedImage thumbnailImage300px = DmsUtility.convThumbtoBufferedImage(document, "300");
					BufferedImage thumbnailImage500px = DmsUtility.convThumbtoBufferedImage(document, "500");

					ImageIO.write(thumbnailImage150px, "jpg", img150pxfile);
					ImageIO.write(thumbnailImage300px, "jpg", img300pxfile);
					ImageIO.write(thumbnailImage500px, "jpg", img500pxfile);

					thumbnailsFiles.add(img150pxfile);
					thumbnailsFiles.add(img300pxfile);
					thumbnailsFiles.add(img500pxfile);
				}
				catch (IOException e)
				{
					log.log(Level.SEVERE, "Image thumbnail creation failure:" + e.getLocalizedMessage());
					throw new AdempiereException("Image thumbnail creation failure:" + e.getLocalizedMessage());
				}
			}
		}

		return thumbnailsFiles;
	}
}
