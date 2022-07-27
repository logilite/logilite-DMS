package com.logilite.dms.uuid.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.idempiere.dms.util.Utils;
import org.idempiere.model.MDMSVersion;

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
}
