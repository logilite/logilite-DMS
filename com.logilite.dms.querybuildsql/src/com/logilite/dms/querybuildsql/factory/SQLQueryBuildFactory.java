package com.logilite.dms.querybuildsql.factory;

import org.compiere.util.Util;

import com.logilite.dms.factories.IIndexQueryBuildFactory;
import com.logilite.dms.factories.IIndexQueryBuilder;
import com.logilite.dms.querybuildsql.service.SQLQueryBuilder;
import com.logilite.search.factory.ServiceUtils;

/**
 * SQL Query Build Factory as Default factory
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class SQLQueryBuildFactory implements IIndexQueryBuildFactory
{

	@Override
	public IIndexQueryBuilder get(String indexingType)
	{
		/**
		 * Lowest priority
		 * Work as default if no other one configured or down
		 */
		if (Util.isEmpty(indexingType, true) || indexingType.equals(ServiceUtils.INDEXING_TYPE_SQL))
			return new SQLQueryBuilder();

		return null;
	}

}
