package com.logilite.dms.querybuildelastic.factory;

import com.logilite.dms.factories.IIndexQueryBuildFactory;
import com.logilite.dms.factories.IIndexQueryBuilder;
import com.logilite.dms.querybuildelastic.service.ElasticIndexQueryBuilder;
import com.logilite.search.model.MIndexingConfig;

/**
 * Elastic - index query build factory
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class ElasticIndexQueryBuildFactory implements IIndexQueryBuildFactory
{

	@Override
	public IIndexQueryBuilder get(String indexingType)
	{
		if (MIndexingConfig.LTX_INDEXING_TYPE_Elastic.equalsIgnoreCase(indexingType))
		{
			return new ElasticIndexQueryBuilder();
		}

		return null;
	}

}
