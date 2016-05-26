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
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;

public class FileSystemStorageProvider implements IFileStorageProvider
{
	public static CLogger		log				= CLogger.getCLogger(FileSystemStorageProvider.class);

	public I_AD_StorageProvider	provider		= null;

	public String				baseDir			= null;
	private String				fileSeparator	= null;

	@Override
	public void init(I_AD_StorageProvider storageProvider)
	{
		provider = storageProvider;
		baseDir = storageProvider.getFolder();

		if (Util.isEmpty(baseDir))
			baseDir = "/opt/DMS_Content";

		fileSeparator = Utils.getStorageProviderFileSeparator();
	}

	@Override
	public File[] getFiles(String parent, String pattern)
	{
		File directory = new File(baseDir + fileSeparator + parent);

		if (directory.exists())
		{
			FileFilter filter = new RegexFileFilter(pattern);
			return directory.listFiles(filter);
		}
		else
			return null;
	}

	@Override
	public File getFile(String path)
	{
		File file = new File(baseDir + fileSeparator + path);

		if (file.exists())
			return file;
		else
			return null;
	}

	@Override
	public String[] list(String parent)
	{
		File[] files = new File(baseDir + fileSeparator + parent).listFiles();
		String[] fileList = new String[files.length];

		for (int i = 0; i < files.length; i++)
		{
			fileList[i] = files[i].getName();
		}

		return fileList;
	}

	@Override
	public byte[] getBLOB(String path)
	{
		try
		{
			File file = new File(baseDir + fileSeparator + path);
			if (file.exists())
			{
				Path filePath = Paths.get(file.getAbsolutePath());
				return Files.readAllBytes(filePath);
			}
			else
				return null;
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Blob getting error occure: " + e.getLocalizedMessage());
			throw new AdempiereException("Blob Reading Failure: " + e.getLocalizedMessage());
		}
	}

	@Override
	public boolean writeBLOB(String path, byte[] data, I_DMS_Content DMS_Content)
	{
		File file = null;
		try
		{
			file = new File(path);

			String absolutePath = file.getAbsolutePath();
			String folderpath = absolutePath.substring(0, absolutePath.lastIndexOf(fileSeparator));

			new File(folderpath).mkdirs();

			if (file.exists())
			{
				file = new File(Utils.getUniqueFilename(file.getAbsolutePath()));
				MDMSContent revisedDMSContent = (MDMSContent) DMS_Content;
				revisedDMSContent.setName(file.getName());
				revisedDMSContent.saveEx();
			}

			FileOutputStream fos = new FileOutputStream(file, true);
			fos.write(data);
			fos.close();
			return true;
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Blob Writting Failure ", e);
			throw new AdempiereException("Blob Writting Failure: " + e.getLocalizedMessage());
		}
	}

	@Override
	public String getBaseDirectory(String path)
	{
		if (!Util.isEmpty(path))
		{
			if (path.charAt(0) == fileSeparator.charAt(0))
				return baseDir + path;
			else
				return baseDir + fileSeparator + path;
		}
		else
			return baseDir;
	}
}
