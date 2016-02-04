package org.idempiere.dms.storage;

import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.IFileStorageProviderFactory;

public class DefaultFileStorageProviderFactory implements IFileStorageProviderFactory
{

	@Override
	public IFileStorageProvider get(String type)
	{
		if (type.equalsIgnoreCase("FileSystem"))
		{
			return new FileSystemStorageProvider();
		}
		return null;
	}
}
