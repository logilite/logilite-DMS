package com.logilite.dms.factories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.logilite.dms.model.MDMSContent;

/**
 * Index Query Builder for implementation by specific searching tool
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public interface IIndexQueryBuilder
{

	public String buildSearchQueryFromMap(HashMap<String, List<Object>> params);

	public ArrayList<String> addCommonCriteria(String query, int AD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView);

	public String getGenericSearchContentQuery(String searchText, int AD_Client_ID, MDMSContent content, int tableID, int recordID, String documentView);

}
