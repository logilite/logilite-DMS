package com.logilite.dms.uuid.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.compiere.model.MTable;
import org.compiere.util.Env;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IContentManager;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.MDMSVersion;
import com.logilite.dms.util.Utils;

/**
 * Miscellaneous Utils
 * 
 * @author Sachin Bhimani
 */
public class UtilsUUID
{

	/**
	 * Create DMS Version with UUID
	 * 
	 * @param  contentID
	 * @param  seqNo
	 * @param  file
	 * @return           {@link MDMSVersion}
	 */
	public static MDMSVersion createVersionUU(int contentID, int seqNo, File file, String trxName)
	{
		String uuid = UUID.randomUUID().toString();

		return createVersionUU(uuid, contentID, seqNo, file, trxName);
	} // createVersionUU

	/**
	 * Create DMS Version with UUID
	 * 
	 * @param  versionUUID
	 * @param  contentID
	 * @param  seqNo
	 * @param  file
	 * @return             {@link MDMSVersion}
	 */
	public static MDMSVersion createVersionUU(String versionUUID, int contentID, int seqNo, File file, String trxName)
	{
		MDMSVersion version = (MDMSVersion) MTable.get(Env.getCtx(), MDMSVersion.Table_ID).getPO(0, trxName);
		version.setDMS_Version_UU(versionUUID);
		version.setDMS_Content_ID(contentID);
		version.setValue(versionUUID);
		version.setSeqNo(seqNo);
		if (file != null)
			version.setDMS_FileSize(Utils.readableFileSize(FileUtils.sizeOf(file)));
		version.saveEx();
		return version;
	} // createVersionUU

	// Create a Zip from the given path
	public static void zipFolder(Path sourceFolderPath, Path zipPath) throws Exception
	{
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
		Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
				Files.copy(file, zos);
				zos.closeEntry();
				return FileVisitResult.CONTINUE;
			}
		});
		zos.close();
	} // zipFolder

	/**
	 * Get copy file name for Directory or File based on Content Type
	 * @param content
	 * @param ruuCM
	 * @param contentType
	 * @param actualName
	 * @param fileName
	 * @param extention
	 */
	public static String getCopyName(I_DMS_Content content, IContentManager ruuCM, boolean isDir, String actualName, String fileName, String extention)
	{
		String name = FilenameUtils.getBaseName(fileName);
		List<String> matchingFileNames;
		if (isDir)
			matchingFileNames = Utils.getActualDirNameForCopiedDirName(ruuCM.getPathByValue(content), name);
		else
			matchingFileNames = Utils.getExtistingFileNamesFromCopiedFileName(ruuCM.getPathByValue(content), name, extention);
		actualName = name;
		if (!isDir)
			actualName += "." + extention;
		if (!fileName.contains(DMSConstant.REG_EXP_UNDERSCORE_COPY) && matchingFileNames.size() == 1)
		{
			actualName = name + DMSConstant.REG_EXP_UNDERSCORE_COPY;
			if (!isDir)
				actualName += DMSConstant.REG_EXP_PERIOD + extention;
		}
		else if (matchingFileNames.size() > 1)
		{
			Object[] fileArray = matchingFileNames.toArray();
			int seq = 0;
			boolean match = false;
			while (!match)
			{
				match = true;
				seq++;
				actualName = name + DMSConstant.REG_EXP_UNDERSCORE_COPY_UNDERSCORE + seq;
				if (!isDir)
					actualName += DMSConstant.REG_EXP_PERIOD + extention;
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
		return actualName;
	} // getCopyName
}
