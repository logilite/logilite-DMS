package com.logilite.dms.uuid.util;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import com.logilite.dms.DMS;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IContentManager;
import com.logilite.dms.factories.IThumbnailGenerator;
import com.logilite.dms.model.IFileStorageProvider;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSAssociationType;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSVersion;
import com.logilite.dms.util.DMSFactoryUtils;
import com.logilite.dms.util.DMSOprUtils;
import com.logilite.dms.util.DMSSearchUtils;
import com.logilite.dms.util.Utils;
import com.logilite.dms.uuid.classes.UUIDContentManager;

/**
 * Utils for Relational UUID
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class RelationalUUIDUtils
{

	public static String getActualContentName(	IContentManager ruuCM, IFileStorageProvider storageProvider, String contentType, I_DMS_Content content,
												String fileName, String extention, String type, String operationType)
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
							actualName = newFileNameWithVersion + DMSConstant.REG_EXP_LIKE_STR + extention;
						}
						// check if file exists on actual location or not
						File newFile = storageProvider.getFile(content.getParentURL() + DMSConstant.FILE_SEPARATOR + actualName);
						if (newFile != null)
						{
							actualName = getActualContentName(ruuCM, storageProvider, contentType, content, actualName.substring(0, actualName.lastIndexOf("."))
																											+ "_1", extention, type, operationType);
						}
						break;
					}
					case MDMSAssociationType.TYPE_PARENT:
					{
						List<String> matchingFileNames = Utils.getMatchingActualNames(	ruuCM.getPathByName(content), fileName,
																						DMSConstant.REG_EXP_UNDERSCORE_LIKE_STR, extention);
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

						File newFile = storageProvider.getFile(Util.isEmpty(ruuCM.getPathByName(content), true) ? DMSConstant.FILE_SEPARATOR + actualName
																												: ruuCM.getPathByName(content)
																													+ DMSConstant.FILE_SEPARATOR + actualName);
						if (newFile != null)
						{
							actualName = getActualContentName(	ruuCM, storageProvider, contentType, content,
																actualName.substring(0, actualName.lastIndexOf(".")) + "_1", extention, type, operationType);
						}
						break;
					}
				}
			}
			else if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_COPY))
			{
				actualName = fileName;
				// No need to handle
			}
			else if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_RENAME))
			{
				switch (type)
				{
					case MDMSAssociationType.TYPE_VERSIONPARENT:
					case MDMSAssociationType.TYPE_PARENT:
					{
						List<String> matchingFileNames = Utils.getMatchingActualNames(	ruuCM.getPathByName(content), fileName,
																						DMSConstant.REG_EXP_UNDERSCORE_LIKE_STR, extention);
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
				List<String> matchingFileNames = Utils.getMatchingActualNames(	ruuCM.getPathByName(content), fileName, DMSConstant.REG_EXP_UNDERSCORE_LIKE_STR,
																				"");
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
				File rootFolder = new File(storageProvider.getBaseDirectory(ruuCM.getPathByValue(content)));
				String dirPath = rootFolder.getPath() + DMSConstant.FILE_SEPARATOR + (seq > 0 ? (fileName + "_" + seq) : fileName);
				File newDir = new File(dirPath);
				if (newDir.exists())
				{
					actualName = getActualContentName(	ruuCM, storageProvider, DMSConstant.CONTENT_DIR, content, fileName + "_" + ++seq, "", "",
														DMSConstant.OPERATION_CREATE);
				}
				else
				{
					actualName = (seq > 0 ? (fileName + "_" + seq) : fileName);
				}
			}

			else if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_COPY))
			{
				actualName = fileName;
				// No need to handle
			}
			if (operationType.equalsIgnoreCase(DMSConstant.OPERATION_RENAME))
			{
				int DMS_Content_ID = UUIDContentManager.checkExistsDir(content.getParentURL(), fileName);
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

	public static MDMSContent createDirectory(	DMS dms, String dirContentName, MDMSContent parentContent, int AD_Table_ID, int Record_ID,
												boolean errorIfDirExists, boolean isCreateAssociation, String trxName)
	{
		int contentID = 0;

		dirContentName = dirContentName.trim();

		String error = Utils.isValidFileName(dirContentName, true);
		if (!Util.isEmpty(error))
			throw new AdempiereException(error);

		try
		{
			MDMSVersion parentVersion = (MDMSVersion) MDMSVersion.getLatestVersion(parentContent);
			String baseURL = dms.getPathFromContentManager(parentVersion);
			File rootFolder = new File(dms.getBaseDirPath(parentVersion));
			if (!rootFolder.exists())
				rootFolder.mkdirs();
			String dirName = dirContentName;
			int DMS_Content_ID = UUIDContentManager.checkExistsFileDir(baseURL, dirContentName);
			String dirPath = rootFolder.getPath() + DMSConstant.FILE_SEPARATOR + dirContentName;
			if (DMS_Content_ID > 0)
			{
				MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_Name).getPO(DMS_Content_ID, null);
				String existingDir = dms.getContentManager().getPathByName(content);
				if (existingDir.equalsIgnoreCase(dirPath) && errorIfDirExists)
				{
					File newDir = new File(dirPath);
					if (!newDir.exists())
					{
						throw new AdempiereException(Msg.getMsg(Env.getCtx(),
																"Directory already exists. \n (Either same file name content exist in inActive mode)"));
					}
				}
				else if (errorIfDirExists)
				{
					throw new AdempiereException(Msg.getMsg(Env.getCtx(),
															"Directory already exists. \n (Either same file name content exist in inActive mode)"));
				}
			}
			else
			{
				dirName = dms.getContentManager().getContentName(	dms.getFileStorageProvider(), DMSConstant.CONTENT_DIR, parentContent, dirContentName, "", "",
																	DMSConstant.OPERATION_CREATE);
			}

			// Get directory content ID
			List<Object> params = new ArrayList<Object>();
			params.add(Env.getAD_Client_ID(Env.getCtx()));
			params.add(AD_Table_ID);
			params.add(Record_ID);
			params.add(dirName);

			String sql = "SELECT dc.DMS_Content_ID FROM DMS_Content dc "
							+ "INNER JOIN DMS_Association da ON (dc.DMS_Content_ID = da.DMS_Content_ID) "
							+ "WHERE dc.IsActive = 'Y' AND dc.ContentBaseType = 'DIR' AND da.AD_Client_ID = ? AND NVL(da.AD_Table_ID, 0) = ? AND da.Record_ID = ? AND dc.Name = ? ";

			if (parentContent == null)
				sql += "AND dc.ParentURL IS NULL";
			else
			{
				sql += "AND dc.ParentURL = ? ";
				params.add(dms.getPathFromContentManager(parentVersion));
			}

			contentID = DB.getSQLValueEx(trxName, sql, params);

			if (contentID <= 0)
			{
				contentID = MDMSContent.create(	dirContentName, MDMSContent.CONTENTBASETYPE_Directory, dms.getPathFromContentManager(parentVersion), false,
												trxName);
				if (isCreateAssociation)
					MDMSAssociation.create(	contentID, (parentContent != null) ? parentContent.getDMS_Content_ID() : 0, Record_ID, AD_Table_ID,
											MDMSAssociationType.PARENT_ID, trxName);

				// Create DMS Version
				MDMSVersion version = UtilsUUID.createVersionUU(contentID, 0, null, trxName);
				File newFile = new File(rootFolder.getPath() + DMSConstant.FILE_SEPARATOR + version.getDMS_Version_UU());
				// File newFile = new File(dirName);
				if (!newFile.exists())
				{
					if (!newFile.mkdir())
						throw new AdempiereException(	"Something went wrong!\nDirectory is not created:\n'"
														+ dms.getPathFromContentManager(parentVersion)
														+ DMSConstant.FILE_SEPARATOR + newFile.getName() + "'"); // TODO
				}

			}

			parentContent = new MDMSContent(Env.getCtx(), contentID, trxName);

		}
		catch (Exception e)
		{
			throw new AdempiereException(e.getLocalizedMessage(), e);
		}

		return parentContent;
	} // createDirectory

	public static String getCopyDirContentName(MDMSContent content, String dirName, String contentPath)
	{
		String actualName = dirName;
		int DMS_Conent_ID = UUIDContentManager.checkExistsFileDir(contentPath, dirName);
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
		dms_content_id = UUIDContentManager.checkExistsFileDir(ParentURL, name);
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

	/**
	 * Paste copied folder into physical storage
	 * 
	 * @param  dms
	 * @param  copiedContent    - Copied Content
	 * @param  destPasteContent - Paste Content Destination
	 * @param  actualNameUU     - actual dir name UUID
	 * @return                  FileName
	 */
	public static String pastePhysicalCopiedFolder(DMS dms, MDMSContent copiedContent, MDMSContent destPasteContent, String actualNameUU)
	{
		String newFileName = dms.getBaseDirPath(destPasteContent);

		File newFile = new File(newFileName + DMSConstant.FILE_SEPARATOR + actualNameUU);

		if (newFile.exists())
			return actualNameUU;
		if (!newFile.mkdir())
			throw new AdempiereException(	"Something went wrong!\nDirectory is not created:\n'"	+ dms.getPathFromContentManager(destPasteContent)
											+ DMSConstant.FILE_SEPARATOR + actualNameUU + "'");

		return actualNameUU;
	} // pastePhysicalCopiedFolder

	/**
	 * Paste the Copy Directory Content
	 * 
	 * @param dms                               - DMS
	 * @param copiedContent                     - Content From
	 * @param destPasteContent                  - Content To
	 * @param baseURL                           - Base URL
	 * @param renamedURL                        - Renamed URL
	 * @param tableID                           - AD_Table_ID
	 * @param recordID                          - Record_ID
	 * @param isCreatePermissionForPasteContent - create permission for paste content from parent if true
	 */
	public static void pasteCopyDirContent(	DMS dms, MDMSContent copiedContent, MDMSContent destPasteContent, String baseURL, String renamedURL, int tableID,
											int recordID, boolean isCreatePermissionForPasteContent)
	{
		HashMap<I_DMS_Version, I_DMS_Association> map = dms.getDMSContentsWithAssociation(copiedContent, dms.AD_Client_ID, true);
		for (Entry<I_DMS_Version, I_DMS_Association> mapEntry : map.entrySet())
		{
			MDMSContent oldDMSContent = (MDMSContent) ((MDMSVersion) mapEntry.getKey()).getDMS_Content();
			MDMSAssociation oldDMSAssociation = (MDMSAssociation) mapEntry.getValue();
			if (oldDMSContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
			{
				String trxName = Trx.createTrxName("DMSPasteCopyDir_");
				Trx trx = Trx.get(trxName, true);

				//
				MDMSContent newDMSContent = dms.createDirectory(oldDMSContent.getName(), destPasteContent, dms.getSubstituteTableInfo().getOriginTable_ID(),
																dms.getSubstituteTableInfo().getOriginRecord_ID(), true, true, trx.getTrxName());
				if (copiedContent.getM_AttributeSetInstance_ID() > 0)
				{
					MAttributeSetInstance newASI = Utils.copyASI(copiedContent.getM_AttributeSetInstance_ID(), trx.getTrxName());
					newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
				}

				// Note: Must save association first other wise creating issue of wrong info in solr
				// indexing entry
				newDMSContent.saveEx();

				//
				trx.commit();
				trx.close();

				if (!Util.isEmpty(oldDMSContent.getParentURL()))
				{
					if (oldDMSContent.getParentURL().startsWith(baseURL))
					{
						newDMSContent.setParentURL(dms.getPathFromContentManager(destPasteContent));
						newDMSContent.saveEx();
					}
					pasteCopyDirContent(dms, oldDMSContent, newDMSContent, baseURL, renamedURL, tableID, recordID, isCreatePermissionForPasteContent);
				}
			}
			else if (MDMSAssociationType.isLink(oldDMSAssociation))
			{
				dms.createAssociation(	oldDMSAssociation.getDMS_Content_ID(), destPasteContent.getDMS_Content_ID(),
										dms.getSubstituteTableInfo().getOriginRecord_ID(), dms.getSubstituteTableInfo().getOriginTable_ID(),
										MDMSAssociationType.LINK_ID, null);
			}
			else
			{
				pasteCopyFileContent(dms, oldDMSContent, destPasteContent, tableID, recordID, isCreatePermissionForPasteContent);
			}
		}
	} // pasteCopyDirContent

	/**
	 * Paste copy file content
	 * 
	 * @param dms
	 * @param copiedContent                     - Copied File Content
	 * @param destContent                       - Destination Content
	 * @param tableID                           - AD_Table_ID
	 * @param recordID                          - Record_ID
	 * @param isCreatePermissionForPasteContent - create permission for paste content from parent if true
	 */
	public static void pasteCopyFileContent(DMS dms, MDMSContent copiedContent, MDMSContent destContent, int tableID, int recordID,
											boolean isCreatePermissionForPasteContent)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		int DMS_Content_ID = (new MDMSContent(Env.getCtx(), copiedContent.getDMS_Content_ID(), null)).getDMS_Content_Related_ID();

		String trxName = Trx.createTrxName("DMSCopyPaste_");
		Trx trx = Trx.get(trxName, true);

		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_ASSOCIATION_FOR_COPY_PASTE, null);
			pstmt.setInt(1, DMS_Content_ID);
			pstmt.setInt(2, MDMSAssociationType.PARENT_ID);
			pstmt.setInt(3, DMS_Content_ID);
			pstmt.setInt(4, MDMSAssociationType.LINK_ID);
			rs = pstmt.executeQuery();

			while (rs.next())
			{
				copiedContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), trx.getTrxName());

				// Copy Content
				MDMSContent newDMSContent = new MDMSContent(Env.getCtx(), 0, trx.getTrxName());
				PO.copyValues(copiedContent, newDMSContent);
				if (copiedContent.getM_AttributeSetInstance_ID() > 0)
				{
					MAttributeSetInstance newASI = Utils.copyASI(copiedContent.getM_AttributeSetInstance_ID(), trx.getTrxName());
					newDMSContent.setM_AttributeSetInstance_ID(newASI.getM_AttributeSetInstance_ID());
				}
				newDMSContent.setDMS_Owner_ID(newDMSContent.getCreatedBy());
				newDMSContent.saveEx();

				// Copy Association
				MDMSAssociation oldDMSAssociation = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
				MDMSAssociation newDMSAssociation = new MDMSAssociation(Env.getCtx(), 0, trx.getTrxName());
				PO.copyValues(oldDMSAssociation, newDMSAssociation);
				newDMSAssociation.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
				if (oldDMSAssociation.getDMS_AssociationType_ID() == MDMSAssociationType.PARENT_ID)
				{
					if (destContent != null && destContent.getDMS_Content_ID() > 0)
						newDMSAssociation.setDMS_Content_Related_ID(destContent.getDMS_Content_ID());
					else
						newDMSAssociation.setDMS_Content_Related_ID(0);
				}
				else
				{
					newDMSAssociation.setDMS_Content_Related_ID(newDMSContent.getDMS_Content_ID());
				}
				newDMSAssociation.updateTableRecordRef(tableID, recordID);

				// Copy Version
				boolean isContentSaved = false;
				for (MDMSVersion oldVersion : MDMSVersion.getVersionHistory(copiedContent))
				{
					String baseURL = null;
					if (!Util.isEmpty(copiedContent.getParentURL()))
						baseURL = dms.getPathFromContentManager(oldVersion);
					else
						baseURL = DMSConstant.FILE_SEPARATOR + oldVersion.getDMS_Version_UU();

					baseURL = baseURL.substring(0, baseURL.lastIndexOf(DMSConstant.FILE_SEPARATOR));

					//
					String uuid = UUID.randomUUID().toString();
					//
					pastePhysicalCopiedContent(dms, oldVersion, destContent, uuid);
					//
					MDMSVersion newVersion = new MDMSVersion(Env.getCtx(), 0, trx.getTrxName());
					PO.copyValues(oldVersion, newVersion);
					newVersion.setDMS_Content_ID(newDMSContent.getDMS_Content_ID());
					newVersion.setDMS_Version_UU(uuid);
					newVersion.setValue(uuid);
					newVersion.saveEx();

					if (!isContentSaved)
					{
						String renamedURL = dms.getPathFromContentManager(destContent);
						String associationType = MDMSContent.getContentAssociationType(copiedContent.getDMS_Content_ID());

						String name = "";
						if (associationType.equals(MDMSAssociationType.TYPE_PARENT))
							name = getCopyFileContentName(dms.getPathFromContentManager(newVersion), copiedContent.getName(), copiedContent.getName());
						else
							name = newDMSContent.getName();

						newDMSContent.setParentURL(renamedURL);
						newDMSContent.setName(name);
						newDMSContent.saveEx();

						// Once content is saved then no need to update again for another versioning
						// iterating
						isContentSaved = true;
					}

					//if it's false then thumbnail will not be created/used otherwise it will be created/used 
					if (dms.isAllowThumbnailContentCreation())
					{
						IThumbnailGenerator thumbnailGenerator = DMSFactoryUtils.getThumbnailGenerator(dms, newDMSContent.getDMS_MimeType().getMimeType());
						if (thumbnailGenerator != null)
							thumbnailGenerator.addThumbnail(newVersion, dms.getFileFromStorage(oldVersion), null);
					}
				}

				trx.commit();

				if (isCreatePermissionForPasteContent)
				{
					dms.grantChildPermissionFromParentContent(newDMSContent, destContent, isCreatePermissionForPasteContent);
				}

			}
		}
		catch (Exception e)
		{
			trx.rollback();
			DMS.log.log(Level.SEVERE, "Error while paste Copy File Content", e);
			throw new AdempiereException(e);
		}
		finally
		{
			if (trx != null)
				trx.close();
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	} // pasteCopyFileContent

	/**
	 * Paste Physical Copied content
	 * 
	 * @param  dms
	 * @param  oldVersion       - Copied Content
	 * @param  destPasteContent - Destination Content
	 * @param  fileNameUU       - FileName UUID
	 * @return                  FileName
	 */
	public static String pastePhysicalCopiedContent(DMS dms, MDMSVersion oldVersion, MDMSContent destPasteContent, String fileNameUU)
	{
		File oldFile = new File(dms.getBaseDirPath(oldVersion));
		String actualFileName = "";
		String newFileName = dms.getBaseDirPath(destPasteContent);

		if (newFileName.charAt(newFileName.length() - 1) == DMSConstant.FILE_SEPARATOR.charAt(0))
			actualFileName += fileNameUU;
		else
			actualFileName += DMSConstant.FILE_SEPARATOR + fileNameUU;

		File newFile = new File(newFileName + DMSConstant.FILE_SEPARATOR + actualFileName);

		try
		{
			FileUtils.copyFile(oldFile, newFile);
		}
		catch (IOException e)
		{
			DMS.log.log(Level.SEVERE, "Copy Content Failure.", e);
			throw new AdempiereException("Copy Content Failure." + e.getLocalizedMessage());
		}

		return newFile.getName();
	} // pastePhysicalCopiedContent

	/**
	 * Move File
	 * 
	 * @param dms
	 * @param version
	 * @param destContent
	 */
	public static void moveFile(DMS dms, MDMSVersion version, MDMSContent destContent)
	{
		String newPath = dms.getBaseDirPath(destContent);

		newPath = newPath + DMSConstant.FILE_SEPARATOR + version.getDMS_Version_UU();

		File oldFile = new File(dms.getFileFromStorage(version).getAbsolutePath());
		File newFile = new File(newPath);

		if (!newFile.exists())
			oldFile.renameTo(newFile);
		else
			throw new AdempiereException("File is already exist.");
	} // moveFile

	/**
	 * Rename Folder
	 * 
	 * @param content
	 * @param baseURL
	 * @param renamedURL
	 * @param tableID
	 * @param recordID
	 * @param isDocExplorerWindow
	 */
	public static void renameFolder(MDMSContent content, String baseURL, String renamedURL, int tableID, int recordID, boolean isDocExplorerWindow)
	{
		HashMap<I_DMS_Version, I_DMS_Association> map = DMSSearchUtils.getDMSContentsWithAssociation(	content, content.getAD_Client_ID(),
																										DMSConstant.DOCUMENT_VIEW_ALL_VALUE);

		for (Entry<I_DMS_Version, I_DMS_Association> mapEntry : map.entrySet())
		{
			MDMSContent dmsContent = (MDMSContent) ((MDMSVersion) mapEntry.getKey()).getDMS_Content();
			MDMSAssociation associationWithContent = (MDMSAssociation) mapEntry.getValue();

			// Do not change association type is link for the content
			if (MDMSAssociationType.isLink(associationWithContent))
				continue;

			if (dmsContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
			{
				String parentURL = dmsContent.getParentURL() == null ? "" : dmsContent.getParentURL();
				if (parentURL.startsWith(baseURL))
					dmsContent.setParentURL(Utils.replacePath(baseURL, renamedURL, parentURL));
				renameFolder(dmsContent, baseURL, renamedURL, tableID, recordID, isDocExplorerWindow);

				MDMSAssociation associationDir = MDMSAssociation.getParentAssociationFromContent(dmsContent.getDMS_Content_ID(), false, null);
				associationDir.updateTableRecordRef(tableID, recordID);

				// Note: Must save association first other wise creating issue of wrong info in solr
				// indexing entry
				dmsContent.saveEx();
			}
			else
			{
				Query query = new Query(Env.getCtx(), MDMSAssociation.Table_Name, " DMS_Content_ID = ? AND DMS_AssociationType_ID <> 1000003 ", null);
				query.setClient_ID();
				query.setParameters(dmsContent.getDMS_Content_ID());
				query.setOrderBy(MDMSAssociation.COLUMNNAME_DMS_Association_ID);
				List<MDMSAssociation> associationList = query.list();

				boolean isUpdateContent = true;
				for (MDMSAssociation association : associationList)
				{
					if (isUpdateContent && dmsContent.getParentURL().startsWith(baseURL))
						dmsContent.setParentURL(Utils.replacePath(baseURL, renamedURL, dmsContent.getParentURL()));

					association.updateTableRecordRef(tableID, recordID);

					// Note: Must save association first other wise creating issue of wrong info in
					// solr indexing entry
					if (isUpdateContent)
					{
						dmsContent.saveEx();
						isUpdateContent = false;
					}
				}
			}
		}
	} // renameFolder

	public static void deleteContent(DMS dms, MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isDeleteLinkableRefs, String trxName)
	{
		if (MDMSAssociationType.isLink(dmsAssociation))
		{
			DMSOprUtils.setContentAndAssociation(null, dmsAssociation, false, trxName);
			return;
		}
		else if (dmsContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Content))
		{
			HashMap<I_DMS_Content, I_DMS_Association> relatedContentList = null;
			if (dmsAssociation.getDMS_AssociationType_ID() == MDMSAssociationType.RECORD_ID)
			{
				MDMSContent parentContent = new MDMSContent(Env.getCtx(), dmsAssociation.getDMS_Content_Related_ID(), null);
				MDMSAssociation parentAssociation = dms.getParentAssociationFromContent(parentContent.getDMS_Content_ID());
				DMSOprUtils.setContentAndAssociation(parentContent, parentAssociation, false, trxName);
				relatedContentList = DMSOprUtils.getRelatedContents(parentContent, true, trxName);
			}
			else
			{
				DMSOprUtils.setContentAndAssociation(dmsContent, dmsAssociation, false, trxName);
				relatedContentList = DMSOprUtils.getRelatedContents(dmsContent, true, trxName);
			}

			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : relatedContentList.entrySet())
			{
				MDMSContent content = (MDMSContent) entry.getKey();
				MDMSAssociation association = (MDMSAssociation) entry.getValue();
				DMSOprUtils.setContentAndAssociation(content, association, false, trxName);
			}
		}
		else if (dmsContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Directory))
		{
			// Inactive Directory content and its child recursively
			DMSOprUtils.setContentAndAssociation(dmsContent, dmsAssociation, false, trxName);
			HashMap<I_DMS_Content, I_DMS_Association> relatedContentList = DMSOprUtils.getRelatedContents(dmsContent, true, trxName);
			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : relatedContentList.entrySet())
			{
				MDMSContent content = (MDMSContent) entry.getKey();
				MDMSAssociation association = (MDMSAssociation) entry.getValue();
				// recursive call
				deleteContent(dms, content, association, isDeleteLinkableRefs, trxName);
			}
		}

		// Linkable association references to set as InActive
		if (isDeleteLinkableRefs)
		{
			HashMap<I_DMS_Association, I_DMS_Content> linkRefs = dms.getLinkableAssociationWithContentRelated(dmsContent);
			for (Entry<I_DMS_Association, I_DMS_Content> linkRef : linkRefs.entrySet())
			{
				MDMSAssociation linkAssociation = (MDMSAssociation) linkRef.getKey();
				linkAssociation.setIsActive(false);
				linkAssociation.save();
			}
		}
	} // deleteContent

	/**
	 * Undo Delete Content [ Do Active ]
	 * 
	 * @param dms
	 * @param dmsContent
	 * @param dmsAssociation
	 * @param isUnDeleteLinkableRefs
	 * @param trxName
	 */
	public static void undoDeleteContent(DMS dms, MDMSContent dmsContent, MDMSAssociation dmsAssociation, Boolean isUnDeleteLinkableRefs, String trxName)
	{
		if (MDMSAssociationType.isLink(dmsAssociation))
		{
			DMSOprUtils.setContentAndAssociation(null, dmsAssociation, true, trxName);
			return;
		}
		else if (dmsContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Content))
		{
			HashMap<I_DMS_Content, I_DMS_Association> relatedContentList = null;
			if (dmsAssociation.getDMS_AssociationType_ID() == MDMSAssociationType.RECORD_ID)
			{
				MDMSContent parentContent = new MDMSContent(Env.getCtx(), dmsAssociation.getDMS_Content_Related_ID(), null);
				MDMSAssociation parentAssociation = dms.getParentAssociationFromContent(parentContent.getDMS_Content_ID());
				DMSOprUtils.setContentAndAssociation(parentContent, parentAssociation, true, trxName);
				relatedContentList = DMSOprUtils.getRelatedContents(parentContent, false, trxName);
			}
			else
			{
				DMSOprUtils.setContentAndAssociation(dmsContent, dmsAssociation, true, trxName);
				relatedContentList = DMSOprUtils.getRelatedContents(dmsContent, false, trxName);
			}

			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : relatedContentList.entrySet())
			{
				MDMSContent content = (MDMSContent) entry.getKey();
				MDMSAssociation association = (MDMSAssociation) entry.getValue();
				DMSOprUtils.setContentAndAssociation(content, association, true, trxName);
			}
		}
		else if (dmsContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Directory))
		{
			// Active Directory content and its child recursively
			DMSOprUtils.setContentAndAssociation(dmsContent, dmsAssociation, true, trxName);
			HashMap<I_DMS_Content, I_DMS_Association> relatedContentList = DMSOprUtils.getRelatedContents(dmsContent, false, trxName);
			for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : relatedContentList.entrySet())
			{
				MDMSContent content = (MDMSContent) entry.getKey();
				MDMSAssociation association = (MDMSAssociation) entry.getValue();
				// recursive call
				undoDeleteContent(dms, content, association, isUnDeleteLinkableRefs, trxName);
			}
		}

		// Linkable association references to set as Active
		if (isUnDeleteLinkableRefs)
		{
			HashMap<I_DMS_Association, I_DMS_Content> linkRefs = dms.getLinkableAssociationWithContentRelated(dmsContent, false);
			for (Entry<I_DMS_Association, I_DMS_Content> linkRef : linkRefs.entrySet())
			{
				MDMSAssociation linkAssociation = (MDMSAssociation) linkRef.getKey();
				linkAssociation.setIsActive(true);
				linkAssociation.save();
			}
		}

		MDMSAssociation parentAssociation = dmsAssociation;
		while (parentAssociation != null && parentAssociation.getDMS_Content_Related_ID() > 0)
		{
			MDMSContent parentContent = new MDMSContent(Env.getCtx(), parentAssociation.getDMS_Content_Related_ID(), null);
			if (parentContent.getContentBaseType().equalsIgnoreCase(MDMSContent.CONTENTBASETYPE_Directory) && !parentContent.isActive())
			{
				List<MDMSAssociation> associationlist = dms.getAssociationFromContent(parentContent.getDMS_Content_ID(), 0, false, trxName);
				if (!associationlist.isEmpty())
				{
					parentAssociation = (MDMSAssociation) associationlist.get(0);
					DMSOprUtils.setContentAndAssociation(parentContent, parentAssociation, true, trxName);
				}
			}
			else
			{
				break;
			}
		}
	} // undoDeleteContent

	public static int checkDMSContentExists(String parentURL, String contentName, boolean isActiveOnly, boolean isCheckByContentName)
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

}
