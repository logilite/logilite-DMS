/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package org.idempiere.dms.storage;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.util.Utils;
import org.idempiere.model.I_DMS_Version;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class PDFThumbnailGenerator implements IThumbnailGenerator
{

	private static CLogger		log				= CLogger.getCLogger(PDFThumbnailGenerator.class);

	private DMS					dms;
	private ArrayList<String>	thumbSizesList	= null;

	/**
	 * Constructor
	 * 
	 * @param dms
	 */
	public PDFThumbnailGenerator(DMS dms)
	{
		this.dms = dms;
	}

	@Override
	public void init()
	{
		String thumbnailSizes = MSysConfig.getValue(DMSConstant.DMS_THUMBNAILS_SIZES, "150,300,500");
		thumbSizesList = new ArrayList<String>(Arrays.asList(thumbnailSizes.split(",")));
	}

	@Override
	public void addThumbnail(I_DMS_Version version, File file, String size)
	{
		try
		{
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			FileChannel fileChannel = raf.getChannel();
			MappedByteBuffer mbBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
			PDFFile pFile = new PDFFile(mbBuffer);
			PDFPage page = pFile.getPage(0);
			Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());

			if (size == null)
			{
				for (int i = 0; i < thumbSizesList.size(); i++)
					createThumbnail(version, thumbSizesList.get(i), page, rect);
			}
			else
			{
				createThumbnail(version, size, page, rect);
			}
			// Window OS Issue - Rename of file is not working after immediate
			// upload.
			/*if (mbBuffer != null)
			{
				Cleaner cleaner = ((DirectBuffer) mbBuffer).cleaner();
				cleaner.clean();
			}*/
			if (fileChannel != null)
				fileChannel.close();
			if (raf != null)
				raf.close();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "PDF thumbnail creation failure:", e);
		}
	} // addThumbnail

	public void createThumbnail(I_DMS_Version version, String size, PDFPage page, Rectangle rect) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		String path = dms.getThumbnailProvider().getThumbPath(version, size);

		BufferedImage imagepx = Utils.toBufferedImage(page.getImage(Integer.parseInt(size), Integer.parseInt(size), rect, null, true, true));

		ImageIO.write(imagepx, "jpg", baos);

		dms.getThumbnailStorageProvider().writeBLOB(path, baos.toByteArray());
	} // createThumbnail

}
