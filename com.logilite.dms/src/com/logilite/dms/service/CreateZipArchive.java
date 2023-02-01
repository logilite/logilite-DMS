package com.logilite.dms.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.tools.FileUtil;

import com.logilite.dms.DMS;
import com.logilite.dms.DMS_ZK_Util;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.model.MDMSContent;

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
			File srcFolder = new File(packageDirectory);
			File destZipFile = new File(rootDir + ".zip");

			// delete the old packages if exists having same name, before build the zip
			destZipFile.delete();

			// create the compressed file
			String includesdir = headDirName + File.separator + "**";
			DMS_ZK_Util.zipFolder(srcFolder, destZipFile, includesdir);

			// Delete root directory, No needed after zip created
			FileUtil.deleteFolderRecursive(new File(rootDir));

			/**
			 * Download zip
			 */
			DMS_ZK_Util.downloadFile(headDirName + ".zip", "", "multipart/x-mixed-replace;boundary=END", destZipFile);

			/**
			 * Delete the zip file after the download
			 */
			destZipFile.delete();
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
			//
			DMS_ZK_Util.readFileFromAndWriteToDir(packageDirectory, fileToZip);
		}
	} // buildPackStructure
}
