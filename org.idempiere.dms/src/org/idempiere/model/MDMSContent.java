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

package org.idempiere.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.Utils;

public class MDMSContent extends X_DMS_Content
{

	/**
	 * 
	 */
	private static final long	serialVersionUID			= -6250555517481249806L;

	private String				seqNo						= null;

	private boolean				isSyncIndexForLinkableDocs	= false;

	public MDMSContent(Properties ctx, int DMS_Content_ID, String trxName)
	{
		super(ctx, DMS_Content_ID, trxName);
	}

	public MDMSContent(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public static List<I_DMS_Content> getVersionHistory(MDMSContent content)
	{
		MDMSAssociation association = Utils.getAssociationFromContent(content.getDMS_Content_ID(), null);

		List<I_DMS_Content> contentList = new ArrayList<I_DMS_Content>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			pstmt = DB.prepareStatement(DMSConstant.SQL_FETCH_CONTENT_VERSION_LIST, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, null);
			pstmt.setInt(1, MDMSAssociationType.VERSION_ID);
			pstmt.setInt(2, association.getDMS_Content_Related_ID());
			pstmt.setInt(3, association.getDMS_Content_Related_ID());
			pstmt.setInt(4, MDMSAssociationType.VERSION_ID);

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				MDMSContent contentVersion = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
				// Set version number
				contentVersion.setSeqNo(rs.getString("SeqNo"));
				contentList.add(contentVersion);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Version list fetching failure: " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return contentList;
	} // getVersionHistory

	/**
	 * Get DMS content from Content UU
	 * 
	 * @param DMS_Content_UU
	 * @return {@link I_DMS_Content}
	 */
	public static I_DMS_Content getContent(String DMS_Content_UU)
	{
		if (Util.isEmpty(DMS_Content_UU, true))
			return null;

		return (I_DMS_Content) new Query(Env.getCtx(), MDMSContent.Table_Name, " DMS_Content_UU ILIKE ? ", null).setParameters(DMS_Content_UU).first();
	} // getContent

	public String getSeqNo()
	{
		return seqNo;
	}

	public void setSeqNo(String seqNo)
	{
		this.seqNo = seqNo;
	}

	public boolean isSyncIndexForLinkableDocs()
	{
		return isSyncIndexForLinkableDocs;
	}

	public void setSyncIndexForLinkableDocs(boolean isSyncIndexForLinkableDocs)
	{
		this.isSyncIndexForLinkableDocs = isSyncIndexForLinkableDocs;
	}
	
	public static int checkFileDirExists(String ParentUrl, String fileName)
	{
		int DMS_Content_ID = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if (Util.isEmpty(ParentUrl, true))
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_CHECK_CONTENT_NAME_EXISTS_NO_PARENT, null);
				pstmt.setString(1, fileName);
			}
			else
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_CHECK_CONTETNT_NAME_EXISTS, null);
				pstmt.setString(1, ParentUrl);
				pstmt.setString(2, fileName);
			}
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				DMS_Content_ID = rs.getInt(1);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while checking the file or directory name: " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return DMS_Content_ID;
	}


	public static List<String> getExtistingFileNamesForCopiedFile(String ParentURL, String fileName, String ext,
			boolean isVersion)
	{
		List<String> fileNames = new ArrayList<String>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if (Util.isEmpty(ParentURL, true))
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_CHECK_COPY_FILE_EXISTS_NO_PARENT, null);
				pstmt.setString(1, fileName + "." + ext);
				pstmt.setString(2, fileName + " - copy%" + "." + ext);

			}
			else
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_CHECK_COPY_FILE_EXISTS_FOR, null);
				pstmt.setString(1, fileName + "." + ext);
				pstmt.setString(2, fileName + " - copy%" + "." + ext);
				pstmt.setString(3, ParentURL);
			}

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String value = rs.getString(1);
				fileNames.add(value);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching existing files name: " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return fileNames;
	}

	public static String getNameOfPreviousVersion(int DMS_Content_ID)
	{
		String name = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_FILE_NAME_BY_FOR_VERSION, null);
			pstmt.setInt(1, DMS_Content_ID);
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				name = rs.getString(1);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the file from dms content: " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return name;
	}

	public static String getContentAttributeType(int DMS_Content_ID)
	{
		String contentType = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_CONTENT_TYPE, null);
			pstmt.setInt(1, DMS_Content_ID);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				contentType = rs.getString(1);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the content type for dms content: " + e,
					e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return contentType;
	}

	public static String getCopiedContentParentName(int DMS_Content_ID)
	{
		String contentType = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_COPIED_CONTENT_NAME, null);
			pstmt.setInt(1, DMS_Content_ID);
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				contentType = rs.getString(1);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the content type for dms content: " + e,
					e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return contentType;
	}
	
	public static List<MDMSContent> getVersionRelatedContentID(int DMS_Content_ID)
	{
		List<MDMSContent> dmsContent = new ArrayList<MDMSContent>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_ANOTHER_VERSION_IDS, null);
			pstmt.setInt(1, DMS_Content_ID);
			pstmt.setInt(2, DMS_Content_ID);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				MDMSContent content = new MDMSContent(Env.getCtx(), rs, null);
				dmsContent.add(content);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the content type for dms content: " + e,
					e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return dmsContent;
	}
	
	public static List<String> getMatchingActualNames(String ParentUrl, String fileName, String regExp, String extention){
		List<String> actualNames = new ArrayList<String>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if (Util.isEmpty(ParentUrl, true))
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_CHECK_FILE_EXISTS_NO_PARENT, null);
				pstmt.setString(1, fileName + extention);
				pstmt.setString(2, fileName + regExp + extention);
			}
			else
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_CHECK_FILE_EXISTS, null);
				pstmt.setString(1, fileName + extention);
				pstmt.setString(2, fileName + regExp + extention);
				pstmt.setString(3, ParentUrl);
			}

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String value = rs.getString(1);
				actualNames.add(value);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the existing file names : " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return actualNames;
	}
	
	public static MDMSContent getDMSContent(int DMS_Content_ID)
	{
		MDMSContent dmsContent = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_GET_DMS_CONTENT, null);
				pstmt.setInt(1, DMS_Content_ID);
			}

			rs = pstmt.executeQuery();
			if (rs.next())
			{
				dmsContent = new MDMSContent(Env.getCtx(), DMS_Content_ID, null);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the existing file names : " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return dmsContent;
	}
	
	// TODO Move to org.idempiere.dms.factories.Utils class 
	// and refactor as per query changes suggested in DMSConstant class
	public static List<String> getMatchingDirContentNames(String ParentUrl, String dirName){
		List<String> actualNames = new ArrayList<String>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if (Util.isEmpty(ParentUrl, true))
			{
				pstmt = DB.prepareStatement(DMSConstant.GET_COPY_DIR_MACHING_CONTENT_NAME_NO_PARENT, null);
				pstmt.setString(1, dirName);
				pstmt.setString(2, dirName + " %");
			}
			else
			{
				pstmt = DB.prepareStatement(DMSConstant.GET_COPY_DIR_MACHING_CONTENT_NAME, null);
				pstmt.setString(1, dirName);
				pstmt.setString(2, dirName+ " %");
				pstmt.setString(3, ParentUrl);
			}

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String value = rs.getString(1);
				actualNames.add(value);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the existing file names : " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return actualNames;
	}
	
	// TODO Move to org.idempiere.dms.factories.Utils class 
	// and refactor as per query changes suggested in DMSConstant class
	public static List<String> getActualDirNameForCopiedDir(String ParentUrl, String dirName)
	{

		List<String> fileNames = new ArrayList<String>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if (Util.isEmpty(ParentUrl, true))
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_CHECK_FILE_EXISTS_NO_PARENT, null);
				pstmt.setString(1, dirName );
				pstmt.setString(2, dirName + " %");
			}
			else
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_CHECK_FILE_EXISTS, null);
				pstmt.setString(1, dirName );
				pstmt.setString(2, dirName + " %");
				pstmt.setString(3, ParentUrl);
			}

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String value = rs.getString(1);
				fileNames.add(value);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the existing file names : " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return fileNames;
	}
	
	// TODO Move to org.idempiere.dms.factories.Utils class 
	// and refactor as per query changes suggested in DMSConstant class
	public static int checkDirExists(String ParentUrl, String dirName)
	{
		int DMS_Content_ID = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if (Util.isEmpty(ParentUrl, true))
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_CHECK_ACTUAL_FILE_DIR_EXISTS_NO_PARENT, null);
				pstmt.setString(1, dirName);
			}
			else
			{
				pstmt = DB.prepareStatement(DMSConstant.SQL_CHECK_ACTUAL_FILE_DIR_EXISTS, null);
				pstmt.setString(1, ParentUrl);
				pstmt.setString(2, dirName);
			}
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				DMS_Content_ID = rs.getInt(1);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while checking the file name: " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return DMS_Content_ID;
	}
}
