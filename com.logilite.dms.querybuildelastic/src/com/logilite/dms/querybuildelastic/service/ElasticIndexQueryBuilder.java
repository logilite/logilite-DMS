package com.logilite.dms.querybuildelastic.service;

import java.util.HashMap;
import java.util.List;

import com.logilite.dms.factories.IIndexQueryBuilder;
import com.logilite.dms.model.MDMSContent;

/**
 * Index Query Builder for Elastic services
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class ElasticIndexQueryBuilder implements IIndexQueryBuilder
{

	@Override
	public String buildSearchQueryFromMap(HashMap<String, List<Object>> params)
	{
		return null;
	}

	@Override
	public String appendCriteria(String query, int aD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		return null;
	}

	@Override
	public String getGenericSearchedContentQuery(String searchText, int aD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		return null;
	}

}
