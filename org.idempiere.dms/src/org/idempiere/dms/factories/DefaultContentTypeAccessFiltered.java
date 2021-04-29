package org.idempiere.dms.factories;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Version;

public class DefaultContentTypeAccessFiltered implements IContentTypeAccess
{

	@Override
	public List<Integer> getAccessedContentsRoleWise(int AD_Role_ID)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ArrayList<Integer> contentIDList = new ArrayList<Integer>();
		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_CONTENT_ON_CONTENTTYPE_ACCESS, null);
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

	@Override
	public HashMap<I_DMS_Version, I_DMS_Association> getFilteredContentList(HashMap<I_DMS_Version, I_DMS_Association> contentMap) throws DBException
	{
		List<Integer> contentIDList = getAccessedContentsRoleWise(Env.getAD_Role_ID(Env.getCtx()));

		//
		HashMap<I_DMS_Version, I_DMS_Association> contentsMapFiltered = new HashMap<I_DMS_Version, I_DMS_Association>();

		Iterator<Entry<I_DMS_Version, I_DMS_Association>> it = contentMap.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<I_DMS_Version, I_DMS_Association> mapEntry = (Entry<I_DMS_Version, I_DMS_Association>) it.next();
			I_DMS_Version version = mapEntry.getKey();
			if (contentIDList.contains(version.getDMS_Content_ID()) || version.getDMS_Content().getDMS_ContentType_ID() == 0)
			{
				contentsMapFiltered.put(mapEntry.getKey(), mapEntry.getValue());
			}
		}

		return contentsMapFiltered;
	} // getFilteredContentList
}
