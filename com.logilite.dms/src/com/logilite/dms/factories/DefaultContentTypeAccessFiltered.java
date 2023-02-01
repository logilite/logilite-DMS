package com.logilite.dms.factories;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.Map.Entry;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Version;

public class DefaultContentTypeAccessFiltered implements IContentTypeAccess
{

	@Override
	public List<Integer> getAccessedContentsRoleWise(int AD_Role_ID)
	{
		return getAccessedContentsRoleWise(AD_Role_ID, null);

	} // getAccessedContentsRoleWise

	@Override
	public HashMap<I_DMS_Version, I_DMS_Association> getFilteredContentList(HashMap<I_DMS_Version, I_DMS_Association> contentMap) throws DBException
	{
		String whereClause = getDynamicWhereClause(contentMap);
		List<Integer> contentIDList = getAccessedContentsRoleWise(Env.getAD_Role_ID(Env.getCtx()), whereClause);

		//
		HashMap<I_DMS_Version, I_DMS_Association> contentsMapFiltered = new HashMap<I_DMS_Version, I_DMS_Association>();

		Iterator<Entry<I_DMS_Version, I_DMS_Association>> it = contentMap.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<I_DMS_Version, I_DMS_Association> mapEntry = (Entry<I_DMS_Version, I_DMS_Association>) it.next();
			I_DMS_Version version = mapEntry.getKey();
			if (version.getDMS_Content().getDMS_ContentType_ID() == 0 || contentIDList.contains(version.getDMS_Content_ID()))
			{
				contentsMapFiltered.put(mapEntry.getKey(), mapEntry.getValue());
			}
		}

		return contentsMapFiltered;
	} // getFilteredContentList

	@Override
	public List<Integer> getAccessedContentsRoleWise(int AD_Role_ID, String dynamicWhere)
	{
		StringBuilder sqlDynamicWhere = new StringBuilder(DMSConstant.SQL_GET_CONTENT_ON_CONTENTTYPE_ACCESS);

		if (!Util.isEmpty(dynamicWhere, true))
		{
			sqlDynamicWhere.append(" AND (" + dynamicWhere + ")");
		}

		ArrayList<Integer> contentIDList = new ArrayList<Integer>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sqlDynamicWhere.toString(), null);
			pstmt.setInt(1, AD_Role_ID);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				contentIDList.add(rs.getInt(1));
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Failed while content type access based filtering " + e.getMessage(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return contentIDList;
	} // getAccessedContentsRoleWise

	/**
	 * Get dynamic where based on header wise filtered
	 * 
	 * @param  contentMap
	 * @return
	 */
	public String getDynamicWhereClause(HashMap<I_DMS_Version, I_DMS_Association> contentMap)
	{
		StringJoiner separator = new StringJoiner(" OR ");
		Iterator<Entry<I_DMS_Version, I_DMS_Association>> it = contentMap.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<I_DMS_Version, I_DMS_Association> mapEntry = (Entry<I_DMS_Version, I_DMS_Association>) it.next();
			I_DMS_Version version = mapEntry.getKey();
			separator.add("c.DMS_Content_ID = " + version.getDMS_Content_ID());

		}
		return separator.toString();
	} // getDynamicWhereClause
}
