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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IContentTypeAccess;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Version;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSVersion;

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

	static CLogger				log							= CLogger.getCLogger(DMSSearchUtils.class);

	public static final String	SOLR_FIELDTYPE_TLONGS		= "tlongs";
	public static final String	SOLR_FIELDTYPE_TEXT_GENERAL	= "text_general";

	static boolean				isIndexingInitiated			= false;

	/**
	 * Get DMS Contents for rendering for specific level wise.
	 * 
	 * @param  content
	 * @param  AD_Client_ID
	 * @param  documentView
	 * @return              map of DMS content and association
	 */
	public static HashMap<I_DMS_Version, I_DMS_Association> getDMSContentsWithAssociation(I_DMS_Content content, int AD_Client_ID, String documentView)
	{
		int contentID = 0;
		if (content != null)
			contentID = content.getDMS_Content_ID();

		HashMap<I_DMS_Version, I_DMS_Association> map = new LinkedHashMap<I_DMS_Version, I_DMS_Association>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			String sql = "";

			if (DMSConstant.DOCUMENT_VIEW_ALL_VALUE.equalsIgnoreCase(documentView))
				sql = DMSConstant.SQL_GET_CONTENT_DIR_LEVEL_WISE_ALL;
			else if (DMSConstant.DOCUMENT_VIEW_DELETED_ONLY_VALUE.equalsIgnoreCase(documentView))
				sql = DMSConstant.SQL_GET_CONTENT_DIR_LEVEL_WISE_INACTIVE;
			else
				sql = DMSConstant.SQL_GET_CONTENT_DIR_LEVEL_WISE_ACTIVE;

			int i = 1;
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(i++, AD_Client_ID);
			pstmt.setInt(i++, contentID);
			pstmt.setInt(i++, AD_Client_ID);
			pstmt.setInt(i++, contentID);
			pstmt.setInt(i++, contentID);

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				MDMSVersion version = new MDMSVersion(Env.getCtx(), rs.getInt("DMS_Version_ID"), null);
				MDMSAssociation associationChild = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);

				map.put(version, associationChild);
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
	 * @param  documentView
	 * @return              Map of Content with Association
	 */
	public static HashMap<I_DMS_Version, I_DMS_Association> getGenericSearchedContent(	DMS dms, String searchText, int tableID, int recordID,
																						MDMSContent content, String documentView)
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

		//
		StringBuffer hirachicalContent = new StringBuffer(" AND ").append(DMSConstant.DMS_CONTENT_ID).append(":(");

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

		query	.append(DMSConstant.AD_CLIENT_ID).append(":(").append(Env.getAD_Client_ID(Env.getCtx())).append(")")
				.append(" AND ");

		if (DMSConstant.DOCUMENT_VIEW_DELETED_ONLY_VALUE.equalsIgnoreCase(documentView))
			query.append(DMSConstant.SHOW_INACTIVE).append(" :true");
		else if (DMSConstant.DOCUMENT_VIEW_NON_DELETED_VALUE.equalsIgnoreCase(documentView))
			query.append(DMSConstant.SHOW_INACTIVE).append(" :false");

		if (recordID > 0)
			query.append(" AND ").append(DMSConstant.RECORD_ID).append(":").append(recordID);

		if (tableID > 0)
			query.append(" AND ").append(DMSConstant.AD_TABLE_ID).append(":").append(tableID);

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
	public static HashMap<I_DMS_Version, I_DMS_Association> renderSearchedContent(	DMS dms, HashMap<String, List<Object>> queryParamas, MDMSContent content,
																					int tableID, int recordID)
	{
		String query = dms.buildSolrSearchQuery(queryParamas);

		// AD_Client_id append for search client wise
		if (!Util.isEmpty(query))
			query += " AND ";

		query += DMSConstant.AD_CLIENT_ID + " :(" + (Env.getAD_Client_ID(Env.getCtx()) + ")");

		//
		StringBuffer hirachicalContent = new StringBuffer(" AND ").append(DMSConstant.DMS_CONTENT_ID).append(":(");

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
		HashMap<I_DMS_Version, I_DMS_Association> map = DMSSearchUtils.getDMSContentsWithAssociation(	content, AD_Client_ID,
																										DMSConstant.DOCUMENT_VIEW_ALL_VALUE);
		for (Entry<I_DMS_Version, I_DMS_Association> mapEntry : map.entrySet())
		{
			MDMSVersion version = (MDMSVersion) mapEntry.getKey();
			if (version.getDMS_Content().getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
				getHierarchicalContent(hierarchicalContent, version.getDMS_Content_ID(), AD_Client_ID, tableID, recordID);
			else
			{
				MDMSAssociation association = (MDMSAssociation) mapEntry.getValue();
				hierarchicalContent.append(association.getDMS_Content_ID()).append(" OR ");

				if (association.getDMS_Content_ID() != version.getDMS_Content_ID())
					hierarchicalContent.append(version.getDMS_Content_ID()).append(" OR ");
			}
		}
	} // getHierarchicalContent

	/**
	 * Fill searched content on map with latest version if exists
	 * 
	 * @param  searchedList
	 * @return              Map of Content with Association
	 */
	public static HashMap<I_DMS_Version, I_DMS_Association> fillSearchedContentMap(HashSet<Integer> searchedList)
	{
		HashMap<I_DMS_Version, I_DMS_Association> map = new LinkedHashMap<I_DMS_Version, I_DMS_Association>();

		for (Integer contentID : searchedList)
		{
			List<Object> latestVersion = DB.getSQLValueObjectsEx(null, DMSConstant.SQL_GET_CONTENT_LATEST_VERSION_NONLINK, contentID);

			if (latestVersion != null)
			{
				map.put(new MDMSVersion(Env.getCtx(), ((BigDecimal) latestVersion.get(0)).intValue(), null),
						new MDMSAssociation(Env.getCtx(), ((BigDecimal) latestVersion.get(1)).intValue(), null));
			}
		}
		return map;
	} // fillSearchedContentMap

	/**
	 * Create Index Map for Solr
	 * 
	 * @param  content
	 * @param  association
	 * @param  version
	 * @param  file
	 * @return             Map
	 */
	public static Map<String, Object> createIndexMap(I_DMS_Content content, I_DMS_Association association, I_DMS_Version version, File file)
	{
		Map<String, Object> solrValue = new HashMap<String, Object>();
		solrValue.put(DMSConstant.DMS_CONTENT_ID, content.getDMS_Content_ID());
		solrValue.put(DMSConstant.NAME, content.getName().toLowerCase());
		solrValue.put(DMSConstant.CREATED, content.getCreated());
		solrValue.put(DMSConstant.UPDATED, content.getUpdated());
		solrValue.put(DMSConstant.CREATEDBY, content.getCreatedBy());
		solrValue.put(DMSConstant.UPDATEDBY, content.getUpdatedBy());
		solrValue.put(DMSConstant.CONTENTTYPE, content.getDMS_ContentType_ID());
		solrValue.put(DMSConstant.DESCRIPTION, (!Util.isEmpty(content.getDescription(), true) ? content.getDescription().toLowerCase() : null));
		solrValue.put(DMSConstant.AD_CLIENT_ID, content.getAD_Client_ID());
		solrValue.put(DMSConstant.SHOW_INACTIVE, !(content.isActive() && association.isActive()));

		solrValue.put(DMSConstant.RECORD_ID, association.getRecord_ID());
		solrValue.put(DMSConstant.AD_TABLE_ID, association.getAD_Table_ID());
		solrValue.put(DMSConstant.DMS_ASSOCIATION_ID, association.getDMS_Association_ID());

		solrValue.put(DMSConstant.VERSION_SEQ_NO, version.getSeqNo());
		solrValue.put(DMSConstant.DMS_VERSION_ID, version.getDMS_Version_ID());

		// File Content
		if (DMSSearchUtils.isAllowDocumentContentSearch() && file != null)
		{
			solrValue.put(DMSConstant.FILE_CONTENT, new FileContentParsingThroughTika(file).getParsedDocumentContent());
		}

		if (content.getM_AttributeSetInstance_ID() > 0)
		{
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try
			{
				stmt = DB.prepareStatement(DMSConstant.SQL_GET_ASI_INFO, null);
				stmt.setInt(1, content.getM_AttributeSetInstance_ID());
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

	/**
	 * Check is Allowed to extract content text from the document and use for searching
	 * 
	 * @return
	 */
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
			if (!idxSearcher.getFieldTypeSet().contains(SOLR_FIELDTYPE_TLONGS))
			{
				mapAttribute.put("name", SOLR_FIELDTYPE_TLONGS);
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
				mapAttribute.put("type", SOLR_FIELDTYPE_TEXT_GENERAL);
				mapAttribute.put("indexed", true);
				mapAttribute.put("stored", false);
				mapAttribute.put("multiValued", true);
				//
				idxSearcher.createFieldsInIndexSchema(mapAttribute);
			}

			// Create fields in index schema DMS_Content_ID
			if (!idxSearcher.getFieldSet().contains(DMSConstant.DMS_CONTENT_ID))
			{
				mapAttribute.clear();
				mapAttribute.put("name", DMSConstant.DMS_CONTENT_ID);
				mapAttribute.put("type", SOLR_FIELDTYPE_TLONGS);
				//
				idxSearcher.createFieldsInIndexSchema(mapAttribute);
			}

			// Create fields in index schema DMS_Version_ID
			if (!idxSearcher.getFieldSet().contains(DMSConstant.DMS_VERSION_ID))
			{
				mapAttribute.clear();
				mapAttribute.put("name", DMSConstant.DMS_VERSION_ID);
				mapAttribute.put("type", SOLR_FIELDTYPE_TLONGS);
				//
				idxSearcher.createFieldsInIndexSchema(mapAttribute);
			}

			// Create fields in index schema DMS_Association_ID
			if (!idxSearcher.getFieldSet().contains(DMSConstant.DMS_ASSOCIATION_ID))
			{
				mapAttribute.clear();
				mapAttribute.put("name", DMSConstant.DMS_ASSOCIATION_ID);
				mapAttribute.put("type", SOLR_FIELDTYPE_TLONGS);
				//
				idxSearcher.createFieldsInIndexSchema(mapAttribute);
			}

			if (!idxSearcher.getFieldSet().contains(DMSConstant.DESCRIPTION))
			{
				mapAttribute.clear();
				mapAttribute.put("name", DMSConstant.DESCRIPTION);
				mapAttribute.put("type", SOLR_FIELDTYPE_TEXT_GENERAL);
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
	public static HashSet<Integer> searchIndex(IIndexSearcher indexSearcher, String query)
	{
		Object docList = indexSearcher.searchIndexNoRestriction(query);

		HashSet<Integer> set = new HashSet<Integer>();
		if (docList != null && docList instanceof SolrDocumentList)
		{
			for (int i = 0; i < ((SolrDocumentList) docList).size(); i++)
			{
				SolrDocument solrDocument = ((SolrDocumentList) docList).get(i);
				ArrayList<?> valueIDs = (ArrayList<?>) solrDocument.getFieldValue(DMSConstant.DMS_CONTENT_ID);
				int contentID = ((Long) valueIDs.get(0)).intValue();
				set.add(contentID);
			}
		}
		return set;
	} // searchIndex

	/**
	 * Create indexing in index server
	 * 
	 * @param content          DMS_Content
	 * @param association      DMS_Association
	 * @param version          DMS_Version
	 * @param deleteIndexQuery Query for delete existing index
	 */
	public static void doIndexingInServer(I_DMS_Content content, I_DMS_Association association, I_DMS_Version version, String deleteIndexQuery)
	{
		IIndexSearcher indexSeracher = ServiceUtils.getIndexSearcher(content.getAD_Client_ID());
		if (indexSeracher == null)
			throw new AdempiereException("Index Server not found");

		IFileStorageProvider fsProvider = FileStorageUtil.get(content.getAD_Client_ID(), false);
		if (fsProvider == null)
			throw new AdempiereException("Storage provider is not define on clientInfo.");

		IContentManager contentManager = DMSFactoryUtils.getContentManager(content.getAD_Client_ID());
		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		// Delete existing index
		if (!Util.isEmpty(deleteIndexQuery, true))
			indexSeracher.deleteIndexByQuery(deleteIndexQuery);

		// Create index
		File file = fsProvider.getFile(contentManager.getPathByValue(content));
		Map<String, Object> solrValue = createIndexMap(content, association, version, file);
		indexSeracher.indexContent(solrValue);

		// Update the value of IsIndexed flag in Content
		if (!version.isIndexed())
		{
			DB.executeUpdate("UPDATE DMS_Version SET IsIndexed='Y' WHERE DMS_Version_ID = ? ", version.getDMS_Version_ID(), null);
		}
	} // doIndexing

	/**
	 * Retrieve content based on params with sub-level only
	 * 
	 * @param  parentContent     - Parent Content
	 * @param  associationTypeID - Association Type ID [ optional ]
	 * @param  fileName          - FileName [ optional ]
	 * @return                   Array of Contents
	 */
	public static I_DMS_Content[] selectContentActiveOnly(I_DMS_Content parentContent, int associationTypeID, int contentTypeID, String fileName)
	{
		int contentID = 0;
		if (parentContent != null)
			contentID = parentContent.getDMS_Content_ID();

		StringBuffer sql = new StringBuffer(DMSConstant.SQL_GET_CONTENT_DIR_LEVEL_WISE_ACTIVE);
		if (contentTypeID > 0)
			sql.append(" AND c.DMS_ContentType_ID = ").append(contentTypeID);
		if (associationTypeID > 0)
			sql.append(" AND c.DMS_AssociationType_ID = ").append(associationTypeID);

		ArrayList<I_DMS_Content> arrContents = new ArrayList<I_DMS_Content>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			int i = 1;
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(i++, Env.getAD_Client_ID(Env.getCtx()));
			pstmt.setInt(i++, contentID);
			pstmt.setInt(i++, Env.getAD_Client_ID(Env.getCtx()));
			pstmt.setInt(i++, contentID);
			pstmt.setInt(i++, contentID);

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				MDMSContent childContent = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
				if (!Util.isEmpty(fileName, true))
				{
					MDMSVersion version = (MDMSVersion) MDMSVersion.getLatestVersion(childContent, true, 0);
					if (version.getValue().equals(fileName.trim()))
					{
						arrContents.add(childContent);
						break;
					}
				}
				else
				{
					arrContents.add(childContent);
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Based on params to get content directory level wise fetching failure: ", e);
			throw new AdempiereException("Based on params to get content directory level wise fetching failure: " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return (I_DMS_Content[]) arrContents.toArray(new I_DMS_Content[0]);
	} // selectContentActiveOnly

	/**
	 * Get Filtered contents based on parameters
	 * 
	 * @param  contents                       - Array of Content
	 * @param  isApplyContentTypeAccessFilter - Apply ContentTypeAccess filter
	 * @param  isApplyPermissionFilter        - Apply Permission Filter
	 * @return                                - Return filtered content array
	 */
	public static I_DMS_Content[] getFilteredContents(I_DMS_Content[] contents, boolean isApplyContentTypeAccessFilter, boolean isApplyPermissionFilter)
	{
		ArrayList<I_DMS_Content> contentTypeAccessFiltered = new ArrayList<I_DMS_Content>();

		// Content Type Access
		if (isApplyContentTypeAccessFilter)
		{
			// Content Type wise access restriction
			IContentTypeAccess contentTypeAccess = DMSFactoryUtils.getContentTypeAccessFactory();
			List<Integer> accessibleContentList = contentTypeAccess.getAccessedContentsRoleWise(Env.getAD_Role_ID(Env.getCtx()));

			for (I_DMS_Content content : contents)
			{
				if (accessibleContentList.contains(content.getDMS_Content_ID()) || content.getDMS_ContentType_ID() == 0)
				{
					contentTypeAccessFiltered.add(content);
				}
			}
		}
		else
		{
			contentTypeAccessFiltered.addAll(Arrays.asList(contents));
		}

		// Permission Access
		ArrayList<I_DMS_Content> permissionAccessFiltered = new ArrayList<I_DMS_Content>(contentTypeAccessFiltered);
		if (isApplyPermissionFilter)
		{
			// Permission wise access restriction
			if (DMSPermissionUtils.isPermissionAllowed())
			{
				permissionAccessFiltered = DMSFactoryUtils.getPermissionFactory().getFilteredContentList(new HashSet<I_DMS_Content>(contentTypeAccessFiltered));
			}
		}

		return (I_DMS_Content[]) permissionAccessFiltered.toArray(new I_DMS_Content[permissionAccessFiltered.size()]);
	} // getFilteredContents

	/**
	 * Get list of child content based on ParentContent
	 * 
	 * @param  dms
	 * @param  parentContent                  - Directory type content
	 * @param  isApplyContentTypeAccessFilter
	 * @param  isApplyPermissionFilter
	 * @return                                List of child content versions
	 */
	public static I_DMS_Version[] getChildContentFiltered(	DMS dms, I_DMS_Content parentContent, boolean isApplyContentTypeAccessFilter,
															boolean isApplyPermissionFilter)
	{
		HashMap<I_DMS_Version, I_DMS_Association> contentsMap = dms.getDMSContentsWithAssociation(parentContent, dms.AD_Client_ID, null);

		// Content Type Access
		if (isApplyContentTypeAccessFilter)
		{
			// Content Type wise access restriction
			IContentTypeAccess contentTypeAccess = DMSFactoryUtils.getContentTypeAccessFactory();
			contentsMap = contentTypeAccess.getFilteredContentList(contentsMap);
		}

		// Permission wise access
		if (isApplyPermissionFilter)
		{
			if (DMSPermissionUtils.isPermissionAllowed())
			{
				contentsMap = dms.getPermissionManager().getFilteredVersionList(contentsMap);
			}
		}

		Set<I_DMS_Version> childContents = contentsMap.keySet();
		return childContents.toArray(new I_DMS_Version[childContents.size()]);
	} // getChildContentFiltered

}
