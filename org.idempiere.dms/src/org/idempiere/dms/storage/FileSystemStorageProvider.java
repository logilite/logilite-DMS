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
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.util.Utils;
import org.idempiere.model.IFileStorageProvider;

public class FileSystemStorageProvider implements IFileStorageProvider
{
	public static CLogger		log			= CLogger.getCLogger(FileSystemStorageProvider.class);

	public I_AD_StorageProvider	provider	= null;

	public String				baseDir		= null;

	@Override
	public void init(I_AD_StorageProvider storageProvider)
	{
		provider = storageProvider;
		baseDir = storageProvider.getFolder();

		if (Util.isEmpty(baseDir))
			baseDir = "/opt/DMS_Content";

	}

	@Override
	public File[] getFiles(String parent, String pattern)
	{
		File directory = new File(baseDir + DMSConstant.FILE_SEPARATOR + parent);

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
		path = buildValidPath(path);
		File file = new File(path);

		if (file.exists())
			return file;
		else
			return null;
	}

	@Override
	public String[] list(String parent)
	{
		File[] files = new File(baseDir + DMSConstant.FILE_SEPARATOR + parent).listFiles();
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
			File file = new File(baseDir + DMSConstant.FILE_SEPARATOR + path);
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
	public boolean writeBLOB(String path, byte[] data)
	{
		File file = null;
		try
		{
			file = new File(path);

			String absolutePath = file.getAbsolutePath();
			String folderpath = absolutePath.substring(0, absolutePath.lastIndexOf(DMSConstant.FILE_SEPARATOR));

			new File(folderpath).mkdirs();

			if (file.exists())
			{
				file = new File(Utils.getUniqueFilename(file.getAbsolutePath()));
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
	} // writeBLOB

	@Override
	public String getBaseDirectory(String path)
	{
		if (!Util.isEmpty(path))
			return buildValidPath(path);
		else
			return baseDir;
	}

	private String buildValidPath(String path)
	{
		if (DMSConstant.FILE_SEPARATOR.charAt(0) == path.charAt(0) && path.charAt(0) == baseDir.charAt(baseDir.length() - 1))
			return baseDir + path.substring(1, path.length());
		else if (DMSConstant.FILE_SEPARATOR.charAt(0) == path.charAt(0) || DMSConstant.FILE_SEPARATOR.charAt(0) == baseDir.charAt(baseDir.length() - 1))
			return baseDir + path;
		else
			return baseDir + DMSConstant.FILE_SEPARATOR + path;
	}
}
