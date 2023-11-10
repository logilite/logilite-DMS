package com.logilite.dms.querybuildsolr.factory;

import com.logilite.dms.factories.IIndexQueryBuildFactory;
import com.logilite.dms.factories.IIndexQueryBuilder;
import com.logilite.dms.querybuildsolr.service.SolrIndexQueryBuilder;
import com.logilite.search.model.MIndexingConfig;

/**
 * Solr - index query build factory
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class SolrIndexQueryBuildFactory implements IIndexQueryBuildFactory
{

	@Override
	public IIndexQueryBuilder get(String indexingType)
	{
		if (MIndexingConfig.LTX_INDEXING_TYPE_Solr.equalsIgnoreCase(indexingType))
		{
			return new SolrIndexQueryBuilder();
		}

		return null;
	}

}
