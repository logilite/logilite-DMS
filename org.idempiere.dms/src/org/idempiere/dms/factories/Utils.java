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
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.adempiere.base.Core;
import org.adempiere.base.IResourceFinder;
import org.adempiere.base.Service;
import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MClientInfo;
import org.compiere.model.MImage;
import org.compiere.model.MStorageProvider;
import org.compiere.model.MSysConfig;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.zkoss.image.AImage;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.WrongValueException;

/**
 * @author deepak@logilite.com
 */
public class Utils
{
	private static CLogger						log										= CLogger.getCLogger(Utils.class);

	private static final String					SQL_GET_ASI								= "SELECT REPLACE(a.Name,' ','_') AS Name, ai.Value, ai.ValueTimestamp, ai.ValueNumber, ai.ValueInt FROM M_AttributeInstance ai "
																								+ " INNER JOIN M_Attribute a ON (ai.M_Attribute_ID = a.M_Attribute_ID) "
																								+ " WHERE ai.M_AttributeSetInstance_ID = ?";

	public static final String					SQL_GET_RELATED_FOLDER_CONTENT_COMMON	= "WITH ContentAssociation AS "
																								+ " ( "
																								+ " SELECT	c.DMS_Content_ID, a.DMS_Content_Related_ID, c.ContentBasetype, "
																								+ " a.DMS_Association_ID, a.DMS_AssociationType_ID, a.AD_Table_ID, a.Record_ID "
																								+ " FROM DMS_Association a "
																								+ " INNER JOIN DMS_Content c	ON (c.DMS_Content_ID = a.DMS_Content_ID  #IsActive#) "
																								+ " WHERE c.AD_Client_ID=?), "
																								+ "  VersionRelatedIDs AS ( "
																								+ "  SELECT DMS_Content_Related_ID, MAX(SeqNo) AS SeqNo FROM DMS_Association a WHERE a.DMS_AssociationType_ID = 1000000 GROUP  BY DMS_Content_Related_ID "
																								+ " ) "
																								+ " SELECT "
																								+ " NVL(( SELECT a.DMS_Content_ID FROM  DMS_Association a INNER JOIN VersionRelatedIDs x ON (x.DMS_Content_Related_ID = a.DMS_Content_Related_ID AND x.SeqNo = a.SeqNo ) WHERE  a.DMS_Content_Related_ID = ca.DMS_Content_ID AND a.DMS_AssociationType_ID = 1000000 "
																								+ "  ) , DMS_Content_ID ) AS DMS_Content_ID, "
																								+ " NVL(ca.DMS_Content_Related_ID,DMS_Content_Related_ID) AS DMS_Content_Related_ID ,"
																								+ " NVL((SELECT DMS_Association_ID FROM (SELECT a.DMS_Association_ID FROM DMS_Association a WHERE a.DMS_Content_Related_ID = DMS_Content_ID AND a.DMS_Association_ID = 1000000 ORDER BY seqno DESC) b #DB_SPECIFIC_CONDITION#), DMS_Association_ID) AS DMS_Association_ID "
																								+ " FROM ContentAssociation ca "
																								+ " WHERE "
																								+ " (NVL(DMS_Content_Related_ID,0) = NVL(?,0)) OR "
																								+ " (NVL(DMS_Content_Related_ID,0) = NVL(?,0) AND ContentBaseType = 'DIR') ";

	public static final String					SQL_GET_RELATED_CONTENT					= "SELECT DMS_Association_ID, DMS_Content_ID FROM DMS_Association "
																								+ " WHERE DMS_Content_Related_ID = ? AND DMS_AssociationType_ID = 1000000 OR DMS_Content_ID = ? "
																								+ " ORDER BY DMS_Association_ID";

	public static String						SQL_GET_RELATED_FOLDER_CONTENT			= null;
	public static String						SQL_GET_RELATED_FOLDER_CONTENT_ALL		= null;
	public static String						SQL_GET_RELATED_FOLDER_CONTENT_ACTIVE	= null;

	static CCache<Integer, IThumbnailProvider>	cache_thumbnailProvider					= new CCache<Integer, IThumbnailProvider>("ThumbnailProvider", 2);
	static CCache<String, IThumbnailGenerator>	cache_thumbnailGenerator				= new CCache<String, IThumbnailGenerator>("ThumbnailGenerator", 2);
	static CCache<Integer, IContentManager>		cache_contentManager					= new CCache<Integer, IContentManager>("ContentManager", 2);
	static CCache<String, MImage>				cache_dirThumbnail						= new CCache<String, MImage>("DirThumbnail", 2);
	static CCache<Integer, MImage>				cache_mimetypeThumbnail					= new CCache<Integer, MImage>("MimetypeThumbnail", 2);
	static CCache<String, Integer>				cache_contentType						= new CCache<String, Integer>("ContentTypeCache", 100);

	// Oracle Database compatible
	static
	{

		SQL_GET_RELATED_FOLDER_CONTENT = SQL_GET_RELATED_FOLDER_CONTENT_COMMON.replace("#DB_SPECIFIC_CONDITION#",
				DB.isPostgreSQL() == true ? "FETCH FIRST ROW ONLY" : " WHERE rownum <= 1");

		SQL_GET_RELATED_FOLDER_CONTENT_ALL = SQL_GET_RELATED_FOLDER_CONTENT.replace("#IsActive#", "");

		SQL_GET_RELATED_FOLDER_CONTENT_ACTIVE = SQL_GET_RELATED_FOLDER_CONTENT.replace("#IsActive#", "AND c.IsActive='Y' AND a.IsActive='Y'");
	}

	/**
	 * Factory - Content Editor
	 * 
	 * @param mimeType
	 * @return {@link IContentEditor}
	 */
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
	 * Factory - Thumbnail Generator. Generate the thumbnail of content or
	 * directory
	 * 
	 * @param dms
	 * @param mimeType
	 * @return {@link IThumbnailGenerator}
	 */
	public static IThumbnailGenerator getThumbnailGenerator(DMS dms, String mimeType)
	{
		String key = (Env.getAD_Client_ID(Env.getCtx()) + "_" + mimeType);

		IThumbnailGenerator thumbnailGenerator = cache_thumbnailGenerator.get(key);

		if (thumbnailGenerator != null)
			return thumbnailGenerator;

		List<IThumbnailGeneratorFactory> factories = Service.locator().list(IThumbnailGeneratorFactory.class).getServices();
		for (IThumbnailGeneratorFactory factory : factories)
		{
			thumbnailGenerator = factory.get(dms, mimeType);
			if (thumbnailGenerator != null)
			{
				thumbnailGenerator.init();
				cache_thumbnailGenerator.put(key, thumbnailGenerator);
				break;
			}
		}

		return thumbnailGenerator;
	}

	/**
	 * Factory - Content Manager
	 * 
	 * @param AD_Client_ID
	 * @return {@link IContentManager}
	 */
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
	 * Factory - Thumbnail Provider. Apply the thumbnail of content or directory
	 * 
	 * @param dms
	 * @param AD_Client_ID
	 * @return {@link IThumbnailProvider}
	 */
	public static IThumbnailProvider getThumbnailProvider(int AD_Client_ID)
	{
		IThumbnailProvider thumbnailProvider = cache_thumbnailProvider.get(AD_Client_ID);

		if (thumbnailProvider != null)
			return thumbnailProvider;

		List<IThumbnailProviderFactory> factories = Service.locator().list(IThumbnailProviderFactory.class).getServices();

		for (IThumbnailProviderFactory factory : factories)
		{
			thumbnailProvider = factory.get(AD_Client_ID);

			if (thumbnailProvider != null)
			{
				cache_thumbnailProvider.put(AD_Client_ID, thumbnailProvider);
				thumbnailProvider.init();
				break;
			}
		}

		return thumbnailProvider;
	}

	/**
	 * Factory - Mounting Strategy
	 * 
	 * @param Table_Name
	 * @return {@link IMountingStrategy}
	 */
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
	 * @return "/" or "\" Depends on storage provider
	 */
	public static String getStorageProviderFileSeparator()
	{
		String fileSeparator = MSysConfig.getValue(DMSConstant.STORAGE_PROVIDER_FILE_SEPARATOR, "0");

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
	 * get MimeTypeID from File
	 * 
	 * @param file
	 * @return
	 */
	public static int getMimeTypeID(File file)
	{
		int dmsMimeType_ID = -1;

		if (file != null && getFileMimeType(file) != null)
			dmsMimeType_ID = DB.getSQLValue(null, "SELECT DMS_MimeType_ID FROM DMS_MimeType WHERE UPPER(MimeType) = '" + getFileMimeType(file).toUpperCase()
					+ "'");

		if (dmsMimeType_ID != -1)
			return dmsMimeType_ID;
		else
		{
			dmsMimeType_ID = DB.getSQLValue(null, "SELECT DMS_MimeType_ID FROM DMS_MimeType WHERE name = ?", DMSConstant.DEFAULT);
		}

		return dmsMimeType_ID;
	}

	public static String getFileMimeType(File file)
	{

		// InputStream is = new BufferedInputStream(new FileInputStream(file));
		// mimeType = URLConnection.guessContentTypeFromStream(is);

		return URLConnection.guessContentTypeFromName(file.getName());
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
			thumbnailImage = new BufferedImage(Integer.parseInt(size), Integer.parseInt(size), BufferedImage.TYPE_INT_RGB);
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
			String fileNameWOExt = fullPath.substring(fullPath.lastIndexOf(DMSConstant.FILE_SEPARATOR) + 1, fullPath.lastIndexOf("."));
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
			String fileNameWOExt = fullPath.substring(fullPath.lastIndexOf(DMSConstant.FILE_SEPARATOR) + 1, fullPath.lastIndexOf("."));
			if (fileNameWOExt.matches("\\(.*\\d\\)"))
			{
				fileNameWOExt = fileNameWOExt.substring(fileNameWOExt.lastIndexOf(0) + 1, fileNameWOExt.lastIndexOf("("));
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
			String fileNameWOExt = fullPath.substring(fullPath.lastIndexOf(DMSConstant.FILE_SEPARATOR) + 1, fullPath.length());
			if (fileNameWOExt.matches("\\(.*\\d\\)"))
			{
				fileNameWOExt = fileNameWOExt.substring(fileNameWOExt.lastIndexOf(0) + 1, fileNameWOExt.lastIndexOf("("));
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
		MImage mImage = cache_dirThumbnail.get(DMSConstant.DIRECTORY);
		if (mImage != null)
		{
			return mImage;
		}

		int AD_Image_ID = DB.getSQLValue(null, "SELECT AD_Image_ID FROM AD_Image WHERE Upper(name) =  UPPER(?) ", DMSConstant.DIRECTORY);
		mImage = new MImage(Env.getCtx(), AD_Image_ID, null);
		cache_dirThumbnail.put(DMSConstant.DIRECTORY, mImage);
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
			Icon_ID = DB.getSQLValue(null, "SELECT Icon_ID FROM DMS_MimeType WHERE UPPER(Name) = UPPER(?) ", DMSConstant.DEFAULT);
			if (Icon_ID <= 0)
			{
				Icon_ID = DB.getSQLValue(null, "SELECT AD_Image_ID FROM AD_Image WHERE UPPER(Name) = UPPER(?) ", DMSConstant.DOWNLOAD);
			}
		}

		mImage = new MImage(Env.getCtx(), Icon_ID, null);
		cache_mimetypeThumbnail.put(DMS_MimeType_ID, mImage);
		return mImage;

	}

	public static int getContentTypeID(String contentType, int AD_Client_ID)
	{
		Integer contentTypeID = cache_contentType.get(contentType);
		if (contentTypeID == null || contentTypeID <= 0)
		{
			contentTypeID = DB.getSQLValue(null, "SELECT DMS_ContentType_ID FROM DMS_ContentType WHERE IsActive = 'Y' AND Value = ? AND AD_Client_ID = ?",
					contentType, AD_Client_ID);

			if (contentTypeID > 0)
				cache_contentType.put(contentType, contentTypeID);
		}
		return contentTypeID;
	}

	/**
	 * get DMS_Content_Related_ID from DMS_content
	 * 
	 * @param DMS_Content
	 * @return
	 */
	public static int getDMS_Content_Related_ID(I_DMS_Content DMS_Content)
	{
		if (DMS_Content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
			return DMS_Content.getDMS_Content_ID();
		else
		{
			int DMS_Association_ID = DB.getSQLValue(null, "SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ? ",
					DMS_Content.getDMS_Content_ID());
			MDMSAssociation DMSAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);

			if (DMSAssociation.getDMS_Content_Related_ID() > 0)
			{
				DMS_Content = new MDMSContent(Env.getCtx(), DMSAssociation.getDMS_Content_Related_ID(), null);
				if (DMS_Content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
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
		linkID = DB.getSQLValue(null, MDMSAssociationType.SQL_GET_ASSOCIATION_TYPE, MDMSAssociationType.AssociationType_Link);

		if (linkID != -1)
			return linkID;
		else
			return 0;
	}

	/**
	 * get DMS_Association_Record_ID
	 * 
	 * @return
	 */
	public static int getDMS_Association_Record_ID()
	{
		int recordID = DB.getSQLValue(null, MDMSAssociationType.SQL_GET_ASSOCIATION_TYPE, DMSConstant.RECORD);

		if (recordID != -1)
			return recordID;
		else
			return 0;
	}

	/**
	 * Create Index Map for Solr
	 * 
	 * @param DMSContent
	 * @param DMSAssociation
	 * @return Map
	 */
	public static Map<String, Object> createIndexMap(MDMSContent DMSContent, MDMSAssociation DMSAssociation)
	{
		Map<String, Object> solrValue = new HashMap<String, Object>();
		solrValue.put(DMSConstant.AD_CLIENT_ID, DMSContent.getAD_Client_ID());
		solrValue.put(DMSConstant.NAME, DMSContent.getName().toLowerCase());
		solrValue.put(DMSConstant.CREATED, DMSContent.getCreated());
		solrValue.put(DMSConstant.CREATEDBY, DMSContent.getCreatedBy());
		solrValue.put(DMSConstant.UPDATED, DMSContent.getUpdated());
		solrValue.put(DMSConstant.UPDATEDBY, DMSContent.getUpdatedBy());
		solrValue.put(DMSConstant.DESCRIPTION, (!Util.isEmpty(DMSContent.getDescription(), true) ? DMSContent.getDescription().toLowerCase() : null));
		solrValue.put(DMSConstant.CONTENTTYPE, DMSContent.getDMS_ContentType_ID());
		solrValue.put(DMSConstant.DMS_CONTENT_ID, DMSContent.getDMS_Content_ID());
		solrValue.put(DMSConstant.AD_Table_ID, DMSAssociation.getAD_Table_ID());
		solrValue.put(DMSConstant.RECORD_ID, DMSAssociation.getRecord_ID());
		solrValue.put(DMSConstant.SHOW_INACTIVE, !DMSContent.isActive());

		if (DMSContent.getM_AttributeSetInstance_ID() > 0)
		{
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try
			{
				stmt = DB.prepareStatement(SQL_GET_ASI, null);
				stmt.setInt(1, DMSContent.getM_AttributeSetInstance_ID());
				rs = stmt.executeQuery();

				if (rs.isBeforeFirst())
				{
					while (rs.next())
					{
						String fieldName = "ASI_" + rs.getString("Name");

						if (rs.getTimestamp(MAttributeInstance.COLUMNNAME_ValueTimeStamp) != null)
							solrValue.put(fieldName, rs.getTimestamp(MAttributeInstance.COLUMNNAME_ValueTimeStamp));
						else if (rs.getDouble(MAttributeInstance.COLUMNNAME_ValueNumber) > 0)
							solrValue.put(fieldName, rs.getDouble(MAttributeInstance.COLUMNNAME_ValueNumber));
						else if (rs.getInt(MAttributeInstance.COLUMNNAME_ValueInt) > 0)
							solrValue.put(fieldName, rs.getInt(MAttributeInstance.COLUMNNAME_ValueInt));
						else if (!Util.isEmpty(rs.getString(MAttributeInstance.COLUMNNAME_Value), true))
							solrValue.put(fieldName, rs.getString(MAttributeInstance.COLUMNNAME_Value));
					}
				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, "ASI fetching failure.", e);
				throw new AdempiereException("ASI fetching failure." + e.getLocalizedMessage());
			}
			finally
			{
				DB.close(rs, stmt);
				rs = null;
				stmt = null;
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
			pstmt = DB.prepareStatement(Utils.SQL_GET_RELATED_FOLDER_CONTENT_ALL, null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			pstmt.setInt(2, content.getDMS_Content_ID());
			pstmt.setInt(3, content.getDMS_Content_ID());

			rs = pstmt.executeQuery();

			while (rs.next())
			{
				MDMSContent dmsContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);

				if (dmsContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
				{
					String parentURL = dmsContent.getParentURL() == null ? "" : dmsContent.getParentURL();
					if (parentURL.startsWith(baseURL))
					{
						dmsContent.setParentURL(parentURL.replaceFirst(Pattern.quote(baseURL), renamedURL));
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
							content_file.setParentURL(content_file.getParentURL().replaceFirst(Pattern.quote(baseURL), renamedURL));
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

		if (content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			IFileStorageProvider fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);
			IContentManager contentManager = getContentManager(Env.getAD_Client_ID(Env.getCtx()));

			if (fileStorageProvider == null)
				throw new AdempiereException("File Storage Provider not found.");

			if (contentManager == null)
				throw new AdempiereException("Content manager is not found.");

			File file = fileStorageProvider.getFile(contentManager.getPath(content));

			if (file != null)
			{
				if (!Util.isEmpty(msg))
					msg = msg + "Size: " + readableFileSize(file.length()) + "\n";
				else
					msg = "Size: " + readableFileSize(file.length()) + "\n";
			}
			else
			{
				msg = null;
			}
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
		String mountingBaseName = MSysConfig.getValue("DMS_MOUNTING_BASE", "Attachment");
		initiateMountingContent(mountingBaseName, table_Name, Record_ID, AD_Table_ID);
	}

	public static void initiateMountingContent(String mountingBaseName, String table_Name, int Record_ID, int AD_Table_ID)
	{
		IFileStorageProvider fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);
		String baseDir = fileStorageProvider.getBaseDirectory(null);

		File file = new File(baseDir + DMSConstant.FILE_SEPARATOR + mountingBaseName);

		int mountingContent_ID = 0;
		int tableNameContentID = 0;
		int recordContentID = 0;

		if (!file.exists())
		{
			file.mkdirs();
			mountingContent_ID = createDMSContent(mountingBaseName, MDMSContent.CONTENTBASETYPE_Directory, null, true);
			createAssociation(mountingContent_ID, 0, Record_ID, AD_Table_ID, 0, 0, null);
		}
		else
		{
			mountingContent_ID = DB.getSQLValue(null,
					"SELECT DMS_Content_ID FROM DMS_Content WHERE name = ? AND AD_Client_ID= ? AND ContentBaseType ='DIR' AND ParentUrl IS NULL",
					mountingBaseName, Env.getAD_Client_ID(Env.getCtx()));
		}

		if (!Util.isEmpty(table_Name) && Record_ID > 0)
		{
			file = new File(baseDir + DMSConstant.FILE_SEPARATOR + mountingBaseName + DMSConstant.FILE_SEPARATOR + table_Name);

			if (!file.exists())
			{
				file.mkdirs();
				tableNameContentID = createDMSContent(table_Name, MDMSContent.CONTENTBASETYPE_Directory, DMSConstant.FILE_SEPARATOR + mountingBaseName, true);
				createAssociation(tableNameContentID, mountingContent_ID, Record_ID, AD_Table_ID, 0, 0, null);
			}
			else
			{
				tableNameContentID = DB.getSQLValue(null, "SELECT DMS_Content_ID FROM DMS_Content WHERE name = ? AND AD_Client_ID= ?", table_Name,
						Env.getAD_Client_ID(Env.getCtx()));
			}

			file = new File(baseDir + DMSConstant.FILE_SEPARATOR + mountingBaseName + DMSConstant.FILE_SEPARATOR + table_Name + DMSConstant.FILE_SEPARATOR
					+ Record_ID);

			if (!file.exists())
			{
				file.mkdirs();
				recordContentID = createDMSContent(String.valueOf(Record_ID), MDMSContent.CONTENTBASETYPE_Directory, DMSConstant.FILE_SEPARATOR
						+ mountingBaseName + DMSConstant.FILE_SEPARATOR + table_Name, true);
				createAssociation(recordContentID, tableNameContentID, Record_ID, AD_Table_ID, 0, 0, null);
			}
		}
	}

	/**
	 * Create DMS Content
	 * 
	 * @param name
	 * @param contentBaseType
	 * @param parentURL
	 * @param isMounting
	 * @return DMS_Content_ID
	 */
	public static int createDMSContent(String name, String contentBaseType, String parentURL, boolean isMounting)
	{
		return createDMSContent(name, name, contentBaseType, parentURL, null, null, 0, 0, isMounting, null);
	} // createDMSContent

	/**
	 * Create DMS Content
	 * 
	 * @param name
	 * @param value
	 * @param contentBaseType
	 * @param parentURL
	 * @param desc
	 * @param media
	 * @param contentTypeID
	 * @param asiID
	 * @param isMounting
	 * @param trxName
	 * @return DMS_Content_ID
	 */
	public static int createDMSContent(String name, String value, String contentBaseType, String parentURL, String desc, File file, int contentTypeID,
			int asiID, boolean isMounting, String trxName)
	{
		MDMSContent content = new MDMSContent(Env.getCtx(), 0, trxName);
		content.setName(name);
		content.setValue(value);
		content.setDescription(desc);
		content.setParentURL(parentURL);
		content.setIsMounting(isMounting);
		content.setContentBaseType(contentBaseType);
		content.setDMS_MimeType_ID(Utils.getMimeTypeID(file));
		if (asiID > 0)
			content.setM_AttributeSetInstance_ID(asiID);
		if (contentTypeID > 0)
			content.setDMS_ContentType_ID(contentTypeID);
		if (file != null)
			content.setDMS_FileSize(Utils.readableFileSize(FileUtils.sizeOf(file)));
		content.saveEx();

		log.log(Level.INFO, "New DMS_Content_ID = " + content.getDMS_Content_ID() + " for " + name + " as (" + contentBaseType + ") at " + parentURL);

		return content.getDMS_Content_ID();
	} // createDMSContent

	/**
	 * Create Association
	 * 
	 * @param contentID
	 * @param contentRelatedID
	 * @param Record_ID
	 * @param AD_Table_ID
	 * @param associationTypeID
	 * @param seqNo
	 * @param trxName
	 * @return DMS_Association_ID
	 */
	public static int createAssociation(int contentID, int contentRelatedID, int Record_ID, int AD_Table_ID, int associationTypeID, int seqNo, String trxName)
	{
		MDMSAssociation association = new MDMSAssociation(Env.getCtx(), 0, trxName);
		association.setSeqNo(seqNo);
		association.setDMS_Content_ID(contentID);
		association.setDMS_Content_Related_ID(contentRelatedID);
		if (Record_ID > 0)
			association.setRecord_ID(Record_ID);
		if (AD_Table_ID > 0)
			association.setAD_Table_ID(AD_Table_ID);
		if (associationTypeID > 0)
			association.setDMS_AssociationType_ID(associationTypeID);
		association.saveEx();

		log.log(Level.INFO, "New DMS_Association_ID = " + association.getDMS_Association_ID() + " of Content=" + contentID + " TableID=" + AD_Table_ID
				+ " RecordID=" + Record_ID);

		return association.getDMS_Association_ID();
	} // createAssociation

	/**
	 * Check file name is valid
	 * 
	 * @param fileName
	 * @param isThrowEx
	 * @return
	 */
	public static String isValidFileName(String fileName, boolean isThrowEx)
	{
		String error = null;
		if (fileName.equals("") || fileName.equals(null))
			error = "FillMandatory";

		if (fileName.length() > DMSConstant.MAX_FILENAME_LENGTH)
			error = "Invalid File Name. file name less than 250 character";

		if (!fileName.matches(DMSConstant.REG_EXP_FILENAME))
			error = "Invalid File Name.";

		if (!Utils.isFileNameEndWithNotBracket(fileName))
			error = "Invalid File Name. not support end with ()";

		if (!Util.isEmpty(error, true))
			throw new WrongValueException(Msg.translate(Env.getCtx(), error));

		return error;
	} // isValidFileName

	public static boolean isFileNameEndWithNotBracket(String fileName)
	{
		boolean isvalidFName = false;
		int indexofClosingP = fileName.lastIndexOf(')');
		if (indexofClosingP > 0)
		{
			if ((indexofClosingP + 1) == fileName.length())
			{
				int indexofOpeningP = fileName.lastIndexOf('(');
				if (indexofOpeningP > 0)
				{
					isvalidFName = false;
				}
				else
				{
					isvalidFName = true;
				}
			}
			else
			{
				String s = fileName.substring(indexofClosingP + 1, fileName.length());
				if (!s.matches(DMSConstant.REG_SPACE))
				{
					isvalidFName = false;
				}
				else
				{
					isvalidFName = true;
				}
			}
		}
		else
		{
			isvalidFName = true;
		}

		return isvalidFName;
	}

	/**
	 * Create Directory
	 * 
	 * @param dirName - Directory Name
	 * @param dmsContent - Root or Parent Directory or Null
	 * @param AD_Table_ID - Table ID
	 * @param Record_ID - Record ID
	 * @param fileStorageProvider - File Storage Provider
	 * @param contentMngr - Content Manager
	 * @param errorIfDirExists - Throw error if directory Exists
	 * @param trxName - Transaction Name
	 * @return DMS Content
	 */
	public static MDMSContent createDirectory(String dirName, MDMSContent content, int AD_Table_ID, int Record_ID, IFileStorageProvider fileStorageProvider,
			IContentManager contentMngr, boolean errorIfDirExists, String trxName)
	{
		int contentID = 0;

		if (Util.isEmpty(dirName) || dirName.equals(""))
			throw new WrongValueException(DMSConstant.MSG_FILL_MANDATORY);

		if (dirName.length() > DMSConstant.MAX_FILENAME_LENGTH)
			throw new WrongValueException("Invalid Directory Name. Directory name less than 250 character");

		if (dirName.contains(DMSConstant.FILE_SEPARATOR))
			throw new WrongValueException("Invalid Directory Name.");

		try
		{
			File rootFolder = new File(fileStorageProvider.getBaseDirectory(contentMngr.getPath(content)));
			if (!rootFolder.exists())
				rootFolder.mkdirs();

			File files[] = rootFolder.listFiles();

			File newFile = new File(rootFolder.getPath() + DMSConstant.FILE_SEPARATOR + dirName);

			for (int i = 0; i < files.length; i++)
			{
				if (newFile.getName().equalsIgnoreCase(files[i].getName()))
				{
					if (errorIfDirExists)
					{
						throw new AdempiereException(Msg.getMsg(Env.getCtx(),
								"Directory already exists. \n (Either same file name content exist in inActive mode)"));
					}
					else
					{
						// Get directory content ID
						List<Object> params = new ArrayList<Object>();
						params.add(Env.getAD_Client_ID(Env.getCtx()));
						params.add(AD_Table_ID);
						params.add(Record_ID);
						params.add(dirName);

						String sql = "SELECT dc.DMS_Content_ID FROM DMS_Content dc "
								+ "INNER JOIN DMS_Association da ON (dc.DMS_Content_ID = da.DMS_Content_ID) "
								+ "WHERE dc.IsActive = 'Y' AND dc.ContentBaseType = 'DIR' AND da.AD_Client_ID = ? AND COALESCE(da.AD_Table_ID, 0) = ? AND da.Record_ID = ? AND dc.Name = ? ";

						if (content == null || (content != null && Util.isEmpty(content.getParentURL(), true)))
							sql += "AND dc.ParentURL IS NULL";
						else
						{
							sql += "AND dc.ParentURL = ? ";
							params.add(contentMngr.getPath(content));
						}

						contentID = DB.getSQLValueEx(trxName, sql, params);

						break;
					}
				}
			}

			if (!newFile.exists())
			{
				if (!newFile.mkdir())
					throw new AdempiereException(Msg.getMsg(Env.getCtx(), "Invalid Directory Name."));
			}

			if (contentID <= 0)
			{
				contentID = Utils.createDMSContent(dirName, MDMSContent.CONTENTBASETYPE_Directory, contentMngr.getPath(content), false);
				Utils.createAssociation(contentID, (content != null) ? content.getDMS_Content_ID() : 0, Record_ID, AD_Table_ID, 0, 0, trxName);
			}

			content = new MDMSContent(Env.getCtx(), contentID, trxName);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Directory is not created", e);
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "Directory is not created"), e);
		}

		return content;
	} // createDirectory

	/**
	 * Create new ASI with attribute values
	 * 
	 * @param asiMap - Map of Key & Value of the attribute set
	 * @param attributeSet_ID
	 * @param trxName
	 * @return ASI_ID
	 */
	public static int createASI(Map<String, String> asiMap, int attributeSet_ID, String trxName)
	{
		int asiID = 0;

		if (asiMap != null && asiMap.size() > 0)
		{
			MAttributeSetInstance asi = new MAttributeSetInstance(Env.getCtx(), 0, attributeSet_ID, trxName);
			asi.saveEx();

			asiID = asi.getM_AttributeSetInstance_ID();
			MAttributeSet attrSet = new MAttributeSet(Env.getCtx(), attributeSet_ID, trxName);
			MAttribute[] attrs = attrSet.getMAttributes(false);

			for (int i = 0; i < attrs.length; i++)
			{
				String attName = attrs[i].getName();
				for (Entry<String, String> paramAttr : asiMap.entrySet())
				{
					String key = paramAttr.getKey();
					if (attName.equalsIgnoreCase(key))
					{
						String value = asiMap.get(key);
						boolean isMandatory = attrs[i].isMandatory();

						if (isMandatory && Util.isEmpty(value, true))
							throw new WrongValueException("Fill Mandatory Attribute:" + key);

						if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attrs[i].getAttributeValueType()))
						{
							MAttributeValue atVal = (value != null && Integer.valueOf(value) > 0) ? new MAttributeValue(Env.getCtx(), Integer.valueOf(value),
									trxName) : null;
							if (value != null)
								attrs[i].setMAttributeInstance(asiID, atVal);
						}
						else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attrs[i].getAttributeValueType()))
						{
							BigDecimal bd = new BigDecimal(value);
							if (bd != null && bd.scale() == 0)
								bd = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
							if (bd != null)
								attrs[i].setMAttributeInstance(asiID, bd);
						}
						else if (MAttribute.ATTRIBUTEVALUETYPE_Reference.equals(attrs[i].getAttributeValueType()))
						{
							int displayType = attrs[i].getAD_Reference_ID();
							switch (displayType)
							{
								case DisplayType.YesNo:
									attrs[i].setMAttributeInstance(asiID, value);
									break;

								case DisplayType.Date:
								case DisplayType.DateTime:
								case DisplayType.Time:

									// TODO String_to_timestamp
									Timestamp valueTimeStamp = new Timestamp(111); // value;
									attrs[i].setMAttributeInstance(asiID, valueTimeStamp);
									break;

								case DisplayType.Integer:
									attrs[i].setMAttributeInstance(asiID, value == null ? 0 : Integer.parseInt(value), value);
									break;

								case DisplayType.Amount:
								case DisplayType.Number:
								case DisplayType.CostPrice:
								case DisplayType.Quantity:
									attrs[i].setMAttributeInstance(asiID, new BigDecimal(value));
									break;

								case DisplayType.Image:
								case DisplayType.Assignment:
								case DisplayType.Locator:
								case DisplayType.Payment:
								case DisplayType.TableDir:
								case DisplayType.Table:
								case DisplayType.Search:
								case DisplayType.Account:

									if (!Util.isEmpty(value, true) && Integer.parseInt(value) > 0)
										attrs[i].setMAttributeInstance(asiID, Integer.parseInt(value), value);
									break;

								default:
									if (!Util.isEmpty(value))
										attrs[i].setMAttributeInstance(asiID, value);
							}
						}
						else
						{
							if (!Util.isEmpty(value))
								attrs[i].setMAttributeInstance(asiID, value);
						}
					}
				}
			}
		}

		return asiID;
	} // createASI

	/**
	 * @param dms
	 * @param parentContent
	 * @param file
	 * @param AD_Table_ID
	 * @param Record_ID
	 * @param trxName
	 * @return TRUE is success
	 */
	public static boolean addFile(DMS dms, MDMSContent parentContent, File file, int AD_Table_ID, int Record_ID, String trxName)
	{
		if (file == null)
			throw new AdempiereException(DMSConstant.MSG_FILL_MANDATORY + " file");

		// Create Content
		int contentID = Utils.createDMSContent(file.getName(), file.getName(), MDMSContent.CONTENTBASETYPE_Content,
				dms.getContentManager().getPath(parentContent), null, file, 0, 0, false, trxName);

		MDMSContent content = new MDMSContent(Env.getCtx(), contentID, trxName);

		int contentRelatedID = 0;
		if (parentContent != null)
			contentRelatedID = parentContent.getDMS_Content_ID();

		// Create Association
		Utils.createAssociation(contentID, contentRelatedID, Record_ID, AD_Table_ID, MDMSAssociationType.getVersionType(true), 0, trxName);

		// File write on Storage provider and create thumbnail
		writeFileOnStorageAndThumnail(dms, file, content);

		return true;
	} // addFile

	/**
	 * File write on Storage provider and create thumbnail
	 * 
	 * @param dms
	 * @param file
	 * @param content
	 */
	public static void writeFileOnStorageAndThumnail(DMS dms, File file, MDMSContent content)
	{
		byte[] data = null;
		try
		{
			data = Files.readAllBytes(file.toPath());
		}
		catch (IOException e)
		{
			throw new AdempiereException("Error while reading file", e);
		}

		IFileStorageProvider fsProvider = dms.getFileStorageProvider();
		IContentManager contentMngr = dms.getContentManager();

		fsProvider.writeBLOB(fsProvider.getBaseDirectory(contentMngr.getPath(content)), data, content);

		IThumbnailGenerator thumbnailGenerator = Utils.getThumbnailGenerator(dms, content.getDMS_MimeType().getMimeType());

		if (thumbnailGenerator != null)
			thumbnailGenerator.addThumbnail(content, fsProvider.getFile(contentMngr.getPath(content)), null);
	} // writeFileOnStorageAndThumnail

	/**
	 * Get Media from File
	 * 
	 * @param file
	 * @return {@link AMedia}
	 */
	public static AMedia getMediaFromFile(File file)
	{
		try
		{
			return new AMedia(file.getName(), null, null, FileUtils.readFileToByteArray(file));
		}
		catch (IOException e)
		{
			throw new AdempiereException("Issue while creating Media file.", e);
		}
	} // getMediaFromFile
}
