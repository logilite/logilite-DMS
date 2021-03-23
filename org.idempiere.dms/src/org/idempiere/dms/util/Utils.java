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

package org.idempiere.dms.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FilenameUtils;
import org.compiere.Adempiere;
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
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSMimeType;

/**
 * @author deepak@logilite.com
 */
public class Utils
{

	static CLogger					log					= CLogger.getCLogger(Utils.class);

	static CCache<String, MImage>	cache_dirThumbnail	= new CCache<String, MImage>("DirThumbnail", 2);

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
	} // getStorageProviderFileSeparator

	/**
	 * @param  AD_Client_ID
	 * @return
	 */
	public static I_AD_StorageProvider getStorageProvider(int AD_Client_ID)
	{
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);

		if (clientInfo.get_ValueAsInt("DMS_StorageProvider_ID") != 0)
			return new MStorageProvider(Env.getCtx(), clientInfo.get_ValueAsInt("DMS_StorageProvider_ID"), null);
		else
			return null;
	} // getStorageProvider

	/**
	 * @param  AD_Client_ID
	 * @return
	 */
	public static I_AD_StorageProvider getThumbnailStorageProvider(int AD_Client_ID)
	{
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);

		if (clientInfo.get_ValueAsInt("DMS_Thumb_Storage_Provider_ID") != 0)
			return new MStorageProvider(Env.getCtx(), clientInfo.get_ValueAsInt("DMS_Thumb_Storage_Provider_ID"), null);
		else
			return null;
	} // getThumbnailStorageProvider

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
	 * convert component image toBufferedImage
	 * 
	 * @param  src
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
	} // toBufferedImage

	/**
	 * Get file extension with dot. ie. [.jpg]
	 * 
	 * @param  name
	 * @return      extension
	 */
	public static String getFileExtension(String name)
	{
		String ext = FilenameUtils.getExtension(name);
		if (Util.isEmpty(ext, true))
			ext = null;
		else
			ext = "." + ext;
		return ext;
	} // getFileExtension

	/**
	 * get thumbnail for file
	 * 
	 * @param  file
	 * @param  size
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
	} // getImageThumbnail

	/**
	 * get unique name for version history
	 * 
	 * @param  fullPath
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
			String ext = Utils.getFileExtension(document.getName());
			int n = 1;
			do
			{
				fullPath = path + fileNameWOExt + "(" + n++ + ")" + ext;
				document = new File(fullPath);
			}
			while (document.exists());
		}
		return fullPath;
	} // getUniqueFilename

	/**
	 * @deprecated          No more usage after soft delete implementation
	 * @param      fullPath
	 * @return
	 */
	public static String getCopiedUniqueFileName(String fullPath)
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
			String ext = Utils.getFileExtension(document.getName());
			int n = 1;
			do
			{
				fullPath = path + fileNameWOExt + " (" + n++ + ")" + ext;
				document = new File(fullPath);
			}
			while (document.exists());
		}

		return fullPath;
	} // getCopiedUniqueFileName

	/**
	 * @deprecated          No more usage after soft delete implementation
	 * @param      fullPath
	 * @return
	 */
	public static String getUniqueFolderName(String fullPath)
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
				fullPath = path + fileNameWOExt + " (" + n++ + ")";
				document = new File(fullPath);
			}
			while (document.exists());
		}
		return fullPath;
	} // getUniqueFolderName

	public static String replacePath(String baseURL, String renamedURL, String parentURL)
	{
		String setParentURL = null;
		if (Adempiere.getOSInfo().startsWith("Windows"))
		{
			parentURL = parentURL.replaceAll("\\\\", "\\\\\\\\");
			baseURL = baseURL.replaceAll("\\\\", "\\\\\\\\");
			renamedURL = renamedURL.replaceAll("\\\\", "\\\\\\\\");
			setParentURL = parentURL.replaceFirst(Pattern.quote(baseURL), renamedURL);
			setParentURL = setParentURL.replaceAll(Pattern.quote("\\\\"), "\\\\");
		}
		else
		{
			setParentURL = parentURL.replaceFirst(Pattern.quote(baseURL), renamedURL);
		}
		return setParentURL;
	} // replacePath

	public static String readableFileSize(long size)
	{
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "Byte", "kB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	} // readableFileSize

	/**
	 * Check file or directory name is valid
	 * 
	 * @param  fileName  - File / Directory Name
	 * @param  isDirName - Is Directory
	 * @return           Error if any
	 */
	public static String isValidFileName(String fileName, boolean isDirName)
	{
		if (Util.isEmpty(fileName, true))
			return DMSConstant.MSG_FILL_MANDATORY;

		if (isDirName && fileName.length() > DMSConstant.MAX_DIRECTORY_LENGTH)
			return "The folder name would be too long. You can shorten the folder name and try again (Maximum characters: " + DMSConstant.MAX_DIRECTORY_LENGTH
					+ " instead of " + fileName.length() + ")";
		else if (!isDirName && fileName.length() > DMSConstant.MAX_FILENAME_LENGTH)
			return "The filename would be too long. You can shorten the filename and try again (Maximum characters: "	+ DMSConstant.MAX_FILENAME_LENGTH
					+ " instead of " + fileName.length() + ")";

		if (fileName.contains(DMSConstant.FILE_SEPARATOR))
			return "Invalid Name, Due to file separator is used";

		if (!isDirName)
		{
			if (!fileName.matches(DMSConstant.REG_EXP_FILENAME))
				return "Invalid File Name.";

			if (!Utils.isFileNameEndWithNotBracket(fileName))
				return "Invalid File Name. Not supportted end with ()";
		}
		else
		{
			String invalidChars = "";
			Matcher matcher = DMSConstant.PATTERN_WINDOWS_DIRNAME_ISVALID.matcher(fileName.toUpperCase());
			while (matcher.find())
			{
				invalidChars += "\n" + matcher.group(0);
			}

			if (!Util.isEmpty(invalidChars, true))
				return "A file name can't contain as below characters" + invalidChars;
		}
		return null;
	} // isValidFileName

	public static boolean isFileNameEndWithNotBracket(String fileName)
	{
		boolean isValidName = false;
		int indexofClosingP = fileName.lastIndexOf(')');
		if (indexofClosingP > 0)
		{
			if ((indexofClosingP + 1) == fileName.length())
			{
				int indexofOpeningP = fileName.lastIndexOf('(');
				if (indexofOpeningP > 0)
					isValidName = false;
				else
					isValidName = true;
			}
			else
			{
				String s = fileName.substring(indexofClosingP + 1, fileName.length());
				if (!s.matches(DMSConstant.REG_SPACE))
					isValidName = false;
				else
					isValidName = true;
			}
		}
		else
		{
			isValidName = true;
		}

		return isValidName;
	} // isFileNameEndWithNotBracket

	/**
	 * Create new ASI with attribute values
	 * 
	 * @param  asiMap          - Map of Key & Value of the attribute set
	 * @param  attributeSet_ID
	 * @param  existingASIID
	 * @param  trxName
	 * @return                 ASI_ID
	 */
	public static int createOrUpdateASI(Map<String, String> asiMap, int existingASIID, int attributeSet_ID, String trxName)
	{
		int asiID = 0;

		if (asiMap != null && asiMap.size() > 0)
		{
			MAttributeSetInstance asi;
			if (existingASIID > 0)
			{
				asi = (MAttributeSetInstance) MTable.get(Env.getCtx(), MAttributeSetInstance.Table_ID).getPO(existingASIID, trxName);
			}
			else
			{
				asi = new MAttributeSetInstance(Env.getCtx(), 0, attributeSet_ID, trxName);
				asi.saveEx();
			}

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

						if ((isMandatory && Util.isEmpty(value, true) && existingASIID <= 0)
							|| (existingASIID > 0 && attrs[i].getMAttributeInstance(existingASIID).getValue() == null))
							throw new AdempiereException("Fill Mandatory Attribute:" + key);

						if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attrs[i].getAttributeValueType()))
						{
							MAttributeValue atVal = (value != null && Integer.valueOf(value) > 0) ? new MAttributeValue(Env.getCtx(),
																														Integer.valueOf(String.valueOf(value)),
																														trxName) : null;
							attrs[i].setMAttributeInstance(asiID, atVal);
						}
						else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attrs[i].getAttributeValueType()))
						{
							BigDecimal bd = new BigDecimal(value);
							if (bd != null && bd.scale() == 0)
								bd = bd.setScale(1, RoundingMode.HALF_UP);

							attrs[i].setMAttributeInstance(asiID, bd);
						}
						else if (MAttribute.ATTRIBUTEVALUETYPE_Date.equals(attrs[i].getAttributeValueType()))
						{
							Timestamp timestamp = null;
							try
							{
								Date parsedDate = new SimpleDateFormat(attrs[i].getDateFormat()).parse(value);
								timestamp = new Timestamp(parsedDate.getTime());
							}
							catch (ParseException e)
							{
								throw new AdempiereException("Error while parsing String to Timestamp", e);
							}
							attrs[i].setMAttributeInstance(asiID, timestamp);
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
									try
									{
										Date parsedDate = DMSConstant.SDF_WITH_TIME.parse(value);
										Timestamp timestamp = new Timestamp(parsedDate.getTime());
										attrs[i].setMAttributeInstance(asiID, timestamp);
									}
									catch (ParseException e)
									{
										throw new AdempiereException("Error while parsing String to Timestamp", e);
									}
									break;

								case DisplayType.Integer:

									attrs[i].setMAttributeInstance(asiID, value == null ? 0 : Integer.parseInt(value));
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
									attrs[i].setMAttributeInstance(asiID, new KeyNamePair(value == null ? 0 : Integer.parseInt(value), value));
									break;

								default:
									attrs[i].setMAttributeInstance(asiID, value);
							}
						}
						else
						{
							attrs[i].setMAttributeInstance(asiID, value);
						}
					}
				}
			}
		}

		return asiID;
	} // createOrUpdateASI

	/**
	 * Copy/Duplicate ASI Create
	 * 
	 * @param  asiID
	 * @param  trxName
	 * @return         {@link MAttributeSetInstance}
	 */
	public static MAttributeSetInstance copyASI(int asiID, String trxName)
	{
		MAttributeSetInstance oldASI = null;
		MAttributeSetInstance newASI = null;
		if (asiID > 0)
		{
			oldASI = new MAttributeSetInstance(Env.getCtx(), asiID, trxName);
			newASI = new MAttributeSetInstance(Env.getCtx(), 0, trxName);
			PO.copyValues(oldASI, newASI);
			newASI.saveEx();

			List<MAttributeInstance> oldAIList = new Query(Env.getCtx(), MAttributeInstance.Table_Name, "M_AttributeSetInstance_ID = ?", trxName)
																																					.setParameters(asiID)
																																					.list();
			for (MAttributeInstance oldAI : oldAIList)
			{
				MAttributeInstance newAI = new MAttributeInstance(Env.getCtx(), 0, trxName);
				PO.copyValues(oldAI, newAI);
				newAI.setM_Attribute_ID(oldAI.getM_Attribute_ID());
				newAI.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
				newAI.saveEx();
			}
		}
		return newASI;
	} // copyASI

	/**
	 * DMS Mounting Base
	 * 
	 * @param  AD_Client_ID
	 * @return
	 */
	public static String getDMSMountingBase(int AD_Client_ID)
	{
		return MSysConfig.getValue(DMSConstant.DMS_MOUNTING_BASE, "Attachment", AD_Client_ID);
	}

	/**
	 * DMS Mounting Archive Base
	 * 
	 * @param  AD_Client_ID
	 * @return
	 */
	public static String getDMSMountingArchiveBase(int AD_Client_ID)
	{
		return MSysConfig.getValue(DMSConstant.DMS_MOUNTING_ARCHIVE_BASE, "Archive", AD_Client_ID);
	}

	/**
	 * Return validated file name
	 * 
	 * @param  parentContent
	 * @param  file
	 * @param  fileName
	 * @param  isVersion
	 * @return               file name
	 */
	public static String validateFileName(MDMSContent parentContent, File file, String fileName, boolean isVersion)
	{
		if (file == null)
			throw new AdempiereException("File not found.");

		if (!isVersion)
		{
			if (Util.isEmpty(fileName, true))
				fileName = file.getName();

			String errMsg = Utils.isValidFileName(fileName, false);
			if (!Util.isEmpty(errMsg, true))
				throw new AdempiereException("FileName: " + fileName + " : " + errMsg);
		}
		else
		{
			if (MDMSMimeType.getMimeTypeID(file) != parentContent.getDMS_MimeType_ID())
				throw new AdempiereException("Mime type not matched, please upload same mime type version document.");
		}
		return fileName;
	} // validateFileName

	public static String getCopyDirContentName(MDMSContent content, String dirName, String contentPath)
	{
		String actualName = dirName;
		int DMS_Conent_ID = Utils.checkExistsFileDir(contentPath, dirName);
		if (DMS_Conent_ID > 0)
		{
			List<String> dirNames = Utils.getMatchingDirContentNames(contentPath, dirName + " - copy");
			if (dirNames.size() == 0)
			{
				actualName = actualName + " - copy";
			}
			else
			{
				boolean match = false;
				int count = 0;
				while (!match)
				{
					match = true;
					if (count == 0)
					{
						actualName = actualName + " - copy";
						count++;
					}
					else
					{
						actualName = dirName + " - copy (" + count + ")";
						count++;
					}
					for (Object matchingFileName : dirNames)
					{
						if (((String) matchingFileName).equalsIgnoreCase(actualName))
						{
							match = false;
							break;
						}
					}
				}
			}
		}
		else
		{
			actualName = dirName;
		}
		return actualName;
	} // getCopyDirContentName

	public static String getCopyFileContentName(String ParentURL, String origName, String newName)
	{
		String name = newName;
		int dms_content_id = 0;
		dms_content_id = Utils.checkExistsFileDir(ParentURL, name);
		if (dms_content_id > 0)
		{
			if (!name.contains(" - copy"))
				name = FilenameUtils.getBaseName(name) + " - copy" + "." + FilenameUtils.getExtension(name);
			else
			{
				String fileNameWOExt = FilenameUtils.getBaseName(name);
				int count = 0;
				if (fileNameWOExt.contains("("))
				{
					String c = fileNameWOExt.substring(fileNameWOExt.lastIndexOf("(") + 1, fileNameWOExt.lastIndexOf(")"));
					count = Integer.parseInt(c);
				}
				String ext = FilenameUtils.getExtension(name);
				String baseName = FilenameUtils.getBaseName(origName);
				if (baseName.contains(" - copy"))
				{
					baseName = baseName.split(" \\(")[0];
					name = baseName + " (" + ++count + ")" + "." + ext;
				}
				else
					name = baseName + " - copy" + " (" + ++count + ")" + "." + ext;
			}
			return getCopyFileContentName(ParentURL, origName, name);
		}
		else
			return name;
	} // getCopyFileContentName

	public static int checkExistsDir(String ParentURL, String dirName)
	{
		return checkContentExists(ParentURL, dirName, false, false);
	}

	public static int checkExistsFileDir(String parentURL, String contentName)
	{
		return checkExistsFileDir(parentURL, contentName, false);
	}

	public static int checkExistsFileDir(String parentURL, String contentName, boolean isActiveOnly)
	{
		return checkContentExists(parentURL, contentName, isActiveOnly, true);
	}

	public static int checkContentExists(String parentURL, String contentName, boolean isActiveOnly, boolean isCheckByContentName)
	{
		int DMS_Content_ID = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			String sql = isCheckByContentName ? DMSConstant.SQL_GET_CONTENT_ID_BY_CONTENT_NAME : DMSConstant.SQL_GET_CONTENT_ID_BY_CONTENT_VALUE;
			sql += isActiveOnly ? " AND c.IsActive = 'Y' " : "";

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			pstmt.setString(2, contentName);
			if (!Util.isEmpty(parentURL, true))
			{
				pstmt.setString(3, parentURL);
				pstmt.setBoolean(4, true);
				pstmt.setBoolean(5, true);
			}
			else
			{
				pstmt.setString(3, "");
				pstmt.setBoolean(4, false);
				pstmt.setBoolean(5, false);
			}

			rs = pstmt.executeQuery();
			if (rs.next())
			{
				DMS_Content_ID = rs.getInt(1);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while checking the file or directory name: " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return DMS_Content_ID;
	} // checkContentExists

	public static List<String> getExtistingFileNamesFromCopiedFile(String parentURL, String fileName, String extention)
	{
		return getMatchingContentList(parentURL, fileName + "." + extention, fileName + " - copy%" + "." + extention, false);
	}

	public static List<String> getMatchingActualNames(String parentUrl, String fileName, String regExp, String extention)
	{
		return getMatchingContentList(parentUrl, fileName + extention, fileName + regExp + extention, false);
	}

	public static List<String> getActualDirNameForCopiedDir(String ParentUrl, String dirName)
	{
		return getMatchingContentList(ParentUrl, dirName, dirName + " %", false);
	}

	public static List<String> getMatchingDirContentNames(String parentUrl, String dirName)
	{
		return getMatchingContentList(parentUrl, dirName, dirName + " %", true);
	}

	/**
	 * @param  ParentUrl
	 * @param  str1                 - Name or Value
	 * @param  str2                 - Name or Value with extra characters
	 * @param  isCheckByContentName - SLQ Check with Content Name or Value
	 * @return
	 */
	private static List<String> getMatchingContentList(String ParentUrl, String str1, String str2, boolean isCheckByContentName)
	{
		List<String> actualNames = new ArrayList<String>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(isCheckByContentName ? DMSConstant.SQL_GET_MATCHING_CONTENT_BY_NAME : DMSConstant.SQL_GET_MATCHING_CONTENT_BY_VALUE,
										null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			pstmt.setString(2, str1);
			pstmt.setString(3, str2);
			if (!Util.isEmpty(ParentUrl, true))
			{
				pstmt.setString(4, ParentUrl);
				pstmt.setBoolean(5, true);
				pstmt.setBoolean(6, true);
			}
			else
			{
				pstmt.setString(4, "");
				pstmt.setBoolean(5, false);
				pstmt.setBoolean(6, false);
			}
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				actualNames.add(rs.getString(1));
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the existing file names : " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return actualNames;
	} // getMatchingContentList

}
