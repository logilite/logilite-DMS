package org.idempiere.dms.process;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_MimeType;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

public class FileSizeFixProcess extends SvrProcess{

	private IFileStorageProvider	fileStorgProvider	= null;
	private IContentManager			contentManager		= null;
	private IIndexSearcher			indexSeracher		= null;
	private static final String		spFileSeprator		= Utils.getStorageProviderFileSeparator();
	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		
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
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub
		int cntMigrated = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		List<I_DMS_Content> contentList = getDMSContents();
		for (I_DMS_Content DMSContent : contentList){
			
	//		if(DMSContent.getName().lastIndexOf(".") <= 0){
				I_DMS_MimeType mimytype = DMSContent.getDMS_MimeType();
				if(mimytype.getFileExtension() == ".def"){
					continue;
				}
				
				if(DMSContent.getName().endsWith(mimytype.getFileExtension())){
					continue;
				}

				int DMS_Content_ID = Utils.getDMS_Content_Related_ID(DMSContent);
				MDMSContent content = null;
				MDMSAssociation association = null;
				try
				{
					pstmt = DB.prepareStatement(Utils.SQL_GET_RELATED_CONTENT, null);
					pstmt.setInt(1, DMS_Content_ID);
					pstmt.setInt(2, DMS_Content_ID);

					rs = pstmt.executeQuery();

					while (rs.next())
					{
						content = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
						association = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
						renameFile(content, association, DMSContent.getName().replaceAll("\\s+$", "") + mimytype.getFileExtension());
						cntMigrated++;
					}
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, "Rename content failure.", e);
					throw new AdempiereException("Rename content failure: " + e.getLocalizedMessage());
				}
				finally
				{
					DB.close(rs, pstmt);
					rs = null;
					pstmt = null;
				}

		//	}
			
		}
		return "Process Completed. ("+cntMigrated+") file renamed..";
	}
	
	
	private List<I_DMS_Content> getDMSContents()
	{
		List<I_DMS_Content> lst = new ArrayList<I_DMS_Content>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT * FROM dms_content WHERE contentbasetype = ? AND length(name) >= 60";
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setString(1, X_DMS_Content.CONTENTBASETYPE_Content);
			rs = pstmt.executeQuery();

			if (rs != null)
			{
				while (rs.next())
				{
					lst.add(new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null));
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Content fetching failure: ", e);
			throw new AdempiereException("Content fetching failure: " + e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return lst;
	}
	
	private void renameFile(MDMSContent content, MDMSAssociation association, String newName)
	{
		if(fileStorgProvider.getFile(contentManager.getPath(content)) != null){
			String newPath = fileStorgProvider.getFile(contentManager.getPath(content)).getAbsolutePath();
			//	String fileExt = newPath.substring(newPath.lastIndexOf("."), newPath.length());
				newPath = newPath.substring(0, newPath.lastIndexOf(spFileSeprator));
				newPath = newPath + spFileSeprator + newName;
				newPath = Utils.getUniqueFilename(newPath);

				File oldFile = new File(fileStorgProvider.getFile(contentManager.getPath(content)).getAbsolutePath());
				File newFile = new File(newPath);
				oldFile.renameTo(newFile);

				content.setName(newFile.getAbsolutePath().substring(newFile.getAbsolutePath().lastIndexOf("/") + 1,
						newFile.getAbsolutePath().length()));
		}else{
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
			throw new AdempiereException("RE-Indexing of Content Failure :" + e);
		}
	}
	
}
