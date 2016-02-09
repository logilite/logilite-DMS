package org.idempiere.dms.factories;

import java.util.List;

import org.adempiere.base.Service;
import org.compiere.model.MSysConfig;
import org.compiere.util.CCache;
import org.compiere.util.Util;

/**
 * @author deepak@logilite.com
 */
public class Utils
{
	public static final String					STORAGE_PROVIDER_FILE_SEPARATOR	= "STORAGE_PROVIDER_FILE_SEPARATOR";

	static CCache<Integer, IThumbnailProvider>	cache_thumbnailProvider			= new CCache<Integer, IThumbnailProvider>(
																						"ThumbnailProvider", 2);
	static CCache<String, IThumbnailGenerator>	cache_thumbnailGenerator		= new CCache<String, IThumbnailGenerator>(
																						"ThumbnailGenerator", 2);
	static CCache<String, IContentManager>		cache_contentManager			= new CCache<String, IContentManager>(
																						"ContentManager", 2);
	static CCache<String, String>				cache_fileseparator				= new CCache<String, String>(
																						"FileSeparator", 2);

	public static IContentEditor getContentEditor(String mimeType)
	{
		List<IContentEditorFactory> factories = Service.locator().list(IContentEditorFactory.class).getServices();
		IContentEditor editor = null;
		for (IContentEditorFactory factory : factories)
		{
			editor = factory.get(mimeType);
			if (editor != null)
				break;
		}
		
		return editor;
	}

	public static IThumbnailGenerator getThumbnailGenerator(String mimeType)
	{
		IThumbnailGenerator thumbnailGenerator = cache_thumbnailGenerator.get(mimeType);

		if (thumbnailGenerator != null)
			return thumbnailGenerator;

		List<IThumbnailGeneratorFactory> factories = Service.locator().list(IThumbnailGeneratorFactory.class)
				.getServices();
		for (IThumbnailGeneratorFactory factory : factories)
		{
			thumbnailGenerator = factory.get(mimeType);
			if (thumbnailGenerator != null)
			{
				thumbnailGenerator.init();
				cache_thumbnailGenerator.put(mimeType, thumbnailGenerator);
				break;
			}
		}
		
		return thumbnailGenerator;
	}

	public static IContentManager getContentManager(String key)
	{
		IContentManager contentManager = cache_contentManager.get(key);

		if (contentManager != null)
			return contentManager;

		List<IContentManagerProvider> factories = Service.locator().list(IContentManagerProvider.class).getServices();

		for (IContentManagerProvider factory : factories)
		{
			contentManager = factory.get(key);

			if (contentManager != null)
			{
				cache_contentManager.put(key, contentManager);
				break;
			}
		}
		
		return contentManager;
	}

	public static IThumbnailProvider getThumbnailProvider(int Ad_Client_ID)
	{
		IThumbnailProvider thumbnailProvider = cache_thumbnailProvider.get(Ad_Client_ID);

		if (thumbnailProvider != null)
			return thumbnailProvider;

		List<IThumbnailProviderFactory> factories = Service.locator().list(IThumbnailProviderFactory.class)
				.getServices();

		for (IThumbnailProviderFactory factory : factories)
		{
			thumbnailProvider = factory.get(Ad_Client_ID);

			if (thumbnailProvider != null)
			{
				cache_thumbnailProvider.put(Ad_Client_ID, thumbnailProvider);
				thumbnailProvider.init();
				break;
			}
		}
		
		return thumbnailProvider;
	}

	public static String getStorageProviderFileSeparator()
	{
		String fileSeparator = cache_fileseparator.get(STORAGE_PROVIDER_FILE_SEPARATOR);

		if (!Util.isEmpty(fileSeparator, true))
			return fileSeparator;

		fileSeparator = MSysConfig.getValue(STORAGE_PROVIDER_FILE_SEPARATOR, "/");
		
		cache_fileseparator.put(STORAGE_PROVIDER_FILE_SEPARATOR, fileSeparator);
		
		return fileSeparator;
	}
}
