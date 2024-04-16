package com.logilite.dms.querybuildelastic.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.compiere.util.Util;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IIndexQueryBuilder;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.util.DMSSearchUtils;

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
		StringBuffer query = new StringBuffer();

		for (Entry<String, List<Object>> row : params.entrySet())
		{
			String key = row.getKey();
			List<Object> value = row.getValue();

			if (value.size() == 2)
			{
				if (value.get(0) instanceof String && value.get(1) instanceof Boolean && value.get(1) == Boolean.TRUE)
				{
					query.append(" AND \"").append(key + "\" =" + value.get(0));
				}
				else if (value.get(0) instanceof String || value.get(1) instanceof String)
				{
					if (value.get(0).equals("*"))
						query.append(" AND (\"").append(key + "\" <= '" + value.get(1) + "') ");
					else if (value.get(1).equals("*"))
						query.append(" AND (\"").append(key + "\" >= '" + value.get(0) + "') ");
					else
						query.append(" AND (\"").append(key + "\" BETWEEN '" + value.get(0) + "' AND '" + value.get(1) + "') ");
				} // Handle condition when two boolean value passed.
				else if (value.get(0) instanceof Boolean || value.get(1) instanceof Boolean)
				{
					query.append(" AND \"").append(key + "\" IN (" + value.get(0) + ", " + value.get(1) + ")");
				}
				else if (value.get(1).equals("*"))
				{
					query.append(" AND \"").append(key + "\" IN (\"" + value.get(0) + "\" , \"" + value.get(1) + "\" )");
				}
				else
				{
					query.append(" AND \"").append(key + "\" BETWEEN '" + value.get(0) + "' AND '" + value.get(1) + "' ");
				}
			}
			else
			{
				if (value.get(0) instanceof String)
				{
					if (Util.isEmpty((String) value.get(0), true))
					{
						query.append(" AND \"").append(key + "\" LIKE '%'");
					}
					else if (DMSConstant.DESCRIPTION.equals(key))
					{
						String description = (String) value.get(0);
						description = description.replace('*', '%');
						query.append(" AND \"").append(key + "\" LIKE '" + description + "'");
					}
					else
					{
						query.append(" AND \"").append(key + "\" LIKE '%" + value.get(0) + "%'");
					}
				}
				else
				{
					query.append(" AND \"").append(key + "\"=" + value.get(0) + "");
				}
			}
		}

		if (query.length() > 0)
			query.delete(0, 5);

		return query.toString();
	} // buildSearchQueryFromMap

	@Override
	public ArrayList<String> addCommonCriteria(String query, int ad_client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		ArrayList<String> qList = new ArrayList<>();
		// AD_Client_id append for search client wise
		if (!Util.isEmpty(query))
			query += " AND ";

		query += commonSearch(ad_client_ID, content, tableID, recordID, documentView);

		qList.add(query);

		return qList;
	} // addCommonCriteria

	@Override
	public String getGenericSearchContentQuery(String searchText, int AD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		StringBuffer query = new StringBuffer();
		if (!Util.isEmpty(searchText, true))
		{
			String inputParam = searchText.toLowerCase().trim().replaceAll(" +", " ");
			query.append(" ( \"").append(DMSConstant.NAME).append("\" LIKE '%").append(inputParam).append("%'");
			query.append(" OR \"").append(DMSConstant.DESCRIPTION).append("\" LIKE '%").append(inputParam).append("%'");

			// Lookup from file content
			if (DMSSearchUtils.isAllowDocumentContentSearch())
			{
				query.append(" OR \"").append(DMSConstant.FILE_CONTENT).append("\" LIKE '%").append(inputParam).append("%'");
			}
			query.append(" ) ");
		}

		return query.toString();
	} // getGenericSearchContentQuery

	private static String commonSearch(int AD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		StringBuffer query = new StringBuffer();
		query.append(" \"").append(DMSConstant.AD_CLIENT_ID + "\" =" + AD_Client_ID);

		//
		StringBuffer hirachicalContent = new StringBuffer("");
		if (content != null && content.getDMS_Content_ID() > 0)
		{
			MDMSAssociation association = MDMSAssociation.getParentAssociationFromContent(content.getDMS_Content_ID(), false, null);
			if (content.isMounting() && association.getRecord_ID() <= 0)
			{
				if (association.getAD_Table_ID() > 0)
				{
					hirachicalContent.append(" AND \"AD_Table_ID\"=").append(association.getAD_Table_ID());
				}
				else
				{
					// Search those content which has any Table ID reference
					// (ass.getAD_Table_ID() <= 0)
					hirachicalContent.append("-AD_Table_ID:0");
					// TODO: need to change Table ID not 0
				}
			}
			else
			{
				hirachicalContent.append(" AND \"").append(DMSConstant.DMS_CONTENT_ID).append("\" IN (");
				DMSSearchUtils.getHierarchicalContent(hirachicalContent, content.getDMS_Content_ID(), AD_Client_ID, tableID, recordID, ", ");
				hirachicalContent.append(content.getDMS_Content_ID()).append(") ");
			}

			//
			query.append("  ").append(hirachicalContent.toString());
		}

		if (DMSConstant.DOCUMENT_VIEW_DELETED_ONLY_VALUE.equalsIgnoreCase(documentView))
			query.append(" AND \"").append(DMSConstant.SHOW_INACTIVE).append("\"=true");
		else if (DMSConstant.DOCUMENT_VIEW_NON_DELETED_VALUE.equalsIgnoreCase(documentView))
			query.append(" AND \"").append(DMSConstant.SHOW_INACTIVE).append("\"=false");

		if (recordID > 0)
			query.append(" AND \"").append(DMSConstant.RECORD_ID).append("\"=").append(recordID);

		if (tableID > 0)
			query.append(" AND \"").append(DMSConstant.AD_TABLE_ID).append("\"=").append(tableID);

		return query.toString();
	} // commonSearch

}
