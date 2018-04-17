package org.idempiere.dms.process;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_MimeType;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

public class FileSizeFixProcess extends SvrProcess
{

	private IFileStorageProvider	fileStorgProvider	= null;
	private IContentManager			contentManager		= null;
	private IIndexSearcher			indexSeracher		= null;
	private static final String		spFileSeprator		= Utils.getStorageProviderFileSeparator();

	@Override
	protected void prepare()
	{

		fileStorgProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);

		if (fileStorgProvider == null)
			throw new AdempiereException("Storage provider is not define on clientInfo.");

		contentManager = Utils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		indexSeracher = ServiceUtils.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));

		if (indexSeracher == null)
			throw new AdempiereException("Solr Index server is not found.");

	}

	@Override
	protected String doIt() throws Exception
	{
		int cntMigrated = 0;
		
		addLog("Process started..");

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		HashMap<I_DMS_Content, I_DMS_Association> contentList = getDMSContents();
		for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : contentList.entrySet())
		{

			MDMSContent mdmsContent = (MDMSContent) entry.getKey();
			MDMSAssociation mdmsAssociation = (MDMSAssociation) entry.getValue();

			I_DMS_MimeType mimytype = mdmsContent.getDMS_MimeType();
			if (mimytype.getFileExtension() == ".def" || mdmsContent.getName().endsWith(mimytype.getFileExtension()))
			{
				continue;
			}

			String oldname = mdmsContent.getName().replaceAll("\\s+$", "");
			String newName = oldname + mimytype.getFileExtension();
			addLog("start rename : [" + oldname +"] to [" + newName +"] dms_content_id[" + mdmsContent.getDMS_Content_ID() + "]" );
			renameFile(mdmsContent, mdmsAssociation, newName);
			addLog("rename DONE : [" + oldname +"] to [" + newName +"] dms_content_id[" + mdmsContent.getDMS_Content_ID() + "]" );
			cntMigrated++;

			int DMS_Content_ID = mdmsContent.getDMS_Content_ID();
			MDMSContent content = null;
			MDMSAssociation association = null;
			try
			{
				String sql = "SELECT a.DMS_Content_ID, a.DMS_Association_ID "
						+ "		FROM dms_association a INNER JOIN dms_content c ON a.dms_content_id = c.dms_content_id "
						+ "		WHERE AND a.dms_associationtype_id = 1000000 AND DMS_Content_Related_ID = ? "
						+ "		Order By a.DMS_Association_ID";
				
				pstmt = DB.prepareStatement(sql, null);
				pstmt.setInt(1, DMS_Content_ID);

				rs = pstmt.executeQuery();

				while (rs.next())
				{
					content = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
					association = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
					String newVersionName = oldname + "("+association.getSeqNo()+")" +mimytype.getFileExtension();
					addLog("start rename version : [" + content.getName() +"] to [" + newVersionName +"] dms_content_id[" + content.getDMS_Content_ID() + "]" );
					renameFile(content, association, newVersionName);
					addLog("rename version DONE: [" + content.getName() +"] to [" + newVersionName +"] dms_content_id[" + content.getDMS_Content_ID() + "]" );
					cntMigrated++;
				}
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Rename content failure.", e);
				int contentID = content != null ? content.getDMS_Content_ID() : 0 ;
				addLog("Fail to rename version oldName[" + content.getName() +"] dms_content_id[" + contentID + "]" );
				throw new AdempiereException("Rename content failure: " + e.getLocalizedMessage());
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}

		}
		addLog("Process complete sucessfully..");
		return "Process Completed. (" + cntMigrated + ") file renamed..";
	}

	private HashMap<I_DMS_Content, I_DMS_Association> getDMSContents()
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT a.DMS_Content_ID, a.DMS_Association_ID "
					+ "     FROM dms_association a INNER JOIN dms_content c ON a.dms_content_id = c.dms_content_id "
					+ "     WHERE c.contentbasetype = ? AND length(c.name) >= 60 AND a.dms_associationtype_id <> 1000000";
			
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setString(1, X_DMS_Content.CONTENTBASETYPE_Content);
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

	private void renameFile(MDMSContent content, MDMSAssociation association, String newName)
	{
		if (fileStorgProvider.getFile(contentManager.getPath(content)) != null)
		{
			String newPath = fileStorgProvider.getFile(contentManager.getPath(content)).getAbsolutePath();
			// String fileExt = newPath.substring(newPath.lastIndexOf("."),
			// newPath.length());
			newPath = newPath.substring(0, newPath.lastIndexOf(spFileSeprator));
			newPath = newPath + spFileSeprator + newName;
			newPath = Utils.getUniqueFilename(newPath);

			File oldFile = new File(fileStorgProvider.getFile(contentManager.getPath(content)).getAbsolutePath());
			File newFile = new File(newPath);
			oldFile.renameTo(newFile);

			content.setName(newFile.getAbsolutePath().substring(newFile.getAbsolutePath().lastIndexOf(spFileSeprator) + 1,
					newFile.getAbsolutePath().length()));
		}
		else
		{
			addLog("NO File available  : dms_content_id[" + content.getDMS_Content_ID() + "]");
			content.setName(newName);
		}

		content.saveEx();

		try
		{
			Map<String, Object> solrValue = Utils.createIndexMap(content, association);
			indexSeracher.deleteIndex(content.getDMS_Content_ID());
			indexSeracher.indexContent(solrValue);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "RE-Indexing of Content Failure :", e);
			addLog("RE-Indexing of Content Failure : dms_content_id[" + content.getDMS_Content_ID() + "]");
			throw new AdempiereException("RE-Indexing of Content Failure :" + e);
		}
	}

}
