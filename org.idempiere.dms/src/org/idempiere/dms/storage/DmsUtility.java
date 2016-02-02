package org.idempiere.dms.storage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MClientInfo;
import org.compiere.model.MStorageProvider;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.model.MDMS_AssociationType;
import org.idempiere.model.MDMS_ContentType;
import org.idempiere.model.MDMS_MimeType;
import org.idempiere.model.MDMS_Status;
import org.zkoss.util.media.AMedia;

public class DmsUtility
{
	private static CLogger	log	= CLogger.getCLogger(DmsUtility.class);

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

		if (clientInfo.get_ValueAsInt("DMS_StorageProvider_ID") != 0)
			return new MStorageProvider(Env.getCtx(), clientInfo.get_ValueAsInt("DMS_StorageProvider_ID"), null);
		else
			return null;
	}

	/**
	 * Use for check uploaded content for image or pdf.
	 * 
	 * @param file
	 * @return
	 */
	public static boolean accept(File file)
	{
		String[] filter = new String[] { "jpg", "png", "gif", "jpeg", "pdf", "ico" };

		if (file != null)
		{
			if (file.isDirectory())
				return false;

			for (int i = 0; i < filter.length; i++)
			{
				if (filter[i].equalsIgnoreCase(FilenameUtils.getExtension(file.getName())))
					return true;
			}
		}

		return false;
	}

	public static int getMimeTypeID(AMedia media)
	{
		int dmsMimeType_ID = -1;

		if (media != null)
			dmsMimeType_ID = DB.getSQLValue(null, "SELECT DMS_MimeType_ID FROM DMS_MimeType WHERE mimetype ilike '"
					+ media.getFormat() + "'");

		if (dmsMimeType_ID != -1)
			return dmsMimeType_ID;
		else
		{
			dmsMimeType_ID = DB
					.getSQLValue(null, "SELECT DMS_MimeType_ID FROM DMS_MimeType WHERE name ilike 'Default'");
			if (dmsMimeType_ID == -1)
			{
				MDMS_MimeType dmsMimeType = new MDMS_MimeType(Env.getCtx(), 0, null);
				dmsMimeType.setName("Default");
				dmsMimeType.setValue("Default");
				dmsMimeType.setIsDefault(true);
				dmsMimeType.setFileExtension("def");
				if (media != null)
					dmsMimeType.setMimeType(media.getContentType());
				else
					dmsMimeType.setMimeType("Default");
				dmsMimeType.saveEx();

				dmsMimeType_ID = dmsMimeType.getDMS_MimeType_ID();
			}

			return dmsMimeType_ID;
		}
	}

	public static int getStatusID()
	{
		int dms_statusID = DB.getSQLValue(null, "SELECT dms_Status_ID FROM DMS_Status WHERE name ilike 'draft'");

		if (dms_statusID != -1)
			return dms_statusID;
		else
		{
			MDMS_Status dmsStatus = new MDMS_Status(Env.getCtx(), 0, null);
			dmsStatus.setName("Draft");
			dmsStatus.setValue("Draft");
			dmsStatus.setIsDefault(true);
			dmsStatus.setDMS_ContentType_ID(DmsUtility.getContentTypeID());
			dmsStatus.saveEx();

			dms_statusID = dmsStatus.getDMS_Status_ID();
		}
		return dms_statusID;
	}

	public static int getContentTypeID()
	{
		int dms_ContentType_ID = DB.getSQLValue(null,
				"SELECT DMS_ContentType_ID FROM DMS_ContentType WHERE name ilike 'directory'");
		if (dms_ContentType_ID != -1)
			return dms_ContentType_ID;
		else
		{
			MDMS_ContentType dms_ContentType = new MDMS_ContentType(Env.getCtx(), 0, null);
			dms_ContentType.setName("Directory");
			dms_ContentType.setValue("Dir");
			dms_ContentType.setIsDefault(true);
			dms_ContentType.setM_AttributeSet_ID(100);
			dms_ContentType.saveEx();

			dms_ContentType_ID = dms_ContentType.getDMS_ContentType_ID();
		}

		return dms_ContentType_ID;
	}

	/*
	 * public static int getAttributeSet_ID() { int m_AttributeSet_ID =
	 * DB.getSQLValue(null,
	 * "SELECT M_AttributeSetInstance_ID FROM M_AttributeSetInstance WHERE value ilike 'default'"
	 * ); if (m_AttributeSet_ID == -1) { MAttributeSetInstance attributeSet =
	 * new MAttributeSetInstance(Env.getCtx(), 0, null);
	 * attributeSet.setM_AttributeSet_ID(100); attributeSet.saveEx(); return
	 * attributeSet.getM_AttributeSet_ID(); } else { return m_AttributeSet_ID; }
	 * }
	 */

	public static int getVersionID()
	{
		int versionID = DB.getSQLValue(null,
				"SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE name ilike 'version'");

		if (versionID == -1)
		{
			MDMS_AssociationType associationType = new MDMS_AssociationType(Env.getCtx(), 0, null);
			associationType.setName("Version");
			associationType.setValue("version");
			associationType.saveEx();

			versionID = associationType.getDMS_AssociationType_ID();
		}

		return versionID;
	}

	public static BufferedImage convThumbtoBufferedImage(File file, String size)
	{
		BufferedImage thumbnailImage = null;
		try
		{
			Image image = ImageIO.read(file);
			thumbnailImage = new BufferedImage(Integer.parseInt(size), Integer.parseInt(size),
					BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = thumbnailImage.createGraphics();
			graphics2D.setBackground(Color.WHITE);
			graphics2D.setPaint(Color.WHITE);
			graphics2D.fillRect(0, 0, Integer.parseInt(size), Integer.parseInt(size));
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.drawImage(image, 0, 0, Integer.parseInt(size), Integer.parseInt(size), null);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Image thumbnail creation failure:" + e.getLocalizedMessage());
			throw new AdempiereException("Image thumbnail creation failure:" + e.getLocalizedMessage());
		}

		return thumbnailImage;
	}

}
