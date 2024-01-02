package com.logilite.dms.querybuildsql.factory;

import com.logilite.dms.factories.IIndexQueryBuildFactory;
import com.logilite.dms.factories.IIndexQueryBuilder;
import com.logilite.dms.querybuildsql.service.SqlQueryBuilder;
import com.logilite.search.model.MIndexingConfig;

/**
 * Generic Query Build Factory
 * 
 * @author Bhautik Panchal
 */
public class SqlQueryBuildFactory implements IIndexQueryBuildFactory
{

	@Override
	public IIndexQueryBuilder get(String indexingType)
	{
		if (MIndexingConfig.LTX_INDEXING_TYPE_GENERIC.equalsIgnoreCase(indexingType))
		{
			return new SqlQueryBuilder();
		}
		return null;
	}

}
