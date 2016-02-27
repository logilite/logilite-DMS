package org.idempiere.dms.factories;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.adempiere.base.Service;
import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.model.MClientInfo;
import org.compiere.model.MImage;
import org.compiere.model.MStorageProvider;
import org.compiere.model.MSysConfig;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.idempiere.model.MDMSMimeType;
import org.idempiere.model.MDMSStatus;
import org.idempiere.model.X_DMS_Content;
import org.zkoss.util.media.AMedia;

/**
 * @author deepak@logilite.com
 */
public class Utils
{
	private static CLogger						log								= CLogger.getCLogger(Utils.class);

	public static final String					STORAGE_PROVIDER_FILE_SEPARATOR	= "STORAGE_PROVIDER_FILE_SEPARATOR";

	public static final String					DIRECTORY						= "Directory";
	public static final String					DEFAULT							= "Default";
	public static final String					DOWNLOAD						= "Download";
	public static final String					DRAFT							= "Draft";

	static CCache<Integer, IThumbnailProvider>	cache_thumbnailProvider			= new CCache<Integer, IThumbnailProvider>(
																						"ThumbnailProvider", 2);
	static CCache<String, IThumbnailGenerator>	cache_thumbnailGenerator		= new CCache<String, IThumbnailGenerator>(
																						"ThumbnailGenerator", 2);
	static CCache<Integer, IContentManager>		cache_contentManager			= new CCache<Integer, IContentManager>(
																						"ContentManager", 2);
	static CCache<String, String>				cache_fileseparator				= new CCache<String, String>(
																						"FileSeparator", 2);
	static CCache<String, MImage>				cache_dirThumbnail				= new CCache<String, MImage>(
																						"DirThumbnail", 2);
	static CCache<Integer, MImage>				cache_mimetypeThumbnail			= new CCache<Integer, MImage>(
																						"MimetypeThumbnail", 2);

	static CCache<String, IContentEditor>		cache_contentEditor				= new CCache<String, IContentEditor>(
																						"ContentEditor", 2);

	public static IContentEditor getContentEditor(String mimeType)
	{
		IContentEditor contentEditor = cache_contentEditor.get(mimeType);

		if (contentEditor != null)
			return contentEditor;

		List<IContentEditorFactory> factories = Service.locator().list(IContentEditorFactory.class).getServices();

		for (IContentEditorFactory factory : factories)
		{
			contentEditor = factory.get(mimeType);
			if (contentEditor != null)
			{
				cache_contentEditor.put(mimeType, contentEditor);
				break;
			}
		}

		return contentEditor;
	}

	public static IThumbnailGenerator getThumbnailGenerator(String mimeType)
	{
		IThumbnailGenerator thumbnailGenerator = cache_thumbnailGenerator.get(mimeType);

		if (thumbnailGenerator != null)
			return thumbnailGenerator;

		List<IThumbnailGeneratorFactory> factories = Service.locator().list(IThumbnailGeneratorFactory.class)
				.getServices();
		for (IThumbnailGeneratorFactory factory : factories)
		{
			thumbnailGenerator = factory.get(mimeType);
			if (thumbnailGenerator != null)
			{
				thumbnailGenerator.init();
				cache_thumbnailGenerator.put(mimeType, thumbnailGenerator);
				break;
			}
		}

		return thumbnailGenerator;
	}

	public static IContentManager getContentManager(int AD_Client_ID)
	{
		IContentManager contentManager = cache_contentManager.get(AD_Client_ID);

		if (contentManager != null)
			return contentManager;

		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);

		String c_key = clientInfo.get_ValueAsString("DMS_ContentManagerType");

		if (Util.isEmpty(c_key))
			throw new AdempiereException("Content Manager is not defined");

		List<IContentManagerProvider> factories = Service.locator().list(IContentManagerProvider.class).getServices();

		for (IContentManagerProvider factory : factories)
		{
			contentManager = factory.get(AD_Client_ID);

			if (contentManager != null)
			{
				cache_contentManager.put(AD_Client_ID, contentManager);
				break;
			}
		}

		return contentManager;
	}

	public static IThumbnailProvider getThumbnailProvider(int Ad_Client_ID)
	{
		IThumbnailProvider thumbnailProvider = cache_thumbnailProvider.get(Ad_Client_ID);

		if (thumbnailProvider != null)
			return thumbnailProvider;

		List<IThumbnailProviderFactory> factories = Service.locator().list(IThumbnailProviderFactory.class)
				.getServices();

		for (IThumbnailProviderFactory factory : factories)
		{
			thumbnailProvider = factory.get(Ad_Client_ID);

			if (thumbnailProvider != null)
			{
				cache_thumbnailProvider.put(Ad_Client_ID, thumbnailProvider);
				thumbnailProvider.init();
				break;
			}
		}

		return thumbnailProvider;
	}

	public static String getStorageProviderFileSeparator()
	{
		String fileSeparator = cache_fileseparator.get(STORAGE_PROVIDER_FILE_SEPARATOR);

		if (!Util.isEmpty(fileSeparator, true))
			return fileSeparator;

		fileSeparator = MSysConfig.getValue(STORAGE_PROVIDER_FILE_SEPARATOR, "/");

		cache_fileseparator.put(STORAGE_PROVIDER_FILE_SEPARATOR, fileSeparator);

		return fileSeparator;
	}

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

	public static I_AD_StorageProvider getStorageProvider(int AD_Client_ID)
	{
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);

		if (clientInfo.get_ValueAsInt("DMS_StorageProvider_ID") != 0)
			return new MStorageProvider(Env.getCtx(), clientInfo.get_ValueAsInt("DMS_StorageProvider_ID"), null);
		else
			return null;
	}

	public static int getMimeTypeID(AMedia media)
	{
		int dmsMimeType_ID = -1;

		if (media != null)
			dmsMimeType_ID = DB.getSQLValue(null, "SELECT DMS_MimeType_ID FROM DMS_MimeType WHERE mimetype ilike '"
					+ media.getContentType() + "'");

		if (dmsMimeType_ID != -1)
			return dmsMimeType_ID;
		else
		{
			dmsMimeType_ID = DB.getSQLValue(null, "SELECT DMS_MimeType_ID FROM DMS_MimeType WHERE name ilike ?",
					DEFAULT);
			if (dmsMimeType_ID == -1)
			{
				MDMSMimeType dmsMimeType = new MDMSMimeType(Env.getCtx(), 0, null);
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
		int dms_statusID = DB.getSQLValue(null, "SELECT dms_Status_ID FROM DMS_Status WHERE name ilike ?", DRAFT);

		if (dms_statusID != -1)
			return dms_statusID;
		else
		{
			MDMSStatus dmsStatus = new MDMSStatus(Env.getCtx(), 0, null);
			dmsStatus.setName("Draft");
			dmsStatus.setValue("Draft");
			dmsStatus.setIsDefault(true);
			dmsStatus.setDMS_ContentType_ID(Utils.getContentTypeID());
			dmsStatus.saveEx();

			dms_statusID = dmsStatus.getDMS_Status_ID();
		}
		return dms_statusID;
	}

	public static int getContentTypeID()
	{
		int dms_ContentType_ID = DB.getSQLValue(null,
				"SELECT DMS_ContentType_ID FROM DMS_ContentType WHERE name ilike ?", DIRECTORY);
		if (dms_ContentType_ID != -1)
			return dms_ContentType_ID;
		else
		{
			MDMSContentType dms_ContentType = new MDMSContentType(Env.getCtx(), 0, null);
			dms_ContentType.setName("Directory");
			dms_ContentType.setValue("Dir");
			dms_ContentType.setIsDefault(true);
			dms_ContentType.setM_AttributeSet_ID(100);
			dms_ContentType.saveEx();

			dms_ContentType_ID = dms_ContentType.getDMS_ContentType_ID();
		}

		return dms_ContentType_ID;
	}

	public static BufferedImage getImageThumbnail(File file, String size)
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

	public static String getUniqueFilename(String fullPath)
	{
		File document = new File(fullPath);

		if (document.exists())
		{
			String fileName = document.getName();
			String path = fullPath.substring(0, fullPath.length() - fileName.length());
			String fileNameWOExt = fullPath.substring(fullPath.lastIndexOf("/") + 1, fullPath.lastIndexOf("."));
			if (fileNameWOExt.contains("(") && fileNameWOExt.contains(")"))
			{
				fileNameWOExt = fileNameWOExt.substring(fileNameWOExt.indexOf(0) + 1, fileNameWOExt.indexOf("("));
			}
			String ext = FilenameUtils.getExtension(document.getName());
			int n = 1;
			do
			{
				fullPath = path + fileNameWOExt + "(" + n++ + ")." + ext;
				document = new File(fullPath);
			}
			while (document.exists());
		}
		return fullPath;
	}

	public static MImage getDirThumbnail()
	{
		MImage mImage = cache_dirThumbnail.get(DIRECTORY);
		if (mImage != null)
		{
			return mImage;
		}

		int AD_Image_ID = DB.getSQLValue(null, "SELECT AD_Image_ID FROM AD_Image WHERE name ilike ?", DIRECTORY);
		mImage = new MImage(Env.getCtx(), AD_Image_ID, null);
		cache_dirThumbnail.put(DIRECTORY, mImage);
		return mImage;
	}

	public static MImage getMimetypeThumbnail(int DMS_MimeType_ID)
	{
		MImage mImage = cache_mimetypeThumbnail.get(DMS_MimeType_ID);

		if (mImage != null)
		{
			return mImage;
		}

		int Icon_ID = DB.getSQLValue(null, "SELECT Icon_ID FROM DMS_MimeType WHERE DMS_MimeType_ID=?", DMS_MimeType_ID);

		if (Icon_ID <= 0)
		{
			Icon_ID = DB.getSQLValue(null, "SELECT Icon_ID FROM DMS_MimeType WHERE Name ilike ?", DEFAULT);
			if (Icon_ID <= 0)
			{
				Icon_ID = DB.getSQLValue(null, "SELECT AD_Image_ID FROM AD_Image WHERE name ilike ?", DOWNLOAD);
			}
		}

		mImage = new MImage(Env.getCtx(), Icon_ID, null);
		cache_mimetypeThumbnail.put(DMS_MimeType_ID, mImage);
		return mImage;

	}

	public static int getDMS_Content_Related_ID(I_DMS_Content DMS_Content)
	{
		if (DMS_Content.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
			return DMS_Content.getDMS_Content_ID();
		else
		{
			int DMS_Association_ID = DB.getSQLValue(null,
					"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ? ",
					DMS_Content.getDMS_Content_ID());
			MDMSAssociation DMSAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);

			if (DMSAssociation.getDMS_Content_Related_ID() > 0)
			{
				DMS_Content = new MDMSContent(Env.getCtx(), DMSAssociation.getDMS_Content_Related_ID(), null);
				if (DMS_Content.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
					return DMSAssociation.getDMS_Content_ID();
				else
					return DMSAssociation.getDMS_Content_Related_ID();
			}
			else
				return DMSAssociation.getDMS_Content_ID();
		}
	}
}
