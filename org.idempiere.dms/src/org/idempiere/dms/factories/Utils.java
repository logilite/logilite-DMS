package org.idempiere.dms.factories;

import java.util.List;

import org.adempiere.base.Service;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.IFileStorageProviderFactory;

/**
 * @author deepak@logilite.com
 */
public class Utils
{

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
		List<IThumbnailGeneratorFactory> factories = Service.locator().list(IThumbnailGeneratorFactory.class)
				.getServices();
		IThumbnailGenerator tGenerator = null;
		for (IThumbnailGeneratorFactory factory : factories)
		{
			tGenerator = factory.get(mimeType);
			if (tGenerator != null)
				break;
		}
		return tGenerator;
	}

	public static IFileStorageProvider getFileStorageProvider(String key)
	{
		List<IFileStorageProviderFactory> factories = Service.locator().list(IFileStorageProviderFactory.class)
				.getServices();
		IFileStorageProvider fileStorageProvider = null;

		for (IFileStorageProviderFactory factory : factories)
		{
			fileStorageProvider = factory.get(key);
			if (fileStorageProvider != null)
				break;
		}
		return fileStorageProvider;
	}

	public static IContentManager getContentManager(String key)
	{
		List<IContentManagerProvider> factories = Service.locator().list(IContentManagerProvider.class).getServices();
		IContentManager contentManager = null;

		for (IContentManagerProvider factory : factories)
		{
			contentManager = factory.get(key);

			if (contentManager != null)
				break;
		}
		return contentManager;
	}

	public static IThumbnailProvider getThumbnailProvider(int Ad_Client_ID)
	{
		List<IThumbnailProviderFactory> factories = Service.locator().list(IThumbnailProviderFactory.class)
				.getServices();
		IThumbnailProvider thumbnailProvider = null;

		for (IThumbnailProviderFactory factory : factories)
		{
			thumbnailProvider = factory.get(Ad_Client_ID);

			if (thumbnailProvider != null)
				break;
		}
		return thumbnailProvider;
	}
}
