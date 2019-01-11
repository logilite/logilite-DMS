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
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_MimeType;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;

public class FileSizeFixProcess extends SvrProcess
{

	private DMS	dms;

	@Override
	protected void prepare()
	{
		dms = new DMS(getAD_Client_ID());
	}

	@Override
	protected String doIt() throws Exception
	{
		int cntMigratedwithoutVersion = 0;
		int cntMigratedwithVersion = 0;

		addLog("Process started..");
		addLog("MIGRATING WITHOUT VERSION");

		cntMigratedwithoutVersion = migrateWithoutVersion();
		// addLog("MIGRATING WITH VERSION");
		// cntMigratedwithVersion = migrateWithVersion();
		addLog("Process complete withoutVersion(" + cntMigratedwithoutVersion + ") withVersion (" + cntMigratedwithVersion + ") file renamed..");
		return "Process complete withoutVersion(" + cntMigratedwithoutVersion + ") withVersion (" + cntMigratedwithVersion + ") file renamed..";
	}

	private int migrateWithoutVersion()
	{
		int cntMigrated = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		HashMap<I_DMS_Content, I_DMS_Association> contentList = getDMSContents(false);
		for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : contentList.entrySet())
		{

			MDMSContent mdmsContent = (MDMSContent) entry.getKey();
			MDMSAssociation mdmsAssociation = (MDMSAssociation) entry.getValue();

			I_DMS_MimeType mimytype = mdmsContent.getDMS_MimeType();
			if (mimytype.getFileExtension().equalsIgnoreCase(".def"))
			{
				continue;
			}

			if (mdmsContent.getName().endsWith(mimytype.getFileExtension()))
			{
				continue;
			}

			String oldname = mdmsContent.getName().replaceAll("\\s+$", "");
			String newName = oldname + mimytype.getFileExtension();

			int DMS_Content_ID = mdmsContent.getDMS_Content_ID();
			try
			{
				String sql = "SELECT a.DMS_Content_ID, a.DMS_Association_ID	FROM DMS_Association a 		"
						+ " INNER JOIN DMS_Content c ON a.DMS_Content_ID = c.DMS_Content_ID 			"
						+ "	WHERE a.DMS_AssociationType_ID = 1000000 AND DMS_Content_Related_ID = ?		ORDER BY a.DMS_Association_ID ";

				pstmt = DB.prepareStatement(sql, null);
				pstmt.setInt(1, DMS_Content_ID);

				rs = pstmt.executeQuery();

				if (!rs.next())
				{
					if (renameFile(mdmsContent, mdmsAssociation, newName))
					{
						cntMigrated++;
					}
				}
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Rename content failure.", e);
				int contentID = mdmsContent != null ? mdmsContent.getDMS_Content_ID() : 0;
				addLog("Fail to rename version oldName[" + mdmsContent.getName() + "] dms_content_id[" + contentID + "]");
				throw new AdempiereException("Rename content failure: " + e.getLocalizedMessage());
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}

		}
		return cntMigrated;
	}

	private HashMap<I_DMS_Content, I_DMS_Association> getDMSContents(boolean version)
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = null;
		try
		{
			if (!version)
			{
				sql = "SELECT a.DMS_Content_ID, a.DMS_Association_ID "
						+ "     FROM dms_association a INNER JOIN dms_content c ON a.dms_content_id = c.dms_content_id "
						+ "     WHERE c.contentbasetype = ? AND length(c.name) = 60 AND a.dms_associationtype_id <> 1000000";
			}
			else
			{
				sql = "SELECT DMS_Content_ID, DMS_Association_ID FROM dms_association "
						+ " WHERE dms_content_id IN (SELECT a.DMS_Content_Related_ID FROM dms_content c "
						+ " JOIN dms_association a ON c.dms_content_id = a.dms_content_id "
						+ " WHERE c.contentbasetype = ? and length(c.name) = 60 AND a.dms_associationtype_id = 1000000 )";
			}

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setString(1, X_DMS_Content.CONTENTBASETYPE_Content);
			rs = pstmt.executeQuery();

			if (rs != null)
			{
				while (rs.next())
				{
					map.put((new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null)),
							(new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null)));
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

	private boolean renameFile(MDMSContent content, MDMSAssociation association, String newName)
	{
		boolean flag = false;
		if (dms.getFileFromStorage(content) != null)
		{
			addLog(content.getDMS_Content_ID() + "| old name [" + content.getName() + "] to new name [" + newName + "] |parentPath [" + content.getParentURL()
					+ "] | SUCCESS");
			String newPath = dms.getFileFromStorage(content).getAbsolutePath();
			// String fileExt = newPath.substring(newPath.lastIndexOf("."),
			// newPath.length());
			newPath = newPath.substring(0, newPath.lastIndexOf(DMSConstant.FILE_SEPARATOR));
			newPath = newPath + DMSConstant.FILE_SEPARATOR + newName;
			newPath = Utils.getUniqueFilename(newPath);

			File oldFile = new File(dms.getFileFromStorage(content).getAbsolutePath());
			File newFile = new File(newPath);
			oldFile.renameTo(newFile);

			content.setName(newFile.getAbsolutePath().substring(newFile.getAbsolutePath().lastIndexOf(DMSConstant.FILE_SEPARATOR) + 1,
					newFile.getAbsolutePath().length()));
			
			//addLog("File available  : dms_content_id[" + content.getDMS_Content_ID() + "]");
			content.saveEx();
			flag = true;

		}
		else
		{
			addLog(content.getDMS_Content_ID() + "| old name [" + content.getName() + "] to new name [" + newName + "] |parentPath [" + content.getParentURL()
					+ "] | FAIL");
			//addLog("NO File available  : dms_content_id[" + content.getDMS_Content_ID() + "]");
			//content.setName(newName);
		}
		return flag;
	}

	private int migrateWithVersion()
	{

		int cntMigrated = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		HashMap<I_DMS_Content, I_DMS_Association> contentList = getDMSContents(true);
		for (Map.Entry<I_DMS_Content, I_DMS_Association> entry : contentList.entrySet())
		{

			MDMSContent mdmsContent = (MDMSContent) entry.getKey();
			MDMSAssociation mdmsAssociation = (MDMSAssociation) entry.getValue();

			I_DMS_MimeType mimytype = mdmsContent.getDMS_MimeType();
			if (mimytype.getFileExtension().equalsIgnoreCase(".def"))
			{
				continue;
			}

			if (mdmsContent.getName().endsWith(mimytype.getFileExtension()))
			{
				continue;
			}

			String oldname = mdmsContent.getName().replaceAll("\\s+$", "");
			String newName = oldname + mimytype.getFileExtension();
		//	addLog("start rename : [" + oldname +"] to [" + newName +"] dms_content_id[" + mdmsContent.getDMS_Content_ID() + "]" );
			renameFile(mdmsContent, mdmsAssociation, newName);
		//	addLog("rename DONE : [" + oldname +"] to [" + newName +"] dms_content_id[" + mdmsContent.getDMS_Content_ID() + "]" );
			cntMigrated++;

			int DMS_Content_ID = mdmsContent.getDMS_Content_ID();
			MDMSContent content = null;
			MDMSAssociation association = null;
			try
			{
				String sql = "SELECT a.DMS_Content_ID, a.DMS_Association_ID "
						+ "		FROM dms_association a INNER JOIN dms_content c ON a.dms_content_id = c.dms_content_id "
						+ "		WHERE a.dms_associationtype_id = 1000000 AND DMS_Content_Related_ID = ? " + "		Order By a.DMS_Association_ID";

				pstmt = DB.prepareStatement(sql, null);
				pstmt.setInt(1, DMS_Content_ID);

				rs = pstmt.executeQuery();

				while (rs.next())
				{
					content = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
					association = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
					String newVersionName = oldname + "(" + association.getSeqNo() + ")" + mimytype.getFileExtension();
				//	addLog("start rename version : [" + content.getName() +"] to [" + newVersionName +"] dms_content_id[" + content.getDMS_Content_ID() + "]" );
					renameFile(content, association, newVersionName);
				//	addLog("rename version DONE: [" + content.getName() +"] to [" + newVersionName +"] dms_content_id[" + content.getDMS_Content_ID() + "]" );
					cntMigrated++;
				}
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Rename content failure.", e);
				int contentID = content != null ? content.getDMS_Content_ID() : 0;
				addLog("Fail to rename version oldName[" + content.getName() + "] dms_content_id[" + contentID + "]");
				throw new AdempiereException("Rename content failure: " + e.getLocalizedMessage());
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}

		}
		return cntMigrated;

	}

}
