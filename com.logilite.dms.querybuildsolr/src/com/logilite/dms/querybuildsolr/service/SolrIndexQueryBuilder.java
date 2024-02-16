package com.logilite.dms.querybuildsolr.service;

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
 * Index Query Builder for Solr services
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class SolrIndexQueryBuilder implements IIndexQueryBuilder
{

	/**
	 * Build Solr search Query From Map parameters
	 * 
	 * @param List of Parameters
	 */
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
					query.append(" AND (").append(key + ":" + value.get(0) + ")");
				}
				else if (value.get(0) instanceof String || value.get(1) instanceof String)
				{
					query.append(" AND (").append(key + ":[" + value.get(0) + " TO " + value.get(1) + " ])");
				} // Handle condition when two boolean value passed.
				else if (value.get(0) instanceof Boolean || value.get(1) instanceof Boolean)
				{
					query.append(" AND (").append(key + ":" + value.get(0) + " OR ").append(key + ":" + value.get(1) + ")");
				}
				else if (value.get(1).equals("*"))
				{
					query.append(" AND (").append(key + ":[\"" + value.get(0) + "\" TO " + value.get(1) + " ])");
				}
				else
				{
					query.append(" AND (").append(key + ":[\"" + value.get(0) + "\" TO \"" + value.get(1) + "\" ])");
				}
			}
			else
			{
				if (value.get(0) instanceof String)
				{
					if (Util.isEmpty((String) value.get(0), true))
						query.append(" AND (").append(key + ":*)");
					else
						query.append(" AND (").append(key + ":*" + value.get(0) + "*)");
				}
				else
				{
					query.append(" AND (").append(key + ":\"" + value.get(0) + "\")");
				}
			}
		}

		if (query.length() > 0)
			query.delete(0, 5);
		else
			query.append("*:*");

		return query.toString();
	} // buildSearchQueryFromMap

	/**
	 * Append addition query criteria
	 */
	@Override
	public ArrayList<String> appendCriteria(String query, int AD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		ArrayList<String> qList = commonSearch(AD_Client_ID, content, tableID, recordID, documentView);
		// AD_Client_id append for search client wise
		if (!Util.isEmpty(query))
		{
			query += " AND " + qList.get(0);
			qList.set(0, query);
		}

		return qList;
	} // appendCriteria

	/**
	 * Query build for genericSearched Content
	 */
	@Override
	public ArrayList<String> getGenericSearchedContentQuery(String searchText, int AD_Client_ID, MDMSContent content, int tableID, int recordID,
															String documentView)
	{
		StringBuffer query = new StringBuffer();
		if (!Util.isEmpty(searchText, true))
		{
			String inputParam = searchText.toLowerCase().trim().replaceAll(" +", " ");
			query.append("(").append(DMSConstant.NAME).append(":*").append(inputParam).append("*");
			query.append(" OR ").append(DMSConstant.DESCRIPTION).append(":*").append(inputParam).append("*");

			// Lookup from file content
			if (DMSSearchUtils.isAllowDocumentContentSearch())
			{
				query.append(" OR ").append(DMSConstant.FILE_CONTENT).append(":*").append(inputParam).append("*");
			}

			query.append(")");
		}
		else
		{
			query.append("*:*");
		}

		// 0 Index - for common search query
		// 1 Index - for ContentID Hierarchical search query if string characters are more than 7K
		ArrayList<String> qList = commonSearch(AD_Client_ID, content, tableID, recordID, documentView);
		query.append(" AND ").append(qList.get(0));
		qList.set(0, query.toString());
		return qList;
	} // getGenericSearchedContentQuery

	/**
	 * Query build form Common Search criteria
	 * 
	 * @param  AD_Client_ID
	 * @param  content
	 * @param  tableID
	 * @param  recordID
	 * @param  documentView
	 * @return              String
	 */
	private static ArrayList<String> commonSearch(int AD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		StringBuffer query = new StringBuffer();
		query.append(DMSConstant.AD_CLIENT_ID + " :(" + AD_Client_ID + ")");

		//
		StringBuffer hirachicalContent = new StringBuffer("");
		if (content != null && content.getDMS_Content_ID() > 0)
		{
			MDMSAssociation association = MDMSAssociation.getParentAssociationFromContent(content.getDMS_Content_ID(), false, null);
			if (content.isMounting() && association.getRecord_ID() <= 0)
			{
				if (association.getAD_Table_ID() > 0)
				{
					hirachicalContent.append("AD_Table_ID:").append(association.getAD_Table_ID());
				}
				else
				{
					// Search those content which has any Table ID reference
					// (ass.getAD_Table_ID() <= 0)
					hirachicalContent.append("-AD_Table_ID:0");
				}
			}
			else
			{
				hirachicalContent.append(DMSConstant.DMS_CONTENT_ID).append(":(");
				// TODO Check improvement by Recursive SQL
				DMSSearchUtils.getHierarchicalContent(hirachicalContent, content.getDMS_Content_ID(), AD_Client_ID, tableID, recordID, " OR ");
				hirachicalContent.append(content.getDMS_Content_ID()).append(")");
			}
		}

		if (DMSConstant.DOCUMENT_VIEW_DELETED_ONLY_VALUE.equalsIgnoreCase(documentView))
			query.append(" AND ").append(DMSConstant.SHOW_INACTIVE).append(" :true");
		else if (DMSConstant.DOCUMENT_VIEW_NON_DELETED_VALUE.equalsIgnoreCase(documentView))
			query.append(" AND ").append(DMSConstant.SHOW_INACTIVE).append(" :false");

		if (recordID > 0)
			query.append(" AND ").append(DMSConstant.RECORD_ID).append(":").append(recordID);

		if (tableID > 0)
			query.append(" AND ").append(DMSConstant.AD_TABLE_ID).append(":").append(tableID);

		//
		ArrayList<String> qList = new ArrayList<String>();
		if (hirachicalContent.toString().length() > 7000)
		{
			qList.add(query.toString());
			qList.add(hirachicalContent.toString());
		}
		else
		{
			if (!Util.isEmpty(hirachicalContent.toString(), true))
				query.append(" AND ").append(hirachicalContent.toString());
			qList.add(query.toString());
		}

		return qList;
	} // commonSearch

}
