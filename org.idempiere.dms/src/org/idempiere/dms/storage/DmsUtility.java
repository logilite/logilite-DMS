package org.idempiere.dms.storage;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MClientInfo;
import org.compiere.model.MStorageProvider;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.model.MDMS_AssociationType;
import org.idempiere.model.MDMS_Content;
import org.idempiere.model.MDMS_ContentType;
import org.idempiere.model.MDMS_MimeType;
import org.idempiere.model.MDMS_Status;
import org.zkoss.util.media.AMedia;

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
		String[] filter = new String[] { "jpg", "png", "gif", "jpeg", "pdf", "ico" };

		if (file != null)
		{
			if (file.isDirectory())
			{
				return false;
			}
			for (int i = 0; i < filter.length; i++)
			{
				if (filter[i].equalsIgnoreCase(FilenameUtils.getExtension(file.getName())))
					return true;
			}
		}
		return false;
	}

	public static int getMimeTypeId(AMedia media)
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
					.getSQLValue(null, "SELECT DMS_MimeType_ID FROM DMS_MimeType WHERE name ilike 'default'");
			if (dmsMimeType_ID == -1)
			{
				MDMS_MimeType dmsMimeType = new MDMS_MimeType(Env.getCtx(), 0, null);
				dmsMimeType.setName("default");
				dmsMimeType.setValue("default");
				dmsMimeType.setIsDefault(true);
				dmsMimeType.setFileExtension(".def");
				dmsMimeType.setMimeType(media.getContentType());
				dmsMimeType.saveEx();

				return dmsMimeType.getDMS_MimeType_ID();
			}
			else
				return dmsMimeType_ID;
		}
	}

	public static int getStatusID()
	{
		int dms_statusID = DB.getSQLValue(null, "SELECT dms_Status_ID FROM DMS_Status WHERE name ilike 'draft'");

		if (dms_statusID == -1)
		{
			MDMS_Status dmsStatus = new MDMS_Status(Env.getCtx(), 0, null);
			dmsStatus.setName("Draft");
			dmsStatus.setValue("Draft");
			dmsStatus.setIsDefault(true);
			dmsStatus.saveEx();

			return dmsStatus.getDMS_Status_ID();
		}
		else
			return dms_statusID;
	}

	public static int getContentTypeID()
	{
		int dms_ContentType_ID = DB.getSQLValue(null,
				"SELECT DMS_ContentType_ID FROM DMS_ContentType WHERE name ilike 'directory'");
		if (dms_ContentType_ID == -1)
		{
			MDMS_ContentType dms_ContentType = new MDMS_ContentType(Env.getCtx(), 0, null);
			dms_ContentType.setName("Directory");
			dms_ContentType.setValue("Dir");
			dms_ContentType.setIsDefault(true);
			// dms_ContentType.setM_AttributeSet_ID(getAttributeSet_ID());

			return dms_ContentType_ID;
		}
		else
			return dms_ContentType_ID;
	}

	/*
	 * public static int getAttributeSet_ID() { int m_AttributeSet_ID =
	 * DB.getSQLValue(null,
	 * "SELECT M_AttributeSetInstance_ID FROM M_AttributeSetInstance WHERE name ilike 'default'"
	 * ); if (m_AttributeSet_ID == -1) { MAttributeSet attributeSet = new
	 * MAttributeSet(Env.getCtx(), 0, null); attributeSet.setName("default");
	 * attributeSet.saveEx(); return attributeSet.getM_AttributeSet_ID(); } else
	 * { return m_AttributeSet_ID; } }
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

			return associationType.getDMS_AssociationType_ID();
		}
		else
			return versionID;

	}
	
}
