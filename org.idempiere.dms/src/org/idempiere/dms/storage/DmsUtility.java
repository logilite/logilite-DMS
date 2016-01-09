package org.idempiere.dms.storage;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MClientInfo;
import org.compiere.model.MStorageProvider;
import org.compiere.util.Env;

public class DmsUtility
{
	public static BufferedImage toBufferedImage(java.awt.Image src)
	{
		int w = src.getWidth(null);
		int h = src.getHeight(null);
		int type = BufferedImage.TYPE_INT_RGB; // other options
		BufferedImage dest = new BufferedImage(w, h, type);
		Graphics2D g2 = dest.createGraphics();
		g2.drawImage(src, 0, 0, null);
		g2.dispose();
		return dest;
	} // BufferedImage

	public static MStorageProvider getStorageProvider(int AD_Client_ID)
	{
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);
		return new MStorageProvider(Env.getCtx(), clientInfo.get_ValueAsInt("DMS_StorageProvider_ID"), null);
	}

	public static boolean accept(File file)
	{
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "png", "gif", "jpeg", "pdf");
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(filter);

		if (file != null)
		{
			if (file.isDirectory())
			{
				return true;
			}

			if (FilenameUtils.getExtension(file.getName()) != null
					&& filter.getExtensions().equals(FilenameUtils.getExtension(file.getName())))
			{
				return true;
			}
		}
		return false;
	}
}
