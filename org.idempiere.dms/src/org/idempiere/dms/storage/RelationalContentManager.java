/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP * This program is free software;
 * you can redistribute it and/or modify it * under the terms version 2 of the
 * GNU General Public License as published * by the Free Software Foundation.
 * This program is distributed in the hope * that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. * See the GNU General Public License for
 * more details. * You should have received a copy of the GNU General Public
 * License along * with this program; if not, write to the Free Software
 * Foundation, Inc., * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA. *
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
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;

public class RelationalContentManager implements IContentManager
{
	public static final String	KEY						= "REL";

	public static CLogger		log						= CLogger.getCLogger(RelationalContentManager.class);

	public static final String	REG_EXP_VERSION_FILE	= "^.*\\(\\d+\\).\\w+$";

	public static final String	LIKE_STR				= "%";
	public static final String	PERIOD					= ".";
	public static final String	UNDERSCORE_LIKE_STR		= "__%";
	public static final String	UNDERSCORE_STR			= "_";

	@Override
	public String getPathByValue(I_DMS_Content content)
	{
		String path = "";

		if (content != null && content.getDMS_Content_ID() > 0)
		{
			if (!Util.isEmpty(content.getParentURL(), true))
				path = content.getParentURL() + DMSConstant.FILE_SEPARATOR + content.getValue();
			else if (!Util.isEmpty(content.getName(), true))
				path = DMSConstant.FILE_SEPARATOR + content.getValue();
		}

		return path;
	}

	@Override
	public String getContentName(IFileStorageProvider storageProvider, String contentType, MDMSContent content,
			String fileName, String extention, String type, String operationType)
	{
		// TODO Auto-generated method stub
		String contentName = getActualContentName(storageProvider, contentType, content, fileName, extention, type,
				operationType);
		return contentName;
	}

	private String getActualContentName(IFileStorageProvider storageProvider, String contentType, MDMSContent content,
			String dirName, String extention, String type, String operationType)
	{
		String actualName = null;

		if (contentType.equalsIgnoreCase(DMSConstant.CONTNET_FILE))
		{
			if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_CREATE))
			{
				switch (type)
				{
					case DMSConstant.CONTENT_TYPE_VERSION:
					{
						String previousVersionName = MDMSContent.getNameOfPreviousVersion(content.getDMS_Content_ID());
						int versionCount = 0;
						String newFileNameWithVersion = null;
						if (previousVersionName.matches(DMSConstant.REG_EXP_VERSION_FILE))
						{
							String count = previousVersionName.substring(previousVersionName.lastIndexOf("(") + 1,
									previousVersionName.lastIndexOf(")"));
							versionCount = Integer.parseInt(count);
							newFileNameWithVersion = previousVersionName.substring(0,
									previousVersionName.lastIndexOf("(")) + "(" + ++versionCount + ")";
						}
						else
						{
							newFileNameWithVersion = FilenameUtils.getBaseName(previousVersionName) + "("
									+ ++versionCount + ")";
						}
						extention = FilenameUtils.getExtension(previousVersionName);
						List<String> matchingFileNames = MDMSContent.getMatchingActualNames(content.getParentURL(),
								newFileNameWithVersion, LIKE_STR, PERIOD + extention);
						Object[] matchingFileNameArray = matchingFileNames.toArray();
						boolean match = false;
						while (!match)
						{
							match = true;
							for (Object matchingFileName : matchingFileNameArray)
							{
								if (((String) matchingFileName)
										.equalsIgnoreCase(newFileNameWithVersion + PERIOD + extention))
								{
									match = false;
									break;
								}
							}
						}
						if (match)
						{
							actualName = newFileNameWithVersion + PERIOD + extention;
						}
						else
						{
							while (!match)
							{
								match = true;
								newFileNameWithVersion = newFileNameWithVersion + "_1(1)";
								for (Object matchingFileName : matchingFileNameArray)
								{
									if (((String) matchingFileName)
											.equalsIgnoreCase(newFileNameWithVersion + LIKE_STR + extention))
									{
										match = false;
										break;
									}
								}
							}
							actualName = newFileNameWithVersion + LIKE_STR + extention;
						}

						// check if file exists on actual location or not
						File newFile = storageProvider
								.getFile(content.getParentURL() + DMSConstant.FILE_SEPARATOR + actualName);
						if (newFile != null)
						{
							actualName = getActualContentName(storageProvider, contentType, content,
									actualName.substring(0, actualName.lastIndexOf(".")) + "_1", extention, type,
									operationType);
						}
						break;
					}
					case DMSConstant.CONTENT_TYPE_PARENT:
					{
						List<String> matchingFileNames = MDMSContent.getMatchingActualNames(getURL(content), dirName,
								UNDERSCORE_LIKE_STR, extention);

						if (matchingFileNames.size() == 0)
						{
							actualName = dirName + extention;
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
									if (((String) matchingFileName)
											.equalsIgnoreCase(dirName + UNDERSCORE_STR + seq + extention))
									{
										found = false;
										break;
									}
								}
								if (found)
									break;
							}
							actualName = dirName + "_" + seq + extention;
						}
						File newFile = storageProvider
								.getFile(Util.isEmpty(getURL(content), true) ? DMSConstant.FILE_SEPARATOR + actualName
										: getURL(content) + DMSConstant.FILE_SEPARATOR + actualName);
						if (newFile != null)
						{
							actualName = getActualContentName(storageProvider, contentType, content,
									actualName.substring(0, actualName.lastIndexOf(".")) + "_1", extention, type,
									operationType);
						}
						break;
					}
				}
			}
			else if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_COPY))
			{
				String name = dirName;
				List<String> fileNames = MDMSContent.getExtistingFileNamesForCopiedFile(getURL(content),
						FilenameUtils.getBaseName(dirName), FilenameUtils.getExtension(dirName), false);
				if (fileNames.size() == 0)
				{
					actualName = FilenameUtils.getBaseName(dirName) + "." + FilenameUtils.getExtension(dirName);
				}
				else if (fileNames.size() == 1)
				{
					actualName = FilenameUtils.getBaseName(dirName) + " - copy" + "."
							+ FilenameUtils.getExtension(dirName);
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
						actualName = FilenameUtils.getBaseName(name) + " - copy" + "(" + seq + ")" + "."
								+ FilenameUtils.getExtension(dirName);
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
					case DMSConstant.CONTENT_TYPE_VERSIONPARENT:
					{
						List<String> matchingFileNames = MDMSContent.getMatchingActualNames(getURL(content), dirName,
								UNDERSCORE_LIKE_STR, extention);

						if (matchingFileNames.size() == 0)
						{
							actualName = dirName + extention;
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
									if (((String) matchingFileName)
											.equalsIgnoreCase(dirName + UNDERSCORE_STR + seq + extention))
									{
										found = false;
										break;
									}
								}
								if (found)
									break;
							}
							actualName = dirName + "_" + seq + extention;
						}
						break;
					}
					case DMSConstant.CONTENT_TYPE_PARENT:
					{
						List<String> matchingFileNames = MDMSContent.getMatchingActualNames(getURL(content), dirName,
								UNDERSCORE_LIKE_STR, extention);

						if (matchingFileNames.size() == 0)
						{
							actualName = dirName + extention;
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
									if (((String) matchingFileName)
											.equalsIgnoreCase(dirName + UNDERSCORE_STR + seq + extention))
									{
										found = false;
										break;
									}
								}
								if (found)
									break;
							}
							actualName = dirName + "_" + seq + extention;
						}
						break;
					}
					case DMSConstant.CONTENT_TYPE_VERSION:
					{
						List<String> matchingFileNames = MDMSContent.getMatchingActualNames(content.getParentURL(),
								dirName, LIKE_STR, extention);
						Object[] matchingFileNameArray = matchingFileNames.toArray();
						boolean match = false;
						while (!match)
						{
							match = true;
							for (Object matchingFileName : matchingFileNameArray)
							{
								if (((String) matchingFileName).equalsIgnoreCase(dirName + extention))
								{
									match = false;
									break;
								}
							}
						}
						if (match)
						{
							actualName = dirName + extention;
						}
						else
						{
							while (!match)
							{
								match = true;
								dirName = dirName + "_1(1)";
								for (Object matchingFileName : matchingFileNameArray)
								{
									if (((String) matchingFileName).equalsIgnoreCase(dirName + LIKE_STR + extention))
									{
										match = false;
										break;
									}
								}
							}
							actualName = dirName + LIKE_STR + extention;
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
				List<String> matchingFileNames = MDMSContent.getMatchingActualNames(getURL(content), dirName,
						UNDERSCORE_LIKE_STR, "");
				if (matchingFileNames.size() == 0)
				{
					actualName = dirName;
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
							if (((String) matchingFileName).equalsIgnoreCase(dirName + UNDERSCORE_STR + seq))
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
				String dirPath = rootFolder.getPath() + DMSConstant.FILE_SEPARATOR
						+ (seq > 0 ? (dirName + "_" + seq) : dirName);
				File newDir = new File(dirPath);
				if (newDir.exists())
				{
					actualName = this.getActualContentName(storageProvider, "dir", content, dirName + "_" + ++seq, "",
							"", "create");
				}
				else
				{
					actualName = (seq > 0 ? (dirName + "_" + seq) : dirName);
				}
			}

			else if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_COPY))
			{
				int DMS_Conent_ID = MDMSContent.checkDirExists(getURL(content), dirName);
				actualName = dirName;
				if (DMS_Conent_ID > 0)
				{
					List<String> dirActualNames = MDMSContent.getActualDirNameForCopiedDir(getURL(content),
							dirName + " - copy");
					if (dirActualNames.size() == 0)
					{
						actualName = dirName + " - copy";
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
				int DMS_Content_ID = MDMSContent.checkDirExists(content.getParentURL(), dirName);
				if (DMS_Content_ID > 0)
				{
					List<String> matchingFileNames = MDMSContent.getMatchingActualNames(content.getParentURL(), dirName,
							"%", "");
					if (matchingFileNames.size() == 0)
					{
						actualName = dirName;
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
								if (((String) matchingFileName).equalsIgnoreCase(dirName + UNDERSCORE_STR + seq))
								{
									found = false;
									break;
								}
							}
							if (found)
								break;
						}
						actualName = dirName + "_" + seq;
					}
				}
				else
				{
					actualName = dirName;
				}
			}
		}
		return actualName;
	}

	private static String getURL(MDMSContent content)
	{
		return content == null ? null
				: Util.isEmpty(content.getParentURL(), true)
						? content.getName() != null ? DMSConstant.FILE_SEPARATOR + content.getName() : null
						: content.getParentURL() + DMSConstant.FILE_SEPARATOR + content.getName();
	}

}
