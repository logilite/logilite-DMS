package org.idempiere.dms.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.model.I_DMS_Content;

public class ContentManager implements IContentManager
{
	public static String key = "Relational";
	
	public static CLogger	log	= CLogger.getCLogger(ContentManager.class);

	@Override
	public void addBLOB(I_DMS_Content content, byte[] data)
	{
		try
		{
			File file = new File(content.getParentURL() + File.separator + content.getName());
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(data);
			fos.close();
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Content blob writing failure: " + e.getLocalizedMessage());
			throw new AdempiereException("Content blob writing failure: " + e.getLocalizedMessage());
		}

	}

	@Override
	public void addFile(I_DMS_Content content, File file)
	{
		File document = new File(content.getParentURL() + File.separator + content.getName());
		FileOutputStream fos;
		try
		{
			fos = new FileOutputStream(document);
			Path path = Paths.get(file.getAbsolutePath());
			fos.write(Files.readAllBytes(path));
			fos.close();
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "File adding failure: " + e.getLocalizedMessage());
			throw new AdempiereException("File adding failure: " + e.getLocalizedMessage());
		}

	}

	@Override
	public File getFile(I_DMS_Content content)
	{
		File file = new File(content.getParentURL() + File.separator + content.getName());

		if (file.exists())
			return file;
		else
			return null;
	}

	@Override
	public byte[] getBLOB(I_DMS_Content content)
	{
		try
		{
			File file = new File(content.getParentURL() + File.separator + content.getName());
			if (file.exists())
			{
				Path path = Paths.get(file.getAbsolutePath());
				return Files.readAllBytes(path);
			}
			else
				return null;
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Content blob fetching failure: " + e.getLocalizedMessage());
			throw new AdempiereException("Content blob fetching failure: " + e.getLocalizedMessage());
		}
	}
}
