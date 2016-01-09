package org.idempiere.dms.storage;

import java.awt.Rectangle;
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
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class ThumbnailGenerator implements IThumbnailGenerator
{

	private static CLogger	log	= CLogger.getCLogger(ThumbnailGenerator.class);

	I_AD_StorageProvider	provider;
	String					baseDir;

	@Override
	public void init(I_AD_StorageProvider storageProvider)
	{
		provider = storageProvider;
		baseDir = storageProvider.getFolder();
	}

	@Override
	public ArrayList<File> getThumbnails(File document)
	{
		ArrayList<File> thumbnailsFiles = new ArrayList<File>();
		if (FilenameUtils.getExtension(document.getName()).equalsIgnoreCase("pdf"))
		{
			File img150pxfile = new File(document.getName() + "-150.jpg");
			File img300pxfile = new File(document.getName() + "-300.jpg");
			File img500pxfile = new File(document.getName() + "-500.jpg");

			if (DmsUtility.accept(document))
			{
				if (FilenameUtils.getExtension(document.getName()).equalsIgnoreCase("pdf"))
				{
					try
					{
						@SuppressWarnings("resource")
						RandomAccessFile raf = new RandomAccessFile(document, "r");
						FileChannel fileChannel = raf.getChannel();
						MappedByteBuffer mbBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0,
								fileChannel.size());
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
						BufferedImage img = ImageIO.read(document);
						BufferedImage thumbImg150 = Scalr.resize(img, Method.QUALITY, Mode.AUTOMATIC, 150, 150,
								Scalr.OP_ANTIALIAS);
						BufferedImage thumbImg300 = Scalr.resize(img, Method.QUALITY, Mode.AUTOMATIC, 300, 300,
								Scalr.OP_ANTIALIAS);
						BufferedImage thumbImg500 = Scalr.resize(img, Method.QUALITY, Mode.AUTOMATIC, 500, 500,
								Scalr.OP_ANTIALIAS);

						ImageIO.write(thumbImg150, "jpg", img150pxfile);
						ImageIO.write(thumbImg300, "jpg", img300pxfile);
						ImageIO.write(thumbImg500, "jpg", img500pxfile);

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
		}

		return thumbnailsFiles;
	}
}
