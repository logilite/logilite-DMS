package org.idempiere.dms.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Zip;
import org.compiere.tools.FileUtil;
import org.idempiere.dms.DMS;
import org.idempiere.dms.DMS_ZK_Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSContent;
import org.zkoss.io.Files;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;

/**
 * Create Zip file to download multiple contents
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 * @since  24 Sep 2021
 */
public class CreateZipArchive
{

	private DMS					dms;
	private Set<I_DMS_Version>	downloadSet;
	private MDMSContent			currDMSContent;

	/**
	 * Constructor
	 * 
	 * @param dms
	 * @param currDMSContent
	 * @param downloadVersions
	 * @param documentView     [ Not implemented ]
	 */
	public CreateZipArchive(DMS dms, MDMSContent currDMSContent, Set<I_DMS_Version> downloadVersions, String documentView)
	{
		this.dms = dms;
		this.currDMSContent = currDMSContent;
		this.downloadSet = downloadVersions;
	}

	/**
	 * Download zip file
	 * 
	 * @implSpec                       Its create directory structure in temporary location of the
	 *                                 OS.
	 *                                 <br/>
	 *                                 Directory path: %temp%/DMS-Download/
	 * @throws   IOException
	 * @throws   FileNotFoundException
	 */
	public void downloadZip() throws FileNotFoundException, IOException
	{
		I_DMS_Version[] versions = downloadSet.toArray(new I_DMS_Version[downloadSet.size()]);
		if (versions.length > 1 || MDMSContent.CONTENTBASETYPE_Directory.equals(versions[0].getDMS_Content().getContentBaseType()))
		{
			/**
			 * Prepare root directory in temporary file location
			 */
			String date = DMSConstant.SDF_NO_SPACE.format(new java.util.Date());
			String headDirName = (currDMSContent == null || currDMSContent.get_ID() <= 0 ? "" : currDMSContent.getName() + "_") + date;

			String packageDirectory = System.getProperty("java.io.tmpdir");
			if (!packageDirectory.endsWith("/") && !packageDirectory.endsWith("\\"))
				packageDirectory += File.separator;

			packageDirectory = packageDirectory + "DMS-Download";
			String rootDir = packageDirectory + File.separator + headDirName;

			// create root folder
			File rootDirFile = new File(rootDir);
			if (!rootDirFile.exists())
			{
				boolean success = rootDirFile.mkdirs();
				if (!success)
				{
					System.err.println("Failed to create target directory. " + rootDir);
					return;
				}
			}

			/**
			 * Build hierarchy by recursively
			 */
			for (int i = 0; i < versions.length; i++)
			{
				buildPackStructure(versions[i], rootDirFile);
			}

			/**
			 * Create zip file
			 */
			File srcFolder = new File(rootDir);
			File destZipFile = new File(rootDir + ".zip");

			// delete the old packages if exists
			destZipFile.delete();

			// create the compressed file
			String includesdir = File.separator + "**";
			zipFolder(srcFolder, destZipFile, includesdir);

			// Delete root directory, No needed after zip created
			FileUtil.deleteFolderRecursive(new File(rootDir));

			/**
			 * Download zip
			 */
			AMedia media = new AMedia(headDirName + ".zip", "", "multipart/x-mixed-replace;boundary=END", new FileInputStream(destZipFile.getAbsolutePath()));
			Filedownload.save(media);
		}
		else
		{
			DMS_ZK_Util.downloadDocument(dms, versions[0]);
		}
	} // downloadZip

	/**
	 * Build hierarchy structure for give version to specified directory path
	 * 
	 * @param  version
	 * @param  packoutDirectoryFile
	 * @throws IOException
	 */
	private void buildPackStructure(I_DMS_Version version, File packoutDirectoryFile) throws IOException
	{
		I_DMS_Content content = version.getDMS_Content();
		String packageDirectory = packoutDirectoryFile.getAbsolutePath() + File.separator + content.getName();

		if (MDMSContent.CONTENTBASETYPE_Directory.equals(content.getContentBaseType()))
		{
			File packageDictDirFile = new File(packageDirectory);
			if (!packageDictDirFile.exists())
			{
				boolean success = packageDictDirFile.mkdirs();
				if (!success)
					throw new AdempiereException("Failed to create directory. " + packageDirectory);
			}

			// Goto child content iteration and call recursive for prepare child hierarchy structure
			I_DMS_Version[] childContents = dms.selectChildContentFiltered(content);
			for (int i = 0; i < childContents.length; i++)
			{
				buildPackStructure(childContents[i], packageDictDirFile);
			}
		}
		else
		{
			File fileToZip = dms.getFileFromStorage(version);

			byte[] data = null;
			FileInputStream fis = null;
			try
			{
				fis = new FileInputStream(fileToZip);
				data = Files.readAll(fis);
			}
			catch (IOException e)
			{
				throw new AdempiereException("Error while reading file for the version: " + version.getDMS_Version_ID() + "_" + version.getValue(), e);
			}
			finally
			{
				if (fis != null)
					fis.close();
			}

			//
			writeBLOB(packageDirectory, data);
		}
	} // buildPackStructure

	/**
	 * Write data to given path
	 * 
	 * @param  directoryPath - Path to write byte data
	 * @param  data          - Data of the file
	 * @return               True if successfully write else throws error
	 */
	public boolean writeBLOB(String directoryPath, byte[] data)
	{
		FileOutputStream fos = null;
		try
		{
			File file = new File(directoryPath);

			String absolutePath = file.getAbsolutePath();
			String folderpath = absolutePath.substring(0, absolutePath.lastIndexOf(DMSConstant.FILE_SEPARATOR));

			new File(folderpath).mkdirs();

			if (file.exists())
			{
				file = new File(absolutePath);
			}

			fos = new FileOutputStream(file, true);
			fos.write(data);

			return true;
		}
		catch (Exception e)
		{
			throw new AdempiereException("Blob writing failure for directory path: " + directoryPath + ", Error: " + e.getLocalizedMessage());
		}
		finally
		{
			if (fos != null)
				try
				{
					fos.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
		}
	} // writeBLOB

	/**
	 * Zip the srcFolder into the destFileZipFile. All the folder subtree of the src folder is added
	 * to the destZipFile archive.
	 *
	 * @param srcFolder   File, the path of the srcFolder
	 * @param destZipFile File, the path of the destination zipFile. This file will be created or
	 *                    erased.
	 * @param includesdir
	 */
	public static void zipFolder(File srcFolder, File destZipFile, String includesdir)
	{
		Zip zipper = new Zip();
		zipper.setDestFile(destZipFile);
		zipper.setBasedir(srcFolder);
		zipper.setIncludes(includesdir.replace(" ", "*"));
		zipper.setUpdate(true);
		zipper.setCompress(true);
		zipper.setCaseSensitive(false);
		zipper.setFilesonly(false);
		zipper.setTaskName("zip");
		zipper.setTaskType("zip");
		zipper.setProject(new Project());
		zipper.setOwningTarget(new Target());
		zipper.execute();

		System.out.println(destZipFile);
	} // zipFolder

}
