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

package org.idempiere.dms.factories;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.adempiere.base.Core;
import org.adempiere.base.IResourceFinder;
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
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.idempiere.model.X_DMS_Content;
import org.zkoss.image.AImage;
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
	public static final String					LINK							= "Link";
	public static final String					RECORD							= "Record";

	// constant for index fields

	public static final String					NAME							= "Name";
	public static final String					CREATED							= "created";
	public static final String					CREATEDBY						= "createdBy";
	public static final String					UPDATED							= "updated";
	public static final String					UPDATEDBY						= "updatedBy";
	public static final String					DESCRIPTION						= "description";
	public static final String					CONTENTTYPE						= "contentType";
	public static final String					DMS_CONTENT_ID					= "DMS_Content_ID";
	public static final String					AD_Table_ID						= "AD_Table_ID";
	public static final String					RECORD_ID						= "Record_ID";

	private static final String					SQL_GETASSOCIATIONTYPE			= "SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE name ilike ?";

	private static final String					SQL_GETASI						= "SELECT a.Name,ai.value,ai.valuetimestamp FROM M_AttributeInstance ai "
																						+ " INNER JOIN  M_Attribute a ON (ai.M_Attribute_ID = a.M_Attribute_ID) "
																						+ " WHERE ai.M_AttributeSetInstance_Id = ?";

	public static final String					SQL_GET_RELATED_FOLDER_CONTENT	= "WITH ContentAssociation AS "
																						+ " ( "
																						+ " SELECT	c.DMS_Content_ID, a.DMS_Content_Related_ID, c.ContentBasetype, "
																						+ " a.DMS_Association_ID, a.DMS_AssociationType_ID, a.AD_Table_ID, a.Record_ID "
																						+ " FROM DMS_Association a "
																						+ " INNER JOIN DMS_Content c	ON (c.DMS_Content_ID = a.DMS_Content_ID) "
																						+ " ) "
																						+ " SELECT "
																						+ " COALESCE((SELECT a.DMS_Content_ID FROM DMS_Association a WHERE a.DMS_Content_Related_ID = ca.DMS_Content_ID AND a.DMS_AssociationType_ID = 1000000 ORDER BY SeqNo DESC FETCH FIRST ROW ONLY), DMS_Content_ID) AS DMS_Content_ID, "
																						+ " COALESCE((SELECT a.DMS_Content_Related_ID FROM DMS_Association a WHERE a.DMS_Content_Related_ID = ca.DMS_Content_ID AND a.DMS_AssociationType_ID = 1000000 ORDER BY SeqNo DESC FETCH FIRST ROW ONLY), DMS_Content_Related_ID) AS DMS_Content_Related_ID, DMS_Association_ID "
																						+ " FROM ContentAssociation ca "
																						+ " WHERE "
																						+ " (COALESCE(DMS_Content_Related_ID,0) = COALESCE(?,0)) OR "
																						+ " (COALESCE(DMS_Content_Related_ID,0) = COALESCE(?,0) AND ContentBaseType = 'DIR') ";

	public static final String					SQL_GET_RELATED_CONTENT			= "SELECT DMS_Association_ID,DMS_Content_ID FROM DMS_Association WHERE DMS_Content_Related_ID = ? AND DMS_AssociationType_ID = 1000000 OR DMS_Content_ID = ? Order By DMS_Association_ID";

	static CCache<Integer, IThumbnailProvider>	cache_thumbnailProvider			= new CCache<Integer, IThumbnailProvider>(
																						"ThumbnailProvider", 2);
	static CCache<String, IThumbnailGenerator>	cache_thumbnailGenerator		= new CCache<String, IThumbnailGenerator>(
																						"ThumbnailGenerator", 2);
	static CCache<Integer, IContentManager>		cache_contentManager			= new CCache<Integer, IContentManager>(
																						"ContentManager", 2);
	static CCache<String, MImage>				cache_dirThumbnail				= new CCache<String, MImage>(
																						"DirThumbnail", 2);
	static CCache<Integer, MImage>				cache_mimetypeThumbnail			= new CCache<Integer, MImage>(
																						"MimetypeThumbnail", 2);
	static CCache<String, MImage>				cache_linkThumbnail				= new CCache<String, MImage>(
																						"LinkThumbnail", 2);

	public static IContentEditor getContentEditor(String mimeType)
	{
		IContentEditor contentEditor = null;
		List<IContentEditorFactory> factories = Service.locator().list(IContentEditorFactory.class).getServices();

		for (IContentEditorFactory factory : factories)
		{
			contentEditor = factory.get(mimeType);
			if (contentEditor != null)
			{
				break;
			}
		}

		return contentEditor;
	}

	/**
	 * generate the thumbnail of content or directory
	 * 
	 * @param mimeType
	 * @return
	 */
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
			contentManager = factory.get(c_key);

			if (contentManager != null)
			{
				cache_contentManager.put(AD_Client_ID, contentManager);
				break;
			}
		}

		return contentManager;
	}

	/**
	 * apply the thumbnail of content or directory
	 * 
	 * @param Ad_Client_ID
	 * @return
	 */
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

	public static IMountingStrategy getMountingStrategy(String Table_Name)
	{
		IMountingStrategy mounting = null;
		List<IMountingFactory> factories = Service.locator().list(IMountingFactory.class).getServices();

		for (IMountingFactory factory : factories)
		{
			mounting = factory.getMountingStrategy(Table_Name);

			if (mounting != null)
			{
				return mounting;
			}
		}
		return null;
	}

	/**
	 * get File separator "/"
	 * 
	 * @return
	 */
	public static String getStorageProviderFileSeparator()
	{
		String fileSeparator = MSysConfig.getValue(STORAGE_PROVIDER_FILE_SEPARATOR, "0");

		if (fileSeparator.equals("0"))
			fileSeparator = File.separator;

		return fileSeparator;
	}

	/**
	 * convert component image toBufferedImage
	 * 
	 * @param src
	 * @return
	 */
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

	/**
	 * @param AD_Client_ID
	 * @return
	 */
	public static I_AD_StorageProvider getStorageProvider(int AD_Client_ID)
	{
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);

		if (clientInfo.get_ValueAsInt("DMS_StorageProvider_ID") != 0)
			return new MStorageProvider(Env.getCtx(), clientInfo.get_ValueAsInt("DMS_StorageProvider_ID"), null);
		else
			return null;
	}

	/**
	 * @param AD_Client_ID
	 * @return
	 */
	public static I_AD_StorageProvider getThumbnailStorageProvider(int AD_Client_ID)
	{
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);

		if (clientInfo.get_ValueAsInt("DMS_Thumb_Storage_Provider_ID") != 0)
			return new MStorageProvider(Env.getCtx(), clientInfo.get_ValueAsInt("DMS_Thumb_Storage_Provider_ID"), null);
		else
			return null;
	}

	/**
	 * get mimetypeID from media
	 * 
	 * @param media
	 * @return
	 */
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
		}

		return dmsMimeType_ID;
	}

	/**
	 * get thumbnail for file
	 * 
	 * @param file
	 * @param size
	 * @return
	 */
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

	/**
	 * get unique name for version history
	 * 
	 * @param fullPath
	 * @return
	 */
	public static String getUniqueFilename(String fullPath)
	{
		File document = new File(fullPath);

		if (document.exists())
		{
			String fileName = document.getName();
			String path = fullPath.substring(0, fullPath.length() - fileName.length());
			String fileNameWOExt = fullPath.substring(fullPath.lastIndexOf(getStorageProviderFileSeparator()) + 1, fullPath.lastIndexOf("."));
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

	public static String getCopiedUniqueFilename(String fullPath)
	{
		File document = new File(fullPath);

		if (document.exists())
		{
			String fileName = document.getName();
			String path = fullPath.substring(0, fullPath.length() - fileName.length());
			String fileNameWOExt = fullPath.substring(fullPath.lastIndexOf(getStorageProviderFileSeparator()) + 1, fullPath.lastIndexOf("."));
			if (fileNameWOExt.matches("\\(.*\\d\\)"))
			{
				fileNameWOExt = fileNameWOExt.substring(fileNameWOExt.lastIndexOf(0) + 1,
						fileNameWOExt.lastIndexOf("("));
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

	public static String getUniqueFoldername(String fullPath)
	{
		File document = new File(fullPath);

		if (document.exists())
		{
			String fileName = document.getName();
			String path = fullPath.substring(0, fullPath.length() - fileName.length());
			String fileNameWOExt = fullPath.substring(fullPath.lastIndexOf(getStorageProviderFileSeparator()) + 1, fullPath.length());
			if (fileNameWOExt.matches("\\(.*\\d\\)"))
			{
				fileNameWOExt = fileNameWOExt.substring(fileNameWOExt.lastIndexOf(0) + 1,
						fileNameWOExt.lastIndexOf("("));
			}
			int n = 1;
			do
			{
				fullPath = path + fileNameWOExt + "(" + n++ + ")";
				document = new File(fullPath);
			}
			while (document.exists());
		}
		return fullPath;
	}

	/**
	 * get thumbnail of directory
	 * 
	 * @return
	 */
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

	/**
	 * get thumbnail of content related mimetype
	 * 
	 * @param DMS_MimeType_ID
	 * @return
	 */
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

	/**
	 * get DMS_Content_Related_ID from DMS_content
	 * 
	 * @param DMS_Content
	 * @return
	 */
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

	/**
	 * get DMS_Association_Link_ID
	 * 
	 * @return
	 */
	public static int getDMS_Association_Link_ID()
	{
		int linkID = 0;
		linkID = DB.getSQLValue(null, SQL_GETASSOCIATIONTYPE, LINK);

		if (linkID != -1)
			return linkID;
		else
			return 0;
	}

	/**
	 * get Thumbnail of create Link
	 * 
	 * @return
	 */
	public static MImage getLinkThumbnail()
	{
		MImage mImage = cache_linkThumbnail.get(LINK);
		if (mImage != null)
		{
			return mImage;
		}

		int AD_Image_ID = DB.getSQLValue(null, "SELECT AD_Image_ID FROM AD_Image WHERE name ilike ?", LINK);
		mImage = new MImage(Env.getCtx(), AD_Image_ID, null);
		cache_linkThumbnail.put(LINK, mImage);
		return mImage;
	}

	/**
	 * get DMS_Association_Record_ID
	 * 
	 * @return
	 */
	public static int getDMS_Association_Record_ID()
	{
		int recordID = 0;
		recordID = DB.getSQLValue(null, SQL_GETASSOCIATIONTYPE, RECORD);

		if (recordID != -1)
			return recordID;
		else
			return 0;
	}

	public static Map<String, Object> createIndexMap(MDMSContent DMSContent, MDMSAssociation DMSAssociation)
	{
		Map<String, Object> solrValue = new HashMap<String, Object>();

		solrValue.put(NAME, DMSContent.getName().toLowerCase());
		solrValue.put(CREATED, DMSContent.getCreated());
		solrValue.put(CREATEDBY, DMSContent.getCreatedBy());
		solrValue.put(UPDATED, DMSContent.getUpdated());
		solrValue.put(UPDATEDBY, DMSContent.getUpdatedBy());
		solrValue.put(DESCRIPTION, DMSContent.getDescription());
		solrValue.put(CONTENTTYPE, DMSContent.getDMS_ContentType_ID());
		solrValue.put(DMS_CONTENT_ID, DMSContent.getDMS_Content_ID());
		solrValue.put(AD_Table_ID, DMSAssociation.getAD_Table_ID());
		solrValue.put(RECORD_ID, DMSAssociation.getRecord_ID());

		if (DMSContent.getM_AttributeSetInstance_ID() > 0)
		{
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try
			{
				stmt = DB.prepareStatement(SQL_GETASI, null);
				stmt.setInt(1, DMSContent.getM_AttributeSetInstance_ID());
				rs = stmt.executeQuery();

				if (rs.isBeforeFirst())
				{
					while (rs.next())
					{
						if (rs.getObject("value") != null)
						{
							if (rs.getObject("valuetimestamp") != null)
							{
								solrValue.put("ASI_" + rs.getString("Name"), rs.getObject("valuetimestamp"));
							}
							else
							{
								solrValue.put("ASI_" + rs.getString("Name"), rs.getObject("value"));
							}
						}
					}
				}
			}

			catch (SQLException e)
			{
				log.log(Level.SEVERE, "ASI fetching failure.", e);
				throw new AdempiereException("ASI fetching failure." + e.getLocalizedMessage());
			}
		}

		return solrValue;
	}

	public static void renameFolder(MDMSContent content, String baseURL, String renamedURL)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try
		{
			pstmt = DB.prepareStatement(Utils.SQL_GET_RELATED_FOLDER_CONTENT, null);
			pstmt.setInt(1, content.getDMS_Content_ID());
			pstmt.setInt(2, content.getDMS_Content_ID());

			rs = pstmt.executeQuery();

			while (rs.next())
			{
				MDMSContent dmsContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);

				if (dmsContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
				{
					if (dmsContent.getParentURL().startsWith(baseURL))
					{
						dmsContent.setParentURL(dmsContent.getParentURL().replaceFirst(baseURL, renamedURL));
						dmsContent.saveEx();
					}
					renameFolder(dmsContent, baseURL, renamedURL);
				}
				else
				{
					PreparedStatement ps = DB.prepareStatement(SQL_GET_RELATED_CONTENT, null);
					ps.setInt(1, dmsContent.getDMS_Content_ID());
					ps.setInt(2, dmsContent.getDMS_Content_ID());

					ResultSet res = ps.executeQuery();

					while (res.next())
					{
						MDMSContent content_file = new MDMSContent(Env.getCtx(), res.getInt("DMS_Content_ID"), null);

						if (content_file.getParentURL().startsWith(baseURL))
						{
							content_file.setParentURL(content_file.getParentURL().replaceFirst(baseURL, renamedURL));
							content_file.saveEx();
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Content renaming failure: ", e);
			throw new AdempiereException("Content renaming failure: " + e.getLocalizedMessage());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}

	public static AImage getImage(String name)
	{
		IResourceFinder rf = null;
		URL url = null;
		AImage image = null;
		try
		{
			rf = Core.getResourceFinder();
			url = rf.getResource("/dmsimages/" + name);
			image = new AImage(url);
		}
		catch (IOException e1)
		{
			log.log(Level.INFO, name + " Icon not found");
		}
		return image;
	}

	public static String readableFileSize(long size)
	{
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "Byte", "kB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	public static String getToolTipTextMsg(I_DMS_Content content)
	{
		MDMSContentType mdmsContentType = null;
		String msg = null;

		if (content.getDMS_ContentType_ID() > 0)
		{
			mdmsContentType = new MDMSContentType(Env.getCtx(), content.getDMS_ContentType_ID(), null);
			msg = "Content Type: " + mdmsContentType.getName() + "\n";
		}

		if (content.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
		{
			IFileStorageProvider fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);
			IContentManager contentManager = getContentManager(Env.getAD_Client_ID(Env.getCtx()));

			if (fileStorageProvider == null)
				throw new AdempiereException("File Storage Provider not found.");

			if (contentManager == null)
				throw new AdempiereException("Content manager is not found.");

			File file = fileStorageProvider.getFile(contentManager.getPath(content));

			if (!Util.isEmpty(msg))
				msg = msg + "Size: " + readableFileSize(file.length()) + "\n";
			else
				msg = "Size: " + readableFileSize(file.length()) + "\n";
		}
		else
		{
			if (!Util.isEmpty(content.getDMS_FileSize()) && !Util.isEmpty(msg))
			{
				msg = msg + "Size: " + content.getDMS_FileSize() + "\n";
			}
			else
			{
				msg = "Size: " + content.getDMS_FileSize() + "\n";
			}
		}

		msg = msg + "Created: " + content.getCreated();

		return msg;
	}

	public static void initiateMountingContent(String table_Name, int Record_ID, int AD_Table_ID)
	{
		IFileStorageProvider fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);

		String mountingBaseName = MSysConfig.getValue("DMS_MOUNTING_BASE", "Attachment");
		String baseDir = fileStorageProvider.getBaseDirectory(null);
		String fileSeprator = Utils.getStorageProviderFileSeparator();

		File file = new File(baseDir + fileSeprator + mountingBaseName);

		int mountingContent_ID = 0;
		int tableNameContentID = 0;
		int recordContentID = 0;

		if (!file.exists())
		{
			file.mkdirs();
			mountingContent_ID = createDMSContent(mountingBaseName, null);
			createAssociation(mountingContent_ID, 0, Record_ID, AD_Table_ID);
		}
		else
		{
			mountingContent_ID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE name = ?",
					mountingBaseName);
		}

		file = new File(baseDir + fileSeprator + mountingBaseName + fileSeprator + table_Name);

		if (!file.exists())
		{
			file.mkdirs();
			tableNameContentID = createDMSContent(table_Name, fileSeprator + mountingBaseName);
			createAssociation(tableNameContentID, mountingContent_ID, Record_ID, AD_Table_ID);
		}
		else
		{
			tableNameContentID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE name = ?",
					table_Name);
		}

		file = new File(baseDir + fileSeprator + mountingBaseName + fileSeprator + table_Name + fileSeprator
				+ Record_ID);

		if (!file.exists())
		{
			file.mkdirs();
			recordContentID = createDMSContent(String.valueOf(Record_ID), fileSeprator + mountingBaseName
					+ fileSeprator + table_Name);
			createAssociation(recordContentID, tableNameContentID, Record_ID, AD_Table_ID);
		}
	}

	private static int createDMSContent(String mountingBaseName, String parentURL)
	{
		MDMSContent DMSContent = new MDMSContent(Env.getCtx(), 0, null);
		DMSContent.setName(mountingBaseName);
		DMSContent.setValue(mountingBaseName);
		DMSContent.setDMS_MimeType_ID(Utils.getMimeTypeID(null));
		DMSContent.setParentURL(parentURL);
		DMSContent.setContentBaseType(X_DMS_Content.CONTENTBASETYPE_Directory);
		DMSContent.setIsMounting(true);
		DMSContent.saveEx();
		return DMSContent.getDMS_Content_ID();
	}

	private static void createAssociation(int Content_ID, int Content_Related_ID, int Record_ID, int AD_Table_ID)
	{
		MDMSAssociation DMSAssociation = new MDMSAssociation(Env.getCtx(), 0, null);
		DMSAssociation.setDMS_Content_ID(Content_ID);
		DMSAssociation.setDMS_Content_Related_ID(Content_Related_ID);
		DMSAssociation.setAD_Table_ID(AD_Table_ID);
		DMSAssociation.setRecord_ID(Record_ID);
		DMSAssociation.saveEx();
	}
}