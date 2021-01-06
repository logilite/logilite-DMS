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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.util.Utils;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSVersion;

public class RelationalContentManager implements IContentManager
{
	public static final String	KEY	= "REL";

	public static CLogger		log	= CLogger.getCLogger(RelationalContentManager.class);

	@Override
	public String getPathByValue(I_DMS_Content content)
	{
		return getPathByValue(MDMSVersion.getLatestVersion(content, false));
	} // getPathByValue

	@Override
	public String getPathByValue(I_DMS_Version version)
	{
		String path = "";

		if (version != null && version.getDMS_Content_ID() > 0)
		{
			if (!Util.isEmpty(version.getDMS_Content().getParentURL(), true))
				path = version.getDMS_Content().getParentURL() + DMSConstant.FILE_SEPARATOR + version.getValue();
			else if (!Util.isEmpty(version.getDMS_Content().getName(), true))
				path = DMSConstant.FILE_SEPARATOR + version.getValue();
		}
		return path;
	} // getPathByValue

	@Override
	public String getPathByName(I_DMS_Content content)
	{
		String path = "";

		if (content != null && content.getDMS_Content_ID() > 0)
		{
			if (!Util.isEmpty(content.getParentURL(), true))
				path = content.getParentURL() + DMSConstant.FILE_SEPARATOR + content.getName();
			else if (!Util.isEmpty(content.getName(), true))
				path = DMSConstant.FILE_SEPARATOR + content.getName();
		}
		return path;
	} // getPathByName

	@Override
	public String getContentName(	IFileStorageProvider storageProvider, String contentType, I_DMS_Content content, String fileName, String extention,
									String type, String operationType)
	{
		String contentName = getActualContentName(storageProvider, contentType, content, fileName, extention, type, operationType);
		return contentName;
	} // getContentName

	private String getActualContentName(IFileStorageProvider storageProvider, String contentType, I_DMS_Content content, String fileName, String extention,
										String type, String operationType)
	{
		String actualName = null;

		if (contentType.equalsIgnoreCase(DMSConstant.CONTENT_FILE))
		{
			if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_CREATE))
			{
				switch (type)
				{
					case MDMSAssociationType.TYPE_VERSION:
					{
						String previousVersionName = MDMSVersion.getLatestVersion(content, false).getValue();
						String newFileNameWithVersion = null;
						if (previousVersionName.matches(DMSConstant.REG_EXP_VERSION_FILE))
						{
							String count = previousVersionName.substring(previousVersionName.lastIndexOf("(") + 1, previousVersionName.lastIndexOf(")"));
							int versionCount = Integer.parseInt(count);
							newFileNameWithVersion = previousVersionName.substring(0, previousVersionName.lastIndexOf("(")) + "(" + ++versionCount + ")";
						}
						else
						{
							newFileNameWithVersion = FilenameUtils.getBaseName(previousVersionName) + "(1)";
						}
						extention = FilenameUtils.getExtension(previousVersionName);
						List<String> matchingFileNames = Utils.getMatchingActualNames(	content.getParentURL(), newFileNameWithVersion,
																						DMSConstant.REG_EXP_LIKE_STR, DMSConstant.REG_EXP_PERIOD + extention);
						Object[] matchingFileNameArray = matchingFileNames.toArray();
						boolean match = true;
						for (Object matchingFileName : matchingFileNameArray)
						{
							if (((String) matchingFileName).equalsIgnoreCase(newFileNameWithVersion + DMSConstant.REG_EXP_PERIOD + extention))
							{
								match = false;
								break;
							}
						}
						if (match)
						{
							actualName = newFileNameWithVersion + DMSConstant.REG_EXP_PERIOD + extention;
						}
						else
						{
							while (!match)
							{
								match = true;
								newFileNameWithVersion = newFileNameWithVersion + "_1(1)";
								for (Object matchingFileName : matchingFileNameArray)
								{
									if (((String) matchingFileName).equalsIgnoreCase(newFileNameWithVersion + DMSConstant.REG_EXP_LIKE_STR + extention))
									{
										match = false;
										break;
									}
								}
							}
							actualName = newFileNameWithVersion + DMSConstant.REG_EXP_LIKE_STR +
											extention;
						}
						// check if file exists on actual location or not
						File newFile = storageProvider.getFile(content.getParentURL() + DMSConstant.FILE_SEPARATOR + actualName);
						if (newFile != null)
						{
							actualName = getActualContentName(storageProvider, contentType, content, actualName.substring(0, actualName.lastIndexOf("."))
																										+ "_1", extention, type, operationType);
						}
						break;
					}
					case MDMSAssociationType.TYPE_PARENT:
					{
						List<String> matchingFileNames = Utils.getMatchingActualNames(	getPathByName(content), fileName, DMSConstant.REG_EXP_UNDERSCORE_LIKE_STR,
																						extention);
						if (matchingFileNames.size() == 0)
						{
							actualName = fileName + extention;
						}
						else
						{
							Object[] matchingFileNamesArray = matchingFileNames.toArray();
							int seq = 0;
							boolean found = true;
							while (true)
							{
								found = true;
								seq++;
								for (Object matchingFileName : matchingFileNamesArray)
								{
									if (((String) matchingFileName).equalsIgnoreCase(fileName + DMSConstant.REG_EXP_UNDERSCORE_STR + seq + extention))
									{
										found = false;
										break;
									}
								}
								if (found)
									break;
							}
							actualName = fileName + "_" + seq + extention;
						}

						File newFile = storageProvider.getFile(Util.isEmpty(getPathByName(content), true)	? DMSConstant.FILE_SEPARATOR + actualName
																											: getPathByName(content)
																												+ DMSConstant.FILE_SEPARATOR + actualName);
						if (newFile != null)
						{
							actualName = getActualContentName(storageProvider, contentType, content, actualName.substring(0, actualName.lastIndexOf("."))
																										+ "_1", extention, type, operationType);
						}
						break;
					}
				}
			}
			else if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_COPY))
			{
				String name = fileName;
				List<String> fileNames = Utils.getExtistingFileNamesFromCopiedFile(	getPathByName(content), FilenameUtils.getBaseName(fileName),
																					FilenameUtils.getExtension(fileName));
				if (fileNames.size() == 0)
				{
					actualName = FilenameUtils.getBaseName(fileName) + "." + FilenameUtils.getExtension(fileName);
				}
				else if (fileNames.size() == 1)
				{
					actualName = FilenameUtils.getBaseName(fileName) + " - copy" + "." + FilenameUtils.getExtension(fileName);
				}
				else
				{
					Object[] fileArray = fileNames.toArray();
					Arrays.sort(fileArray);
					int seq = 0;
					boolean match = false;
					while (!match)
					{
						match = true;
						seq++;
						actualName = FilenameUtils.getBaseName(name) + " - copy" + "(" + seq + ")" + "." + FilenameUtils.getExtension(fileName);
						for (Object f : fileArray)
						{
							if (((String) f).equalsIgnoreCase(actualName))
							{
								match = false;
								break;
							}
						}
					}
				}
			}
			else if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_RENAME))
			{
				switch (type)
				{
					case MDMSAssociationType.TYPE_VERSIONPARENT:
					case MDMSAssociationType.TYPE_PARENT:
					{
						List<String> matchingFileNames = Utils.getMatchingActualNames(	getPathByName(content), fileName, DMSConstant.REG_EXP_UNDERSCORE_LIKE_STR,
																						extention);
						if (matchingFileNames.size() == 0)
						{
							actualName = fileName + extention;
						}
						else
						{
							Object[] matchingFileNamesArray = matchingFileNames.toArray();
							int seq = 0;
							boolean found = true;
							while (true)
							{
								found = true;
								seq++;
								for (Object matchingFileName : matchingFileNamesArray)
								{
									if (((String) matchingFileName).equalsIgnoreCase(fileName + DMSConstant.REG_EXP_UNDERSCORE_STR + seq + extention))
									{
										found = false;
										break;
									}
								}
								if (found)
									break;
							}
							actualName = fileName + "_" + seq + extention;
						}
						break;
					}
				}
			}
		}
		else if (contentType.equalsIgnoreCase(DMSConstant.CONTENT_DIR))
		{
			if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_CREATE))
			{
				int seq = 0;
				List<String> matchingFileNames = Utils.getMatchingActualNames(getPathByName(content), fileName, DMSConstant.REG_EXP_UNDERSCORE_LIKE_STR, "");
				if (matchingFileNames.size() == 0)
				{
					actualName = fileName;
				}
				else
				{
					Object[] matchingFileNamesArray = matchingFileNames.toArray();
					boolean found = true;
					while (true)
					{
						found = true;
						seq++;
						for (Object matchingFileName : matchingFileNamesArray)
						{
							if (((String) matchingFileName).equalsIgnoreCase(fileName + DMSConstant.REG_EXP_UNDERSCORE_STR + seq))
							{
								found = false;
								break;
							}
						}
						if (found)
							break;
					}
				}
				File rootFolder = new File(storageProvider.getBaseDirectory(this.getPathByValue(content)));
				String dirPath = rootFolder.getPath() + DMSConstant.FILE_SEPARATOR + (seq > 0 ? (fileName + "_" + seq) : fileName);
				File newDir = new File(dirPath);
				if (newDir.exists())
				{
					actualName = this.getActualContentName(	storageProvider, DMSConstant.CONTENT_DIR, content, fileName + "_" + ++seq, "", "",
															DMSConstant.OPERATION_CREATE);
				}
				else
				{
					actualName = (seq > 0 ? (fileName + "_" + seq) : fileName);
				}
			}

			else if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_COPY))
			{
				int DMS_Conent_ID = Utils.checkExistsDir(getPathByName(content), fileName);
				actualName = fileName;
				if (DMS_Conent_ID > 0)
				{
					List<String> dirActualNames = Utils.getActualDirNameForCopiedDir(getPathByName(content), fileName + " - copy");
					if (dirActualNames.size() == 0)
					{
						actualName = fileName + " - copy";
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
								actualName = fileName + " - copy (" + count + ")";
								count++;
							}
							for (Object matchingFileName : dirActualNames)
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
			}
			if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_RENAME))
			{
				int DMS_Content_ID = Utils.checkExistsDir(content.getParentURL(), fileName);
				if (DMS_Content_ID > 0)
				{
					List<String> matchingFileNames = Utils.getMatchingActualNames(content.getParentURL(), fileName, "%", "");
					if (matchingFileNames.size() == 0)
					{
						actualName = fileName;
					}
					else
					{
						Object[] matchingFileNamesArray = matchingFileNames.toArray();
						int seq = 0;
						boolean found = true;
						while (true)
						{
							found = true;
							seq++;
							for (Object matchingFileName : matchingFileNamesArray)
							{
								if (((String) matchingFileName).equalsIgnoreCase(fileName + DMSConstant.REG_EXP_UNDERSCORE_STR + seq))
								{
									found = false;
									break;
								}
							}
							if (found)
								break;
						}
						actualName = fileName + "_" + seq;
					}
				}
				else
				{
					actualName = fileName;
				}
			}
		}
		return actualName;
	} // getActualContentName
}
