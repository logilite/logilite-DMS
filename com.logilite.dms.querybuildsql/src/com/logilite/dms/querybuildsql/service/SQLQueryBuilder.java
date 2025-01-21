package com.logilite.dms.querybuildsql.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.compiere.util.DB;
import org.compiere.util.Util;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IIndexQueryBuilder;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.util.DMSSearchUtils;

/**
 * Query Build for Local Search
 * 
 * @author Bhautik Panchal
 */
public class SQLQueryBuilder implements IIndexQueryBuilder
{
	// TODO Should be move in DMSConstant class
	private String	isActive	= "IsActive";
	private String	parentURL	= "ParentURL";

	@Override
	public String buildSearchQueryFromMap(HashMap<String, List<Object>> params)
	{
		boolean hasASIAttrib = false;

		StringBuffer queryMain = new StringBuffer();
		StringBuffer queryCrossTabColumns = new StringBuffer();
		StringBuffer queryCrossTabWhereclause = new StringBuffer();

		for (Entry<String, List<Object>> row : params.entrySet())
		{
			String key = row.getKey();
			List<Object> value = row.getValue();

			if (key.equals(DMSConstant.CONTENTTYPE))
			{
				key = DMSConstant.DMS_CONTENTTYPE_ID;
			}

			if (key.startsWith(DMSConstant.PREFIX_SEARCH_ATTRIB_ASI))
			{
				// Add Content type attribute
				hasASIAttrib = true;
				queryCrossTabWhereclause.append(getSubQuery(key, value));
			}
			else
			{
				if (value.size() == 2)
				{
					if (value.get(0) instanceof String && value.get(1) instanceof Boolean && value.get(1) == Boolean.TRUE)
					{
						queryMain.append(" AND \"").append(key + "\" = '" + value.get(0) + "' ");
					}
					else if (value.get(0) instanceof String && value.get(1) instanceof Boolean && value.get(1) == Boolean.TRUE)
					{
						queryMain.append(" AND ").append(key + " =" + value.get(0));
					}
					else if (value.get(0) instanceof String || value.get(1) instanceof String)
					{
						if (value.get(0).equals("*"))
							queryMain.append(" AND (").append(key + " <= '" + value.get(1) + "') ");
						else if (value.get(1).equals("*"))
							queryMain.append(" AND (").append(key + " >= '" + value.get(0) + "') ");
						else
							queryMain.append(" AND (").append(key + " BETWEEN '" + value.get(0) + "' AND '" + value.get(1) + "') ");
					} // Handle condition when two boolean value passed.
					else if (value.get(0) instanceof Boolean || value.get(1) instanceof Boolean)
					{
						queryMain.append(" AND ").append(key + " IN (" + value.get(0) + ", " + value.get(1) + ")");
					}
					else if (value.get(1).equals("*"))
					{
						queryMain.append(" AND ").append(key + " IN (\"" + value.get(0) + "\" , \"" + value.get(1) + "\" )");
					}
					else
					{
						queryMain.append(" AND ").append(key + " BETWEEN '" + value.get(0) + "' AND '" + value.get(1) + "' ");
					}
				}
				else
				{
					if (value.get(0) instanceof String)
					{
						if (Util.isEmpty((String) value.get(0), true))
						{
							queryMain.append(" AND ").append(key + " ILIKE '%'");
						}
						else if (DMSConstant.DESCRIPTION.equals(key))
						{
							String description = (String) value.get(0);
							description = description.replace('*', '%');
							queryMain.append(" AND ").append(key + " ILIKE '" + description + "'");
						}
						else
						{
							queryMain.append(" AND ").append(key + " LIKE '%" + value.get(0) + "%'");
						}
					}
					else
					{
						queryMain.append(" AND ").append(key + "=" + value.get(0) + "");
					}
				}
			}
		}

		//
		if (hasASIAttrib)
		{
			Object[] name = DB.getKeyNamePairs(DMSConstant.SQL_GET_ATTRIBUTE_CONTENTTYPE_LIST, false, params.get(DMSConstant.CONTENTTYPE).get(0));
			for (Object object : name)
			{
				queryCrossTabColumns.append(", \"" + DMSSearchUtils.getIndexFieldName(object.toString()) + "\" text");
			}
			queryMain.append(" And " + DMSConstant.M_ATTRIBUTESETINSTANCE_ID + " IN ");
		}

		if (queryCrossTabWhereclause.length() > 0)
		{
			queryCrossTabWhereclause.delete(0, 4);
			queryMain.append(	DMSConstant.SQL_GET_ATTRI_SET_INSTANCE_LIST + queryCrossTabColumns + " ) Where "
								+ queryCrossTabWhereclause.toString().replace("[", "").toString().replace("]", "")
								+ " )");
		}

		if (queryMain.length() > 0)
			queryMain.delete(0, 5);
		// else
		// query.append("*:*");

		return queryMain.toString();
	}

	private String getSubQuery(String key, List<Object> value)
	{
		StringBuffer query = new StringBuffer();
		key = key.replace(DMSConstant.PREFIX_SEARCH_ATTRIB_ASI, "");
		if (value.size() == 2)
		{
			if (value.get(0) instanceof String	&&
				value.get(1) instanceof Boolean &&
				value.get(1) == Boolean.TRUE) // Chosen Multiple Selection
			{
				query.append(" AND \"").append(key + "\" =" + value.get(0));
			}
			else if (value.get(0) instanceof String && value.get(1) instanceof String) // Date
			{
				if (value.get(0).equals("*"))
					query.append(" AND CAST(\"" + key + "\" AS TimeStamp) <='" + value.get(1) + "' ");
				else if (value.get(1).equals("*"))
					query.append(" AND CAST(\"" + key + "\" AS TimeStamp) >='" + value.get(0) + "' ");
				else
					query.append(	" AND CAST(\""	+ key + "\" AS TimeStamp) BETWEEN '"
									+ value.get(0) + "' AND '" + value.get(1) + "' ");
			}
			else if (value.get(0) instanceof Boolean || value.get(1) instanceof Boolean)
			{
				query.append(" AND \"").append(key + "\" IN (" + value.get(0) + ", " + value.get(1) + ")");
			}
			else // Number
			{
				if (value.get(1).equals("*"))
					query.append(" AND CAST(\"" + key + "\" AS NUMERIC) >= " + value.get(0) + " ");
				else if (value.get(0).equals("*"))
					query.append(" AND CAST(\"" + key + "\" AS NUMERIC) <= " + value.get(1) + " ");
				else
					query.append(	" AND CAST(\""	+ key + "\" AS NUMERIC)  BETWEEN "
									+ value.get(0) + " AND " + value.get(1) + " ");
			}
		}
		else
		{
			if (value.get(0) instanceof String)
			{
				query.append(" AND \"" + key + "\" ILIKE '%" + value + "%'  ");
			}
			else
			{
				query.append(" AND CAST(\"" + key + "\" AS NUMERIC) =" + value + " ");
			}
		}
		return query.toString();
	}

	@Override
	public ArrayList<String> addCommonCriteria(String query, int ad_Client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		// AD_Client_id append for search client wise
		if (!Util.isEmpty(query))
			query += " AND ";

		query += commonSearch(ad_Client_ID, content, tableID, recordID, documentView);

		ArrayList<String> qList = new ArrayList<>();
		qList.add(query);
		return qList;
	}

	@Override
	public String getGenericSearchContentQuery(String searchText, int ad_client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		StringBuffer query = new StringBuffer();
		if (!Util.isEmpty(searchText, true))
		{
			String inputParam = searchText.trim().replaceAll(" +", " ");
			query.append(" ( ").append(DMSConstant.NAME).append(" ILIKE '%").append(inputParam).append("%'");
			query.append(" OR ").append(DMSConstant.DESCRIPTION).append(" ILIKE '%").append(inputParam).append("%'");
		}
		if (!Util.isEmpty(searchText, true))
		{
			query.append(" ) AND ");
		}

		query.append(commonSearch(ad_client_ID, content, tableID, recordID, documentView));

		return query.toString();
	}

	private String commonSearch(int ad_client_ID, MDMSContent content, int tableID, int recordID, String documentView)
	{
		StringBuffer query = new StringBuffer();
		query.append(" ").append(DMSConstant.AD_CLIENT_ID + " =" + ad_client_ID);

		//
		StringBuffer hirachicalContent = new StringBuffer("");
		if (content != null && content.getDMS_Content_ID() > 0)
		{
			MDMSAssociation association = MDMSAssociation.getParentAssociationFromContent(content.getDMS_Content_ID(), false, null);
			if (content.isMounting() && association.getRecord_ID() <= 0)
			{
				if (association.getAD_Table_ID() > 0)
				{
					hirachicalContent.append(" AND ").append(DMSConstant.AD_TABLE_ID + " =").append(association.getAD_Table_ID());
				}
			}
			else
			{
				hirachicalContent.append(" AND ").append(DMSConstant.DMS_CONTENT_ID).append(" IN (");
				DMSSearchUtils.getHierarchicalContent(hirachicalContent, content.getDMS_Content_ID(), ad_client_ID, tableID, recordID, ", ");
				hirachicalContent.append(content.getDMS_Content_ID()).append(") ");
			}

			//
			query.append("  ").append(hirachicalContent.toString());
		}

		if (DMSConstant.DOCUMENT_VIEW_DELETED_ONLY_VALUE.equalsIgnoreCase(documentView))
			query.append(" AND ").append(isActive).append("= 'N'");
		else if (DMSConstant.DOCUMENT_VIEW_NON_DELETED_VALUE.equalsIgnoreCase(documentView))
			query.append(" AND ").append(isActive).append("= 'Y'");

		if (recordID > 0)
			query.append(" AND ").append(parentURL).append(" Like '%").append(recordID + "'");

		// TODO FIXME Table And Record ID reference check

		return query.toString();
	}

}
