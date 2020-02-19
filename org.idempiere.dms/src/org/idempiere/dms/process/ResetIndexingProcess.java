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
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

/**
 * @author ravi
 */
public class ResetIndexingProcess extends SvrProcess
{
	public static final String	SQL_GET_ALL_DMS_CONTENT	= " SELECT a.DMS_Content_ID, a.DMS_Association_ID 	FROM DMS_Association a	"
	                                                      + " INNER JOIN DMS_Content c ON a.DMS_Content_ID = c.DMS_Content_ID		"
	                                                      + " WHERE c.AD_Client_ID = ? AND c.ContentBaseType = 'CNT'				";

	private IIndexSearcher		indexSeracher			= null;

	private String				m_isIndexed				= null;

	private boolean				isAllReIndex			= false;

	@Override
	protected void prepare()
	{
		/*
		 * Get Parameters
		 * parameter 1 : IsIndexed
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

		indexSeracher = ServiceUtils.getIndexSearcher(getAD_Client_ID());
		if (indexSeracher == null)
			throw new AdempiereException("Solr Index server is not found.");

		// 
		isAllReIndex = "A".equals(m_isIndexed);
	}

	@Override
	protected String doIt() throws Exception
	{
		int cntSuccess = 0;
		int cntFailed = 0;

		IFileStorageProvider fsProvider = FileStorageUtil.get(getAD_Client_ID(), false);
		if (fsProvider == null)
			throw new AdempiereException("Storage provider is not defined on clientInfo.");

		IContentManager contentManager = Utils.getContentManager(getAD_Client_ID());
		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		// Delete all the index from indexing server
		if (isAllReIndex)
		{
			indexSeracher.deleteAllIndex();

			int no = DB.executeUpdate("UPDATE DMS_Content	SET IsIndexed='N'	WHERE ContentBaseType='CNT' AND AD_Client_ID=?", getAD_Client_ID(), null);
			log.log(Level.INFO, "All the DMS content indexed marked as false, count: " + no);
		}

		// Get all DMS contents
		statusUpdate("Fetching DMS contents from DB");
		HashMap <Integer, Integer> contentList = getAllDMSContents();

		//
		int count = 0;
		for (Map.Entry <Integer, Integer> entry : contentList.entrySet())
		{
			MDMSContent content = new MDMSContent(getCtx(), entry.getKey(), null);
			MDMSAssociation association = new MDMSAssociation(getCtx(), entry.getValue(), null);

			try
			{
				if (!isAllReIndex)
				{
					indexSeracher.deleteIndex(content.getDMS_Content_ID());
				}

				Map <String, Object> solrValue = Utils.createIndexMap(content, association);
				indexSeracher.indexContent(solrValue, fsProvider.getFile(contentManager.getPath(content)));
				cntSuccess++;

				// Update the value of IsIndexed flag in dmsContent
				if (!content.isIndexed())
				{
					DB.executeUpdate("UPDATE DMS_Content	SET IsIndexed='Y'	WHERE DMS_Content_ID=? ", content.get_ID(), null);
				}
			}
			catch (Exception e)
			{
				String errorMsg = "RE-Indexing of Content Failure: DMS_Content_ID [" + content.getDMS_Content_ID() + "] - " + e.getLocalizedMessage();
				log.log(Level.SEVERE, errorMsg, e);
				addLog(errorMsg);
				cntFailed++;

				// Update the value of IsIndexed flag in dmsContent
				if (!isAllReIndex && content.isIndexed())
				{
					DB.executeUpdate("UPDATE DMS_Content 	SET IsIndexed='N'	WHERE DMS_Content_ID=? ", content.get_ID(), null);
				}
			}
			statusUpdate("Content Indexed [" + count + " / " + contentList.size() + "] Success=" + cntSuccess + ", Fail=" + cntFailed);
			count++;
		}
		return "Process completed. Success : " + cntSuccess + ", Failed : " + cntFailed;
	} // doIt

	/**
	 * Fetch record from DMS content and its Association
	 * based on IsIndexed flag
	 * 
	 * @return map
	 */
	private HashMap <Integer, Integer> getAllDMSContents()
	{
		HashMap <Integer, Integer> map = new LinkedHashMap <Integer, Integer>();

		String sql = SQL_GET_ALL_DMS_CONTENT;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if ("T".equals(m_isIndexed)) // "T" => records in which isIndex flag is true.
				sql += " AND c.IsIndexed = 'Y'";
			else if ("F".equals(m_isIndexed)) // "F" => records in which isIndex flag is false.
				sql += " AND c.IsIndexed = 'N'";
			else if ("A".equals(m_isIndexed)) // All records.
				; // sql += " AND (c.IsIndexed = 'Y' OR c.IsIndexed = 'N')";

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, getAD_Client_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				map.put(rs.getInt("DMS_Content_ID"), rs.getInt("DMS_Association_ID"));
			}
		}
		catch (SQLException e)
		{
			addLog("Content fetching failure" + e.getLocalizedMessage());
			log.log(Level.SEVERE, "Content fetching failure: " + e.getLocalizedMessage(), e);
			throw new AdempiereException("Content fetching failure: " + e.getLocalizedMessage(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return map;
	} // getAllDMSContents
}
