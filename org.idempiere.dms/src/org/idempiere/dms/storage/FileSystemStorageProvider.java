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
import org.idempiere.model.IFileStorageProvider;

public class FileSystemStorageProvider implements IFileStorageProvider
{
	public static CLogger		log	= CLogger.getCLogger(FileSystemStorageProvider.class);

	public I_AD_StorageProvider	provider;
	public String				baseDir;

	@Override
	public void init(I_AD_StorageProvider storageProvider)
	{
		provider = storageProvider;
		baseDir = storageProvider.getFolder();
	}

	@Override
	public File[] getFiles(String parent, String pattern)
	{
		File directory = new File(parent);
		FileFilter filter = new RegexFileFilter(pattern);
		return directory.listFiles(filter);
	}

	@Override
	public File getFile(String path)
	{
		File file = new File(path);

		if (file.exists())
			return file;
		else
			return null;
	}

	@Override
	public String[] list(String parent)
	{
		File[] files = new File(parent).listFiles();
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
			File file = new File(path);
			if (file.exists())
			{
				Path filePath = Paths.get(path);
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
		try
		{
			File file = new File(path);
			FileOutputStream fos = new FileOutputStream(file, true);
			fos.write(data);
			fos.close();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Blob Writting Failure " + e.getLocalizedMessage());
			throw new AdempiereException("Blob Writting Failure: " + e.getLocalizedMessage());
		}
		return true;
	}
}
