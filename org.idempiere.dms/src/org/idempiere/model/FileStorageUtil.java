package org.idempiere.model;

import java.util.List;

import org.adempiere.base.Service;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.util.CCache;
import org.idempiere.dms.storage.DmsUtility;


/**
 * 
 * @author Deepak@logilite.com
 *
 */
public class FileStorageUtil {
	static CCache<Integer, IFileStorageProvider> s_cache = new CCache<Integer, IFileStorageProvider>("FileStorageProvider", 2);
	
	//TODO This util method should be added in MStorageProvider when it ended into iDempiere core
	public static IFileStorageProvider get(Integer AD_Client_ID){
		
		IFileStorageProvider fileStorageProvider = s_cache.get(AD_Client_ID);
		if(fileStorageProvider!=null){
			return fileStorageProvider;
		}
		I_AD_StorageProvider storageProvider = DmsUtility.getStorageProvider(AD_Client_ID);
		if (storageProvider != null)
		{
			String method = storageProvider.getMethod();
			if (method == null)
				method = "FileSystem";

			List<IFileStorageProviderFactory> factories = Service.locator().list(IFileStorageProviderFactory.class)
					.getServices();
			for (IFileStorageProviderFactory factory : factories)
			{
				fileStorageProvider = factory.get(method);
				if (fileStorageProvider != null)
				{
					fileStorageProvider.init(storageProvider);
					s_cache.put(AD_Client_ID, fileStorageProvider);
					return fileStorageProvider;
				}
			}
		}
		return null;
	}
	
	
}
