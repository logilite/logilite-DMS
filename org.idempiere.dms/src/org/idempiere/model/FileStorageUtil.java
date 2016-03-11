package org.idempiere.model;

import java.util.List;

import org.adempiere.base.Service;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.util.CCache;
import org.idempiere.dms.factories.Utils;

/**
 * @author Deepak@logilite.com
 */
public class FileStorageUtil
{
	static CCache<String, IFileStorageProvider>	s_cache	= new CCache<String, IFileStorageProvider>(
																"FileStorageProvider", 2);

	// TODO This util method should be added in MStorageProvider when it ended
	// into iDempiere core
	public static IFileStorageProvider get(Integer AD_Client_ID, Boolean isThumbStorage)
	{
		String key = AD_Client_ID + "_" + isThumbStorage;
		I_AD_StorageProvider storageProvider = null;

		IFileStorageProvider fileStorageProvider = s_cache.get(key);

		if (fileStorageProvider != null)
		{
			return fileStorageProvider;
		}

		if (isThumbStorage)
			storageProvider = Utils.getThumbnailStorageProvider(AD_Client_ID);
		else
			storageProvider = Utils.getStorageProvider(AD_Client_ID);

		if (storageProvider != null)
		{
			String method = storageProvider.getMethod();
			if (method == null)
				throw new AdempiereException("Method is not define on Storage Provider.");

			List<IFileStorageProviderFactory> factories = Service.locator().list(IFileStorageProviderFactory.class)
					.getServices();
			for (IFileStorageProviderFactory factory : factories)
			{
				fileStorageProvider = factory.get(method);
				if (fileStorageProvider != null)
				{
					fileStorageProvider.init(storageProvider);
					s_cache.put(key, fileStorageProvider);
					return fileStorageProvider;
				}
			}
		}
		return null;
	}

}
