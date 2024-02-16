package com.logilite.dms.querybuildsql.factory;

import com.logilite.dms.factories.IIndexQueryBuildFactory;
import com.logilite.dms.factories.IIndexQueryBuilder;
import com.logilite.dms.querybuildsql.service.SQLQueryBuilder;

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
		return new SQLQueryBuilder();
	}

}
