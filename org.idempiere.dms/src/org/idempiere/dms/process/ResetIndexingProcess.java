package org.idempiere.dms.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

/**
 * 
 * @author ravi
 *
 */
public class ResetIndexingProcess extends SvrProcess
{

	private IIndexSearcher			indexSeracher		= null;
	
	private String  				m_isIndexed         = null; 

	@Override
	protected void prepare()
	{

		/*
		 * Get Parameters
		 * parameter 1 : IsIndexed
		 * 
		 */
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (("IsIndexed").equals(name))
			{
				m_isIndexed = para[i].getParameterAsString();
			}

		}
		
		indexSeracher = ServiceUtils.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));

		if (indexSeracher == null)
			throw new AdempiereException("Solr Index server is not found.");

	}

	@Override
	protected String doIt() throws Exception
	{
		int cntSuccess = 0;
		int cntFailed = 0;
		HashMap<I_DMS_Content, I_DMS_Association> contentList = getAllDMSContents();
		for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : contentList.entrySet())
		{
			MDMSContent mdmsContent = (MDMSContent) entry.getKey();
			boolean isIndexed = mdmsContent.isIndexed();
			String contentType = mdmsContent.getContentBaseType();
			MDMSAssociation mdmsAssociation = (MDMSAssociation) entry.getValue();
			try
			{
				if (MDMSContent.CONTENTBASETYPE_Content.equals(contentType))
				{
					Map<String, Object> solrValue = Utils.createIndexMap(mdmsContent, mdmsAssociation);
					indexSeracher.deleteIndex(mdmsContent.getDMS_Content_ID());
					indexSeracher.indexContent(solrValue);
					cntSuccess++;

					// Update the value of IsIndexed flag in dmsContent
					if (!isIndexed)
					{
						DB.executeUpdate("UPDATE DMS_Content SET IsIndexed = 'Y' WHERE DMS_Content_ID = ? ",
								mdmsContent.get_ID(), null);
					}
				}
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "RE-Indexing of Content Failure :", e);
				addLog("RE-Indexing of Content Failure : dms_content_id[" + mdmsContent.getDMS_Content_ID() + "]"+ e);
				cntFailed++;
				
				// Update the value of IsIndexed flag in dmsContent
				if (isIndexed)
				{
					DB.executeUpdate("UPDATE DMS_Content SET IsIndexed = 'N' WHERE DMS_Content_ID = ? ",
							mdmsContent.get_ID(), null);
				}
			}
			
		}
		return "Process completed. Success : " + cntSuccess + ", Failed : " + cntFailed;
	}

	

	/**
	 * Fetch record from DMS content
	 * based on IsIndexed flag
	 * 
	 * @return map
	 */
	private HashMap<I_DMS_Content, I_DMS_Association> getAllDMSContents()
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuilder sql = new StringBuilder();
		try
		{
			sql.append("SELECT a.DMS_Content_ID, a.DMS_Association_ID FROM dms_association a ").append(
					"INNER JOIN dms_content c ON a.dms_content_id = c.dms_content_id WHERE c.AD_Client_ID = ? ");

			if ("T".equals(m_isIndexed)) // "T" => records in which isIndex flag is true.
				sql.append(" AND c.IsIndexed = 'Y'");
			else if ("F".equals(m_isIndexed)) // "F" => records in which isIndex flag is false.
				sql.append(" AND c.IsIndexed = 'N'");
			else if ("A".equals(m_isIndexed)) // All records.
				sql.append(" AND (c.IsIndexed = 'Y' OR c.IsIndexed = 'N')");
			
			
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, Env.getAD_Client_ID(getCtx()));
			rs = pstmt.executeQuery();

			if (rs != null)
			{
				while (rs.next())
				{
					map.put((new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null)), (new MDMSAssociation(
							Env.getCtx(), rs.getInt("DMS_Association_ID"), null)));
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Content fetching failure: ", e);
			addLog("Content fetching failure");
			throw new AdempiereException("Content fetching failure: " + e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return map;
	}
}
