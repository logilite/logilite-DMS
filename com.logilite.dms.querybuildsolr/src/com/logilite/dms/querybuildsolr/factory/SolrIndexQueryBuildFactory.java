package com.logilite.dms.querybuildsolr.factory;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IIndexQueryBuildFactory;
import com.logilite.dms.factories.IIndexQueryBuilder;
import com.logilite.dms.querybuildsolr.service.SolrIndexQueryBuilder;

public class SolrIndexQueryBuildFactory implements IIndexQueryBuildFactory
{

	@Override
	public IIndexQueryBuilder get(String indexingType)
	{
		if (DMSConstant.INDEXING_TYPE_SOLR.equalsIgnoreCase(indexingType))
		{
			return new SolrIndexQueryBuilder();
		}
		else
		{
			return null;
		}
	}

}
