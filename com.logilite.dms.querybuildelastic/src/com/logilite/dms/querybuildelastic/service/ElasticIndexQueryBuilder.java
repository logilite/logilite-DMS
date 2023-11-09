package com.logilite.dms.querybuildelastic.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.compiere.util.Util;

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
		// TODO: Implement search query
		System.out.println(params);

		StringBuffer query = new StringBuffer();

		for (Entry<String, List<Object>> row : params.entrySet())
		{
			String key = row.getKey();
			List<Object> value = row.getValue();

			if (value.size() == 2)
			{
				if (value.get(0) instanceof String && value.get(1) instanceof Boolean && value.get(1) == Boolean.TRUE)
				{
					query.append(" , ").append(key + ":" + value.get(0) + "");
				}
				else if (value.get(0) instanceof String || value.get(1) instanceof String)
				{
					query.append(" , \"").append(key + "\":{\"gte\": \"" + value.get(0) + "\" , \"lte\": \"" + value.get(1) + "\" }");
				} // Handle condition when two boolean value passed.
				else if (value.get(0) instanceof Boolean || value.get(1) instanceof Boolean)
				{
					query.append(" , ").append(key + ":" + value.get(0) + " OR ").append(key + ":" + value.get(1) + "");
				}
				else if (value.get(1).equals("*"))
				{
					query.append(" , ").append(key + ":[\"" + value.get(0) + "\" TO " + value.get(1) + " ]");
				}
				else
				{
					query.append(" , \"").append(key + "\":{\"gte\": \"" + value.get(0) + "\" , \"lte\": \"" + value.get(1) + "\" }");
				}
			}
			else
			{
				if (value.get(0) instanceof String)
				{
					if (Util.isEmpty((String) value.get(0), true))
						query.append(" , ").append(key + ":*");
					else
						query.append(" , ").append(key + ":\"" + value.get(0) + "\"");
				}
				else
				{
					query.append(" , ").append(key + ":" + value.get(0) + "");
				}
			}
		}

		System.out.println(query);
		if (query.length() > 0)
			query.delete(0, 3);
		else
			query.append("*:*");

		return query.toString();
	}

	@Override
	public String appendCriteria(String query, int aD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		// AD_Client_id append for search client wise
		if (!Util.isEmpty(query))
			query += " , ";

		//
		query += commonSearch(aD_Client_ID, content, tableID, recordID, documentView);

		return query;
	}

	private String commonSearch(int aD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		// TODO
		return null;
	}

	@Override
	public String getGenericSearchedContentQuery(String searchText, int aD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		// TODO
		return null;
	}

}
