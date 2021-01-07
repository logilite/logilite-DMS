/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package org.idempiere.dms.util;

import java.io.File;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;
import com.logilite.search.solr.tika.FileContentParsingThroughTika;

/**
 * Util for DMS generic / advance searching from index server or normal content
 * 
 * @author Sachin Bhimani
 */
public class DMSSearchUtils
{

	static CLogger	log					= CLogger.getCLogger(DMSSearchUtils.class);

	static boolean	isIndexingInitiated	= false;

	/**
	 * Get DMS Contents for rendering for specific level wise.
	 * 
	 * @param  content
	 * @param  AD_Client_ID
	 * @param  isActiveOnly
	 * @return              map of DMS content and association
	 */
	public static HashMap<I_DMS_Content, I_DMS_Association> getDMSContentsWithAssociation(I_DMS_Content content, int AD_Client_ID, boolean isActiveOnly)
	{
		int contentID = 0;
		if (content != null)
			contentID = content.getDMS_Content_ID();

		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			int i = 1;
			pstmt = DB.prepareStatement(isActiveOnly	? DMSConstant.SQL_GET_CONTENT_DIRECTORY_LEVEL_WISE_ACTIVE
														: DMSConstant.SQL_GET_CONTENT_DIRECTORY_LEVEL_WISE_ALL, null);
			pstmt.setInt(i++, AD_Client_ID);
			pstmt.setInt(i++, contentID);
			pstmt.setInt(i++, AD_Client_ID);
			pstmt.setInt(i++, contentID);
			pstmt.setInt(i++, contentID);

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				MDMSAssociation associationChild = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
				MDMSContent contentChild = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
				contentChild.setSeqNo(rs.getString("SeqNo"));

				map.put(contentChild, associationChild);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Content directory level wise fetching failure: ", e);
			throw new AdempiereException("Content directory level wise fetching failure: " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return map;
	} // getDMSContentsWithAssociation

	/**
	 * Generic Searched Content Map
	 * 
	 * @param  dms
	 * @param  searchText
	 * @param  tableID
	 * @param  recordID
	 * @param  content
	 * @return            Map of Content with Association
	 */
	public static HashMap<I_DMS_Content, I_DMS_Association> getGenericSearchedContent(	DMS dms, String searchText, int tableID, int recordID,
																						MDMSContent content)
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

		StringBuffer hirachicalContent = new StringBuffer(" AND DMS_Content_ID:(");

		DMSSearchUtils.getHierarchicalContent(hirachicalContent, content != null ? content.getDMS_Content_ID() : 0, dms.AD_Client_ID, tableID, recordID);

		if (content != null)
		{
			hirachicalContent.append(content.getDMS_Content_ID()).append(")");
			query.append(hirachicalContent.toString());
		}
		else
		{
			if (hirachicalContent.substring(hirachicalContent.length() - 4, hirachicalContent.length()).equals(" OR "))
			{
				hirachicalContent.replace(hirachicalContent.length() - 4, hirachicalContent.length(), ")");
				query.append(hirachicalContent.toString());
			}
		}

		// AD_Client_id append for search client wise
		if (!Util.isEmpty(query.toString()))
			query.append(" AND ");

		query.append(" AD_Client_ID:(").append(Env.getAD_Client_ID(Env.getCtx())).append(")").append(" AND Show_InActive : 'false'");

		if (recordID > 0)
			query.append(" AND Record_ID:" + recordID);

		if (tableID > 0)
			query.append(" AND AD_Table_ID:" + tableID);

		return DMSSearchUtils.fillSearchedContentMap(dms.searchIndex(query.toString()));
	} // getGenericSearchedContent

	/**
	 * Render Searched Content
	 * 
	 * @param  dms
	 * @param  queryParamas
	 * @param  content
	 * @param  tableID
	 * @param  recordID
	 * @return              Map of Content with Association
	 */
	public static HashMap<I_DMS_Content, I_DMS_Association> renderSearchedContent(	DMS dms, HashMap<String, List<Object>> queryParamas, MDMSContent content,
																					int tableID, int recordID)
	{
		String query = dms.buildSolrSearchQuery(queryParamas);

		// AD_Client_id append for search client wise
		if (!Util.isEmpty(query))
			query += " AND ";

		query += "AD_Client_ID :(" + (Env.getAD_Client_ID(Env.getCtx()) + ")");

		StringBuffer hirachicalContent = new StringBuffer(" AND DMS_Content_ID:(");

		DMSSearchUtils.getHierarchicalContent(hirachicalContent, content != null ? content.getDMS_Content_ID() : 0, dms.AD_Client_ID, tableID, recordID);

		if (content != null)
		{
			hirachicalContent.append(content.getDMS_Content_ID()).append(")");
			query += " " + hirachicalContent.toString();
		}
		else
		{
			if (hirachicalContent.substring(hirachicalContent.length() - 4, hirachicalContent.length()).equals(" OR "))
			{
				hirachicalContent.replace(hirachicalContent.length() - 4, hirachicalContent.length(), ")");
				query += " " + hirachicalContent.toString();
			}
		}

		return DMSSearchUtils.fillSearchedContentMap(dms.searchIndex(query));
	} // renderSearchedContent

	/**
	 * Build Hierarchical Content Condition for searching
	 * 
	 * @param hierarchicalContent
	 * @param DMS_Content_ID
	 * @param AD_Client_ID
	 * @param tableID
	 * @param recordID
	 */
	private static void getHierarchicalContent(StringBuffer hierarchicalContent, int DMS_Content_ID, int AD_Client_ID, int tableID, int recordID)
	{
		MDMSContent content = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
		HashMap<I_DMS_Content, I_DMS_Association> map = DMSSearchUtils.getDMSContentsWithAssociation(content, AD_Client_ID, false);
		for (Entry<I_DMS_Content, I_DMS_Association> mapEntry : map.entrySet())
		{
			MDMSContent dmsContent = (MDMSContent) mapEntry.getKey();

			if (dmsContent.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
				getHierarchicalContent(hierarchicalContent, dmsContent.getDMS_Content_ID(), AD_Client_ID, tableID, recordID);
			else
			{
				MDMSAssociation association = (MDMSAssociation) mapEntry.getValue();
				hierarchicalContent.append(association.getDMS_Content_ID()).append(" OR ");

				if (association.getDMS_Content_ID() != dmsContent.getDMS_Content_ID())
					hierarchicalContent.append(dmsContent.getDMS_Content_ID()).append(" OR ");
			}
		}
	} // getHierarchicalContent

	/**
	 * Fill searched content on map with latest version if exists
	 * 
	 * @param  searchedList
	 * @return              Map of Content with Association
	 */
	public static HashMap<I_DMS_Content, I_DMS_Association> fillSearchedContentMap(List<Integer> searchedList)
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		for (Integer entry : searchedList)
		{
			List<Object> latestVersion = DB.getSQLValueObjectsEx(null, DMSConstant.SQL_GET_CONTENT_LATEST_VERSION_NONLINK, entry, entry);

			if (latestVersion != null)
			{
				map.put(new MDMSContent(Env.getCtx(), ((BigDecimal) latestVersion.get(0)).intValue(), null),
						new MDMSAssociation(Env.getCtx(), ((BigDecimal) latestVersion.get(1)).intValue(), null));
			}
		}
		return map;
	} // fillSearchedContentMap

	/**
	 * Create Index Map for Solr
	 * 
	 * @param  DMSContent
	 * @param  DMSAssociation
	 * @param  file
	 * @return                Map
	 */
	public static Map<String, Object> createIndexMap(MDMSContent DMSContent, MDMSAssociation DMSAssociation, File file)
	{
		Map<String, Object> solrValue = new HashMap<String, Object>();
		solrValue.put(DMSConstant.AD_CLIENT_ID, DMSContent.getAD_Client_ID());
		solrValue.put(DMSConstant.NAME, DMSContent.getName().toLowerCase());
		solrValue.put(DMSConstant.CREATED, DMSContent.getCreated());
		solrValue.put(DMSConstant.CREATEDBY, DMSContent.getCreatedBy());
		solrValue.put(DMSConstant.UPDATED, DMSContent.getUpdated());
		solrValue.put(DMSConstant.UPDATEDBY, DMSContent.getUpdatedBy());
		solrValue.put(DMSConstant.DESCRIPTION, (!Util.isEmpty(DMSContent.getDescription(), true) ? DMSContent.getDescription().toLowerCase() : null));
		solrValue.put(DMSConstant.CONTENTTYPE, DMSContent.getDMS_ContentType_ID());
		solrValue.put(DMSConstant.DMS_CONTENT_ID, DMSContent.getDMS_Content_ID());
		solrValue.put(DMSConstant.AD_Table_ID, DMSAssociation.getAD_Table_ID());
		solrValue.put(DMSConstant.RECORD_ID, DMSAssociation.getRecord_ID());
		solrValue.put(DMSConstant.SHOW_INACTIVE, !(DMSContent.isActive() && DMSAssociation.isActive()));

		// File Content
		if (DMSSearchUtils.isAllowDocumentContentSearch() && file != null)
		{
			solrValue.put(DMSConstant.FILE_CONTENT, new FileContentParsingThroughTika(file).getParsedDocumentContent());
		}

		if (DMSContent.getM_AttributeSetInstance_ID() > 0)
		{
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try
			{
				stmt = DB.prepareStatement(DMSConstant.SQL_GET_ASI, null);
				stmt.setInt(1, DMSContent.getM_AttributeSetInstance_ID());
				rs = stmt.executeQuery();

				if (rs.isBeforeFirst())
				{
					while (rs.next())
					{
						String fieldName = "ASI_" + rs.getString("Name");

						if (rs.getTimestamp(MAttributeInstance.COLUMNNAME_ValueTimeStamp) != null)
							solrValue.put(fieldName, rs.getTimestamp(MAttributeInstance.COLUMNNAME_ValueTimeStamp));
						else if (rs.getDouble(MAttributeInstance.COLUMNNAME_ValueNumber) > 0)
							solrValue.put(fieldName, rs.getDouble(MAttributeInstance.COLUMNNAME_ValueNumber));
						else if (rs.getInt(MAttributeInstance.COLUMNNAME_ValueInt) > 0)
							solrValue.put(fieldName, rs.getInt(MAttributeInstance.COLUMNNAME_ValueInt));
						else if (!Util.isEmpty(rs.getString(MAttributeInstance.COLUMNNAME_Value), true))
							solrValue.put(fieldName, rs.getString(MAttributeInstance.COLUMNNAME_Value));
					}
				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, "ASI fetching failure.", e);
				throw new AdempiereException("ASI fetching failure." + e.getLocalizedMessage());
			}
			finally
			{
				DB.close(rs, stmt);
				rs = null;
				stmt = null;
			}
		}

		return solrValue;
	} // createIndexMap

	public static boolean isAllowDocumentContentSearch()
	{
		return MSysConfig.getBooleanValue(DMSConstant.DMS_ALLOW_DOCUMENT_CONTENT_SEARCH, false, Env.getAD_Client_ID(Env.getCtx()));
	} // isAllowDocumentContentSearch

	/**
	 * Get index searcher
	 * 
	 * @param  AD_Client_ID
	 * @return              {@link IIndexSearcher}
	 */
	public static IIndexSearcher getIndexSearcher(int AD_Client_ID)
	{
		IIndexSearcher idxSearcher = ServiceUtils.getIndexSearcher(AD_Client_ID);
		if (!isIndexingInitiated)
		{
			/*
			 * Create Fields Type in schema if not exists
			 */
			Map<String, Object> mapAttribute = new HashMap<String, Object>();
			if (!idxSearcher.getFieldTypeSet().contains(DMSConstant.SOLR_FIELDTYPE_TLONGS))
			{
				mapAttribute.put("name", DMSConstant.SOLR_FIELDTYPE_TLONGS);
				mapAttribute.put("class", "solr.TrieLongField");
				mapAttribute.put("precisionStep", "8");
				mapAttribute.put("multiValued", "true");
				mapAttribute.put("positionIncrementGap", "0");

				//
				idxSearcher.createFieldTypeInIndexSchema(mapAttribute);
			}

			/*
			 * Create Fields in schema if not exists
			 */
			if (!idxSearcher.getFieldSet().contains(DMSConstant.FILE_CONTENT))
			{
				mapAttribute.clear();
				mapAttribute.put("name", DMSConstant.FILE_CONTENT);
				mapAttribute.put("type", "text_general");
				mapAttribute.put("indexed", true);
				mapAttribute.put("stored", false);
				mapAttribute.put("multiValued", true);
				//
				idxSearcher.createFieldsInIndexSchema(mapAttribute);
			}

			if (!idxSearcher.getFieldSet().contains(DMSConstant.DMS_CONTENT_ID))
			{
				mapAttribute.clear();
				mapAttribute.put("name", DMSConstant.DMS_CONTENT_ID);
				mapAttribute.put("type", DMSConstant.SOLR_FIELDTYPE_TLONGS);
				//
				idxSearcher.createFieldsInIndexSchema(mapAttribute);
			}

			if (!idxSearcher.getFieldSet().contains(DMSConstant.SOLR_FIELD_DESCRIPTION))
			{
				mapAttribute.clear();
				mapAttribute.put("name", DMSConstant.SOLR_FIELD_DESCRIPTION);
				mapAttribute.put("type", "text_general");
				//
				idxSearcher.createFieldsInIndexSchema(mapAttribute);
			}

			isIndexingInitiated = true;
		}
		return idxSearcher;
	}

	/**
	 * From query to search the content reference
	 * 
	 * @param  indexSearcher
	 * @param  query
	 * @return               search result as list of DMS_Content_ID
	 */
	public static List<Integer> searchIndex(IIndexSearcher indexSearcher, String query)
	{
		Object docList = indexSearcher.searchIndexNoRestriction(query);

		ArrayList<Integer> list = new ArrayList<Integer>();
		if (docList != null && docList instanceof SolrDocumentList)
		{
			for (int i = 0; i < ((SolrDocumentList) docList).size(); i++)
			{
				SolrDocument solrDocument = ((SolrDocumentList) docList).get(i);
				ArrayList<?> valueIDs = (ArrayList<?>) solrDocument.getFieldValue(DMSConstant.DMS_CONTENT_ID);
				int contentID = ((Long) valueIDs.get(0)).intValue();
				list.add(contentID);
			}
		}
		return list;
	} // searchIndex

}
