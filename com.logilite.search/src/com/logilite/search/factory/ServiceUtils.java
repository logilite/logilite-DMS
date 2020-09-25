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

package com.logilite.search.factory;

import java.util.List;

import org.adempiere.base.Service;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClientInfo;
import org.compiere.model.MSysConfig;
import org.compiere.util.CCache;
import org.compiere.util.Env;

import com.idempiere.model.MIndexingConfig;

public class ServiceUtils
{
	static CCache<Integer, IIndexSearcher>	cache_indexSearcher					= new CCache<Integer, IIndexSearcher>("IndexSearcher", 2);

	public static final String				DMS_ALLOW_DOCUMENT_CONTENT_SEARCH	= "DMS_ALLOW_DOCUMENT_CONTENT_SEARCH";

	public static final String				FILE_CONTENT						= "fileContent";
	public static final String				DMS_CONTENT_ID						= "DMS_Content_ID";

	/**
	 * @param  AD_Client_ID
	 * @return
	 */
	public static IIndexSearcher getIndexSearcher(int AD_Client_ID)
	{
		IIndexSearcher indexSearcher = cache_indexSearcher.get(AD_Client_ID);

		if (indexSearcher != null)
		{
			return indexSearcher;
		}

		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID, null);
		int indexingConf_ID = clientInfo.get_ValueAsInt("LTX_Indexing_Conf_ID");
		MIndexingConfig indexingConfig = null;

		if (indexingConf_ID > 0)
		{
			indexingConfig = new MIndexingConfig(Env.getCtx(), indexingConf_ID, null);
		}
		else
			throw new AdempiereException("Index Server Not Found on Client Info");

		List<IIndexSearcherFactory> factories = Service.locator().list(IIndexSearcherFactory.class).getServices();

		for (IIndexSearcherFactory factory : factories)
		{
			indexSearcher = factory.getIndexSearcher(indexingConfig.getLTX_Indexing_Type());

			if (indexSearcher != null)
			{
				indexSearcher.init(indexingConfig);
				cache_indexSearcher.put(AD_Client_ID, indexSearcher);
				break;
			}
		}

		return indexSearcher;
	}

	public static boolean isAllowDocumentContentSearch()
	{
		return MSysConfig.getBooleanValue(ServiceUtils.DMS_ALLOW_DOCUMENT_CONTENT_SEARCH, false, Env.getAD_Client_ID(Env.getCtx()));
	} // isAllowDocumentContentSearch
}
