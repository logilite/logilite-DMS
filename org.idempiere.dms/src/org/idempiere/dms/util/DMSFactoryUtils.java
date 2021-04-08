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

package org.idempiere.dms.util;

import java.util.List;

import org.adempiere.base.Service;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClientInfo;
import org.compiere.util.CCache;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.DMS;
import org.idempiere.dms.factories.IContentEditor;
import org.idempiere.dms.factories.IContentEditorFactory;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IContentManagerProvider;
import org.idempiere.dms.factories.IContentTypeAccess;
import org.idempiere.dms.factories.IContentTypeAccessFactory;
import org.idempiere.dms.factories.IDMSViewer;
import org.idempiere.dms.factories.IDMSViewerFactory;
import org.idempiere.dms.factories.IMountingFactory;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.IPermission;
import org.idempiere.dms.factories.IPermissionFactory;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.factories.IThumbnailGeneratorFactory;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.IThumbnailProviderFactory;

/**
 * Utils for Factory get
 * 
 * @author Sachin Bhimani
 */
public class DMSFactoryUtils
{

	static CCache<Integer, IThumbnailProvider>	cache_thumbnailProvider		= new CCache<Integer, IThumbnailProvider>("ThumbnailProvider", 2);
	static CCache<String, IThumbnailGenerator>	cache_thumbnailGenerator	= new CCache<String, IThumbnailGenerator>("ThumbnailGenerator", 2);
	static CCache<Integer, IContentManager>		cache_contentManager		= new CCache<Integer, IContentManager>("ContentManager", 2);

	/**
	 * Factory - Content Editor
	 * 
	 * @param  mimeType
	 * @return          {@link IContentEditor}
	 */
	public static IContentEditor getContentEditor(String mimeType)
	{
		IContentEditor contentEditor = null;
		List<IContentEditorFactory> factories = Service.locator().list(IContentEditorFactory.class).getServices();

		for (IContentEditorFactory factory : factories)
		{
			contentEditor = factory.get(mimeType);
			if (contentEditor != null)
			{
				break;
			}
		}

		return contentEditor;
	} // getContentEditor

	/**
	 * Factory - Thumbnail Generator. Generate the thumbnail of content or
	 * directory
	 * 
	 * @param  dms
	 * @param  mimeType
	 * @return          {@link IThumbnailGenerator}
	 */
	public static IThumbnailGenerator getThumbnailGenerator(DMS dms, String mimeType)
	{
		String key = (Env.getAD_Client_ID(Env.getCtx()) + "_" + mimeType);

		IThumbnailGenerator thumbnailGenerator = cache_thumbnailGenerator.get(key);

		if (thumbnailGenerator != null)
			return thumbnailGenerator;

		List<IThumbnailGeneratorFactory> factories = Service.locator().list(IThumbnailGeneratorFactory.class).getServices();
		for (IThumbnailGeneratorFactory factory : factories)
		{
			thumbnailGenerator = factory.get(dms, mimeType);
			if (thumbnailGenerator != null)
			{
				thumbnailGenerator.init();
				cache_thumbnailGenerator.put(key, thumbnailGenerator);
				break;
			}
		}

		return thumbnailGenerator;
	} // getThumbnailGenerator

	/**
	 * Factory - Content Manager
	 * 
	 * @param  AD_Client_ID
	 * @return              {@link IContentManager}
	 */
	public static IContentManager getContentManager(int AD_Client_ID)
	{
		IContentManager contentManager = cache_contentManager.get(AD_Client_ID);

		if (contentManager != null)
			return contentManager;

		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);

		String key = clientInfo.get_ValueAsString("DMS_ContentManagerType");

		if (Util.isEmpty(key))
			throw new AdempiereException("Content Manager is not defined");

		List<IContentManagerProvider> factories = Service.locator().list(IContentManagerProvider.class).getServices();

		for (IContentManagerProvider factory : factories)
		{
			contentManager = factory.get(key);
			if (contentManager != null)
			{
				cache_contentManager.put(AD_Client_ID, contentManager);
				break;
			}
		}

		return contentManager;
	} // getContentManager

	/**
	 * Factory - Thumbnail Provider. Apply the thumbnail of content or directory
	 * 
	 * @param  dms
	 * @param  AD_Client_ID
	 * @return              {@link IThumbnailProvider}
	 */
	public static IThumbnailProvider getThumbnailProvider(int AD_Client_ID)
	{
		IThumbnailProvider thumbnailProvider = cache_thumbnailProvider.get(AD_Client_ID);

		if (thumbnailProvider != null)
			return thumbnailProvider;

		List<IThumbnailProviderFactory> factories = Service.locator().list(IThumbnailProviderFactory.class).getServices();

		for (IThumbnailProviderFactory factory : factories)
		{
			thumbnailProvider = factory.get(AD_Client_ID);

			if (thumbnailProvider != null)
			{
				cache_thumbnailProvider.put(AD_Client_ID, thumbnailProvider);
				thumbnailProvider.init();
				break;
			}
		}

		return thumbnailProvider;
	} // getThumbnailProvider

	/**
	 * Factory - Mounting Strategy
	 * 
	 * @param  Table_Name
	 * @return            {@link IMountingStrategy}
	 */
	public static IMountingStrategy getMountingStrategy(String Table_Name)
	{
		IMountingStrategy mounting = null;
		List<IMountingFactory> factories = Service.locator().list(IMountingFactory.class).getServices();

		for (IMountingFactory factory : factories)
		{
			mounting = factory.getMountingStrategy(Table_Name);

			if (mounting != null)
			{
				return mounting;
			}
		}
		return null;
	} // getMountingStrategy

	/**
	 * Factory call for DMS ContentTypeAccess
	 * 
	 * @param  toggleAction
	 * @return              {@link IContentTypeAccess}
	 */
	public static IContentTypeAccess getContentTypeAccessFactory()
	{
		List<IContentTypeAccessFactory> factories = Service.locator().list(IContentTypeAccessFactory.class).getServices();
		for (IContentTypeAccessFactory factory : factories)
		{
			return factory.get();
		}

		return null;
	} // getContentTypeAccessFactory

	/**
	 * Factory call for DMS Component Viewer
	 * 
	 * @param  toggleAction
	 * @return              {@link IDMSViewer}
	 */
	public static IDMSViewer getDMSComponentViewer(String toggleAction)
	{
		List<IDMSViewerFactory> factories = Service.locator().list(IDMSViewerFactory.class).getServices();
		for (IDMSViewerFactory factory : factories)
		{
			return factory.get(toggleAction);
		}

		return null;
	} // getDMSComponentViewer
	
	public static IPermission getContentPermissionValidator( )
	{
		List <IPermissionFactory> factoryList = Service.locator().list(IPermissionFactory.class).getServices();
		if (factoryList != null)
		{
			for (IPermissionFactory factory : factoryList)
			{
				IPermission permission = factory.getPermission();
				if (permission != null)
				{
					return permission;
				}
			}
		}
		return null;
	}

}
