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

package com.logilite.dms.util;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.Service;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Menupopup;
import org.compiere.model.MClientInfo;
import org.compiere.util.CCache;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.logilite.dms.DMS;
import com.logilite.dms.factories.IContentEditor;
import com.logilite.dms.factories.IContentEditorFactory;
import com.logilite.dms.factories.IContentManager;
import com.logilite.dms.factories.IContentManagerProvider;
import com.logilite.dms.factories.IContentTypeAccess;
import com.logilite.dms.factories.IContentTypeAccessFactory;
import com.logilite.dms.factories.IDMSExplorerContextMenuFactory;
import com.logilite.dms.factories.IDMSExplorerMenuitem;
import com.logilite.dms.factories.IDMSUploadContent;
import com.logilite.dms.factories.IDMSUploadContentFactory;
import com.logilite.dms.factories.IDMSViewer;
import com.logilite.dms.factories.IDMSViewerFactory;
import com.logilite.dms.factories.IIndexQueryBuildFactory;
import com.logilite.dms.factories.IIndexQueryBuilder;
import com.logilite.dms.factories.IMountingFactory;
import com.logilite.dms.factories.IMountingStrategy;
import com.logilite.dms.factories.IPermissionFactory;
import com.logilite.dms.factories.IPermissionManager;
import com.logilite.dms.factories.IThumbnailGenerator;
import com.logilite.dms.factories.IThumbnailGeneratorFactory;
import com.logilite.dms.factories.IThumbnailProvider;
import com.logilite.dms.factories.IThumbnailProviderFactory;
import com.logilite.dms.model.MDMSContent;
import com.logilite.search.factory.IIndexSearcher;

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
	static CCache<String, IIndexQueryBuilder>	cache_idxQueryBuilder		= new CCache<String, IIndexQueryBuilder>("IndexQueryBuilder", 2);

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
	 * @param  contentManagerType
	 * @return                    {@link IContentManager}
	 */
	public static IContentManager getContentManager(int AD_Client_ID)
	{
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);
		String contentManagerType = clientInfo.get_ValueAsString("DMS_ContentManagerType");

		if (Util.isEmpty(contentManagerType))
			throw new AdempiereException("Content Manager is not defined");

		IContentManager contentManager = cache_contentManager.get(AD_Client_ID);
		if (contentManager != null)
		{
			if (contentManagerType.equals(contentManager.getKey()))
				return contentManager;
			else
				cache_contentManager.remove(AD_Client_ID);
		}

		//
		List<IContentManagerProvider> factories = Service.locator().list(IContentManagerProvider.class).getServices();
		for (IContentManagerProvider factory : factories)
		{
			contentManager = factory.get(contentManagerType);
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
	 * @param  contentManagerType
	 * @param  Table_Name
	 * @return                    {@link IMountingStrategy}
	 */
	public static IMountingStrategy getMountingStrategy(String contentManagerType, String Table_Name)
	{
		IMountingStrategy mounting = null;
		List<IMountingFactory> factories = Service.locator().list(IMountingFactory.class).getServices();

		for (IMountingFactory factory : factories)
		{
			mounting = factory.getMountingStrategy(contentManagerType, Table_Name);

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

	/**
	 * Factory call for DMS Permission
	 * 
	 * @return {@link IPermissionManager}
	 */
	public static IPermissionManager getPermissionFactory()
	{
		List<IPermissionFactory> factoryies = Service.locator().list(IPermissionFactory.class).getServices();
		if (factoryies != null)
		{
			for (IPermissionFactory factory : factoryies)
			{
				IPermissionManager permission = factory.getPermissionManager();
				if (permission != null)
				{
					return permission;
				}
			}
		}
		return null;
	} // getPermissionFactory

	/**
	 * Factory call for DMS Upload content form
	 * 
	 * @param  dms
	 * @param  content
	 * @param  isVersion
	 * @param  tableID
	 * @param  recordID
	 * @param  windowNo
	 * @param  tabNo
	 * @return           {@link IDMSUploadContent}
	 */
	public static IDMSUploadContent getUploadContenFactory(DMS dms, MDMSContent content, boolean isVersion, int tableID, int recordID, int windowNo, int tabNo)
	{
		List<IDMSUploadContentFactory> factoryies = Service.locator().list(IDMSUploadContentFactory.class).getServices();
		if (factoryies != null)
		{
			for (IDMSUploadContentFactory factory : factoryies)
			{
				IDMSUploadContent uploadContent = factory.getUploadForm(dms, content, isVersion, tableID, recordID, windowNo, tabNo);

				if (uploadContent != null)
				{
					return uploadContent;
				}
			}
		}
		return null;
	} // getUploadContenFactory

	/**
	 * Get Index Query Builder
	 * 
	 * @param  AD_Client_ID
	 * @param  indexSearcher
	 * @return               {@link IIndexQueryBuilder}
	 */
	public static IIndexQueryBuilder getIndexQueryBuilder(IIndexSearcher indexSearcher)
	{
		String key = indexSearcher.getIndexingType();
		IIndexQueryBuilder idxQueryBuilder = cache_idxQueryBuilder.get(key);
		if (idxQueryBuilder != null)
		{
			return idxQueryBuilder;
		}

		// Factory call
		List<IIndexQueryBuildFactory> factories = Service.locator().list(IIndexQueryBuildFactory.class).getServices();

		for (IIndexQueryBuildFactory factory : factories)
		{
			idxQueryBuilder = factory.get(indexSearcher.getIndexingType());
			if (idxQueryBuilder != null)
			{
				cache_idxQueryBuilder.put(key, idxQueryBuilder);
				break;
			}
		}

		return idxQueryBuilder;
	} // getIndexQueryBuilder

	/**
	 * Factory call for Add DMS Explorer Customized Context Menu popup items
	 * 
	 * @param  dms         DMS
	 * @param  currContent Current Selected Content
	 * @param  copyContent Copied Content
	 * @param  menupopup   Menu Popup object
	 * @return
	 */
	public static ArrayList<IDMSExplorerMenuitem> addDMSCustomCtxMenu(Menupopup menupopup)
	{
		// Factory call
		ArrayList<IDMSExplorerMenuitem> list = new ArrayList<>();
		List<IDMSExplorerContextMenuFactory> factories = Service.locator().list(IDMSExplorerContextMenuFactory.class).getServices();

		for (IDMSExplorerContextMenuFactory factory : factories)
		{
			List<IDMSExplorerMenuitem> items = factory.addMenuitems(menupopup);
			if (items != null)
				list.addAll(items);
		}
		return list;
	} // addDMSCustomCtxMenu
}
